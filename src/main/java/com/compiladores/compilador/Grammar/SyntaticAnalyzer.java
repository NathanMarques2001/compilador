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

    public void analyze() {
        try {
            this.typeGeneration();
            this.beginGeneration();
            System.out.println("Analise sintatica concluida com sucesso!");
        } catch (Exception e) {
            System.err.println("Erro de analise sintatica: " + e.getMessage());
        }
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

    void typeGeneration() throws CompilerException {
        while (this.identifyType()) {
            this.nextToken();
            this.expectClassification("ID");

            this.nextToken();
            if (this.currentToken.getName().equals("=")) {
                this.nextToken();
                if (!this.identifyCONSTorID()) {
                    ErrorHandler.syntaxErrorAssignment(currentToken.getName());
                }
                this.nextToken();
            }
            expectName(";");
            this.nextToken();
        }
    }

    void beginGeneration() throws CompilerException {
        if (this.currentToken.getName().equals("begin")) {
            this.nextToken();
            while (!this.currentToken.getName().equals("end")) {
                this.statements();
            }
        }
    }

    void statements() throws CompilerException {
        this.ifGeneration();
        this.elseGeneration();
        this.readlnGeneration();
        this.writeGeneration();
        this.assignmentGeneration();
        this.whileGeneration();
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

    void readlnGeneration() throws CompilerException {
        if (this.currentToken.getName().equals("readln")) {
            this.nextToken();
            this.expectName(",");
            this.nextToken();
            this.expectClassification("ID");
            this.nextToken();
            this.expectName(";");
            this.nextToken();
        }
    }

    void writeGeneration() throws CompilerException {
        if (this.currentToken.getName().equals("write") || this.currentToken.getName().equals("writeln")) {
            this.nextToken();
            this.expectName(",");
            this.nextToken();
            if (!this.identifyCONSTorID()) {
                ErrorHandler.syntaxErrorAssignment(currentToken.getName());
            }
            this.nextToken();
            while (!this.currentToken.getName().equals(";")) {
                if (this.currentToken.getName().equals(",")) {
                    this.nextToken();
                }
                if (!this.identifyCONSTorID()) {
                    ErrorHandler.syntaxErrorAssignment(currentToken.getName());
                }
                this.nextToken();
            }
            this.expectName(";");
            this.nextToken();
        }
    }

    void assignmentGeneration() throws CompilerException {
        if (this.currentToken.getClassification().equals("ID")) {
            this.nextToken();
            this.expectName("=");
            this.nextToken();
            if (!this.identifyMathematicalOperationGeneration() && !this.identifyBooleanValue()
                    && !this.identifyCONSTorID()) {
                ErrorHandler.syntaxErrorAssignment(currentToken.getName());
            }
            this.nextToken();

            if (!this.currentToken.equals("=")) {
                if (this.identifyLogicalOperator() || this.identifyMathematicalOperator()) {
                    this.nextToken();
                    if (!this.identifyCONSTorID() && !this.identifyBooleanValue()) {
                        ErrorHandler.syntaxErrorAssignment(currentToken.getName());
                    }
                    this.nextToken();
                }
            }

            this.expectName(";");
            this.nextToken();
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

    boolean identifyType() {
        return (currentToken.getName().equals("int") || currentToken.getName().equals("string")
                || currentToken.getName().equals("boolean") || currentToken.getName().equals("final")
                || currentToken.getName().equals("byte"));
    }

    boolean identifyCONSTorID() {
        return (this.currentToken.getClassification().equals("CONST")
                || this.currentToken.getClassification().equals("ID")
                || this.identifyBooleanValue());
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
                    ErrorHandler.syntaxError("Expressão matemática inválida dentro dos parênteses.","");
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
