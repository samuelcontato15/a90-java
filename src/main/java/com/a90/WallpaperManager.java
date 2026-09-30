package com.a90;

import com.sun.jna.Native;
import com.sun.jna.WString;
import com.sun.jna.win32.StdCallLibrary;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Troca o wallpaper via user32.dll (a API oficial do Windows).
 * saveOriginal() → applyTheme() → restore()
 * restore() é idempotente: seguro chamar múltiplas vezes.
 */
public class WallpaperManager {

    private static final int SPI_GETDESKWALLPAPER = 0x0073;
    private static final int SPI_SETDESKWALLPAPER = 0x0014;
    private static final int SPIF_UPDATEINIFILE   = 0x01;
    private static final int SPIF_SENDCHANGE      = 0x02;

    /** Interface JNA mínima — só o que precisamos de user32. */
    interface User32W extends StdCallLibrary {
        User32W INSTANCE = Native.load("user32", User32W.class);
        boolean SystemParametersInfoW(int action, int param, char[]  buf,  int flags);
        boolean SystemParametersInfoW(int action, int param, WString val,  int flags);
    }

    private static String  originalWallpaper = null;
    private static Path    tempWallpaper     = null;
    private static boolean wallpaperChanged  = false;

    /** Chame antes de applyTheme(). */
    public static void saveOriginal() {
        char[] buf = new char[512];
        User32W.INSTANCE.SystemParametersInfoW(SPI_GETDESKWALLPAPER, buf.length, buf, 0);
        originalWallpaper = Native.toString(buf);
    }

    /** Extrai ransom_attack.png para um temp e aplica como wallpaper. */
    public static void applyTheme() {
        try {
            InputStream is = WallpaperManager.class.getResourceAsStream("/assets/ransom_attack.png");
            if (is == null) return;
            tempWallpaper = Files.createTempFile("a90_wp_", ".png");
            Files.copy(is, tempWallpaper, StandardCopyOption.REPLACE_EXISTING);
            setWallpaper(tempWallpaper.toAbsolutePath().toString());
            wallpaperChanged = true;
        } catch (Exception ignored) {
            // Falha silenciosa — o jogo roda sem mudar o wallpaper
        }
    }

    /** Restaura o wallpaper original. Idempotente e thread-safe o suficiente para shutdown hook. */
    public static void restore() {
        if (!wallpaperChanged) return;
        if (originalWallpaper != null && !originalWallpaper.isBlank()) {
            setWallpaper(originalWallpaper);
        }
        wallpaperChanged = false;
        if (tempWallpaper != null) {
            try { Files.deleteIfExists(tempWallpaper); } catch (Exception ignored) {}
        }
    }

    private static void setWallpaper(String path) {
        try {
            User32W.INSTANCE.SystemParametersInfoW(
                    SPI_SETDESKWALLPAPER, 0,
                    new WString(path),
                    SPIF_UPDATEINIFILE | SPIF_SENDCHANGE);
        } catch (Exception ignored) {}
    }
}
