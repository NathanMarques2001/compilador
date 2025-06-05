package com.compiladores.compilador.Assembly;

import com.compiladores.compilador.Lexical.Token;
import com.compiladores.compilador.Table.SymbolsTable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Stack;

public class AssemblyGenerator {

    private final SymbolsTable symbolsTable;
    private Token currentToken;
    private int currentTokenIndex = 0;
    private final String path = "./src/main/java/out";
    private String fileName = "";
    private final StringBuilder headerSection = new StringBuilder();
    private final StringBuilder dataSection = new StringBuilder();
    private final StringBuilder codeSection = new StringBuilder();
    private int stringCount = 1;
    private int loopCounter = 1;
    private int ifCounter = 1;
    private boolean formatDSDeclared = false; // Flag to ensure format_d is declared only once

    public AssemblyGenerator(SymbolsTable symbolsTable, String fileName) {
        this.symbolsTable = symbolsTable;
        this.fileName = fileName + ".asm";
        // It's safer to get the first token only if the table is not empty
        if (this.symbolsTable.getSize() > 0) {
            this.currentToken = symbolsTable.currentToken(this.currentTokenIndex);
        } else {
            this.currentToken = null; // Handle empty symbol table
        }
    }

    private void nextToken() {
        this.currentTokenIndex++;
        if (this.currentTokenIndex < this.symbolsTable.getSize()) {
            this.currentToken = this.symbolsTable.currentToken(this.currentTokenIndex);
        } else {
            this.currentToken = null; // Indicates the end of tokens
        }
    }

    private void createOutDirectory() {
        File outDir = new File(path);
        if (!outDir.exists()) {
            outDir.mkdirs(); // Cria o diretório se não existir
        }
    }

    private void writeAssemblyCode(String assemblyCode) {
        File asmFile = new File(path, fileName);
        try (FileWriter writer = new FileWriter(asmFile)) {
            writer.write(assemblyCode);
            System.out.println("Código Assembly escrito com sucesso no arquivo '" + fileName + "'.");
        } catch (IOException e) {
            System.out.println("Erro ao escrever no arquivo '" + fileName + "': " + e.getMessage());
        }
    }

    public void convert() {
        createOutDirectory();
        generateAssemblyCode();
    }

    private void generateAssemblyCode() {
        StringBuilder assemblyCode = new StringBuilder();
        generateHeader();
        generateDataSection(); // Gerar a seção de dados primeiro, incluindo declarações
        generateCodeSection(); // Depois gerar a seção de código

        assemblyCode.append(headerSection);
        assemblyCode.append(dataSection);
        assemblyCode.append(codeSection);

        writeAssemblyCode(assemblyCode.toString());
    }

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

    private void generateDataSection() {
        this.dataSection.append(".data\n");
        // Adiciona strings fixas se necessário (ex: Natan/Nathan do exemplo anterior)
        // this.dataSection.append(String.format("    %-15s db %s, 0\n", "str_Natan", "\"Natan\""));
        // this.dataSection.append(String.format("    %-15s db %s, 0\n", "str_Nathan", "\"Nathan\""));

        // Percorre a tabela de símbolos para encontrar declarações
        // É importante resetar o currentTokenIndex para o início da tabela
        // para processar declarações antes de gerar o código.
        int originalIndex = this.currentTokenIndex;
        this.currentTokenIndex = 0;
        if (this.symbolsTable.getSize() > 0) {
            this.currentToken = this.symbolsTable.currentToken(this.currentTokenIndex);
            while (currentToken != null && isDeclarationScope()) {
                identifyDeclaration();
            }
        }
        // Restaura o índice do token para onde estava antes de processar declarações
        // ou para o início da seção de código (após declarações)
        // A lógica de 'beginGeneration' cuidará de avançar até 'begin'
        this.currentTokenIndex = originalIndex; // Ou um valor que aponte para o início dos comandos
        if (this.currentTokenIndex < this.symbolsTable.getSize()) {
            this.currentToken = this.symbolsTable.currentToken(this.currentTokenIndex);
        } else {
            this.currentToken = null;
        }
    }

    private boolean isDeclarationScope() {
        // Verifica se o token atual faz parte de uma declaração global
        // Isso é chamado ANTES do loop principal de comandos
        return currentToken != null && (isPrimitiveType() || currentToken.getName().equalsIgnoreCase("final"));
    }

    private String formatValue(String value, String type) {
        if (value == null) return "0"; // Default for uninitialized
        if (type.equalsIgnoreCase("boolean")) {
            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("Fh")) return "1";
            if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("0h")) return "0";
        }
        if (value.toLowerCase().startsWith("0h") && type.equalsIgnoreCase("byte")) {
            // Para byte hexa 0hXXXX, MASM espera XXXXh
            return value.substring(2) + "h";
        }
        // Para inteiros e outros, retorna o valor como está (assumindo que já é numérico)
        // Para constantes string (final NOME="valor"), isso não se aplica aqui,
        // pois constantes string são tratadas como EQU de endereços de labels.
        return value;
    }


    private void identifyDeclaration() {
        if (currentToken.getName().equalsIgnoreCase("final")) {
            nextToken(); // Consome 'final'
            String constName = currentToken.getName();
            nextToken(); // Consome nome da constante
            nextToken(); // Consome '='
            String constValue = currentToken.getName(); // Valor literal
            // Para 'final MAXITER=10;', constValue será "10"
            // Para 'final NOME="texto";', constValue será "\"texto\"" (com aspas)
            // O tipo da constante pode ser inferido ou checado aqui

            // Se for uma string literal, ela precisa ser declarada separadamente
            // e a constante 'equ' apontará para o endereço dessa string.
            if (constValue.startsWith("\"")) {
                String actualString = constValue.substring(1, constValue.length() - 1);
                String strLabel = "const_str_" + constName;
                dataSection.append(String.format("    %-15s db \"%s\", 0\n", strLabel, actualString));
                dataSection.append(String.format("    %-15s equ addr %s\n", constName, strLabel));
            } else { // Para números ou booleanos (true/false)
                // Tentamos obter o tipo da constante se possível, senão default para int para formatValue
                String constType = symbolsTable.getSymbolType(constName); // Isso pode não funcionar bem para 'final'
                // pois o tipo é inferido do valor.
                // Para 'final boolean FLAG = true', constValue="true"
                // Se o lexer gerar "Fh" para true, então constValue="Fh"
                if (constValue.equalsIgnoreCase("true") || constValue.equalsIgnoreCase("false") ||
                        constValue.equalsIgnoreCase("Fh") || constValue.equalsIgnoreCase("0h") ) {
                    dataSection.append(String.format("    %-15s equ %s\n", constName, formatValue(constValue, "boolean")));
                } else { // Assume numérico
                    dataSection.append(String.format("    %-15s equ %s\n", constName, formatValue(constValue, "int")));
                }
            }
            nextToken(); // Consome valor
            nextToken(); // Consome ';'
        } else {
            String type = currentToken.getName(); // int, string, boolean, byte
            String dataTypeMASM = primitiveTypeMASM(type);
            nextToken(); // Consome tipo

            while (currentToken != null && !currentToken.getName().equals(";")) {
                String dataName = currentToken.getName();
                nextToken(); // Consome nome da variável
                String dataValue = "0"; // Valor padrão
                if (type.equalsIgnoreCase("string")) {
                    // Strings são arrays de bytes, inicializados com 0
                    dataSection.append(String.format("    %-15s db 256 dup(0)\n", dataName));
                } else {
                    if (currentToken.getName().equals("=")) {
                        nextToken(); // Consome '='
                        dataValue = formatValue(currentToken.getName(), type); // Usa o tipo da variável
                        nextToken(); // Consome valor
                    }
                    dataSection.append(String.format("    %-15s %-5s %s\n", dataName, dataTypeMASM, dataValue));
                }

                if (currentToken != null && currentToken.getName().equals(",")) {
                    nextToken(); // Consome ',' para próxima declaração na mesma linha
                }
            }
            if (currentToken != null && currentToken.getName().equals(";")) {
                nextToken(); // Consome ';'
            }
        }
    }


    private String primitiveTypeMASM(String type) {
        return switch (type.toLowerCase()) {
            case "int" -> "dd"; // Double word for integers
            case "boolean", "byte" -> "db"; // Byte for boolean and byte
            // String é tratado de forma especial (db 256 dup(0))
            default -> ""; // Should not happen for valid types
        };
    }

    private boolean isPrimitiveType() {
        return currentToken != null && (
                currentToken.getName().equalsIgnoreCase("int") ||
                        currentToken.getName().equalsIgnoreCase("string") ||
                        currentToken.getName().equalsIgnoreCase("boolean") ||
                        currentToken.getName().equalsIgnoreCase("byte")
        );
    }

    private void generateCodeSection() {
        this.codeSection.append(".code\n").append("start:\n");
        beginGeneration(); // Processa os comandos dentro do bloco principal
        codeSection.append("\n    invoke ExitProcess, 0\n").append("end start\n");
    }

    private void beginGeneration() {
        // Avança até encontrar 'begin' que marca o início do bloco de código principal
        while (currentToken != null && !currentToken.getName().equalsIgnoreCase("begin")) {
            nextToken();
        }
        if (currentToken != null && currentToken.getName().equalsIgnoreCase("begin")) {
            nextToken(); // Consome 'begin'
        }

        // Processa comandos até encontrar 'end' (do bloco principal) ou fim dos tokens
        while (currentToken != null && !currentToken.getName().equalsIgnoreCase("end")) {
            identifyCommands();
        }
        if (currentToken != null && currentToken.getName().equalsIgnoreCase("end")) {
            nextToken(); // Consome 'end' final do programa
        }
    }

    private void identifyCommands() {
        if (currentToken == null) return; // Se não há mais tokens, retorna

        switch (currentToken.getName().toLowerCase()) {
            case "write", "writeln" -> identifyWrite();
            case "readln" -> identifyRead();
            case "while" -> identifyWhile();
            case "if" -> identifyIf();
            case ";" -> nextToken(); // Comando vazio, apenas avança
            default -> {
                // Se for um ID, pode ser uma atribuição
                if (currentToken.getClassification().equalsIgnoreCase("ID")) {
                    identifyAssignment();
                } else {
                    // Token inesperado, pode ser um erro ou o fim de um bloco aninhado
                    // Se for 'end' de um bloco aninhado, o loop em identifyWhile/If cuidará disso.
                    // Caso contrário, avançar pode ser perigoso se não for tratado.
                    // Por ora, se não for um comando conhecido, e não 'end', avançamos.
                    if (!currentToken.getName().equalsIgnoreCase("end")) {
                        nextToken();
                    }
                }
            }
        }
    }

    private void identifyWrite() {
        boolean breakLine = currentToken.getName().equalsIgnoreCase("writeln");
        nextToken(); // Consome 'write' ou 'writeln'
        nextToken(); // Consome ','

        StringBuilder formatStr = new StringBuilder();
        ArrayList<String> args = new ArrayList<>();

        while (currentToken != null && !currentToken.getName().equals(";")) {
            if (currentToken.getClassification().equalsIgnoreCase("ID")) {
                String varName = currentToken.getName();
                // É crucial obter o tipo da variável da tabela de símbolos
                String varType = symbolsTable.getSymbolType(varName);
                if (varType == null) varType = "int"; // Fallback, mas deveria estar na tabela

                if (varType.equalsIgnoreCase("string")) {
                    formatStr.append("%s");
                    args.add("addr " + varName);
                } else { // int, boolean, byte
                    formatStr.append("%d");
                    args.add(varName); // Passa o valor para int/boolean/byte
                }
            } else { // Literal (string)
                String literal = currentToken.getName().replace("\"", ""); // Remove aspas
                formatStr.append(literal);
            }
            nextToken(); // Consome o argumento (ID ou literal)
            if (currentToken != null && currentToken.getName().equals(",")) {
                nextToken(); // Consome ',' se houver mais argumentos
            }
        }

        if (currentToken != null && currentToken.getName().equals(";")) {
            nextToken(); // Consome ';'
        }

        String dataLabel = "str" + stringCount++;
        String lineEnding = breakLine ? ", 13, 10, 0" : ", 0"; // LF, CR para nova linha
        dataSection.append(String.format("    %-15s db \"%s\"%s\n", dataLabel, formatStr.toString(), lineEnding));

        codeSection.append("    invoke crt_printf, addr ").append(dataLabel);
        for (String arg : args) {
            codeSection.append(", ").append(arg);
        }
        codeSection.append("\n");
    }

    private void identifyRead() {
        nextToken(); // Consome 'readln'
        nextToken(); // Consome ','
        String variableName = currentToken.getName();
        String varType = symbolsTable.getSymbolType(variableName);
        if (varType == null) varType = "int"; // Fallback, mas deveria estar na tabela

        if (varType.equalsIgnoreCase("int") || varType.equalsIgnoreCase("byte")) {
            // Declara format_d se ainda não foi declarada
            if (!formatDSDeclared) {
                dataSection.append(String.format("    %-15s db \"%%d\", 0\n", "format_d"));
                formatDSDeclared = true;
            }
            codeSection.append("    invoke crt_scanf, addr format_d, addr ").append(variableName).append("\n");
        } else if (varType.equalsIgnoreCase("boolean")) {
            // Para boolean, também lemos como int (0 ou 1)
            if (!formatDSDeclared) {
                dataSection.append(String.format("    %-15s db \"%%d\", 0\n", "format_d"));
                formatDSDeclared = true;
            }
            codeSection.append("    invoke crt_scanf, addr format_d, addr ").append(variableName).append("\n");
        }
        else { // Assume string
            codeSection.append("    invoke crt_gets, addr ").append(variableName).append("\n");
        }

        nextToken(); // Consome nome da variável
        if (currentToken != null && currentToken.getName().equals(";")) {
            nextToken(); // Consome ';'
        }
    }

    private void identifyWhile() {
        int localLoopCounter = loopCounter++;
        String loopLabel = "_loop" + localLoopCounter;
        String loopEndLabel = "_fimLoop" + localLoopCounter;

        codeSection.append("\n").append(loopLabel).append(":\n");
        nextToken(); // Consome 'while'

        String controlVarOrExprStartName = currentToken.getName();
        String controlVarOrExprType = symbolsTable.getSymbolType(controlVarOrExprStartName);

        if(controlVarOrExprType != null && controlVarOrExprType.equalsIgnoreCase("boolean") &&
                (currentTokenIndex + 1 < symbolsTable.getSize() && symbolsTable.currentToken(currentTokenIndex + 1).getName().equalsIgnoreCase("begin")) ){
            // Condição é uma variável booleana simples: while varBooleana begin
            codeSection.append("    mov al, ").append(controlVarOrExprStartName).append("\n");
            codeSection.append("    cmp al, 1\n"); // Compara com true (1)
            codeSection.append("    jne ").append(loopEndLabel).append("\n\n"); // Pula se não for true
            nextToken(); // Consome a variável de controle
        } else {
            // Condição é uma expressão relacional: while n < MAXITER begin
            // O targetLabel é para onde pular se a CONDIÇÃO FOR FALSA.
            generateConditionalExpression(loopEndLabel, true); // true = jumpIfFalse
            // generateConditionalExpression consome os tokens da expressão (ID op ID)
        }

        nextToken(); // Consome 'begin'

        while (currentToken != null && !currentToken.getName().equalsIgnoreCase("end")) {
            identifyCommands();
        }

        if (currentToken != null && currentToken.getName().equalsIgnoreCase("end")) {
            nextToken(); // Consome 'end' do while
        }

        codeSection.append("\n    jmp ").append(loopLabel).append("\n");
        codeSection.append(loopEndLabel).append(":\n");
    }

    private void identifyIf() {
        int localIfCounter = ifCounter++;
        String elseLabel = "_else" + localIfCounter;
        String endIfLabel = "_fimIf" + localIfCounter;
        nextToken(); // Consome 'if'

        // A expressão condicional. Pula para 'elseLabel' se a condição for FALSA.
        generateConditionalExpression(elseLabel, true); // true = jumpIfFalse
        // generateConditionalExpression consome os tokens da expressão (ID op ID ou ID)

        nextToken(); // Consome 'begin' (do bloco 'if')

        while (currentToken != null && !currentToken.getName().equalsIgnoreCase("end") &&
                (currentToken != null && !currentToken.getName().equalsIgnoreCase("else"))) {
            identifyCommands();
        }

        // Consome 'end' do bloco if (se não houver else e 'end' for o token atual)
        if (currentToken != null && currentToken.getName().equalsIgnoreCase("end") &&
                (currentTokenIndex +1 >= symbolsTable.getSize() || // Se for o ultimo token
                        (!symbolsTable.currentToken(currentTokenIndex+1).getName().equalsIgnoreCase("else")) ) // ou se o proximo nao for else
        ) {
            nextToken();
        }


        // Verifica se existe um bloco 'else'
        if (currentToken != null && currentToken.getName().equalsIgnoreCase("else")) {
            codeSection.append("    jmp ").append(endIfLabel).append("\n"); // Pula o bloco 'else' se o 'if' foi executado
            codeSection.append(elseLabel).append(":\n"); // Label para o início do bloco 'else'
            nextToken(); // Consome 'else'
            nextToken(); // Consome 'begin' (do bloco 'else')

            while (currentToken != null && !currentToken.getName().equalsIgnoreCase("end")) {
                identifyCommands();
            }
            if (currentToken != null && currentToken.getName().equalsIgnoreCase("end")) {
                nextToken(); // Consome 'end' do bloco 'else'
            }
            codeSection.append(endIfLabel).append(":\n"); // Label para o fim do 'if-else'
        } else {
            // Se não houver 'else', o 'elseLabel' é onde o código continua após o 'if'
            codeSection.append(elseLabel).append(":\n");
        }
    }

    private void generateConditionalExpression(String targetLabel, boolean jumpIfConditionFalse) {
        Token firstOperandToken = currentToken;
        String firstOperand = currentToken.getName();
        String firstOperandType = symbolsTable.getSymbolType(firstOperand);

        if (firstOperandType == null) { // Pode ser um literal como 10, "true", "Fh"
            if (firstOperand.matches("\\d+")) firstOperandType = "int";
            else if (firstOperand.equalsIgnoreCase("true") || firstOperand.equalsIgnoreCase("false") ||
                    firstOperand.equalsIgnoreCase("Fh") || firstOperand.equalsIgnoreCase("0h")) {
                firstOperandType = "boolean";
            } else {
                firstOperandType = "int"; // Default fallback, pode precisar de ajuste
            }
        }

        nextToken(); // Avança para o operador ou 'begin'

        if (currentToken != null && currentToken.getName().equalsIgnoreCase("begin")) {
            // Condição é uma variável/literal booleano sozinho: if varBooleana begin OR if true begin
            String reg = "al"; // Booleanos são bytes
            String valueToCompare = firstOperand;
            if (firstOperandType.equalsIgnoreCase("boolean")) {
                if (firstOperand.equalsIgnoreCase("true") || firstOperand.equalsIgnoreCase("Fh")) valueToCompare = "1";
                else if (firstOperand.equalsIgnoreCase("false") || firstOperand.equalsIgnoreCase("0h")) valueToCompare = "0";
                // Se for um ID booleano, 'valueToCompare' já é o nome da variável
            }
            // Se valueToCompare for "1" ou "0" (literal), não precisa de mov.
            // Se for um ID, precisa do mov.
            if (symbolsTable.getSymbolType(firstOperand) != null) { // É um ID
                codeSection.append("    mov ").append(reg).append(", ").append(firstOperand).append("\n");
                codeSection.append("    cmp ").append(reg).append(", 1\n"); // Compara ID com true (1)
            } else { // É um literal "true", "false", "Fh", "0h"
                codeSection.append("    cmp byte ptr ").append(valueToCompare).append(", 1\n"); // Compara literal com true (1)
            }

            String jumpInstruction = jumpIfConditionFalse ? "jne" : "je";
            codeSection.append("    ").append(jumpInstruction).append(" ").append(targetLabel).append("\n");
            // currentToken está em 'begin', será consumido pelo chamador (identifyIf/identifyWhile)
            return;
        }

        String operator = currentToken.getName();
        nextToken(); // Avança para o segundo operando

        String secondOperand = currentToken.getName();
        // String secondOperandType = symbolsTable.getSymbolType(secondOperand); // Para consistência, mas não usado diretamente abaixo

        String reg = "eax"; // Default para int
        if (firstOperandType.equalsIgnoreCase("boolean") || firstOperandType.equalsIgnoreCase("byte")) {
            reg = "al";
        }

        String formattedFirstOperand = firstOperand;
        if (firstOperandType.equalsIgnoreCase("boolean")) {
            if (firstOperand.equalsIgnoreCase("true") || firstOperand.equalsIgnoreCase("Fh")) formattedFirstOperand = "1";
            else if (firstOperand.equalsIgnoreCase("false") || firstOperand.equalsIgnoreCase("0h")) formattedFirstOperand = "0";
        }

        String formattedSecondOperand = secondOperand;
        // Se o segundo operando for um literal booleano, converta para 1 ou 0
        if (secondOperand.equalsIgnoreCase("true") || secondOperand.equalsIgnoreCase("Fh")) formattedSecondOperand = "1";
        else if (secondOperand.equalsIgnoreCase("false") || secondOperand.equalsIgnoreCase("0h")) formattedSecondOperand = "0";


        // Carrega o primeiro operando (pode ser var ou literal convertido)
        if (symbolsTable.getSymbolType(firstOperand) != null || firstOperandType.matches("int|byte|boolean")) { // Se for ID ou tipo conhecido
            codeSection.append("    mov ").append(reg).append(", ").append(formattedFirstOperand).append("\n");
        } else { // Se for um literal numérico não formatado acima (improvável aqui)
            codeSection.append("    mov ").append(reg).append(", ").append(firstOperand).append("\n");
        }

        // Compara com o segundo operando (pode ser var ou literal convertido)
        // Se o segundo operando é um ID, usa seu nome. Se é literal, usa o valor formatado.
        if (symbolsTable.getSymbolType(secondOperand) != null) {
            codeSection.append("    cmp ").append(reg).append(", ").append(secondOperand).append("\n");
        } else { // É um literal (numérico ou booleano formatado para 0/1)
            codeSection.append("    cmp ").append(reg).append(", ").append(formattedSecondOperand).append("\n");
        }

        String jumpInstruction = getJumpInstruction(operator, jumpIfConditionFalse);
        codeSection.append("    ").append(jumpInstruction).append(" ").append(targetLabel).append("\n");
        //nextToken(); // Consome o segundo operando. O chamador (identifyIf/While) consumirá o 'begin'.
    }

    private String getJumpInstruction(String operator, boolean jumpIfConditionFalse) {
        return switch (operator) {
            case "==" -> jumpIfConditionFalse ? "jne" : "je";
            case "<>" -> jumpIfConditionFalse ? "je" : "jne";
            case "<"  -> jumpIfConditionFalse ? "jge" : "jl";
            case ">"  -> jumpIfConditionFalse ? "jle" : "jg";
            case "<=" -> jumpIfConditionFalse ? "jg" : "jle";
            case ">=" -> jumpIfConditionFalse ? "jl" : "jge";
            default -> "";
        };
    }

    private void identifyAssignment() {
        String variableName = currentToken.getName();
        String varType = symbolsTable.getSymbolType(variableName);
        if (varType == null) varType = "int"; // Fallback

        nextToken(); // Consome nome da variável (lado esquerdo)
        nextToken(); // Consome '='

        if (varType.equalsIgnoreCase("string")) {
            String stringLiteral = currentToken.getName();
            String actualStringValue = stringLiteral.substring(1, stringLiteral.length() - 1);
            String stringLabelInData = "str_assign_" + actualStringValue.replaceAll("[^a-zA-Z0-9_]", "") + stringCount++;
            dataSection.append(String.format("    %-15s db \"%s\", 0\n", stringLabelInData, actualStringValue));
            codeSection.append("    invoke crt_strcpy, addr ").append(variableName).append(", addr ").append(stringLabelInData).append("\n");
            nextToken();
        } else {
            ArrayList<Token> expressionTokens = new ArrayList<>();
            while (currentToken != null && !currentToken.getName().equals(";")) {
                expressionTokens.add(currentToken);
                nextToken();
            }
            evaluateExpression(expressionTokens);

            codeSection.append("    pop eax\n");
            if (varType.equalsIgnoreCase("boolean") || varType.equalsIgnoreCase("byte")) {
                codeSection.append("    mov ").append(variableName).append(", al\n");
            } else {
                codeSection.append("    mov ").append(variableName).append(", eax\n");
            }
        }

        if (currentToken != null && currentToken.getName().equals(";")) {
            nextToken(); // Consome ';'
        }
    }

    private void evaluateExpression(ArrayList<Token> tokens) {
        Stack<String> ops = new Stack<>();

        for (Token token : tokens) {
            String name = token.getName();
            String classification = token.getClassification();

            // Handle specific boolean literal representations from lexer first
            if (name.equalsIgnoreCase("Fh")) { // Lexer's token name for true
                codeSection.append("    push 1\n");
            } else if (name.equalsIgnoreCase("0h")) { // Lexer's token name for false
                codeSection.append("    push 0\n");
            }
            // Handle canonical "true"/"false" if lexer might produce them too,
            // or for other IDs/CONSTs (numeric)
            else if (classification.matches("ID") || classification.matches("CONST") ||
                    name.equalsIgnoreCase("true") || name.equalsIgnoreCase("false")) {
                String valueToPush = name;
                if(name.equalsIgnoreCase("true")) {
                    valueToPush = "1";
                } else if (name.equalsIgnoreCase("false")) {
                    valueToPush = "0";
                }
                // If 'name' is an ID (e.g., 'myIntVar') or a numeric CONST (e.g., '123'),
                // valueToPush remains 'name' and is pushed directly. This is correct.
                codeSection.append("    push ").append(valueToPush).append("\n");
            } else if (name.equals("(")) {
                ops.push(name);
            } else if (name.equals(")")) {
                while (!ops.empty() && !ops.peek().equals("(")) {
                    generateOp(ops.pop());
                }
                if (!ops.empty()) ops.pop(); // Remove "("
            } else if (isOperator(name)) { // +, -, *, /
                while (!ops.empty() && hasPrecedence(ops.peek(), name)) {
                    generateOp(ops.pop());
                }
                ops.push(name);
            }
        }
        while (!ops.empty()) {
            if (ops.peek().equals("(")) {
                ops.pop();
                continue;
            }
            generateOp(ops.pop());
        }
    }

    private void generateOp(String op) {
        codeSection.append("    pop ebx\n");
        codeSection.append("    pop eax\n");
        switch (op) {
            case "+" -> codeSection.append("    add eax, ebx\n");
            case "-" -> codeSection.append("    sub eax, ebx\n");
            case "*" -> codeSection.append("    imul eax, ebx\n");
            case "/" -> {
                codeSection.append("    cdq\n");
                codeSection.append("    idiv ebx\n");
            }
        }
        codeSection.append("    push eax\n");
    }

    private boolean isOperator(String op) {
        return op.matches("[+\\-*/]");
    }

    private boolean hasPrecedence(String op1, String op2) {
        if (op1.equals("(") || op1.equals(")")) return false;
        if ((op1.equals("*") || op1.equals("/")) && (op2.equals("+") || op2.equals("-"))) return true;
        if ((op1.equals("*") || op1.equals("/")) && (op2.equals("*") || op2.equals("/"))) return true;
        if ((op1.equals("+") || op1.equals("-")) && (op2.equals("+") || op2.equals("-"))) return true;
        return false;
    }
}