package com.compiladores.compilador.Semantic;

import com.compiladores.compilador.Exceptions.CompilerException;
import com.compiladores.compilador.Exceptions.ErrorHandler;
import com.compiladores.compilador.Lexical.Token;
import com.compiladores.compilador.Table.SymbolsTable;

import java.util.ArrayList;

public class SemanticAnalyzer {

    private final SymbolsTable symbolsTable;
    private final ArrayList<Token> declaredTokens = new ArrayList<>();

    private Token currentToken;
    private int currentTokenIndex = 0;
    private String currentType;

    public SemanticAnalyzer(SymbolsTable symbolsTable) {
        this.symbolsTable = symbolsTable;
        this.currentToken = symbolsTable.currentToken(currentTokenIndex);
    }

    public void analyze() throws CompilerException {
        try {
            checkDeclarations();
            setSymbolsTableTypes();
            checkAssignments();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private void nextToken() {
        currentToken = symbolsTable.currentToken(++currentTokenIndex);
    }

    private void previousToken() {
        currentToken = symbolsTable.currentToken(--currentTokenIndex);
    }

    private boolean isPrimitiveType() {
        return switch (currentToken.getName().toLowerCase()) {
            case "int", "string", "boolean", "byte" -> true;
            default -> false;
        };
    }

    private boolean isConstOrId() {
        return currentToken.getClassification().equalsIgnoreCase("CONST")
                || currentToken.getClassification().equalsIgnoreCase("ID");
    }

    private boolean isDeclared() {
        return declaredTokens.stream().anyMatch(t -> t.getName().equals(currentToken.getName()));
    }

    private boolean isLogicalOperator(String op) {
        return switch (op) {
            case "==", "<", "<=", ">", ">=", "<>", "and", "or", "not" -> true;
            default -> false;
        };
    }

    private boolean isArithmeticOperator(String op) {
        return switch (op) {
            case "+", "-", "*", "/" -> true;
            default -> false;
        };
    }

    private boolean isLogicalOp() {
        return isLogicalOperator(currentToken.getName());
    }

    private boolean isArithmeticOp() {
        return isArithmeticOperator(currentToken.getName());
    }

    private void expectAssignment(Token expected) throws CompilerException {
        if (!currentToken.getType().equalsIgnoreCase(expected.getType())) {
            ErrorHandler.semanticErrorAssignment(currentToken, expected);
        }
    }

    private void checkDeclarations() throws CompilerException {
        if (isPrimitiveType() || currentToken.getName().equals("final")) {
            currentType = currentToken.getName();
            nextToken();

            Token declaredToken = currentToken;
            declaredToken.setType(currentType);
            declaredTokens.add(declaredToken);
            nextToken();

            if (currentToken.getName().equals("=")) {
                nextToken();

                if (currentType.equals("final")) {
                    currentType = currentToken.getType();
                    declaredToken.setType(currentToken.getType());
                }

                expectAssignment(declaredToken);
                nextToken();
            }

            nextToken();
            checkDeclarations();
        }
    }

    private void checkAssignments() throws CompilerException {
        nextToken();

        if (currentToken.getClassification().equals("ID")) {
            if (!isDeclared()) {
                ErrorHandler.semanticErrorNotDeclared(currentToken);
            }
            checkAssignment();
        }

        if (currentToken.getName().equals("while") || currentToken.getName().equals("if")) {
            currentType = "boolean";
            checkBooleanExpressionAfterIfOrWhile();
        }

        if (currentTokenIndex < symbolsTable.getSize() - 1) {
            checkAssignments();
        }
    }

    private void checkAssignment() throws CompilerException {
        for (Token declared : declaredTokens) {
            if (!declared.getName().equals(currentToken.getName())) continue;

            currentType = declared.getType();
            nextToken();

            if (currentToken.getName().equals("=")) {
                nextToken();

                if (isSimpleAssignment()) {
                    expectAssignment(declared);
                    nextToken();
                } else {
                    verifyExpression();
                }

            } else if (isLogicalOp()) {
                nextToken();
                if (!currentToken.getType().equalsIgnoreCase(currentType)) {
                    ErrorHandler.semanticErrorBadComparation(declared, currentType);
                }
                nextToken();
            } else if (!currentType.equalsIgnoreCase("boolean") && currentToken.getName().equals("begin")) {
                ErrorHandler.semanticErrorBadComparation(declared, "boolean");
                nextToken();
            }
        }
    }

    private boolean isSimpleAssignment() {
        nextToken();
        boolean isEnd = currentToken.getName().equals(";");
        previousToken();
        return isEnd;
    }

    private void verifyExpression() throws CompilerException {
        String type = evaluateExpressionType();

        if (!type.equalsIgnoreCase(currentType)) {
            ErrorHandler.semanticErrorInvalidExpression(currentType, type, currentToken);
        }
    }

    private String evaluateExpressionType() throws CompilerException {
        String leftType = "";

        if (currentToken.getName().equals("(")) {
            nextToken();
            leftType = evaluateExpressionType();

            nextToken();
        } else if (isConstOrId()) {
            leftType = currentToken.getType();
            nextToken();
        } else {
            ErrorHandler.semanticErrorInvalidToken(this.currentToken);
        }

        while (!isEndOfExpression()) {
            String op = currentToken.getName();
            boolean arithmetic = isArithmeticOperator(op);
            boolean logical = isLogicalOperator(op);

            nextToken();
            String rightType = evaluateExpressionType();

            if (arithmetic) {
                if (!leftType.equalsIgnoreCase("int") || !rightType.equalsIgnoreCase("int")) {
                    ErrorHandler.semanticErrorInvalidExpression("int", rightType, currentToken);
                }
                leftType = "int";
            } else if (logical) {
                if (!leftType.equalsIgnoreCase("int") || !rightType.equalsIgnoreCase("int")) {
                    ErrorHandler.semanticErrorInvalidExpression("boolean", rightType, currentToken);
                }
                leftType = "boolean";
                break;
            }
        }

        return leftType;
    }

    private boolean isEndOfExpression() {
        return currentToken.getName().equals(";")
                || currentToken.getName().equals(")")
                || currentToken.getName().equals(",")
                || (!isArithmeticOp() && !isLogicalOp());
    }

    private void checkBooleanExpressionAfterIfOrWhile() throws CompilerException {
        nextToken();
        String exprType = parseExpressionUntil("begin");
        if (!exprType.equalsIgnoreCase("boolean")) {
            ErrorHandler.semanticErrorInvalidExpression("boolean", exprType, currentToken);
        }
    }

    private String parseExpressionUntil(String stopToken) throws CompilerException {
        String exprType = null;

        if (isConstOrId()) {
            if (currentToken.getClassification().equals("ID") && !isDeclared()) {
                ErrorHandler.semanticErrorNotDeclared(currentToken);
            }

            exprType = declaredTokens.stream()
                    .filter(t -> t.getName().equals(currentToken.getName()))
                    .map(t -> t.getType().toLowerCase())
                    .findFirst()
                    .orElse(currentToken.getType());

            nextToken();
        } else {
            ErrorHandler.semanticErrorInvalidExpressionAfterControl(this.currentToken);
        }

        while (!currentToken.getName().equalsIgnoreCase(stopToken)) {
            if (!isLogicalOp() && !isArithmeticOp()) break;

            String operator = currentToken.getName();
            boolean isLogical = isLogicalOperator(operator);
            boolean isArithmetic = isArithmeticOperator(operator);

            nextToken();

            if (!isConstOrId()) {
                ErrorHandler.semanticErrorExpectedOperandAfter(operator, currentToken);
            }

            if (currentToken.getClassification().equals("ID") && !isDeclared()) {
                ErrorHandler.semanticErrorNotDeclared(currentToken);
            }

            String rightType = currentToken.getType();

            if (isLogical) {
                if (!(exprType.equalsIgnoreCase("int") && rightType.equalsIgnoreCase("int")) &&
                        !(exprType.equalsIgnoreCase("boolean") && rightType.equalsIgnoreCase("boolean"))) {
                    ErrorHandler.semanticErrorInvalidExpression(exprType, rightType, currentToken);
                }
                exprType = "boolean";
            } else if (isArithmetic) {
                if (!exprType.equalsIgnoreCase("int") || !rightType.equalsIgnoreCase("int")) {
                    ErrorHandler.semanticErrorInvalidExpression("int", rightType, currentToken);
                }
                exprType = "int";
            }

            nextToken();
        }

        return exprType;
    }

    private void setSymbolsTableTypes() {
        for (Token declared : declaredTokens) {
            for (int i = 0; i < symbolsTable.getSize(); i++) {
                Token symbol = symbolsTable.currentToken(i);
                if (declared.getName().equals(symbol.getName())) {
                    symbol.setType(declared.getType());
                }
            }
        }
    }
}
