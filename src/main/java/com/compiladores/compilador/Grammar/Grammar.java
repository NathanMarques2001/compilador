package com.compiladores.compilador.Grammar;

import com.compiladores.compilador.Exceptions.CompilerException;
import com.compiladores.compilador.Table.SymbolsTable;
import com.compiladores.compilador.Table.Symbol;
import com.compiladores.compilador.Exceptions.ErrorHandler;

public class Grammar {

    private SymbolsTable symbolsTable;
    private Symbol currentToken;
    private int currentTokenIndex = 0;

    public Grammar(SymbolsTable symbolsTable) {
        this.symbolsTable = symbolsTable;
        this.currentToken = symbolsTable.currentToken(this.currentTokenIndex);
    }

    private void nextToken() {
        System.out.println(this.currentToken.getName());
        this.currentToken = this.symbolsTable.currentToken(++this.currentTokenIndex);
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
        if (this.identifyType()) {
            this.expectClassification("ID");

            if (this.currentToken.getName().equals("=")) {
                if (!this.identifyCONSTorID()) {
                    ErrorHandler.syntaxErrorAssignment(currentToken.getName());
                }
            }
            expectName(";");
        }
    }

    void beginGeneration() throws CompilerException {
        if (this.currentToken.getName().equals("begin")) {
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
            if ((this.identifyMathematicalOperation() || this.identifyCONSTorID()) &&
                    this.identifyLogicalOperator() &&
                    (this.identifyMathematicalOperation() || this.identifyCONSTorID()))
                while (!currentToken.getName().equals("end") && !currentToken.getName().equals("else")) {
                    this.statements();
                }
        }
    }

    void elseGeneration() throws CompilerException {
        if (this.currentToken.getName().equals("else")) {
            while (!this.currentToken.getName().equals("end")) {
                this.statements();
            }
        }
    }

    void readlnGeneration() throws CompilerException {
        if (this.currentToken.getName().equals("readln")) {
            this.expectName(",");
            this.expectClassification("ID");
            this.expectName(";");
        }
    }

    void writeGeneration() throws CompilerException {
        if (this.currentToken.getName().equals("write") || this.currentToken.getName().equals("writeln")) {
            while (!this.currentToken.getName().equals(";")) {
                this.expectName(",");
                if (!this.identifyCONSTorID()) {
                    ErrorHandler.syntaxErrorAssignment(currentToken.getName());
                }
            }
            this.expectName(";");
        }
    }

    void assignmentGeneration() throws CompilerException {
        this.expectName("=");
        if (!this.identifyMathematicalOperation() || !this.identifyBooleanValue()) {
            ErrorHandler.syntaxErrorAssignment(currentToken.getName());
        }
        this.expectName(";");
    }

    void whileGeneration() throws CompilerException {
        if (this.currentToken.getName().equals("while")) {
            while (!this.currentToken.getName().equals("end")) {
                statements();
            }
        }
    }

    boolean identifyType() {
        return (currentToken.getName().equals("int") || currentToken.getName().equals("string")
                || currentToken.getName().equals("boolean") || currentToken.getName().equals("final"));
    }

    boolean identifyCONSTorID() {
        return (this.currentToken.getClassification().equals("CONST") || this.currentToken.getClassification().equals("ID"));
    }

    boolean identifyLogicalOperator() {
        return (this.currentToken.getName().equals("==") || this.currentToken.getName().equals("<") ||
                this.currentToken.getName().equals("<=") || this.currentToken.getName().equals(">") ||
                this.currentToken.getName().equals(">=") || this.currentToken.getName().equals("<>") ||
                this.currentToken.getName().equals("and") || this.currentToken.getName().equals("or") ||
                this.currentToken.getName().equals("not"));
    }

    boolean identifyMathematicalOperator() {
        return (this.currentToken.getName().equals("+") || this.currentToken.getName().equals("-") ||
                this.currentToken.getName().equals("*") || this.currentToken.getName().equals("/"));
    }

    boolean identifyBooleanValue() {
        return (this.currentToken.getName().equals("true") || this.currentToken.getName().equals("false"));
    }

    boolean identifyMathematicalOperation() {
        if (this.identifyCONSTorID()) {
            if (this.identifyMathematicalOperator()) {
                return this.identifyCONSTorID();
            }
        }
        return false;
    }
}
