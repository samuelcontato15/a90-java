package com.a90;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.util.Duration;

import java.awt.MouseInfo;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameEngine {

    static final int ROUND_SECONDS = 78;
    static final int PHASE_SECONDS = ROUND_SECONDS / Phase.values().length;
    private static final int COINS_PER_WAVE = 7;
    private static final int MAX_TAUNTS     = 30;

    static final IntegerProperty timeLeft   = new SimpleIntegerProperty(0);
    static final IntegerProperty ransomLeft = new SimpleIntegerProperty(0);

    static Runnable onReturnToMenu = Platform::exit;

    private static final Random           RNG     = new Random();
    private static final List<CoinSprite>  coins   = new ArrayList<>();
    private static final List<TauntWindow> taunts  = new ArrayList<>();
    private static final List<Animation>   roundTimers = new ArrayList<>();
    private static final List<Animation>   phaseTimers = new ArrayList<>();

    private static OverlayWindow    overlay;
    private static GlitchOverlay    glitch;
    private static RansomWindow     ransomWindow;
    private static RansomBackground loseBackground;
    private static MusicPlayer      music;
    private static PauseTransition  pendingEnd;
    private static Phase            phase = Phase.CALM;
    private static boolean          gameOver      = false;
    private static boolean          quitOnCleanup = false;

    public static void start() {
        GameConfig.load();
        overlay = new OverlayWindow();
        overlay.show();
        music   = new MusicPlayer();
        phaseWarning();
    }

    public static void forceExit() {
        gameOver = true;
        stopRound();
        cleanup();
    }

    static Phase currentPhase() { return phase; }

    private static void phaseWarning() {
        gameOver = false;
        Assets.playSound("spawn.wav");

        overlay.showIdleRandom();
        PauseTransition t1 = new PauseTransition(Duration.millis(500));
        t1.setOnFinished(e -> {
            Point before = mousePos();
            overlay.showWarningCenter();
            PauseTransition t2 = new PauseTransition(Duration.millis(500));
            t2.setOnFinished(ev -> {
                boolean moved = !mousePos().equals(before);
                overlay.flashAndHideWarning(moved);
                if (moved) {
                    PauseTransition t3 = new PauseTransition(Duration.millis(100));
                    t3.setOnFinished(e2 -> phaseDownload());
                    t3.play();
                } else {
                    PauseTransition reset = new PauseTransition(Duration.millis(300));
                    reset.setOnFinished(e2 -> cleanup());
                    reset.play();
                }
            });
            t2.play();
        });
        t1.play();
    }

    private static void phaseDownload() {
        Assets.playSound("attack.wav");
        overlay.showJumpscare(() -> {
            Assets.playSound("install.wav");
            overlay.showDownloading(() -> {
                overlay.hide();
                phaseRansom();
            });
        });
    }

    private static void phaseRansom() {
        List<List<CoinType>> waves = new ArrayList<>();
        for (int i = 0; i < Phase.values().length; i++) waves.add(rollWave());
        ransomLeft.set(debtFor(waves));
        timeLeft.set(ROUND_SECONDS);

        ransomWindow = new RansomWindow();
        ransomWindow.launch();

        glitch = new GlitchOverlay();
        glitch.launch();

        Timeline phases = new Timeline();
        for (Phase p : Phase.values()) {
            List<CoinType> wave = waves.get(p.ordinal());
            phases.getKeyFrames().add(new KeyFrame(Duration.seconds(p.ordinal() * PHASE_SECONDS),
                    e -> enterPhase(p, wave)));
        }
        track(roundTimers, phases);

        Timeline countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            int t = timeLeft.get() - 1;
            timeLeft.set(t);
            if (glitch != null) glitch.setIntensity(1.0 - (double) t / ROUND_SECONDS);
            if (t <= 0) lose();
        }));
        countdown.setCycleCount(ROUND_SECONDS);
        track(roundTimers, countdown);
    }

    private static void enterPhase(Phase p, List<CoinType> wave) {
        if (gameOver) return;
        phase = p;
        music.playOnceEndingAt(p.music, PHASE_SECONDS);
        if (p != Phase.CALM && glitch != null) glitch.burst();

        spawnWave(wave);
        for (int i = 0; i < p.tauntBurst; i++) spawnTaunt();

        phaseTimers.forEach(Animation::stop);
        phaseTimers.clear();
        if (p.tauntEveryMs > 0)    track(phaseTimers, repeat(p.tauntEveryMs,    GameEngine::spawnTaunt));
        if (p.coinJumpEveryMs > 0) track(phaseTimers, repeat(p.coinJumpEveryMs, GameEngine::shuffleCoins));
    }

    private static List<CoinType> rollWave() {
        List<CoinType> wave = new ArrayList<>();
        for (int i = 0; i < COINS_PER_WAVE; i++) wave.add(CoinType.weightedRandom());
        if (RNG.nextDouble() < 0.30) wave.add(CoinType.HONEYPOT);
        return wave;
    }

    private static int waveValue(List<CoinType> wave) {
        return wave.stream().mapToInt(t -> t.value).sum();
    }

    static int debtFor(List<List<CoinType>> waves) {
        int last = waveValue(waves.getLast());
        int debt = last - last / 2;
        for (int i = 0; i < waves.size() - 1; i++) debt += waveValue(waves.get(i));
        return debt;
    }

    private static void spawnWave(List<CoinType> wave) {
        for (int i = 0; i < wave.size(); i++) {
            CoinType type = wave.get(i);
            PauseTransition delay = new PauseTransition(
                    Duration.millis(type == CoinType.HONEYPOT ? 3500 : 400L * i));
            delay.setOnFinished(e -> {
                if (gameOver) return;
                Point2D pos = randomCoinPosition();
                CoinSprite coin = new CoinSprite(pos.getX(), pos.getY(), type, GameEngine::onCoinDropped);
                coins.add(coin);
                coin.show();
            });
            track(roundTimers, delay);
        }
    }

    private static void shuffleCoins() {
        for (CoinSprite c : new ArrayList<>(coins)) {
            if (!c.isHeld() && RNG.nextDouble() < phase.coinJumpChance) {
                Point2D pos = randomCoinPosition();
                c.jumpTo(pos.getX(), pos.getY());
            }
        }
    }

    static void onCoinDropped(CoinSprite coin) {
        if (gameOver) return;
        coins.remove(coin);
        coin.close();

        new StarlightBurst(coin.getX() + coin.getWidth()  / 2,
                           coin.getY() + coin.getHeight() / 2,
                           coin.getWidth() * 1.6).show();

        Assets.playSound("cash.wav");
        int left = ransomLeft.get() - coin.type.value;
        ransomLeft.set(Math.max(0, left));
        if (ransomLeft.get() <= 0) win();
    }

    static void spawnTaunt() {
        if (gameOver || taunts.size() >= MAX_TAUNTS) return;
        TauntWindow tw = new TauntWindow();
        taunts.add(tw);
        tw.setOnHidden(e -> taunts.remove(tw));
        tw.launch();
        if (ransomWindow != null) Win32Window.raise(ransomWindow);
        new ArrayList<>(coins).forEach(Win32Window::raise);
    }

    private static void win() {
        if (gameOver) return;
        gameOver = true;
        stopRound();
        closeCoinsAndTaunts();

        double wx = ransomWindow != null ? ransomWindow.getX() : 400;
        double wy = ransomWindow != null ? ransomWindow.getY() : 300;
        if (ransomWindow != null) { ransomWindow.showResult(true); }

        PauseTransition showWin = new PauseTransition(Duration.millis(500));
        showWin.setOnFinished(e -> {
            new ThankYouWindow(wx, wy).show();
            endAfter(5);
        });
        showWin.play();
    }

    private static void lose() {
        if (gameOver) return;
        gameOver = true;
        stopRound();
        closeCoinsAndTaunts();
        if (ransomWindow != null) ransomWindow.showResult(false);

        overlay.show();
        overlay.showCrashJumpscare(() -> {
            Timeline audioFreeze = new Timeline(
                new KeyFrame(Duration.millis(80), e -> Assets.playSound("spawn.wav", 0.4)));
            audioFreeze.setCycleCount(25);
            audioFreeze.play();

            overlay.showFreeze(() -> {
                overlay.close();
                overlay = null;
                loseBackground = new RansomBackground();
                loseBackground.launch();
                quitOnCleanup = true;
                endAfter(3);
            });
        });
    }

    private static void endAfter(int seconds) {
        pendingEnd = new PauseTransition(Duration.seconds(seconds));
        pendingEnd.setOnFinished(e -> cleanup());
        pendingEnd.play();
    }

    static void cleanup() {
        if (pendingEnd != null) { pendingEnd.stop(); pendingEnd = null; }
        if (loseBackground != null) { loseBackground.close(); loseBackground = null; }
        if (overlay == null && !quitOnCleanup) return;
        if (overlay != null) {
            stopRound();
            WallpaperManager.restore();
            closeCoinsAndTaunts();
            overlay.close();
            overlay = null;
        }
        if (ransomWindow != null) { ransomWindow.close(); ransomWindow = null; }
        if (quitOnCleanup) { quitOnCleanup = false; App.quit(); return; }
        if (GameConfig.mode == GameConfig.Mode.INFINITE) scheduleNextAttack();
        else Platform.runLater(onReturnToMenu);
    }

    private static void scheduleNextAttack() {
        int min = Math.min(GameConfig.infiniteMinDelay, GameConfig.infiniteMaxDelay);
        int max = Math.max(GameConfig.infiniteMinDelay, GameConfig.infiniteMaxDelay);
        PauseTransition next = new PauseTransition(Duration.seconds(min + RNG.nextInt(max - min + 1)));
        next.setOnFinished(e -> start());
        next.play();
    }

    private static void stopRound() {
        roundTimers.forEach(Animation::stop);
        roundTimers.clear();
        phaseTimers.forEach(Animation::stop);
        phaseTimers.clear();
        if (music != null) music.stop();
        if (glitch != null) { glitch.close(); glitch = null; }
        phase = Phase.CALM;
    }

    private static void closeCoinsAndTaunts() {
        new ArrayList<>(coins).forEach(c -> { try { c.close(); } catch (Exception ignored) {} });
        coins.clear();
        new ArrayList<>(taunts).forEach(t -> { try { t.close(); } catch (Exception ignored) {} });
        taunts.clear();
    }

    static RansomWindow getRansomWindow() { return ransomWindow; }

    private static void track(List<Animation> list, Animation a) {
        list.add(a);
        a.play();
    }

    private static Timeline repeat(int everyMs, Runnable action) {
        Timeline tl = new Timeline(new KeyFrame(Duration.millis(everyMs), e -> action.run()));
        tl.setCycleCount(Animation.INDEFINITE);
        return tl;
    }

    private static Point2D randomCoinPosition() {
        Rectangle2D b = Screen.getPrimary().getVisualBounds();
        double x, y;
        int tries = 0;
        do {
            x = b.getMinX() + RNG.nextDouble() * (b.getWidth()  - 100);
            y = b.getMinY() + RNG.nextDouble() * (b.getHeight() - 100);
        } while (overlapsRansomWindow(x, y) && ++tries < 50);
        return new Point2D(x, y);
    }

    private static boolean overlapsRansomWindow(double x, double y) {
        if (ransomWindow == null) return false;
        return x < ransomWindow.getX() + ransomWindow.getWidth()  + 20 &&
               x + 100 > ransomWindow.getX() - 20 &&
               y < ransomWindow.getY() + ransomWindow.getHeight() + 20 &&
               y + 100 > ransomWindow.getY() - 20;
    }

    private static Point mousePos() {
        try { return MouseInfo.getPointerInfo().getLocation(); }
        catch (Exception e) { return new Point(0, 0); }
    }
}
