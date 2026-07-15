# ES 공식 이미지에 한글 형태소 분석기(nori)를 설치한 커스텀 이미지
# 버전 선정 기준 : Spring Boot 4.1 → Spring Data Elasticsearch 6.1.x → ES 9.4.x 호환
# 로컬 개발: docker build -t bscene-es:9.4.3 ./elasticsearch
# 서버 배포: buildx로 멀티 아키텍처(amd64/arm64) 빌드 후 팀 Docker Hub에 push
FROM docker.elastic.co/elasticsearch/elasticsearch:9.4.3

# --batch : 설치 중 확인 질문에 자동 yes
RUN bin/elasticsearch-plugin install --batch analysis-nori
