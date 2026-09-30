package com.a90;

import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * Janela do crucifix — aparece quando o jogador entrega a moeda Crucifix.
 * Mostra repent.gif (GIF animado) + crucifix.wav.
 * Fecha após 15s ou assim que o GIF terminar (não há callback de fim no JavaFX Image,
 * então usamos o fallback de 15s, igual ao original).
 */
public class CrucifixWindow extends Stage {

    public CrucifixWindow(double fromX, double fromY) {
        initStyle(StageStyle.UNDECORATED);
        setAlwaysOnTop(true);
        setResizable(false);

        ImageView iv = new ImageView(Assets.loadImage("repent.gif"));
        iv.setFitWidth(320);
        iv.setFitHeight(320);
        iv.setPreserveRatio(true);

        StackPane root = new StackPane(iv);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color:#000000; -fx-padding:16;");

        Scene scene = new Scene(root, 340, 340);
        scene.setFill(Color.BLACK);
        scene.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ESCAPE) GameEngine.forceExit(); });
        setScene(scene);

        setX(fromX);
        setY(fromY);

        Assets.playSound("crucifix.wav");

        // Fecha após 15s (igual ao original)
        PauseTransition timer = new PauseTransition(Duration.seconds(15));
        timer.setOnFinished(e -> close());
        timer.play();
    }
}
