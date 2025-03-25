package com.compiladores.compilador.Exceptions;

import com.compiladores.compilador.Table.Symbol;

public class ErrorHandler {
  public static void lexicalError(String found) throws CompilerException {
    throw new CompilerException("Erro Léxico: Token inválido encontrado -> " + found);
  }

  public static void syntaxError(String expected, String found) throws CompilerException {
    throw new CompilerException(
        "Erro Sintático: esperado '" + expected + "', mas encontrado '"
            + found + "'");
  }

  public static void semanticError(String message) throws CompilerException {
    throw new CompilerException("Erro Semântico: " + message);
  }
}
