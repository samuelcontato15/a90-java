package com.a90;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Random;

/**
 * Popup da fase 3 — o "RansomNotification" original.
 *
 * Mostra: face idle do A-90 + contador de valor restante + cronômetro.
 * É o alvo onde as moedas são arrastadas (CoinSprite.checkDrop faz overlap aqui).
 * GlitchIdle: tremula ±5px para ser assustador.
 */
public class RansomWindow extends Stage {

    private final Label     lblRansom;
    private final Label     lblTimer;
    private final Label     lblResult;
    private final Timeline  glitch;
    private       double    baseX, baseY;
    private final Random    rng = new Random();

    public RansomWindow() {
        initStyle(StageStyle.UNDECORATED);
        setAlwaysOnTop(true);
        setResizable(false);

        // --- A-90 idle pequeno ---
        ImageView iv = new ImageView(Assets.loadImage("ransom_idle.png"));
        iv.setFitWidth(120);
        iv.setFitHeight(120);
        iv.setPreserveRatio(true);

        // --- labels ---
        Label lTitle  = label("RANS0M",   "-fx-font-size:13; -fx-text-fill:#ff4444;");
        lblRansom     = label("",         "-fx-font-size:22; -fx-text-fill:#ffaa00; -fx-font-weight:bold;");
        lblTimer      = label("",         "-fx-font-size:34; -fx-text-fill:#ff2222; -fx-font-weight:bold;");
        lblResult     = label("",         "-fx-font-size:15; -fx-text-fill:#ffffff; -fx-font-weight:bold;");
        Label lHint   = label("[ESC para sair]", "-fx-font-size:8; -fx-text-fill:#333333;");
        Label lDrop   = label("↓  jogue as moedas aqui  ↓",
                              "-fx-font-size:9; -fx-text-fill:#555555;");

        // bindings direto ao GameEngine
        lblRansom.textProperty().bind(
            Bindings.format("DÉBITO:  %d", GameEngine.ransomLeft));
        lblTimer.textProperty().bind(
            Bindings.format("%02d", GameEngine.timeLeft));

        // timer vira vermelho pulsante <= 10s
        GameEngine.timeLeft.addListener((obs, o, n) -> {
            String c = n.intValue() <= 10 ? "#ff0000" : "#ff2222";
            lblTimer.setStyle(lblTimer.getStyle().replaceAll("-fx-text-fill:[^;]+", "-fx-text-fill:" + c));
        });

        VBox content = new VBox(4, lTitle, iv, lblRansom, lblTimer, lDrop, lblResult, lHint);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(12, 20, 12, 20));
        content.setStyle(
            "-fx-background-color:#080808;" +
            "-fx-border-color:#cc0000;" +
            "-fx-border-width:2;");

        Scene scene = new Scene(content);
        scene.setFill(Color.TRANSPARENT);
        scene.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ESCAPE) GameEngine.forceExit(); });
        setScene(scene);

        // Posição: canto direito, 1/3 do topo
        javafx.geometry.Rectangle2D b = javafx.stage.Screen.getPrimary().getVisualBounds();
        baseX = b.getMaxX() - 220;
        baseY = b.getMinY() + b.getHeight() * 0.3;
        setX(baseX);
        setY(baseY);

        // GlitchIdle ±5px
        glitch = new Timeline(new KeyFrame(javafx.util.Duration.millis(200), e -> {
            setX(baseX + (rng.nextDouble() * 2 - 1) * 5);
            setY(baseY + (rng.nextDouble() * 2 - 1) * 5);
        }));
        glitch.setCycleCount(Timeline.INDEFINITE);
    }

    /** Mostra a janela e inicia o GlitchIdle. */
    public void launch() {
        show();
        glitch.play();
    }

    public void showResult(boolean won) {
        glitch.stop();
        setX(baseX);
        setY(baseY);
        lblResult.setText(won ? "✓  PAGO!" : "✗  TEMPO ESGOTADO");
        lblResult.setStyle(won
            ? "-fx-font-size:15; -fx-text-fill:#00ff88; -fx-font-weight:bold;"
            : "-fx-font-size:15; -fx-text-fill:#ff0000; -fx-font-weight:bold;");
    }

    @Override
    public void close() {
        glitch.stop();
        super.close();
    }

    private static Label label(String txt, String style) {
        Label l = new Label(txt);
        l.setStyle("-fx-font-family:'Courier New'; " + style);
        return l;
    }
}
