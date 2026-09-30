package com.a90;

import javafx.scene.image.Image;
import javafx.scene.media.AudioClip;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Carregador de recursos.
 * Images e AudioClips cacheados na primeira carga.
 * Falhas silenciosas — o jogo roda sem assets se necessário.
 */
public class Assets {

    private static final Map<String, Image>     images = new HashMap<>();
    private static final Map<String, AudioClip> sounds = new HashMap<>();

    /** Carrega imagem de /assets/<name>. Suporta PNG, GIF animado, JPG. */
    public static Image loadImage(String name) {
        return images.computeIfAbsent(name, k -> {
            try (var is = Assets.class.getResourceAsStream("/assets/" + k)) {
                return is != null ? new Image(is) : null;
            } catch (Exception e) { return null; }
        });
    }

    /** Dispara som de /assets/Sounds/<name> (fire-and-forget). */
    public static void playSound(String name) {
        AudioClip clip = sounds.computeIfAbsent(name, k -> {
            URL url = Assets.class.getResource("/assets/Sounds/" + k);
            return url != null ? new AudioClip(url.toString()) : null;
        });
        if (clip != null) clip.play();
    }

    /** Retorna URL de som (para MediaPlayer). */
    public static URL soundUrl(String name) {
        return Assets.class.getResource("/assets/Sounds/" + name);
    }
}
