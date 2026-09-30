package com.a90;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Background;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.function.Consumer;

/**
 * Moeda arrastável — janela transparente always-on-top sobre o desktop.
 * Cada tipo tem valor diferente (CoinType). O label mostra o valor em cima da imagem.
 *
 * Drop: ao soltar o mouse, checa sobreposição com RansomWindow → chama onDropped.
 */
public class CoinSprite extends Stage {

    public final CoinType type;
    private double dragOffsetX, dragOffsetY;
    private final Consumer<CoinSprite> onDropped;

    public CoinSprite(double x, double y, CoinType type, Consumer<CoinSprite> onDropped) {
        this.type      = type;
        this.onDropped = onDropped;

        initStyle(StageStyle.TRANSPARENT);
        setAlwaysOnTop(true);
        setResizable(false);

        // --- imagem ---
        double size = coinSize(type);
        ImageView iv = new ImageView(Assets.loadImage(type.image));
        iv.setFitWidth(size);
        iv.setFitHeight(size);
        iv.setPreserveRatio(true);

        // --- label de valor ---
        Label lbl = new Label(type.label());
        lbl.setStyle("-fx-font-family:'Courier New'; -fx-font-size:11; -fx-font-weight:bold; " +
                     "-fx-text-fill:" + type.labelColor() + "; " +
                     "-fx-effect:dropshadow(gaussian,black,3,1,0,0);");

        StackPane root = new StackPane(iv, lbl);
        StackPane.setAlignment(lbl, Pos.BOTTOM_CENTER);
        root.setBackground(Background.EMPTY);

        double wSize = size + 8;
        Scene scene = new Scene(root, wSize, wSize, Color.TRANSPARENT);

        scene.setOnMousePressed(e -> {
            dragOffsetX = e.getSceneX();
            dragOffsetY = e.getSceneY();
        });
        scene.setOnMouseDragged(e -> {
            setX(e.getScreenX() - dragOffsetX);
            setY(e.getScreenY() - dragOffsetY);
        });
        scene.setOnMouseReleased(e -> checkDrop());
        scene.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ESCAPE) GameEngine.forceExit(); });

        setScene(scene);
        setX(x);
        setY(y);
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

    /** Moedas maiores para valores maiores — feedback visual imediato. */
    private static double coinSize(CoinType t) {
        return switch (t) {
            case GOLD1    -> 56;
            case GOLD2    -> 64;
            case GOLD3    -> 72;
            case GOLD4    -> 80;
            case GOLD5    -> 90;
            case HONEYPOT -> 90;
            case CRUCIFIX -> 72;
        };
    }
}
