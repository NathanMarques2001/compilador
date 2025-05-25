package com.compiladores.compilador.Exceptions;

import com.compiladores.compilador.Lexical.Token;

public class ErrorHandler {

    public static void lexicalError(String lexeme, int line, int column) throws CompilerException {
        throw new CompilerException(buildLexicalMessage(lexeme, line, column));
    }

    public static void syntaxError(String expected, Token found) throws CompilerException {
        throw new CompilerException(buildSyntaxMessage(expected, found));
    }

    public static void syntaxErrorAssignment(Token found) throws CompilerException {
        throw new CompilerException(buildSyntaxAssignmentMessage(found));
    }

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
                "Erro Semantico: expressão inválida com token '" + token.getName() +
                        "' na linha " + token.getLine() + ", coluna " + token.getColumn()
        );
    }

    public static void semanticErrorExpectedOperandAfter(String operator, Token token) throws CompilerException {
        throw new CompilerException(
                "Erro Semantico: esperado operando após operador '" + operator +
                        "' na linha " + token.getLine() + ", coluna " + token.getColumn()
        );
    }

    public static void semanticErrorInvalidExpressionAfterControl(Token token) throws CompilerException {
        throw new CompilerException(
                "Erro Semantico: expressão inválida após if/while, token '" + token.getName() +
                        "' na linha " + token.getLine() + ", coluna " + token.getColumn()
        );
    }

    private static String buildLexicalMessage(String lexeme, int line, int column) {
        return "Erro Lexico: Token nao reconhecido '" + lexeme + "' na linha " + line + ", coluna " + column;
    }

    private static String buildSyntaxMessage(String expected, Token found) {
        return "Erro Sintatico: esperado '" + expected + "', mas encontrado '" +
                found.getName() + "' na linha " + found.getLine() + ", coluna " + found.getColumn();
    }

    private static String buildSyntaxAssignmentMessage(Token found) {
        return "Erro Sintatico: esperado uma atribuicao de valor vindo ou nao de uma variavel, mas encontrado '" +
                found.getName() + "' na linha " + found.getLine() + ", coluna " + found.getColumn();
    }

    private static String buildSemanticAssignmentMessage(Token wrongToken, Token declaredToken) {
        return "Erro Semantico: atribuicao incorreta! Foi atribuido um tipo '" + wrongToken.getType() +
                "' a variavel '" + declaredToken.getName() +
                "', mas deveria ter sido atribuido um tipo '" + declaredToken.getType() +
                "', na linha " + wrongToken.getLine() + ", coluna " + wrongToken.getColumn();
    }

    private static String buildSemanticNotDeclaredMessage(Token token) {
        return "Erro Semantico: variavel '" + token.getName() + "', nao foi declarada! " +
                "Na linha " + token.getLine() + ", coluna " + token.getColumn();
    }

    private static String buildSemanticBadComparationMessage(Token token, String expectedType) {
        return "Erro Semantico: nao eh possivel comparar um tipo '" + token.getType() +
                "', com um tipo '" + expectedType + "'! Na linha " + token.getLine() +
                ", coluna " + token.getColumn();
    }

    private static String buildSemanticInvalidExpressionMessage(String expectedType, String invalidType, Token token) {
        return "Erro Semantico: operacao invalida! Era esperado um tipo '" + expectedType +
                "', mas foi declarado um tipo '" + invalidType + "', na linha " + token.getLine();
    }
}
