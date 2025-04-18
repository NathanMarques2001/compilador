package com.compiladores.compilador.Exceptions;

import com.compiladores.compilador.Lexical.Token;

public class ErrorHandler {

    public static void lexicalError(String lexeme, int line, int column) throws CompilerException {
        throw new CompilerException(
                "Erro Lexico: Token nao reconhecido '" + lexeme + "' na linha " + line + ", coluna " + column);
    }

    public static void syntaxError(String expected, Token found) throws CompilerException {
        throw new CompilerException(
                "Erro Sintatico: esperado '" + expected + "', mas encontrado '" +
                        found.getName() + "' na linha " + found.getLine() + ", coluna " + found.getColumn()
        );
    }

    public static void syntaxErrorAssignment(Token found) throws CompilerException {
        throw new CompilerException(
                "Erro Sintatico: esperado uma atribuicao de valor vindo ou nao de uma variavel, mas encontrado '" +
                        found.getName() + "' na linha " + found.getLine() + ", coluna " + found.getColumn()
        );
    }

    public static void semanticError(String message) throws CompilerException {
        throw new CompilerException("Erro Semantico: " + message);
    }
}
