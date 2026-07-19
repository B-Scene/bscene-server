FROM python:3.13-slim

WORKDIR /app

ENV TZ=Asia/Seoul

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

# 빌드 타임에 모델을 미리 받아 이미지에 포함시킨다.
# --rm으로 컨테이너를 매번 지우므로, 미리 안 받아두면 크론 실행마다(매일) HuggingFace에서 재다운로드하게 된다.
RUN python -c "from sentence_transformers import SentenceTransformer; SentenceTransformer('jhgan/ko-sroberta-multitask')"

# 위 다운로드 이후에만 오프라인 모드를 켠다 (빌드 중엔 네트워크가 있어야 받을 수 있으므로).
# 런타임엔 캐시된 모델만 쓰고 HuggingFace로 아예 네트워크 요청을 안 보낸다.
ENV HF_HUB_OFFLINE=1
