package com.a90;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinUser;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

final class KillSwitch {

    static final String LABEL = "Ctrl+Alt+Shift+A";

    private static final int MOD_ALT = 0x0001, MOD_CONTROL = 0x0002, MOD_SHIFT = 0x0004, MOD_NOREPEAT = 0x4000;
    private static final int VK_A      = 0x41;
    private static final int WM_HOTKEY = 0x0312;
    private static final int HOTKEY_ID = 0xA90;

    private KillSwitch() {}

    static boolean register(Runnable onPressed) {
        boolean[] ok = {false};
        CountDownLatch registered = new CountDownLatch(1);

        Thread t = new Thread(() -> {
            try {
                ok[0] = User32.INSTANCE.RegisterHotKey(null, HOTKEY_ID,
                        MOD_CONTROL | MOD_ALT | MOD_SHIFT | MOD_NOREPEAT, VK_A);
            } catch (Throwable e) {
                ok[0] = false;
            }
            registered.countDown();
            if (!ok[0]) return;

            WinUser.MSG msg = new WinUser.MSG();
            while (User32.INSTANCE.GetMessage(msg, null, 0, 0) > 0) {
                if (msg.message == WM_HOTKEY && msg.wParam.intValue() == HOTKEY_ID) onPressed.run();
            }
        }, "a90-kill-switch");
        t.setDaemon(true);
        t.start();

        try { registered.await(2, TimeUnit.SECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return ok[0];
    }
}
