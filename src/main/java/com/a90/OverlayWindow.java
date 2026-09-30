package com.a90;

import javafx.animation.*;
import javafx.geometry.Bounds;
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

/**
 * Overlay full-screen transparente — fases 1 e 2.
 *
 * Fase 1 (Warning):  face idle aparece em pos aleatória → centraliza + placa STOP.
 * Fase 2 (Download): jumpscare (img_attack shakendo) → tela "DOWNLOADING..." com
 *                    barra de 10 segmentos acendendo progressivamente.
 * Encerra em fase 3: esconde-se e cede controle ao RansomWindow + CoinSprites.
 */
public class OverlayWindow extends Stage {

    private final StackPane root;
    private final Pane      canvas;     // livre para posicionamento absoluto
    private final ImageView imgIdle;
    private final ImageView imgAttack;
    private final ImageView imgStopSign;
    private final ImageView imgStatic;
    private final ImageView imgVignette;

    // Download bar
    private final HBox      downloadPanel;
    private final Label     txtDownload;
    private final Rectangle[] segments = new Rectangle[10];

    private final Random RNG = new Random();
    private       Timeline shakeTimeline;

    public OverlayWindow() {
        initStyle(StageStyle.TRANSPARENT);
        setAlwaysOnTop(true);
        setResizable(false);

        Rectangle2D full = Screen.getPrimary().getBounds();

        // ---- imagens ----
        imgIdle     = makeIV("ransom_idle.png",    200, 200);
        imgAttack   = makeIV("ransom_attack.gif",  280, 280);
        imgStopSign = makeIV("stop_sign.png",      160, 160);
        imgStatic   = makeIV("static.gif",         full.getWidth(), full.getHeight());
        imgVignette = makeIV("red_vignette.gif",   full.getWidth(), full.getHeight());

        imgStatic.setOpacity(0);
        imgVignette.setOpacity(0);
        imgIdle.setOpacity(0);
        imgAttack.setOpacity(0);
        imgStopSign.setOpacity(0);

        // ---- barra de download ----
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

        downloadPanel = new HBox(12);
        downloadPanel.setAlignment(Pos.CENTER);
        downloadPanel.setOpacity(0);

        VBox downloadVbox = new VBox(8, txtDownload, segsBox);
        downloadVbox.setAlignment(Pos.CENTER);
        downloadPanel.getChildren().add(downloadVbox);

        // ---- canvas e root ----
        canvas = new Pane(imgStatic, imgVignette, imgIdle, imgAttack, imgStopSign);
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

    // ─────────────────── FASE 1: WARNING ───────────────────

    /** Mostra face idle em posição aleatória. */
    public void showIdleRandom() {
        Rectangle2D b = Screen.getPrimary().getVisualBounds();
        double x = 40 + RNG.nextDouble() * (b.getWidth()  - 280);
        double y = 40 + RNG.nextDouble() * (b.getHeight() - 280);
        imgIdle.setTranslateX(x - b.getWidth()  / 2 + 100);
        imgIdle.setTranslateY(y - b.getHeight() / 2 + 100);
        imgIdle.setOpacity(1);
    }

    /** Centraliza face + placa STOP, fundo vermelho escuro. */
    public void showWarningCenter() {
        imgIdle.setTranslateX(0);
        imgIdle.setTranslateY(50);
        imgStopSign.setTranslateX(0);
        imgStopSign.setTranslateY(-80);
        imgStopSign.setOpacity(1);
        root.setBackground(new Background(new BackgroundFill(
                Color.rgb(40, 0, 0), null, null)));
    }

    /** Esconde a fase de warning, flash vermelho breve. */
    public void flashAndHideWarning(boolean mouseMoved) {
        imgStopSign.setOpacity(0);
        if (mouseMoved) {
            root.setBackground(new Background(new BackgroundFill(
                    Color.rgb(100, 0, 0, 0.6), null, null)));
        }
        imgIdle.setOpacity(0);
        PauseTransition clear = new PauseTransition(Duration.millis(mouseMoved ? 100 : 200));
        clear.setOnFinished(e -> root.setBackground(Background.EMPTY));
        clear.play();
    }

    // ─────────────────── FASE 2: JUMPSCARE ───────────────────

    /** Mostra jumpscare com face de ataque tremendo, fundo vermelho. */
    public void showJumpscare(Runnable onDone) {
        imgAttack.setOpacity(1);
        imgAttack.setTranslateX(0);
        imgAttack.setTranslateY(0);
        imgStatic.setOpacity(0.05);
        root.setBackground(new Background(new BackgroundFill(
                Color.rgb(120, 0, 0), null, null)));

        shakeTimeline = new Timeline(new KeyFrame(Duration.millis(20), e -> {
            imgAttack.setTranslateX((RNG.nextDouble() * 2 - 1) * 40);
            imgAttack.setTranslateY((RNG.nextDouble() * 2 - 1) * 40);
        }));
        shakeTimeline.setCycleCount(40); // 800ms
        shakeTimeline.setOnFinished(e -> {
            imgAttack.setOpacity(0);
            if (onDone != null) onDone.run();
        });
        shakeTimeline.play();
    }

    /** Animação "DOWNLOADING..." com 10 segmentos acendendo. */
    public void showDownloading(Runnable onDone) {
        txtDownload.setTranslateX(0);
        downloadPanel.setOpacity(1);
        imgStatic.setOpacity(0.05);

        Color segColor = Color.web("#cc0000");

        // Segmentos acendem 1 a 1 (120ms cada = 1200ms total)
        SequentialTransition seq = new SequentialTransition();
        for (int i = 0; i < 10; i++) {
            final int idx = i;
            PauseTransition pt = new PauseTransition(Duration.millis(120));
            pt.setOnFinished(e -> segments[idx].setFill(segColor));
            seq.getChildren().add(pt);
        }

        // Texto piscando "DOWNLOADING."/"DOWNLOADING.."
        Timeline dots = dotBlinkTimeline();
        dots.play();

        // Shake no texto
        Timeline textShake = new Timeline(new KeyFrame(Duration.millis(40), e ->
            txtDownload.setTranslateX((RNG.nextDouble() * 2 - 1) * 5)));
        textShake.setCycleCount(40);
        textShake.play();

        seq.setOnFinished(e -> {
            dots.stop();
            textShake.stop();
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

    // ─────────────────── LOSE JUMPSCARE ───────────────────

    /** Jumpscare de morte (tempo esgotado). Chama onDone após ~1s. */
    public void showCrashJumpscare(Runnable onDone) {
        Assets.playSound("attack.wav");
        imgAttack.setOpacity(1);
        imgAttack.setTranslateX(0);
        imgAttack.setTranslateY(0);
        imgStatic.setOpacity(0.05);
        root.setBackground(new Background(new BackgroundFill(
                Color.rgb(100, 0, 0), null, null)));

        Timeline shake = new Timeline(new KeyFrame(Duration.millis(20), e -> {
            imgAttack.setTranslateX((RNG.nextDouble() * 2 - 1) * 40);
            imgAttack.setTranslateY((RNG.nextDouble() * 2 - 1) * 40);
        }));
        shake.setCycleCount(50); // 1s
        shake.setOnFinished(e -> {
            imgAttack.setOpacity(0);
            imgStatic.setOpacity(0);
            root.setBackground(Background.EMPTY);
            if (onDone != null) onDone.run();
        });
        shake.play();
    }

    // ─────────────────── helpers ───────────────────

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
        super.close();
    }
}
