package com.a90;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Janela de configuração simples.
 * Não inclui CrashOnDeath / ExecCMD — apenas parâmetros de jogo.
 */
public class ConfigWindow extends Stage {

    public ConfigWindow(Stage owner) {
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.UNDECORATED);
        setResizable(false);

        String labelStyle = "-fx-font-family:'Courier New'; -fx-font-size:11; -fx-text-fill:#cccccc;";
        String fieldStyle = "-fx-background-color:#1a1a1a; -fx-text-fill:#ffaa00; " +
                            "-fx-font-family:'Courier New'; -fx-border-color:#440000; -fx-border-width:1;";

        Label title = new Label("[ CONFIGURAÇÃO ]");
        title.setStyle("-fx-font-family:'Courier New'; -fx-font-size:13; -fx-text-fill:#ff2222; -fx-font-weight:bold;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(10, 0, 10, 0));

        // Duração
        TextField tfDuration = field(String.valueOf(GameConfig.infectionDuration), fieldStyle);
        addRow(grid, 0, "Duração (s):", tfDuration, labelStyle);

        // Valor do ransom
        TextField tfRansom = field(String.valueOf(GameConfig.ransomAmount), fieldStyle);
        addRow(grid, 1, "Valor do ransom:", tfRansom, labelStyle);

        // Min / Max delay de spawn automático
        CheckBox cbAuto = new CheckBox("Spawn automático");
        cbAuto.setSelected(GameConfig.spawnAutomatically);
        cbAuto.setStyle(labelStyle + "-fx-text-fill:#aaaaaa;");

        TextField tfMin = field(String.valueOf(GameConfig.minSpawnDelay), fieldStyle);
        TextField tfMax = field(String.valueOf(GameConfig.maxSpawnDelay), fieldStyle);
        tfMin.setDisable(!GameConfig.spawnAutomatically);
        tfMax.setDisable(!GameConfig.spawnAutomatically);
        cbAuto.setOnAction(e -> {
            tfMin.setDisable(!cbAuto.isSelected());
            tfMax.setDisable(!cbAuto.isSelected());
        });

        addRow(grid, 2, "Delay mín (s):", tfMin, labelStyle);
        addRow(grid, 3, "Delay máx (s):", tfMax, labelStyle);

        // Botões
        Button btnSave   = btn("SALVAR",   "#004400", "#00cc00");
        Button btnCancel = btn("CANCELAR", "#440000", "#ff4444");

        btnSave.setOnAction(e -> {
            GameConfig.infectionDuration   = clamp(tfDuration.getText(), GameConfig.infectionDuration, 5, 300);
            GameConfig.ransomAmount        = clamp(tfRansom.getText(),   GameConfig.ransomAmount, 50, 5000);
            GameConfig.spawnAutomatically  = cbAuto.isSelected();
            GameConfig.minSpawnDelay       = clamp(tfMin.getText(), GameConfig.minSpawnDelay, 1, 3600);
            GameConfig.maxSpawnDelay       = clamp(tfMax.getText(), GameConfig.maxSpawnDelay, 1, 3600);
            GameConfig.save();
            close();
        });
        btnCancel.setOnAction(e -> close());

        HBox buttons = new HBox(12, btnSave, btnCancel);
        buttons.setAlignment(Pos.CENTER);

        VBox root = new VBox(10, title, cbAuto, grid, buttons);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(18, 24, 18, 24));
        root.setStyle("-fx-background-color:#0a0a0a; -fx-border-color:#cc0000; -fx-border-width:2;");

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        setScene(scene);
        sizeToScene();
        centerOnScreen();
    }

    private static void addRow(GridPane g, int row, String lbl, TextField tf, String style) {
        Label l = new Label(lbl);
        l.setStyle(style);
        g.add(l, 0, row);
        g.add(tf, 1, row);
    }

    private static TextField field(String text, String style) {
        TextField tf = new TextField(text);
        tf.setStyle(style);
        tf.setPrefWidth(90);
        return tf;
    }

    private static Button btn(String text, String bg, String fg) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:" + fg + "; " +
                   "-fx-font-family:'Courier New'; -fx-font-size:10; -fx-font-weight:bold; " +
                   "-fx-border-color:" + fg + "; -fx-border-width:1;");
        return b;
    }

    private static int clamp(String txt, int fallback, int min, int max) {
        try { return Math.clamp(Integer.parseInt(txt.trim()), min, max); }
        catch (NumberFormatException e) { return fallback; }
    }
}
