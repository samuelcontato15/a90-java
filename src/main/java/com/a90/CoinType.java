package com.a90;

import java.util.Random;

/**
 * Tipos de moeda — valores e pesos fiéis ao original C#.
 * CRUCIFIX não tem peso (spawna separado com 10% de chance).
 */
public enum CoinType {
    GOLD1("Gold.png",            10,  40),
    GOLD2("Gold.png",            50,  30),
    GOLD3("Gold.png",           100,  15),
    GOLD4("Gold.png",           150,   8),
    GOLD5("Gold.png",           200,   7),
    HONEYPOT("Gold.png",        500,   0),   // 30% chance especial
    CRUCIFIX("ransom_crucifix.png", 0, 0);   // 10% chance especial

    public final String image;
    public final int    value;
    public final int    weight; // 0 = spawna fora do pool normal

    CoinType(String image, int value, int weight) {
        this.image  = image;
        this.value  = value;
        this.weight = weight;
    }

    private static final CoinType[] NORMAL = {GOLD1, GOLD2, GOLD3, GOLD4, GOLD5};
    private static final Random     RNG    = new Random();

    /** Retorna um tipo normal com probabilidade ponderada (40/30/15/8/7). */
    public static CoinType weightedRandom() {
        int roll = RNG.nextInt(100);
        if (roll < 40) return GOLD1;
        if (roll < 70) return GOLD2;
        if (roll < 85) return GOLD3;
        if (roll < 93) return GOLD4;
        return GOLD5;
    }

    /** Label colorida pra mostrar em cima da moeda. Vazio para tier 1. */
    public String label() {
        return switch (this) {
            case GOLD1    -> "";
            case GOLD2    -> "50";
            case GOLD3    -> "100";
            case GOLD4    -> "150";
            case GOLD5    -> "200";
            case HONEYPOT -> "★";
            case CRUCIFIX -> "✝";
        };
    }

    /** Cor do label (JavaFX CSS). */
    public String labelColor() {
        return switch (this) {
            case GOLD1    -> "#ffffff";
            case GOLD2    -> "#ffe066";
            case GOLD3    -> "#ffaa00";
            case GOLD4    -> "#ff7700";
            case GOLD5    -> "#ff2200";
            case HONEYPOT -> "#aa00ff";
            case CRUCIFIX -> "#00ccff";
        };
    }
}
