package com.a90;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;

/**
 * Gerencia as 3 camadas do OST (layer1 → layer2 → layer3).
 * Para e libera recursos antes de trocar de faixa.
 */
public class MusicPlayer {

    private MediaPlayer current;

    public void playLooping(String name) {
        stop();
        URL url = MusicPlayer.class.getResource("/assets/Sounds/" + name);
        if (url == null) return;
        current = new MediaPlayer(new Media(url.toString()));
        current.setCycleCount(MediaPlayer.INDEFINITE);
        current.play();
    }

    public void playOnce(String name) {
        stop();
        URL url = MusicPlayer.class.getResource("/assets/Sounds/" + name);
        if (url == null) return;
        current = new MediaPlayer(new Media(url.toString()));
        current.play();
    }

    public void stop() {
        if (current != null) {
            current.stop();
            current.dispose();
            current = null;
        }
    }
}
