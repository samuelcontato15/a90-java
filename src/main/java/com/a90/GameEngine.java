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

/**
 * Máquina de estados — fiel ao fluxo original:
 *
 *  IDLE → trigger (botão INICIAR, ou o agendamento do modo infinito)
 *  ↓
 *  PHASE_1_WARNING
 *    Face idle aparece aleatória → centro + stop sign + vinheta vermelha + spy de mouse 500ms
 *    Se mouse NÃO moveu → dodge: a rodada termina sem resgate
 *    Se moveu → PHASE_2_DOWNLOAD
 *  ↓
 *  PHASE_2_DOWNLOAD
 *    Jumpscare + tela "DOWNLOADING..." com CD girando e barra de 10 segmentos (install.wav)
 *  ↓
 *  PHASE_3_RANSOM — sempre 1:30, em 3 fases de 30s, uma layer da OST cada (ver Phase)
 *    CALM → TENSE → DESPERATE: cada troca de layer traz uma nova leva de moedas,
 *    mais popups e moedas pulando de lugar até serem pegas.
 *    O débito só fecha com moedas da última leva, então toda rodada chega à fase final.
 *    - Moeda entregue: ransomLeft -= valor (+ brilho Starlight) → if ransomLeft <= 0: WIN
 *    - Timer esgota: LOSE
 *  ↓
 *  WIN  → ThankYouWindow → cleanup
 *  LOSE → CrashJumpscare → cleanup
 *  cleanup → volta ao menu (modo MENU) ou novo ataque após intervalo aleatório (modo INFINITE)
 *
 * ESC: forceExit() encerra a rodada atual em qualquer fase.
 * Ctrl+Alt+Shift+A (KillSwitch): fecha o app inteiro, inclusive no modo infinito.
 */
public class GameEngine {

    static final int ROUND_SECONDS = 90;  // sempre 1:30
    static final int PHASE_SECONDS = ROUND_SECONDS / Phase.values().length; // 30s por layer
    private static final int COINS_PER_WAVE = 7;
    private static final int MAX_TAUNTS     = 30;  // teto de popups abertos ao mesmo tempo

    // Propriedades observáveis (RansomWindow faz binding)
    static final IntegerProperty timeLeft    = new SimpleIntegerProperty(0);
    static final IntegerProperty ransomLeft  = new SimpleIntegerProperty(0);

    /** App registra aqui o que fazer ao fim de cada rodada no modo MENU (volta pra tela de início). */
    static Runnable onReturnToMenu = Platform::exit; // fallback: fecha o app

    private static final Random           RNG     = new Random();
    private static final List<CoinSprite>  coins   = new ArrayList<>();
    private static final List<TauntWindow> taunts  = new ArrayList<>();
    /** Timers da rodada inteira (fases, cronômetro, spawns atrasados). */
    private static final List<Animation>   roundTimers = new ArrayList<>();
    /** Repetidores da fase atual (popups periódicos, pulos de moeda) — trocados a cada fase. */
    private static final List<Animation>   phaseTimers = new ArrayList<>();

    private static OverlayWindow   overlay;
    private static GlitchOverlay   glitch;
    private static RansomWindow    ransomWindow;
    private static MusicPlayer     music;
    private static PauseTransition pendingEnd; // cleanup agendado após win/lose
    private static Phase           phase = Phase.CALM;
    private static boolean         gameOver = false;

    // ──────────────────────────────────────────────────────
    //                     ENTRY POINTS
    // ──────────────────────────────────────────────────────

    /** Chamado pelo botão na tela de início ou pelo agendamento do modo infinito. */
    public static void start() {
        GameConfig.load();
        overlay = new OverlayWindow();
        overlay.show();
        music   = new MusicPlayer();
        phaseWarning();
    }

    /** ESC — encerra a rodada atual em qualquer fase. */
    public static void forceExit() {
        gameOver = true;
        stopRound();
        cleanup();
    }

    static Phase currentPhase() { return phase; }

    // ──────────────────────────────────────────────────────
    //                FASE 1 — WARNING
    // ──────────────────────────────────────────────────────

    private static void phaseWarning() {
        gameOver = false;
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
                    PauseTransition t3 = new PauseTransition(Duration.millis(100));
                    t3.setOnFinished(e2 -> phaseDownload());
                    t3.play();
                } else {
                    // Dodgou — a rodada termina sem resgate (menu ou próximo ataque)
                    PauseTransition reset = new PauseTransition(Duration.millis(300));
                    reset.setOnFinished(e2 -> cleanup());
                    reset.play();
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

        // A leva de moedas de cada fase é sorteada já aqui, para o débito ser conhecido
        List<List<CoinType>> waves = new ArrayList<>();
        for (int i = 0; i < Phase.values().length; i++) waves.add(rollWave());
        ransomLeft.set(debtFor(waves));
        timeLeft.set(ROUND_SECONDS);

        // RansomWindow antes das moedas: elas não podem nascer embaixo dela
        ransomWindow = new RansomWindow();
        ransomWindow.launch();

        // Camada de glitch sobre tudo — vai sujando a tela conforme o tempo passa
        glitch = new GlitchOverlay();
        glitch.launch();

        // Uma fase a cada 30s, cada uma com sua layer
        Timeline phases = new Timeline();
        for (Phase p : Phase.values()) {
            List<CoinType> wave = waves.get(p.ordinal());
            phases.getKeyFrames().add(new KeyFrame(Duration.seconds(p.ordinal() * PHASE_SECONDS),
                    e -> enterPhase(p, wave)));
        }
        track(roundTimers, phases);

        // Cronômetro regressivo
        Timeline countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            int t = timeLeft.get() - 1;
            timeLeft.set(t);
            // Glitch sobe continuamente: 0 no início da rodada, 1 no fim
            if (glitch != null) glitch.setIntensity(1.0 - (double) t / ROUND_SECONDS);
            if (t <= 0) lose();
        }));
        countdown.setCycleCount(ROUND_SECONDS);
        track(roundTimers, countdown);
    }

    /** Troca de layer: o jogo fica mais tenso. */
    private static void enterPhase(Phase p, List<CoinType> wave) {
        if (gameOver) return;
        phase = p;
        music.playLooping(p.music);
        if (p != Phase.CALM) {
            Assets.playSound("spawn.wav");           // o A-90 avisa que piorou
            if (glitch != null) glitch.burst();      // pico de glitch na troca de layer
        }

        spawnWave(wave);
        for (int i = 0; i < p.tauntBurst; i++) spawnTaunt();

        phaseTimers.forEach(Animation::stop);
        phaseTimers.clear();
        if (p.tauntEveryMs > 0)    track(phaseTimers, repeat(p.tauntEveryMs,    GameEngine::spawnTaunt));
        if (p.coinJumpEveryMs > 0) track(phaseTimers, repeat(p.coinJumpEveryMs, GameEngine::shuffleCoins));
    }

    // ──────────────────────────────────────────────────────
    //                  COIN MANAGEMENT
    // ──────────────────────────────────────────────────────

    /** 7 moedas (tipos ponderados) + HoneyPot com 30% de chance. */
    private static List<CoinType> rollWave() {
        List<CoinType> wave = new ArrayList<>();
        for (int i = 0; i < COINS_PER_WAVE; i++) wave.add(CoinType.weightedRandom());
        if (RNG.nextDouble() < 0.30) wave.add(CoinType.HONEYPOT);
        return wave;
    }

    private static int waveValue(List<CoinType> wave) {
        return wave.stream().mapToInt(t -> t.value).sum();
    }

    /**
     * Débito = tudo das levas anteriores + metade (arredondada pra cima) da última:
     * impossível pagar antes da fase final, mas sobra folga para deixar moedas para trás.
     */
    static int debtFor(List<List<CoinType>> waves) {
        int last = waveValue(waves.getLast());
        int debt = last - last / 2;
        for (int i = 0; i < waves.size() - 1; i++) debt += waveValue(waves.get(i));
        return debt;
    }

    /** Moedas aparecem uma a cada 400ms; a HoneyPot chega 3,5s depois. */
    private static void spawnWave(List<CoinType> wave) {
        for (int i = 0; i < wave.size(); i++) {
            CoinType type = wave.get(i);
            PauseTransition delay = new PauseTransition(
                    Duration.millis(type == CoinType.HONEYPOT ? 3500 : 400L * i));
            delay.setOnFinished(e -> {
                if (gameOver) return; // rodada já acabou antes do spawn atrasado
                Point2D pos = randomCoinPosition();
                CoinSprite coin = new CoinSprite(pos.getX(), pos.getY(), type, GameEngine::onCoinDropped);
                coins.add(coin);
                coin.show();
            });
            track(roundTimers, delay);
        }
    }

    /** Moedas soltas pulam para outro lugar — só a que está sendo arrastada escapa. */
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

        // Brilho no ponto de entrega (maior para moedas maiores)
        new StarlightBurst(coin.getX() + coin.getWidth()  / 2,
                           coin.getY() + coin.getHeight() / 2,
                           coin.getWidth() * 1.6).show();

        Assets.playSound("cash.wav");
        int left = ransomLeft.get() - coin.type.value;
        ransomLeft.set(Math.max(0, left));
        if (ransomLeft.get() <= 0) win();
    }

    // ──────────────────────────────────────────────────────
    //                     TAUNTS
    // ──────────────────────────────────────────────────────

    /** Abre um popup de taunt (respeitando o teto). Também chamado pelos popups que se multiplicam. */
    static void spawnTaunt() {
        if (gameOver || taunts.size() >= MAX_TAUNTS) return;
        TauntWindow tw = new TauntWindow();
        taunts.add(tw);
        tw.setOnHidden(e -> taunts.remove(tw));
        tw.launch();
        // O alvo nunca fica soterrado (sem roubar o foco de quem está arrastando)
        if (ransomWindow != null) Win32Window.raise(ransomWindow);
    }

    // ──────────────────────────────────────────────────────
    //                   WIN / LOSE
    // ──────────────────────────────────────────────────────

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
        overlay.showCrashJumpscare(() -> endAfter(2));
    }

    // ──────────────────────────────────────────────────────
    //                   CLEANUP
    // ──────────────────────────────────────────────────────

    private static void endAfter(int seconds) {
        pendingEnd = new PauseTransition(Duration.seconds(seconds));
        pendingEnd.setOnFinished(e -> cleanup());
        pendingEnd.play();
    }

    static void cleanup() {
        // ESC durante a tela final não pode disparar um segundo cleanup depois
        if (pendingEnd != null) { pendingEnd.stop(); pendingEnd = null; }
        if (overlay == null) return; // rodada já limpa — evita abrir o menu duas vezes
        stopRound();
        WallpaperManager.restore();
        closeCoinsAndTaunts();
        overlay.close();
        overlay = null;
        if (ransomWindow != null) { ransomWindow.close(); ransomWindow = null; }

        if (GameConfig.mode == GameConfig.Mode.INFINITE) scheduleNextAttack();
        else Platform.runLater(onReturnToMenu);
    }

    /** Modo infinito: o A-90 volta sozinho depois de um intervalo aleatório. */
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
        // A tela final (vitória ou jumpscare) aparece limpa
        if (glitch != null) { glitch.close(); glitch = null; }
        phase = Phase.CALM;
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
