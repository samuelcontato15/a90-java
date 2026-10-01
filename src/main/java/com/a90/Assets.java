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

public class Assets {

    public static final String APP_ICON        = "stop_sign.ico";
    public static final String INFECTED_CURSOR = "infectedcursor.cur";

    private static final String VIGNETTE   = "red_vignette.gif";
    private static final double VIGNETTE_W = 640, VIGNETTE_H = 360;

    private static final Map<String, Image>     images  = new HashMap<>();
    private static final Map<String, AudioClip> sounds  = new HashMap<>();
    private static final Map<String, Cursor>    cursors = new HashMap<>();

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

    public static Image loadVignette() {
        return images.computeIfAbsent(VIGNETTE, k -> {
            URL url = Assets.class.getResource("/assets/" + k);
            return url != null ? new Image(url.toExternalForm(), VIGNETTE_W, VIGNETTE_H, false, true, true) : null;
        });
    }

    public static void preload() {
        loadVignette();
    }

    public static Cursor loadCursor(String name) {
        return cursors.computeIfAbsent(name, k -> {
            IcoDecoder.Icon icon = loadIcon(k);
            return icon != null ? new ImageCursor(icon.image(), icon.hotspotX(), icon.hotspotY())
                                : Cursor.DEFAULT;
        });
    }

    public static void infect(Scene scene) {
        scene.setCursor(loadCursor(INFECTED_CURSOR));
    }

    public static void setIcon(Stage stage, String name) {
        Image img = loadImage(name);
        if (img != null) stage.getIcons().setAll(img);
    }

    public static void playSound(String name) {
        playSound(name, 1.0);
    }

    public static void playSound(String name, double volume) {
        AudioClip clip = sounds.computeIfAbsent(name, k -> {
            URL url = soundUrl(k);
            return url != null ? new AudioClip(url.toString()) : null;
        });
        if (clip != null) clip.play(volume);
    }

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
