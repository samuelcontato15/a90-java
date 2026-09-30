package com.a90;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.MouseInfo;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Máquina de estados — fiel ao fluxo original:
 *
 *  IDLE → trigger (botão ou auto-spawn)
 *  ↓
 *  PHASE_1_WARNING
 *    Face idle aparece aleatória → centro + stop sign + spy de mouse 500ms
 *    Se mouse NÃO moveu → IDLE (dodge)
 *    Se moveu → PHASE_2_DOWNLOAD
 *  ↓
 *  PHASE_2_DOWNLOAD
 *    Jumpscare + tela "DOWNLOADING..." com barra de 10 segmentos
 *  ↓
 *  PHASE_3_RANSOM
 *    Wallpaper muda, moedas spawnam, RansomWindow + 9 TauntWindows, OST em 3 camadas
 *    - Moeda entregue: ransomLeft -= valor
 *      → if ransomLeft <= 0: WIN
 *    - Timer esgota: LOSE
 *  ↓
 *  WIN  → ThankYouWindow / CrucifixWindow → cleanup
 *  LOSE → CrashJumpscare → cleanup
 *
 * Tecla de pânico ESC: forceExit() → cleanup imediato em qualquer fase.
 */
public class GameEngine {

    // Propriedades observáveis (RansomWindow faz binding)
    static final IntegerProperty timeLeft    = new SimpleIntegerProperty(0);
    static final IntegerProperty ransomLeft  = new SimpleIntegerProperty(0);

    /** App registra aqui o que fazer ao fim de cada rodada (volta pra tela de início). */
    static Runnable onReturnToMenu = Platform::exit; // fallback: fecha o app

    private static final Random          RNG       = new Random();
    private static final List<CoinSprite> coins    = new ArrayList<>();
    private static final List<TauntWindow> taunts  = new ArrayList<>();

    private static OverlayWindow overlay;
    private static RansomWindow  ransomWindow;
    private static MusicPlayer   music;
    private static Timeline      countdown;
    private static boolean       gameOver = false;
    private static boolean       crucifixUsed = false;

    // ──────────────────────────────────────────────────────
    //                     ENTRY POINTS
    // ──────────────────────────────────────────────────────

    /** Chamado por App.start() ou pelo botão na tela de início. */
    public static void start() {
        GameConfig.load();
        overlay = new OverlayWindow();
        overlay.show();
        music   = new MusicPlayer();
        phaseWarning();
    }

    /** ESC / Ctrl+Q — sempre funciona. */
    public static void forceExit() {
        gameOver = true;
        if (countdown != null) countdown.stop();
        music.stop();
        cleanup();
    }

    // ──────────────────────────────────────────────────────
    //                FASE 1 — WARNING
    // ──────────────────────────────────────────────────────

    private static void phaseWarning() {
        gameOver = false;
        crucifixUsed = false;
        Assets.playSound("spawn.wav");

        // Face idle em posição aleatória — 500ms
        overlay.showIdleRandom();
        PauseTransition t1 = new PauseTransition(Duration.millis(500));
        t1.setOnFinished(e -> {
            // Spy: registra posição do mouse
            Point before = mousePos();
            // Centraliza + stop sign — 500ms (spy phase)
            overlay.showWarningCenter();
            PauseTransition t2 = new PauseTransition(Duration.millis(500));
            t2.setOnFinished(ev -> {
                boolean moved = !mousePos().equals(before);
                overlay.flashAndHideWarning(moved);
                if (moved) {
                    // Pequena pausa antes do jumpscare
                    PauseTransition t3 = new PauseTransition(Duration.millis(moved ? 100 : 200));
                    t3.setOnFinished(e2 -> phaseDownload());
                    t3.play();
                } else {
                    // Dodgou — reseta silenciosamente
                    PauseTransition reset = new PauseTransition(Duration.millis(300));
                    reset.setOnFinished(e2 -> overlay.hide());
                    reset.play();
                    // Auto-spawn: reagenda o próximo ataque
                    scheduleAutoSpawn();
                }
            });
            t2.play();
        });
        t1.play();
    }

    // ──────────────────────────────────────────────────────
    //                FASE 2 — DOWNLOAD
    // ──────────────────────────────────────────────────────

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

    // ──────────────────────────────────────────────────────
    //                FASE 3 — RANSOM
    // ──────────────────────────────────────────────────────

    private static void phaseRansom() {
        WallpaperManager.saveOriginal();
        WallpaperManager.applyTheme();

        // Spawna moedas e calcula valor total gerado
        int totalValue = spawnCoins();

        // Ransom = min(configurado, gerado) — garante sempre vencível
        int target = Math.min(GameConfig.ransomAmount, totalValue);
        ransomLeft.set(target);
        timeLeft.set(GameConfig.infectionDuration);

        // RansomWindow
        ransomWindow = new RansomWindow();
        ransomWindow.launch();

        // 9 TauntWindows (igual ao original)
        for (int i = 0; i < 9; i++) {
            TauntWindow tw = new TauntWindow();
            taunts.add(tw);
            tw.launch();
        }

        // OST — 3 camadas conforme o original
        // layer3 dura 26s fixos (os últimos), layer1+2 dividem o restante
        int layer3s = Math.min(26, GameConfig.infectionDuration);
        int remaining = Math.max(0, GameConfig.infectionDuration - layer3s);
        int layer1s = remaining / 2;
        int layer2s = remaining - layer1s;

        music.playLooping("layer1.wav");

        if (layer1s > 0) {
            Timeline tl1 = new Timeline(new KeyFrame(Duration.seconds(layer1s),
                    e -> music.playLooping("layer2.wav")));
            tl1.play();
        }
        if (layer1s + layer2s > 0) {
            Timeline tl2 = new Timeline(new KeyFrame(Duration.seconds(layer1s + layer2s),
                    e -> music.playOnce("layer3.wav")));
            tl2.play();
        }

        // Cronômetro regressivo
        countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            int t = timeLeft.get() - 1;
            timeLeft.set(t);
            if (t <= 0 && !gameOver) lose();
        }));
        countdown.setCycleCount(GameConfig.infectionDuration);
        countdown.play();
    }

    // ──────────────────────────────────────────────────────
    //                  COIN MANAGEMENT
    // ──────────────────────────────────────────────────────

    /** Spawna 7 moedas (tipos ponderados) + possível Honeypot/Crucifix. Retorna valor total. */
    private static int spawnCoins() {
        coins.clear();
        Rectangle2D b = Screen.getPrimary().getVisualBounds();
        int totalValue = 0;

        for (int i = 0; i < 7; i++) {
            CoinType type = CoinType.weightedRandom();
            double x, y;
            do {
                x = b.getMinX() + RNG.nextDouble() * (b.getWidth()  - 100);
                y = b.getMinY() + RNG.nextDouble() * (b.getHeight() - 100);
            } while (ransomWindow != null && overlapsRansomWindow(x, y));

            final double fx = x, fy = y;
            final CoinType ft = type;
            PauseTransition delay = new PauseTransition(Duration.millis(400L * i));
            delay.setOnFinished(e -> {
                CoinSprite coin = new CoinSprite(fx, fy, ft, GameEngine::onCoinDropped);
                coins.add(coin);
                coin.show();
            });
            delay.play();
            totalValue += type.value;
        }

        // Honeypot: 30% de chance
        if (RNG.nextDouble() < 0.30) {
            double x = b.getMinX() + RNG.nextDouble() * (b.getWidth()  - 100);
            double y = b.getMinY() + RNG.nextDouble() * (b.getHeight() - 100);
            PauseTransition hd = new PauseTransition(Duration.millis(3500));
            hd.setOnFinished(e -> {
                CoinSprite hp = new CoinSprite(x, y, CoinType.HONEYPOT, GameEngine::onCoinDropped);
                coins.add(hp);
                hp.show();
            });
            hd.play();
            totalValue += CoinType.HONEYPOT.value;
        }

        // Crucifix: 10% de chance
        if (RNG.nextDouble() < 0.10) {
            double x = b.getMinX() + RNG.nextDouble() * (b.getWidth()  - 100);
            double y = b.getMinY() + RNG.nextDouble() * (b.getHeight() - 100);
            PauseTransition cd = new PauseTransition(Duration.millis(5000));
            cd.setOnFinished(e -> {
                CoinSprite cr = new CoinSprite(x, y, CoinType.CRUCIFIX, GameEngine::onCoinDropped);
                coins.add(cr);
                cr.show();
            });
            cd.play();
            // Crucifix não tem value numérico — win imediato ao entregar
        }

        return totalValue;
    }

    static void onCoinDropped(CoinSprite coin) {
        if (gameOver) return;
        coins.remove(coin);
        coin.close();

        if (coin.type == CoinType.CRUCIFIX) {
            crucifixUsed = true;
            ransomLeft.set(0);
            win();
            return;
        }

        Assets.playSound("cash.wav");
        int left = ransomLeft.get() - coin.type.value;
        ransomLeft.set(Math.max(0, left));
        if (ransomLeft.get() <= 0) win();
    }

    // ──────────────────────────────────────────────────────
    //                   WIN / LOSE
    // ──────────────────────────────────────────────────────

    private static void win() {
        if (gameOver) return;
        gameOver = true;
        countdown.stop();
        music.stop();
        closeCoinsAndTaunts();

        double wx = ransomWindow != null ? ransomWindow.getX() : 400;
        double wy = ransomWindow != null ? ransomWindow.getY() : 300;
        if (ransomWindow != null) { ransomWindow.showResult(true); }

        PauseTransition showWin = new PauseTransition(Duration.millis(500));
        showWin.setOnFinished(e -> {
            if (crucifixUsed) new CrucifixWindow(wx, wy).show();
            else              new ThankYouWindow(wx, wy).show();
            endAfter(5);
        });
        showWin.play();
    }

    private static void lose() {
        if (gameOver) return;
        gameOver = true;
        countdown.stop();
        music.stop();
        closeCoinsAndTaunts();
        if (ransomWindow != null) ransomWindow.showResult(false);

        overlay.show();
        overlay.showCrashJumpscare(() -> endAfter(2));
    }

    // ──────────────────────────────────────────────────────
    //                  AUTO-SPAWN
    // ──────────────────────────────────────────────────────

    private static void scheduleAutoSpawn() {
        if (!GameConfig.spawnAutomatically) return;
        int min = Math.min(GameConfig.minSpawnDelay, GameConfig.maxSpawnDelay);
        int max = Math.max(GameConfig.minSpawnDelay, GameConfig.maxSpawnDelay);
        int delay = min + RNG.nextInt(Math.max(1, max - min));
        PauseTransition pt = new PauseTransition(Duration.seconds(delay));
        pt.setOnFinished(e -> phaseWarning());
        pt.play();
    }

    // ──────────────────────────────────────────────────────
    //                   CLEANUP
    // ──────────────────────────────────────────────────────

    private static void endAfter(int seconds) {
        PauseTransition pt = new PauseTransition(Duration.seconds(seconds));
        pt.setOnFinished(e -> cleanup());
        pt.play();
    }

    static void cleanup() {
        WallpaperManager.restore();
        music.stop();
        coins.forEach(c -> { try { c.close(); } catch (Exception ignored) {} });
        coins.clear();
        closeCoinsAndTaunts();
        if (overlay      != null) { overlay.close();      overlay      = null; }
        if (ransomWindow != null) { ransomWindow.close();  ransomWindow = null; }
        Platform.runLater(onReturnToMenu);
    }

    private static void closeCoinsAndTaunts() {
        new ArrayList<>(coins).forEach(c -> { try { c.close(); } catch (Exception ignored) {} });
        coins.clear();
        new ArrayList<>(taunts).forEach(t -> { try { t.close(); } catch (Exception ignored) {} });
        taunts.clear();
    }

    // ──────────────────────────────────────────────────────
    //                   UTILS
    // ──────────────────────────────────────────────────────

    static RansomWindow getRansomWindow() { return ransomWindow; }

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
