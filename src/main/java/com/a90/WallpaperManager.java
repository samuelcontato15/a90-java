package com.a90;

import com.sun.jna.Native;
import com.sun.jna.WString;
import com.sun.jna.win32.StdCallLibrary;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class WallpaperManager {

    private static final int SPI_GETDESKWALLPAPER = 0x0073;
    private static final int SPI_SETDESKWALLPAPER = 0x0014;
    private static final int SPIF_UPDATEINIFILE   = 0x01;
    private static final int SPIF_SENDCHANGE      = 0x02;

    private static final Path STATE_FILE = Path.of(
        System.getenv().getOrDefault("APPDATA", System.getProperty("user.home")),
        "a90minigame", "wallpaper.state");

    interface User32W extends StdCallLibrary {
        User32W INSTANCE = Native.load("user32", User32W.class);
        boolean SystemParametersInfoW(int action, int param, char[]  buf,  int flags);
        boolean SystemParametersInfoW(int action, int param, WString val,  int flags);
    }

    private static String  originalWallpaper = null;
    private static Path    tempWallpaper     = null;
    private static boolean wallpaperChanged  = false;

    public static void saveOriginal() {
        try {
            char[] buf = new char[512];
            boolean ok = User32W.INSTANCE.SystemParametersInfoW(SPI_GETDESKWALLPAPER, buf.length, buf, 0);
            originalWallpaper = ok ? Native.toString(buf) : null;
        } catch (Throwable e) {
            originalWallpaper = null;
        }
    }

    public static synchronized void applyTheme() {
        if (originalWallpaper == null) return;
        try (InputStream is = WallpaperManager.class.getResourceAsStream("/assets/ransom_attack.png")) {
            if (is == null) return;
            tempWallpaper = Files.createTempFile("a90_wp_", ".png");
            Files.copy(is, tempWallpaper, StandardCopyOption.REPLACE_EXISTING);
            Files.createDirectories(STATE_FILE.getParent());
            Files.write(STATE_FILE, List.of(originalWallpaper, tempWallpaper.toAbsolutePath().toString()),
                        StandardCharsets.UTF_8);
            setWallpaper(tempWallpaper.toAbsolutePath().toString());
            wallpaperChanged = true;
        } catch (Exception ignored) {}
    }

    public static synchronized void restore() {
        if (!wallpaperChanged) return;
        setWallpaper(originalWallpaper);
        wallpaperChanged = false;
        deleteQuietly(tempWallpaper);
        deleteQuietly(STATE_FILE);
    }

    public static synchronized void recoverFromCrash() {
        try {
            if (!Files.exists(STATE_FILE)) return;
            List<String> lines = Files.readAllLines(STATE_FILE, StandardCharsets.UTF_8);
            if (!lines.isEmpty()) setWallpaper(lines.get(0));
            if (lines.size() > 1 && !lines.get(1).isBlank()) deleteQuietly(Path.of(lines.get(1)));
            deleteQuietly(STATE_FILE);
        } catch (Exception ignored) {}
    }

    private static void setWallpaper(String path) {
        try {
            User32W.INSTANCE.SystemParametersInfoW(
                    SPI_SETDESKWALLPAPER, 0,
                    new WString(path),
                    SPIF_UPDATEINIFILE | SPIF_SENDCHANGE);
        } catch (Throwable ignored) {}
    }

    private static void deleteQuietly(Path p) {
        if (p == null) return;
        try { Files.deleteIfExists(p); } catch (Exception ignored) {}
    }
}
