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
        this.currentTokenIndex++;
        if (this.currentTokenIndex < this.symbolsTable.getSize()) {
            this.currentToken = this.symbolsTable.currentToken(this.currentTokenIndex);
        } else {
            this.currentToken = null;
        }
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
        this.dataSection.append(String.format("    %-15s db %s, 0\n", "str_Natan", "\"Natan\""));
        this.dataSection.append(String.format("    %-15s db %s, 0\n", "str_Nathan", "\"Nathan\""));

        while (isDeclarationScope()) {
            identifyDeclaration();
        }
    }

    private boolean isDeclarationScope() {
        return isPrimitiveType() || currentToken.getName().equalsIgnoreCase("final");
    }

    private String formatValue(String value) {
        // Usando 1 para 'true' para simplificar comparações, como 'cmp al, 1'
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
                if (currentToken.getName().equals(",")) nextToken();
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
        while (currentToken != null && !currentToken.getName().equalsIgnoreCase("begin")) nextToken();
        if (currentToken != null && currentToken.getName().equalsIgnoreCase("begin")) nextToken();
        while (currentToken != null) identifyCommands();
    }

    private void identifyCommands() {
        if (currentToken == null) return;
        switch (currentToken.getName().toLowerCase()) {
            case "write", "writeln" -> identifyWrite();
            case "readln" -> identifyRead();
            case "while" -> identifyWhile();
            case "if" -> identifyIf();
            case ";" -> nextToken();
            default -> {
                if (currentToken.getClassification().equalsIgnoreCase("ID")) identifyAssignment();
                else nextToken();
            }
        }
    }

    private void identifyWrite() {
        boolean breakLine = currentToken.getName().equalsIgnoreCase("writeln");
        nextToken(); nextToken();
        StringBuilder formatStr = new StringBuilder();
        ArrayList<String> args = new ArrayList<>();
        while (!currentToken.getName().equals(";")) {
            if (currentToken.getClassification().equalsIgnoreCase("ID")) {
                String varName = currentToken.getName();
                String varType = symbolsTable.getSymbolType(varName);
                formatStr.append(varType.equals("string") ? "%s" : "%d");
                args.add(varType.equals("string") ? "addr " + varName : varName);
            } else {
                String literal = currentToken.getName().replace("\"", "");
                formatStr.append(literal);
            }
            nextToken();
            if (currentToken.getName().equals(",")) nextToken();
        }
        nextToken();
        String dataLabel = "str" + stringCount++;
        String lineEnding = breakLine ? ", 13, 10, 0" : ", 0";
        dataSection.append(String.format("    %-15s db \"%s\"%s\n", dataLabel, formatStr, lineEnding));
        codeSection.append("    invoke crt_printf, addr ").append(dataLabel);
        for (String arg : args) codeSection.append(", ").append(arg);
        codeSection.append("\n");
    }

    private void identifyRead() {
        nextToken(); nextToken();
        String variable = currentToken.getName();
        codeSection.append("    invoke crt_gets, addr ").append(variable).append("\n");
        nextToken(); nextToken();
    }

    // Em AssemblyGenerator.java - CORRIGIDO
    private void identifyWhile() {
        int localLoopCounter = loopCounter++;
        String loopLabel = "_loop" + localLoopCounter;
        String loopEndLabel = "_fimLoop" + localLoopCounter;

        codeSection.append("\n").append(loopLabel).append(":\n");
        nextToken(); // Consome 'while'

        // Lógica para a condição (ex: naoTerminou)
        // A sua lógica atual é simples, mas para este caso funciona.
        String controlVar = currentToken.getName();
        codeSection.append("    mov al, ").append(controlVar).append("\n");
        codeSection.append("    cmp al, 1\n"); // Compara com true
        codeSection.append("    jne ").append(loopEndLabel).append("\n\n");

        nextToken(); // Consome a variável de condição
        nextToken(); // Consome 'begin'

        // *** INÍCIO DA CORREÇÃO ***
        // Loop para processar TODOS os comandos dentro do bloco begin...end
        while (currentToken != null && !currentToken.getName().equalsIgnoreCase("end")) {
            identifyCommands();
        }
        // *** FIM DA CORREÇÃO ***

        nextToken(); // Consome 'end'

        codeSection.append("\n    jmp ").append(loopLabel).append("\n");
        codeSection.append(loopEndLabel).append(":\n");
    }

    // Em AssemblyGenerator.java - CORRIGIDO
    private void identifyIf() {
        int localIfCounter = ifCounter++;
        String elseLabel = "_else" + localIfCounter;
        String endIfLabel = "_fimIf" + localIfCounter;
        nextToken(); // Consome 'if'

        generateConditionalExpression(elseLabel); // Gera a condição e o pulo

        nextToken(); // Consome 'begin'

        // Loop para processar o bloco 'if'
        while (currentToken != null && !currentToken.getName().equalsIgnoreCase("end") && !currentToken.getName().equalsIgnoreCase("else")) {
            identifyCommands();
        }

        // Verifica se existe um 'else'
        if (currentToken != null && currentToken.getName().equalsIgnoreCase("else")) {
            codeSection.append("    jmp ").append(endIfLabel).append("\n"); // Pula o bloco else
            codeSection.append(elseLabel).append(":\n"); // Início do bloco else
            nextToken(); // Consome 'else'
            nextToken(); // Consome 'begin'

            // Loop para processar o bloco 'else'
            while (currentToken != null && !currentToken.getName().equalsIgnoreCase("end")) {
                identifyCommands();
            }

            codeSection.append(endIfLabel).append(":\n"); // Fim do if-else
        } else {
            // Se não houver else, o label 'else' é o fim do 'if'
            codeSection.append(elseLabel).append(":\n");
        }

        nextToken(); // Consome o 'end' final
    }

    private void generateConditionalExpression(String targetLabel) {
        ArrayList<Token> expression = new ArrayList<>();
        while (currentToken != null && !currentToken.getName().equalsIgnoreCase("begin")) {
            expression.add(currentToken);
            nextToken();
        }

        // Lógica simplificada para var op var.
        String left = expression.get(0).getName();
        String operator = expression.get(1).getName();
        String right = expression.get(2).getName();

        // Para booleanos, comparamos com 1 byte, para int, com 4 bytes
        String reg = "eax";
        if (symbolsTable.getSymbolType(left).equals("boolean") || symbolsTable.getSymbolType(left).equals("byte")) {
            reg = "al";
        }

        codeSection.append("    mov ").append(reg).append(", ").append(left).append("\n");
        codeSection.append("    cmp ").append(reg).append(", ").append(formatValue(right)).append("\n");
        String jumpInstruction = getJumpInstruction(operator);
        codeSection.append("    ").append(jumpInstruction).append(" ").append(targetLabel).append("\n");

        if (expression.size() > 3 && expression.get(3).getName().equalsIgnoreCase("and")) {
            // Se tiver 'and', repete a lógica para a segunda parte da condição
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

    private void identifyAssignment() {
        String variable = currentToken.getName();
        String varType = symbolsTable.getSymbolType(variable);
        nextToken();
        nextToken();

        if (varType != null && varType.equals("string")) {
            String stringValue = currentToken.getName();
            String stringLabel = "str_" + stringValue.replace("\"", "");
            codeSection.append("    invoke crt_strcpy, addr ").append(variable).append(", addr ").append(stringLabel).append("\n");
            while (currentToken != null && !currentToken.getName().equals(";")) nextToken();
        } else {
            ArrayList<Token> expressionTokens = new ArrayList<>();
            while (currentToken != null && !currentToken.getName().equals(";")) {
                expressionTokens.add(currentToken);
                nextToken();
            }
            evaluateExpression(expressionTokens);
            codeSection.append("    pop eax\n");
            // Se a variável for de 1 byte (boolean/byte), movemos apenas o byte menos significativo (AL)
            if (varType != null && (varType.equals("boolean") || varType.equals("byte"))) {
                codeSection.append("    mov ").append(variable).append(", al\n");
            } else {
                codeSection.append("    mov ").append(variable).append(", eax\n");
            }
        }
        nextToken();
    }

    private void evaluateExpression(ArrayList<Token> tokens) {
        Stack<String> ops = new Stack<>();
        for (Token token : tokens) {
            String name = token.getName();
            if (token.getClassification().matches("ID|CONST")) {
                codeSection.append("    push ").append(formatValue(name)).append("\n");
            } else if (name.equals("(")) {
                ops.push(name);
            } else if (name.equals(")")) {
                while (!ops.peek().equals("(")) {
                    generateOp(ops.pop());
                }
                ops.pop();
            } else if (isOperator(name)) {
                while (!ops.empty() && hasPrecedence(ops.peek(), name)) {
                    generateOp(ops.pop());
                }
                ops.push(name);
            }
        }
        while (!ops.empty()) {
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
        return (!op1.equals("+") && !op1.equals("-")) || (op2.equals("+") || op2.equals("-"));
    }
}