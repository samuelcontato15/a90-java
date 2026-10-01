package com.a90;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.List;
import java.util.Random;

class RansomBackground extends Stage {

    private static final List<String> CRASH_IMAGES = List.of(
        "Taunts/glitch1.jpg", "Taunts/glitch2.jpeg", "Taunts/glitch3.jpg",
        "Taunts/glitch4.jpg", "Taunts/glitch5.jpg", "static.gif"
    );
    private static final Random RNG = new Random();

    private final Timeline cycle;

    RansomBackground() {
        initStyle(StageStyle.TRANSPARENT);
        setAlwaysOnTop(true);
        setResizable(false);

        Rectangle2D full = Screen.getPrimary().getBounds();

        ImageView iv = new ImageView(Assets.loadImage(CRASH_IMAGES.get(0)));
        iv.setFitWidth(full.getWidth());
        iv.setFitHeight(full.getHeight());
        iv.setPreserveRatio(false);

        StackPane root = new StackPane(iv);
        root.setBackground(new Background(new BackgroundFill(Color.BLACK, null, null)));

        setScene(new Scene(root, full.getWidth(), full.getHeight(), Color.TRANSPARENT));
        setX(full.getMinX());
        setY(full.getMinY());

        cycle = new Timeline(new KeyFrame(Duration.millis(200),
            e -> iv.setImage(Assets.loadImage(CRASH_IMAGES.get(RNG.nextInt(CRASH_IMAGES.size()))))));
        cycle.setCycleCount(Timeline.INDEFINITE);
    }

    void launch() {
        Win32Window.tag(this);
        show();
        Win32Window.clickThrough(this);
        cycle.play();
    }

    @Override
    public void close() {
        cycle.stop();
        super.close();
    }
}
