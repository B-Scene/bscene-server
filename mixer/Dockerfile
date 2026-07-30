FROM debian:bookworm-slim

# GStreamer 런타임 + RTSP 요소(rtspsrc/rtspclientsink) + Opus 코덱
# - plugins-base: audiomixer, audioconvert, audioresample, opusenc/opusdec
# - plugins-good: rtspsrc, RTP 관련 요소
# - gstreamer1.0-rtsp: rtspclientsink (믹스 결과를 MediaMTX로 되돌려 넣는 publish 요소)
RUN apt-get update && apt-get install -y --no-install-recommends \
        python3 \
        gstreamer1.0-tools \
        gstreamer1.0-plugins-base \
        gstreamer1.0-plugins-good \
        gstreamer1.0-plugins-bad \
        gstreamer1.0-rtsp \
    && rm -rf /var/lib/apt/lists/*

COPY mixer.py /app/mixer.py

# -u: 컨테이너 로그로 즉시 flush
CMD ["python3", "-u", "/app/mixer.py"]
