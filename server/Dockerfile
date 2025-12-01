# server/Dockerfile

FROM python:3.12-slim

WORKDIR /app

# MySQL 접속용 드라이버 + cryptography 설치 (이전에 떴던 에러 방지)
RUN pip install --no-cache-dir mysql-connector-python cryptography

# 서버 스크립트 복사
COPY lms_server.py .

# 제출/영상 디렉토리 생성 (볼륨 마운트 하면 videos는 덮어씌워져도 상관 없음)
RUN mkdir -p /app/submissions/app/videos

EXPOSE 5051 8081

CMD ["python", "-u", "lms_server.py"]
