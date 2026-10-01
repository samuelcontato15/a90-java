package com.a90;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser;
import javafx.stage.Stage;

import java.util.Map;
import java.util.WeakHashMap;

final class Win32Window {

    private static final int WS_EX_LAYERED     = 0x00080000;
    private static final int WS_EX_TRANSPARENT = 0x00000020;
    private static final int WS_EX_NOACTIVATE  = 0x08000000;

    private static final int SWP_NOSIZE     = 0x0001;
    private static final int SWP_NOMOVE     = 0x0002;
    private static final int SWP_NOACTIVATE = 0x0010;
    private static final HWND HWND_TOPMOST  = new HWND(com.sun.jna.Pointer.createConstant(-1));

    private static final Map<Stage, HWND> HANDLES = new WeakHashMap<>();
    private static int seq;

    private Win32Window() {}

    static void tag(Stage stage) {
        stage.setTitle("a90-" + (++seq) + "-" + System.nanoTime());
    }

    static void clickThrough(Stage stage) {
        HWND hwnd = handleOf(stage);
        if (hwnd == null) return;
        try {
            int ex = User32.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE);
            User32.INSTANCE.SetWindowLong(hwnd, WinUser.GWL_EXSTYLE,
                    ex | WS_EX_LAYERED | WS_EX_TRANSPARENT | WS_EX_NOACTIVATE);
        } catch (Throwable ignored) {}
    }

    static void raise(Stage stage) {
        HWND hwnd = handleOf(stage);
        if (hwnd == null) { stage.toFront(); return; }
        try {
            User32.INSTANCE.SetWindowPos(hwnd, HWND_TOPMOST, 0, 0, 0, 0,
                    SWP_NOMOVE | SWP_NOSIZE | SWP_NOACTIVATE);
        } catch (Throwable ignored) {
            stage.toFront();
        }
    }

    private static HWND handleOf(Stage stage) {
        HWND cached = HANDLES.get(stage);
        if (cached != null) return cached;
        String title = stage.getTitle();
        if (title == null || title.isBlank()) return null;
        try {
            HWND hwnd = User32.INSTANCE.FindWindow(null, title);
            if (hwnd != null) HANDLES.put(stage, hwnd);
            return hwnd;
        } catch (Throwable e) {
            return null;
        }
    }
}
