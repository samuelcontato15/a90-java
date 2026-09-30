package com.a90;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Entry point.
 * Registra shutdown hook (wallpaper restaurado mesmo em crash/kill).
 * Mostra tela de início com botão "INICIAR" e engrenagem de configuração.
 */
public class App extends Application {

    private Stage startStage;

    @Override
    public void start(Stage primaryStage) {
        // Trava de segurança: wallpaper volta mesmo se o processo for morto
        Runtime.getRuntime().addShutdownHook(new Thread(WallpaperManager::restore,
                "wallpaper-restore-hook"));

        Platform.setImplicitExit(false);
        GameConfig.load();
        showStartScreen(new Stage());
        showStartScreen(primaryStage);
    }

    private void showStartScreen(Stage stage) {
        startStage = stage;

        Label title = label("A-90 MINIGAME",
            "-fx-font-size:28; -fx-text-fill:#ff2222; -fx-font-weight:bold;");
        Label sub = label("inspirado em DOORS — Archives",
            "-fx-font-size:10; -fx-text-fill:#555555;");
        Label hint = label("jogue 7 moedas no A-90 antes do tempo acabar",
            "-fx-font-size:10; -fx-text-fill:#444444;");

        Button btnStart  = btn("[ INICIAR ]",       "#cc0000", "#ffffff");
        Button btnConfig = btn("[ configuração ]",  "#1a1a1a", "#555555");

        btnStart.setOnAction(e -> {
            stage.close();
            GameEngine.start();
        });
        btnConfig.setOnAction(e -> new ConfigWindow(stage).showAndWait());

        VBox root = new VBox(10, title, sub, hint, btnStart, btnConfig);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color:#060606; -fx-padding:40;");

        Scene scene = new Scene(root);
        scene.setFill(Color.BLACK);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(scene);
        stage.setAlwaysOnTop(true);
        stage.setResizable(false);
        stage.setTitle("A-90");
        stage.show();
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
