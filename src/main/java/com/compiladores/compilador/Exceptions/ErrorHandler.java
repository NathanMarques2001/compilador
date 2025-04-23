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

    public static void semanticErrorAssignment(Token found, String correctType) throws CompilerException {
        throw new CompilerException(
                "Erro Semantico: atribuicao incorreta! Foi atribuido um tipo '" +
                        found.getType() + "' a variavel '" + found.getName() + "', mas deveria ter sido atribuido um tipo '" + correctType + "', na linha " + found.getLine() + ", coluna " + found.getColumn()
        );
    }

    public static void semanticErrorNotDeclared(Token found) throws CompilerException {
        throw new CompilerException(
                "Erro Semantico: variavel '" + found.getName() + "', nao foi declarada! Na linha " + found.getLine() + ", coluna " + found.getColumn()
        );
    }

    public static void semanticErrorBadComparation(Token found, String correctType) throws CompilerException {
        throw new CompilerException(
                "Erro Semantico: nao eh possivel comparar um tipo '" + found.getType() + "', com um tipo '" + correctType + "'! Na linha " + found.getLine() + ", coluna " + found.getColumn()
        );
    }

    public static void semanticErrorInvalidArithmetic(Token found, String invalidType) throws CompilerException {
        throw new CompilerException(
                "Erro Semantico: operacao aritmetica invalida com tipo '" + invalidType +
                        "' em '" + found.getName() + "', na linha " + found.getLine() + ", coluna " + found.getColumn()
        );
    }
}
