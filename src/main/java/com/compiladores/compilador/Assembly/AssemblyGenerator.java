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

    public AssemblyGenerator(SymbolsTable symbolsTable, String fileName) {
        this.symbolsTable = symbolsTable;
        this.fileName = fileName + ".asm";
        this.currentToken = symbolsTable.currentToken(this.currentTokenIndex);
    }

    private void nextToken() {
        this.currentToken = this.symbolsTable.currentToken(++this.currentTokenIndex);
    }

    private void createOutDirectory() {
        File outDir = new File(path);
        if (!outDir.exists()) {
            outDir.mkdirs();
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
        generateDataSection();
        generateCodeSection();

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
        // Adiciona strings que serão usadas para atribuição
        this.dataSection.append(String.format("    %-15s db %s, 0\n", "str_Natan", "\"Natan\""));
        this.dataSection.append(String.format("    %-15s db %s, 0\n", "str_Nathan", "\"Nathan\""));

        while (isDeclarationScope()) {
            this.identifyDeclaration();
        }
    }

    private boolean isDeclarationScope() {
        if (currentToken == null || currentToken.getName().equalsIgnoreCase("{")) {
            return false;
        }
        return isPrimitiveType() || currentToken.getName().equalsIgnoreCase("final");
    }

    private String formatValue(String value) {
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("Fh")) return "1";
        if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("0h")) return "0";
        if (value.toLowerCase().startsWith("0h")) return value.substring(2) + "h";
        return value;
    }

    private void identifyDeclaration() {
        if (currentToken.getName().equalsIgnoreCase("final")) {
            nextToken();
            String constName = currentToken.getName();
            nextToken();
            nextToken();
            String constValue = formatValue(currentToken.getName());
            dataSection.append(String.format("    %-15s equ %s\n", constName, constValue));
            nextToken();
            nextToken();
        } else {
            String type = currentToken.getName();
            String dataTypeMASM = primitiveTypeMASM(type);
            nextToken();
            while (currentToken != null && !currentToken.getName().equals(";")) {
                String dataName = currentToken.getName();
                nextToken();
                String dataValue = "0";
                if (currentToken.getName().equals("=")) {
                    nextToken();
                    dataValue = formatValue(currentToken.getName());
                    nextToken();
                }
                if (type.equalsIgnoreCase("string")) {
                    dataSection.append(String.format("    %-15s db 256 dup(0)\n", dataName));
                } else {
                    dataSection.append(String.format("    %-15s %-5s %s\n", dataName, dataTypeMASM, dataValue));
                }
                if (currentToken.getName().equals(",")) {
                    nextToken();
                }
            }
            nextToken();
        }
    }

    private String primitiveTypeMASM(String type) {
        return switch (type) {
            case "int" -> "dd";
            case "boolean", "byte" -> "db";
            default -> "";
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
        beginGeneration();
        codeSection.append("\n    invoke ExitProcess, 0\n").append("end start\n");
    }

    private void beginGeneration() {
        while (currentToken != null && !currentToken.getName().equalsIgnoreCase("begin")) {
            nextToken();
        }
        if (currentToken != null && currentToken.getName().equalsIgnoreCase("begin")) {
            nextToken();
            identifyCommands();
        }
    }

    private void identifyCommands() {
        while (currentToken != null && !currentToken.getName().equalsIgnoreCase("end") && !currentToken.getName().equalsIgnoreCase("else")) {
            if (currentToken == null) break;
            switch (currentToken.getName().toLowerCase()) {
                case "write", "writeln" -> identifyWrite();
                case "readln" -> identifyRead();
                case "while" -> identifyWhile();
                case "if" -> identifyIf();
                case ";" -> nextToken(); // Ignora comando nulo
                default -> {
                    if (currentToken.getClassification().equalsIgnoreCase("ID")) {
                        identifyAssignment();
                    } else {
                        nextToken();
                    }
                }
            }
        }
    }

    private void identifyWrite() {
        boolean breakLine = currentToken.getName().equalsIgnoreCase("writeln");
        nextToken();
        nextToken();

        StringBuilder formatStr = new StringBuilder();
        ArrayList<String> args = new ArrayList<>();

        while (!currentToken.getName().equalsIgnoreCase(";")) {
            if (currentToken.getClassification().equalsIgnoreCase("ID")) {
                String varName = currentToken.getName();
                // Assumindo que o tipo está no token, como no seu ajuste.
                // Se não, precisaria buscar na tabela de símbolos.
                String varType = this.currentToken.getType();

                if (varType != null && varType.equals("string")) {
                    formatStr.append("%s");
                } else {
                    formatStr.append("%d");
                }
                args.add(varName);
            } else {
                String literal = currentToken.getName().replace("'", "").replace("\"", "");
                formatStr.append(literal);
            }
            nextToken();
            if (currentToken.getName().equals(",")) {
                nextToken();
            }
        }
        nextToken();

        String dataLabel = "str" + stringCount++;
        String lineEnding = breakLine ? ", 13, 10, 0" : ", 0";
        dataSection.append(String.format("    %-15s db \"%s\"%s\n", dataLabel, formatStr, lineEnding));
        codeSection.append("    invoke crt_printf, addr ").append(dataLabel);
        for (String arg : args) {
            codeSection.append(", ").append(arg);
        }
        codeSection.append("\n");
    }

    private void identifyRead() {
        nextToken();
        nextToken();
        String variable = currentToken.getName();
        codeSection.append("    invoke crt_gets, addr ").append(variable).append("\n");
        nextToken();
        nextToken();
    }

    private void identifyWhile() {
        int localLoopCounter = loopCounter++;
        String loopLabel = "_loop" + localLoopCounter;
        String loopEndLabel = "_fimLoop" + localLoopCounter;

        codeSection.append("\n").append(loopLabel).append(":\n");
        nextToken();

        String controlVar = currentToken.getName();
        codeSection.append("    mov al, ").append(controlVar).append("\n");
        codeSection.append("    cmp al, 1\n");
        codeSection.append("    jne ").append(loopEndLabel).append("\n\n");

        nextToken();
        nextToken();
        identifyCommands();
        nextToken();

        codeSection.append("\n    jmp ").append(loopLabel).append("\n");
        codeSection.append(loopEndLabel).append(":\n");
    }

    private void identifyIf() {
        int localIfCounter = ifCounter++;
        String elseLabel = "_else" + localIfCounter;
        String endIfLabel = "_fimIf" + localIfCounter;
        nextToken();
        generateConditionalExpression(elseLabel);
        nextToken();
        identifyCommands();

        if (currentToken != null && currentToken.getName().equalsIgnoreCase("else")) {
            codeSection.append("    jmp ").append(endIfLabel).append("\n");
            codeSection.append(elseLabel).append(":\n");
            nextToken();
            nextToken();
            identifyCommands();
            codeSection.append(endIfLabel).append(":\n");
        } else {
            codeSection.append(elseLabel).append(":\n");
        }
        nextToken();
    }

    private void generateConditionalExpression(String targetLabel) {
        String left = currentToken.getName();
        nextToken();
        String operator = currentToken.getName();
        nextToken();
        String right = currentToken.getName();
        nextToken();

        codeSection.append("    mov eax, ").append(left).append("\n");
        codeSection.append("    cmp eax, ").append(right).append("\n");
        String jumpInstruction = getJumpInstruction(operator);
        codeSection.append("    ").append(jumpInstruction).append(" ").append(targetLabel).append("\n");

        if (currentToken.getName().equalsIgnoreCase("and")) {
            nextToken();
            left = currentToken.getName();
            nextToken();
            operator = currentToken.getName();
            nextToken();
            right = currentToken.getName();
            nextToken();

            codeSection.append("    mov eax, ").append(left).append("\n");
            codeSection.append("    cmp eax, ").append(right).append("\n");
            jumpInstruction = getJumpInstruction(operator);
            codeSection.append("    ").append(jumpInstruction).append(" ").append(targetLabel).append("\n");
        }
    }

    private String getJumpInstruction(String operator) {
        return switch (operator) {
            case "==" -> "jne";
            case "<>" -> "je";
            case "<" -> "jge";
            case ">" -> "jle";
            case "<=" -> "jg";
            case ">=" -> "jl";
            default -> "";
        };
    }

    // --- O NOVO E MELHORADO IDENTIFYASSIGNMENT ---
    private void identifyAssignment() {
        String variable = currentToken.getName();
        String varType = this.currentToken.getType();
        nextToken();
        nextToken();

        // Tratamento especial para atribuição de string
        if (varType != null && varType.equals("string")) {
            String stringValue = currentToken.getName();
            String stringLabel = "str_" + stringValue.replace("\"", "");
            codeSection.append("    invoke crt_strcpy, addr ").append(variable).append(", addr ").append(stringLabel).append("\n");
            while (currentToken != null && !currentToken.getName().equals(";")) nextToken();
        } else {
            // Avalia a expressão aritmética/lógica até encontrar o ';'
            ArrayList<Token> expressionTokens = new ArrayList<>();
            while (currentToken != null && !currentToken.getName().equals(";")) {
                expressionTokens.add(currentToken);
                nextToken();
            }
            evaluateExpression(expressionTokens);
            // O resultado da expressão está em EAX, agora movemos para a variável
            codeSection.append("    mov ").append(variable).append(", eax\n");
        }
        nextToken(); // Pula o ';'
    }

    // Função para avaliar uma expressão usando pilhas para respeitar a precedência
    private void evaluateExpression(ArrayList<Token> tokens) {
        Stack<String> values = new Stack<>();
        Stack<String> ops = new Stack<>();

        for (Token token : tokens) {
            String name = token.getName();
            if (token.getClassification().matches("ID|Número")) {
                values.push(formatValue(name));
            } else if (name.equals("(")) {
                ops.push(name);
            } else if (name.equals(")")) {
                while (!ops.peek().equals("(")) {
                    generateOp(ops.pop(), values);
                }
                ops.pop(); // Descarta o '('
            } else if (isOperator(name)) {
                while (!ops.empty() && hasPrecedence(ops.peek(), name)) {
                    generateOp(ops.pop(), values);
                }
                ops.push(name);
            }
        }

        while (!ops.empty()) {
            generateOp(ops.pop(), values);
        }

        // No final, o resultado da expressão deve estar no topo da pilha de valores
        codeSection.append("    pop eax\n");
    }

    // Gera o código para uma operação (ex: ADD, IMUL)
    private void generateOp(String op, Stack<String> values) {
        codeSection.append("    pop ebx\n"); // operando 2
        codeSection.append("    pop eax\n"); // operando 1
        switch (op) {
            case "+" -> codeSection.append("    add eax, ebx\n");
            case "-" -> codeSection.append("    sub eax, ebx\n");
            case "*" -> codeSection.append("    imul eax, ebx\n");
            case "/" -> {
                codeSection.append("    cdq\n"); // Limpa EDX para a divisão
                codeSection.append("    idiv ebx\n");
            }
        }
        codeSection.append("    push eax\n"); // Empilha o resultado
    }

    private boolean isOperator(String op) {
        return op.matches("[+\\-*/]");
    }

    // Verifica se op1 tem precedência sobre op2
    private boolean hasPrecedence(String op1, String op2) {
        if (op1.equals("(") || op1.equals(")")) return false;
        if ((op1.equals("*") || op1.equals("/")) && (op2.equals("+") || op2.equals("-"))) return true;
        return false;
    }
}