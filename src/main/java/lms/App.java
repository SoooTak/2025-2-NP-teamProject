package lms;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.stage.FileChooser;

public class App extends Application {

    private Stage primaryStage;
    private Scene loginScene;
    private Scene mainScene;

    // 로그인 후 현재 사용자 역할 (STUDENT / TEACHER)
    private String currentRole;
    private String currentUserName;
    private String currentUserId;

    // 메인 패널 컨테이너
    private StackPane contentPane;

    // 패널들
    private VBox homePanel;
    private VBox assignmentPanel;
    private VBox noticePanel;
    private VBox videoPanel;
    private VBox chatPanel;

    // 네비게이션 버튼들 (active 스타일 주기)
    private Button homeBtn;
    private Button assignmentBtn;
    private Button noticeBtn;
    private Button videoBtn;
    private Button chatBtn;

    public static void main(String[] args) {
        launch(args);
    }

    // 색상 팔레트 (HTML과 유사)
    private static final String BG = "#eef2ff";
    private static final String SURFACE = "#ffffff";
    private static final String TEXT = "#111a35";
    private static final String MUTED = "#6b7a99";
    private static final String PRIMARY = "#2563eb";
    private static final String BORDER = "#d7deee";
    private static final String SIDEBAR_BG = "#101323";

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

        VBox card = new VBox(14);
        card.setPadding(new Insets(24));
        card.setStyle(
                "-fx-background-color: " + SURFACE + ";" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-radius: 20;" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.45), 24, 0, 0, 12);"
        );
        card.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("네트워크 프로그래밍\nMini-LMS 로그인");
        title.setFont(Font.font("Segoe UI Semibold", 20));
        title.setStyle("-fx-text-fill: " + TEXT + ";");

        Label sub = new Label("테스트 계정\n학생: student / 1234\n교사: teacher / 1234");
        sub.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px;");

        VBox form = new VBox(8);
        Label idLabel = new Label("아이디");
        idLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");
        TextField idField = new TextField();
        idField.setPromptText("student 또는 teacher");

        Label pwLabel = new Label("비밀번호");
        pwLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");
        PasswordField pwField = new PasswordField();
        pwField.setPromptText("예: 1234");

        styleTextField(idField);
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

        VBox textBox = new VBox(6, title, sub);
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
    // 로그인 처리 (서버 연동)
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
                currentRole = result.role;               // "STUDENT" / "TEACHER"
                currentUserName = result.displayName;    // ex) "김민상"
                currentUserId = id.trim();               // 실제 로그인 아이디 저장

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
    // 2) 메인 LMS 화면
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

        // 패널 생성
        homePanel = buildHomePanel();
        assignmentPanel = buildAssignmentPanel();
        noticePanel = buildNoticePanel();
        videoPanel = buildVideoPanel();
        chatPanel = buildChatPanel();

        contentPane.getChildren().addAll(homePanel, assignmentPanel, noticePanel, videoPanel, chatPanel);
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
        styleNavButtonState(homeBtn, homePanel.isVisible());
        styleNavButtonState(assignmentBtn, assignmentPanel.isVisible());
        styleNavButtonState(noticeBtn, noticePanel.isVisible());
        styleNavButtonState(videoBtn, videoPanel.isVisible());
        styleNavButtonState(chatBtn, chatPanel.isVisible());
    }

    private void styleNavButtonState(Button btn, boolean active) {
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

    private void showPanel(String name) {
        homePanel.setVisible("home".equals(name));
        assignmentPanel.setVisible("assignments".equals(name));
        noticePanel.setVisible("notices".equals(name));
        videoPanel.setVisible("videos".equals(name));
        chatPanel.setVisible("chat".equals(name));

        updateNavButtonStyles();
    }

    // -----------------------------
    // 홈 패널
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

        HBox summaryRow = new HBox(14);
        summaryRow.setFillHeight(true);

        VBox s1 = createSummaryCard("현재 사용자", ("TEACHER".equals(currentRole) ? "교수 · 박교수" : "학생 · 김민상 (예시)"));
        VBox s2 = createSummaryCard("현재 주차", "Week 3");
        VBox s3 = createSummaryCard("등록 과제", "4건");
        VBox s4 = createSummaryCard("강의 영상", "5개");

        HBox.setHgrow(s1, Priority.ALWAYS);
        HBox.setHgrow(s2, Priority.ALWAYS);
        HBox.setHgrow(s3, Priority.ALWAYS);
        HBox.setHgrow(s4, Priority.ALWAYS);

        summaryRow.getChildren().addAll(s1, s2, s3, s4);

        HBox homeColumns = new HBox(18);

        VBox col1 = createHomeBox(
                "최근 과제",
                new String[]{
                        "[1주차] 네트워크 계층 퀴즈",
                        "9.7 마감 · 진행 중",
                        "[3주차] TCP Handshake 보고서",
                        "9.21 마감 · 예정"
                }
        );
        VBox col2 = createHomeBox(
                "최근 공지",
                new String[]{
                        "중간고사 안내",
                        "10.17 온라인 시험",
                        "과제 제출 유의사항",
                        "GitHub 링크+보고서 모두 제출"
                }
        );
        VBox col3 = createHomeBox(
                "최근 강의 영상",
                new String[]{
                        "Week1: TCP 기본",
                        "시청률 82% (예시)",
                        "Week2: UDP와 멀티캐스트",
                        "시청률 40% (예시)"
                }
        );

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
        for (String line : lines) {
            Label l = new Label(line);
            l.setWrapText(true);
            l.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");
            list.getChildren().add(l);
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

    // -----------------------------
    // 과제 패널 (서버 연동 + 제출 버튼)
    // -----------------------------
    private VBox buildAssignmentPanel() {
        VBox root = new VBox(18);
        root.setPadding(new Insets(0));

        VBox panel = createMainPanelWrapper("과제");

        VBox listBox = new VBox(12);

        try {
            // 서버에서 과제 목록 가져오기
            List<LmsClient.Assignment> assignments =
                    LmsClient.fetchAssignments(currentUserId != null ? currentUserId : "student");

            if (assignments.isEmpty()) {
                Label emptyLabel = new Label("등록된 과제가 없습니다.");
                emptyLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 13px;");
                listBox.getChildren().add(emptyLabel);
            } else {
                for (LmsClient.Assignment a : assignments) {
                    VBox card = createAssignmentCardFromData(a);
                    listBox.getChildren().add(card);
                }
            }
        } catch (IOException e) {
            Label errLabel = new Label("과제 목록을 불러오는 중 오류가 발생했습니다:\n" + e.getMessage());
            errLabel.setStyle("-fx-text-fill: #b91c1c; -fx-font-size: 12px;");
            listBox.getChildren().add(errLabel);
        }

        panel.getChildren().add(listBox);
        root.getChildren().add(panel);
        return root;
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

    // 예전 하드코딩 카드 (지금은 사용 X일 수 있지만 남겨둠)
    private VBox createAssignmentCard(String title, String meta, String status) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16));
        card.setStyle(
                "-fx-background-color: #f8faff;" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 18;" +
                        "-fx-background-radius: 18;"
        );

        Label t = new Label(title);
        t.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 16px; -fx-font-weight: 600;");

        Label m = new Label(meta);
        m.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");

        Label s = new Label(status);
        s.setStyle(
                "-fx-text-fill: #b45309;" +
                        "-fx-font-size: 12px;" +
                        "-fx-background-color: rgba(248,113,113,0.15);" +
                        "-fx-padding: 2 8 2 8;" +
                        "-fx-background-radius: 999;"
        );

        card.getChildren().addAll(t, m, s);
        return card;
    }

    // 서버에서 받은 Assignment 데이터 + 제출 버튼 포함 카드
    private VBox createAssignmentCardFromData(LmsClient.Assignment a) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16));
        card.setStyle(
                "-fx-background-color: #f8faff;" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 18;" +
                        "-fx-background-radius: 18;"
        );

        Label t = new Label(a.title);
        t.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 16px; -fx-font-weight: 600;");

        Label m = new Label("마감: " + a.due + "\n" + a.summary);
        m.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");

        Label statusLabel = new Label("제출 가능");
        statusLabel.setStyle(
                "-fx-text-fill: #1d4ed8;" +
                        "-fx-font-size: 12px;" +
                        "-fx-background-color: rgba(59,130,246,0.12);" +
                        "-fx-padding: 2 8 2 8;" +
                        "-fx-background-radius: 999;"
        );

        Button submitBtn = new Button("제출");
        styleOutlineButton(submitBtn);
        submitBtn.setOnAction(e -> openAssignmentSubmitDialog(a));

        HBox bottom = new HBox(8, statusLabel, new Spacer(), submitBtn);
        bottom.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(t, m, bottom);
        return card;
    }

    // 과제 제출: 파일 선택 후 서버로 전송
    private void openAssignmentSubmitDialog(LmsClient.Assignment a) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("과제 파일 선택");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("모든 파일 (*.*)", "*.*")
        );

        File file = fileChooser.showOpenDialog(primaryStage);
        if (file == null) {
            // 사용자가 취소한 경우
            return;
        }
        if (!file.exists() || !file.isFile()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("제출 실패");
            alert.setHeaderText(null);
            alert.setContentText("유효한 파일이 아닙니다.");
            alert.showAndWait();
            return;
        }
        if (file.length() == 0) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("제출 실패");
            alert.setHeaderText(null);
            alert.setContentText("빈 파일은 제출할 수 없습니다.");
            alert.showAndWait();
            return;
        }

        try {
            String userId = (currentUserId != null) ? currentUserId : "student";
            boolean ok = LmsClient.submitAssignmentFile(userId, a.id, file);
            if (ok) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("제출 완료");
                alert.setHeaderText(null);
                alert.setContentText("과제 파일이 정상적으로 제출되었습니다.\n"
                        + "파일명: " + file.getName());
                alert.showAndWait();
            }
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("제출 오류");
            alert.setHeaderText(null);
            alert.setContentText("서버 전송 중 오류가 발생했습니다.\n" + e.getMessage());
            alert.showAndWait();
        }
    }

    // 레이아웃용 스페이서 (HBox 안에서 왼쪽/오른쪽 띄우기용)
    private static class Spacer extends Region {
        public Spacer() {
            HBox.setHgrow(this, Priority.ALWAYS);
        }
    }

    // -----------------------------
    // 공지 패널
    // -----------------------------
    private VBox buildNoticePanel() {
        VBox root = new VBox(18);
        VBox panel = createMainPanelWrapper("공지");

        VBox list = new VBox(12);
        list.getChildren().addAll(
                createNoticeCard("중간고사 안내", "10.17 온라인 시험 · 실습형 문제 포함"),
                createNoticeCard("실습실 사용 안내", "실습실 5실 09~21시 개방 · 팀 예약 가능"),
                createNoticeCard("과제 제출 유의사항", "GitHub 링크와 보고서를 모두 제출해야 합니다.")
        );

        panel.getChildren().add(list);
        root.getChildren().add(panel);
        return root;
    }

    private VBox createNoticeCard(String title, String body) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(16));
        card.setStyle(
                "-fx-background-color: #fdfdff;" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 18;" +
                        "-fx-background-radius: 18;"
        );

        Label t = new Label(title);
        t.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 16px; -fx-font-weight: 600;");

        Label b = new Label(body);
        b.setWrapText(true);
        b.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 13px;");

        card.getChildren().addAll(t, b);
        return card;
    }

    // -----------------------------
    // 강의 영상 패널
    // -----------------------------
    private VBox buildVideoPanel() {
        VBox root = new VBox(18);
        VBox panel = createMainPanelWrapper("강의 영상");

        VBox list = new VBox(12);
        list.getChildren().addAll(
                createVideoCard("Week1: TCP 기본", "길이 16:40 · 마감 9.8", "재생률 82% (예시)"),
                createVideoCard("Week2: UDP와 멀티캐스트", "길이 18:05 · 마감 9.15", "재생률 40% (예시)"),
                createVideoCard("Week3: 소켓 멀티플렉싱", "길이 17:55 · 마감 9.22", "재생률 25% (예시)")
        );

        panel.getChildren().add(list);
        root.getChildren().add(panel);
        return root;
    }

    private VBox createVideoCard(String title, String meta, String progress) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14));
        card.setStyle(
                "-fx-background-color: #fdfdff;" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 18;" +
                        "-fx-background-radius: 18;"
        );

        Label t = new Label(title);
        t.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 15px; -fx-font-weight: 600;");

        Label m = new Label(meta);
        m.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");

        Label p = new Label(progress);
        p.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");

        Button playBtn = new Button("재생 (예시)");
        styleOutlineButton(playBtn);
        playBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("영상 재생");
            alert.setHeaderText(null);
            alert.setContentText("실제 영상 재생 및 시청률 기록은 추후 서버 연동 후 구현 예정입니다.");
            alert.showAndWait();
        });

        HBox cta = new HBox(playBtn);
        cta.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(t, m, p, cta);
        return card;
    }

    // -----------------------------
    // 채팅 패널 (서버 연동)
    // -----------------------------
    private VBox buildChatPanel() {
        VBox root = new VBox(18);
        VBox panel = createMainPanelWrapper("채팅");

        HBox layout = new HBox(16);

        // 학생 -> teacher, 교사 -> student (데모용)
        final String partnerId =
                "TEACHER".equals(currentRole) ? "student" : "teacher";

        // 왼쪽: 대화 상대 표시
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

        Label targetNameLabel = new Label(
                "TEACHER".equals(currentRole) ? "학생: student" : "교수: teacher"
        );
        targetNameLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 13px;");

        targetBox.getChildren().addAll(targetTitle, targetNameLabel);

        // 오른쪽: 메시지 영역
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

        // 초기 메시지 목록 불러오기
        refreshChatMessages(messageList, currentUserId, partnerId);

        HBox inputBox = new HBox(8);
        TextField inputField = new TextField();
        inputField.setPromptText("메시지를 입력하세요");
        styleTextField(inputField);

        Button sendBtn = new Button("전송");
        stylePrimaryButton(sendBtn);

        sendBtn.setOnAction(e -> {
            String text = inputField.getText().trim();
            if (text.isEmpty()) {
                return;
            }

            try {
                // 서버에 메시지 전송
                LmsClient.sendChat(currentUserId, partnerId, text);
                inputField.clear();

                // 전송 후 목록 다시 불러오기
                refreshChatMessages(messageList, currentUserId, partnerId);
            } catch (IOException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("채팅 오류");
                alert.setHeaderText(null);
                alert.setContentText("메시지 전송 중 오류가 발생했습니다.\n" + ex.getMessage());
                alert.showAndWait();
            }
        });

        HBox.setHgrow(inputField, Priority.ALWAYS);
        inputBox.getChildren().addAll(inputField, sendBtn);

        chatBox.getChildren().addAll(messageList, inputBox);

        layout.getChildren().addAll(targetBox, chatBox);
        HBox.setHgrow(chatBox, Priority.ALWAYS);

        panel.getChildren().add(layout);
        root.getChildren().add(panel);
        return root;
    }

    // 채팅 목록 서버에서 불러와 ListView에 적용
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

    private Label createChatTargetLabel(String name) {
        Label l = new Label(name);
        l.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: " + TEXT + ";" +
                        "-fx-padding: 8 10 8 10;" +
                        "-fx-background-radius: 12;"
        );
        return l;
    }
}
