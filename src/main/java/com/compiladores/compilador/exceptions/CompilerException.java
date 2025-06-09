package com.compiladores.compilador.exceptions;

/**
 * Exceção customizada para representar qualquer erro encontrado durante o processo de compilação.
 * Ao lançar essa exceção, o compilador pode parar a execução imediatamente.
 */
public class CompilerException extends Exception {

    public CompilerException(String message) {
        super(message);
    }

}
