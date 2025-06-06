package com.compiladores.compilador.utils;

import com.compiladores.compilador.lexer.Token;

public class TokenUtils {

    public static boolean isLogicalOperator(String op) {
        return op.equals("==") || op.equals("<") || op.equals("<=") ||
                op.equals(">") || op.equals(">=") || op.equals("<>") ||
                op.equalsIgnoreCase("and") || op.equalsIgnoreCase("or") ||
                op.equalsIgnoreCase("not");
    }

    public static boolean isArithmeticOperator(String op) {
        return op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/");
    }

    public static boolean isPrimitiveType(Token token) {
        String name = token.getName().toLowerCase();
        return name.equals("int") || name.equals("string") ||
                name.equals("boolean") || name.equals("byte");
    }

    public static boolean isConstOrId(Token token) {
        String classification = token.getClassification();
        return classification.equalsIgnoreCase("const") || classification.equalsIgnoreCase("id");
    }

    public static boolean isLogicalOp(Token token) {
        return isLogicalOperator(token.getName());
    }

    public static boolean isArithmeticOp(Token token) {
        return isArithmeticOperator(token.getName());
    }
}
