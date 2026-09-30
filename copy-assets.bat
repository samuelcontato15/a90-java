@echo off
:: Copia os assets do projeto C# de referência para o projeto Java
:: Execute este arquivo UMA VEZ antes do primeiro build

set SRC=..\ransomdoors-main\Assets
set DST=src\main\resources\assets

mkdir "%DST%\Sounds" 2>nul

:: Imagens do A-90
copy /Y "%SRC%\ransom_idle.png"   "%DST%\ransom_idle.png"
copy /Y "%SRC%\ransom_attack.png" "%DST%\ransom_attack.png"
copy /Y "%SRC%\Gold.png"          "%DST%\Gold.png"

:: Sons
copy /Y "%SRC%\Sounds\cash.wav"       "%DST%\Sounds\cash.wav"
copy /Y "%SRC%\Sounds\spawn.wav"      "%DST%\Sounds\spawn.wav"
copy /Y "%SRC%\Sounds\tauntSpawn.wav" "%DST%\Sounds\tauntSpawn.wav"
copy /Y "%SRC%\Sounds\thankyou.wav"   "%DST%\Sounds\thankyou.wav"
copy /Y "%SRC%\Sounds\attack.wav"     "%DST%\Sounds\attack.wav"

echo Assets copiados com sucesso!
pause
