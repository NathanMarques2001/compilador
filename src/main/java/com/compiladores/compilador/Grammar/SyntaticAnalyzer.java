
package com.compiladores.compilador.Grammar;

import com.compiladores.compilador.Exceptions.CompilerException;
import com.compiladores.compilador.Exceptions.ErrorHandler;
import com.compiladores.compilador.Lexical.Token;
import com.compiladores.compilador.Table.SymbolsTable;


public class SyntaticAnalyzer {

    private final SymbolsTable symbolsTable;
    private Token currentToken;
    private int currentTokenIndex = 0;

    public SyntaticAnalyzer(SymbolsTable symbolsTable) {
        this.symbolsTable = symbolsTable;
        this.currentToken = symbolsTable.currentToken(this.currentTokenIndex);
    }

    private void nextToken() {
        this.currentToken = this.symbolsTable.currentToken(++this.currentTokenIndex);
    }

    private void expectClassification(String expected) throws CompilerException {
        if (!currentToken.getClassification().equals(expected) && !currentToken.getName().equals(expected)) {
            ErrorHandler.syntaxError(expected, currentToken);
        }
    }

    private void expectName(String expected) throws CompilerException {
        if (!currentToken.getName().equals(expected)) {
            ErrorHandler.syntaxError(expected, currentToken);
        }
    }

    public void parseProgram() {
        try {
            this.parseDeclarations();
            this.parseBlock();
        } catch (Exception e) {
            System.err.println("Erro de analise sintatica: " + e.getMessage());
        }
    }

    void parseDeclarations() throws CompilerException {
        if (this.isPrimitiveType() || this.currentToken.getName().equals("final")) {
            this.nextToken();
            this.expectClassification("ID");

            this.nextToken();
            if (this.currentToken.getName().equals("=")) {
                this.nextToken();
                if (!this.isConstOrId()) {
                    ErrorHandler.syntaxErrorAssignment(this.currentToken);
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
        if (this.currentTokenIndex < this.symbolsTable.getSize() - 1) {
            this.nextToken();
        }
    }

    void parseCommands() throws CompilerException {
        if (!this.currentToken.getName().equals("end")) {
            this.parseCommand();
            this.parseCommands();
        }
    }

    void parseCommand() throws CompilerException {
        if (this.currentToken.getName().equals("write") || this.currentToken.getName().equals("writeln")) {
            this.parseWrite();
        } else if (this.currentToken.getName().equals("readln")) {
            this.parseReadln();
        } else if (this.currentToken.getClassification().equals("ID")) {
            this.parseAssignment();
        } else if (this.currentToken.getName().equals("while")) {
            this.parseWhile();
        } else if (this.currentToken.getName().equals("if")) {
            this.parseIf();
        } else if (this.currentToken.getName().equals("else")) {
            this.parseElse();
        } else if (this.currentToken.getName().equals("begin")) {
            this.parseBlock();
        } else {
            ErrorHandler.syntaxError("Comando válido esperado", currentToken);
        }
    }

    void parseWrite() throws CompilerException {
        nextToken();
        parseStrConcat();
        expectName(";");
        nextToken();
    }

    void parseStrConcat() throws CompilerException {
        expectName(",");
        nextToken();

        if (!isConstOrId()) {
            ErrorHandler.syntaxErrorAssignment(currentToken);
        }
        nextToken();
        parseStrConcatTail();
    }

    void parseStrConcatTail() throws CompilerException {
        if (currentToken.getName().equals(",")) {
            parseStrConcat();
        }
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
        this.parseExpression();
        this.expectName(";");
        this.nextToken();
    }

    // EXPRESSAO -> EXPRESSAO_LOGICA
    void parseExpression() throws CompilerException {
        parseLogicalExpression();
    }

    // EXPRESSAO_LOGICA -> "not" EXPRESSAO_LOGICA
    //                   | EXPRESSAO_ARITM COMPARADOR EXPRESSAO_ARITM
    //                   | EXPRESSAO_LOGICA LOGICO EXPRESSAO_LOGICA
    //                   | EXPRESSAO_ARITM
    void parseLogicalExpression() throws CompilerException {
        if (currentToken.getName().equals("not")) {
            nextToken();
            parseLogicalExpression();
            return;
        }

        parseArithmeticExpression();

        if (identifyLogicalOperator()) {
            nextToken();
            parseLogicalExpression();  // lado direito pode ser nova lógica
        }
    }

    // EXPRESSAO_ARITM -> TERMO EXPRESSAO_ARITM_TAIL
    void parseArithmeticExpression() throws CompilerException {
        parseTerm();
        parseArithmeticExpressionTail();
    }

    // EXPRESSAO_ARITM_TAIL -> ("+"|"-") TERMO EXPRESSAO_ARITM_TAIL | ε
    void parseArithmeticExpressionTail() throws CompilerException {
        if (currentToken.getName().equals("+") || currentToken.getName().equals("-")) {
            nextToken();
            parseTerm();
            parseArithmeticExpressionTail();
        }
    }

    // TERMO -> FATOR TERMO_TAIL
    void parseTerm() throws CompilerException {
        parseFactor();
        parseTermTail();
    }

    // TERMO_TAIL -> ("*"|"/") FATOR TERMO_TAIL | ε
    void parseTermTail() throws CompilerException {
        if (currentToken.getName().equals("*") || currentToken.getName().equals("/")) {
            nextToken();
            parseFactor();
            parseTermTail();
        }
    }

    // FATOR -> CONST | ID | "(" EXPRESSAO ")"
    void parseFactor() throws CompilerException {
        if (isConstOrId() || this.currentToken.getType().equals("BOOLEAN")) {
            nextToken();
        } else if (currentToken.getName().equals("(")) {
            nextToken();
            parseExpression();
            expectName(")");
            nextToken();
        } else {
            ErrorHandler.syntaxError("CONST, ID ou EXPRESSÃO entre parênteses", currentToken);
        }
    }

    void parseWhile() throws CompilerException {
        this.nextToken();
        this.parseExpression();
        this.parseBlock();
    }

    void parseIf() throws CompilerException {
        nextToken();
        parseExpression();
        parseBlock();
    }

    void parseElse() throws CompilerException {
        this.nextToken();
        this.parseBlock();
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
}
