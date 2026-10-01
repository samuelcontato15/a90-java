package com.a90;

import com.sun.jna.platform.win32.User32;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Background;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.function.Consumer;

public class CoinSprite extends Stage {

    private static final int SM_SWAPBUTTON = 23;
    private static final int VK_LBUTTON = 0x01, VK_RBUTTON = 0x02;
    private static Robot robot;

    public final CoinType type;
    private double  dragOffsetX, dragOffsetY;
    private boolean held;
    private final Consumer<CoinSprite> onDropped;
    private final Timeline follow;

    public CoinSprite(double x, double y, CoinType type, Consumer<CoinSprite> onDropped) {
        this.type      = type;
        this.onDropped = onDropped;

        initStyle(StageStyle.TRANSPARENT);
        setAlwaysOnTop(true);
        setResizable(false);
        Assets.setIcon(this, type.image);
        Win32Window.tag(this);

        double size = coinSize(type);
        ImageView iv = new ImageView(Assets.loadImage(type.image));
        iv.setFitWidth(size);
        iv.setFitHeight(size);
        iv.setPreserveRatio(true);

        Label lbl = new Label(type.label());
        lbl.setStyle("-fx-font-family:'Courier New'; -fx-font-size:11; -fx-font-weight:bold; " +
                     "-fx-text-fill:" + type.labelColor() + "; " +
                     "-fx-effect:dropshadow(gaussian,black,3,1,0,0);");

        StackPane root = new StackPane(iv, lbl);
        StackPane.setAlignment(lbl, Pos.BOTTOM_CENTER);
        root.setBackground(Background.EMPTY);

        double wSize = size + 8;
        Scene scene = new Scene(root, wSize, wSize, Color.TRANSPARENT);

        follow = new Timeline(new KeyFrame(Duration.millis(16), e -> followPointer()));
        follow.setCycleCount(Animation.INDEFINITE);

        scene.setOnMousePressed(e -> {
            if (!e.isPrimaryButtonDown()) return;
            dragOffsetX = e.getSceneX();
            dragOffsetY = e.getSceneY();
            held = true;
            Win32Window.raise(this);
            follow.play();
        });
        scene.setOnMouseReleased(e -> release());
        scene.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ESCAPE) GameEngine.forceExit(); });
        Assets.infect(scene);

        setScene(scene);
        setX(x);
        setY(y);
    }

    boolean isHeld() { return held; }
    void jumpTo(double x, double y) {
        setX(x);
        setY(y);
        Win32Window.raise(this);
    }

    private void followPointer() {
        if (!primaryButtonDown()) { release(); return; }
        if (robot == null) robot = new Robot();
        Point2D p = robot.getMousePosition();
        setX(p.getX() - dragOffsetX);
        setY(p.getY() - dragOffsetY);
    }

    private void release() {
        if (!held) return;
        held = false;
        follow.stop();
        checkDrop();
    }

    private void checkDrop() {
        RansomWindow target = GameEngine.getRansomWindow();
        if (target == null || !target.isShowing()) return;
        double cx = getX() + getWidth()  / 2;
        double cy = getY() + getHeight() / 2;
        if (cx >= target.getX() && cx <= target.getX() + target.getWidth() &&
            cy >= target.getY() && cy <= target.getY() + target.getHeight()) {
            onDropped.accept(this);
        }
    }

    private static boolean primaryButtonDown() {
        try {
            boolean swapped = User32.INSTANCE.GetSystemMetrics(SM_SWAPBUTTON) != 0;
            return (User32.INSTANCE.GetAsyncKeyState(swapped ? VK_RBUTTON : VK_LBUTTON) & 0x8000) != 0;
        } catch (Throwable t) {
            return true;
        }
    }

    @Override
    public void close() {
        follow.stop();
        super.close();
    }

    private static double coinSize(CoinType t) {
        return switch (t) {
            case GOLD1    -> 56;
            case GOLD2    -> 64;
            case GOLD3    -> 72;
            case GOLD4    -> 80;
            case GOLD5    -> 90;
            case HONEYPOT -> 90;
        };
    }
}
