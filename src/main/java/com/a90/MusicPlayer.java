package com.a90;

import javafx.animation.PauseTransition;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.net.URL;

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

    public void playOnceEndingAt(String name, int phaseSeconds) {
        stop();
        URL url = Assets.soundUrl(name);
        if (url == null) return;
        MediaPlayer player = new MediaPlayer(new Media(url.toString()));
        current = player;
        player.setOnReady(() -> {
            if (current != player) return;
            double delay = Math.max(0, phaseSeconds - player.getMedia().getDuration().toSeconds());
            PauseTransition pt = new PauseTransition(Duration.seconds(delay));
            pt.setOnFinished(e -> { if (current == player) player.play(); });
            pt.play();
        });
    }

    public void stop() {
        if (current != null) {
            current.stop();
            current.dispose();
            current = null;
        }
    }
}
