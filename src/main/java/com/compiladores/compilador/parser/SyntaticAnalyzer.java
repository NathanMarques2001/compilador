package com.compiladores.compilador.parser;

import com.compiladores.compilador.exceptions.CompilerException;
import com.compiladores.compilador.exceptions.ErrorHandler;
import com.compiladores.compilador.lexer.Token;
import com.compiladores.compilador.symboltable.SymbolsTable;
import com.compiladores.compilador.utils.TokenUtils;

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

    public void parseProgram() throws CompilerException {
        parseDeclarations();
        parseBlock();
    }

    private void parseDeclarations() throws CompilerException {
        if (TokenUtils.isPrimitiveType(this.currentToken) || currentToken.getName().equalsIgnoreCase("final")) {
            nextToken();
            expectClassification("id");

            nextToken();
            if (currentToken.getName().equalsIgnoreCase("=")) {
                nextToken();
                if (!TokenUtils.isConstOrId(this.currentToken)) {
                    ErrorHandler.syntaxErrorAssignment(currentToken);
                }
                nextToken();
            }

            expectName(";");
            nextToken();

            parseDeclarations();
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
                if (currentToken.getClassification().equalsIgnoreCase("id")) {
                    parseAssignment();
                } else {
                    ErrorHandler.syntaxError("um comando válido", currentToken);
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

        if (!TokenUtils.isConstOrId(this.currentToken)) {
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
        parseExpression(false);
        expectName(";");
        nextToken();
    }

    private void parseExpression(boolean allowLogical) throws CompilerException {
        parseLogicalExpression(allowLogical);
    }

    private void parseLogicalExpression(boolean allowLogical) throws CompilerException {
        if (currentToken.getName().equalsIgnoreCase("not")) {
            nextToken();
            parseLogicalExpression(allowLogical);
            return;
        }

        parseArithmeticExpression(allowLogical);

        if (TokenUtils.isLogicalOp(this.currentToken)) {
            if (!allowLogical) {
                ErrorHandler.syntaxErrorAssignmentLogicalExpression(this.currentToken);
            } else {
                nextToken();
                parseArithmeticExpression(allowLogical);
            }
        }
    }

    private void parseArithmeticExpression(boolean allowLogical) throws CompilerException {
        parseTerm(allowLogical);
        parseArithmeticExpressionTail(allowLogical);
    }

    private void parseArithmeticExpressionTail(boolean allowLogical) throws CompilerException {
        if (currentToken.getName().equalsIgnoreCase("+") ||
                currentToken.getName().equalsIgnoreCase("-")) {
            nextToken();
            parseTerm(allowLogical);
            parseArithmeticExpressionTail(allowLogical);
        }
    }

    private void parseTerm(boolean allowLogical) throws CompilerException {
        parseFactor(allowLogical);
        parseTermTail(allowLogical);
    }

    private void parseTermTail(boolean allowLogical) throws CompilerException {
        if (currentToken.getName().equalsIgnoreCase("*") ||
                currentToken.getName().equalsIgnoreCase("/")) {
            nextToken();
            parseFactor(allowLogical);
            parseTermTail(allowLogical);
        }
    }

    private void parseFactor(boolean allowLogical) throws CompilerException {
        if (TokenUtils.isConstOrId(this.currentToken) || currentToken.getType().equalsIgnoreCase("boolean")) {
            nextToken();
        } else if (currentToken.getName().equalsIgnoreCase("(")) {
            nextToken();
            parseExpression(allowLogical);
            expectName(")");
            nextToken();
        } else {
            ErrorHandler.syntaxError("CONST, ID ou EXPRESSÃO entre parênteses", currentToken);
        }
    }

    private void parseIf() throws CompilerException {
        nextToken();
        parseExpression(true);
        parseBlock();
    }

    private void parseWhile() throws CompilerException {
        nextToken();
        parseExpression(true);
        parseBlock();
    }

    private void parseElse() throws CompilerException {
        nextToken();
        parseBlock();
    }
}
