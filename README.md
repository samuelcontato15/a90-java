# A-90 Minigame — Java/JavaFX · v1.1

Recriação educacional do minigame A-90 (Archives) de Doors em Java puro.  
Nenhum arquivo do sistema é criado. A única interação com o SO é a troca  
**temporária e reversível** do wallpaper, que volta automaticamente ao encerrar
(inclusive se o processo for morto pelo Gerenciador de Tarefas — ver [Segurança](#segurança)).

## Novidades da v1.1

- **Rodada fixa de 1:30**, em 3 fases de 30 s — uma layer da OST por fase. A cada troca
  de layer o jogo fica mais tenso, e os últimos 30 s são desesperadores (ver [Fases](#fases)).
- **Moedas em 3 levas** (uma por fase) que **pulam de lugar até serem pegas**; o débito só
  fecha com moedas da última leva, então toda rodada chega à fase final.
- **Dois modos**: voltar ao menu ao terminar, ou **infinito** (o A-90 volta sozinho em
  intervalos aleatórios até você fechar o app).
- **Atalho global `Ctrl+Alt+Shift+A`** fecha o app de qualquer lugar.
- **Glitch na tela inteira** que vai poluindo a imagem conforme o tempo passa
  (ver [Glitch](#glitch)).
- **Jumpscare visível de verdade**: o código sempre existiu, mas `ransom_attack.gif`
  nunca era empacotado, então a tela só tremia vermelha sem rosto nenhum. Agora o rosto
  aparece, ocupando 78% da altura da tela, com soco de zoom.
- **Todos os assets da pasta `Assets/` em uso** (exceto os do crucifixo). O Maven
  empacota a pasta direto — não existe mais `copy-assets.bat` nem cópia manual.
- **Arte por tier de moeda** (`Gold/Gold1..5.ico`, `HoneyPot.ico`), **cursor infectado**,
  **vinheta vermelha** no aviso e nos jumpscares, **CD girando** no "DOWNLOADING",
  **brilho `Starlight`** a cada moeda entregue e **ícones** de janela e do `.exe`.
- Suporte a `.ico`/`.cur` (o JavaFX não lê esses formatos nativamente) via `IcoDecoder`.
- **Crucifixo removido**: a moeda Crucifix e a `CrucifixWindow` saíram do jogo, e
  `crucifix.ico`, `crucifix.wav`, `ransom_crucifix.png` e `repent.gif` não são empacotados.
- Correções: overlay centralizado (jumpscare e placa STOP saíam no canto da tela),
  música que voltava a tocar depois do fim da rodada, moedas e popups que surgiam após a
  rodada acabar, moedas que nasciam embaixo da janela do A-90, janela do A-90 cortada na
  borda da tela, tela de início duplicada, wallpaper não restaurado quando o fundo
  original era uma cor sólida.

## Mecânica

1. **Aviso** — o rosto do A-90 aparece num ponto aleatório, vai para o centro com a
   placa STOP sobre a vinheta vermelha. **Não mexa o mouse** por 0,5 s:
   se ficar parado, você desvia e a rodada termina sem resgate.
2. **Instalação** — se mexeu: jumpscare, e a tela "DOWNLOADING..." com um CD girando.
3. **Resgate (1:30)** — o wallpaper muda, popups de taunt aparecem e as moedas se
   espalham pelo desktop. Arraste moedas até a janela do A-90 (direita) até zerar o
   débito. A cada 30 s a música troca e o jogo aperta.
4. **Fim** — pagou: animação "THANK YOU". Tempo esgotado: jumpscare final.
   O wallpaper é restaurado e o jogo volta ao menu (ou, no modo infinito, some até o
   próximo ataque).

### Fases

| Tempo     | Layer        | Ao entrar                        | Popups                         | Moedas                          | Tremor |
|-----------|--------------|----------------------------------|--------------------------------|---------------------------------|-------:|
| 1:30–1:00 | `layer1.wav` | leva 1 de moedas + 9 popups      | se multiplicam (2%)            | paradas                         |   5 px |
| 1:00–0:30 | `layer2.wav` | leva 2 + 5 popups + `spawn.wav`  | 1 novo a cada 2 s, 3%          | pulam (~a cada 4 s)             |   9 px |
| 0:30–0:00 | `layer3.wav` | leva 3 + 10 popups + `spawn.wav` | 1 novo a cada 0,6 s, 4%        | pulam (~a cada 1,4 s)           |  16 px |

- "Se multiplicam (N%)": a cada 200 ms, cada popup aberto tem N% de chance de abrir outro.
- No máximo 30 popups abertos ao mesmo tempo; a janela do A-90 é trazida para a frente
  sempre que um popup abre, e uma moeda que pula volta para cima dos popups.
- A moeda que está sendo arrastada nunca pula.
- Cada layer tem 26,18 s e fica em loop até a troca de fase.
- Todos os números ficam em `Phase.java`, para ajustar a dificuldade.

### Glitch

Uma camada cobre a tela inteira durante o resgate e vai sujando a imagem: começa
imperceptível e no fim atrapalha de verdade enxergar as moedas. A intensidade sobe de
forma contínua com o tempo (curva `t^1.6`), e cada troca de layer dá um pico curto.

| Efeito | O que é |
|---|---|
| Estática | `static.gif` ladrilhado, deslocado a cada quadro |
| Rasgos | Fatias horizontais aleatórias dos `Taunts/glitch*` esticadas na largura da tela, deslocadas na horizontal, com blend variado |
| Separação de cor | Faixas vermelhas/ciano deslocadas |
| Scanlines | Linhas escuras de 3 px |
| Vinheta | `red_vignette.gif` pulsando |
| Piscadas | Inversão de cor da tela e rostos (`tauntface`, `idiot`, `tauntflower`, `ransom_idle`) em tela cheia |

A camada é **click-through**: é só pintura, os cliques atravessam para as moedas e para
a barra de tarefas. Ela nunca recebe foco, então o `ESC` e o atalho global continuam
valendo. Cobre só a área útil da tela, deixando a barra de tarefas visível.

Medido a 1920x1032: no pico, cerca de **63%** do que está embaixo ainda aparece.
Os tetos ficam nas constantes `MAX_*` de `GlitchOverlay.java` — suba-os para sujar mais.

> **Epilepsia fotossensível**: as piscadas de tela cheia são limitadas a no máximo uma a
> cada 0,42 s (medido: ~1,1 por segundo no pico), abaixo do limiar usual de 3 flashes por
> segundo. A constante é `FLASH_GAP_TICKS`. Considere quem vai jogar antes de afrouxá-la.

### Moedas

Cada fase traz uma leva de 7 moedas sorteadas, mais uma HoneyPot com 30% de chance
(ela chega 3,5 s depois da leva). Moedas não pegas continuam na tela.

| Tier     | Valor | Chance | Sprite              |
|----------|------:|-------:|---------------------|
| Gold 1   |    10 |    40% | `Gold/Gold1.ico`    |
| Gold 2   |    50 |    30% | `Gold/Gold2.ico`    |
| Gold 3   |   100 |    15% | `Gold/Gold3.ico`    |
| Gold 4   |   150 |     8% | `Gold/Gold4.ico`    |
| Gold 5   |   200 |     7% | `Gold/Gold5.ico`    |
| HoneyPot |   500 |    30% | `Gold/HoneyPot.ico` |

**Débito** = valor total das levas 1 e 2 + metade da leva 3. Não dá para pagar antes da
fase final, mas sobra folga: dá para deixar ~20% do valor em moedas para trás
(média de 10.000 rodadas simuladas: débito 1421, folga 285).

## Modos

Escolha em **[ configuração ]** na tela de início:

- **Voltar ao menu** (padrão) — ao terminar a rodada (vitória, derrota, desvio ou ESC),
  a tela de início abre de novo.
- **Infinito** — o primeiro ataque começa ao clicar INICIAR. Ao terminar cada rodada,
  o A-90 some e volta sozinho após um intervalo aleatório (padrão: entre 15 e 45 s,
  ~30 s em média), para sempre. Entre um ataque e outro não há janela nenhuma aberta.
  Para parar: **`Ctrl+Alt+Shift+A`**, ou encerrar o processo pelo Gerenciador de Tarefas
  (`A90-Minigame.exe` no `.exe`, `java.exe` / `javaw.exe` ao rodar pelo Maven).

## Controles

| Tecla / ação         | Efeito |
|----------------------|--------|
| Arrastar             | Move as moedas |
| `ESC`                | Encerra a rodada atual e restaura o wallpaper (no modo infinito o A-90 volta depois). Na tela de início, fecha o jogo |
| `Ctrl+Alt+Shift+A`   | Fecha o app de qualquer lugar, em qualquer modo (atalho global) |

Se outro programa já usar `Ctrl+Alt+Shift+A`, a tela de início avisa (no modo infinito)
e o modo infinito só para pelo Gerenciador de Tarefas.

## Configuração

Salva em `%APPDATA%\a90minigame\config.properties`.

| Opção                    | Padrão         | Faixa  |
|--------------------------|----------------|--------|
| Ao terminar a rodada     | voltar ao menu | menu / infinito |
| Intervalo mín/máx (s)    | 15 / 45        | 1–3600 (só no modo infinito) |

A duração (1:30) e o débito são fixos do jogo.

## Segurança

- O wallpaper original é restaurado ao fim de cada rodada, com ESC, com o atalho e ao
  fechar o app.
- Antes de trocar o wallpaper, o original é anotado em
  `%APPDATA%\a90minigame\wallpaper.state`. Se o processo for morto no meio de um ataque
  (Gerenciador de Tarefas), o wallpaper do A-90 fica até a **próxima execução** do jogo,
  que o restaura automaticamente ao abrir.
- Se não for possível ler o wallpaper atual, o jogo não troca o wallpaper.

## Pré-requisitos

- JDK 21+
- Maven 3.8+

## Rodar

```bat
mvn javafx:run
```

Não há passo de setup: os assets vêm direto de `Assets/`.

## Gerar o .exe

```bat
build-exe.bat
```

Gera `target\dist\A90-Minigame\A90-Minigame.exe` (com o ícone `stop_sign.ico`),
que roda sem Java instalado. Ao mudar a versão no `pom.xml`, atualize `VERSION` no script.

## Assets

Tudo em `Assets/` é empacotado como `/assets` no classpath (ver `<resources>` no `pom.xml`).

| Arquivo                                   | Uso |
|-------------------------------------------|-----|
| `ransom_idle.png`                         | Rosto do A-90 no aviso e na janela de resgate |
| `ransom_attack.gif`                       | Jumpscares (instalação e derrota), em tela cheia |
| `ransom_attack.png`                       | Wallpaper temporário durante o resgate |
| `stop_sign.png`                           | Placa STOP da fase de aviso |
| `red_vignette.gif`                        | Vinheta no aviso, nos jumpscares e na camada de glitch |
| `static.gif`                              | Estática no jumpscare, no download e na camada de glitch |
| `CD-1.png`                                | CD girando na tela "DOWNLOADING" |
| `Gold.png`                                | Ícone ao lado do débito na janela de resgate |
| `Gold/Gold1..5.ico`, `Gold/HoneyPot.ico`  | Sprites (e ícones de janela) de cada moeda |
| `Starlight.png`                           | Brilho ao entregar uma moeda |
| `infectedcursor.cur`                      | Cursor nas janelas do jogo após a infecção |
| `ok_sign.png`, `thx_txt.png`              | Animação de vitória "THANK YOU" |
| `Taunts/*`                                | Imagens dos popups de taunt e das faixas rasgadas do glitch |
| `stop_sign.ico`                           | Ícone do app, das janelas e do `.exe` |
| `CD-1.ico`                                | Ícone da janela de instalação (overlay) |
| `Gold.ico`                                | Ícone da janela de resgate |
| `Sounds/spawn.wav`                        | A-90 aparece; também marca cada troca de fase |
| `Sounds/attack.wav`                       | Jumpscares |
| `Sounds/install.wav`                      | Início do "DOWNLOADING" |
| `Sounds/layer1..3.wav`                    | OST do resgate, uma layer por fase (30 s cada, em loop) |
| `Sounds/cash.wav`                         | Moeda entregue |
| `Sounds/tauntSpawn.wav`, `tauntLeave.wav` | Popup de taunt abre / fecha sozinho |
| `Sounds/thankyou.wav`                     | Vitória |

Fora do build (crucifixo): `crucifix.ico`, `ransom_crucifix.png`, `repent.gif`,
`Sounds/crucifix.wav`.

## Estrutura

```
Assets/                  — imagens, ícones, cursor e sons (fonte única dos assets)

src/main/java/com/a90/
  App.java               — entry point, tela de início, shutdown hook, recuperação do wallpaper
  Launcher.java          — main() para fat JAR / jpackage
  GameEngine.java        — máquina de estados (aviso, instalação, resgate, win/lose, modos)
  Phase.java             — as 3 fases do resgate e a intensidade de cada uma
  GameConfig.java        — configurações persistidas em %APPDATA%
  ConfigWindow.java      — janela de configuração
  KillSwitch.java        — atalho global Ctrl+Alt+Shift+A (JNA RegisterHotKey)
  OverlayWindow.java     — overlay full-screen: aviso, jumpscares, download
  GlitchOverlay.java     — camada de glitch click-through que polui a tela
  Win32Window.java       — click-through e trazer para frente sem roubar foco (user32)
  RansomWindow.java      — janela do A-90 onde as moedas são entregues
  TauntWindow.java       — popups de glitch/taunt
  ThankYouWindow.java    — animação de vitória
  CoinSprite.java        — moeda arrastável transparente always-on-top
  CoinType.java          — tiers de moeda (valor, chance, sprite)
  StarlightBurst.java    — brilho ao entregar moeda
  MusicPlayer.java       — OST em layers sequenciais
  Assets.java            — carregador de imagens, ícones, cursores e sons
  IcoDecoder.java        — decodificador de .ico/.cur (PNG e BMP)
  WallpaperManager.java  — JNA user32.dll, salva/restaura wallpaper
  A90Window.java, TimerHUD.java — stubs legados (substituídos)
```
