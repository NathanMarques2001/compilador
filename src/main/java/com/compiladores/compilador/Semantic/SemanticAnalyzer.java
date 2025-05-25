package com.compiladores.compilador.Semantic;

import com.compiladores.compilador.Exceptions.CompilerException;
import com.compiladores.compilador.Exceptions.ErrorHandler;
import com.compiladores.compilador.Lexical.Token;
import com.compiladores.compilador.Table.SymbolsTable;

import java.util.ArrayList;

public class SemanticAnalyzer {

    private final SymbolsTable symbolsTable;
    private Token currentToken;
    private int currentTokenIndex = 0;
    private String currentType;
    private final ArrayList<Token> declaredTokens;

    public SemanticAnalyzer(SymbolsTable symbolsTable) {
        this.symbolsTable = symbolsTable;
        this.currentToken = symbolsTable.currentToken(this.currentTokenIndex);
        this.declaredTokens = new ArrayList<>();
    }

    private void nextToken() {
        this.currentToken = this.symbolsTable.currentToken(++this.currentTokenIndex);
    }

    private void previousToken() {
        this.currentToken = this.symbolsTable.currentToken(--this.currentTokenIndex);
    }

    private void expectAssignment(Token token) throws CompilerException {
        if (!this.currentToken.getType().equalsIgnoreCase(token.getType())) {
            System.out.println("CurrentToken" + this.currentToken + " is not of type " + this.currentToken.getType());
            ErrorHandler.semanticErrorAssignment(this.currentToken, token);
        }
    }

    public void analyze() throws CompilerException {
        try {
            this.checkDeclarations();
            this.setSymbolsTableTypes();
            this.checkAssignments();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    void checkDeclarations() throws CompilerException {
        if (isPrimitiveType() || this.currentToken.getName().equals("final")) {
            this.currentType = this.currentToken.getName();
            this.nextToken();
            Token declaredToken = this.currentToken; // variavel declarada
            declaredToken.setType(this.currentType);
            this.declaredTokens.add(declaredToken);
            this.nextToken();
            if (this.currentToken.getName().equals("=")) {
                this.nextToken();
                if (this.currentType.equals("final")) {
                    this.currentType = this.currentToken.getType();
                    declaredToken.setType(this.currentToken.getType());
                }
                this.expectAssignment(declaredToken);
                this.nextToken();
            }
            this.nextToken();
            this.checkDeclarations();
        }
    }

    void checkAssignments() throws CompilerException {
        this.nextToken();
        if (this.currentToken.getClassification().equals("ID")) {
            if (!this.isDeclared()) {
                ErrorHandler.semanticErrorNotDeclared(this.currentToken);
            }
            this.checkAssignment();
        }
        if (this.currentToken.getName().equals("while") || this.currentToken.getName().equals("if")) {
            this.currentType = "boolean";
            this.checkBooleanExpressionAfterIfOrWhile();
        }
        if (this.currentTokenIndex < this.symbolsTable.getSize() - 1) {
            this.checkAssignments();
        }
    }

    void checkAssignment() throws CompilerException {
        for (Token token : this.declaredTokens) {
            if (!token.getName().equals(this.currentToken.getName())) continue;

            this.currentType = token.getType();
            this.nextToken();

            if (this.currentToken.getName().equals("=")) {
                this.nextToken();
                if (this.isNotExpression()) {
                    this.expectAssignment(token);
                    this.nextToken();
                } else {
                    this.verifyExpression();
                }
            } else if (this.identifyLogicalOperator()) {
                this.nextToken();
                if (!this.currentToken.getType().equalsIgnoreCase(this.currentType)) {
                    ErrorHandler.semanticErrorBadComparation(token, currentType);
                }
                this.nextToken();
            } else if (!currentType.equalsIgnoreCase("boolean") && this.currentToken.getName().equals("begin")) {
                ErrorHandler.semanticErrorBadComparation(token, "boolean");
                this.nextToken();
            }
        }
    }

    void verifyExpression() throws CompilerException {
        String expressionType = evaluateExpressionType();

        if (!expressionType.equalsIgnoreCase(this.currentType)) {
            ErrorHandler.semanticErrorInvalidExpression(this.currentType, expressionType, this.currentToken);
        }
    }

    String evaluateExpressionType() throws CompilerException {
        String leftType = null;

        if (this.currentToken.getName().equals("(")) {
            this.nextToken();
            leftType = evaluateExpressionType();  // recursivo
            if (!this.currentToken.getName().equals(")")) {
                throw new CompilerException("Esperado ')' após expressão");
            }
            this.nextToken();
        } else if (this.isConstOrId()) {
            leftType = this.currentToken.getType();
            this.nextToken();
        } else {
            throw new CompilerException("Expressão inválida com token: " + currentToken.getName());
        }

        while (!this.currentToken.getName().equals(";")
                && !this.currentToken.getName().equals(")")
                && !this.currentToken.getName().equals(",")
                && (this.identifyArithmeticOperator() || this.identifyLogicalOperator())) {

            String operator = this.currentToken.getName(); // Salva o operador antes
            boolean isArithmetic = isArithmeticOperator(operator);
            boolean isLogical = isLogicalOperator(operator);

            this.nextToken(); // Avança para o operando

            String rightType = evaluateExpressionType(); // Avalia recursivamente

            if (isArithmetic) {
                if (!leftType.equalsIgnoreCase("int") || !rightType.equalsIgnoreCase("int")) {
                    ErrorHandler.semanticErrorInvalidExpression("int", rightType, this.currentToken);
                }
                leftType = "int";
            } else if (isLogical) {
                if (!leftType.equalsIgnoreCase("int") || !rightType.equalsIgnoreCase("int")) {
                    ErrorHandler.semanticErrorInvalidExpression("boolean", rightType, this.currentToken);
                }
                leftType = "boolean";
                break;
            }
        }

        return leftType;
    }

    void checkBooleanExpressionAfterIfOrWhile() throws CompilerException {
        this.nextToken();
        String expressionType = parseExpressionUntil("begin");
        if (!expressionType.equalsIgnoreCase("boolean")) {
            ErrorHandler.semanticErrorInvalidExpression("boolean", expressionType, this.currentToken);
        }
    }

    String parseExpressionUntil(String stopToken) throws CompilerException {
        String expressionType = null;

        if (isConstOrId()) {
            if (this.currentToken.getClassification().equals("ID") && !isDeclared()) {
                ErrorHandler.semanticErrorNotDeclared(this.currentToken);
            }

            for (Token token : this.declaredTokens) {
                if (!token.getName().equals(this.currentToken.getName())) continue;
                expressionType = token.getType().toLowerCase();
            }

            this.nextToken();
        } else {
            throw new CompilerException("Expressão inválida após if/while: " + this.currentToken.getName());
        }

        while (!this.currentToken.getName().equalsIgnoreCase(stopToken)) {
            if (identifyLogicalOperator() || identifyArithmeticOperator()) {
                String operator = this.currentToken.getName(); // salva operador antes de avançar
                boolean isLogical = isLogicalOperator(operator);
                boolean isArithmetic = isArithmeticOperator(operator);

                this.nextToken();

                if (!isConstOrId()) {
                    throw new CompilerException("Expressão inválida: esperado operando após " + operator);
                }

                if (this.currentToken.getClassification().equals("ID") && !isDeclared()) {
                    ErrorHandler.semanticErrorNotDeclared(this.currentToken);
                }

                String rightType = this.currentToken.getType();

                if (isLogical) {
                    if (!(expressionType.equalsIgnoreCase("int") && rightType.equalsIgnoreCase("int")) &&
                            !(expressionType.equalsIgnoreCase("boolean") && rightType.equalsIgnoreCase("boolean"))) {
                        ErrorHandler.semanticErrorInvalidExpression(expressionType, rightType, this.currentToken);
                    }
                    expressionType = "boolean";
                } else if (isArithmetic) {
                    if (!expressionType.equalsIgnoreCase("int") || !rightType.equalsIgnoreCase("int")) {
                        ErrorHandler.semanticErrorInvalidExpression("int", rightType, this.currentToken);
                    }
                    expressionType = "int";
                }

                this.nextToken();
            } else {
                break;
            }
        }

        return expressionType;
    }

    boolean isPrimitiveType() {
        return (currentToken.getName().equalsIgnoreCase("int")
                || currentToken.getName().equalsIgnoreCase("string")
                || currentToken.getName().equalsIgnoreCase("boolean")
                || currentToken.getName().equalsIgnoreCase("byte"));
    }

    boolean isDeclared() {
        for (Token token : this.declaredTokens) {
            if (token.getName().equals(this.currentToken.getName())) return true;
        }
        return false;
    }

    boolean identifyLogicalOperator() {
        return isLogicalOperator(this.currentToken.getName());
    }

    boolean identifyArithmeticOperator() {
        return isArithmeticOperator(this.currentToken.getName());
    }

    boolean isLogicalOperator(String op) {
        return (op.equals("==") || op.equals("<") || op.equals("<=") || op.equals(">")
                || op.equals(">=") || op.equals("<>") || op.equals("and")
                || op.equals("or") || op.equals("not"));
    }

    boolean isArithmeticOperator(String op) {
        return (op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/"));
    }

    boolean isConstOrId() {
        return (this.currentToken.getClassification().equalsIgnoreCase("CONST")
                || this.currentToken.getClassification().equalsIgnoreCase("ID"));
    }

    void setSymbolsTableTypes() {
        for (Token declared : this.declaredTokens) {
            for (int i = 0; i < symbolsTable.getSize(); i++) {
                Token symbol = symbolsTable.currentToken(i);
                if (declared.getName().equals(symbol.getName())) {
                    symbol.setType(declared.getType());
                }
            }
        }
    }

    boolean isNotExpression() {
        this.nextToken();
        if (this.currentToken.getName().equals(";")) {
            this.previousToken();
            return true;
        }
        this.previousToken();
        return false;
    }
}
