package lms;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 서버와 TCP로 통신하는 헬퍼 클래스.
 * 화면(App.java)은 이 메서드들을 호출해서 문자열 기반 프로토콜만 주고받는다.
 */
public class LmsClient {

    // Docker 컨테이너 포트 매핑 기준
    // docker run ... -p 6000:5051 ...
    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT = 6000;

    // ==========================
    //  데이터 모델
    // ==========================

    public static class LoginResult {
        public final boolean success;
        public final String role;         // "STUDENT" / "TEACHER"
        public final String displayName;  // ex) "김민상"
        public final String errorCode;    // ex) "INVALID_CREDENTIALS", null if success

        public LoginResult(boolean success, String role, String displayName, String errorCode) {
            this.success = success;
            this.role = role;
            this.displayName = displayName;
            this.errorCode = errorCode;
        }
    }

    public static class Assignment {
        public final String id;
        public final String title;
        public final String due;
        public final String summary;

        public Assignment(String id, String title, String due, String summary) {
            this.id = id;
            this.title = title;
            this.due = due;
            this.summary = summary;
        }
    }

    public static class NoticeItem {
        public final String id;
        public final String title;
        public final String content;
        public final String createdAt;

        public NoticeItem(String id, String title, String content, String createdAt) {
            this.id = id;
            this.title = title;
            this.content = content;
            this.createdAt = createdAt;
        }
    }

    public static class VideoItem {
    public final String id;
    public final String title;
    public final String weekLabel;

    public VideoItem(String id, String title, String weekLabel) {
        this.id = id;
        this.title = title;
        this.weekLabel = weekLabel;
    }
}

    public static class SubmissionInfo {
        public final String studentId;
        public final String studentName;
        public final String submittedAt;
        public final String filePath;

        public SubmissionInfo(String studentId, String studentName, String submittedAt, String filePath) {
            this.studentId = studentId;
            this.studentName = studentName;
            this.submittedAt = submittedAt;
            this.filePath = filePath;
        }
    }

    public static class StudentInfo {
        public final String studentId;
        public final String name;
        public final String contact;

        public StudentInfo(String studentId, String name, String contact) {
            this.studentId = studentId;
            this.name = name;
            this.contact = contact;
        }
    }

    // ==========================
    //  로그인
    // ==========================

    public static LoginResult login(String userId, String password) throws IOException {
        try (
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
        ) {
            String requestLine = "LOGIN|" + userId + "|" + password + "\n";
            writer.write(requestLine);
            writer.flush();

            String responseLine = reader.readLine();
            if (responseLine == null) {
                throw new IOException("서버 응답이 없습니다.");
            }

            String[] parts = responseLine.split("\\|");
            if (parts.length >= 1 && "OK".equals(parts[0]) && parts.length == 3) {
                String role = parts[1];
                String displayName = parts[2];
                return new LoginResult(true, role, displayName, null);
            } else if (parts.length >= 2 && "ERR".equals(parts[0])) {
                String err = parts[1];
                return new LoginResult(false, null, null, err);
            } else {
                throw new IOException("알 수 없는 서버 응답: " + responseLine);
            }
        }
    }

    // ==========================
    //  채팅
    // ==========================

    // CHAT_POST
    public static boolean sendChat(String fromId, String toId, String message) throws IOException {
        try (
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
        ) {
            String requestLine = "CHAT_POST|" + fromId + "|" + toId + "|" + message + "\n";
            writer.write(requestLine);
            writer.flush();

            String responseLine = reader.readLine();
            if (responseLine == null) {
                throw new IOException("서버 응답이 없습니다.");
            }

            if ("OK".equals(responseLine)) {
                return true;
            } else if (responseLine.startsWith("ERR|")) {
                throw new IOException("채팅 전송 실패: " + responseLine);
            } else {
                throw new IOException("알 수 없는 서버 응답: " + responseLine);
            }
        }
    }

    // CHAT_LIST
    public static List<String> fetchChat(String userA, String userB) throws IOException {
        List<String> messages = new ArrayList<>();

        try (
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
        ) {
            String requestLine = "CHAT_LIST|" + userA + "|" + userB + "\n";
            writer.write(requestLine);
            writer.flush();

            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    throw new IOException("서버 응답이 도중에 끊어졌습니다.");
                }
                if ("END".equals(line)) {
                    break;
                }

                String[] parts = line.split("\\|", 4);
                if (parts.length == 4 && "MSG".equals(parts[0])) {
                    String fromId = parts[1];
                    String toId = parts[2];
                    String msg = parts[3];
                    messages.add(fromId + " → " + toId + ": " + msg);
                }
            }
        }

        return messages;
    }

    // ==========================
    //  과제 목록
    // ==========================

    public static List<Assignment> fetchAssignments(String userId) throws IOException {
        List<Assignment> list = new ArrayList<>();

        try (
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
        ) {
            String requestLine = "ASSIGN_LIST|" + userId + "\n";
            writer.write(requestLine);
            writer.flush();

            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    throw new IOException("서버 응답이 도중에 끊어졌습니다.");
                }
                if ("END".equals(line)) {
                    break;
                }

                // ASSIGN|id|title|due|summary
                String[] parts = line.split("\\|", 5);
                if (parts.length == 5 && "ASSIGN".equals(parts[0])) {
                    String id = parts[1];
                    String title = parts[2];
                    String due = parts[3];
                    String summary = parts[4];
                    list.add(new Assignment(id, title, due, summary));
                }
            }
        }

        return list;
    }

    // ==========================
    //  과제 파일 제출
    // ==========================

    public static boolean submitAssignmentFile(String userId, String assignmentId, File file) throws IOException {
        long fileSize = file.length();
        String fileName = file.getName();

        try (
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                OutputStream out = socket.getOutputStream();
                InputStream in = socket.getInputStream();
                FileInputStream fis = new FileInputStream(file)
        ) {
            String header = "ASSIGN_SUBMIT_FILE|" + userId + "|" + assignmentId + "|" +
                    fileName + "|" + fileSize + "\n";
            out.write(header.getBytes(StandardCharsets.UTF_8));
            out.flush();

            // OK 헤더 먼저
            String respHeader = readLine(in);
            if (respHeader == null) {
                throw new IOException("서버 응답이 없습니다.");
            }
            if (!respHeader.startsWith("OK")) {
                throw new IOException("과제 제출 실패: " + respHeader);
            }

            // 파일 바디 전송
            byte[] buf = new byte[8192];
            long remaining = fileSize;
            while (remaining > 0) {
                int read = fis.read(buf, 0, (int) Math.min(buf.length, remaining));
                if (read == -1) break;
                out.write(buf, 0, read);
                remaining -= read;
            }
            out.flush();

            // DONE 응답
            String done = readLine(in);
            if (done == null) {
                throw new IOException("서버 응답이 도중에 끊어졌습니다.");
            }
            if (!done.startsWith("DONE")) {
                throw new IOException("과제 제출 중 오류: " + done);
            }
            return true;
        }
    }

    // ==========================
    //  과제 제출 현황 / 다운로드
    // ==========================

    public static List<SubmissionInfo> fetchSubmissions(String userId, String taskId) throws IOException {
        List<SubmissionInfo> list = new ArrayList<>();

        try (
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
        ) {
            String line = "ASSIGN_SUBMISSION_LIST|" + userId + "|" + taskId + "\n";
            writer.write(line);
            writer.flush();

            while (true) {
                String resp = reader.readLine();
                if (resp == null) {
                    throw new IOException("서버 응답이 도중에 끊어졌습니다.");
                }
                if ("END".equals(resp)) break;

                // SUBMIT|studentId|studentName|submittedAt|filePath
                String[] parts = resp.split("\\|", 5);
                if (parts.length == 5 && "SUBMIT".equals(parts[0])) {
                    String studentId = parts[1];
                    String studentName = parts[2];
                    String submittedAt = parts[3];
                    String filePath = parts[4];
                    list.add(new SubmissionInfo(studentId, studentName, submittedAt, filePath));
                }
            }
        }

        return list;
    }

    public static void downloadSubmissionFile(String userId, String filePath, File destFile) throws IOException {
        try (
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                OutputStream out = socket.getOutputStream();
                InputStream in = socket.getInputStream()
        ) {
            String cmd = "ASSIGN_DOWNLOAD_FILE|" + userId + "|" + filePath + "\n";
            out.write(cmd.getBytes(StandardCharsets.UTF_8));
            out.flush();

            String headerLine = readLine(in);
            if (headerLine == null) {
                throw new IOException("서버 응답이 없습니다.");
            }
            if (headerLine.startsWith("ERR|")) {
                throw new IOException("파일 다운로드 실패: " + headerLine);
            }

            String[] parts = headerLine.split("\\|", 2);
            if (parts.length != 2 || !"OK".equals(parts[0])) {
                throw new IOException("알 수 없는 서버 응답: " + headerLine);
            }

            long size;
            try {
                size = Long.parseLong(parts[1]);
            } catch (NumberFormatException e) {
                throw new IOException("잘못된 파일 크기 응답: " + headerLine);
            }

            try (FileOutputStream fos = new FileOutputStream(destFile)) {
                byte[] buf = new byte[8192];
                long remaining = size;
                while (remaining > 0) {
                    int read = in.read(buf, 0, (int) Math.min(buf.length, remaining));
                    if (read == -1) {
                        throw new IOException("파일 수신 중 연결이 끊어졌습니다.");
                    }
                    fos.write(buf, 0, read);
                    remaining -= read;
                }
            }
        }
    }

    // ==========================
    //  공지
    // ==========================

    public static List<NoticeItem> fetchNotices(String userId) throws IOException {
        List<NoticeItem> list = new ArrayList<>();

        try (
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
        ) {
            String line = "NOTICE_LIST|" + userId + "\n";
            writer.write(line);
            writer.flush();

            while (true) {
                String resp = reader.readLine();
                if (resp == null) {
                    throw new IOException("서버 응답이 도중에 끊어졌습니다.");
                }
                if ("END".equals(resp)) break;

                // NOTICE|id|title|content|createdAt  (createdAt이 없으면 4개일 수도 있음)
                String[] parts = resp.split("\\|", 5);
                if (parts.length >= 3 && "NOTICE".equals(parts[0])) {
                    String id = parts[1];
                    String title = parts[2];
                    String content = (parts.length >= 4) ? parts[3] : "";
                    String createdAt = (parts.length >= 5) ? parts[4] : "";
                    list.add(new NoticeItem(id, title, content, createdAt));
                }
            }
        }

        return list;
    }

    public static void createNotice(String userId, String content) throws IOException {
        sendSimpleCommandWithBody("NOTICE_CREATE", new String[]{userId, content});
    }

    public static void updateNotice(String userId, String noticeId, String content) throws IOException {
        sendSimpleCommandWithBody("NOTICE_UPDATE", new String[]{userId, noticeId, content});
    }

    public static void deleteNotice(String userId, String noticeId) throws IOException {
        sendSimpleCommand("NOTICE_DELETE", new String[]{userId, noticeId});
    }

    // -----------------------------
// 영상 목록 / CRUD
// -----------------------------
public static List<VideoItem> fetchVideos(String userId) throws IOException {
    List<VideoItem> list = new ArrayList<>();

    try (
            Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
    ) {
        String line = "VIDEO_LIST|" + userId + "\n";
        writer.write(line);
        writer.flush();

        while (true) {
            String resp = reader.readLine();
            if (resp == null) {
                throw new IOException("서버 응답이 도중에 끊어졌습니다.");
            }
            if ("END".equals(resp)) break;

            // VIDEO|id|title|weekLabel|(optional progress...)
            String[] parts = resp.split("\\|");
            if (parts.length >= 4 && "VIDEO".equals(parts[0])) {
                String id = parts[1];
                String title = parts[2];
                String weekLabel = parts[3];
                list.add(new VideoItem(id, title, weekLabel));
            }
        }
    }

    return list;
}

    public static void deleteVideo(String userId, String videoId) throws IOException {
        sendSimpleCommand("VIDEO_DELETE", new String[]{userId, videoId});
    }
    

    // VIDEO_UPLOAD_FILE : 주차 + 파일 업로드
    public static void uploadVideoFile(String userId, String weekId, File file) throws IOException {
        long fileSize = file.length();
        String fileName = file.getName();

        try (
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                OutputStream out = socket.getOutputStream();
                InputStream in = socket.getInputStream();
                FileInputStream fis = new FileInputStream(file)
        ) {
            String header = "VIDEO_UPLOAD_FILE|" + userId + "|" + weekId + "|" +
                    fileName + "|" + fileSize + "\n";
            out.write(header.getBytes(StandardCharsets.UTF_8));
            out.flush();

            // 서버에서 OK 헤더 (필요 없는 경우라도 한 번 읽어 줌)
            String respHeader = readLine(in);
            if (respHeader != null && respHeader.startsWith("ERR|")) {
                throw new IOException("영상 업로드 실패: " + respHeader);
            }

            // 파일 전송
            byte[] buf = new byte[8192];
            long remaining = fileSize;
            while (remaining > 0) {
                int read = fis.read(buf, 0, (int) Math.min(buf.length, remaining));
                if (read == -1) break;
                out.write(buf, 0, read);
                remaining -= read;
            }
            out.flush();

            // DONE 또는 OK 등 마지막 한 줄 (없어도 크게 문제는 안 됨)
            String done = readLine(in);
            if (done != null && done.startsWith("ERR|")) {
                throw new IOException("영상 업로드 중 오류: " + done);
            }
        }
    }

    // ==========================
    //  과제 CRUD (교사용)
    // ==========================

    public static void createAssignment(String userId, String title, String summary) throws IOException {
        sendSimpleCommandWithBody("ASSIGN_CREATE", new String[]{userId, title, summary});
    }

    public static void updateAssignment(String userId, String taskId, String title, String summary) throws IOException {
        sendSimpleCommandWithBody("ASSIGN_UPDATE", new String[]{userId, taskId, title, summary});
    }

    public static void deleteAssignment(String userId, String taskId) throws IOException {
        sendSimpleCommand("ASSIGN_DELETE", new String[]{userId, taskId});
    }

    // ==========================
    //  학생 목록 (교사용)
    // ==========================

    public static List<StudentInfo> fetchStudents(String userId) throws IOException {
        List<StudentInfo> list = new ArrayList<>();

        try (
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
        ) {
            String line = "STUDENT_LIST|" + userId + "\n";
            writer.write(line);
            writer.flush();

            while (true) {
                String resp = reader.readLine();
                if (resp == null) {
                    throw new IOException("서버 응답이 도중에 끊어졌습니다.");
                }
                if ("END".equals(resp)) break;

                // 형식: STUDENT|id|name|contact  또는 id|name|contact (fallback)
                String[] parts = resp.split("\\|", -1);
                int offset = 0;
                if (parts.length > 0 && "STUDENT".equals(parts[0])) {
                    offset = 1;
                }
                if (parts.length - offset >= 1) {
                    String sid = parts[offset];
                    String name = (parts.length - offset >= 2) ? parts[offset + 1] : "";
                    String contact = (parts.length - offset >= 3) ? parts[offset + 2] : "";
                    list.add(new StudentInfo(sid, name, contact));
                }
            }
        }

        return list;
    }

    // ==========================
    //  공용 유틸
    // ==========================

    private static void sendSimpleCommand(String cmd, String[] args) throws IOException {
        try (
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
        ) {
            StringBuilder sb = new StringBuilder(cmd);
            for (String a : args) {
                sb.append("|").append(a);
            }
            sb.append("\n");

            writer.write(sb.toString());
            writer.flush();

            String resp = reader.readLine();
            if (resp == null) {
                throw new IOException("서버 응답이 없습니다.");
            }
            if (resp.startsWith("ERR|")) {
                throw new IOException("요청 실패: " + resp);
            }
            // 보통 "OK" 기대
            if (!"OK".equals(resp)) {
                // 혹시 다른 형식이면 그냥 한번에 보고 싶으니 예외로 처리
                throw new IOException("예상치 못한 응답: " + resp);
            }
        }
    }

    // 마지막 인자는 text body일 때, 개행 제거
    private static void sendSimpleCommandWithBody(String cmd, String[] args) throws IOException {
        try (
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
        ) {
            StringBuilder sb = new StringBuilder(cmd);
            for (int i = 0; i < args.length - 1; i++) {
                sb.append("|").append(args[i]);
            }
            if (args.length > 0) {
                String last = args[args.length - 1].replace("\n", " ");
                sb.append("|").append(last);
            }
            sb.append("\n");

            writer.write(sb.toString());
            writer.flush();

            String resp = reader.readLine();
            if (resp == null) {
                throw new IOException("서버 응답이 없습니다.");
            }
            if (resp.startsWith("ERR|")) {
                throw new IOException("요청 실패: " + resp);
            }
            if (!"OK".equals(resp)) {
                throw new IOException("예상치 못한 응답: " + resp);
            }
        }
    }

    // TCP 스트림에서 한 줄 읽기 (LF까지, \r\n도 처리)
    private static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        while (true) {
            int b = in.read();
            if (b == -1) {
                if (buf.size() == 0) return null;
                break;
            }
            if (b == '\n') {
                break;
            }
            if (b == '\r') {
                // \r\n 대응용: CR은 버리고, 다음 read에서 LF가 올 것
                continue;
            }
            buf.write(b);
        }
        return buf.toString(StandardCharsets.UTF_8).trim();
    }
}
