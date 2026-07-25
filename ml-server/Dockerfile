FROM python:3.13-slim AS builder

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir --target=/deps \
    --index-url https://download.pytorch.org/whl/cpu \
    --extra-index-url https://pypi.org/simple \
    -r requirements.txt

ENV PYTHONPATH=/deps
ENV HF_HOME=/hf-cache

# 빌드 타임에 모델을 미리 받아 이미지에 포함시킨다.
# --rm으로 컨테이너를 매번 지우므로, 미리 안 받아두면 크론 실행마다(매일) HuggingFace에서 재다운로드하게 된다.
RUN python -c "from sentence_transformers import SentenceTransformer; SentenceTransformer('jhgan/ko-sroberta-multitask')"


FROM python:3.13-slim

WORKDIR /app

ENV TZ=Asia/Seoul
ENV PYTHONPATH=/deps
ENV HF_HOME=/hf-cache
# 런타임엔 캐시된 모델만 쓰고 HuggingFace로 아예 네트워크 요청을 안 보낸다.
ENV HF_HUB_OFFLINE=1

COPY --from=builder /deps /deps
COPY --from=builder /hf-cache /hf-cache
COPY . .
