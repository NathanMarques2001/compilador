package com.compiladores.compilador.codegen;

import com.compiladores.compilador.lexer.Token;
import com.compiladores.compilador.symboltable.SymbolsTable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Stack;

/**
 * Responsável por gerar o código Assembly (MASM) a partir da tabela de símbolos.
 * Esta é a fase final do compilador, traduzindo as estruturas da linguagem fonte
 * para instruções de máquina de baixo nível.
 */
public class AssemblyGenerator {

    private final SymbolsTable symbolsTable;
    private Token currentToken;
    private int currentTokenIndex = 0;

    // Caminho e nome do arquivo de saída.
    private final String path = "./src/main/java/com/compiladores/compilador/codegen/out";
    private String fileName = "";

    // StringBuilders para montar as diferentes seções do arquivo Assembly.
    private final StringBuilder headerSection = new StringBuilder(); // Cabeçalho e includes.
    private final StringBuilder dataSection = new StringBuilder();   // Seção .data para variáveis e constantes.
    private final StringBuilder codeSection = new StringBuilder();   // Seção .code para o código executável.

    // Contadores para gerar rótulos (labels) únicos.
    private int stringCount = 1;
    private int loopCounter = 1;
    private int ifCounter = 1;

    // Flag para controlar a declaração de formatos de `scanf`.
    private boolean formatDSDeclared = false;

    public AssemblyGenerator(SymbolsTable symbolsTable, String fileName) {
        this.symbolsTable = symbolsTable;
        this.fileName = fileName + ".asm";
        if (this.symbolsTable.getSize() > 0) {
            this.currentToken = this.symbolsTable.currentToken(this.currentTokenIndex);
        } else {
            this.currentToken = null;
        }
    }

    // Avança para o próximo token na tabela de símbolos.
    private void nextToken() {
        this.currentTokenIndex++;
        if (this.currentTokenIndex < this.symbolsTable.getSize()) {
            this.currentToken = this.symbolsTable.currentToken(this.currentTokenIndex);
        } else {
            this.currentToken = null; // Fim da tabela de símbolos.
        }
    }

    // Cria o diretório de saída para os arquivos .asm, se ele não existir.
    private void createOutDirectory() {
        File outDir = new File(this.path);
        if (!outDir.exists()) {
            outDir.mkdirs();
        }
    }

    // Escreve o código Assembly gerado em um arquivo .asm.
    private void writeAssemblyCode(String assemblyCode) {
        File asmFile = new File(this.path, this.fileName);
        try (FileWriter writer = new FileWriter(asmFile)) {
            writer.write(assemblyCode);
            System.out.println("Código Assembly escrito com sucesso no arquivo '" + this.fileName + "'.");
        } catch (IOException e) {
            System.out.println("Erro ao escrever no arquivo '" + this.fileName + "': " + e.getMessage());
        }
    }

    // Ponto de entrada público para iniciar o processo de conversão para Assembly.
    public void convert() {
        this.createOutDirectory();
        this.generateAssemblyCode();
    }

    // Orquestra a geração das seções do código Assembly e escreve o resultado no arquivo.
    private void generateAssemblyCode() {
        StringBuilder assemblyCode = new StringBuilder();

        // Gera cada seção separadamente.
        this.generateHeader();
        this.generateDataSection();
        this.generateCodeSection();

        // Concatena todas as seções para formar o arquivo final.
        assemblyCode.append(this.headerSection);
        assemblyCode.append(this.dataSection);
        assemblyCode.append(this.codeSection);

        this.writeAssemblyCode(assemblyCode.toString());
    }

    /**
     * Gera o cabeçalho padrão para um executável MASM de 32 bits para Windows,
     * utilizando o modelo de memória flat (com pilha expansível até 4GB), sem
     * diferenciação entre maiúsculas e minúsculas (case-insensitive), e incluindo
     * as bibliotecas necessárias para operações de entrada e saída (printf, scanf etc.).
     */
    private void generateHeader() {
        this.headerSection.append(".686\n")
                .append(".model flat, stdcall\n")
                .append("option casemap :none\n\n")
                .append("include \\masm32\\include\\windows.inc\n")
                .append("include \\masm32\\include\\kernel32.inc\n")
                .append("include \\masm32\\include\\masm32.inc\n")
                .append("include \\masm32\\include\\msvcrt.inc\n")
                .append("includelib \\masm32\\lib\\kernel32.lib\n")
                .append("includelib \\masm32\\lib\\masm32.lib\n")
                .append("includelib \\masm32\\lib\\msvcrt.lib\n")
                .append("include \\masm32\\macros\\macros.asm\n\n");
    }

    /**
     * Gera a seção .data, percorrendo a tabela de símbolos para encontrar
     * declarações de variáveis e constantes e alocando espaço para elas.
     */
    private void generateDataSection() {
        this.dataSection.append(".data\n");
        int originalIndex = this.currentTokenIndex; // Salva a posição atual.
        this.currentTokenIndex = 0; // Reseta para o início da tabela.
        if (this.symbolsTable.getSize() > 0) {
            this.currentToken = this.symbolsTable.currentToken(this.currentTokenIndex);
            // Itera apenas sobre a parte de declarações do código.
            while (this.currentToken != null && isDeclarationScope()) {
                identifyDeclaration();
            }
        }
        // Restaura a posição original para a geração da seção de código.
        this.currentTokenIndex = originalIndex;
        if (this.currentTokenIndex < this.symbolsTable.getSize()) {
            this.currentToken = this.symbolsTable.currentToken(this.currentTokenIndex);
        } else {
            this.currentToken = null;
        }
    }

    // Itera sobre as declarações de variáveis e constantes e as traduz para diretivas MASM.
    private void identifyDeclaration() {
        if (this.currentToken.getName().equalsIgnoreCase("final")) {
            // Tratamento de constantes (final -> equ).
            nextToken(); // Consome 'final'.
            String constName = this.currentToken.getName();
            nextToken(); // Consome o nome.
            nextToken(); // Consome '='.
            String constValue = this.currentToken.getName();

            if (constValue.startsWith("\"")) { // Constante string.
                String actualString = constValue.substring(1, constValue.length() - 1);
                String strLabel = "const_str_" + constName;
                this.dataSection.append(String.format("    %-15s db \"%s\", 0\n", strLabel, actualString));
                this.dataSection.append(String.format("    %-15s equ addr %s\n", constName, strLabel));
            } else { // Constante numérica ou booleana.
                this.dataSection.append(String.format("    %-15s equ %s\n", constName, formatValue(constValue, "int")));
            }
            nextToken(); // Consome o valor.
            nextToken(); // Consome ';'.
        } else {
            // Tratamento de variáveis.
            String type = this.currentToken.getName();
            String dataTypeMASM = primitiveTypeMASM(type);
            nextToken(); // Consome o tipo.

            // Loop para tratar múltiplas declarações na mesma linha (ex: int a, b, c;).
            while (this.currentToken != null && !this.currentToken.getName().equals(";")) {
                String dataName = this.currentToken.getName();
                nextToken(); // Consome o nome da variável.
                String dataValue = "0"; // Valor padrão para variáveis não inicializadas.

                if (type.equalsIgnoreCase("string")) {
                    this.dataSection.append(String.format("    %-15s db 256 dup(0)\n", dataName));
                } else {
                    if (this.currentToken.getName().equals("=")) { // Se houver inicialização.
                        nextToken(); // Consome '='.
                        dataValue = formatValue(this.currentToken.getName(), type);
                        nextToken(); // Consome o valor.
                    }
                    this.dataSection.append(String.format("    %-15s %-5s %s\n", dataName, dataTypeMASM, dataValue));
                }

                if (this.currentToken != null && this.currentToken.getName().equals(",")) {
                    nextToken(); // Consome ','.
                }
            }
            if (this.currentToken != null && this.currentToken.getName().equals(";")) {
                nextToken(); // Consome ';'.
            }
        }
    }

    // Gera a seção .code, onde a lógica do programa é traduzida em instruções.
    private void generateCodeSection() {
        this.codeSection.append(".code\n").append("start:\n");
        this.beginGeneration();
        // Finaliza o programa.
        this.codeSection.append("\n    invoke ExitProcess, 0\n").append("end start\n");
    }

    // Inicia a geração de código a partir do bloco 'begin'.
    private void beginGeneration() {
        // Pula os tokens de declaração até encontrar 'begin'.
        while (this.currentToken != null && !this.currentToken.getName().equalsIgnoreCase("begin")) {
            nextToken();
        }
        if (this.currentToken != null && this.currentToken.getName().equalsIgnoreCase("begin")) {
            nextToken(); // Consome 'begin'.
        }

        // Processa todos os comandos até encontrar 'end'.
        while (this.currentToken != null && !this.currentToken.getName().equalsIgnoreCase("end")) {
            identifyCommands();
        }
        if (this.currentToken != null && this.currentToken.getName().equalsIgnoreCase("end")) {
            nextToken(); // Consome 'end'.
        }
    }

    /**
     * Identifica o comando atual e delega para o método de geração apropriado.
     * Funciona como um dispatcher para os comandos da linguagem.
     */
    private void identifyCommands() {
        if (this.currentToken == null) return;

        switch (this.currentToken.getName().toLowerCase()) {
            case "write", "writeln" -> identifyWrite();
            case "readln" -> identifyRead();
            case "while" -> identifyWhile();
            case "if" -> identifyIf();
            case ";" -> nextToken(); // Comando nulo.
            default -> {
                // Se não for uma palavra-chave, pode ser uma atribuição (que começa com ID).
                if (this.currentToken.getClassification().equalsIgnoreCase("ID")) {
                    identifyAssignment();
                } else {
                    // Ignora tokens inesperados que não sejam 'end'.
                    if (!this.currentToken.getName().equalsIgnoreCase("end")) {
                        nextToken();
                    }
                }
            }
        }
    }

    // Gera código Assembly para os comandos 'write' e 'writeln' usando crt_printf.
    private void identifyWrite() {
        boolean breakLine = this.currentToken.getName().equalsIgnoreCase("writeln");
        nextToken(); // Consome 'write' ou 'writeln'.
        nextToken(); // Consome ','.

        StringBuilder formatStr = new StringBuilder(); // String de formato para printf.
        ArrayList<String> args = new ArrayList<>();   // Argumentos para printf.

        // Constrói a string de formato e a lista de argumentos.
        while (this.currentToken != null && !this.currentToken.getName().equals(";")) {
            if (this.currentToken.getClassification().equalsIgnoreCase("ID")) {
                String varName = this.currentToken.getName();
                String varType = this.symbolsTable.getSymbolType(varName);
                if (varType == null) varType = "int"; // Fallback

                if (varType.equalsIgnoreCase("string")) {
                    formatStr.append("%s");
                    args.add("addr " + varName);
                } else {
                    formatStr.append("%d");
                    args.add(varName);
                }
            } else { // Literal string no meio do write.
                String literal = this.currentToken.getName().replace("\"", "");
                formatStr.append(literal);
            }
            nextToken();
            if (this.currentToken != null && this.currentToken.getName().equals(",")) {
                nextToken(); // Consome ','.
            }
        }

        if (this.currentToken != null && this.currentToken.getName().equals(";")) {
            nextToken(); // Consome ';'.
        }

        // Declara a string de formato na seção .data.
        String dataLabel = "str" + this.stringCount++;
        String lineEnding = breakLine ? ", 13, 10, 0" : ", 0"; // Adiciona quebra de linha para writeln.
        this.dataSection.append(String.format("    %-15s db \"%s\"%s\n", dataLabel, formatStr.toString(), lineEnding));

        // Gera a chamada para a função printf.
        this.codeSection.append("    invoke crt_printf, addr ").append(dataLabel);
        for (String arg : args) {
            this.codeSection.append(", ").append(arg);
        }
        this.codeSection.append("\n");
    }

    // Gera código Assembly para o comando 'readln' usando crt_scanf ou crt_gets.
    private void identifyRead() {
        nextToken(); // Consome 'readln'.
        nextToken(); // Consome ','.
        String variableName = this.currentToken.getName();
        String varType = this.symbolsTable.getSymbolType(variableName);

        if (varType.equalsIgnoreCase("int") || varType.equalsIgnoreCase("byte") || varType.equalsIgnoreCase("boolean")) {
            // Usa scanf para tipos numéricos/booleanos.
            if (!this.formatDSDeclared) { // Declara a string de formato "%d" se ainda não foi declarada.
                this.dataSection.append(String.format("    %-15s db \"%%d\", 0\n", "format_d"));
                this.formatDSDeclared = true;
            }
            this.codeSection.append("    invoke crt_scanf, addr format_d, addr ").append(variableName).append("\n");
        } else { // string
            // Usa gets para ler strings.
            this.codeSection.append("    invoke crt_gets, addr ").append(variableName).append("\n");
        }

        nextToken(); // Consome o nome da variável.
        if (this.currentToken != null && this.currentToken.getName().equals(";")) {
            nextToken(); // Consome ';'.
        }
    }

    // Gera a estrutura de um loop 'while' em Assembly, com labels e saltos.
    private void identifyWhile() {
        int localLoopCounter = this.loopCounter++;
        String loopLabel = "_loop" + localLoopCounter;
        String loopEndLabel = "_fimLoop" + localLoopCounter;

        this.codeSection.append("\n").append(loopLabel).append(":\n"); // Label de início do loop.
        nextToken(); // Consome 'while'.

        // Gera o código para a condição do loop. O salto ocorrerá se a condição for falsa.
        generateConditionalExpression(loopEndLabel, true);

        nextToken(); // Consome 'begin'.

        // Gera o código para o corpo do loop.
        while (this.currentToken != null && !this.currentToken.getName().equalsIgnoreCase("end")) {
            identifyCommands();
        }

        if (this.currentToken != null && this.currentToken.getName().equalsIgnoreCase("end")) {
            nextToken(); // Consome 'end'.
        }

        this.codeSection.append("\n    jmp ").append(loopLabel).append("\n"); // Salta de volta para o início do loop.
        this.codeSection.append(loopEndLabel).append(":\n"); // Label de saída do loop.
    }

    // Gera a estrutura de um condicional 'if-else' em Assembly.
    private void identifyIf() {
        int localIfCounter = this.ifCounter++;
        String elseLabel = "_else" + localIfCounter;
        String endIfLabel = "_fimIf" + localIfCounter;
        nextToken(); // Consome 'if'.

        // Gera a condição. Se for falsa, salta para o bloco 'else' (ou para o fim do 'if').
        generateConditionalExpression(elseLabel, true);
        nextToken(); // Consome 'begin'.

        // Gera o código do bloco 'if'.
        while (this.currentToken != null && !this.currentToken.getName().equalsIgnoreCase("end") &&
                !this.currentToken.getName().equalsIgnoreCase("else")) {
            identifyCommands();
        }

        boolean hasElse = this.currentToken != null && this.currentToken.getName().equalsIgnoreCase("else");

        if (this.currentToken != null && this.currentToken.getName().equalsIgnoreCase("end")) {
            nextToken(); // Consome 'end' do bloco if.
        }

        if (hasElse) {
            this.codeSection.append("    jmp ").append(endIfLabel).append("\n"); // Se o 'if' executou, pula o 'else'.
            this.codeSection.append(elseLabel).append(":\n"); // Label do bloco 'else'.
            nextToken(); // Consome 'else'.
            nextToken(); // Consome 'begin' do else.

            // Gera o código do bloco 'else'.
            while (this.currentToken != null && !this.currentToken.getName().equalsIgnoreCase("end")) {
                identifyCommands();
            }
            if (this.currentToken != null && this.currentToken.getName().equalsIgnoreCase("end")) {
                nextToken(); // Consome 'end' do else.
            }
            this.codeSection.append(endIfLabel).append(":\n"); // Label do fim do if-else.
        } else {
            // Se não houver 'else', o 'elseLabel' se torna o ponto de saída do 'if'.
            this.codeSection.append(elseLabel).append(":\n");
        }
    }

    // Gera código para uma expressão condicional (ex: x < 5), resultando em um salto.
    private void generateConditionalExpression(String targetLabel, boolean jumpIfConditionFalse) {
        String firstOperand = this.currentToken.getName();
        nextToken(); // Avança para o operador.
        String operator = this.currentToken.getName();
        nextToken(); // Avança para o segundo operando.
        String secondOperand = this.currentToken.getName();

        // Usa eax para inteiros, al para bytes/booleanos.
        String reg = "eax";
        String type = symbolsTable.getSymbolType(firstOperand);
        if (type != null && (type.equalsIgnoreCase("boolean") || type.equalsIgnoreCase("byte"))) {
            reg = "al";
        }

        // Carrega o primeiro operando no registrador, compara com o segundo.
        this.codeSection.append("    mov ").append(reg).append(", ").append(formatValue(firstOperand, type)).append("\n");
        this.codeSection.append("    cmp ").append(reg).append(", ").append(formatValue(secondOperand, type)).append("\n");

        // Determina a instrução de salto correta com base no operador.
        String jumpInstruction = getJumpInstruction(operator, jumpIfConditionFalse);
        this.codeSection.append("    ").append(jumpInstruction).append(" ").append(targetLabel).append("\n");
    }

    // Mapeia um operador relacional para a instrução de salto condicional correspondente em Assembly.
    private String getJumpInstruction(String operator, boolean jumpIfConditionFalse) {
        return switch (operator) {
            case "==" -> jumpIfConditionFalse ? "jne" : "je"; // Jump if Not Equal / Jump if Equal
            case "<>" -> jumpIfConditionFalse ? "je" : "jne"; // Jump if Equal / Jump if Not Equal
            case "<" -> jumpIfConditionFalse ? "jge" : "jl";  // Jump if Greater or Equal / Jump if Less
            case ">" -> jumpIfConditionFalse ? "jle" : "jg";  // Jump if Less or Equal / Jump if Greater
            case "<=" -> jumpIfConditionFalse ? "jg" : "jle";  // Jump if Greater / Jump if Less or Equal
            case ">=" -> jumpIfConditionFalse ? "jl" : "jge";  // Jump if Less / Jump if Greater or Equal
            default -> "";
        };
    }

    // Gera código para um comando de atribuição.
    private void identifyAssignment() {
        String variableName = this.currentToken.getName();
        String varType = this.symbolsTable.getSymbolType(variableName);
        if (varType == null) varType = "int"; // Fallback

        nextToken(); // Consome o nome da variável.
        nextToken(); // Consome '='.

        if (varType.equalsIgnoreCase("string")) {
            // Atribuição de string usa a função crt_strcpy.
            String stringLiteral = this.currentToken.getName();
            String actualStringValue = stringLiteral.substring(1, stringLiteral.length() - 1);
            String stringLabelInData = "str_assign_" + this.stringCount++;
            this.dataSection.append(String.format("    %-15s db \"%s\", 0\n", stringLabelInData, actualStringValue));
            this.codeSection.append("    invoke crt_strcpy, addr ").append(variableName).append(", addr ").append(stringLabelInData).append("\n");
            nextToken();
        } else {
            // Para tipos numéricos, coleta a expressão e a avalia.
            ArrayList<Token> expressionTokens = new ArrayList<>();
            while (this.currentToken != null && !this.currentToken.getName().equals(";")) {
                expressionTokens.add(this.currentToken);
                nextToken();
            }
            evaluateExpression(expressionTokens);

            // O resultado da expressão estará no topo da pilha.
            this.codeSection.append("    pop eax\n");
            if (varType.equalsIgnoreCase("boolean") || varType.equalsIgnoreCase("byte")) {
                this.codeSection.append("    mov ").append(variableName).append(", al\n"); // Usa o registrador de 8 bits.
            } else {
                this.codeSection.append("    mov ").append(variableName).append(", eax\n"); // Usa o registrador de 32 bits.
            }
        }

        if (this.currentToken != null && this.currentToken.getName().equals(";")) {
            nextToken(); // Consome ';'.
        }
    }

    /**
     * Avalia uma expressão aritmética infixa usando o algoritmo Shunting-yard
     * para gerar código Assembly em ordem postfix.
     */
    private void evaluateExpression(ArrayList<Token> tokens) {
        Stack<String> ops = new Stack<>(); // Pilha de operadores.

        for (Token token : tokens) {
            String name = token.getName();

            // Se for um número ou variável, empurra o valor para a pilha do processador.
            if (token.getClassification().matches("ID|CONST") || name.equalsIgnoreCase("true") || name.equalsIgnoreCase("false")) {
                String valueToPush = formatValue(name, token.getType());
                this.codeSection.append("    push ").append(valueToPush).append("\n");
            } else if (name.equals("(")) {
                ops.push(name);
            } else if (name.equals(")")) {
                // Desempilha operadores até encontrar '('.
                while (!ops.empty() && !ops.peek().equals("(")) {
                    generateOp(ops.pop());
                }
                if (!ops.empty()) ops.pop(); // Descarta '('.
            } else if (isOperator(name)) {
                // Desempilha operadores com maior ou igual precedência.
                while (!ops.empty() && hasPrecedence(ops.peek(), name)) {
                    generateOp(ops.pop());
                }
                ops.push(name);
            }
        }
        // Desempilha e aplica os operadores restantes.
        while (!ops.empty()) {
            generateOp(ops.pop());
        }
    }

    // Gera a instrução Assembly para um operador aritmético.
    private void generateOp(String op) {
        // Desempilha os dois operandos para os registradores ebx e eax.
        this.codeSection.append("    pop ebx\n"); // Segundo operando.
        this.codeSection.append("    pop eax\n"); // Primeiro operando.
        switch (op) {
            case "+" -> this.codeSection.append("    add eax, ebx\n");
            case "-" -> this.codeSection.append("    sub eax, ebx\n");
            case "*" -> this.codeSection.append("    imul eax, ebx\n");
            case "/" -> {
                this.codeSection.append("    cdq\n"); // Estende o sinal de eax para edx (necessário para idiv).
                this.codeSection.append("    idiv ebx\n"); // Divisão, resultado em eax.
            }
        }
        this.codeSection.append("    push eax\n"); // Empurra o resultado de volta para a pilha.
    }

    private boolean isDeclarationScope() {
        return this.currentToken != null && (isPrimitiveType() || this.currentToken.getName().equalsIgnoreCase("final"));
    }

    private String formatValue(String value, String type) {
        if (value == null) return "0";
        if (type != null && type.equalsIgnoreCase("boolean")) {
            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("Fh")) return "1";
            if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("0h")) return "0";
        }
        if (type != null && value.toLowerCase().startsWith("0h") && type.equalsIgnoreCase("byte")) {
            return value.substring(2) + "h";
        }
        return value;
    }

    private String primitiveTypeMASM(String type) {
        return switch (type.toLowerCase()) {
            case "int" -> "dd"; // Double Word (32 bits)
            case "boolean", "byte" -> "db"; // Define Byte (8 bits)
            default -> "";
        };
    }

    private boolean isPrimitiveType() {
        if (this.currentToken == null) return false;
        String name = this.currentToken.getName().toLowerCase();
        return name.equals("int") || name.equals("string") || name.equals("boolean") || name.equals("byte");
    }

    private boolean isOperator(String op) {
        return op.matches("[+\\-*/]");
    }

    private boolean hasPrecedence(String op1, String op2) {
        if (op1.equals("(") || op1.equals(")")) return false;
        if ((op1.equals("*") || op1.equals("/")) && (op2.equals("+") || op2.equals("-"))) return true;
        if ((op1.equals("*") || op1.equals("/")) && (op2.equals("*") || op2.equals("/"))) return true;
        return (op1.equals("+") || op1.equals("-")) && (op2.equals("+") || op2.equals("-"));
    }
}