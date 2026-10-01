package com.a90;

enum Phase {
    CALM     ("layer1.wav",    3,        5000,        2,           0,             0.00,        5),
    TENSE    ("layer2.wav",    2,        2000,        3,           1000,          0.25,        9),
    DESPERATE("layer3.wav",    3,         600,        4,           500,           0.35,       16);

    final String music;
    final int    tauntBurst;
    final int    tauntEveryMs;
    final int    tauntMultiplyPct;
    final int    coinJumpEveryMs;
    final double coinJumpChance;
    final double glitchPx;

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
