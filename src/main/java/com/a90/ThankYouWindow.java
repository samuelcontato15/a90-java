package com.a90;

import javafx.animation.*;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class ThankYouWindow extends Stage {

    public ThankYouWindow(double fromX, double fromY) {
        initStyle(StageStyle.UNDECORATED);
        setAlwaysOnTop(true);
        setResizable(false);
        Assets.setIcon(this, Assets.APP_ICON);

        ImageView ivOkSign = new ImageView(Assets.loadImage("ok_sign.png"));
        ivOkSign.setFitWidth(200);
        ivOkSign.setFitHeight(200);
        ivOkSign.setPreserveRatio(true);
        ivOkSign.setOpacity(0);

        ImageView ivThx = new ImageView(Assets.loadImage("thx_txt.png"));
        ivThx.setFitWidth(260);
        ivThx.setFitHeight(80);
        ivThx.setPreserveRatio(true);
        ivThx.setOpacity(0);

        Scale scaleOk  = new Scale(0.1, 0.1, 100, 100);
        Scale scaleThx = new Scale(0.1, 0.1, 130, 40);
        ivOkSign.getTransforms().add(scaleOk);
        ivThx.getTransforms().add(scaleThx);

        VBox root = new VBox(10, ivOkSign, ivThx);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color:#000000; -fx-padding:20;");

        Scene scene = new Scene(root, 300, 320);
        scene.setFill(Color.BLACK);
        scene.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ESCAPE) GameEngine.forceExit(); });
        Assets.infect(scene);
        setScene(scene);

        setX(fromX);
        setY(fromY);
        playAnimation(fromX, fromY, scaleOk, scaleThx, ivOkSign, ivThx);
    }

    private void playAnimation(double fromX, double fromY,
                                Scale scaleOk, Scale scaleThx,
                                ImageView ivOkSign, ImageView ivThx) {
        Rectangle2D b  = Screen.getPrimary().getVisualBounds();
        double centerX = b.getMinX() + (b.getWidth()  - 300) / 2;
        double centerY = b.getMinY() + (b.getHeight() - 320) / 2;

        PauseTransition step1 = new PauseTransition(Duration.millis(200));
        step1.setOnFinished(e -> {
            setX(lerp(fromX, centerX, 0.5));
            setY(lerp(fromY, centerY, 0.5));
        });

        PauseTransition step2 = new PauseTransition(Duration.millis(100));
        step2.setOnFinished(e -> {
            setX(centerX);
            setY(centerY);
        });

        PauseTransition step3 = new PauseTransition(Duration.millis(200));
        step3.setOnFinished(e -> {
            ivOkSign.setOpacity(1);
            Assets.playSound("thankyou.wav");
            growAnim(scaleOk, 300).play();
        });

        PauseTransition step4 = new PauseTransition(Duration.millis(400));
        step4.setOnFinished(e -> {
            ivThx.setOpacity(1);
            growAnim(scaleThx, 200).play();
        });

        PauseTransition close = new PauseTransition(Duration.seconds(4));
        close.setOnFinished(e -> this.close());

        new SequentialTransition(step1, step2, step3, step4, close).play();
    }

    private static Timeline growAnim(Scale scale, int durationMs) {
        return new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(scale.xProperty(), 0.1),
                new KeyValue(scale.yProperty(), 0.1)),
            new KeyFrame(Duration.millis(durationMs),
                new KeyValue(scale.xProperty(), 1.0, Interpolator.EASE_OUT),
                new KeyValue(scale.yProperty(), 1.0, Interpolator.EASE_OUT))
        );
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
