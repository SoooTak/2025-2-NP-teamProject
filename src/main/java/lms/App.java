package lms;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.web.WebView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JavaFX로 만든 간단한 LMS 클라이언트 앱.
 * 화면 구성만 담당하고, 실제 네트워크 통신은 LmsClient가 맡는다.
 */
public class App extends Application {

    private Stage primaryStage;
    private Scene loginScene;
    private Scene mainScene;

    private String currentRole;     // "STUDENT" / "TEACHER"
    private String currentUserName;
    private String currentUserId;

    private StackPane contentPane;
    private Button homeBtn;
    private Button assignmentBtn;
    private Button noticeBtn;
    private Button videoBtn;
    private Button chatBtn;
    private Button studentBtn;   // 교수 전용

    private String currentPanel = "home";

    // 색상 팔레트
    private static final String BG = "#eef2ff";
    private static final String SURFACE = "#ffffff";
    private static final String TEXT = "#111a35";
    private static final String MUTED = "#6b7a99";
    private static final String PRIMARY = "#2563eb";
    private static final String BORDER = "#d7deee";
    private static final String SIDEBAR_BG = "#101323";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.loginScene = buildLoginScene();

        stage.setTitle("네트워크 프로그래밍 LMS 데모");
        stage.setScene(loginScene);
        stage.setResizable(false);
        stage.show();
    }

    // -----------------------------
// 1) 로그인 화면
// -----------------------------
private Scene buildLoginScene() {
    BorderPane root = new BorderPane();
    root.setStyle(
            "-fx-background-color: linear-gradient(to bottom right,#101323,#1d2445);"
    );

    VBox card = new VBox(18);
    card.setPadding(new Insets(24));
    card.setStyle(
            "-fx-background-color: " + SURFACE + ";" +
                    "-fx-background-radius: 20;" +
                    "-fx-border-radius: 20;" +
                    "-fx-border-color: " + BORDER + ";" +
                    "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.45), 24, 0, 0, 12);"
    );
    card.setAlignment(Pos.CENTER_LEFT);
    card.setMaxWidth(360);

    // 👉 제목만 남김
    Label title = new Label("네트워크 프로그래밍");
    title.setFont(Font.font("Segoe UI Semibold", 22));
    title.setStyle("-fx-text-fill: " + TEXT + ";");

    VBox textBox = new VBox(6, title);

    VBox form = new VBox(10);
    Label idLabel = new Label("아이디");
    idLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");
    TextField idField = new TextField();
    idField.setPromptText("로그인 아이디를 입력하세요");
    styleTextField(idField);

    Label pwLabel = new Label("비밀번호");
    pwLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");
    PasswordField pwField = new PasswordField();
    pwField.setPromptText("비밀번호를 입력하세요");
    styleTextField(pwField);

    Button loginButton = new Button("로그인");
    stylePrimaryButton(loginButton);
    loginButton.setMaxWidth(Double.MAX_VALUE);

    loginButton.setOnAction(e -> {
        String id = idField.getText().trim();
        String pw = pwField.getText().trim();
        handleLogin(id, pw);
    });

    form.getChildren().addAll(idLabel, idField, pwLabel, pwField, loginButton);

    card.getChildren().addAll(textBox, new Separator(), form);

    BorderPane.setAlignment(card, Pos.CENTER);
    root.setCenter(card);
    BorderPane.setMargin(card, new Insets(40));

    Scene scene = new Scene(root, 640, 400);
    scene.getRoot().setStyle(
            "-fx-font-family: 'Malgun Gothic', 'Apple SD Gothic Neo', 'Segoe UI', sans-serif;" +
                    "-fx-font-size: 14px;"
    );
    return scene;
}


    private void styleTextField(TextField field) {
        field.setStyle(
                "-fx-background-color: #f9fbff;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-padding: 8 10 8 10;"
        );
    }

    private void stylePrimaryButton(Button btn) {
        btn.setStyle(
                "-fx-background-color: " + PRIMARY + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-font-weight: 600;" +
                        "-fx-padding: 8 16 8 16;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.3), 12, 0, 0, 4);"
        );
        btn.setOnMouseEntered(e ->
                btn.setStyle(
                        "-fx-background-color: #1d4ed8;" +
                                "-fx-text-fill: white;" +
                                "-fx-background-radius: 12;" +
                                "-fx-font-weight: 600;" +
                                "-fx-padding: 8 16 8 16;" +
                                "-fx-cursor: hand;" +
                                "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.4), 14, 0, 0, 6);"
                )
        );
        btn.setOnMouseExited(e ->
                btn.setStyle(
                        "-fx-background-color: " + PRIMARY + ";" +
                                "-fx-text-fill: white;" +
                                "-fx-background-radius: 12;" +
                                "-fx-font-weight: 600;" +
                                "-fx-padding: 8 16 8 16;" +
                                "-fx-cursor: hand;" +
                                "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.3), 12, 0, 0, 4);"
                )
        );
    }

    // -----------------------------
    // 로그인 처리
    // -----------------------------
    private void handleLogin(String id, String pw) {
        if (id == null || id.isBlank() || pw == null || pw.isBlank()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("로그인 실패");
            alert.setHeaderText(null);
            alert.setContentText("아이디와 비밀번호를 모두 입력해 주세요.");
            alert.showAndWait();
            return;
        }

        try {
            LmsClient.LoginResult result = LmsClient.login(id.trim(), pw.trim());
            if (result.success) {
                currentRole = result.role;
                currentUserName = result.displayName;
                currentUserId = id.trim();

                if (mainScene == null) {
                    mainScene = buildMainScene();
                }
                primaryStage.setScene(mainScene);
                primaryStage.setWidth(1080);
                primaryStage.setHeight(720);
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("로그인 실패");
                alert.setHeaderText(null);
                String msg = "로그인에 실패했습니다. (" + result.errorCode + ")";
                if ("INVALID_CREDENTIALS".equals(result.errorCode)) {
                    msg = "아이디 또는 비밀번호가 올바르지 않습니다.";
                }
                alert.setContentText(msg);
                alert.showAndWait();
            }
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("로그인 실패");
            alert.setHeaderText(null);
            alert.setContentText("서버에 연결할 수 없습니다.\n" + e.getMessage());
            alert.showAndWait();
        }
    }

    // -----------------------------
    // 2) 메인 화면
    // -----------------------------
    private Scene buildMainScene() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(0));
        root.setStyle("-fx-background-color: " + BG + ";");

        // ---- 왼쪽 사이드바 ----
        VBox sideBar = new VBox(18);
        sideBar.setPadding(new Insets(28, 20, 28, 20));
        sideBar.setPrefWidth(240);
        sideBar.setStyle(
                "-fx-background-color: " + SIDEBAR_BG + ";" +
                        "-fx-text-fill: #cfd5f7;"
        );

        Label brand = new Label("네트워크 프로그래밍");
        brand.setStyle(
                "-fx-text-fill: #cfd5f7;" +
                        "-fx-font-size: 18px;" +
                        "-fx-font-weight: 800;"
        );

        Label sideTitle = new Label("메뉴");
        sideTitle.setStyle(
                "-fx-text-fill: #8790bf;" +
                        "-fx-font-size: 12px;" +
                        "-fx-letter-spacing: 0.5px;"
        );

        homeBtn = createNavButton("강의실 홈");
        assignmentBtn = createNavButton("과제");
        noticeBtn = createNavButton("공지");
        videoBtn = createNavButton("강의 영상");
        chatBtn = createNavButton("채팅");

        VBox navBox = new VBox(6, homeBtn, assignmentBtn, noticeBtn, videoBtn, chatBtn);

        if ("TEACHER".equals(currentRole)) {
            studentBtn = createNavButton("학생 정보");
            navBox.getChildren().add(studentBtn);
        }

        sideBar.getChildren().addAll(brand, sideTitle, navBox);

        // ---- 상단 헤더 ----
        HBox header = new HBox(16);
        header.setPadding(new Insets(24, 40, 20, 40));
        header.setAlignment(Pos.CENTER_LEFT);

        VBox courseMeta = new VBox(4);
        Label title = new Label("2025-2 네트워크 프로그래밍");
        title.setStyle(
                "-fx-text-fill: " + TEXT + ";" +
                        "-fx-font-size: 26px;" +
                        "-fx-font-weight: 700;"
        );
        Label prof = new Label("담당 교수 : 박교수");
        prof.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                        "-fx-font-size: 13px;"
        );
        courseMeta.getChildren().addAll(title, prof);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label roleLabel = new Label();
        if ("TEACHER".equals(currentRole)) {
            roleLabel.setText("교사 · " + currentUserName);
        } else {
            roleLabel.setText("학생 · " + currentUserName);
        }
        roleLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 13px;");

        Button logoutButton = new Button("로그아웃");
        styleOutlineButton(logoutButton);
        logoutButton.setOnAction(e -> {
            currentRole = null;
            currentUserName = null;
            currentUserId = null;
            mainScene = null;
            currentPanel = "home";
            primaryStage.setScene(loginScene);
            primaryStage.setWidth(640);
            primaryStage.setHeight(400);
        });

        HBox controls = new HBox(10, roleLabel, logoutButton);
        controls.setAlignment(Pos.CENTER_RIGHT);

        header.getChildren().addAll(courseMeta, spacer, controls);

        // ---- 중앙 컨텐트 영역 ----
        contentPane = new StackPane();
        contentPane.setPadding(new Insets(0, 40, 40, 40));

        // 첫 화면: 홈
        showPanel("home");

        root.setLeft(sideBar);

        VBox topAndCenter = new VBox();
        topAndCenter.getChildren().addAll(header, contentPane);
        VBox.setVgrow(contentPane, Priority.ALWAYS);

        root.setCenter(topAndCenter);

        Scene scene = new Scene(root, 1080, 720);
        scene.getRoot().setStyle(
                "-fx-font-family: 'Malgun Gothic', 'Apple SD Gothic Neo', 'Segoe UI', sans-serif;" +
                        "-fx-font-size: 14px;"
        );

        // 네비게이션 버튼 핸들러
        homeBtn.setOnAction(e -> showPanel("home"));
        assignmentBtn.setOnAction(e -> showPanel("assignments"));
        noticeBtn.setOnAction(e -> showPanel("notices"));
        videoBtn.setOnAction(e -> showPanel("videos"));
        chatBtn.setOnAction(e -> showPanel("chat"));
        if (studentBtn != null) {
            studentBtn.setOnAction(e -> showPanel("students"));
        }

        return scene;
    }

    private Button createNavButton(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #cfd5f7;" +
                        "-fx-background-radius: 12;" +
                        "-fx-font-weight: 600;" +
                        "-fx-padding: 10 14 10 14;" +
                        "-fx-cursor: hand;"
        );
        btn.setOnMouseEntered(e ->
                btn.setStyle(
                        "-fx-background-color: rgba(255,255,255,0.06);" +
                                "-fx-text-fill: #e5ebff;" +
                                "-fx-background-radius: 12;" +
                                "-fx-font-weight: 600;" +
                                "-fx-padding: 10 14 10 14;" +
                                "-fx-cursor: hand;"
                )
        );
        btn.setOnMouseExited(e -> updateNavButtonStyles());
        return btn;
    }

    private void styleOutlineButton(Button btn) {
        btn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: " + TEXT + ";" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-padding: 7 12 7 12;" +
                        "-fx-font-size: 13px;" +
                        "-fx-cursor: hand;"
        );
        btn.setOnMouseEntered(e ->
                btn.setStyle(
                        "-fx-background-color: rgba(37,99,235,0.08);" +
                                "-fx-text-fill: " + PRIMARY + ";" +
                                "-fx-background-radius: 12;" +
                                "-fx-border-radius: 12;" +
                                "-fx-border-color: " + PRIMARY + ";" +
                                "-fx-padding: 7 12 7 12;" +
                                "-fx-font-size: 13px;" +
                                "-fx-cursor: hand;"
                )
        );
        btn.setOnMouseExited(e ->
                btn.setStyle(
                        "-fx-background-color: transparent;" +
                                "-fx-text-fill: " + TEXT + ";" +
                                "-fx-background-radius: 12;" +
                                "-fx-border-radius: 12;" +
                                "-fx-border-color: " + BORDER + ";" +
                                "-fx-padding: 7 12 7 12;" +
                                "-fx-font-size: 13px;" +
                                "-fx-cursor: hand;"
                )
        );
    }

    private void updateNavButtonStyles() {
        styleNavButtonState(homeBtn, "home".equals(currentPanel));
        styleNavButtonState(assignmentBtn, "assignments".equals(currentPanel));
        styleNavButtonState(noticeBtn, "notices".equals(currentPanel));
        styleNavButtonState(videoBtn, "videos".equals(currentPanel));
        styleNavButtonState(chatBtn, "chat".equals(currentPanel));
        if (studentBtn != null) {
            styleNavButtonState(studentBtn, "students".equals(currentPanel));
        }
    }

    private void styleNavButtonState(Button btn, boolean active) {
        if (btn == null) return;
        if (active) {
            btn.setStyle(
                    "-fx-background-color: #1d2445;" +
                            "-fx-text-fill: #78a7ff;" +
                            "-fx-background-radius: 12;" +
                            "-fx-border-radius: 12;" +
                            "-fx-border-color: rgba(120,167,255,0.4);" +
                            "-fx-border-width: 1;" +
                            "-fx-font-weight: 600;" +
                            "-fx-padding: 10 14 10 14;" +
                            "-fx-cursor: hand;"
            );
        } else {
            btn.setStyle(
                    "-fx-background-color: transparent;" +
                            "-fx-text-fill: #cfd5f7;" +
                            "-fx-background-radius: 12;" +
                            "-fx-font-weight: 600;" +
                            "-fx-padding: 10 14 10 14;" +
                            "-fx-cursor: hand;"
            );
        }
    }

    // 패널 전환
    private void showPanel(String name) {
        currentPanel = name;
        Pane panel;
        switch (name) {
            case "assignments":
                panel = buildAssignmentPanel();
                break;
            case "notices":
                panel = buildNoticePanel();
                break;
            case "videos":
                panel = buildVideoPanel();
                break;
            case "chat":
                panel = buildChatPanel();
                break;
            case "students":
                panel = buildStudentPanel();
                break;
            case "home":
            default:
                panel = buildHomePanel();
                break;
        }
        contentPane.getChildren().setAll(panel);
        updateNavButtonStyles();
    }

    // -----------------------------
    // 홈 패널 (DB 데이터 기반)
    // -----------------------------
    private VBox buildHomePanel() {
        VBox box = new VBox(18);
        box.setPadding(new Insets(0, 0, 0, 0));

        VBox panel = new VBox(18);
        panel.setPadding(new Insets(24));
        panel.setStyle(
                "-fx-background-color: " + SURFACE + ";" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 24;" +
                        "-fx-background-radius: 24;" +
                        "-fx-effect: dropshadow(gaussian, rgba(17,23,35,0.12), 18, 0, 0, 8);"
        );

        HBox head = new HBox();
        Label hTitle = new Label("강의실 홈");
        hTitle.setStyle(
                "-fx-text-fill: " + TEXT + ";" +
                        "-fx-font-size: 22px;" +
                        "-fx-font-weight: 700;"
        );
        head.getChildren().add(hTitle);

        // ---- DB 데이터 읽어오기 ----
        List<LmsClient.Assignment> assignments = null;
        List<LmsClient.NoticeItem> notices = null;
        List<LmsClient.VideoItem> videos = null;

        String userIdForQuery = (currentUserId != null ? currentUserId : "student");

        try {
            assignments = LmsClient.fetchAssignments(userIdForQuery);
        } catch (IOException ignored) {
        }
        try {
            notices = LmsClient.fetchNotices(userIdForQuery);
        } catch (IOException ignored) {
        }
        try {
            videos = LmsClient.fetchVideos(userIdForQuery);
        } catch (IOException ignored) {
        }

        // 요약 카드용 텍스트
        String userLabel = ("TEACHER".equals(currentRole) ? "교수 · " + currentUserName : "학생 · " + currentUserName);

        String currentWeekText = "데이터 없음";
        if (assignments != null && !assignments.isEmpty()) {
            int maxWeek = 0;
            for (LmsClient.Assignment a : assignments) {
                int w = extractLastWeekNumberFromTitle(a.title);
                if (w > maxWeek) maxWeek = w;
            }
            if (maxWeek > 0) currentWeekText = "Week " + maxWeek;
        }

        String assignCountText = (assignments == null ? "데이터 없음" : assignments.size() + "개");
        String videoCountText = (videos == null ? "데이터 없음" : videos.size() + "개");

        HBox summaryRow = new HBox(14);
        summaryRow.setFillHeight(true);

        VBox s1 = createSummaryCard("현재 사용자", userLabel);
        VBox s2 = createSummaryCard("현재 주차(과제 기준)", currentWeekText);
        VBox s3 = createSummaryCard("등록 과제 수", assignCountText);
        VBox s4 = createSummaryCard("강의 영상 수", videoCountText);

        HBox.setHgrow(s1, Priority.ALWAYS);
        HBox.setHgrow(s2, Priority.ALWAYS);
        HBox.setHgrow(s3, Priority.ALWAYS);
        HBox.setHgrow(s4, Priority.ALWAYS);

        summaryRow.getChildren().addAll(s1, s2, s3, s4);

        // ---- 최근 항목들 ----
        HBox homeColumns = new HBox(18);

        String[] recentAssignLines = buildRecentAssignments(assignments);
        String[] recentNoticeLines = buildRecentNotices(notices);
        String[] recentVideoLines = buildRecentVideos(videos);

        VBox col1 = createHomeBox("최근 과제", recentAssignLines);
        VBox col2 = createHomeBox("최근 공지", recentNoticeLines);
        VBox col3 = createHomeBox("최근 강의 영상", recentVideoLines);

        HBox.setHgrow(col1, Priority.ALWAYS);
        HBox.setHgrow(col2, Priority.ALWAYS);
        HBox.setHgrow(col3, Priority.ALWAYS);

        homeColumns.getChildren().addAll(col1, col2, col3);

        panel.getChildren().addAll(head, summaryRow, homeColumns);
        box.getChildren().add(panel);
        return box;
    }

    private VBox createSummaryCard(String label, String value) {
        VBox v = new VBox(6);
        v.setPadding(new Insets(14));
        v.setStyle(
                "-fx-background-color: #f8faff;" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 18;" +
                        "-fx-background-radius: 18;"
        );

        Label l = new Label(label);
        l.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");

        Label val = new Label(value);
        val.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 20px; -fx-font-weight: 800;");

        v.getChildren().addAll(l, val);
        return v;
    }

    private VBox createHomeBox(String title, String[] lines) {
        VBox box = new VBox(10);
        box.setPadding(new Insets(18));
        box.setStyle(
                "-fx-background-color: #fdfdff;" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 20;" +
                        "-fx-background-radius: 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.08), 18, 0, 0, 8);"
        );

        Label t = new Label(title);
        t.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 17px; -fx-font-weight: 700;");

        VBox list = new VBox(4);
        if (lines != null) {
            for (String line : lines) {
                Label l = new Label(line);
                l.setWrapText(true);
                l.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");
                list.getChildren().add(l);
            }
        }

        Button goBtn = new Button(title.contains("과제") ? "과제 화면으로" :
                title.contains("공지") ? "공지 화면으로" : "강의 영상 화면으로");
        styleOutlineButton(goBtn);
        goBtn.setOnAction(e -> {
            if (title.contains("과제")) showPanel("assignments");
            else if (title.contains("공지")) showPanel("notices");
            else showPanel("videos");
        });

        HBox cta = new HBox(goBtn);
        cta.setAlignment(Pos.CENTER_RIGHT);

        box.getChildren().addAll(t, list, cta);
        return box;
    }

    private String[] buildRecentAssignments(List<LmsClient.Assignment> assignments) {
        List<String> lines = new ArrayList<>();
        if (assignments == null || assignments.isEmpty()) {
            lines.add("최근 과제가 없습니다.");
        } else {
            int count = Math.min(2, assignments.size());
            for (int i = 0; i < count; i++) {
                LmsClient.Assignment a = assignments.get(i);
                lines.add(buildNormalizedWeekTitle(a.title));
                lines.add("마감: " + a.due);
            }
        }
        return lines.toArray(new String[0]);
    }

    private String[] buildRecentNotices(List<LmsClient.NoticeItem> notices) {
        List<String> lines = new ArrayList<>();
        if (notices == null || notices.isEmpty()) {
            lines.add("최근 공지가 없습니다.");
        } else {
            int count = Math.min(2, notices.size());
            for (int i = 0; i < count; i++) {
                LmsClient.NoticeItem n = notices.get(i);
                ParsedNotice pn = parseNotice(
                        n.content != null && !n.content.isBlank() ? n.content : n.title
                );
                lines.add(pn.title);
                lines.add(n.createdAt);
            }
        }
        return lines.toArray(new String[0]);
    }

    private String[] buildRecentVideos(List<LmsClient.VideoItem> videos) {
        List<String> lines = new ArrayList<>();
        if (videos == null || videos.isEmpty()) {
            lines.add("최근 강의 영상이 없습니다.");
        } else {
            int count = Math.min(2, videos.size());
            for (int i = 0; i < count; i++) {
                LmsClient.VideoItem v = videos.get(i);
                lines.add(v.title);
                lines.add(v.weekLabel);
            }
        }
        return lines.toArray(new String[0]);
    }

    private VBox createMainPanelWrapper(String titleText) {
        VBox panel = new VBox(16);
        panel.setPadding(new Insets(24));
        panel.setStyle(
                "-fx-background-color: " + SURFACE + ";" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 24;" +
                        "-fx-background-radius: 24;" +
                        "-fx-effect: dropshadow(gaussian, rgba(17,23,35,0.12), 18, 0, 0, 8);"
        );

        HBox head = new HBox();
        head.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(titleText);
        title.setStyle(
                "-fx-text-fill: " + TEXT + ";" +
                        "-fx-font-size: 22px;" +
                        "-fx-font-weight: 700;"
        );

        head.getChildren().add(title);
        panel.getChildren().add(head);
        return panel;
    }

    // -----------------------------
    // 과제 주차 관련 헬퍼들
    // -----------------------------

    // "[N주차] ..." 패턴 하나만 제거
    private String stripWeekPrefix(String title) {
        if (title == null) return "";
        String t = title.trim();
        if (t.startsWith("[") && t.contains("주차]")) {
            int close = t.indexOf(']');
            if (close > 0 && close + 1 < t.length()) {
                return t.substring(close + 1).trim();
            }
        }
        return t;
    }

    // 맨 앞에 연달아 붙은 "[N주차]"들을 전부 제거
    private String stripAllWeekPrefixes(String title) {
        if (title == null) return "";
        String t = title.trim();
        while (true) {
            String next = stripWeekPrefix(t);
            if (next.equals(t)) break;
            t = next.trim();
        }
        return t;
    }

    // 제목 문자열에서 "마지막" [N주차]를 찾아서 그 N을 리턴
    private int extractLastWeekNumberFromTitle(String title) {
        if (title == null) return 1;
        String t = title.trim();
        int lastWeek = -1;
        int idx = 0;
        while (true) {
            int open = t.indexOf('[', idx);
            if (open < 0) break;
            int close = t.indexOf(']', open + 1);
            if (close < 0) break;
            String inside = t.substring(open + 1, close); // 예: "1주차"
            if (inside.contains("주차")) {
                String numStr = inside.replace("주차", "").trim();
                try {
                    lastWeek = Integer.parseInt(numStr);
                } catch (NumberFormatException ignored) {
                }
            }
            idx = close + 1;
        }
        if (lastWeek <= 0) lastWeek = 1;
        return lastWeek;
    }

    // DB에 "[1주차] [10주차] 네트워크..."처럼 망가져 있어도
    // 화면에는 "[10주차] 네트워크..."로만 보이게 정규화
    private String buildNormalizedWeekTitle(String original) {
        if (original == null || original.isBlank()) return "";
        int week = extractLastWeekNumberFromTitle(original);
        String base = stripAllWeekPrefixes(original);
        return "[" + week + "주차] " + base;
    }

    // -----------------------------
// 공지 파싱 헬퍼
// -----------------------------
private static class ParsedNotice {
    String title;
    String body;
}

// "제목  내용" 또는 "제목\n\n내용" 형태에서 제목/내용 분리
private ParsedNotice parseNotice(String full) {
    ParsedNotice p = new ParsedNotice();
    if (full == null) full = "";
    full = full.replace("\r\n", "\n"); // 윈도우 개행 정리

    // 1) 옛 포맷: "제목  내용" (공백 두 칸)
    int idxSpace = full.indexOf("  ");
    if (idxSpace >= 0) {
        p.title = full.substring(0, idxSpace).trim();
        p.body  = full.substring(idxSpace + 2).trim();
        return p;
    }

    // 2) 새 포맷: "제목\n\n내용"
    int idx = full.indexOf("\n\n");
    if (idx >= 0) {
        p.title = full.substring(0, idx).trim();
        p.body  = full.substring(idx + 2).trim();
        return p;
    }

    // 3) 그냥 한 줄만 있는 경우
    String[] lines = full.split("\\n", 2);
    p.title = lines[0].trim();
    p.body  = (lines.length > 1) ? lines[1].trim() : "";
    return p;
}

    // -----------------------------
    // 과제 패널
    // -----------------------------
    private VBox buildAssignmentPanel() {
        VBox root = new VBox(18);
        VBox panel = createMainPanelWrapper("과제");

        VBox listBox = new VBox(12);

        // 교사일 때: 상단에 "과제 등록" 버튼
        if ("TEACHER".equals(currentRole)) {
            Button createBtn = new Button("새 과제 등록");
            stylePrimaryButton(createBtn);
            createBtn.setOnAction(e -> openAssignmentCreateDialog());
            HBox topBar = new HBox(createBtn);
            topBar.setAlignment(Pos.CENTER_RIGHT);
            topBar.setPadding(new Insets(4, 0, 8, 0));
            panel.getChildren().add(topBar);
        }

        try {
            List<LmsClient.Assignment> assignments =
                    LmsClient.fetchAssignments(currentUserId != null ? currentUserId : "student");

            if (assignments.isEmpty()) {
                Label emptyLabel = new Label("등록된 과제가 없습니다.");
                emptyLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 13px;");
                listBox.getChildren().add(emptyLabel);
            } else {
                for (LmsClient.Assignment a : assignments) {
                    VBox card = createAssignmentCard(a);
                    listBox.getChildren().add(card);
                }
            }
        } catch (IOException e) {
            Label errLabel = new Label("과제 목록을 불러오는 중 오류가 발생했습니다:\n" + e.getMessage());
            errLabel.setStyle("-fx-text-fill: #b91c1c; -fx-font-size: 12px;");
            listBox.getChildren().add(errLabel);
        }

        // 스크롤바 추가
        ScrollPane sp = new ScrollPane(listBox);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setStyle("-fx-background-color: transparent; -fx-background-insets: 0;");
        VBox.setVgrow(sp, Priority.ALWAYS);

        panel.getChildren().add(sp);
        root.getChildren().add(panel);
        return root;
    }

    private VBox createAssignmentCard(LmsClient.Assignment a) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16));
        card.setStyle(
                "-fx-background-color: #f8faff;" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 18;" +
                        "-fx-background-radius: 18;"
        );

        // 여기서 제목을 정규화해서 보여줌 → [1주차] [10주차] → [10주차] 하나만
        Label t = new Label(buildNormalizedWeekTitle(a.title));
        t.setWrapText(true);
        t.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 16px; -fx-font-weight: 600;");

        Label m = new Label("마감: " + a.due);
        m.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");

        Label s = new Label(a.summary);
        s.setWrapText(true);
        s.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");

        card.getChildren().addAll(t, m, s);

        HBox bottom = new HBox(8);
        bottom.setAlignment(Pos.CENTER_RIGHT);

        if ("STUDENT".equals(currentRole)) {
            Button submitBtn = new Button("파일 제출");
            styleOutlineButton(submitBtn);
            submitBtn.setOnAction(e -> openAssignmentSubmitDialog(a));
            bottom.getChildren().add(submitBtn);
        } else if ("TEACHER".equals(currentRole)) {
            Button statusBtn = new Button("제출 현황");
            styleOutlineButton(statusBtn);
            statusBtn.setOnAction(e -> openSubmissionStatusDialog(a));

            Button editBtn = new Button("수정");
            styleOutlineButton(editBtn);
            editBtn.setOnAction(e -> openAssignmentEditDialog(a));

            Button deleteBtn = new Button("삭제");
            styleOutlineButton(deleteBtn);
            deleteBtn.setOnAction(e -> {
                if (confirm("과제를 삭제하시겠습니까? (제출물도 함께 삭제될 수 있습니다)")) {
                    try {
                        LmsClient.deleteAssignment(currentUserId, a.id);
                        info("삭제 완료", "과제가 삭제되었습니다.");
                        showPanel("assignments");
                    } catch (IOException ex) {
                        error("삭제 오류", "과제를 삭제하는 중 오류가 발생했습니다.\n" + ex.getMessage());
                    }
                }
            });

            bottom.getChildren().addAll(statusBtn, editBtn, deleteBtn);
        }

        card.getChildren().add(bottom);
        return card;
    }

    // 과제 등록 : 주차 선택 포함 (DB 안이 어떻게 꼬여 있어도, 화면은 항상 한 번만 [N주차] 보이게)
    private void openAssignmentCreateDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("새 과제 등록");
        dialog.setHeaderText("새 과제를 등록합니다.");

        ButtonType saveType = new ButtonType("등록", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        ComboBox<Integer> weekCombo = new ComboBox<>();
        weekCombo.setItems(FXCollections.observableArrayList(
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15
        ));
        weekCombo.getSelectionModel().selectFirst();

        TextField titleField = new TextField();
        titleField.setPromptText("과제 제목");

        TextArea summaryArea = new TextArea();
        summaryArea.setPromptText("과제 설명 / 요약");

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));

        int row = 0;
        grid.add(new Label("주차"), 0, row);
        grid.add(weekCombo, 1, row++);

        grid.add(new Label("제목"), 0, row);
        grid.add(titleField, 1, row++);

        grid.add(new Label("설명"), 0, row);
        grid.add(summaryArea, 1, row);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveType) {
                String titleInput = titleField.getText().trim();
                String summary = summaryArea.getText().trim();

                if (titleInput.isEmpty()) {
                    warning("등록 실패", "제목을 입력해 주세요.");
                } else {
                    String baseTitle = stripAllWeekPrefixes(titleInput);
                    Integer week = weekCombo.getSelectionModel().getSelectedItem();
                    String finalTitle;
                    if (week != null) {
                        finalTitle = "[" + week + "주차] " + baseTitle;
                    } else {
                        finalTitle = baseTitle;
                    }

                    try {
                        // LmsClient 쪽은 (userId, title, summary) 시그니처라고 가정
                        LmsClient.createAssignment(currentUserId, finalTitle, summary);
                        info("등록 완료", "과제가 등록되었습니다.");
                        showPanel("assignments");
                    } catch (IOException e) {
                        error("등록 오류", "과제를 등록하는 중 오류가 발생했습니다.\n" + e.getMessage());
                    }
                }
            }
            return null;
        });

        dialog.initOwner(primaryStage);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.showAndWait();
    }

    // 과제 수정 : 주차 선택 포함 + [1주차] [10주차] 꼬여 있어도 마지막 것만 사용
    private void openAssignmentEditDialog(LmsClient.Assignment a) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("과제 수정");
        dialog.setHeaderText("과제 내용을 수정합니다.");

        ButtonType saveType = new ButtonType("저장", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        int defaultWeek = extractLastWeekNumberFromTitle(a.title);
        String rawTitle = stripAllWeekPrefixes(a.title);

        ComboBox<Integer> weekCombo = new ComboBox<>();
        weekCombo.setItems(FXCollections.observableArrayList(
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15
        ));
        if (defaultWeek >= 1 && defaultWeek <= 15) {
            weekCombo.getSelectionModel().select(Integer.valueOf(defaultWeek));
        } else {
            weekCombo.getSelectionModel().selectFirst();
        }

        TextField titleField = new TextField(rawTitle);
        TextArea summaryArea = new TextArea(a.summary);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));

        int row = 0;
        grid.add(new Label("주차"), 0, row);
        grid.add(weekCombo, 1, row++);

        grid.add(new Label("제목"), 0, row);
        grid.add(titleField, 1, row++);

        grid.add(new Label("설명"), 0, row);
        grid.add(summaryArea, 1, row);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveType) {
                String titleInput = titleField.getText().trim();
                String summary = summaryArea.getText().trim();
                if (titleInput.isEmpty()) {
                    warning("수정 실패", "제목을 입력해 주세요.");
                } else {
                    String baseTitle = stripAllWeekPrefixes(titleInput);
                    Integer week = weekCombo.getSelectionModel().getSelectedItem();
                    String finalTitle;
                    if (week != null) {
                        finalTitle = "[" + week + "주차] " + baseTitle;
                    } else {
                        finalTitle = baseTitle;
                    }

                    try {
                        LmsClient.updateAssignment(currentUserId, a.id, finalTitle, summary);
                        info("수정 완료", "과제가 수정되었습니다.");
                        showPanel("assignments");
                    } catch (IOException e) {
                        error("수정 오류", "과제를 수정하는 중 오류가 발생했습니다.\n" + e.getMessage());
                    }
                }
            }
            return null;
        });

        dialog.initOwner(primaryStage);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.showAndWait();
    }

    // 학생용 과제 파일 제출
    private void openAssignmentSubmitDialog(LmsClient.Assignment a) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("제출할 파일 선택");
        File file = chooser.showOpenDialog(primaryStage);
        if (file == null) return;

        if (!confirm("과제를 제출하시겠습니까?\n" + file.getName())) {
            return;
        }

        try {
            boolean ok = LmsClient.submitAssignmentFile(currentUserId, a.id, file);
            if (ok) {
                info("제출 완료", "과제가 정상적으로 제출되었습니다.");
            }
        } catch (IOException e) {
            error("제출 오류", "서버 전송 중 오류가 발생했습니다.\n" + e.getMessage());
        }
    }

    // 교사용: 과제 제출 현황 모달
    private void openSubmissionStatusDialog(LmsClient.Assignment a) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("과제 제출 현황");
        dialog.setHeaderText(buildNormalizedWeekTitle(a.title) + " 제출 현황");

        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox root = new VBox(8);
        root.setPadding(new Insets(10));

        try {
            List<LmsClient.SubmissionInfo> list =
                    LmsClient.fetchSubmissions(currentUserId, a.id);

            if (list.isEmpty()) {
                Label empty = new Label("제출된 파일이 없습니다.");
                empty.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 13px;");
                root.getChildren().add(empty);
            } else {
                for (LmsClient.SubmissionInfo info : list) {
                    HBox row = new HBox(10);
                    row.setAlignment(Pos.CENTER_LEFT);

                    String nameDisplay;
                    if (info.studentName != null && !info.studentName.isBlank()) {
                        nameDisplay = info.studentName + " (" + info.studentId + ")";
                    } else {
                        nameDisplay = info.studentId;
                    }

                    Label nameLabel = new Label(nameDisplay);
                    nameLabel.setPrefWidth(200);

                    Label timeLabel = new Label(info.submittedAt);
                    timeLabel.setPrefWidth(180);
                    timeLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");

                    Button downloadBtn = new Button("다운로드");
                    styleOutlineButton(downloadBtn);
                    downloadBtn.setOnAction(e -> {
                        FileChooser chooser = new FileChooser();
                        String suggested = extractOriginalFilename(info.filePath);
                        chooser.setInitialFileName(suggested);
                        File dest = chooser.showSaveDialog(primaryStage);
                        if (dest != null) {
                            try {
                                LmsClient.downloadSubmissionFile(currentUserId, info.filePath, dest);
                                info("다운로드 완료", "파일이 저장되었습니다.");
                            } catch (IOException ex) {
                                error("다운로드 오류", "파일 다운로드 중 오류가 발생했습니다.\n" + ex.getMessage());
                            }
                        }
                    });

                    row.getChildren().addAll(nameLabel, timeLabel, downloadBtn);
                    root.getChildren().add(row);
                }
            }
        } catch (IOException e) {
            Label err = new Label("제출 현황을 불러오는 중 오류가 발생했습니다:\n" + e.getMessage());
            err.setStyle("-fx-text-fill: #b91c1c; -fx-font-size: 12px;");
            root.getChildren().add(err);
        }

        dialog.getDialogPane().setContent(root);
        dialog.initOwner(primaryStage);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.showAndWait();
    }

    // 서버에 저장된 파일명에서 원래 파일명 추출 (taskId_userId_timestamp_original.ext)
    private String extractOriginalFilename(String filePath) {
        if (filePath == null) return "submission.dat";
        String name = filePath;
        int lastSlash = name.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash + 1 < name.length()) {
            name = name.substring(lastSlash + 1);
        }
        int idx = name.indexOf('_');
        if (idx >= 0 && idx + 1 < name.length()) {
            String rest = name.substring(idx + 1);
            idx = rest.indexOf('_');
            if (idx >= 0 && idx + 1 < rest.length()) {
                rest = rest.substring(idx + 1);
                idx = rest.indexOf('_');
                if (idx >= 0 && idx + 1 < rest.length()) {
                    return rest.substring(idx + 1);
                }
            }
        }
        return name;
    }

    // -----------------------------
    // 공지 패널
    // -----------------------------
    private VBox buildNoticePanel() {
        VBox root = new VBox(18);
        VBox panel = createMainPanelWrapper("공지");

        VBox list = new VBox(12);

        if ("TEACHER".equals(currentRole)) {
            Button createBtn = new Button("새 공지 등록");
            stylePrimaryButton(createBtn);
            createBtn.setOnAction(e -> openNoticeEditor(null));
            HBox topBar = new HBox(createBtn);
            topBar.setAlignment(Pos.CENTER_RIGHT);
            topBar.setPadding(new Insets(4, 0, 8, 0));
            panel.getChildren().add(topBar);
        }

        try {
            List<LmsClient.NoticeItem> notices =
                    LmsClient.fetchNotices(currentUserId != null ? currentUserId : "student");
            if (notices.isEmpty()) {
                Label emptyLabel = new Label("등록된 공지가 없습니다.");
                emptyLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 13px;");
                list.getChildren().add(emptyLabel);
            } else {
                for (LmsClient.NoticeItem n : notices) {
                    VBox card = createNoticeCard(n);
                    list.getChildren().add(card);
                }
            }
        } catch (IOException e) {
            Label errLabel = new Label("공지 목록을 불러오는 중 오류가 발생했습니다:\n" + e.getMessage());
            errLabel.setStyle("-fx-text-fill: #b91c1c; -fx-font-size: 12px;");
            list.getChildren().add(errLabel);
        }

        // 공지 탭도 스크롤 추가
        ScrollPane sp = new ScrollPane(list);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setStyle("-fx-background-color: transparent; -fx-background-insets: 0;");
        VBox.setVgrow(sp, Priority.ALWAYS);

        panel.getChildren().add(sp);
        root.getChildren().add(panel);
        return root;
    }

    private VBox createNoticeCard(LmsClient.NoticeItem item) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(16));
        card.setStyle(
                "-fx-background-color: #fdfdff;" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 18;" +
                        "-fx-background-radius: 18;"
        );

        // item.content OR item.title 에 뭐가 들어있든, 통째로 파싱해서 제목/본문 분리
        String full = (item.content != null && !item.content.isBlank())
                ? item.content
                : item.title;
        ParsedNotice pn = parseNotice(full);

        Label t = new Label(pn.title);
        t.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 16px; -fx-font-weight: 600;");

        Label meta = new Label(item.createdAt);
        meta.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px;");

        Label body = new Label(pn.body);
        body.setWrapText(true);
        body.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 13px;");

        card.getChildren().addAll(t, meta, body);

        if ("TEACHER".equals(currentRole)) {
            HBox btnBar = new HBox(8);
            btnBar.setAlignment(Pos.CENTER_RIGHT);

            Button editBtn = new Button("수정");
            styleOutlineButton(editBtn);
            editBtn.setOnAction(e -> openNoticeEditor(item));

            Button delBtn = new Button("삭제");
            styleOutlineButton(delBtn);
            delBtn.setOnAction(e -> {
                if (confirm("공지를 삭제하시겠습니까?")) {
                    try {
                        LmsClient.deleteNotice(currentUserId, item.id);
                        info("삭제 완료", "공지가 삭제되었습니다.");
                        showPanel("notices");
                    } catch (IOException ex) {
                        error("삭제 오류", "공지를 삭제하는 중 오류가 발생했습니다.\n" + ex.getMessage());
                    }
                }
            });

            btnBar.getChildren().addAll(editBtn, delBtn);
            card.getChildren().add(btnBar);
        }

        return card;
    }

    // 공지 등록/수정 다이얼로그
    private void openNoticeEditor(LmsClient.NoticeItem item) {
        boolean isNew = (item == null);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(isNew ? "새 공지 등록" : "공지 수정");
        dialog.setHeaderText(isNew ? "새 공지를 등록합니다." : "공지를 수정합니다.");

        ButtonType saveType = new ButtonType(isNew ? "등록" : "저장", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        TextField titleField = new TextField();
        TextArea bodyArea = new TextArea();

        if (!isNew) {
            String full = (item.content != null && !item.content.isBlank())
                    ? item.content
                    : item.title;
            ParsedNotice pn = parseNotice(full);
            titleField.setText(pn.title);
            bodyArea.setText(pn.body);
        }

        VBox box = new VBox(8,
                new Label("제목"), titleField,
                new Label("내용"), bodyArea
        );
        box.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(box);

        dialog.setResultConverter(btn -> {
    if (btn == saveType) {
        String t = titleField.getText().trim();
        String b = bodyArea.getText().trim();
        if (t.isEmpty()) {
            warning("저장 실패", "제목을 입력해 주세요.");
        } else {
            // 서버/기존 데이터와 맞추기 위해 "제목  내용" 형태로 합침 (공백 두 칸)
            String combined = t;
            if (!b.isEmpty()) combined += "  " + b;

            try {
                if (isNew) {
                    LmsClient.createNotice(currentUserId, combined);
                } else {
                    LmsClient.updateNotice(currentUserId, item.id, combined);
                }
                info("저장 완료", "공지가 저장되었습니다.");
                showPanel("notices");
            } catch (IOException e) {
                error("저장 오류", "공지를 저장하는 중 오류가 발생했습니다.\n" + e.getMessage());
            }
        }
    }
    return null;
});

        dialog.initOwner(primaryStage);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.showAndWait();
    }

    // -----------------------------
    // 강의 영상 패널
    // -----------------------------
    private VBox buildVideoPanel() {
        VBox root = new VBox(18);
        VBox panel = createMainPanelWrapper("강의 영상");

        VBox list = new VBox(12);

        if ("TEACHER".equals(currentRole)) {
            Button uploadBtn = new Button("영상 업로드");
            stylePrimaryButton(uploadBtn);
            uploadBtn.setOnAction(e -> openVideoUploadDialog());

            HBox topBar = new HBox(uploadBtn);
            topBar.setAlignment(Pos.CENTER_RIGHT);
            topBar.setPadding(new Insets(4, 0, 8, 0));
            panel.getChildren().add(topBar);
        }

        try {
            List<LmsClient.VideoItem> videos =
                    LmsClient.fetchVideos(currentUserId != null ? currentUserId : "student");
            if (videos.isEmpty()) {
                Label emptyLabel = new Label("등록된 강의 영상이 없습니다.");
                emptyLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 13px;");
                list.getChildren().add(emptyLabel);
            } else {
                for (LmsClient.VideoItem v : videos) {
                    VBox card = createVideoCard(v);
                    list.getChildren().add(card);
                }
            }
        } catch (IOException e) {
            Label errLabel = new Label("영상 목록을 불러오는 중 오류가 발생했습니다:\n" + e.getMessage());
            errLabel.setStyle("-fx-text-fill: #b91c1c; -fx-font-size: 12px;");
            list.getChildren().add(errLabel);
        }

        // 영상 탭도 스크롤 추가
        ScrollPane sp = new ScrollPane(list);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setStyle("-fx-background-color: transparent; -fx-background-insets: 0;");
        VBox.setVgrow(sp, Priority.ALWAYS);

        panel.getChildren().add(sp);
        root.getChildren().add(panel);
        return root;
    }

    private VBox createVideoCard(LmsClient.VideoItem v) {
    VBox card = new VBox(6);
    card.setPadding(new Insets(14));
    card.setStyle(
            "-fx-background-color: #fdfdff;" +
                    "-fx-border-color: " + BORDER + ";" +
                    "-fx-border-radius: 18;" +
                    "-fx-background-radius: 18;"
    );

    Label t = new Label(v.title);
    t.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 15px; -fx-font-weight: 600;");

    Label m = new Label(v.weekLabel);
    m.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");

    HBox btnBar = new HBox(8);
    btnBar.setAlignment(Pos.CENTER_RIGHT);

    // 재생 버튼만
    Button playBtn = new Button("재생");
    styleOutlineButton(playBtn);
    playBtn.setOnAction(e -> openVideoModal(v));
    btnBar.getChildren().add(playBtn);

    // 교사일 때만 삭제 버튼
    if ("TEACHER".equals(currentRole)) {
        Button delBtn = new Button("삭제");
        styleOutlineButton(delBtn);
        delBtn.setOnAction(e -> {
            if (confirm("영상을 삭제하시겠습니까? (파일도 삭제될 수 있습니다)")) {
                try {
                    LmsClient.deleteVideo(currentUserId, v.id);
                    info("삭제 완료", "영상이 삭제되었습니다.");
                    showPanel("videos");
                } catch (IOException ex) {
                    error("삭제 오류", "영상을 삭제하는 중 오류가 발생했습니다.\n" + ex.getMessage());
                }
            }
        });
        btnBar.getChildren().add(delBtn);
    }

    card.getChildren().addAll(t, m, btnBar);
    return card;
}

    // 영상 재생 모달 (WebView + HTTP 스트리밍)
    private void openVideoModal(LmsClient.VideoItem v) {
        Stage dialog = new Stage();
        dialog.setTitle(v.title);
        dialog.initOwner(primaryStage);
        dialog.initModality(Modality.APPLICATION_MODAL);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        WebView webView = new WebView();
        String url = "http://127.0.0.1:8081/video/" + v.id;
        webView.getEngine().load(url);

        webView.setPrefSize(960, 540);
        root.setCenter(webView);

        Scene scene = new Scene(root, 980, 580);
        dialog.setScene(scene);
        dialog.show();
    }

    // 교사용: 영상 업로드 다이얼로그 (파일 선택)
    private void openVideoUploadDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("영상 업로드");
        dialog.setHeaderText("강의 영상을 업로드합니다.");

        ButtonType uploadType = new ButtonType("업로드", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(uploadType, ButtonType.CANCEL);

        ComboBox<Integer> weekCombo = new ComboBox<>();
        weekCombo.setItems(FXCollections.observableArrayList(
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15
        ));
        weekCombo.getSelectionModel().selectFirst();

        Label fileLabel = new Label("선택된 파일이 없습니다.");
        Button chooseBtn = new Button("파일 선택");
        styleOutlineButton(chooseBtn);

        final File[] selectedFile = new File[1];

        chooseBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("업로드할 영상 선택");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("MP4 파일", "*.mp4")
            );
            File f = chooser.showOpenDialog(primaryStage);
            if (f != null) {
                selectedFile[0] = f;
                fileLabel.setText(f.getName() + " (" + (f.length() / 1024) + " KB)");
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));

        grid.add(new Label("주차"), 0, 0);
        grid.add(weekCombo, 1, 0);

        grid.add(new Label("파일"), 0, 1);
        HBox fileRow = new HBox(8, chooseBtn, fileLabel);
        fileRow.setAlignment(Pos.CENTER_LEFT);
        grid.add(fileRow, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == uploadType) {
                if (selectedFile[0] == null) {
                    warning("업로드 실패", "업로드할 파일을 선택해 주세요.");
                } else {
                    int week = weekCombo.getSelectionModel().getSelectedItem();
                    try {
                        LmsClient.uploadVideoFile(currentUserId, String.valueOf(week), selectedFile[0]);
                        info("업로드 완료", "영상이 업로드되었습니다.");
                        showPanel("videos");
                    } catch (IOException e) {
                        error("업로드 오류", "영상을 업로드하는 중 오류가 발생했습니다.\n" + e.getMessage());
                    }
                }
            }
            return null;
        });

        dialog.initOwner(primaryStage);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.showAndWait();
    }

    // -----------------------------
// 채팅 패널
// -----------------------------
private VBox buildChatPanel() {
    VBox root = new VBox(18);
    VBox panel = createMainPanelWrapper("채팅");

    HBox layout = new HBox(16);

    // 왼쪽: 상대 정보 / 학생 선택
    VBox targetBox = new VBox(10);
    targetBox.setPadding(new Insets(16));
    targetBox.setStyle(
            "-fx-background-color: #f8f9ff;" +
                    "-fx-border-color: " + BORDER + ";" +
                    "-fx-border-radius: 18;" +
                    "-fx-background-radius: 18;"
    );
    targetBox.setPrefWidth(260);

    Label targetTitle = new Label("대화 상대");
    targetTitle.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-weight: 600;");

    // 오른쪽: 채팅 영역
    VBox chatBox = new VBox(10);
    chatBox.setPadding(new Insets(16));
    chatBox.setStyle(
            "-fx-background-color: #fdfdff;" +
                    "-fx-border-color: " + BORDER + ";" +
                    "-fx-border-radius: 18;" +
                    "-fx-background-radius: 18;"
    );
    chatBox.setPrefHeight(420);
    VBox.setVgrow(chatBox, Priority.ALWAYS);

    ListView<String> messageList = new ListView<>();
    VBox.setVgrow(messageList, Priority.ALWAYS);

    HBox inputBox = new HBox(8);
    TextField inputField = new TextField();
    inputField.setPromptText("메시지를 입력하세요");
    styleTextField(inputField);

    Button sendBtn = new Button("전송");
    stylePrimaryButton(sendBtn);
    HBox.setHgrow(inputField, Priority.ALWAYS);
    inputBox.getChildren().addAll(inputField, sendBtn);

    chatBox.getChildren().addAll(messageList, inputBox);

    // ------------------------
    // 2-1. 교수 / 학생 분기
    // ------------------------
    if ("TEACHER".equals(currentRole)) {
        // 🔹 교수: 학생 목록에서 선택
        Label help = new Label("학생을 선택하면 1:1 대화가 시작됩니다.");
        help.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");

        ComboBox<LmsClient.StudentInfo> studentCombo = new ComboBox<>();
        studentCombo.setPrefWidth(220);

        try {
            List<LmsClient.StudentInfo> students = LmsClient.fetchStudents(currentUserId);
            if (students.isEmpty()) {
                studentCombo.setPromptText("등록된 학생이 없습니다");
            } else {
                studentCombo.setItems(FXCollections.observableArrayList(students));
                studentCombo.setPromptText("학생을 선택하세요");

                // 콤보박스에 이름(학번) 표시
                studentCombo.setCellFactory(cb -> new ListCell<>() {
                    @Override
                    protected void updateItem(LmsClient.StudentInfo item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            String name = (item.name == null || item.name.isBlank())
                                    ? item.studentId
                                    : item.name + " (" + item.studentId + ")";
                            setText(name);
                        }
                    }
                });
                studentCombo.setButtonCell(new ListCell<>() {
                    @Override
                    protected void updateItem(LmsClient.StudentInfo item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText("학생을 선택하세요");
                        } else {
                            String name = (item.name == null || item.name.isBlank())
                                    ? item.studentId
                                    : item.name + " (" + item.studentId + ")";
                            setText(name);
                        }
                    }
                });

                // 기본으로 첫 학생 선택
                studentCombo.getSelectionModel().selectFirst();
                LmsClient.StudentInfo first = studentCombo.getSelectionModel().getSelectedItem();
                if (first != null) {
                    refreshChatMessages(messageList, currentUserId, first.studentId);
                }

                // 선택이 바뀔 때마다 해당 학생과의 대화만 로딩
                studentCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
                    if (newV != null) {
                        messageList.getItems().clear();
                        refreshChatMessages(messageList, currentUserId, newV.studentId);
                    }
                });

                // 전송 버튼: 현재 선택된 학생에게만 전송
                sendBtn.setOnAction(e -> {
                    String text = inputField.getText().trim();
                    if (text.isEmpty()) return;

                    LmsClient.StudentInfo target = studentCombo.getSelectionModel().getSelectedItem();
                    if (target == null) {
                        warning("대상 선택", "메시지를 보낼 학생을 먼저 선택해 주세요.");
                        return;
                    }
                    try {
                        LmsClient.sendChat(currentUserId, target.studentId, text);
                        inputField.clear();
                        refreshChatMessages(messageList, currentUserId, target.studentId);
                    } catch (IOException ex) {
                        error("채팅 오류", "메시지 전송 중 오류가 발생했습니다.\n" + ex.getMessage());
                    }
                });
            }
        } catch (IOException e) {
            Label err = new Label("학생 목록을 불러오는 중 오류가 발생했습니다:\n" + e.getMessage());
            err.setStyle("-fx-text-fill: #b91c1c; -fx-font-size: 12px;");
            targetBox.getChildren().addAll(targetTitle, err);
        }

        targetBox.getChildren().addAll(targetTitle, help, studentCombo);

    } else {
        // 🔹 학생: 항상 교수(teacher)와만 대화
        final String partnerId = "teacher";

        Label targetNameLabel = new Label("교수: teacher");
        targetNameLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 13px;");
        targetBox.getChildren().addAll(targetTitle, targetNameLabel);

        // 처음 열 때 메시지 로딩
        refreshChatMessages(messageList, currentUserId, partnerId);

        sendBtn.setOnAction(e -> {
            String text = inputField.getText().trim();
            if (text.isEmpty()) return;
            try {
                LmsClient.sendChat(currentUserId, partnerId, text);
                inputField.clear();
                refreshChatMessages(messageList, currentUserId, partnerId);
            } catch (IOException ex) {
                error("채팅 오류", "메시지 전송 중 오류가 발생했습니다.\n" + ex.getMessage());
            }
        });
    }

    layout.getChildren().addAll(targetBox, chatBox);
    HBox.setHgrow(chatBox, Priority.ALWAYS);

    panel.getChildren().add(layout);
    root.getChildren().add(panel);
    return root;
}

    private void refreshChatMessages(ListView<String> listView, String userA, String userB) {
        try {
            List<String> messages = LmsClient.fetchChat(userA, userB);
            listView.setItems(FXCollections.observableArrayList(messages));
            if (!messages.isEmpty()) {
                listView.scrollTo(messages.size() - 1);
            }
        } catch (IOException e) {
            listView.setItems(FXCollections.observableArrayList(
                    "채팅 목록을 불러오는 중 오류가 발생했습니다:",
                    e.getMessage()
            ));
        }
    }

    // -----------------------------
    // 교수 전용 학생 정보 패널
    // -----------------------------
    private VBox buildStudentPanel() {
        VBox root = new VBox(18);
        VBox panel = createMainPanelWrapper("학생 정보");

        VBox list = new VBox(8);

        if (!"TEACHER".equals(currentRole)) {
            Label msg = new Label("학생 정보는 교사만 조회할 수 있습니다.");
            msg.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 13px;");
            list.getChildren().add(msg);
        } else {
            try {
                List<LmsClient.StudentInfo> students =
                        LmsClient.fetchStudents(currentUserId);
                if (students.isEmpty()) {
                    Label empty = new Label("등록된 학생 정보가 없습니다.");
                    empty.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 13px;");
                    list.getChildren().add(empty);
                } else {
                    for (LmsClient.StudentInfo s : students) {
                        HBox row = new HBox(10);
                        row.setAlignment(Pos.CENTER_LEFT);
                        row.setPadding(new Insets(6, 0, 6, 0));

                        Label name = new Label(
                                (s.name == null || s.name.isBlank()) ? s.studentId : s.name + " (" + s.studentId + ")"
                        );
                        name.setPrefWidth(250);

                        Label contact = new Label(s.contact != null ? s.contact : "");
                        contact.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");

                        row.getChildren().addAll(name, contact);
                        list.getChildren().add(row);
                    }
                }
            } catch (IOException e) {
                Label err = new Label("학생 정보를 불러오는 중 오류가 발생했습니다:\n" + e.getMessage());
                err.setStyle("-fx-text-fill: #b91c1c; -fx-font-size: 12px;");
                list.getChildren().add(err);
            }
        }

        panel.getChildren().add(list);
        root.getChildren().add(panel);
        return root;
    }

    // -----------------------------
    // 공통 Alert 유틸
    // -----------------------------
    private void info(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void warning(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void error(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private boolean confirm(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("확인");
        a.setHeaderText(null);
        a.setContentText(msg);
        return a.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }
}
