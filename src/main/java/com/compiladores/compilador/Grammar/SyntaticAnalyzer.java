
package com.compiladores.compilador.Grammar;

import com.compiladores.compilador.Exceptions.CompilerException;
import com.compiladores.compilador.Exceptions.ErrorHandler;
import com.compiladores.compilador.Lexical.Token;
import com.compiladores.compilador.Table.SymbolsTable;


public class SyntaticAnalyzer {

    private SymbolsTable symbolsTable;
    private Token currentToken;
    private int currentTokenIndex = 0;

    public SyntaticAnalyzer(SymbolsTable symbolsTable) {
        this.symbolsTable = symbolsTable;
        this.currentToken = symbolsTable.currentToken(this.currentTokenIndex);
    }

    private void nextToken() {
        System.out.println(this.currentToken.getName());
        this.currentToken = this.symbolsTable.currentToken(++this.currentTokenIndex);
    }

    private void previousToken() {
        this.currentToken = this.symbolsTable.currentToken(--this.currentTokenIndex);
    }

    private void expectClassification(String expected) throws CompilerException {
        if (!currentToken.getClassification().equals(expected) && !currentToken.getName().equals(expected)) {
            ErrorHandler.syntaxError(expected, currentToken.getClassification());
        }
    }

    private void expectName(String expected) throws CompilerException {
        if (!currentToken.getName().equals(expected)) {
            ErrorHandler.syntaxError(expected, currentToken.getClassification());
        }
    }

    public void parseProgram() {
        try {
            this.parseDeclarations();
            this.parseBlock();
            System.out.println("Analise sintatica concluida com sucesso!");
        } catch (Exception e) {
            System.err.println("Erro de analise sintatica: " + e.getMessage());
        }
    }

    void parseDeclarations() throws CompilerException {
        if (this.isPrimitiveType()) {
            this.nextToken();
            this.expectClassification("ID");

            this.nextToken();
            if (this.currentToken.getName().equals("=")) {
                this.nextToken();
                if (!this.isConstOrId()) {
                    ErrorHandler.syntaxErrorAssignment(this.currentToken.getName());
                }
                this.nextToken();
            }
            expectName(";");
            this.nextToken();
            this.parseDeclarations();
        }
    }

    void parseBlock() throws CompilerException {
        this.expectName("begin");
        this.nextToken();
        this.parseCommands();
        this.expectName("end");
    }

    void parseCommands() throws CompilerException {
        if (this.currentToken.getName().equals("end")) {
            this.nextToken();
            this.parseCommand();
            this.parseCommands();
        }
    }

    void parseCommand() throws CompilerException {
        if (this.currentToken.getName().equals("write") || this.currentToken.getName().equals("writeln")) {
            this.parseWrite();
        }
        if (this.currentToken.getName().equals("readln")) {
            this.parseReadln();
        }
        if (this.currentToken.getClassification().equals("ID")) {
            this.parseAssignment();
        }
        this.elseGeneration();
        this.ifGeneration();
        this.whileGeneration();
    }

    void parseWrite() throws CompilerException {
        this.nextToken();
        this.expectName(",");
        this.nextToken();
        if (!this.isConstOrId()) {
            ErrorHandler.syntaxErrorAssignment(currentToken.getName());
        }
        this.nextToken();
        if (!this.currentToken.getName().equals(";")) {
            this.parseWrite();
            return;
        }
        this.expectName(";");
        this.nextToken();
    }

    void parseReadln() throws CompilerException {
        this.nextToken();
        this.expectName(",");
        this.nextToken();
        this.expectClassification("ID");
        this.nextToken();
        this.expectName(";");
        this.nextToken();
    }

    void parseAssignment() throws CompilerException {
            this.nextToken();
            this.expectName("=");
            this.nextToken();
            this.expectName(";");
            this.nextToken();
        }
    }

    void parseExpression(){
}

    void ifGeneration() throws CompilerException {
        if (this.currentToken.getName().equals("if")) {
            this.nextToken();
            if (this.identifyMathematicalOperationGeneration() || this.identifyCONSTorID()
                    || this.identifyBooleanValue()) {
                this.nextToken();
                if (this.identifyLogicalOperator()) {
                    this.nextToken();
                    if (this.identifyMathematicalOperationGeneration() || this.identifyCONSTorID()
                            || this.identifyBooleanValue()) {
                        this.nextToken();
                        while (!currentToken.getName().equals("end") && !currentToken.getName().equals("else")) {
                            this.nextToken();
                            this.statements();
                        }
                    }
                }
            }
        }
    }

    void elseGeneration() throws CompilerException {
        if (this.currentToken.getName().equals("else")) {
            this.nextToken();
            while (!this.currentToken.getName().equals("end")) {
                this.nextToken();
                this.statements();
            }
        }
    }

    void whileGeneration() throws CompilerException {
        if (this.currentToken.getName().equals("while")) {
            this.nextToken();
            this.expectClassification("ID");
            while (!this.currentToken.getName().equals("end")) {
                this.nextToken();
                this.beginGeneration();
            }
            this.nextToken();
        }
    }

    boolean isPrimitiveType() {
        return (currentToken.getName().equals("int") || currentToken.getName().equals("string")
                || currentToken.getName().equals("boolean") || currentToken.getName().equals("byte"));
    }

    boolean isConstOrId() {
        return (this.currentToken.getClassification().equals("CONST")
                || this.currentToken.getClassification().equals("ID"));
    }

    boolean identifyLogicalOperator() {
        return (this.currentToken.getName().equals("==") || this.currentToken.getName().equals("<")
                || this.currentToken.getName().equals("<=") || this.currentToken.getName().equals(">")
                || this.currentToken.getName().equals(">=") || this.currentToken.getName().equals("<>")
                || this.currentToken.getName().equals("and") || this.currentToken.getName().equals("or")
                || this.currentToken.getName().equals("not"));
    }

    boolean identifyMathematicalOperator() {
        return (this.currentToken.getName().equals("+") || this.currentToken.getName().equals("-")
                || this.currentToken.getName().equals("*") || this.currentToken.getName().equals("/"));
    }

    boolean identifyBooleanValue() {
        return this.currentToken.getType().equals("BOOLEAN");
    }

    boolean identifyMathematicalOperationGeneration() throws CompilerException {
        if (this.identifyCONSTorID() || this.identifyParentheses()) {
            if (this.identifyParentheses()) {
                this.nextToken(); // Avança para o conteúdo dentro dos parênteses
                if (!this.identifyMathematicalOperationGeneration()) {
                    ErrorHandler.syntaxError("Expressão matemática inválida dentro dos parênteses.", "");
                }
                this.expectName(")");
                this.nextToken();
            } else {
                this.nextToken();
            }

            if (this.identifyMathematicalOperator()) {
                this.nextToken();
                return this.identifyMathematicalOperationGeneration();
            }

            this.previousToken();
        }
        return false;
    }

    boolean identifyParentheses() {
        return this.currentToken.getName().equals("(") || this.currentToken.getName().equals(")");
    }
}
