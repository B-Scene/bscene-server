"""
진행자 개인 path(WHIP 원본)들을 GStreamer로 믹싱해 메인 path로 되돌려 넣는 슈퍼바이저.

동작 방식
- MediaMTX Control API(v3/paths/list)를 폴링해 ready 상태의 멤버 path({mainPath}-m{n})를 메인 path별로 그루핑
- 메인 path마다 gst-launch 파이프라인(서브프로세스) 1개: rtspsrc x n -> decodebin -> audiomixer -> opusenc -> rtspclientsink
- 멤버 입퇴장으로 입력 구성이 바뀌면 해당 파이프라인을 재시작 (재시작 동안 1~2초 출력 공백은 v1 트레이드오프)
- 입력이 0개가 되면 파이프라인 종료 -> 메인 path 소멸 -> Spring syncLiveState가 LIVE 키 TTL(15초) 후 라이브 종료 처리

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

# StreamServiceImpl.MEMBER_PATH_PATTERN과 동일한 규약: {UUID}-m{streamMemberId}
MEMBER_PATH = re.compile(r"^([0-9a-f\-]{36})-m\d+$")

# main_path -> {"members": frozenset[str], "proc": subprocess.Popen}
pipelines = {}
# main_path -> 마지막 시작 시각 (크래시 루프 백오프)
last_start = {}


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


def shutdown(signum, _frame):
    for main_path, entry in list(pipelines.items()):
        stop_pipeline(main_path, entry, "shutdown")
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

        # 크래시했거나, 입력 구성이 바뀌었거나, 멤버가 전부 나간 파이프라인 정리
        for main_path in list(pipelines):
            entry = pipelines[main_path]
            crashed = entry["proc"].poll() is not None
            changed = frozenset(desired.get(main_path, set())) != entry["members"]
            if crashed or changed:
                stop_pipeline(main_path, entry, "crashed" if crashed else "membership-changed")
                del pipelines[main_path]

        # 새로 시작 (백오프 이내 재시작은 다음 폴링으로 미룸)
        now = time.monotonic()
        for main_path, members in desired.items():
            if main_path in pipelines:
                continue
            if now - last_start.get(main_path, float("-inf")) < RESTART_BACKOFF_SEC:
                continue
            proc = subprocess.Popen(build_command(main_path, members))
            pipelines[main_path] = {"members": frozenset(members), "proc": proc}
            last_start[main_path] = now
            print("믹서 시작 main=%s inputs=%d" % (main_path, len(members)))

        time.sleep(POLL_INTERVAL_SEC)


if __name__ == "__main__":
    main()
