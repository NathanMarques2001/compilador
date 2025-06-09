package com.compiladores.compilador.semantic;

import com.compiladores.compilador.exceptions.CompilerException;
import com.compiladores.compilador.exceptions.ErrorHandler;
import com.compiladores.compilador.lexer.Token;
import com.compiladores.compilador.symboltable.SymbolsTable;
import com.compiladores.compilador.utils.TokenUtils;

import java.util.ArrayList;

/**
 * Realiza a análise semântica do código.
 * Esta fase verifica a coerência e o significado do programa, como a checagem de tipos,
 * a declaração de variáveis e a validade das expressões.
 */
public class SemanticAnalyzer {

    private final SymbolsTable symbolsTable;
    // Lista para manter o controle de todos os identificadores (variáveis/constantes) declarados.
    private final ArrayList<Token> declaredTokens = new ArrayList<>();
    // Lista para guardar os nomes das constantes.
    private final ArrayList<String> constantNames = new ArrayList<>();
    private Token currentToken;
    private int currentTokenIndex = 0;
    // Mantém o tipo esperado durante a análise de uma declaração ou expressão.
    private String currentType;

    public SemanticAnalyzer(SymbolsTable symbolsTable) {
        this.symbolsTable = symbolsTable;
        this.currentToken = symbolsTable.currentToken(currentTokenIndex);
    }

    // Ponto de entrada principal para a análise semântica.
    public void analyze() throws CompilerException {
        // 1. Processa todas as declarações primeiro para popular a lista de 'declaredTokens'.
        checkDeclarations();
        // 2. Com base nas declarações, atualiza o tipo de todos os tokens na tabela de símbolos.
        updateSymbolTypes();
        // 3. Percorre o código novamente para verificar a semântica das atribuições e expressões.
        checkAssignments();
    }

    private void nextToken() {
        if (currentTokenIndex < symbolsTable.getSize() - 1) {
            currentToken = symbolsTable.currentToken(++currentTokenIndex);
        }
    }

    private void previousToken() {
        currentToken = symbolsTable.currentToken(--currentTokenIndex);
    }

    // Verifica se o token atual (um identificador) já foi declarado.
    private boolean isDeclared() {
        for (Token t : declaredTokens) {
            if (t.getName().equalsIgnoreCase(currentToken.getName())) {
                return true;
            }
        }
        return false;
    }

    // Valida se o tipo do token atual é compatível com o tipo do alvo da atribuição.
    private void expectAssignment(Token target) throws CompilerException {
        if (!currentToken.getType().equalsIgnoreCase(target.getType())) {
            ErrorHandler.semanticErrorAssignment(currentToken, target);
        }
    }

    /*
     * Percorre a seção de declarações, registrando cada variável e constante declarada.
     * Também realiza a primeira verificação de tipo para declarações com inicialização.
     */
    private void checkDeclarations() throws CompilerException {
        if (TokenUtils.isPrimitiveType(this.currentToken) || currentToken.getName().equalsIgnoreCase("final")) {
            // Verifica se a declaração atual é de uma constante.
            boolean isConstant = currentToken.getName().equalsIgnoreCase("final");

            if (isConstant) {
                currentType = "final";
            } else {
                currentType = currentToken.getName();
            }
            nextToken(); // Avança para o ID

            Token declared = currentToken;

            // ADICIONADO: Se for uma constante, registre seu nome.
            if (isConstant) {
                constantNames.add(declared.getName());
            }

            declared.setType(currentType);
            declaredTokens.add(declared);
            nextToken(); // Avança para '=' ou ';'

            // Se for uma declaração com inicialização
            if (currentToken.getName().equals("=")) {
                nextToken(); // Avança para o valor

                if (currentType.equalsIgnoreCase("final")) {
                    currentType = currentToken.getType();
                    declared.setType(currentType); // Define o tipo de dado real (int, string, etc.)
                }

                expectAssignment(declared);
                nextToken();
            }

            nextToken();
            checkDeclarations();
        }
    }

    /**
     * Percorre o corpo do programa (após as declarações) para validar o uso de variáveis.
     * Verifica atribuições e expressões em estruturas de controle.
     */
    private void checkAssignments() throws CompilerException {
        nextToken();

        // Se encontrar um identificador, pode ser o início de uma atribuição.
        if (currentToken.getClassification().equalsIgnoreCase("id")) {
            if (!isDeclared()) {
                ErrorHandler.semanticErrorNotDeclared(currentToken);
            }
            checkAssignment();
        }

        // Se for uma estrutura de controle, a expressão seguinte deve ser booleana.
        if (currentToken.getName().equalsIgnoreCase("while") || currentToken.getName().equalsIgnoreCase("if")) {
            currentType = "boolean"; // O tipo esperado para a expressão é 'boolean'.
            checkBooleanExpression();
        }

        // Continua a verificação até o final da tabela de símbolos.
        if (currentTokenIndex < symbolsTable.getSize() - 1) {
            checkAssignments();
        }
    }

    // Valida uma única instrução de atribuição.
    private void checkAssignment() throws CompilerException {
        // Loop para verificar se o token atual é uma constante.
        for (String constName : constantNames) {
            if (constName.equalsIgnoreCase(currentToken.getName())) {
                ErrorHandler.semanticErrorAssignmentToConstant(currentToken);
            }
        }

        for (Token declared : declaredTokens) {
            // Encontra a declaração correspondente ao ID atual.
            if (declared.getName().equalsIgnoreCase(currentToken.getName())) {
                currentType = declared.getType(); // Define o tipo esperado para a expressão.
                nextToken(); // Avança para o '='

                if (currentToken.getName().equals("=")) {
                    nextToken(); // Avança para o início da expressão.

                    // Se for uma atribuição simples (ex: x = 10;), a validação é mais direta.
                    if (isSimpleValue()) {
                        expectAssignment(declared);
                        nextToken();
                    } else {
                        // Se for uma expressão complexa (ex: x = 5 * y;), valida a expressão inteira.
                        validateExpression();
                    }
                }
            }
        }
    }

    // Verifica se uma atribuição é de um valor simples (sem operadores).
    private boolean isSimpleValue() {
        nextToken();
        boolean isEnd = currentToken.getName().equals(";");
        previousToken();
        return isEnd;
    }

    // Valida uma expressão complexa, garantindo que o tipo resultante seja o esperado.
    private void validateExpression() throws CompilerException {
        String resultType = evaluateExpression(); // Calcula o tipo resultante da expressão.

        // Compara o tipo resultante com o tipo da variável que recebe a atribuição.
        if (!resultType.equalsIgnoreCase(currentType)) {
            ErrorHandler.semanticErrorInvalidExpression(currentType, resultType, currentToken);
        }
    }

    /**
     * Avalia uma expressão e retorna seu tipo resultante.
     * Lida com operadores aritméticos.
     */
    private String evaluateExpression() throws CompilerException {
        String leftType;

        if (currentToken.getName().equals("(")) { // Expressão entre parênteses
            nextToken();
            leftType = evaluateExpression();
            nextToken(); // consome ')'
        } else if (TokenUtils.isConstOrId(this.currentToken)) { // Valor ou variável
            leftType = currentToken.getType();
            nextToken();
        } else {
            ErrorHandler.semanticErrorInvalidToken(currentToken);
            return "unknown";
        }

        // Loop para lidar com operadores
        while (!isExpressionEnd()) {
            String op = currentToken.getName();
            boolean isArith = TokenUtils.isArithmeticOperator(op);
            boolean isLogic = TokenUtils.isLogicalOperator(op);

            nextToken();
            String rightType = evaluateExpression(); // Avalia o lado direito recursivamente.

            if (isArith) {
                // Para operações aritméticas, ambos os operandos devem ser 'int'.
                if (!leftType.equals("int") || !rightType.equals("int")) {
                    ErrorHandler.semanticErrorInvalidExpression("int", rightType, currentToken);
                }
                leftType = "int"; // O resultado de uma operação aritmética é 'int'.
            } else if (isLogic) {
                // Para operações lógicas, ambos devem ser 'int' ou 'boolean' (dependendo do operador).
                if (!leftType.equals("int") || !rightType.equals("int")) {
                    ErrorHandler.semanticErrorInvalidExpression("boolean", rightType, currentToken);
                }
                leftType = "boolean"; // O resultado é 'boolean'.
                break;
            }
        }

        return leftType;
    }

    // Verifica se o token atual marca o fim de uma expressão.
    private boolean isExpressionEnd() {
        String name = currentToken.getName();
        return name.equals(";") || name.equals(")") || name.equals(",") || (!TokenUtils.isArithmeticOp(this.currentToken) && !TokenUtils.isLogicalOp(this.currentToken));
    }

    // Valida a expressão de um 'if' ou 'while', garantindo que resulte em 'boolean'.
    private void checkBooleanExpression() throws CompilerException {
        nextToken(); // Avança para o início da expressão.
        String resultType = parseExpressionUntil("begin");
        if (!resultType.equals("boolean")) {
            ErrorHandler.semanticErrorInvalidExpression("boolean", resultType, currentToken);
        }
    }

    /**
     * Analisa uma expressão até encontrar um token de parada (como 'begin').
     * Usado para expressões de if/while.
     */
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
                // Operadores lógicos (and, or) podem operar em booleanos.
                // Operadores relacionais (==, <, >) operam em inteiros e resultam em booleano.
                if (!(exprType.equals("int") && rightType.equals("int")) &&
                        !(exprType.equals("boolean") && rightType.equals("boolean"))) {
                    ErrorHandler.semanticErrorInvalidExpression(exprType, rightType, currentToken);
                }
                exprType = "boolean"; // O resultado final é sempre booleano.
            } else if (isArith) {
                if (!exprType.equals("int") || !rightType.equals("int")) {
                    ErrorHandler.semanticErrorInvalidExpression("int", rightType, currentToken);
                }
                exprType = "int"; // O resultado intermediário é inteiro.
            }

            nextToken();
        }

        return exprType;
    }

    /**
     * Após as declarações serem processadas, esta função percorre a tabela de símbolos inteira
     * e atualiza o tipo de cada token de identificador com o tipo que foi determinado
     * na análise de declaração.
     */
    private void updateSymbolTypes() {
        // Para cada variável/constante que foi declarada
        for (Token declared : declaredTokens) {
            // percorre toda a tabela de símbolos.
            for (int i = 0; i < symbolsTable.getSize(); i++) {
                Token symbol = symbolsTable.currentToken(i);
                // Se encontrar um uso dessa variável
                if (declared.getName().equalsIgnoreCase(symbol.getName())) {
                    // atualize seu tipo.
                    symbol.setType(declared.getType());
                }
            }
        }
    }
}