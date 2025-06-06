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
        this.currentToken = symbolsTable.currentToken(currentTokenIndex);
    }

    private void nextToken() {
        currentToken = symbolsTable.currentToken(++currentTokenIndex);
    }

    private void expectClassification(String expected) throws CompilerException {
        if (!currentToken.getClassification().equalsIgnoreCase(expected) &&
                !currentToken.getName().equalsIgnoreCase(expected)) {
            ErrorHandler.syntaxError(expected, currentToken);
        }
    }

    private void expectName(String expected) throws CompilerException {
        if (!currentToken.getName().equalsIgnoreCase(expected)) {
            ErrorHandler.syntaxError(expected, currentToken);
        }
    }

    public void parseProgram() {
        try {
            parseDeclarations();
            parseBlock();
        } catch (Exception e) {
            System.err.println("Erro de análise sintática: " + e.getMessage());
        }
    }

    private void parseDeclarations() throws CompilerException {
        if (isPrimitiveType() || currentToken.getName().equalsIgnoreCase("final")) {
            nextToken();
            expectClassification("ID");

            nextToken();
            if (currentToken.getName().equalsIgnoreCase("=")) {
                nextToken();
                if (!isConstOrId()) {
                    ErrorHandler.syntaxErrorAssignment(currentToken);
                }
                nextToken();
            }

            expectName(";");
            nextToken();

            parseDeclarations(); // recursão para múltiplas declarações
        }
    }

    private void parseBlock() throws CompilerException {
        expectName("begin");
        nextToken();
        parseCommands();
        expectName("end");
        if (currentTokenIndex < symbolsTable.getSize() - 1) {
            nextToken();
        }
    }

    private void parseCommands() throws CompilerException {
        if (!currentToken.getName().equalsIgnoreCase("end")) {
            parseCommand();
            parseCommands();
        }
    }

    private void parseCommand() throws CompilerException {
        String name = currentToken.getName().toLowerCase();

        switch (name) {
            case "write":
            case "writeln":
                parseWrite();
                break;
            case "readln":
                parseReadln();
                break;
            case "while":
                parseWhile();
                break;
            case "if":
                parseIf();
                break;
            case "else":
                parseElse();
                break;
            case "begin":
                parseBlock();
                break;
            default:
                if (currentToken.getClassification().equalsIgnoreCase("ID")) {
                    parseAssignment();
                } else {
                    ErrorHandler.syntaxError("Comando válido esperado", currentToken);
                }
        }
    }

    private void parseWrite() throws CompilerException {
        nextToken();
        parseStrConcat();
        expectName(";");
        nextToken();
    }

    private void parseStrConcat() throws CompilerException {
        expectName(",");
        nextToken();

        if (!isConstOrId()) {
            ErrorHandler.syntaxErrorAssignment(currentToken);
        }
        nextToken();
        parseStrConcatTail();
    }

    private void parseStrConcatTail() throws CompilerException {
        if (currentToken.getName().equalsIgnoreCase(",")) {
            parseStrConcat();
        }
    }

    private void parseReadln() throws CompilerException {
        nextToken();
        expectName(",");
        nextToken();
        expectClassification("ID");
        nextToken();
        expectName(";");
        nextToken();
    }

    private void parseAssignment() throws CompilerException {
        nextToken();
        expectName("=");
        nextToken();
        parseExpression();
        expectName(";");
        nextToken();
    }

    private void parseExpression() throws CompilerException {
        parseLogicalExpression();
    }

    private void parseLogicalExpression() throws CompilerException {
        if (currentToken.getName().equalsIgnoreCase("not")) {
            nextToken();
            parseLogicalExpression();
            return;
        }

        parseArithmeticExpression();

        if (identifyLogicalOperator()) {
            nextToken();
            parseLogicalExpression(); // lado direito recursivo
        }
    }

    private void parseArithmeticExpression() throws CompilerException {
        parseTerm();
        parseArithmeticExpressionTail();
    }

    private void parseArithmeticExpressionTail() throws CompilerException {
        if (currentToken.getName().equalsIgnoreCase("+") ||
                currentToken.getName().equalsIgnoreCase("-")) {
            nextToken();
            parseTerm();
            parseArithmeticExpressionTail();
        }
    }

    private void parseTerm() throws CompilerException {
        parseFactor();
        parseTermTail();
    }

    private void parseTermTail() throws CompilerException {
        if (currentToken.getName().equalsIgnoreCase("*") ||
                currentToken.getName().equalsIgnoreCase("/")) {
            nextToken();
            parseFactor();
            parseTermTail();
        }
    }

    private void parseFactor() throws CompilerException {
        if (isConstOrId() || currentToken.getType().equalsIgnoreCase("BOOLEAN")) {
            nextToken();
        } else if (currentToken.getName().equalsIgnoreCase("(")) {
            nextToken();
            parseExpression();
            expectName(")");
            nextToken();
        } else {
            ErrorHandler.syntaxError("CONST, ID ou EXPRESSÃO entre parênteses", currentToken);
        }
    }

    private void parseWhile() throws CompilerException {
        nextToken();
        parseExpression();
        parseBlock();
    }

    private void parseIf() throws CompilerException {
        nextToken();
        parseExpression();
        parseBlock();
    }

    private void parseElse() throws CompilerException {
        nextToken();
        parseBlock();
    }

    private boolean isPrimitiveType() {
        String name = currentToken.getName().toLowerCase();
        return name.equals("int") || name.equals("string")
                || name.equals("boolean") || name.equals("byte");
    }

    private boolean isConstOrId() {
        return currentToken.getClassification().equalsIgnoreCase("CONST") ||
                currentToken.getClassification().equalsIgnoreCase("ID");
    }

    private boolean identifyLogicalOperator() {
        String op = currentToken.getName().toLowerCase();
        return op.equals("==") || op.equals("<") || op.equals("<=") ||
                op.equals(">") || op.equals(">=") || op.equals("<>") ||
                op.equals("and") || op.equals("or") || op.equals("not");
    }
}
