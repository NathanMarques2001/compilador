package com.compiladores.compilador.Exceptions;

import com.compiladores.compilador.Lexical.Token;

public class ErrorHandler {

    // ========== ERROS LÉXICOS ==========

    public static void lexicalError(String lexeme, int line, int column) throws CompilerException {
        throw new CompilerException(buildLexicalMessage(lexeme, line, column));
    }

    public static void lexicalErrorIdentifierTooLong(String lexeme, int line, int column) throws CompilerException {
        throw new CompilerException("Erro Léxico: identificador '" + lexeme + "' excede o tamanho máximo permitido na linha " + line + ", coluna " + column);
    }

    public static void lexicalErrorStringTooLong(String lexeme, int line, int column) throws CompilerException {
        throw new CompilerException("Erro Léxico: literal string excede o limite de 255 caracteres na linha " + line + ", coluna " + column);
    }

    public static void lexicalErrorEmptyString(int line, int column) throws CompilerException {
        throw new CompilerException("Erro Léxico: literal string vazia na linha " + line + ", coluna " + column);
    }

    public static void lexicalErrorInvalidHexByte(String lexeme, int line, int column) throws CompilerException {
        throw new CompilerException("Erro Léxico: byte hexadecimal inválido '" + lexeme + "' na linha " + line + ", coluna " + column);
    }

    public static void lexicalErrorIntOutOfRange(String lexeme, int line, int column) throws CompilerException {
        throw new CompilerException("Erro Léxico: valor inteiro '" + lexeme + "' fora do intervalo permitido (-32768 a 32767), na linha " + line + ", coluna " + column);
    }

    private static String buildLexicalMessage(String lexeme, int line, int column) {
        return "Erro Léxico: token não reconhecido '" + lexeme + "' na linha " + line + ", coluna " + column;
    }

    // ========== ERROS SINTÁTICOS ==========

    public static void syntaxError(String expected, Token found) throws CompilerException {
        throw new CompilerException(buildSyntaxMessage(expected, found));
    }

    public static void syntaxErrorAssignment(Token found) throws CompilerException {
        throw new CompilerException(buildSyntaxAssignmentMessage(found));
    }

    private static String buildSyntaxMessage(String expected, Token found) {
        return "Erro Sintático: esperado '" + expected + "', mas encontrado '" +
                found.getName() + "' na linha " + found.getLine() + ", coluna " + found.getColumn();
    }

    private static String buildSyntaxAssignmentMessage(Token found) {
        return "Erro Sintático: esperado uma atribuição de valor vindo ou não de uma variável, mas encontrado '" +
                found.getName() + "' na linha " + found.getLine() + ", coluna " + found.getColumn();
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
        throw new CompilerException(
                "Erro Semântico: expressão inválida com token '" + token.getName() +
                        "' na linha " + token.getLine() + ", coluna " + token.getColumn()
        );
    }

    public static void semanticErrorExpectedOperandAfter(String operator, Token token) throws CompilerException {
        throw new CompilerException(
                "Erro Semântico: esperado operando após operador '" + operator +
                        "' na linha " + token.getLine() + ", coluna " + token.getColumn()
        );
    }

    public static void semanticErrorInvalidExpressionAfterControl(Token token) throws CompilerException {
        throw new CompilerException(
                "Erro Semântico: expressão inválida após if/while, token '" + token.getName() +
                        "' na linha " + token.getLine() + ", coluna " + token.getColumn()
        );
    }

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
