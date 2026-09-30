package com.a90;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/**
 * Configurações persistidas em %APPDATA%/a90minigame/config.properties.
 * Valores padrão garantem que o jogo funciona sem nenhum arquivo de config.
 */
public class GameConfig {

    public static int     infectionDuration  = 30;    // segundos
    public static int     ransomAmount       = 350;   // valor total de moedas necessário
    public static boolean spawnAutomatically = false;
    public static int     minSpawnDelay      = 30;    // segundos entre spawns automáticos
    public static int     maxSpawnDelay      = 120;

    private static final Path CONFIG_FILE = Path.of(
        System.getenv().getOrDefault("APPDATA", System.getProperty("user.home")),
        "a90minigame", "config.properties");

    public static void load() {
        if (!Files.exists(CONFIG_FILE)) return;
        Properties p = new Properties();
        try (InputStream is = Files.newInputStream(CONFIG_FILE)) {
            p.load(is);
            infectionDuration  = intProp(p, "infectionDuration",  infectionDuration);
            ransomAmount       = intProp(p, "ransomAmount",       ransomAmount);
            spawnAutomatically = Boolean.parseBoolean(p.getProperty("spawnAutomatically", String.valueOf(spawnAutomatically)));
            minSpawnDelay      = intProp(p, "minSpawnDelay",      minSpawnDelay);
            maxSpawnDelay      = intProp(p, "maxSpawnDelay",      maxSpawnDelay);
        } catch (Exception ignored) {}
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            Properties p = new Properties();
            p.setProperty("infectionDuration",  String.valueOf(infectionDuration));
            p.setProperty("ransomAmount",        String.valueOf(ransomAmount));
            p.setProperty("spawnAutomatically",  String.valueOf(spawnAutomatically));
            p.setProperty("minSpawnDelay",       String.valueOf(minSpawnDelay));
            p.setProperty("maxSpawnDelay",       String.valueOf(maxSpawnDelay));
            try (OutputStream os = Files.newOutputStream(CONFIG_FILE)) {
                p.store(os, "A-90 Minigame Config");
            }
        } catch (Exception ignored) {}
    }

    private static int intProp(Properties p, String key, int fallback) {
        try { return Integer.parseInt(p.getProperty(key, String.valueOf(fallback))); }
        catch (NumberFormatException e) { return fallback; }
    }
}
