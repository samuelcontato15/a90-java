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

public class ConfigWindow extends Stage {

    public ConfigWindow(Stage owner) {
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.UNDECORATED);
        setResizable(false);

        String labelStyle = "-fx-font-family:'Courier New'; -fx-font-size:11; -fx-text-fill:#cccccc;";
        String noteStyle  = "-fx-font-family:'Courier New'; -fx-font-size:9;  -fx-text-fill:#666666;";
        String fieldStyle = "-fx-background-color:#1a1a1a; -fx-text-fill:#ffaa00; " +
                            "-fx-font-family:'Courier New'; -fx-border-color:#440000; -fx-border-width:1;";

        Label title = new Label("[ CONFIGURAÇÃO ]");
        title.setStyle("-fx-font-family:'Courier New'; -fx-font-size:13; -fx-text-fill:#ff2222; -fx-font-weight:bold;");

        Label duration = new Label("Duração: 1:30 (fixa)");
        duration.setStyle(labelStyle);

        Label modeLabel = new Label("Ao terminar a rodada:");
        modeLabel.setStyle(labelStyle);
        ToggleGroup modes = new ToggleGroup();
        RadioButton rbMenu     = radio("voltar ao menu", modes, labelStyle);
        RadioButton rbInfinite = radio("modo infinito (o A-90 volta sozinho)", modes, labelStyle);
        (GameConfig.mode == GameConfig.Mode.INFINITE ? rbInfinite : rbMenu).setSelected(true);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(4, 0, 4, 22));
        TextField tfMin = field(String.valueOf(GameConfig.infiniteMinDelay), fieldStyle);
        TextField tfMax = field(String.valueOf(GameConfig.infiniteMaxDelay), fieldStyle);
        addRow(grid, 0, "Intervalo mín (s):", tfMin, labelStyle);
        addRow(grid, 1, "Intervalo máx (s):", tfMax, labelStyle);
        grid.disableProperty().bind(rbInfinite.selectedProperty().not());

        Label stopNote = new Label("Parar o modo infinito: " + KillSwitch.LABEL + "\n" +
                                   "(ou encerrar pelo Gerenciador de Tarefas)");
        stopNote.setStyle(noteStyle);
        stopNote.visibleProperty().bind(rbInfinite.selectedProperty());

        Button btnSave   = btn("SALVAR",   "#004400", "#00cc00");
        Button btnCancel = btn("CANCELAR", "#440000", "#ff4444");

        btnSave.setOnAction(e -> {
            GameConfig.mode             = rbInfinite.isSelected() ? GameConfig.Mode.INFINITE : GameConfig.Mode.MENU;
            GameConfig.infiniteMinDelay = clamp(tfMin.getText(), GameConfig.infiniteMinDelay, 1, 3600);
            GameConfig.infiniteMaxDelay = clamp(tfMax.getText(), GameConfig.infiniteMaxDelay, 1, 3600);
            GameConfig.save();
            close();
        });
        btnCancel.setOnAction(e -> close());

        HBox buttons = new HBox(12, btnSave, btnCancel);
        buttons.setAlignment(Pos.CENTER);

        VBox root = new VBox(10, title, duration, modeLabel, rbMenu, rbInfinite, grid, stopNote, buttons);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(18, 24, 18, 24));
        root.setStyle("-fx-background-color:#0a0a0a; -fx-border-color:#cc0000; -fx-border-width:2;");

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        setScene(scene);
        sizeToScene();
        centerOnScreen();
    }

    private static RadioButton radio(String text, ToggleGroup group, String style) {
        RadioButton rb = new RadioButton(text);
        rb.setToggleGroup(group);
        rb.setStyle(style);
        return rb;
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
