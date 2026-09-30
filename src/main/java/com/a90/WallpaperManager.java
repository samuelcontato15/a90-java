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

/**
 * Troca o wallpaper via user32.dll (a API oficial do Windows).
 * saveOriginal() → applyTheme() → restore()
 * restore() é idempotente: seguro chamar múltiplas vezes.
 *
 * Se o processo for morto no meio da rodada (Gerenciador de Tarefas), o shutdown hook
 * não roda — por isso o original fica anotado em disco enquanto o tema está aplicado,
 * e recoverFromCrash() o restaura na próxima execução.
 */
public class WallpaperManager {

    private static final int SPI_GETDESKWALLPAPER = 0x0073;
    private static final int SPI_SETDESKWALLPAPER = 0x0014;
    private static final int SPIF_UPDATEINIFILE   = 0x01;
    private static final int SPIF_SENDCHANGE      = 0x02;

    /** Linha 1: wallpaper original ("" = cor sólida). Linha 2: arquivo temporário do tema. */
    private static final Path STATE_FILE = Path.of(
        System.getenv().getOrDefault("APPDATA", System.getProperty("user.home")),
        "a90minigame", "wallpaper.state");

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
        try {
            char[] buf = new char[512];
            boolean ok = User32W.INSTANCE.SystemParametersInfoW(SPI_GETDESKWALLPAPER, buf.length, buf, 0);
            originalWallpaper = ok ? Native.toString(buf) : null;
        } catch (Throwable e) {
            originalWallpaper = null;
        }
    }

    /** Extrai ransom_attack.png para um temp e aplica como wallpaper. */
    public static synchronized void applyTheme() {
        // Sem saber qual era o original, não troca: não daria para desfazer
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
        } catch (Exception ignored) {
            // Falha silenciosa — o jogo roda sem mudar o wallpaper
        }
    }

    /** Restaura o wallpaper original. Idempotente; sincronizado por causa do shutdown hook. */
    public static synchronized void restore() {
        if (!wallpaperChanged) return;
        setWallpaper(originalWallpaper); // "" remove a imagem e volta para a cor sólida
        wallpaperChanged = false;
        deleteQuietly(tempWallpaper);
        deleteQuietly(STATE_FILE);
    }

    /** Chamado ao abrir o app: desfaz uma troca que ficou pendente de uma execução morta. */
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
