package com.a90;

/**
 * As 3 fases do resgate — uma por layer da OST, 30s cada (1:30 no total).
 * A cada troca de layer o jogo aperta: nova leva de moedas, mais popups,
 * moedas pulando de lugar até serem pegas e tudo tremendo mais.
 * Os últimos 30s (DESPERATE) são o pico de todos os valores.
 */
enum Phase {
    //           música        popups    popup novo   multiplica   moedas pulam   chance de   tremor
    //                         ao entrar a cada (ms)  (%/200ms)    a cada (ms)    pular       (px)
    CALM     ("layer1.wav",    3,        5000,        2,           0,             0.00,        5),
    TENSE    ("layer2.wav",    2,        2000,        3,           1000,          0.25,        9),
    DESPERATE("layer3.wav",    3,         600,        4,           500,           0.35,       16);

    final String music;
    final int    tauntBurst;       // popups abertos de uma vez ao entrar na fase
    final int    tauntEveryMs;     // popup novo periódico (0 = nenhum)
    final int    tauntMultiplyPct; // chance de cada popup abrir outro a cada 200ms
    final int    coinJumpEveryMs;  // intervalo entre chances de pulo das moedas (0 = paradas)
    final double coinJumpChance;   // chance de cada moeda solta pular a cada intervalo
    final double glitchPx;         // amplitude do tremor da RansomWindow e dos popups

    Phase(String music, int tauntBurst, int tauntEveryMs, int tauntMultiplyPct,
          int coinJumpEveryMs, double coinJumpChance, double glitchPx) {
        this.music            = music;
        this.tauntBurst       = tauntBurst;
        this.tauntEveryMs     = tauntEveryMs;
        this.tauntMultiplyPct = tauntMultiplyPct;
        this.coinJumpEveryMs  = coinJumpEveryMs;
        this.coinJumpChance   = coinJumpChance;
        this.glitchPx         = glitchPx;
    }
}
