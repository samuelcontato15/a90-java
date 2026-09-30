@echo off
setlocal

echo [1/3] Compilando e gerando fat JAR...
call mvn package -q
if errorlevel 1 (
    echo ERRO: falha no mvn package.
    pause & exit /b 1
)

echo [2/3] Criando .exe com jpackage...
if exist "target\dist" rmdir /s /q "target\dist"

jpackage ^
  --type app-image ^
  --input target ^
  --main-jar a90-minigame-1.0.jar ^
  --name A90-Minigame ^
  --dest target\dist ^
  --app-version 1.0 ^
  --java-options "--add-opens=javafx.graphics/com.sun.javafx.stage=ALL-UNNAMED" ^
  --java-options "-Dfile.encoding=UTF-8"

if errorlevel 1 (
    echo ERRO: jpackage falhou. Verifique se o JDK 14+ esta no PATH.
    pause & exit /b 1
)

echo.
echo [3/3] Pronto!
echo Executavel em: target\dist\A90-Minigame\A90-Minigame.exe
echo.
echo Pode copiar a pasta inteira target\dist\A90-Minigame\ para qualquer lugar.
echo Nao precisa de Java instalado para rodar.
echo.
pause
