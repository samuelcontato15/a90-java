package com.a90;

import javafx.animation.*;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * Brilho (Starlight.png) que estoura no ponto onde a moeda foi entregue ao A-90.
 * Cresce girando e some em ~450ms; fecha sozinho.
 */
public class StarlightBurst extends Stage {

    public StarlightBurst(double centerX, double centerY, double size) {
        initStyle(StageStyle.TRANSPARENT);
        setAlwaysOnTop(true);
        setResizable(false);

        ImageView star = new ImageView(Assets.loadImage("Starlight.png"));
        star.setFitWidth(size);
        star.setFitHeight(size);
        star.setPreserveRatio(true);
        star.setMouseTransparent(true);

        StackPane root = new StackPane(star);
        root.setStyle("-fx-background-color:transparent;");
        setScene(new Scene(root, size, size, Color.TRANSPARENT));
        setX(centerX - size / 2);
        setY(centerY - size / 2);

        ScaleTransition grow = new ScaleTransition(Duration.millis(450), star);
        grow.setFromX(0.2);
        grow.setFromY(0.2);
        grow.setToX(1.0);
        grow.setToY(1.0);
        grow.setInterpolator(Interpolator.EASE_OUT);

        RotateTransition spin = new RotateTransition(Duration.millis(450), star);
        spin.setByAngle(90);

        FadeTransition fade = new FadeTransition(Duration.millis(250), star);
        fade.setDelay(Duration.millis(200));
        fade.setFromValue(1);
        fade.setToValue(0);

        ParallelTransition burst = new ParallelTransition(grow, spin, fade);
        burst.setOnFinished(e -> close());
        burst.play();
    }
}
