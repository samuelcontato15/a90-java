package com.a90;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class App extends Application {

    private static boolean killSwitchOk;

    @Override
    public void start(Stage primaryStage) {
        Runtime.getRuntime().addShutdownHook(new Thread(WallpaperManager::restore,
                "wallpaper-restore-hook"));
        WallpaperManager.recoverFromCrash();

        Platform.setImplicitExit(false);
        GameConfig.load();
        Assets.preload();
        killSwitchOk = KillSwitch.register(App::quit);
        GameEngine.onReturnToMenu = () -> showStartScreen(new Stage());
        showStartScreen(primaryStage);
    }

    static void quit() {
        System.exit(0);
    }

    private void showStartScreen(Stage stage) {
        Label title = label("A-90 MINIGAME",
            "-fx-font-size:28; -fx-text-fill:#ff2222; -fx-font-weight:bold;");
        Label sub = label("inspirado em DOORS — Archives",
            "-fx-font-size:10; -fx-text-fill:#555555;");
        Label hint = label("pague o A-90 em 1:30 — arraste as moedas até ele",
            "-fx-font-size:10; -fx-text-fill:#444444;");
        Label mode = label(modeText(),
            "-fx-font-size:10; -fx-text-fill:#886600;");

        Button btnStart  = btn("[ INICIAR ]",       "#cc0000", "#ffffff");
        Button btnConfig = btn("[ configuração ]",  "#1a1a1a", "#555555");
        Button btnExit   = btn("[ sair ]",          "#1a1a1a", "#555555");

        btnStart.setOnAction(e -> {
            stage.close();
            GameEngine.start();
        });
        btnConfig.setOnAction(e -> {
            new ConfigWindow(stage).showAndWait();
            mode.setText(modeText());
            stage.sizeToScene();
        });
        btnExit.setOnAction(e -> quit());

        VBox root = new VBox(10, title, sub, hint, mode, btnStart, btnConfig, btnExit);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color:#060606; -fx-padding:40;");

        Scene scene = new Scene(root);
        scene.setFill(Color.BLACK);
        scene.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ESCAPE) quit(); });
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(scene);
        stage.setAlwaysOnTop(true);
        stage.setResizable(false);
        stage.setTitle("A-90");
        Assets.setIcon(stage, Assets.APP_ICON);
        stage.show();
    }

    private static String modeText() {
        if (GameConfig.mode == GameConfig.Mode.MENU) return "modo: voltar ao menu";
        return "modo: infinito — " + (killSwitchOk
                ? KillSwitch.LABEL + " para parar"
                : "pare pelo Gerenciador de Tarefas");
    }

    private static Label label(String text, String style) {
        Label l = new Label(text);
        l.setStyle("-fx-font-family:'Courier New'; " + style);
        return l;
    }

    private static Button btn(String text, String bg, String fg) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:" + fg + "; " +
                   "-fx-font-family:'Courier New'; -fx-font-size:12; " +
                   "-fx-font-weight:bold; -fx-padding:8 20;");
        return b;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
