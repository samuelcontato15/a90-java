package com.a90;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.List;
import java.util.Random;

/**
 * Janela de taunt — glitch popup aleatório.
 * Título aleatório, imagem aleatória, tamanho 200-400, GlitchIdle ±5px.
 * Fecha sozinha após 4-10s com som tauntLeave.
 * 2% de chance a cada 200ms de spawnar mais uma TauntWindow (divideAndTaunt).
 */
public class TauntWindow extends Stage {

    private static final List<String> TITLES = List.of(
        "RANS0M", "MOSNAR", "RANSOM", "M0NARS", "YOU ARE AN IDIOT",
        "Untitled", "Untitled (3)", "I FOUND YOU", "RANSOM.exe",
        "RAANNNSSSSOOOOOMMMMMM", "times up", "GIVE MONEY",
        "ERROR", "DHAUFGH", "_________", "IMG.JPG"
    );
    private static final List<String> IMAGES = List.of(
        "Taunts/glitch1.jpg", "Taunts/glitch2.jpeg", "Taunts/glitch3.jpg",
        "Taunts/glitch4.jpg", "Taunts/glitch5.jpg",
        "Taunts/idiot.png", "Taunts/tauntface.png", "Taunts/tauntflower.png"
    );

    private static final Random RNG = new Random();
    private Timeline glitch;
    private boolean  closed = false;

    public TauntWindow() {
        // Decorated — igual ao original (com barra de título)
        initStyle(StageStyle.DECORATED);
        setAlwaysOnTop(true);
        setResizable(false);

        double w = 200 + RNG.nextInt(200);
        double h = 200 + RNG.nextInt(200);

        // Título e imagem aleatórios
        setTitle(TITLES.get(RNG.nextInt(TITLES.size())));
        Image img = Assets.loadImage(IMAGES.get(RNG.nextInt(IMAGES.size())));

        Rectangle bg = new Rectangle(w, h);
        if (img != null) bg.setFill(new ImagePattern(img));
        else             bg.setFill(Color.BLACK);

        Pane root = new Pane(bg);
        Scene scene = new Scene(root, w, h);
        scene.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ESCAPE) GameEngine.forceExit(); });
        setScene(scene);

        // Posição aleatória
        Rectangle2D b = Screen.getPrimary().getVisualBounds();
        setX(RNG.nextDouble() * Math.max(0, b.getWidth()  - w));
        setY(RNG.nextDouble() * Math.max(0, b.getHeight() - h));

        setOnCloseRequest(e -> {
            closed = true;
            if (glitch != null) glitch.stop();
        });
    }

    /** Mostra a janela e inicia glitch + timer de fechamento. */
    public void launch() {
        show();
        Assets.playSound("tauntSpawn.wav");

        double bX = getX(), bY = getY();

        // GlitchIdle ±5px, com 2% de chance de spawnar outra janela
        glitch = new Timeline(new KeyFrame(Duration.millis(200), e -> {
            if (closed) return;
            setX(bX + (RNG.nextDouble() * 2 - 1) * 5);
            setY(bY + (RNG.nextDouble() * 2 - 1) * 5);
            if (RNG.nextInt(100) < 2) {
                TauntWindow extra = new TauntWindow();
                extra.launch();
            }
        }));
        glitch.setCycleCount(Timeline.INDEFINITE);
        glitch.play();

        // Fecha após 4-10 segundos
        int delay = 4000 + RNG.nextInt(6000);
        PauseTransition closeTimer = new PauseTransition(Duration.millis(delay));
        closeTimer.setOnFinished(ev -> {
            if (!closed) {
                Assets.playSound("tauntLeave.wav");
                glitch.stop();
                close();
            }
        });
        closeTimer.play();
    }

    @Override
    public void close() {
        closed = true;
        if (glitch != null) glitch.stop();
        super.close();
    }
}
