"""
진행자 개인 path(WHIP 원본)들을 GStreamer로 믹싱해 메인 path로 되돌려 넣는 슈퍼바이저.

동작 방식
- MediaMTX Control API(v3/paths/list)를 폴링해 ready 상태의 멤버 path({mainPath}-m{n})를 메인 path별로 그루핑
- 메인 path마다 gst-launch 파이프라인(서브프로세스) 1개: rtspsrc x n -> decodebin -> audiomixer -> opusenc -> rtspclientsink
- 멤버 입퇴장으로 입력 구성이 바뀌면 새 파이프라인을 먼저 띄운 뒤 기존 것을 정리한다(make-before-break).
  새 파이프라인의 publish가 기존 RTSP 세션을 대체(MediaMTX overridePublisher)하므로 메인 path 공백이 없고,
  대체당한 구 파이프라인은 sink 에러로 스스로 종료된다 (안 죽으면 시한 후 강제 종료)
- 크래시 재시작은 연속 크래시 횟수에 따라 지수 백오프한다. 라이브가 이미 종료돼 publish 인가가
  계속 403인 경우(Spring canPublish: OPEN 검사) 재시도 폭주를 막기 위함
- 입력이 0개가 되면 파이프라인 종료 -> 메인 path 소멸 -> Spring syncLiveState가 연속 미검출
  유예(폴링 3회, ~15초) 후 라이브 종료 처리

인증
- RTSP pull/publish 모두 MediaMTX가 Spring(authHTTP)에 위임하므로, URL에 mixer:{MIXER_TOKEN} 자격을 실어 보낸다
  (Spring StreamServiceImpl.isMixerToken과 동일 값이어야 함)
"""
import json
import os
import re
import signal
import subprocess
import sys
import time
import urllib.parse
import urllib.request

API_URL = os.environ.get("MTX_API_URL", "http://mediamtx:9997")
RTSP_URL = os.environ.get("MTX_RTSP_URL", "rtsp://mediamtx:8554")
TOKEN = os.environ.get("MIXER_TOKEN", "")
POLL_INTERVAL_SEC = float(os.environ.get("POLL_INTERVAL_SEC", "2"))
RESTART_BACKOFF_SEC = float(os.environ.get("RESTART_BACKOFF_SEC", "3"))
RESTART_BACKOFF_MAX_SEC = float(os.environ.get("RESTART_BACKOFF_MAX_SEC", "60"))
# 이 시간 이상 돌다 죽은 파이프라인은 크래시 루프가 아니라 일시 장애로 보고 백오프를 리셋
HEALTHY_RUNTIME_SEC = float(os.environ.get("HEALTHY_RUNTIME_SEC", "30"))
# make-before-break로 대체된 구 파이프라인이 스스로 안 죽을 때 강제 종료까지의 시한
DRAIN_TIMEOUT_SEC = float(os.environ.get("DRAIN_TIMEOUT_SEC", "10"))

# StreamServiceImpl.MEMBER_PATH_PATTERN과 동일한 규약: {UUID}-m{streamMemberId}
MEMBER_PATH = re.compile(r"^([0-9a-f\-]{36})-m\d+$")

# main_path -> {"members": frozenset[str], "proc": subprocess.Popen, "started": float}
pipelines = {}
# main_path -> 마지막 시작 시각 (크래시 루프 백오프)
last_start = {}
# main_path -> 연속 크래시 횟수 (지수 백오프 지수)
crash_counts = {}
# make-before-break로 대체된 구 파이프라인: {"proc": Popen, "main": str, "deadline": float}
draining = []


def list_ready_paths():
    req = urllib.request.Request(API_URL + "/v3/paths/list?itemsPerPage=500")
    with urllib.request.urlopen(req, timeout=3) as res:
        body = json.load(res)
    return {item["name"] for item in body.get("items", []) if item.get("ready")}


def rtsp_url(path):
    cred = "mixer:" + urllib.parse.quote(TOKEN, safe="")
    return RTSP_URL.replace("rtsp://", "rtsp://" + cred + "@", 1) + "/" + path


def build_command(main_path, member_paths):
    cmd = [
        "gst-launch-1.0", "-q",
        "audiomixer", "name=mix",
        "!", "audioconvert",
        "!", "audioresample",
        "!", "audio/x-raw,rate=48000,channels=2",
        "!", "opusenc", "bitrate=96000",
        "!", "rtspclientsink", "location=" + rtsp_url(main_path), "protocols=tcp",
    ]
    # 입력 순서를 고정해 같은 구성이면 같은 커맨드가 되도록 정렬
    for member_path in sorted(member_paths):
        cmd += [
            "rtspsrc", "location=" + rtsp_url(member_path), "protocols=tcp", "latency=200",
            "!", "decodebin",
            "!", "audioconvert",
            "!", "audioresample",
            "!", "mix.",
        ]
    return cmd


def start_pipeline(main_path, members, now):
    proc = subprocess.Popen(build_command(main_path, members))
    pipelines[main_path] = {"members": frozenset(members), "proc": proc, "started": now}
    last_start[main_path] = now
    print("믹서 시작 main=%s inputs=%d" % (main_path, len(members)))


def stop_pipeline(main_path, entry, reason):
    proc = entry["proc"]
    if proc.poll() is None:
        proc.terminate()
        try:
            proc.wait(timeout=5)
        except subprocess.TimeoutExpired:
            proc.kill()
            proc.wait()
    print("믹서 중지 main=%s reason=%s" % (main_path, reason))


def reap_draining(now):
    for entry in draining[:]:
        if entry["proc"].poll() is not None:
            draining.remove(entry)
        elif now >= entry["deadline"]:
            stop_pipeline(entry["main"], entry, "drain-timeout")
            draining.remove(entry)


def shutdown(signum, _frame):
    for main_path, entry in list(pipelines.items()):
        stop_pipeline(main_path, entry, "shutdown")
    for entry in draining:
        stop_pipeline(entry["main"], entry, "shutdown")
    sys.exit(0)


def main():
    if not TOKEN:
        # 토큰 없이는 Spring 인증을 통과할 수 없으므로 기동 실패로 처리 (restart: always가 재시도)
        print("MIXER_TOKEN 미설정: 종료", file=sys.stderr)
        sys.exit(1)

    signal.signal(signal.SIGTERM, shutdown)
    signal.signal(signal.SIGINT, shutdown)

    while True:
        try:
            ready = list_ready_paths()
        except Exception as e:  # MediaMTX 재기동 등. 다음 폴링에서 복구
            print("MediaMTX 폴링 실패: %s" % e, file=sys.stderr)
            time.sleep(POLL_INTERVAL_SEC)
            continue

        desired = {}
        for path in ready:
            match = MEMBER_PATH.match(path)
            if match:
                desired.setdefault(match.group(1), set()).add(path)

        now = time.monotonic()
        reap_draining(now)

        # 크래시했거나, 입력 구성이 바뀌었거나, 멤버가 전부 나간 파이프라인 처리
        for main_path in list(pipelines):
            entry = pipelines[main_path]
            crashed = entry["proc"].poll() is not None
            new_members = frozenset(desired.get(main_path, set()))
            changed = new_members != entry["members"]

            if crashed:
                # 충분히 돌다 죽었으면 일시 장애로 보고 백오프 리셋, 기동 직후 죽었으면 크래시 루프로 집계
                if now - entry["started"] >= HEALTHY_RUNTIME_SEC:
                    crash_counts.pop(main_path, None)
                else:
                    crash_counts[main_path] = crash_counts.get(main_path, 0) + 1
                stop_pipeline(main_path, entry, "crashed")
                del pipelines[main_path]
            elif changed and new_members:
                # make-before-break: 새 파이프라인을 먼저 띄워 메인 path 공백을 없앤다.
                # 구 파이프라인은 새 publish에 세션을 대체당하면(overridePublisher) 스스로 종료된다
                draining.append({"proc": entry["proc"], "main": main_path, "deadline": now + DRAIN_TIMEOUT_SEC})
                print("믹서 교체 main=%s reason=membership-changed" % main_path)
                start_pipeline(main_path, new_members, now)
            elif changed:
                stop_pipeline(main_path, entry, "membership-changed")
                del pipelines[main_path]

        # 새로 시작 (연속 크래시 횟수에 따른 지수 백오프 이내 재시작은 다음 폴링으로 미룸)
        for main_path, members in desired.items():
            if main_path in pipelines:
                continue
            backoff = min(RESTART_BACKOFF_SEC * (2 ** crash_counts.get(main_path, 0)), RESTART_BACKOFF_MAX_SEC)
            if now - last_start.get(main_path, float("-inf")) < backoff:
                continue
            start_pipeline(main_path, members, now)

        # 라이브가 끝나 desired에서 사라진 main_path의 잔여 상태 정리 (무한 누적 방지)
        for state in (last_start, crash_counts):
            for main_path in list(state):
                if main_path not in desired and main_path not in pipelines:
                    del state[main_path]

        time.sleep(POLL_INTERVAL_SEC)


if __name__ == "__main__":
    main()
