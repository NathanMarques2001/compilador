package com.compiladores.compilador.semantic;

import com.compiladores.compilador.exceptions.CompilerException;
import com.compiladores.compilador.exceptions.ErrorHandler;
import com.compiladores.compilador.lexer.Token;
import com.compiladores.compilador.symboltable.SymbolsTable;
import com.compiladores.compilador.utils.TokenUtils;

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
        checkDeclarations();
        updateSymbolTypes();
        checkAssignments();
    }

    private void nextToken() {
        currentToken = symbolsTable.currentToken(++currentTokenIndex);
    }

    private void previousToken() {
        currentToken = symbolsTable.currentToken(--currentTokenIndex);
    }

    private boolean isDeclared() {
        for (Token t : declaredTokens) {
            if (t.getName().equalsIgnoreCase(currentToken.getName())) {
                return true;
            }
        }
        return false;
    }

    private void expectAssignment(Token target) throws CompilerException {
        if (!currentToken.getType().equalsIgnoreCase(target.getType())) {
            ErrorHandler.semanticErrorAssignment(currentToken, target);
        }
    }

    private void checkDeclarations() throws CompilerException {
        if (TokenUtils.isPrimitiveType(this.currentToken) || currentToken.getName().equalsIgnoreCase("final")) {
            currentType = currentToken.getName();
            nextToken();

            Token declared = currentToken;
            declared.setType(currentType);
            declaredTokens.add(declared);
            nextToken();

            if (currentToken.getName().equals("=")) {
                nextToken();

                if (currentType.equalsIgnoreCase("final")) {
                    currentType = currentToken.getType();
                    declared.setType(currentType);
                }

                expectAssignment(declared);
                nextToken();
            }

            nextToken();
            checkDeclarations();
        }
    }

    private void checkAssignments() throws CompilerException {
        nextToken();

        if (currentToken.getClassification().equalsIgnoreCase("id")) {
            if (!isDeclared()) {
                ErrorHandler.semanticErrorNotDeclared(currentToken);
            }
            checkAssignment();
        }

        if (currentToken.getName().equalsIgnoreCase("while") || currentToken.getName().equalsIgnoreCase("if")) {
            currentType = "boolean";
            checkBooleanExpression();
        }

        if (currentTokenIndex < symbolsTable.getSize() - 1) {
            checkAssignments();
        }
    }

    private void checkAssignment() throws CompilerException {
        for (Token declared : declaredTokens) {
            if (declared.getName().equalsIgnoreCase(currentToken.getName())) {
                currentType = declared.getType();
                nextToken();

                if (currentToken.getName().equals("=")) {
                    nextToken();

                    if (isSimpleValue()) {
                        expectAssignment(declared);
                        nextToken();
                    } else {
                        validateExpression();
                    }
                }
            }
        }
    }

    private boolean isSimpleValue() {
        nextToken();
        boolean isEnd = currentToken.getName().equals(";");
        previousToken();
        return isEnd;
    }

    private void validateExpression() throws CompilerException {
        String resultType = evaluateExpression();

        if (!resultType.equalsIgnoreCase(currentType)) {
            ErrorHandler.semanticErrorInvalidExpression(currentType, resultType, currentToken);
        }
    }

    private String evaluateExpression() throws CompilerException {
        String leftType;

        if (currentToken.getName().equals("(")) {
            nextToken();
            leftType = evaluateExpression();
            nextToken();
        } else if (TokenUtils.isConstOrId(this.currentToken)) {
            leftType = currentToken.getType();
            nextToken();
        } else {
            ErrorHandler.semanticErrorInvalidToken(currentToken);
            return "unknown";
        }

        while (!isExpressionEnd()) {
            String op = currentToken.getName();
            boolean isArith = TokenUtils.isArithmeticOperator(op);
            boolean isLogic = TokenUtils.isLogicalOperator(op);

            nextToken();
            String rightType = evaluateExpression();

            if (isArith) {
                if (!leftType.equals("int") || !rightType.equals("int")) {
                    ErrorHandler.semanticErrorInvalidExpression("int", rightType, currentToken);
                }
                leftType = "int";
            } else if (isLogic) {
                if (!leftType.equals("int") || !rightType.equals("int")) {
                    ErrorHandler.semanticErrorInvalidExpression("boolean", rightType, currentToken);
                }
                leftType = "boolean";
                break;
            }
        }

        return leftType;
    }

    private boolean isExpressionEnd() {
        String name = currentToken.getName();
        return name.equals(";") || name.equals(")") || name.equals(",") || (!TokenUtils.isArithmeticOp(this.currentToken) && !TokenUtils.isLogicalOp(this.currentToken));
    }

    private void checkBooleanExpression() throws CompilerException {
        nextToken();
        String resultType = parseExpressionUntil("begin");
        if (!resultType.equals("boolean")) {
            ErrorHandler.semanticErrorInvalidExpression("boolean", resultType, currentToken);
        }
    }

    private String parseExpressionUntil(String stopToken) throws CompilerException {
        String exprType = null;

        if (TokenUtils.isConstOrId(this.currentToken)) {
            if (currentToken.getClassification().equalsIgnoreCase("id") && !isDeclared()) {
                ErrorHandler.semanticErrorNotDeclared(currentToken);
            }

            exprType = currentToken.getType();
            nextToken();
        } else {
            ErrorHandler.semanticErrorInvalidExpressionAfterControl(currentToken);
        }

        while (!currentToken.getName().equalsIgnoreCase(stopToken)) {
            String op = currentToken.getName();
            boolean isLogic = TokenUtils.isLogicalOperator(op);
            boolean isArith = TokenUtils.isArithmeticOperator(op);

            nextToken();

            if (!TokenUtils.isConstOrId(this.currentToken)) {
                ErrorHandler.semanticErrorExpectedOperandAfter(op, currentToken);
            }

            String rightType = currentToken.getType();

            if (isLogic) {
                if (!(exprType.equals("int") && rightType.equals("int")) &&
                        !(exprType.equals("boolean") && rightType.equals("boolean"))) {
                    ErrorHandler.semanticErrorInvalidExpression(exprType, rightType, currentToken);
                }
                exprType = "boolean";
            } else if (isArith) {
                if (!exprType.equals("int") || !rightType.equals("int")) {
                    ErrorHandler.semanticErrorInvalidExpression("int", rightType, currentToken);
                }
                exprType = "int";
            }

            nextToken();
        }

        return exprType;
    }

    private void updateSymbolTypes() {
        for (Token declared : declaredTokens) {
            for (int i = 0; i < symbolsTable.getSize(); i++) {
                Token symbol = symbolsTable.currentToken(i);
                if (declared.getName().equalsIgnoreCase(symbol.getName())) {
                    symbol.setType(declared.getType());
                }
            }
        }
    }
}
