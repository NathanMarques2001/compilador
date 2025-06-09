package com.compiladores.compilador.utils;

import com.compiladores.compilador.lexer.Token;

/**
 * Classe utilitária com métodos estáticos para facilitar a verificação
 * de propriedades de Tokens. Isso ajuda a evitar a repetição de código
 * e torna os analisadores mais legíveis.
 */
public class TokenUtils {

    // Verifica se uma string representa um operador lógico ou relacional.
    public static boolean isLogicalOperator(String op) {
        return op.equals("==") || op.equals("<") || op.equals("<=") ||
                op.equals(">") || op.equals(">=") || op.equals("<>") ||
                op.equalsIgnoreCase("and") || op.equalsIgnoreCase("or") ||
                op.equalsIgnoreCase("not");
    }

    // Verifica se uma string representa um operador aritmético.
    public static boolean isArithmeticOperator(String op) {
        return op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/");
    }

    // Verifica se um token representa um tipo de dado primitivo da linguagem.
    public static boolean isPrimitiveType(Token token) {
        String name = token.getName().toLowerCase();
        return name.equals("int") || name.equals("string") ||
                name.equals("boolean") || name.equals("byte");
    }

    // Verifica se a classificação de um token é "const" (constante) ou "id" (identificador).
    public static boolean isConstOrId(Token token) {
        String classification = token.getClassification();
        return classification.equalsIgnoreCase("const") || classification.equalsIgnoreCase("id");
    }

    // Atalho para verificar se um token é um operador lógico.
    public static boolean isLogicalOp(Token token) {
        return isLogicalOperator(token.getName());
    }

    // Atalho para verificar se um token é um operador aritmético.
    public static boolean isArithmeticOp(Token token) {
        return isArithmeticOperator(token.getName());
    }
}