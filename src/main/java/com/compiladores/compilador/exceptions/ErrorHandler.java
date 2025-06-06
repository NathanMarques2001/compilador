package com.compiladores.compilador.exceptions;

import com.compiladores.compilador.lexer.Token;

public class ErrorHandler {

    // ========== ERROS LÉXICOS ==========

    public static void lexicalErrorInvalidSymbol(char symbol, int line, int column) throws CompilerException {
        throw new CompilerException("Símbolo inválido: '" + symbol + "' na linha " + line + ", coluna " + column);
    }

    public static void lexicalErrorIdentifierTooLong(String lexeme, int line, int column) throws CompilerException {
        throw new CompilerException("Erro Léxico: identificador '" + lexeme + "' excede o limite de 255 caracteres na linha " + line + ", coluna " + column);
    }

    public static void lexicalErrorStringTooLong(int line, int column) throws CompilerException {
        throw new CompilerException("Erro Léxico: literal string excede o limite de 255 caracteres na linha " + line + ", coluna " + column);
    }

    public static void lexicalErrorBreakLine(int line, int column) throws CompilerException {
        throw new CompilerException("Erro Léxico: literal string não pode conter quebra de linha com '\\n' ou '\\r' na linha " + line + ", coluna " + column);
    }

    public static void lexicalErrorInvalidHexByte(String lexeme, int line, int column) throws CompilerException {
        throw new CompilerException("Erro Léxico: byte hexadecimal inválido! O formato deve ser 0hXX (X vai de 0 a F) '" + lexeme + "' na linha " + line + ", coluna " + column);
    }

    public static void lexicalErrorIntOutOfRange(String lexeme, int line, int column) throws CompilerException {
        throw new CompilerException("Erro Léxico: valor inteiro '" + lexeme + "' fora do intervalo permitido (-32768 a 32767), na linha " + line + ", coluna " + column);
    }

    // ========== ERROS SINTÁTICOS ==========

    public static void syntaxError(String expected, Token found) throws CompilerException {
        throw new CompilerException("Erro Sintático: esperado '" + expected + "', mas encontrado '" +
                found.getName() + "' na linha " + found.getLine() + ", coluna " + found.getColumn());
    }

    public static void syntaxErrorAssignment(Token found) throws CompilerException {
        throw new CompilerException("Erro Sintático: esperado uma atribuição de valor vindo ou não de uma variável, mas encontrado '" +
                found.getName() + "' na linha " + found.getLine() + ", coluna " + found.getColumn());
    }

    public static void syntaxErrorAssignmentLogicalExpression(Token found) throws CompilerException {
        throw new CompilerException("Erro Sintático: atribuição inválida! Não é possível atribuir valor a uma variável vindo de uma expressão lógica '" +
                found.getName() + "' na linha " + found.getLine() + ", coluna " + found.getColumn());
    }

    // ========== ERROS SEMÂNTICOS ==========

    public static void semanticErrorAssignment(Token wrongToken, Token declaredToken) throws CompilerException {
        throw new CompilerException(buildSemanticAssignmentMessage(wrongToken, declaredToken));
    }

    public static void semanticErrorNotDeclared(Token token) throws CompilerException {
        throw new CompilerException(buildSemanticNotDeclaredMessage(token));
    }

    public static void semanticErrorBadComparation(Token token, String expectedType) throws CompilerException {
        throw new CompilerException(buildSemanticBadComparationMessage(token, expectedType));
    }

    public static void semanticErrorInvalidExpression(String expectedType, String actualType, Token token) throws CompilerException {
        throw new CompilerException(buildSemanticInvalidExpressionMessage(expectedType, actualType, token));
    }

    public static void semanticErrorInvalidToken(Token token) throws CompilerException {
        throw new CompilerException("Erro Semântico: expressão inválida com token '" + token.getName() +
                "' na linha " + token.getLine() + ", coluna " + token.getColumn());
    }

    public static void semanticErrorExpectedOperandAfter(String operator, Token token) throws CompilerException {
        throw new CompilerException("Erro Semântico: esperado operando após operador '" + operator +
                "' na linha " + token.getLine() + ", coluna " + token.getColumn());
    }

    public static void semanticErrorInvalidExpressionAfterControl(Token token) throws CompilerException {
        throw new CompilerException("Erro Semântico: expressão inválida após if/while, token '" + token.getName() +
                "' na linha " + token.getLine() + ", coluna " + token.getColumn());
    }

    public static void semanticErrorInvalidType(Token token) throws CompilerException {
        throw new CompilerException("Erro Semântico: tipo inválido ou inexistente '" + token.getName() +
                "' na linha " + token.getLine() + ", coluna " + token.getColumn());
    }

    public static void semanticErrorAssignmentToFinal(Token token) throws CompilerException {
        throw new CompilerException("Erro Semântico: não é permitido atribuir novo valor à variável final '" +
                token.getName() + "' na linha " + token.getLine() + ", coluna " + token.getColumn());
    }

    // ========== MENSAGENS AUXILIARES ==========

    private static String buildSemanticAssignmentMessage(Token wrongToken, Token declaredToken) {
        return "Erro Semântico: atribuição incorreta! Foi atribuído um tipo '" + wrongToken.getType() +
                "' à variável '" + declaredToken.getName() +
                "', mas deveria ter sido atribuído um tipo '" + declaredToken.getType() +
                "', na linha " + wrongToken.getLine() + ", coluna " + wrongToken.getColumn();
    }

    private static String buildSemanticNotDeclaredMessage(Token token) {
        return "Erro Semântico: variável '" + token.getName() + "' não foi declarada! " +
                "Na linha " + token.getLine() + ", coluna " + token.getColumn();
    }

    private static String buildSemanticBadComparationMessage(Token token, String expectedType) {
        return "Erro Semântico: não é possível comparar um tipo '" + token.getType() +
                "' com um tipo '" + expectedType + "'! Na linha " + token.getLine() +
                ", coluna " + token.getColumn();
    }

    private static String buildSemanticInvalidExpressionMessage(String expectedType, String invalidType, Token token) {
        return "Erro Semântico: operação inválida! Era esperado um tipo '" + expectedType +
                "', mas foi utilizado um tipo '" + invalidType +
                "', na linha " + token.getLine() + ", coluna " + token.getColumn();
    }
}
