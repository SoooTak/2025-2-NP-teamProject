# server/Dockerfile

# 1. Python 기본 이미지
FROM python:3.11-slim

# 2. 작업 디렉터리
WORKDIR /app

# 3. 서버 스크립트 복사
COPY lms_server.py .

# 4. MySQL 클라이언트 라이브러리 설치 (PyMySQL 사용)
RUN pip install --no-cache-dir pymysql cryptography

# 5. 버퍼링 없이 서버 실행 (-u)
CMD ["python", "-u", "lms_server.py"]
