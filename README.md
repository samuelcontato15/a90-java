# A-90 Minigame — Java/JavaFX

Recriação educacional do minigame A-90 (Archives) de Doors em Java puro.  
Nenhum arquivo do sistema é criado. A única interação com o SO é a troca  
**temporária e reversível** do wallpaper, que volta automaticamente ao encerrar.

## Mecânica

- 7 moedas aparecem espalhadas pelo desktop como janelas arrastáveis.
- Arraste cada moeda até o A-90 (janela direita) antes de 30 segundos.
- **ESC** encerra o jogo a qualquer momento e restaura o wallpaper.

## Pré-requisitos

- JDK 21 (ou 17+)
- Maven 3.8+

## Setup (1 vez)

```bat
cd a90-minigame
copy-assets.bat
```

## Rodar

```bat
mvn javafx:run
```

## Estrutura

```
src/main/java/com/a90/
  App.java            — entry point + shutdown hook de wallpaper
  GameEngine.java     — máquina de estados (spawn, timer, win/lose)
  WallpaperManager.java — JNA user32.dll, salva/restaura wallpaper
  A90Window.java      — janela do A-90 com glitch idle e ataque
  CoinSprite.java     — moeda arrastável transparente always-on-top
  TimerHUD.java       — HUD com cronômetro + contador
  Assets.java         — carregador de imagens e sons

src/main/resources/assets/
  Gold.png, ransom_idle.png, ransom_attack.png
  Sounds/cash.wav, spawn.wav, tauntSpawn.wav, thankyou.wav, attack.wav
```
