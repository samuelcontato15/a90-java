package com.a90;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GlitchOverlay extends Stage {

    private static final double MAX_STATIC    = 0.50;
    private static final double MAX_VIGNETTE  = 0.75;
    private static final double MAX_SCANLINES = 0.35;
    private static final int    MAX_TEARS     = 16;
    private static final int    MAX_CHROMA    = 6;

    private static final int FLASH_GAP_TICKS = 7;

    private static final List<String> GLITCH_IMAGES = List.of(
        "Taunts/glitch1.jpg", "Taunts/glitch2.jpeg", "Taunts/glitch3.jpg",
        "Taunts/glitch4.jpg", "Taunts/glitch5.jpg"
    );
    private static final List<String> FACES = List.of(
        "Taunts/tauntface.png", "Taunts/idiot.png", "Taunts/tauntflower.png", "ransom_idle.png"
    );

    private static final Random RNG = new Random();

    private final double w, h;
    private final Pane            staticLayer;
    private final double          tileW, tileH;
    private final ImageView       vignette;
    private final Pane            tearPane;
    private final Pane            chromaPane;
    private final Region          scanlines;
    private final Region          invertFlash;
    private final ImageView       faceFlash;
    private final List<ImageView> tears  = new ArrayList<>();
    private final List<Rectangle> chroma = new ArrayList<>();
    private final List<Image>     sources = new ArrayList<>();
    private final Timeline        tick;

    private double intensity;
    private double burst;
    private int    ticksToTop;
    private int    flashCooldown;

    public GlitchOverlay() {
        initStyle(StageStyle.TRANSPARENT);
        setAlwaysOnTop(true);
        setResizable(false);
        Win32Window.tag(this);

        Rectangle2D b = Screen.getPrimary().getVisualBounds();
        w = b.getWidth();
        h = b.getHeight();

        for (String name : GLITCH_IMAGES) {
            Image img = Assets.loadImage(name);
            if (img != null && !img.isError()) sources.add(img);
        }

        Image tile = Assets.loadImage("static.gif");
        tileW = tile != null && tile.getWidth()  > 0 ? tile.getWidth()  : w;
        tileH = tile != null && tile.getHeight() > 0 ? tile.getHeight() : h;
        staticLayer = new Pane();
        staticLayer.setPrefSize(w, h);
        staticLayer.setOpacity(0);
        staticLayer.setMouseTransparent(true);
        for (double ty = -tileH; ty < h + tileH; ty += tileH) {
            for (double tx = -tileW; tx < w + tileW; tx += tileW) {
                ImageView cell = new ImageView(tile);
                cell.setSmooth(false);
                cell.setLayoutX(tx);
                cell.setLayoutY(ty);
                staticLayer.getChildren().add(cell);
            }
        }

        vignette = fullScreen(Assets.loadVignette());
        vignette.setOpacity(0);

        tearPane   = new Pane();
        chromaPane = new Pane();
        tearPane.setPrefSize(w, h);
        chromaPane.setPrefSize(w, h);

        for (int i = 0; i < MAX_TEARS; i++) {
            ImageView iv = new ImageView();
            iv.setVisible(false);
            iv.setPreserveRatio(false);
            iv.setSmooth(false);
            tears.add(iv);
            tearPane.getChildren().add(iv);
        }
        for (int i = 0; i < MAX_CHROMA; i++) {
            Rectangle r = new Rectangle();
            r.setVisible(false);
            chroma.add(r);
            chromaPane.getChildren().add(r);
        }

        scanlines = new Region();
        scanlines.setPrefSize(w, h);
        scanlines.setStyle("-fx-background-color: linear-gradient("
                + "from 0px 0px to 0px 3px, repeat, "
                + "rgba(0,0,0,0.9) 0%, rgba(0,0,0,0.9) 49%, transparent 51%, transparent 100%);");
        scanlines.setOpacity(0);

        invertFlash = new Region();
        invertFlash.setPrefSize(w, h);
        invertFlash.setStyle("-fx-background-color: white;");
        invertFlash.setBlendMode(BlendMode.DIFFERENCE);
        invertFlash.setOpacity(0);

        faceFlash = new ImageView();
        faceFlash.setPreserveRatio(true);
        faceFlash.setFitHeight(h * 0.8);
        faceFlash.setOpacity(0);

        StackPane root = new StackPane(vignette, staticLayer, tearPane, chromaPane,
                                       scanlines, faceFlash, invertFlash);
        root.setBackground(Background.EMPTY);
        root.setPrefSize(w, h);
        root.setMouseTransparent(true);

        Scene scene = new Scene(root, w, h, Color.TRANSPARENT);
        setScene(scene);
        setX(b.getMinX());
        setY(b.getMinY());

        tick = new Timeline(new KeyFrame(Duration.millis(60), e -> paint()));
        tick.setCycleCount(Animation.INDEFINITE);
    }

    public void launch() {
        show();
        Win32Window.clickThrough(this);
        tick.play();
    }

    public void setIntensity(double t) {
        intensity = Math.clamp(t, 0, 1);
    }

    public void burst() {
        burst = 1.0;
        Assets.playSound("tauntSpawn.wav");
    }

    private void paint() {
        double k = Math.min(1.0, Math.pow(intensity, 1.6) + burst);
        burst = Math.max(0, burst - 0.12);

        if (--ticksToTop <= 0) {
            ticksToTop = 8;
            Win32Window.raise(this);
        }

        paintStatic(k);
        vignette.setOpacity(MAX_VIGNETTE * k * (0.7 + 0.3 * RNG.nextDouble()));
        scanlines.setOpacity(MAX_SCANLINES * k);

        paintTears(k);
        paintChroma(k);

        if (flashCooldown > 0) {
            flashCooldown--;
            invertFlash.setOpacity(0);
            return;
        }
        if (RNG.nextDouble() < 0.10 * k) {
            invertFlash.setOpacity(0.55 + 0.45 * RNG.nextDouble());
            flashCooldown = FLASH_GAP_TICKS;
        } else {
            invertFlash.setOpacity(0);
            if (k > 0.55 && RNG.nextDouble() < 0.08) {
                flashFace();
                flashCooldown = FLASH_GAP_TICKS;
            }
        }
    }

    private void paintStatic(double k) {
        staticLayer.setOpacity(MAX_STATIC * k);
        staticLayer.setTranslateX(-RNG.nextDouble() * tileW);
        staticLayer.setTranslateY(-RNG.nextDouble() * tileH);
    }

    private void paintTears(double k) {
        int active = (int) Math.round(MAX_TEARS * k);
        for (int i = 0; i < tears.size(); i++) {
            ImageView iv = tears.get(i);
            if (i >= active || sources.isEmpty() || RNG.nextDouble() < 0.25) {
                iv.setVisible(false);
                continue;
            }
            Image src = sources.get(RNG.nextInt(sources.size()));

            double sliceH = 8 + RNG.nextDouble() * 90;
            double sliceY = RNG.nextDouble() * Math.max(1, src.getHeight() - sliceH);
            iv.setImage(src);
            iv.setViewport(new Rectangle2D(0, sliceY, src.getWidth(), sliceH));

            double barH = 6 + RNG.nextDouble() * 70 * k;
            iv.setFitWidth(w * 1.35);
            iv.setFitHeight(barH);
            iv.setLayoutX(-w * 0.175 + (RNG.nextDouble() * 2 - 1) * 180 * k);
            iv.setLayoutY(RNG.nextDouble() * h);
            iv.setOpacity(0.25 + 0.6 * RNG.nextDouble() * k);
            iv.setBlendMode(switch (RNG.nextInt(4)) {
                case 0  -> BlendMode.DIFFERENCE;
                case 1  -> BlendMode.SCREEN;
                case 2  -> BlendMode.MULTIPLY;
                default -> BlendMode.SRC_OVER;
            });
            iv.setVisible(true);
        }
    }

    private void paintChroma(double k) {
        int active = (int) Math.round(MAX_CHROMA * k);
        for (int i = 0; i < chroma.size(); i++) {
            Rectangle r = chroma.get(i);
            if (i >= active || RNG.nextDouble() < 0.4) { r.setVisible(false); continue; }
            r.setWidth(w);
            r.setHeight(2 + RNG.nextDouble() * 26 * k);
            r.setLayoutX((RNG.nextDouble() * 2 - 1) * 60 * k);
            r.setLayoutY(RNG.nextDouble() * h);
            r.setFill(RNG.nextBoolean() ? Color.web("#ff0033") : Color.web("#00ffe1"));
            r.setBlendMode(BlendMode.SCREEN);
            r.setOpacity(0.12 + 0.35 * RNG.nextDouble() * k);
            r.setVisible(true);
        }
    }

    private void flashFace() {
        Image face = Assets.loadImage(FACES.get(RNG.nextInt(FACES.size())));
        if (face == null || face.isError()) return;
        faceFlash.setImage(face);
        faceFlash.setBlendMode(RNG.nextBoolean() ? BlendMode.DIFFERENCE : BlendMode.SCREEN);
        faceFlash.setOpacity(0.5 + 0.5 * RNG.nextDouble());
        PauseTransition off = new PauseTransition(Duration.millis(60 + RNG.nextInt(90)));
        off.setOnFinished(e -> faceFlash.setOpacity(0));
        off.play();
    }

    private ImageView fullScreen(Image img) {
        ImageView iv = new ImageView(img);
        iv.setFitWidth(w);
        iv.setFitHeight(h);
        iv.setPreserveRatio(false);
        iv.setMouseTransparent(true);
        return iv;
    }

    @Override
    public void close() {
        tick.stop();
        super.close();
    }
}
