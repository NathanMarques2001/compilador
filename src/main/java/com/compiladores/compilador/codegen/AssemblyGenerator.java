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

    // Flag para controlar a declaração de formatos de `scanf` e evitar duplicação.
    private boolean formatDSDeclared = false;

    // Construtor que inicializa o gerador com a tabela de símbolos e o nome do arquivo de saída.
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

    // Método auxiliar para verificar se uma string é um operador relacional.
    private boolean isRelationalOperator(String op) {
        if (op == null) return false;
        return op.equals("==") || op.equals("<>") || op.equals("<") || op.equals(">") || op.equals("<=") || op.equals(">=");
    }

    // Cria o diretório de saída para os arquivos .asm, se ele não existir.
    private void createOutDirectory() {
        File outDir = new File(this.path);
        if (!outDir.exists()) {
            outDir.mkdirs();
        }
    }

    // Escreve o conteúdo final do código Assembly em um arquivo .asm.
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
     * incluindo as bibliotecas necessárias para operações de I/O.
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
        // Trata declarações de constantes (final).
        if (this.currentToken.getName().equalsIgnoreCase("final")) {
            nextToken(); // Consome 'final'.
            String constName = this.currentToken.getName();
            nextToken(); // Consome o nome da constante.
            nextToken(); // Consome '='.
            String constValue = this.currentToken.getName();

            // Constantes string são declaradas como 'db' e seu endereço é atribuído com 'equ'.
            if (constValue.startsWith("\"")) {
                String actualString = constValue.substring(1, constValue.length() - 1);
                String strLabel = "const_str_" + constName;
                this.dataSection.append(String.format("    %-15s db \"%s\", 0\n", strLabel, actualString));
                this.dataSection.append(String.format("    %-15s equ addr %s\n", constName, strLabel));
            } else { // Constantes numéricas são diretamente traduzidas com 'equ'.
                this.dataSection.append(String.format("    %-15s equ %s\n", constName, formatValue(constValue, "int")));
            }
            nextToken(); // Consome o valor.
            nextToken(); // Consome ';'.
        } else {
            String type = this.currentToken.getName();
            String dataTypeMASM = primitiveTypeMASM(type);
            nextToken(); // Consome o tipo.

            // Loop para tratar múltiplas declarações na mesma linha (ex: int a, b;).
            while (this.currentToken != null && !this.currentToken.getName().equals(";")) {
                String dataName = this.currentToken.getName();
                nextToken(); // Consome o nome da variável.
                String dataValue = "0"; // Valor padrão para variáveis não inicializadas.

                // Strings são alocadas com um buffer de 256 bytes.
                if (type.equalsIgnoreCase("string")) {
                    this.dataSection.append(String.format("    %-15s db 256 dup(0)\n", dataName));
                } else {
                    // Verifica se há uma inicialização de valor.
                    if (this.currentToken.getName().equals("=")) {
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
        // Finaliza o programa chamando a função ExitProcess.
        this.codeSection.append("\n    invoke ExitProcess, 0\n").append("end start\n");
    }

    // Inicia a geração de código a partir do bloco principal 'begin'.
    private void beginGeneration() {
        // Avança todos os tokens da fase de declaração até encontrar 'begin'.
        while (this.currentToken != null && !this.currentToken.getName().equalsIgnoreCase("begin")) {
            nextToken();
        }
        if (this.currentToken != null && this.currentToken.getName().equalsIgnoreCase("begin")) {
            nextToken(); // Consome 'begin'.
        }

        // Processa todos os comandos dentro do bloco principal.
        while (this.currentToken != null && !this.currentToken.getName().equalsIgnoreCase("end")) {
            identifyCommands();
        }
        if (this.currentToken != null && this.currentToken.getName().equalsIgnoreCase("end")) {
            nextToken(); // Consome 'end'.
        }
    }

    /**
     * Identifica o comando atual e delega para o método de geração apropriado.
     * Atua como um dispatcher para os diferentes comandos da linguagem.
     */
    private void identifyCommands() {
        if (this.currentToken == null) return;

        switch (this.currentToken.getName().toLowerCase()) {
            case "write", "writeln" -> identifyWrite();
            case "readln" -> identifyRead();
            case "while" -> identifyWhile();
            case "if" -> identifyIf();
            case ";" -> nextToken(); // Ignora comandos nulos (ponto e vírgula extra).
            default -> {
                // Se não for uma palavra-chave, assume que é uma atribuição (que começa com um ID).
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

        StringBuilder formatStr = new StringBuilder(); // String de formato para printf (ex: "%d %s").
        ArrayList<String> args = new ArrayList<>();   // Argumentos para printf.

        // Constrói a string de formato e a lista de argumentos.
        while (this.currentToken != null && !this.currentToken.getName().equals(";")) {
            if (this.currentToken.getClassification().equalsIgnoreCase("ID")) {
                String varName = this.currentToken.getName();
                String varType = this.symbolsTable.getSymbolType(varName);
                if (varType == null) varType = "int"; // Fallback para tipo desconhecido.

                if (varType.equalsIgnoreCase("string")) {
                    formatStr.append("%s");
                    args.add("addr " + varName); // Para strings, passamos o endereço.
                } else { // Trata literais de string no meio do write.
                    formatStr.append("%d");
                    args.add(varName); // Para outros tipos, passamos o valor.
                }
            } else {
                String literal = this.currentToken.getName().replace("\"", "").replace("'", "");
                formatStr.append(literal);
            }
            nextToken();
            if (this.currentToken != null && this.currentToken.getName().equals(",")) {
                nextToken(); // Consome a vírgula entre os argumentos.
            }
        }

        if (this.currentToken != null && this.currentToken.getName().equals(";")) {
            nextToken(); // Consome o ';' final.
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

        // Usa crt_scanf para tipos numéricos e booleanos.
        if (varType != null && (varType.equalsIgnoreCase("int") || varType.equalsIgnoreCase("byte") || varType.equalsIgnoreCase("boolean"))) {
            // Declara a string de formato "%d" uma única vez.
            if (!this.formatDSDeclared) {
                this.dataSection.append(String.format("    %-15s db \"%%d\", 0\n", "format_d"));
                this.formatDSDeclared = true;
            }
            this.codeSection.append("    invoke crt_scanf, addr format_d, addr ").append(variableName).append("\n");
        } else { // Usa crt_gets para ler strings.
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

        // Gera o código para a condição. O salto para o fim do loop ocorrerá se a condição for falsa.
        generateConditionalExpression(loopEndLabel, true);

        if (this.currentToken != null && this.currentToken.getName().equalsIgnoreCase("begin")) {
            nextToken(); // Consome 'begin'.
        }

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

        if (this.currentToken != null && this.currentToken.getName().equalsIgnoreCase("begin")) {
            nextToken();
        }

        // Processa o corpo do IF
        while (this.currentToken != null && !this.currentToken.getName().equalsIgnoreCase("end") && !this.currentToken.getName().equalsIgnoreCase("else")) {
            identifyCommands();
        }

        // Verifica se temos um bloco else
        if (this.currentToken != null && this.currentToken.getName().equalsIgnoreCase("else")) {
            // Se o bloco IF foi executado, salta sobre o bloco ELSE.
            this.codeSection.append("    jmp ").append(endIfLabel).append("\n");
            this.codeSection.append(elseLabel).append(":\n");
            nextToken(); // Consome 'else'.
            if (this.currentToken != null && this.currentToken.getName().equalsIgnoreCase("begin")) {
                nextToken(); // Consome 'begin' do else.
            }
            // Processa o corpo do ELSE.
            while (this.currentToken != null && !this.currentToken.getName().equalsIgnoreCase("end")) {
                identifyCommands();
            }
            if (this.currentToken != null && this.currentToken.getName().equalsIgnoreCase("end")) {
                nextToken(); // Consome o 'end' do ELSE
            }
            this.codeSection.append(endIfLabel).append(":\n");
        } else {
            // Sem bloco else, o elseLabel é o fim do IF
            this.codeSection.append(elseLabel).append(":\n");
        }

        // Consome o 'end' que fecha a estrutura IF (ou IF-ELSE aninhado).
        if (this.currentToken != null && this.currentToken.getName().equalsIgnoreCase("end")) {
            nextToken();
        }
    }

    // Gera código para uma expressão condicional, resultando em um salto.
    private void generateConditionalExpression(String targetLabel, boolean jumpIfConditionFalse) {
        String firstOperand = this.currentToken.getName();
        String type = symbolsTable.getSymbolType(firstOperand);
        nextToken(); // Consome o primeiro operando.

        // Verifica se é uma comparação explícita (ex: n >= 10).
        if (isRelationalOperator(this.currentToken.getName())) {
            String operator = this.currentToken.getName();
            nextToken();
            String secondOperand = this.currentToken.getName();
            nextToken();

            // Usa 'eax' (32 bits) para inteiros, 'al' (8 bits) para bytes/booleanos.
            String reg = "eax";
            if (type != null && (type.equalsIgnoreCase("boolean") || type.equalsIgnoreCase("byte"))) {
                reg = "al";
            }

            // Carrega os operandos, compara e salta.
            this.codeSection.append("    mov ").append(reg).append(", ").append(formatValue(firstOperand, type)).append("\n");
            this.codeSection.append("    cmp ").append(reg).append(", ").append(formatValue(secondOperand, type)).append("\n");

            String jumpInstruction = getJumpInstruction(operator, jumpIfConditionFalse);
            this.codeSection.append("    ").append(jumpInstruction).append(" ").append(targetLabel).append("\n");

        } else { // Trata comparações booleanas implícitas (ex: while naoTerminou).
            String reg = "al"; // Booleans são sempre bytes.

            this.codeSection.append("    mov ").append(reg).append(", ").append(firstOperand).append("\n");
            this.codeSection.append("    cmp ").append(reg).append(", 1\n"); // Compara com 'true' (1).

            String jumpInstruction = jumpIfConditionFalse ? "jne" : "je"; // jne: salta se não for verdadeiro.
            this.codeSection.append("    ").append(jumpInstruction).append(" ").append(targetLabel).append("\n");
        }
    }

    // Mapeia um operador relacional para a instrução de salto condicional correspondente em Assembly.
    private String getJumpInstruction(String operator, boolean jumpIfConditionFalse) {
        return switch (operator) {
            case "==" -> jumpIfConditionFalse ? "jne" : "je";
            case "<>" -> jumpIfConditionFalse ? "je" : "jne";
            case "<" -> jumpIfConditionFalse ? "jge" : "jl";
            case ">" -> jumpIfConditionFalse ? "jle" : "jg";
            case "<=" -> jumpIfConditionFalse ? "jg" : "jle";
            case ">=" -> jumpIfConditionFalse ? "jl" : "jge";
            default -> ""; // Caso inválido
        };
    }

    // Gera código para um comando de atribuição.
    private void identifyAssignment() {
        String variableName = this.currentToken.getName();
        String varType = this.symbolsTable.getSymbolType(variableName);
        if (varType == null) varType = "int"; // Fallback.

        nextToken(); // Consome o nome da variável.
        nextToken(); // Consome '='.

        // Atribuição de string usa a função crt_strcpy.
        if (varType.equalsIgnoreCase("string")) {
            String stringLiteral = this.currentToken.getName();
            String actualStringValue;

            // Validação para remover aspas de forma segura.
            if (stringLiteral.length() >= 2 && stringLiteral.startsWith("\"") && stringLiteral.endsWith("\"")) {
                actualStringValue = stringLiteral.substring(1, stringLiteral.length() - 1);
            } else {
                // Emite um aviso se o valor não for uma string entre aspas.
                System.err.println("[Aviso de Geração de Código] Atribuição para string '" + variableName + "' com valor malformado: " + stringLiteral);
                actualStringValue = ""; // Usa uma string vazia para evitar crash.
            }

            // Declara a string na seção .data e invoca a cópia.
            String stringLabelInData = "str_assign_" + this.stringCount++;
            this.dataSection.append(String.format("    %-15s db \"%s\", 0\n", stringLabelInData, actualStringValue));
            this.codeSection.append("    invoke crt_strcpy, addr ").append(variableName).append(", addr ").append(stringLabelInData).append("\n");
            nextToken();
        } else { // Para tipos numéricos/booleanos, avalia a expressão.
            ArrayList<Token> expressionTokens = new ArrayList<>();
            while (this.currentToken != null && !this.currentToken.getName().equals(";")) {
                expressionTokens.add(this.currentToken);
                nextToken();
            }
            evaluateExpression(expressionTokens);

            // O resultado da expressão está no topo da pilha do processador.
            this.codeSection.append("    pop eax\n");
            // Move o resultado para a variável correta (8 bits para boolean/byte, 32 bits para int).
            if (varType.equalsIgnoreCase("boolean") || varType.equalsIgnoreCase("byte")) {
                this.codeSection.append("    mov ").append(variableName).append(", al\n");
            } else {
                this.codeSection.append("    mov ").append(variableName).append(", eax\n");
            }
        }

        if (this.currentToken != null && this.currentToken.getName().equals(";")) {
            nextToken(); // Consome ';' final.
        }
    }

    /**
     * Avalia uma expressão aritmética infixa usando o algoritmo Shunting-yard
     * para gerar código Assembly em ordem pós-fixa (usando a pilha do processador).
     */
    private void evaluateExpression(ArrayList<Token> tokens) {
        Stack<String> ops = new Stack<>(); // Pilha para operadores.

        for (Token token : tokens) {
            String name = token.getName();

            // Se o token for um operando (ID ou constante), empurra seu valor na pilha do processador.
            if (token.getClassification().equalsIgnoreCase("ID") || token.getClassification().equalsIgnoreCase("CONST") || name.equalsIgnoreCase("true") || name.equalsIgnoreCase("false")) {
                String valueToPush = formatValue(name, token.getType());
                this.codeSection.append("    push ").append(valueToPush).append("\n");
            } else if (name.equals("(")) { // Empilha parênteses de abertura.
                ops.push(name);
            } else if (name.equals(")")) { // Ao encontrar ')', desempilha operadores até encontrar '('.
                while (!ops.empty() && !ops.peek().equals("(")) {
                    generateOp(ops.pop());
                }
                if (!ops.empty()) ops.pop(); // Descarta o '('.
            } else if (isOperator(name)) { // Se for um operador aritmético...
                // Desempilha operadores com maior ou igual precedência antes de empilhar o atual.
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

    // Gera a instrução Assembly para um operador aritmético (+, -, *, /).
    private void generateOp(String op) {
        // Retira os dois operandos do topo da pilha para os registradores.
        this.codeSection.append("    pop ebx\n"); // Segundo operando.
        this.codeSection.append("    pop eax\n"); // Primeiro operando.
        switch (op) {
            case "+" -> this.codeSection.append("    add eax, ebx\n");
            case "-" -> this.codeSection.append("    sub eax, ebx\n");
            case "*" -> this.codeSection.append("    imul eax, ebx\n");
            case "/" -> {
                // Prepara para a divisão de 32 bits.
                this.codeSection.append("    cdq\n"); // Estende o sinal de eax para edx.
                this.codeSection.append("    idiv ebx\n"); // Quociente em eax, resto em edx.
            }
        }
        this.codeSection.append("    push eax\n"); // Empurra o resultado de volta para a pilha.
    }

    // Verifica se o token atual está no escopo de declaração.
    private boolean isDeclarationScope() {
        return this.currentToken != null && (isPrimitiveType() || this.currentToken.getName().equalsIgnoreCase("final"));
    }

    // Formata um valor da linguagem fonte para o formato correto em Assembly.
    private String formatValue(String value, String type) {
        if (value == null) return "0";
        if (type != null && type.equalsIgnoreCase("boolean")) {
            // Verificação para Fh (true) e 0h (false).
            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("1") || value.equalsIgnoreCase("Fh"))
                return "1";
            if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("0") || value.equalsIgnoreCase("0h"))
                return "0";
        }
        // Converte o formato 0hXX para XXh para bytes.
        if (value.toLowerCase().startsWith("0h") && type != null && type.equalsIgnoreCase("byte")) {
            return value.substring(2) + "h";
        }
        return value;
    }

    // Mapeia um tipo primitivo da linguagem para a diretiva de dados correspondente do MASM.
    private String primitiveTypeMASM(String type) {
        return switch (type.toLowerCase()) {
            case "int" -> "dd"; // Define Double Word (32 bits)
            case "boolean", "byte" -> "db"; // Define Byte (8 bits)
            default -> "";
        };
    }

    // Verifica se o token atual é um tipo primitivo da linguagem.
    private boolean isPrimitiveType() {
        if (this.currentToken == null) return false;
        String name = this.currentToken.getName().toLowerCase();
        return name.equals("int") || name.equals("string") || name.equals("boolean") || name.equals("byte");
    }

    // Verifica se uma string é um operador aritmético.
    private boolean isOperator(String op) {
        return op.matches("[+\\-*/]");
    }

    // Verifica a precedência entre dois operadores aritméticos.
    private boolean hasPrecedence(String op1, String op2) {
        if (op1.equals("(") || op1.equals(")")) return false;
        if ((op1.equals("*") || op1.equals("/")) && (op2.equals("+") || op2.equals("-"))) return true;
        if ((op1.equals("*") || op1.equals("/")) && (op2.equals("*") || op2.equals("/"))) return true;
        return (op1.equals("+") || op1.equals("-")) && (op2.equals("+") || op2.equals("-"));
    }
}
