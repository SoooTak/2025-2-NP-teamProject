package lms;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class LmsClient {

    // Docker 컨테이너 포트 매핑 기준
    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT = 5050;

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

    // -----------------------------
    // 로그인
    // -----------------------------
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

    // -----------------------------
    // 채팅: 메시지 보내기
    // -----------------------------
    public static boolean sendChat(String fromId, String toId, String message) throws IOException {
        try (
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
        ) {
            // message 안에 '|'나 개행이 없다고 가정
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

    // -----------------------------
    // 채팅: 두 사용자 간 메시지 목록 가져오기
    // -----------------------------
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

    // -----------------------------
    // 과제 목록 가져오기
    // -----------------------------
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

    // -----------------------------
    // (구) 과제 텍스트 제출
    // -----------------------------
    public static boolean submitAssignment(String userId, String assignmentId, String content) throws IOException {
        try (
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
        ) {
            // content 안에 '|'나 개행이 없다고 가정 (단순 프로토콜)
            String requestLine = "ASSIGN_SUBMIT|" + userId + "|" + assignmentId + "|" + content + "\n";
            writer.write(requestLine);
            writer.flush();

            String responseLine = reader.readLine();
            if (responseLine == null) {
                throw new IOException("서버 응답이 없습니다.");
            }

            if ("OK".equals(responseLine)) {
                return true;
            } else if (responseLine.startsWith("ERR|")) {
                throw new IOException("과제 제출 실패: " + responseLine);
            } else {
                throw new IOException("알 수 없는 서버 응답: " + responseLine);
            }
        }
    }

    // -----------------------------
    // (신) 과제 파일 제출
    // -----------------------------
    public static boolean submitAssignmentFile(String userId, String assignmentId, File file) throws IOException {
        long fileSizeLong = file.length();
        if (fileSizeLong < 0) {
            throw new IOException("파일 크기를 읽을 수 없습니다.");
        }
        if (fileSizeLong > Integer.MAX_VALUE) {
            throw new IOException("파일이 너무 큽니다. (최대 2GB 미만)");
        }
        int fileSize = (int) fileSizeLong;

        try (
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
        ) {
            // 1) 헤더 전송
            String header = "ASSIGN_SUBMIT_FILE|" + userId + "|" + assignmentId + "|" +
                    file.getName() + "|" + fileSize + "\n";
            writer.write(header);
            writer.flush();

            // 2) 서버 OK 응답
            String resp = reader.readLine();
            if (resp == null) {
                throw new IOException("서버 응답이 없습니다.");
            }
            if (!"OK".equals(resp)) {
                if (resp.startsWith("ERR|")) {
                    throw new IOException("파일 제출 요청 거부: " + resp);
                } else {
                    throw new IOException("알 수 없는 서버 응답: " + resp);
                }
            }

            // 3) 파일 바이트 전송
            OutputStream out = socket.getOutputStream();
            int sent = 0;
            try (InputStream inFile = new FileInputStream(file)) {
                byte[] buf = new byte[4096];
                int read;
                while ((read = inFile.read(buf)) != -1) {
                    out.write(buf, 0, read);
                    sent += read;
                }
                out.flush();
            }

            if (sent != fileSize) {
                throw new IOException("파일 크기가 변경되었습니다. (예상: " +
                        fileSize + ", 실제 전송: " + sent + ")");
            }

            // 4) 서버 완료 응답
            String finalResp = reader.readLine();
            if (finalResp == null) {
                throw new IOException("서버 완료 응답이 없습니다.");
            }
            if ("DONE".equals(finalResp) || "OK".equals(finalResp)) {
                return true;
            }
            if (finalResp.startsWith("ERR|")) {
                throw new IOException("파일 저장 실패: " + finalResp);
            }
            throw new IOException("알 수 없는 서버 응답: " + finalResp);
        }
    }
}
