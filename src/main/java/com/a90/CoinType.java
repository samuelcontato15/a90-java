package com.a90;

import java.util.Random;

/**
 * Tipos de moeda — valores e pesos fiéis ao original C#.
 * Cada tier tem sua própria arte (Assets/Gold/*.ico): moeda → pilha → barra → barras → tesouro.
 * HONEYPOT não tem peso (spawna separado com 30% de chance).
 */
public enum CoinType {
    GOLD1("Gold/Gold1.ico",        10,  40),
    GOLD2("Gold/Gold2.ico",        50,  30),
    GOLD3("Gold/Gold3.ico",       100,  15),
    GOLD4("Gold/Gold4.ico",       150,   8),
    GOLD5("Gold/Gold5.ico",       200,   7),
    HONEYPOT("Gold/HoneyPot.ico", 500,   0);   // 30% chance especial

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
        for (CoinType t : NORMAL) {
            if (roll < t.weight) return t;
            roll -= t.weight;
        }
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
        };
    }
}
