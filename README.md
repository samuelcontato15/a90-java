# A-90 Minigame — Java/JavaFX

An educational recreation of the A-90 minigame from DOORS: Archives, built in pure Java.

The system wallpaper is **never touched**. On a loss, a full-screen crash-image window covers the desktop for 3 seconds, then the app closes and the window disappears — Wallpaper Engine and normal desktops resume instantly.

## Gameplay

1. **Warning** — A-90's face appears at a random position, then centers with a STOP sign and red vignette. Keep the mouse still for 0.5 s to dodge; moving triggers the attack.
2. **Install** — Jumpscare, then a "DOWNLOADING…" screen with a spinning CD.
3. **Ransom (1:18)** — Popup windows flood the screen and coins scatter across the desktop. Drag coins to the A-90 window to pay off the debt. Every 26 s the music escalates and the game intensifies.
4. **Win** — Paid in full: "THANK YOU" animation, then the start screen.
5. **Lose** — Crash jumpscare, freeze effect, crash-image window fills the screen for 3 seconds, app closes.

## Phases

| Time      | Layer        | On enter                       | Popups                         | Coins                       | Shake |
|-----------|--------------|--------------------------------|--------------------------------|-----------------------------|------:|
| 1:18–0:52 | `layer1.wav` | 3 popups                       | 1 new every 5 s, 2% multiply  | stationary                  |  5 px |
| 0:52–0:26 | `layer2.wav` | 2 popups                       | 1 new every 2 s, 3% multiply  | jump every ~4 s             |  9 px |
| 0:26–0:00 | `layer3.wav` | 3 popups                       | 1 new every 0.6 s, 4% multiply| jump every ~1.4 s           | 16 px |

- Each layer plays exactly once for its 26 s phase — no looping, no gaps between tracks.
- Layer 3 is timed to end exactly at 0 s.
- "N% multiply": every 200 ms each open popup has an N% chance to spawn another.
- Max 30 popups open at once. Coins are always raised above popups.
- The coin being dragged never jumps.
- All values are in `Phase.java`.

## Coins

Each phase spawns 7 weighted-random coins plus a HoneyPot (30% chance, arrives 3.5 s late).

| Tier     | Value | Chance | Sprite              |
|----------|------:|-------:|---------------------|
| Gold 1   |    10 |    40% | `Gold/Gold1.ico`    |
| Gold 2   |    50 |    30% | `Gold/Gold2.ico`    |
| Gold 3   |   100 |    15% | `Gold/Gold3.ico`    |
| Gold 4   |   150 |     8% | `Gold/Gold4.ico`    |
| Gold 5   |   200 |     7% | `Gold/Gold5.ico`    |
| HoneyPot |   500 |    30% | `Gold/HoneyPot.ico` |

**Debt** = full value of waves 1 and 2 + half of wave 3. Impossible to win before the final phase, but there is ~20% slack.

## Glitch overlay

A click-through full-screen layer pollutes the image continuously during the ransom phase. Intensity follows a `t^1.6` curve; each phase transition adds a short spike.

| Effect         | Description |
|----------------|-------------|
| Static         | `static.gif` tiled, shifted every frame |
| Tears          | Horizontal slices of `Taunts/glitch*` stretched full-width |
| Color split    | Red/cyan offset bands |
| Scanlines      | 3 px dark lines |
| Vignette       | `red_vignette.gif` pulsing |
| Flash          | Full-screen color inversion + face images (≤ 1.1/s, below the 3 Hz photosensitivity threshold) |

## Wallpaper

The system wallpaper is **never touched**. On a loss:

1. Crash jumpscare + 2-second freeze effect.
2. A full-screen window cycling through glitch/crash images opens.
3. After 3 seconds the app closes; the window closes with it, revealing whatever was behind it.

Wallpaper Engine users see their animated wallpaper resume immediately. No restore logic needed.

## Modes

Select in **[ config ]** on the start screen:

- **Return to menu** (default) — after a win, dodge, or ESC, the start screen reopens.
- **Infinite** — A-90 returns automatically after a random interval (default 15–45 s). Use `Ctrl+Alt+Shift+A` or Task Manager to stop it. A loss always closes the app regardless of mode.

## Controls

| Input               | Effect |
|---------------------|--------|
| Drag                | Move coins |
| `ESC`               | Ends the current round; closes the app from the start screen |
| `Ctrl+Alt+Shift+A`  | Closes the app from anywhere (global hotkey) |

## Configuration

Saved to `%APPDATA%\a90minigame\config.properties`.

| Option             | Default        | Range |
|--------------------|----------------|-------|
| After round        | return to menu | menu / infinite |
| Min/max delay (s)  | 15 / 45        | 1–3600 (infinite mode only) |

## Requirements (to build)

- JDK 21+ (includes `jpackage`) — https://adoptium.net
- Maven 3.8+

Make sure both are on your PATH:

```bat
java -version
jpackage --version
mvn -version
```

If `jpackage` is not found but Java is installed, add the JDK `bin` folder to PATH manually:
`Win + R` → `sysdm.cpl` → Advanced → Environment Variables → System `Path` → New → `C:\Program Files\Java\jdk-XX\bin`
Then open a new terminal window.

The recipient needs **nothing installed** — the `.exe` bundles its own Java runtime.

## Run (dev)

```bat
mvn javafx:run
```

Assets are bundled from `Assets/` — no extra setup.

## Build .exe (distribution)

```bat
build-exe.bat
```

Produces `target\dist\A90-Minigame\`. Zip that entire folder and send it — the recipient just extracts and double-clicks `A90-Minigame.exe`.

Update `VERSION` in `build-exe.bat` if you bump the version in `pom.xml`.

## Assets

All files under `Assets/` are packaged as `/assets` on the classpath (see `<resources>` in `pom.xml`).

| File                                      | Use |
|-------------------------------------------|-----|
| `ransom_idle.png`                         | A-90 face in warning and ransom window |
| `ransom_attack.gif`                       | Jumpscares (full-screen) |
| `ransom_attack.png`                       | Wallpaper on loss |
| `stop_sign.png`                           | STOP sign in warning phase |
| `red_vignette.gif`                        | Vignette in warning, jumpscares, glitch layer |
| `static.gif`                              | Static in jumpscare, download, glitch layer, ransom background |
| `CD-1.png`                                | Spinning CD in "DOWNLOADING" |
| `Gold.png`                                | Icon beside the debt counter |
| `Gold/Gold1..5.ico`, `Gold/HoneyPot.ico`  | Coin sprites and window icons |
| `Starlight.png`                           | Burst effect on coin delivery |
| `infectedcursor.cur`                      | Infected cursor after install |
| `ok_sign.png`, `thx_txt.png`              | "THANK YOU" victory animation |
| `Taunts/glitch1..5`, `Taunts/*.png`       | Popup images, glitch tear slices, ransom background cycling |
| `stop_sign.ico`                           | App, window, and .exe icon |
| `CD-1.ico`                                | Install overlay window icon |
| `Gold.ico`                                | Ransom window icon |
| `Sounds/spawn.wav`                        | A-90 appears (warning phase) |
| `Sounds/attack.wav`                       | Jumpscares |
| `Sounds/install.wav`                      | "DOWNLOADING" start |
| `Sounds/layer1..3.wav`                    | Ransom OST — one layer per 26 s phase, played once each |
| `Sounds/cash.wav`                         | Coin delivered |
| `Sounds/thankyou.wav`                     | Victory |

Not packaged (crucifix content removed): `crucifix.ico`, `ransom_crucifix.png`, `repent.gif`, `Sounds/crucifix.wav`.
Unused popup sounds (removed): `Sounds/tauntSpawn.wav`, `Sounds/tauntLeave.wav`.

## Structure

```
Assets/                  — images, icons, cursor, sounds (single asset source)

src/main/java/com/a90/
  App.java               — entry point, start screen, shutdown hook, crash recovery
  Launcher.java          — main() for fat JAR / jpackage
  GameEngine.java        — state machine (warning → install → ransom → win/lose → cleanup)
  Phase.java             — 3 ransom phases and their intensity values
  GameConfig.java        — settings persisted to %APPDATA%
  ConfigWindow.java      — config dialog
  KillSwitch.java        — global hotkey Ctrl+Alt+Shift+A (JNA RegisterHotKey)
  OverlayWindow.java     — full-screen overlay: warning, jumpscares, download
  GlitchOverlay.java     — click-through glitch layer over the ransom phase
  Win32Window.java       — click-through and no-steal-focus raise (user32)
  RansomWindow.java      — A-90 window where coins are delivered
  TauntWindow.java       — glitch popup windows
  ThankYouWindow.java    — victory animation
  CoinSprite.java        — draggable always-on-top coin
  CoinType.java          — coin tiers (value, weight, sprite)
  StarlightBurst.java    — burst effect on coin delivery
  MusicPlayer.java       — OST layer sequencing (each track plays once, back-to-back)
  Assets.java            — image, icon, cursor, and sound loader/cache
  IcoDecoder.java        — .ico/.cur decoder (PNG and BMP)
  WallpaperManager.java  — saves and restores wallpaper; applied only on loss
  A90Window.java, TimerHUD.java — legacy stubs
```
