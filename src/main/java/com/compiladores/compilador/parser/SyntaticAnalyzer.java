package com.compiladores.compilador.parser;

import com.compiladores.compilador.exceptions.CompilerException;
import com.compiladores.compilador.exceptions.ErrorHandler;
import com.compiladores.compilador.lexer.Token;
import com.compiladores.compilador.symboltable.SymbolsTable;
import com.compiladores.compilador.utils.TokenUtils;

/**
 * Realiza a análise sintática (parsing) da sequência de tokens.
 * Verifica se a estrutura do programa está em conformidade com a gramática da linguagem LC.
 * Utiliza a abordagem de "Recursive Descent Parsing".
 */
public class SyntaticAnalyzer {

    private final SymbolsTable symbolsTable;
    private Token currentToken;
    private int currentTokenIndex = 0;

    public SyntaticAnalyzer(SymbolsTable symbolsTable) {
        this.symbolsTable = symbolsTable;
        // Pega o primeiro token para iniciar a análise.
        this.currentToken = symbolsTable.currentToken(currentTokenIndex);
    }

    // Avança para o próximo token na tabela de símbolos.
    private void nextToken() {
        if (currentTokenIndex < symbolsTable.getSize() - 1) {
            currentToken = symbolsTable.currentToken(++currentTokenIndex);
        }
    }

    /**
     * Valida se a classificação do token atual é a esperada.
     * Lança um erro sintático se não for.
     */
    private void expectClassification(String expected) throws CompilerException {
        if (!currentToken.getClassification().equalsIgnoreCase(expected) &&
                !currentToken.getName().equalsIgnoreCase(expected)) {
            ErrorHandler.syntaxError(expected, currentToken);
        }
    }

    /**
     * Valida se o nome (lexema) do token atual é o esperado.
     * Lança um erro sintático se não for.
     */
    private void expectName(String expected) throws CompilerException {
        if (!currentToken.getName().equalsIgnoreCase(expected)) {
            ErrorHandler.syntaxError(expected, currentToken);
        }
    }

    /**
     * Ponto de entrada do parser. Inicia a análise da estrutura geral do programa.
     * Gramática: Programa -> Declarações Bloco
     */
    public void parseProgram() throws CompilerException {
        parseDeclarations();
        parseBlock();
    }

    /**
     * Analisa a seção de declarações de variáveis e constantes.
     * Gramática: Declarações -> (Declaração_Var | Declaração_Const) Declarações | ε
     */
    private void parseDeclarations() throws CompilerException {
        // Verifica se o token atual pode iniciar uma declaração.
        if (TokenUtils.isPrimitiveType(this.currentToken) || currentToken.getName().equalsIgnoreCase("final")) {
            // Consome o tipo (int, byte, final, etc.)
            nextToken();
            expectClassification("id"); // Espera um identificador.

            nextToken();
            // Verifica se há uma inicialização opcional.
            if (currentToken.getName().equalsIgnoreCase("=")) {
                nextToken();
                if (!TokenUtils.isConstOrId(this.currentToken)) { // O valor deve ser uma constante ou outro id.
                    ErrorHandler.syntaxErrorAssignment(currentToken);
                }
                nextToken();
            }

            expectName(";"); // Toda declaração termina com ';'.
            nextToken();

            // Chamada recursiva para analisar múltiplas declarações.
            parseDeclarations();
        }
    }

    /**
     * Analisa um bloco de comandos.
     * Gramática: Bloco -> 'begin' Comandos 'end'
     */
    private void parseBlock() throws CompilerException {
        expectName("begin");
        nextToken();
        parseCommands();
        expectName("end");
        // Avança o token após o 'end' se não for o final do arquivo.
        if (currentTokenIndex < symbolsTable.getSize() - 1) {
            nextToken();
        }
    }

    /**
     * Analisa uma sequência de comandos dentro de um bloco.
     * Gramática: Comandos -> Comando Comandos | ε
     */
    private void parseCommands() throws CompilerException {
        // A condição de parada é encontrar o 'end' do bloco.
        if (!currentToken.getName().equalsIgnoreCase("end")) {
            parseCommand();
            parseCommands(); // Recursão para analisar o próximo comando.
        }
    }

    /**
     * Analisa um único comando, delegando para o método específico.
     * Gramática: Comando -> Comando_Atrib | Comando_IO | Comando_Cond | Comando_Rep | Bloco
     */
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
                parseBlock(); // Um bloco pode conter outros blocos.
                break;
            default:
                // Se não for uma palavra-chave de comando, deve ser uma atribuição (que começa com um id).
                if (currentToken.getClassification().equalsIgnoreCase("id")) {
                    parseAssignment();
                } else {
                    ErrorHandler.syntaxError("um comando válido", currentToken);
                }
        }
    }

    // Analisa os comandos de escrita (write/writeln).
    private void parseWrite() throws CompilerException {
        nextToken(); // Consome 'write' ou 'writeln'
        parseStrConcat(); // Analisa a lista de expressões a serem impressas.
        expectName(";");
        nextToken();
    }

    // Analisa a lista de expressões para os comandos de escrita.
    private void parseStrConcat() throws CompilerException {
        expectName(","); // A lista de expressões é separada por vírgula.
        nextToken();

        if (!TokenUtils.isConstOrId(this.currentToken)) {
            ErrorHandler.syntaxErrorAssignment(currentToken);
        }
        nextToken();
        // Verifica se há mais expressões na lista.
        parseStrConcatTail();
    }

    // Analisa a "cauda" (continuação) de uma lista de expressões de escrita.
    private void parseStrConcatTail() throws CompilerException {
        if (currentToken.getName().equalsIgnoreCase(",")) {
            parseStrConcat(); // Se encontrar outra vírgula, analisa a próxima expressão.
        }
    }

    // Analisa o comando de leitura (readln).
    private void parseReadln() throws CompilerException {
        nextToken(); // Consome 'readln'
        expectName(",");
        nextToken();
        expectClassification("ID"); // Espera um identificador de variável.
        nextToken();
        expectName(";");
        nextToken();
    }

    /**
     * Analisa um comando de atribuição.
     * Gramática: Atribuição -> id '=' Expressão ';'
     */
    private void parseAssignment() throws CompilerException {
        nextToken(); // Consome o 'id'
        expectName("=");
        nextToken();
        parseExpression(false); // Analisa a expressão à direita. 'false' impede expressões lógicas aqui.
        expectName(";");
        nextToken();
    }

    // Ponto de entrada para análise de qualquer tipo de expressão.
    private void parseExpression(boolean allowLogical) throws CompilerException {
        // Delega para o método de expressão lógica, que tem maior precedência.
        parseLogicalExpression(allowLogical);
    }

    /**
     * Analisa expressões lógicas (com 'not', 'and', 'or').
     * A estrutura segue a ordem de precedência.
     */
    private void parseLogicalExpression(boolean allowLogical) throws CompilerException {
        // 'not' tem alta precedência.
        if (currentToken.getName().equalsIgnoreCase("not")) {
            nextToken();
            parseLogicalExpression(allowLogical);
            return;
        }

        parseArithmeticExpression(allowLogical);

        if (TokenUtils.isLogicalOp(this.currentToken)) {
            if (!allowLogical) {
                // Não se pode ter 'and' ou 'or' em uma atribuição normal.
                ErrorHandler.syntaxErrorAssignmentLogicalExpression(this.currentToken);
            } else {
                nextToken();
                parseArithmeticExpression(allowLogical);
            }
        }
    }

    // Os métodos a seguir (parseArithmeticExpression, parseTerm, parseFactor) implementam
    // a análise de expressões aritméticas com a precedência correta:
    // 1. Fator (números, variáveis, expressões entre parênteses)
    // 2. Termo (multiplicação e divisão)
    // 3. Expressão Aritmética (adição e subtração)

    private void parseArithmeticExpression(boolean allowLogical) throws CompilerException {
        parseTerm(allowLogical);
        parseArithmeticExpressionTail(allowLogical);
    }

    private void parseArithmeticExpressionTail(boolean allowLogical) throws CompilerException {
        if (currentToken.getName().equalsIgnoreCase("+") ||
                currentToken.getName().equalsIgnoreCase("-")) {
            nextToken();
            parseTerm(allowLogical);
            parseArithmeticExpressionTail(allowLogical); // Recursão para lidar com múltiplos operadores
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
            parseTermTail(allowLogical); // Recursão
        }
    }

    // Analisa o nível mais fundamental de uma expressão: um valor, uma variável ou outra expressão entre parênteses.
    private void parseFactor(boolean allowLogical) throws CompilerException {
        if (TokenUtils.isConstOrId(this.currentToken) || currentToken.getType().equalsIgnoreCase("boolean")) {
            nextToken();
        } else if (currentToken.getName().equalsIgnoreCase("(")) { // Trata expressões entre parênteses.
            nextToken();
            parseExpression(allowLogical); // Analisa a expressão interna.
            expectName(")");
            nextToken();
        } else {
            ErrorHandler.syntaxError("CONST, ID ou EXPRESSÃO entre parênteses", currentToken);
        }
    }

    // Analisa um comando 'if'.
    private void parseIf() throws CompilerException {
        nextToken(); // consome 'if'
        parseExpression(true); // A condição do 'if' deve ser uma expressão lógica.
        parseBlock(); // O corpo do 'if' é um bloco.
    }

    // Analisa um comando 'while'.
    private void parseWhile() throws CompilerException {
        nextToken(); // consome 'while'
        parseExpression(true); // A condição do 'while' deve ser uma expressão lógica.
        parseBlock(); // O corpo do 'while' é um bloco.
    }

    // Analisa a cláusula 'else'.
    private void parseElse() throws CompilerException {
        nextToken(); // consome 'else'
        parseBlock();
    }
}