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

    private void expectAssignment(Token token) throws CompilerException {
        if (!token.getType().equalsIgnoreCase(this.currentType)) {
            ErrorHandler.semanticErrorAssignment(token, currentType);
        }
    }

    public void analyze() throws CompilerException {
        try {
            this.checkDeclarations();
            System.out.println("========================================");
            this.checkAssignments();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    void checkDeclarations() throws CompilerException {
        if (isPrimitiveType() || this.currentToken.getName().equals("final")) {
            this.currentType = this.currentToken.getName();
            this.nextToken();
            Token declaredToken = this.currentToken;
            declaredToken.setType(this.currentType);
            this.declaredTokens.add(declaredToken);
            this.nextToken();
            if (this.currentToken.getName().equals("=")) {
                this.nextToken();
                if(this.currentType.equals("final")) {
                    this.currentType = this.currentToken.getType();
                    declaredToken.setType(this.currentToken.getType());
                }
                this.expectAssignment(declaredToken);
                System.out.println(declaredToken.getName() + " " + declaredToken.getType() + " - " + this.currentType);
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
                this.expectAssignment(token);
                System.out.println(token.getName() + " " + token.getType() + " - " + this.currentType);
                this.nextToken();
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

    boolean isPrimitiveType() {
        return (currentToken.getName().equalsIgnoreCase("int") || currentToken.getName().equalsIgnoreCase("string")
                || currentToken.getName().equalsIgnoreCase("boolean") || currentToken.getName().equalsIgnoreCase("byte"));
    }

    boolean isDeclared() {
        for (Token token : this.declaredTokens) {
            if (token.getName().equals(this.currentToken.getName())) return true;
        }
        return false;
    }

    boolean identifyLogicalOperator() {
        return (this.currentToken.getName().equals("==") || this.currentToken.getName().equals("<")
                || this.currentToken.getName().equals("<=") || this.currentToken.getName().equals(">")
                || this.currentToken.getName().equals(">=") || this.currentToken.getName().equals("<>")
                || this.currentToken.getName().equals("and") || this.currentToken.getName().equals("or")
                || this.currentToken.getName().equals("not"));
    }
}
