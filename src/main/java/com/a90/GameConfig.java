package com.a90;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public class GameConfig {

    public enum Mode { MENU, INFINITE }

    public static Mode mode             = Mode.MENU;
    public static int  infiniteMinDelay = 15;
    public static int  infiniteMaxDelay = 45;

    private static final Path CONFIG_FILE = Path.of(
        System.getenv().getOrDefault("APPDATA", System.getProperty("user.home")),
        "a90minigame", "config.properties");

    public static void load() {
        if (!Files.exists(CONFIG_FILE)) return;
        Properties p = new Properties();
        try (InputStream is = Files.newInputStream(CONFIG_FILE)) {
            p.load(is);
            try { mode = Mode.valueOf(p.getProperty("mode", mode.name())); }
            catch (IllegalArgumentException ignored) {}
            infiniteMinDelay = intProp(p, "infiniteMinDelay", infiniteMinDelay);
            infiniteMaxDelay = intProp(p, "infiniteMaxDelay", infiniteMaxDelay);
        } catch (Exception ignored) {}
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            Properties p = new Properties();
            p.setProperty("mode",             mode.name());
            p.setProperty("infiniteMinDelay", String.valueOf(infiniteMinDelay));
            p.setProperty("infiniteMaxDelay", String.valueOf(infiniteMaxDelay));
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
