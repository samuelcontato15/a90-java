package com.a90;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;

/**
 * Toca as layers da OST em sequência (layer1 → layer2 → layer3), uma por fase de 30s.
 * Cada layer tem 26,18s e fica em loop até a troca de fase.
 * Para e libera recursos antes de trocar de faixa.
 */
public class MusicPlayer {

    private MediaPlayer current;

    public void playLooping(String name) {
        stop();
        URL url = Assets.soundUrl(name);
        if (url == null) return;
        current = new MediaPlayer(new Media(url.toString()));
        current.setCycleCount(MediaPlayer.INDEFINITE);
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
