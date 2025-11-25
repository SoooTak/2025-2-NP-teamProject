# lms_server.py
import socket
import threading
import os
from datetime import datetime

import pymysql

# -----------------------------
# 네트워크 기본 설정
# -----------------------------
HOST = ""          # 0.0.0.0
PORT = 5050        # 컨테이너 내부 포트

# -----------------------------
# MySQL DB 설정 (환경에 맞게 수정)
# -----------------------------
DB_HOST = "host.docker.internal"   # Docker Desktop에서 호스트 PC 접속용
DB_PORT = 3306
DB_USER = "lmsuser"                # 네가 만든 계정으로 바꿔도 됨
DB_PASSWORD = "lms1234!"           # 네가 만든 계정 비번
DB_NAME = "mini_lms"               # 우리가 만든 DB 이름

# -----------------------------
# 파일 제출 경로
# -----------------------------
SUBMISSION_DIR = "submissions"
os.makedirs(SUBMISSION_DIR, exist_ok=True)


def get_db_connection():
    """
    MySQL 연결 생성 헬퍼.
    각 요청마다 새 커넥션을 열고, 사용 후 닫는다.
    """
    return pymysql.connect(
        host=DB_HOST,
        port=DB_PORT,
        user=DB_USER,
        password=DB_PASSWORD,
        database=DB_NAME,
        charset="utf8mb4",
        autocommit=True,
    )


# -----------------------------
# 로그인 처리 (student / professor 테이블 사용)
# -----------------------------
def handle_login(parts):
    """
    parts: ["LOGIN", user_id, pw]
    student / professor 테이블에서 아이디+비번을 확인한다.
    """
    if len(parts) != 3:
        return "ERR|BAD_REQUEST\n"

    _, user_id, pw = parts

    conn = get_db_connection()
    try:
        with conn.cursor() as cur:
            # 1) student 테이블에서 찾기
            cur.execute(
                "SELECT name FROM student WHERE student_id = %s AND password = %s",
                (user_id, pw),
            )
            row = cur.fetchone()
            if row:
                display_name = row[0]
                print(f"[LOGIN] student {user_id} ({display_name})")
                return f"OK|STUDENT|{display_name}\n"

            # 2) professor 테이블에서 찾기
            cur.execute(
                "SELECT name FROM professor WHERE professor_id = %s AND password = %s",
                (user_id, pw),
            )
            row = cur.fetchone()
            if row:
                display_name = row[0]
                print(f"[LOGIN] professor {user_id} ({display_name})")
                return f"OK|TEACHER|{display_name}\n"

        # 둘 다 없으면
        return "ERR|INVALID_CREDENTIALS\n"

    except Exception as e:
        print(f"[ERROR][LOGIN] DB error: {e}")
        return "ERR|SERVER_ERROR\n"
    finally:
        conn.close()


# -----------------------------
# 채팅 (현재는 메모리 방식 그대로 유지)
# 나중에 필요하면 chat 테이블과 연동 가능
# -----------------------------
MESSAGES = []  # 단순 데모용

def handle_chat_post(parts):
    """
    parts: ["CHAT_POST", from_id, to_id, message]
    지금은 메모리 리스트에만 저장.
    """
    if len(parts) != 4:
        return "ERR|BAD_REQUEST\n"

    _, from_id, to_id, message = parts
    MESSAGES.append((from_id, to_id, message))
    print(f"[CHAT] {from_id} -> {to_id}: {message}")
    return "OK\n"


def handle_chat_list(conn, parts):
    """
    parts: ["CHAT_LIST", userA, userB]
    응답은 여러 줄:
      MSG|from|to|message
      ...
      END
    """
    if len(parts) != 3:
        resp = "ERR|BAD_REQUEST\n"
        conn.sendall(resp.encode("utf-8"))
        return

    _, user_a, user_b = parts

    lines = []
    for (from_id, to_id, msg) in MESSAGES:
        if ((from_id == user_a and to_id == user_b) or
                (from_id == user_b and to_id == user_a)):
            lines.append(f"MSG|{from_id}|{to_id}|{msg}\n")

    lines.append("END\n")
    conn.sendall("".join(lines).encode("utf-8"))


# -----------------------------
# 과제 목록: task_detail 테이블 사용
# -----------------------------
def handle_assign_list(conn, parts):
    """
    parts: ["ASSIGN_LIST", userId]
    응답:
      ASSIGN|id|title|due|summary
      ...
      END

    userId는 지금은 특별히 쓰지 않고 과제 전체를 보여준다.
    """
    if len(parts) != 2:
        resp = "ERR|BAD_REQUEST\n"
        conn.sendall(resp.encode("utf-8"))
        return

    try:
        db = get_db_connection()
        with db.cursor() as cur:
            cur.execute(
                """
                SELECT task_id, title, end_date, content
                FROM task_detail
                ORDER BY end_date ASC, task_id ASC
                """
            )
            rows = cur.fetchall()

        lines = []
        for task_id, title, end_date, content in rows:
            # end_date는 DATE 타입이라고 가정 → 문자열로 변환
            due_str = end_date.strftime("%Y-%m-%d") if end_date else ""
            summary_src = content or ""
            summary_src = summary_src.replace("\n", " ").replace("\r", " ")
            if len(summary_src) > 40:
                summary = summary_src[:40] + "..."
            else:
                summary = summary_src

            lines.append(
                f"ASSIGN|{task_id}|{title}|{due_str}|{summary}\n"
            )

        lines.append("END\n")
        conn.sendall("".join(lines).encode("utf-8"))

    except Exception as e:
        print(f"[ERROR][ASSIGN_LIST] DB error: {e}")
        resp = "ERR|SERVER_ERROR\n"
        conn.sendall(resp.encode("utf-8"))


# -----------------------------
# 과제 제출 기록 INSERT (task_submission)
# -----------------------------
def insert_submission_record(task_id, student_id, file_name, file_path):
    """
    task_submission 테이블에 제출 기록 INSERT
    """
    db = get_db_connection()
    try:
        with db.cursor() as cur:
            sql = """
                INSERT INTO task_submission
                    (task_id, student_id, file_name, file_path, submitted_at)
                VALUES (%s, %s, %s, %s, NOW())
            """
            cur.execute(sql, (task_id, student_id, file_name, file_path))
        print(f"[DB] Insert submission: task={task_id}, student={student_id}")
    finally:
        db.close()


# -----------------------------
# 과제 파일 제출: 파일 저장 + DB 기록
# -----------------------------
def handle_assign_submit_file(conn, reader, parts):
    """
    파일 제출 프로토콜 처리.
    parts: ["ASSIGN_SUBMIT_FILE", userId, assignmentId, filename, filesize]

    흐름:
      1) 헤더 파싱
      2) 서버가 "OK\n" 응답
      3) 클라이언트가 filesize 바이트만큼 raw bytes 전송
      4) 서버는 파일 저장 후 DB에 기록
      5) 서버가 "DONE\n" 응답
    """
    if len(parts) != 5:
        resp = "ERR|BAD_REQUEST\n"
        conn.sendall(resp.encode("utf-8"))
        return

    _, user_id, assignment_id, filename, filesize_str = parts

    # 파일 크기 검증
    try:
        file_size = int(filesize_str)
        if file_size < 0 or file_size > 50 * 1024 * 1024:  # 50MB 제한 예시
            raise ValueError("invalid file size")
    except Exception:
        resp = "ERR|INVALID_SIZE\n"
        conn.sendall(resp.encode("utf-8"))
        return

    # 클라이언트에 OK 보내고 본격적으로 파일 수신 시작
    conn.sendall(b"OK\n")

    # 파일 이름 정리 (디렉토리 traversal 방지)
    safe_name = os.path.basename(filename)
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    saved_name = f"{assignment_id}_{user_id}_{timestamp}_{safe_name}"
    saved_path = os.path.join(SUBMISSION_DIR, saved_name)
    relative_path = os.path.join("submissions", saved_name)

    print(
        f"[SUBMIT_FILE] user={user_id} assignment={assignment_id} "
        f"file={safe_name} size={file_size} path={relative_path}"
    )

    # 파일 바이트 수신
    remaining = file_size
    try:
        with open(saved_path, "wb") as f:
            while remaining > 0:
                chunk_size = 65536 if remaining >= 65536 else remaining
                chunk = reader.read(chunk_size)
                if not chunk:
                    # 클라이언트가 중간에 끊김
                    raise IOError("connection closed while receiving file")
                f.write(chunk)
                remaining -= len(chunk)

        # DB에 제출 기록 INSERT
        try:
            task_id_int = int(assignment_id)
        except ValueError:
            task_id_int = None

        if task_id_int is not None:
            insert_submission_record(
                task_id_int, user_id, safe_name, relative_path
            )
        else:
            print(f"[WARN] assignment_id({assignment_id})를 int로 변환하지 못함. DB 기록 생략.")

        conn.sendall(b"DONE\n")
        print(f"[SUBMIT_FILE] Saved and recorded: {relative_path}")

    except Exception as e:
        print(f"[ERROR][SUBMIT_FILE] {e}")
        try:
            conn.sendall(b"ERR|SUBMIT_FAILED\n")
        except Exception:
            pass


# -----------------------------
# 클라이언트 연결 처리
# -----------------------------
def handle_client(conn, addr):
    print(f"[INFO] Connected from {addr}")
    try:
        # makefile을 써서 readline() + read() 혼용
        reader = conn.makefile("rb")

        # 1줄(헤더) 읽기
        line_bytes = reader.readline()
        if not line_bytes:
            return

        line = line_bytes.decode("utf-8").strip()
        print(f"[RECV] {addr}: {line}")

        parts = line.split("|")
        cmd = parts[0] if parts else ""

        if cmd == "LOGIN":
            resp = handle_login(parts)
            print(f"[SEND] {addr}: {resp.strip()}")
            conn.sendall(resp.encode("utf-8"))

        elif cmd == "CHAT_POST":
            resp = handle_chat_post(parts)
            print(f"[SEND] {addr}: {resp.strip()}")
            conn.sendall(resp.encode("utf-8"))

        elif cmd == "CHAT_LIST":
            handle_chat_list(conn, parts)

        elif cmd == "ASSIGN_LIST":
            handle_assign_list(conn, parts)

        elif cmd == "ASSIGN_SUBMIT_FILE":
            handle_assign_submit_file(conn, reader, parts)

        else:
            resp = "ERR|UNKNOWN_COMMAND\n"
            print(f"[SEND] {addr}: {resp.strip()}")
            conn.sendall(resp.encode("utf-8"))

    except Exception as e:
        print(f"[ERROR] {addr}: {e}")
    finally:
        try:
            conn.close()
        except Exception:
            pass
        print(f"[INFO] Disconnected {addr}")


def main():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        s.bind((HOST, PORT))
        s.listen(5)
        print(f"[LISTEN] Server listening on {HOST or '0.0.0.0'}:{PORT}")

        while True:
            conn, addr = s.accept()
            t = threading.Thread(target=handle_client, args=(conn, addr), daemon=True)
            t.start()


if __name__ == "__main__":
    main()
