# lms_server.py
import os
import socket
import threading
from datetime import datetime, date
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.parse import urlparse

import mysql.connector
from mysql.connector import Error

HOST = ""          # 0.0.0.0
PORT = 5051        # 컨테이너 내부 TCP 포트 (docker -p 6000:5051 로 매핑)
HTTP_PORT = 8081   # 영상 스트리밍 HTTP 포트

# DB 설정 (MySQL 호스트는 host.docker.internal 로 가정)
DB_CONFIG = {
    "host": "host.docker.internal",
    "port": 3306,
    "user": "lmsuser",
    "password": "lms1234!",
    "database": "mini_lms",
}

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
SUBMISSION_DIR = os.path.join(BASE_DIR, "submissions")
VIDEO_DIR = os.path.join("/app", "videos")  # Docker 내부 경로 (호스트의 폴더와 -v로 연결)

os.makedirs(SUBMISSION_DIR, exist_ok=True)
os.makedirs(VIDEO_DIR, exist_ok=True)

# 간단한 유저 테이블 (id: (password, role, display_name))
USERS = {
    "student": ("1234", "STUDENT", "김민상"),
    "teacher": ("1234", "TEACHER", "박교수"),
}

# 아주 간단한 메모리 기반 채팅 저장소
# 각 원소: (from_id, to_id, message)
MESSAGES = []


# ---------------------------
# DB 연결 유틸
# ---------------------------
def get_connection():
    return mysql.connector.connect(**DB_CONFIG)


# ---------------------------
# 로그인
# ---------------------------
def handle_login(parts):
    """
    parts: ["LOGIN", user_id, pw]
    DB의 student / professor 테이블을 조회해서
    - 학생이면: OK|STUDENT|이름
    - 교수이면: OK|TEACHER|이름
    - 아니면:   ERR|INVALID_CREDENTIALS
    """
    if len(parts) != 3:
        return "ERR|BAD_REQUEST\n"

    _, user_id, pw = parts

    try:
        with get_connection() as c:
            cur = c.cursor()

            # 1) 학생 먼저 찾기
            cur.execute(
                """
                SELECT COALESCE(student_name, name) AS display_name
                FROM student
                WHERE student_id = %s
                  AND password   = %s
                """,
                (user_id, pw),
            )
            row = cur.fetchone()
            if row:
                display_name = row[0] or user_id
                print(f"[LOGIN] {user_id} STUDENT ({display_name})")
                return f"OK|STUDENT|{display_name}\n"

            # 2) 교수 찾기
            cur.execute(
                """
                SELECT name
                FROM professor
                WHERE professor_id = %s
                  AND password     = %s
                """,
                (user_id, pw),
            )
            row = cur.fetchone()
            if row:
                display_name = row[0] or user_id
                print(f"[LOGIN] {user_id} TEACHER ({display_name})")
                return f"OK|TEACHER|{display_name}\n"

    except Error as e:
        print(f"[ERROR][LOGIN] {e}")
        return "ERR|SERVER_ERROR\n"

    # 둘 다 못 찾으면
    return "ERR|INVALID_CREDENTIALS\n"


# ---------------------------
# 채팅
# ---------------------------
def handle_chat_post(parts):
    """
    parts: ["CHAT_POST", from_id, to_id, message]
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


# ---------------------------
# 과제 목록 (ASSIGN_LIST)
# task_detail: (task_id, week_id, start_date, end_date, title, content)
# ---------------------------
def handle_assign_list(conn, parts):
    """
    parts: ["ASSIGN_LIST", userId]
    응답:
      ASSIGN|id|title|due|summary
      ...
      END
    """
    if len(parts) != 2:
        resp = "ERR|BAD_REQUEST\n"
        conn.sendall(resp.encode("utf-8"))
        return

    user_id = parts[1]
    print(f"[ASSIGN_LIST] for {user_id}")

    try:
        with get_connection() as c:
            cur = c.cursor()
            # ★ task_detail을 직접 사용, week 테이블이 없어도 동작
            sql = """
                SELECT
                    td.task_id,
                    td.week_id,
                    td.start_date,
                    td.end_date,
                    td.title,
                    td.content
                FROM task_detail td
                ORDER BY td.task_id
            """
            cur.execute(sql)
            rows = cur.fetchall()
    except Error as e:
        print(f"[ERROR][ASSIGN_LIST] {e}")
        conn.sendall(f"ERR|DB_ERROR|{e}\n".encode("utf-8"))
        return

    lines = []
    for (task_id, week_id, start_date, end_date, title, content) in rows:
        # 주차 라벨
        label_week = f"{week_id}주차" if week_id is not None else "과제"

        # 제목 포맷: [1주차] N-Echo 구현
        title = title or ""
        full_title = f"[{label_week}] {title}" if title else label_week

        # 마감일 표시 문자열
        due_str = (
            end_date.strftime("%Y-%m-%d %H:%M")
            if hasattr(end_date, "strftime")
            else str(end_date)
        )

        # 요약은 content 첫 줄 50자
        content = content or ""
        summary = content.strip().splitlines()[0][:50] if content.strip() else ""

        lines.append(f"ASSIGN|{task_id}|{full_title}|{due_str}|{summary}\n")

    lines.append("END\n")
    conn.sendall("".join(lines).encode("utf-8"))


# ---------------------------
# 과제 파일 제출 (ASSIGN_SUBMIT_FILE)
# 헤더: ASSIGN_SUBMIT_FILE|userId|taskId|filename|filesize\n
# ---------------------------
def handle_assign_submit_file(conn, parts, first_line_raw, addr):
    if len(parts) != 5:
        resp = "ERR|BAD_REQUEST\n"
        conn.sendall(resp.encode("utf-8"))
        return

    _, user_id, task_id, filename, filesize_str = parts
    try:
        filesize = int(filesize_str)
    except ValueError:
        conn.sendall(b"ERR|BAD_FILESIZE\n")
        return

    print(f"[RECV FILE HEADER] user={user_id} task={task_id} file={filename} size={filesize}")

    conn.sendall(b"OK\n")

    safe_filename = filename.replace("/", "_").replace("\\", "_")
    ts = datetime.now().strftime("%Y%m%d_%H%M%S")
    stored_name = f"{task_id}_{user_id}_{ts}_{safe_filename}"
    stored_path = os.path.join(SUBMISSION_DIR, stored_name)

    remaining = filesize
    try:
        with open(stored_path, "wb") as f:
            while remaining > 0:
                chunk = conn.recv(min(64 * 1024, remaining))
                if not chunk:
                    raise IOError("연결이 끊어졌습니다(파일 수신 중).")
                f.write(chunk)
                remaining -= len(chunk)
    except Exception as e:
        print(f"[ERROR][SUBMIT_FILE] {addr}: {e}")
        try:
            conn.sendall(b"ERR|FILE_RECV_ERROR\n")
        except Exception:
            pass
        return

    print(f"[SUBMIT_FILE] user={user_id} assignment={task_id} file={filename} size={filesize} path={stored_path}")

    rel_name = os.path.basename(stored_name)
    try:
        with get_connection() as c:
            cur = c.cursor()
            # file_name 컬럼까지 포함해서 INSERT
            sql = """
                INSERT INTO task_submission(task_id, student_id, submitted_at, file_name, file_path)
                VALUES (%s, %s, NOW(), %s, %s)
            """
            cur.execute(sql, (task_id, user_id, safe_filename, rel_name))
            c.commit()
        print(f"[DB] Insert submission: task={task_id}, student={user_id}")
    except Error as e:
        print(f"[ERROR][SUBMIT_FILE][DB] {e}")
        try:
            conn.sendall(f"ERR|DB_ERROR|{e}\n".encode("utf-8"))
        except Exception:
            pass
        return

    try:
        conn.sendall(b"DONE\n")
    except Exception:
        pass
    print(f"[SUBMIT_FILE] Saved and recorded: {stored_path}")


# ---------------------------
# 과제 제출 현황 조회 (ASSIGN_SUBMISSION_LIST)
# 응답: SUBMIT|studentId|studentName|submittedAt|filePath
# ---------------------------
def handle_assign_submission_list(conn, parts):
    if len(parts) != 3:
        conn.sendall(b"ERR|BAD_REQUEST\n")
        return
    _, user_id, task_id = parts

    rows = []
    try:
        with get_connection() as c:
            cur = c.cursor()
            # 1차 시도: student_name 컬럼이 있다고 가정
            try:
                sql = """
                    SELECT ts.student_id,
                           COALESCE(s.student_name, ''),
                           ts.submitted_at,
                           ts.file_path
                    FROM task_submission ts
                    LEFT JOIN student s ON ts.student_id = s.student_id
                    WHERE ts.task_id = %s
                    ORDER BY ts.submitted_at DESC
                """
                cur.execute(sql, (task_id,))
                rows = cur.fetchall()
            except Error as e1:
                print(f"[WARN][ASSIGN_SUBMISSION_LIST] {e1} -> fallback without student_name")
                # 2차 시도: 이름 컬럼이 없을 때, student_id만 사용
                sql = """
                    SELECT ts.student_id,
                           '',
                           ts.submitted_at,
                           ts.file_path
                    FROM task_submission ts
                    WHERE ts.task_id = %s
                    ORDER BY ts.submitted_at DESC
                """
                cur.execute(sql, (task_id,))
                rows = cur.fetchall()
    except Error as e:
        print(f"[ERROR][ASSIGN_SUBMISSION_LIST] {e}")
        conn.sendall(f"ERR|DB_ERROR|{e}\n".encode("utf-8"))
        return

    lines = []
    for student_id, student_name, submitted_at, file_path in rows:
        ts_str = (
            submitted_at.strftime("%Y-%m-%d %H:%M:%S")
            if hasattr(submitted_at, "strftime")
            else str(submitted_at)
        )
        student_name = student_name or ""
        file_path = file_path or ""
        lines.append(f"SUBMIT|{student_id}|{student_name}|{ts_str}|{file_path}\n")
    lines.append("END\n")
    conn.sendall("".join(lines).encode("utf-8"))


# ---------------------------
# 제출 파일 다운로드 (ASSIGN_DOWNLOAD_FILE)
# 요청: ASSIGN_DOWNLOAD_FILE|userId|filePath
# 응답: OK|filesize\n + raw bytes
# ---------------------------
def handle_assign_download_file(conn, parts):
    # 요청: ASSIGN_DOWNLOAD_FILE|userId|filePath
    if len(parts) != 3:
        conn.sendall(b"ERR|BAD_REQUEST\n")
        return

    _, user_id, file_path = parts

    # 경로 탈출 방지 + DB에 전체 경로가 들어있든, 파일명만 들어있든
    # => 항상 파일명(basename)만 사용
    safe = file_path.replace("..", "")
    safe = os.path.basename(safe)
    full_path = os.path.join(SUBMISSION_DIR, safe)

    if not os.path.exists(full_path) or not os.path.isfile(full_path):
        print(f"[WARN][ASSIGN_DOWNLOAD_FILE] not found: {full_path}")
        try:
            conn.sendall(b"ERR|NOT_FOUND\n")
        except Exception:
            pass
        return

    size = os.path.getsize(full_path)
    header = f"OK|{size}\n"
    try:
        conn.sendall(header.encode("utf-8"))
        with open(full_path, "rb") as f:
            while True:
                chunk = f.read(64 * 1024)
                if not chunk:
                    break
                conn.sendall(chunk)
    except Exception as e:
        print(f"[ERROR][ASSIGN_DOWNLOAD_FILE] {e}")
        try:
            conn.sendall(b"ERR|TRANSFER_ERROR\n")
        except Exception:
            pass


# ---------------------------
# 공지 목록 (NOTICE_LIST)
# notice: (notice_id, professor_id, date, content, attachment_file)
# 응답: NOTICE|id|title|content|createdAt
# ---------------------------
def handle_notice_list(conn, parts):
    if len(parts) != 2:
        conn.sendall(b"ERR|BAD_REQUEST\n")
        return

    try:
        with get_connection() as c:
            cur = c.cursor()
            sql = """
                SELECT notice_id, content, date
                FROM notice
                ORDER BY date DESC, notice_id DESC
            """
            cur.execute(sql)
            rows = cur.fetchall()
    except Error as e:
        print(f"[ERROR][NOTICE_LIST] {e}")
        try:
            conn.sendall(f"ERR|DB_ERROR|{e}\n".encode("utf-8"))
        except Exception:
            pass
        return

    lines = []
    for nid, content, date_val in rows:
        content = content or ""

        # 제목: content의 첫 줄 일부만 사용
        first_line = content.strip().splitlines()[0] if content.strip() else ""
        title = first_line[:20] + ("..." if len(first_line) > 20 else "")
        if not title:
            title = f"공지 {nid}"

        # createdAt 표시용 문자열 (DATE 타입이므로 yyyy-MM-dd 정도로)
        if hasattr(date_val, "strftime"):
            ts_str = date_val.strftime("%Y-%m-%d")
        else:
            ts_str = str(date_val)

        # 프로토콜: NOTICE|id|title|content|createdAt
        lines.append(f"NOTICE|{nid}|{title}|{content}|{ts_str}\n")

    lines.append("END\n")
    try:
        conn.sendall("".join(lines).encode("utf-8"))
    except Exception as e:
        print(f"[ERROR][NOTICE_LIST][SEND] {e}")


# ---------------------------
# 공지 생성 (NOTICE_CREATE)
# 요청: NOTICE_CREATE|userId|content
#   - content 안에 "제목\n\n본문" 형식으로 들어있음
# ---------------------------
# 공지 생성 (NOTICE_CREATE)
# 요청: NOTICE_CREATE|userId|content
def handle_notice_create(conn, parts):
    if len(parts) != 3:
        conn.sendall(b"ERR|BAD_REQUEST\n")
        return

    _, user_id, content = parts

    try:
        with get_connection() as c:
            cur = c.cursor()

            # ✅ date NOT NULL 이므로 CURDATE()로 채워 넣기
            sql = """
                INSERT INTO notice (professor_id, date, content, attachment_file)
                VALUES (%s, CURDATE(), %s, NULL)
            """
            cur.execute(sql, (user_id, content))
            c.commit()

        conn.sendall(b"OK\n")
    except Error as e:
        print(f"[ERROR][NOTICE_CREATE] {e}")
        try:
            conn.sendall(f"ERR|DB_ERROR|{e}\n".encode("utf-8"))
        except Exception:
            pass



# ---------------------------
# 공지 수정 (NOTICE_UPDATE)
# 요청: NOTICE_UPDATE|userId|noticeId|content
#   - content 안에 "제목\n\n본문" 형식 유지
# ---------------------------
def handle_notice_update(conn, parts):
    if len(parts) != 4:
        conn.sendall(b"ERR|BAD_REQUEST\n")
        return

    _, user_id, notice_id, content = parts

    try:
        with get_connection() as c:
            cur = c.cursor()
            sql = """
                UPDATE notice
                SET content = %s
                WHERE notice_id = %s
            """
            cur.execute(sql, (content, notice_id))
            c.commit()

        conn.sendall(b"OK\n")
    except Error as e:
        print(f"[ERROR][NOTICE_UPDATE] {e}")
        try:
            conn.sendall(f"ERR|DB_ERROR|{e}\n".encode("utf-8"))
        except Exception:
            pass


# ---------------------------
# 공지 삭제 (NOTICE_DELETE)
# 요청: NOTICE_DELETE|userId|noticeId
# ---------------------------
def handle_notice_delete(conn, parts):
    if len(parts) != 3:
        conn.sendall(b"ERR|BAD_REQUEST\n")
        return

    _, user_id, notice_id = parts

    try:
        with get_connection() as c:
            cur = c.cursor()
            sql = "DELETE FROM notice WHERE notice_id = %s"
            cur.execute(sql, (notice_id,))
            c.commit()

        conn.sendall(b"OK\n")
    except Error as e:
        print(f"[ERROR][NOTICE_DELETE] {e}")
        try:
            conn.sendall(f"ERR|DB_ERROR|{e}\n".encode("utf-8"))
        except Exception:
            pass



# ---------------------------
# 영상 목록 (VIDEO_LIST)
# video: (video_id, week_id, lecture_id, video_path)
# week:  (week_id, week, lecture_id)
# 응답: VIDEO|id|title|weekLabel|progress
#       progress: 내가 본 경우 100, 아니면 0 (교수는 항상 0)
# ---------------------------
def handle_video_list(conn, parts):
    if len(parts) != 2:
        conn.sendall(b"ERR|BAD_REQUEST\n")
        return

    _, user_id = parts  # 학생/교수 아이디

    try:
        with get_connection() as c:
            cur = c.cursor()
            sql = """
                SELECT
                    v.video_id,
                    COALESCE(CONCAT(w.week, '주차'), '주차 미지정') AS week_label,
                    v.video_path,
                    wt.is_attended
                FROM video v
                LEFT JOIN week w
                       ON v.week_id = w.week_id
                LEFT JOIN whether wt
                       ON wt.video_id  = v.video_id
                      AND wt.student_id = %s
                ORDER BY v.video_id
            """
            cur.execute(sql, (user_id,))
            rows = cur.fetchall()
    except Error as e:
        print(f"[ERROR][VIDEO_LIST] {e}")
        conn.sendall(f"ERR|DB_ERROR|{e}\n".encode("utf-8"))
        return

    lines = []
    for vid, week_label, video_path, is_attended in rows:
        week_label = week_label or ""
        title = f"[{week_label}] 강의 영상"
        progress = 100 if is_attended else 0  # 본 적 있으면 100%, 아니면 0%
        lines.append(f"VIDEO|{vid}|{title}|{week_label}|{progress}\n")

    lines.append("END\n")
    conn.sendall("".join(lines).encode("utf-8"))

# ---------------------------
# 영상 시청 기록 (VIDEO_WATCH)
# 요청: VIDEO_WATCH|studentId|videoId
# ---------------------------
def handle_video_watch(conn, parts):
    if len(parts) != 3:
        conn.sendall(b"ERR|BAD_REQUEST\n")
        return

    _, student_id, video_id = parts
    try:
        with get_connection() as c:
            cur = c.cursor()

            # 이미 기록이 있으면 is_attended=1, end_date=오늘 로 갱신
            cur.execute(
                """
                UPDATE whether
                   SET is_attended = 1,
                       end_date    = CURDATE()
                 WHERE video_id   = %s
                   AND student_id = %s
                """,
                (video_id, student_id),
            )
            if cur.rowcount == 0:
                # 없으면 새로 INSERT
                cur.execute(
                    """
                    INSERT INTO whether(video_id, student_id, is_attended, start_date, end_date)
                    VALUES (%s, %s, 1, CURDATE(), CURDATE())
                    """,
                    (video_id, student_id),
                )
            c.commit()
        conn.sendall(b"OK\n")
    except Error as e:
        print(f"[ERROR][VIDEO_WATCH] {e}")
        conn.sendall(f"ERR|DB_ERROR|{e}\n".encode("utf-8"))

# ---------------------------
# 영상별 학생 재생률 조회 (VIDEO_PROGRESS_LIST)
# 요청: VIDEO_PROGRESS_LIST|userId|videoId   (userId는 지금은 그냥 로그용)
# 응답:
#   PROG|studentId|studentName|percent|status
#   ...
#   END
# percent: 0 또는 100
# status:  DONE / NOT_YET
# ---------------------------
def handle_video_progress_list(conn, parts):
    if len(parts) != 3:
        conn.sendall(b"ERR|BAD_REQUEST\n")
        return

    _, user_id, video_id = parts  # user_id는 현재는 사용 X

    try:
        with get_connection() as c:
            cur = c.cursor()
            sql = """
                SELECT
                    s.student_id,
                    COALESCE(s.student_name, s.name) AS student_name,
                    wt.is_attended
                FROM student s
                LEFT JOIN whether wt
                       ON wt.student_id = s.student_id
                      AND wt.video_id   = %s
                ORDER BY s.student_id
            """
            cur.execute(sql, (video_id,))
            rows = cur.fetchall()
    except Error as e:
        print(f"[ERROR][VIDEO_PROGRESS_LIST] {e}")
        conn.sendall(f"ERR|DB_ERROR|{e}\n".encode("utf-8"))
        return

    lines = []
    for sid, sname, is_attended in rows:
        is_attended = int(is_attended or 0)
        percent = 100 if is_attended == 1 else 0
        status = "DONE" if percent == 100 else "NOT_YET"
        sname = sname or ""
        lines.append(f"PROG|{sid}|{sname}|{percent}|{status}\n")

    lines.append("END\n")
    conn.sendall("".join(lines).encode("utf-8"))

# 영상 등록 (기존: 파일명만 DB에 등록, 지금은 파일 업로드용 별도 핸들러도 추가)
def handle_video_create(conn, parts):
    # VIDEO_CREATE|userId|weekId|filename
    if len(parts) != 4:
        conn.sendall(b"ERR|BAD_REQUEST\n")
        return
    _, user_id, week_id, filename = parts
    try:
        with get_connection() as c:
            cur = c.cursor()
            sql = "INSERT INTO video(week_id, lecture_id, video_path) VALUES (%s, %s, %s)"
            cur.execute(sql, (week_id, 1, filename))
            c.commit()
        conn.sendall(b"OK\n")
    except Error as e:
        print(f"[ERROR][VIDEO_CREATE] {e}")
        conn.sendall(f"ERR|DB_ERROR|{e}\n".encode("utf-8"))


def handle_video_delete(conn, parts):
    # VIDEO_DELETE|userId|videoId
    if len(parts) != 3:
        conn.sendall(b"ERR|BAD_REQUEST\n")
        return
    _, user_id, video_id = parts
    try:
        with get_connection() as c:
            cur = c.cursor()
            cur.execute("SELECT video_path FROM video WHERE video_id=%s", (video_id,))
            row = cur.fetchone()
            if row:
                file_name = row[0]
                full_path = os.path.join(VIDEO_DIR, file_name)
                try:
                    if os.path.exists(full_path):
                        os.remove(full_path)
                except Exception as e:
                    print(f"[WARN][VIDEO_DELETE] 파일 삭제 실패: {e}")

            cur.execute("DELETE FROM video WHERE video_id=%s", (video_id,))
            c.commit()
        conn.sendall(b"OK\n")
    except Error as e:
        print(f"[ERROR][VIDEO_DELETE] {e}")
        conn.sendall(f"ERR|DB_ERROR|{e}\n".encode("utf-8"))


# 영상 파일 업로드 (VIDEO_UPLOAD_FILE)
# 헤더: VIDEO_UPLOAD_FILE|userId|weekId|filename|filesize\n
def handle_video_upload_file(conn, parts, first_line_raw, addr):
    if len(parts) != 5:
        conn.sendall(b"ERR|BAD_REQUEST\n")
        return
    _, user_id, week_id_str, filename, filesize_str = parts
    try:
        week_id = int(week_id_str)
    except ValueError:
        week_id = 1
    try:
        filesize = int(filesize_str)
    except ValueError:
        conn.sendall(b"ERR|BAD_FILESIZE\n")
        return

    print(f"[VIDEO_UPLOAD] header user={user_id} week={week_id} file={filename} size={filesize}")

    conn.sendall(b"OK\n")

    safe_filename = filename.replace("/", "_").replace("\\", "_")
    ts = datetime.now().strftime("%Y%m%d_%H%M%S")
    stored_name = f"{week_id}_{user_id}_{ts}_{safe_filename}"
    stored_path = os.path.join(VIDEO_DIR, stored_name)

    remaining = filesize
    try:
        with open(stored_path, "wb") as f:
            while remaining > 0:
                chunk = conn.recv(min(64 * 1024, remaining))
                if not chunk:
                    raise IOError("연결이 끊어졌습니다(영상 수신 중).")
                f.write(chunk)
                remaining -= len(chunk)
    except Exception as e:
        print(f"[ERROR][VIDEO_UPLOAD_FILE] {addr}: {e}")
        try:
            conn.sendall(b"ERR|FILE_RECV_ERROR\n")
        except Exception:
            pass
        return

    print(f"[VIDEO_UPLOAD_FILE] Saved file: {stored_path}")

    try:
        with get_connection() as c:
            cur = c.cursor()
            sql = "INSERT INTO video(week_id, lecture_id, video_path) VALUES (%s, %s, %s)"
            cur.execute(sql, (week_id, 1, stored_name))
            c.commit()
        print(f"[DB] Insert video: week={week_id}, path={stored_name}")
    except Error as e:
        print(f"[ERROR][VIDEO_UPLOAD_FILE][DB] {e}")
        try:
            conn.sendall(f"ERR|DB_ERROR|{e}\n".encode("utf-8"))
        except Exception:
            pass
        return

    try:
        conn.sendall(b"DONE\n")
    except Exception:
        pass


# ---------------------------
# 과제 CRUD (교사용)
# ---------------------------
def handle_assign_create(conn, parts):
    # 기본: ASSIGN_CREATE|userId|title|summary
    if len(parts) < 4:
        conn.sendall(b"ERR|BAD_REQUEST\n")
        return

    _, user_id, title = parts[0:3]
    # summary 안에 '|'가 포함될 수 있으므로 뒤에 것들을 합침
    summary = "|".join(parts[3:]) if len(parts) > 3 else ""

    today = date.today()
    end_date = today  # 심플하게 오늘로

    try:
        with get_connection() as c:
            cur = c.cursor()
            sql = """
                INSERT INTO task_detail(week_id, start_date, end_date, title, content)
                VALUES (%s, %s, %s, %s, %s)
            """
            cur.execute(sql, (1, today, end_date, title, summary))
            c.commit()
        conn.sendall(b"OK\n")
    except Error as e:
        print(f"[ERROR][ASSIGN_CREATE] {e}")
        conn.sendall(f"ERR|DB_ERROR|{e}\n".encode("utf-8"))


def handle_assign_update(conn, parts):
    # ASSIGN_UPDATE|userId|taskId|title|summary
    if len(parts) < 5:
        conn.sendall(b"ERR|BAD_REQUEST\n")
        return
    _, user_id, task_id, title = parts[0:4]
    summary = "|".join(parts[4:]) if len(parts) > 4 else ""

    try:
        with get_connection() as c:
            cur = c.cursor()
            sql = """
                UPDATE task_detail
                SET title=%s, content=%s
                WHERE task_id=%s
            """
            cur.execute(sql, (title, summary, task_id))
            c.commit()
        conn.sendall(b"OK\n")
    except Error as e:
        print(f"[ERROR][ASSIGN_UPDATE] {e}")
        conn.sendall(f"ERR|DB_ERROR|{e}\n".encode("utf-8"))


def handle_assign_delete(conn, parts):
    # ASSIGN_DELETE|userId|taskId
    if len(parts) != 3:
        conn.sendall(b"ERR|BAD_REQUEST\n")
        return
    _, user_id, task_id = parts
    try:
        with get_connection() as c:
            cur = c.cursor()
            sql = "DELETE FROM task_detail WHERE task_id=%s"
            cur.execute(sql, (task_id,))
            c.commit()
        conn.sendall(b"OK\n")
    except Error as e:
        print(f"[ERROR][ASSIGN_DELETE] {e}")
        conn.sendall(f"ERR|DB_ERROR|{e}\n".encode("utf-8"))


# ---------------------------
# 학생 목록 조회 (STUDENT_LIST) - 교사용
# 응답: STUDENT|id|name|contact
# ---------------------------
def handle_student_list(conn, parts):
    if len(parts) != 2:
        conn.sendall(b"ERR|BAD_REQUEST\n")
        return

    rows = []
    mode = "id_only"
    try:
        with get_connection() as c:
            cur = c.cursor()
            try:
                sql = """
                    SELECT student_id, student_name, contact
                    FROM student
                    ORDER BY student_id
                """
                cur.execute(sql)
                rows = cur.fetchall()
                mode = "full"
            except Error as e1:
                print(f"[WARN][STUDENT_LIST] {e1} -> fallback id-only")
                sql = """
                    SELECT student_id
                    FROM student
                    ORDER BY student_id
                """
                cur.execute(sql)
                rows = cur.fetchall()
                mode = "id_only"
    except Error as e:
        print(f"[ERROR][STUDENT_LIST] {e}")
        conn.sendall(f"ERR|DB_ERROR|{e}\n".encode("utf-8"))
        return

    lines = []
    if mode == "full":
        for sid, name, contact in rows:
            sid = sid or ""
            name = name or ""
            contact = contact or ""
            lines.append(f"STUDENT|{sid}|{name}|{contact}\n")
    else:
        for (sid,) in rows:
            sid = sid or ""
            lines.append(f"STUDENT|{sid}||\n")

    lines.append("END\n")
    conn.sendall("".join(lines).encode("utf-8"))


# ---------------------------
# HTTP 영상 스트리밍 핸들러 (Range 지원)
# ---------------------------
class VideoRequestHandler(BaseHTTPRequestHandler):
    def log_message(self, format, *args):
        msg = "%s - %s" % (self.address_string(), format % args)
        print(f"[HTTP] {msg}")

    def do_HEAD(self):
        parsed = urlparse(self.path)
        if parsed.path.startswith("/video/"):
            video_id = parsed.path[len("/video/"):]
            file_path = self._get_video_file_path(video_id)
            if not file_path:
                self.send_response(404)
                self.end_headers()
                return
            try:
                file_size = os.path.getsize(file_path)
            except OSError:
                self.send_response(404)
                self.end_headers()
                return
            self.send_response(200)
            self.send_header("Content-Type", "video/mp4")
            self.send_header("Accept-Ranges", "bytes")
            self.send_header("Content-Length", str(file_size))
            self.end_headers()
        else:
            self.send_response(404)
            self.end_headers()

    def do_GET(self):
        parsed = urlparse(self.path)
        if not parsed.path.startswith("/video/"):
            self.send_response(404)
            self.end_headers()
            return

        video_id = parsed.path[len("/video/"):]
        file_path = self._get_video_file_path(video_id)
        if not file_path:
            self.send_response(404)
            self.end_headers()
            return

        try:
            file_size = os.path.getsize(file_path)
        except OSError:
            self.send_response(404)
            self.end_headers()
            return

        range_header = self.headers.get("Range", None)
        if range_header:
            try:
                bytes_unit, range_spec = range_header.split("=", 1)
                start_str, end_str = range_spec.split("-", 1)
                start = int(start_str) if start_str else 0
                end = int(end_str) if end_str else file_size - 1
                if end >= file_size:
                    end = file_size - 1
                chunk_size = end - start + 1

                self.send_response(206)
                self.send_header("Content-Type", "video/mp4")
                self.send_header("Accept-Ranges", "bytes")
                self.send_header("Content-Range", f"bytes {start}-{end}/{file_size}")
                self.send_header("Content-Length", str(chunk_size))
                self.end_headers()

                with open(file_path, "rb") as f:
                    f.seek(start)
                    remaining = chunk_size
                    while remaining > 0:
                        chunk = f.read(min(64 * 1024, remaining))
                        if not chunk:
                            break
                        self.wfile.write(chunk)
                        remaining -= len(chunk)
            except Exception as e:
                print(f"[ERROR][HTTP][STREAM] {e}")
        else:
            self.send_response(200)
            self.send_header("Content-Type", "video/mp4")
            self.send_header("Accept-Ranges", "bytes")
            self.send_header("Content-Length", str(file_size))
            self.end_headers()
            try:
                with open(file_path, "rb") as f:
                    while True:
                        chunk = f.read(64 * 1024)
                        if not chunk:
                            break
                        self.wfile.write(chunk)
            except Exception as e:
                print(f"[ERROR][HTTP][STREAM] {e}")

    def _get_video_file_path(self, video_id):
        try:
            with get_connection() as c:
                cur = c.cursor()
                cur.execute("SELECT video_path FROM video WHERE video_id=%s", (video_id,))
                row = cur.fetchone()
                if not row:
                    return None
                filename = row[0]
        except Error as e:
            print(f"[ERROR][HTTP][DB] {e}")
            return None

        full_path = os.path.join(VIDEO_DIR, filename)
        if not os.path.exists(full_path):
            print(f"[HTTP] file not found: {full_path}")
            return None
        return full_path


def run_http_server():
    server_address = ("", HTTP_PORT)
    httpd = HTTPServer(server_address, VideoRequestHandler)
    print(f"[HTTP] Serving video on 0.0.0.0:{HTTP_PORT}")
    httpd.serve_forever()


# ---------------------------
# TCP 메인 핸들러
# ---------------------------
def handle_client(conn, addr):
    print(f"[INFO] Connected from {addr}")
    try:
        header_bytes = b""
        while True:
            b = conn.recv(1)
            if not b:
                break
            header_bytes += b
            if b == b"\n":
                break

        if not header_bytes:
            return

        line_raw = header_bytes.decode("utf-8", errors="ignore")
        line = line_raw.strip()
        print(f"[RECV] {addr}: {line}")

        parts = line.split("|")
        cmd = parts[0] if parts else ""

        if cmd == "LOGIN":
            resp = handle_login(parts)
            conn.sendall(resp.encode("utf-8"))

        elif cmd == "CHAT_POST":
            resp = handle_chat_post(parts)
            conn.sendall(resp.encode("utf-8"))

        elif cmd == "CHAT_LIST":
            handle_chat_list(conn, parts)

        elif cmd == "ASSIGN_LIST":
            handle_assign_list(conn, parts)

        elif cmd == "ASSIGN_SUBMIT_FILE":
            handle_assign_submit_file(conn, parts, line_raw, addr)

        elif cmd == "ASSIGN_SUBMISSION_LIST":
            handle_assign_submission_list(conn, parts)

        elif cmd == "ASSIGN_DOWNLOAD_FILE":
            handle_assign_download_file(conn, parts)

        elif cmd == "ASSIGN_CREATE":
            handle_assign_create(conn, parts)

        elif cmd == "ASSIGN_UPDATE":
            handle_assign_update(conn, parts)

        elif cmd == "ASSIGN_DELETE":
            handle_assign_delete(conn, parts)

        elif cmd == "NOTICE_LIST":
            handle_notice_list(conn, parts)

        elif cmd == "NOTICE_CREATE":
            handle_notice_create(conn, parts)

        elif cmd == "NOTICE_UPDATE":
            handle_notice_update(conn, parts)

        elif cmd == "NOTICE_DELETE":
            handle_notice_delete(conn, parts)

        elif cmd == "VIDEO_LIST":
            handle_video_list(conn, parts)

        elif cmd == "VIDEO_CREATE":
            handle_video_create(conn, parts)

        elif cmd == "VIDEO_DELETE":
            handle_video_delete(conn, parts)
            
        elif cmd == "VIDEO_WATCH":
            handle_video_watch(conn, parts)

        elif cmd == "VIDEO_PROGRESS_LIST":
            handle_video_progress_list(conn, parts)

        elif cmd == "VIDEO_UPLOAD_FILE":
            handle_video_upload_file(conn, parts, line_raw, addr)

        elif cmd == "STUDENT_LIST":
            handle_student_list(conn, parts)

        else:
            resp = "ERR|UNKNOWN_COMMAND\n"
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
    t_http = threading.Thread(target=run_http_server, daemon=True)
    t_http.start()

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
