package com.a90;

import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.media.AudioClip;
import javafx.stage.Stage;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Carregador de recursos (empacotados a partir da pasta Assets/ — ver pom.xml).
 * Images, AudioClips e cursores cacheados na primeira carga.
 * Falhas silenciosas — o jogo roda sem assets se necessário.
 */
public class Assets {

    /** Ícone padrão das janelas do jogo (e do .exe gerado pelo build-exe.bat). */
    public static final String APP_ICON        = "stop_sign.ico";
    public static final String INFECTED_CURSOR = "infectedcursor.cur";

    /** GIF 1920x1080 com 50 frames: o JavaFX decodifica todos os frames em memória,
     *  então ele é carregado reduzido (~46 MB em vez de ~415 MB) e esticado na tela. */
    private static final String VIGNETTE   = "red_vignette.gif";
    private static final double VIGNETTE_W = 640, VIGNETTE_H = 360;

    private static final Map<String, Image>     images  = new HashMap<>();
    private static final Map<String, AudioClip> sounds  = new HashMap<>();
    private static final Map<String, Cursor>    cursors = new HashMap<>();

    /** Carrega imagem de /assets/<name>. Suporta PNG, GIF animado, JPG, ICO e CUR. */
    public static Image loadImage(String name) {
        return images.computeIfAbsent(name, k -> {
            if (isIcon(k)) {
                IcoDecoder.Icon icon = loadIcon(k);
                return icon != null ? icon.image() : null;
            }
            try (var is = Assets.class.getResourceAsStream("/assets/" + k)) {
                return is != null ? new Image(is) : null;
            } catch (Exception e) { return null; }
        });
    }

    /** Vinheta vermelha reduzida, carregada em background (não trava a thread do JavaFX). */
    public static Image loadVignette() {
        return images.computeIfAbsent(VIGNETTE, k -> {
            URL url = Assets.class.getResource("/assets/" + k);
            return url != null ? new Image(url.toExternalForm(), VIGNETTE_W, VIGNETTE_H, false, true, true) : null;
        });
    }

    /** Aquece o cache dos assets pesados enquanto a tela de início está aberta. */
    public static void preload() {
        loadVignette();
    }

    /** Cursor a partir de um .cur/.ico (usa o hotspot do arquivo, ou o canto superior esquerdo). */
    public static Cursor loadCursor(String name) {
        return cursors.computeIfAbsent(name, k -> {
            IcoDecoder.Icon icon = loadIcon(k);
            return icon != null ? new ImageCursor(icon.image(), icon.hotspotX(), icon.hotspotY())
                                : Cursor.DEFAULT;
        });
    }

    /** Troca o cursor da cena pelo cursor "infectado" do A-90. */
    public static void infect(Scene scene) {
        scene.setCursor(loadCursor(INFECTED_CURSOR));
    }

    /** Define o ícone da janela (barra de título / barra de tarefas). */
    public static void setIcon(Stage stage, String name) {
        Image img = loadImage(name);
        if (img != null) stage.getIcons().setAll(img);
    }

    /** Dispara som de /assets/Sounds/<name> (fire-and-forget). */
    public static void playSound(String name) {
        AudioClip clip = sounds.computeIfAbsent(name, k -> {
            URL url = soundUrl(k);
            return url != null ? new AudioClip(url.toString()) : null;
        });
        if (clip != null) clip.play();
    }

    /** Retorna URL de som (para MediaPlayer). */
    public static URL soundUrl(String name) {
        return Assets.class.getResource("/assets/Sounds/" + name);
    }

    private static boolean isIcon(String name) {
        String n = name.toLowerCase();
        return n.endsWith(".ico") || n.endsWith(".cur");
    }

    private static IcoDecoder.Icon loadIcon(String name) {
        try (InputStream is = Assets.class.getResourceAsStream("/assets/" + name)) {
            return is != null ? IcoDecoder.decode(is.readAllBytes()) : null;
        } catch (Exception e) { return null; }
    }
}
