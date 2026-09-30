package com.a90;

/**
 * Entry point para fat JAR / jpackage.
 *
 * JavaFX exige que a classe com main() NÃO estenda Application
 * quando empacotada num fat JAR — caso contrário lança erro ao iniciar.
 * Esta classe resolve isso delegando para App.main().
 */
public class Launcher {
    public static void main(String[] args) {
        App.main(args);
    }
}
