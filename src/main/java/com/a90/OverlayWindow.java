package com.a90;

import javafx.animation.*;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.Random;

public class OverlayWindow extends Stage {

    private final StackPane root;
    private final StackPane canvas;
    private final ImageView imgIdle;
    private final ImageView imgAttack;
    private final ImageView imgStopSign;
    private final ImageView imgStatic;
    private final ImageView imgVignette;

    private final HBox      downloadPanel;
    private final Label     txtDownload;
    private final RotateTransition cdSpin;
    private final Rectangle[] segments = new Rectangle[10];

    private final Random RNG = new Random();
    private       Timeline shakeTimeline;

    public OverlayWindow() {
        initStyle(StageStyle.TRANSPARENT);
        setAlwaysOnTop(true);
        setResizable(false);
        Assets.setIcon(this, "CD-1.ico");

        Rectangle2D full = Screen.getPrimary().getBounds();

        double jumpH = full.getHeight() * 0.78;
        imgIdle     = makeIV("ransom_idle.png",    220, 220);
        imgAttack   = makeIV("ransom_attack.gif",  jumpH, jumpH);
        imgStopSign = makeIV("stop_sign.png",      200, 200);
        imgStatic   = makeIV("static.gif",         full.getWidth(), full.getHeight());
        imgVignette = new ImageView(Assets.loadVignette());
        imgVignette.setFitWidth(full.getWidth());
        imgVignette.setFitHeight(full.getHeight());

        imgStatic.setOpacity(0);
        imgVignette.setOpacity(0);
        imgIdle.setOpacity(0);
        imgAttack.setOpacity(0);
        imgStopSign.setOpacity(0);

        txtDownload = new Label("DOWNLOADING");
        txtDownload.setStyle("-fx-font-family:'Courier New'; -fx-font-size:28; " +
                             "-fx-text-fill:white; -fx-font-weight:bold;");

        HBox segsBox = new HBox(4);
        segsBox.setAlignment(Pos.CENTER);
        for (int i = 0; i < 10; i++) {
            segments[i] = new Rectangle(60, 40, Color.BLACK);
            segments[i].setStroke(Color.web("#440000"));
            segments[i].setStrokeWidth(2);
            segsBox.getChildren().add(segments[i]);
        }

        ImageView imgCd = makeIV("CD-1.png", 96, 96);
        cdSpin = new RotateTransition(Duration.millis(600), imgCd);
        cdSpin.setByAngle(360);
        cdSpin.setInterpolator(Interpolator.LINEAR);
        cdSpin.setCycleCount(Animation.INDEFINITE);

        downloadPanel = new HBox(12);
        downloadPanel.setAlignment(Pos.CENTER);
        downloadPanel.setOpacity(0);

        VBox downloadVbox = new VBox(8, txtDownload, segsBox);
        downloadVbox.setAlignment(Pos.CENTER);
        downloadPanel.getChildren().addAll(imgCd, downloadVbox);

        canvas = new StackPane(imgStatic, imgVignette, imgIdle, imgAttack, imgStopSign);
        canvas.setPickOnBounds(false);
        canvas.setMouseTransparent(true);

        root = new StackPane(canvas, downloadPanel);
        root.setBackground(Background.EMPTY);
        root.setPrefSize(full.getWidth(), full.getHeight());

        Scene scene = new Scene(root, full.getWidth(), full.getHeight(), Color.TRANSPARENT);
        setScene(scene);
        setX(full.getMinX());
        setY(full.getMinY());
    }

    public void showIdleRandom() {
        Rectangle2D b = Screen.getPrimary().getVisualBounds();
        double x = 40 + RNG.nextDouble() * (b.getWidth()  - 280);
        double y = 40 + RNG.nextDouble() * (b.getHeight() - 280);
        imgIdle.setTranslateX(x - b.getWidth()  / 2 + 100);
        imgIdle.setTranslateY(y - b.getHeight() / 2 + 100);
        imgIdle.setOpacity(1);
    }

    public void showWarningCenter() {
        imgIdle.setTranslateX(0);
        imgIdle.setTranslateY(50);
        imgStopSign.setTranslateX(0);
        imgStopSign.setTranslateY(-80);
        imgStopSign.setOpacity(1);
        imgVignette.setOpacity(1);
        root.setBackground(new Background(new BackgroundFill(
                Color.rgb(40, 0, 0), null, null)));
    }

    public void flashAndHideWarning(boolean mouseMoved) {
        imgStopSign.setOpacity(0);
        imgVignette.setOpacity(0);
        if (mouseMoved) {
            root.setBackground(new Background(new BackgroundFill(
                    Color.rgb(100, 0, 0, 0.6), null, null)));
        }
        imgIdle.setOpacity(0);
        PauseTransition clear = new PauseTransition(Duration.millis(mouseMoved ? 100 : 200));
        clear.setOnFinished(e -> root.setBackground(Background.EMPTY));
        clear.play();
    }

    public void showJumpscare(Runnable onDone) {
        punchIn(1.0);
        shakeTimeline = shake(40, 55);
        shakeTimeline.setOnFinished(e -> {
            imgAttack.setOpacity(0);
            imgStatic.setOpacity(0);
            imgVignette.setOpacity(0);
            root.setBackground(Background.EMPTY);
            if (onDone != null) onDone.run();
        });
        shakeTimeline.play();
    }

    private void punchIn(double startScale) {
        imgAttack.setOpacity(1);
        imgAttack.setTranslateX(0);
        imgAttack.setTranslateY(0);
        imgStatic.setOpacity(0.12);
        imgVignette.setOpacity(1);
        root.setBackground(new Background(new BackgroundFill(
                Color.rgb(120, 0, 0), null, null)));

        ScaleTransition punch = new ScaleTransition(Duration.millis(220), imgAttack);
        punch.setFromX(startScale * 0.55);
        punch.setFromY(startScale * 0.55);
        punch.setToX(startScale * 1.12);
        punch.setToY(startScale * 1.12);
        punch.setInterpolator(Interpolator.EASE_OUT);
        punch.play();
    }

    private Timeline shake(int cycles, double amp) {
        Timeline tl = new Timeline(new KeyFrame(Duration.millis(20), e -> {
            imgAttack.setTranslateX((RNG.nextDouble() * 2 - 1) * amp);
            imgAttack.setTranslateY((RNG.nextDouble() * 2 - 1) * amp);
        }));
        tl.setCycleCount(cycles);
        return tl;
    }

    public void showDownloading(Runnable onDone) {
        Assets.infect(getScene());
        txtDownload.setTranslateX(0);
        downloadPanel.setOpacity(1);
        imgStatic.setOpacity(0.05);
        cdSpin.playFromStart();

        Color segColor = Color.web("#cc0000");

        SequentialTransition seq = new SequentialTransition();
        for (int i = 0; i < 10; i++) {
            final int idx = i;
            PauseTransition pt = new PauseTransition(Duration.millis(120));
            pt.setOnFinished(e -> segments[idx].setFill(segColor));
            seq.getChildren().add(pt);
        }

        Timeline dots = dotBlinkTimeline();
        dots.play();

        Timeline textShake = new Timeline(new KeyFrame(Duration.millis(40), e ->
            txtDownload.setTranslateX((RNG.nextDouble() * 2 - 1) * 5)));
        textShake.setCycleCount(40);
        textShake.play();

        seq.setOnFinished(e -> {
            dots.stop();
            textShake.stop();
            cdSpin.stop();
            PauseTransition hide = new PauseTransition(Duration.millis(200));
            hide.setOnFinished(ev -> {
                downloadPanel.setOpacity(0);
                imgStatic.setOpacity(0);
                root.setBackground(Background.EMPTY);
                resetSegments();
                if (onDone != null) onDone.run();
            });
            hide.play();
        });
        seq.play();
    }

    public void showCrashJumpscare(Runnable onDone) {
        Assets.playSound("attack.wav");
        punchIn(1.15);

        shakeTimeline = shake(50, 70);
        Timeline shake = shakeTimeline;
        shake.setOnFinished(e -> {
            imgAttack.setOpacity(0);
            imgStatic.setOpacity(0);
            imgVignette.setOpacity(0);
            root.setBackground(Background.EMPTY);
            if (onDone != null) onDone.run();
        });
        shake.play();
    }

    public void showFreeze(Runnable onDone) {
        imgAttack.setOpacity(1);
        imgAttack.setTranslateX(0);
        imgAttack.setTranslateY(0);
        imgStatic.setOpacity(0.9);
        imgVignette.setOpacity(1);
        root.setBackground(new Background(new BackgroundFill(Color.rgb(120, 0, 0), null, null)));

        Timeline freeze = new Timeline(new KeyFrame(Duration.millis(50), e -> {
            imgAttack.setTranslateX((RNG.nextDouble() * 2 - 1) * 30);
            imgAttack.setTranslateY((RNG.nextDouble() * 2 - 1) * 15);
            imgStatic.setOpacity(0.5 + RNG.nextDouble() * 0.5);
            imgVignette.setOpacity(0.3 + RNG.nextDouble() * 0.7);
            int r = 60 + RNG.nextInt(120);
            root.setBackground(new Background(new BackgroundFill(Color.rgb(r, RNG.nextInt(15), 0), null, null)));
        }));
        freeze.setCycleCount(40);
        freeze.setOnFinished(e -> {
            imgAttack.setOpacity(0);
            imgStatic.setOpacity(0);
            imgVignette.setOpacity(0);
            imgAttack.setTranslateX(0);
            imgAttack.setTranslateY(0);
            root.setBackground(Background.EMPTY);
            if (onDone != null) onDone.run();
        });

        shakeTimeline = freeze;
        freeze.play();
    }

    private ImageView makeIV(String name, double w, double h) {
        Image img = Assets.loadImage(name);
        ImageView iv = new ImageView(img);
        iv.setFitWidth(w);
        iv.setFitHeight(h);
        iv.setPreserveRatio(true);
        return iv;
    }

    private Timeline dotBlinkTimeline() {
        String[] states = {"DOWNLOADING", "DOWNLOADING.", "DOWNLOADING..", "DOWNLOADING..."};
        int[]    idx    = {0};
        Timeline tl     = new Timeline(new KeyFrame(Duration.millis(120), e -> {
            txtDownload.setText(states[idx[0] % states.length]);
            idx[0]++;
        }));
        tl.setCycleCount(Timeline.INDEFINITE);
        return tl;
    }

    private void resetSegments() {
        for (Rectangle seg : segments) seg.setFill(Color.BLACK);
    }

    @Override
    public void close() {
        if (shakeTimeline != null) shakeTimeline.stop();
        cdSpin.stop();
        super.close();
    }
}
