package com.compiladores.compilador.Assembly;

import com.compiladores.compilador.Lexical.Token;
import com.compiladores.compilador.Table.SymbolsTable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

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

    private String generateHeader() {
        return this.headerSection.append(".686\n")
                .append(".model flat, stdcall\n")
                .append("option casemap :none\n\n")
                .append("include \\masm32\\include\\windows.inc\n")
                .append("include \\masm32\\include\\kernel32.inc\n")
                .append("include \\masm32\\include\\masm32.inc\n")
                .append("include \\masm32\\include\\msvcrt.inc\n")
                .append("includelib \\masm32\\lib\\kernel32.lib\n")
                .append("includelib \\masm32\\lib\\masm32.lib\n")
                .append("includelib \\masm32\\lib\\msvcrt.lib\n")
                .append("include \\masm32\\macros\\macros.asm\n\n")
                .toString();
    }

    private String generateDataSection() {
        this.dataSection.append(".data\n");
        while (isDeclarationScope()) {
            this.identifyDeclaration();
        }
        return dataSection.toString();
    }

    private boolean isDeclarationScope() {
        if (currentToken == null || currentToken.getName().equalsIgnoreCase("{")) {
            return false;
        }
        return isPrimitiveType() || currentToken.getName().equalsIgnoreCase("final");
    }

    private void identifyDeclaration() {
        String type = this.currentToken.getName();
        String dataTypeMASM = this.primitiveTypeMASM(type);
        this.nextToken();
        String dataName = this.currentToken.getName();
        this.nextToken();

        String dataValue = "";

        if (this.currentToken.getName().equals("=")) {
            this.nextToken();
            dataValue = this.currentToken.getName();
            this.nextToken();
        }

        if (dataValue.equalsIgnoreCase("Fh")) {
            dataValue = "1";
        } else if (dataValue.equalsIgnoreCase("0h")) {
            dataValue = "0";
        }

        if (dataValue.isEmpty()) {
            dataValue = "0";
        }

        // CORREÇÃO APLICADA AQUI
        if (dataTypeMASM.equals("equ")) {
            // Usar a variável 'dataValue' que guardou o valor da constante
            this.dataSection.append(String.format("    %-15s %-5s %s\n", dataName, dataTypeMASM, dataValue));
        } else if (type.equalsIgnoreCase("string")) {
            this.dataSection.append(String.format("    %-15s %-5s %s\n", dataName, "db 256", "dup(0)"));
        } else {
            this.dataSection.append(String.format("    %-15s %-5s %s\n", dataName, dataTypeMASM, dataValue));
        }

        this.nextToken();
    }

    private String primitiveTypeMASM(String type) {
        return switch (type) {
            case "int" -> "dd";
            case "boolean" -> "db";
            case "final" -> "equ";
            default -> "";
        };
    }

    private boolean isPrimitiveType() {
        return (this.currentToken.getName().equalsIgnoreCase("int") ||
                this.currentToken.getName().equalsIgnoreCase("string") ||
                this.currentToken.getName().equalsIgnoreCase("boolean"));
    }

    private String generateCodeSection() {
        this.codeSection.append(".code\n").append("start:\n");
        this.beginGeneration();
        return this.codeSection.append("\n    invoke ExitProcess, 0\n").append("end start\n").toString();
    }

    private void beginGeneration() {
        while (currentToken != null && !currentToken.getName().equalsIgnoreCase("begin")) {
            nextToken();
        }
        if (currentToken != null && currentToken.getName().equalsIgnoreCase("begin")) {
            this.nextToken();
            this.identifyCommands();
        }
    }

    private void identifyCommands() {
        while (currentToken != null && !currentToken.getName().equalsIgnoreCase("end")) {
            switch (this.currentToken.getName().toLowerCase()) {
                case "write", "writeln" -> this.identifyWrite();
                case "readln" -> this.identifyRead();
                case "while" -> this.identifyWhile();
                case "if" -> this.identifyIf();
                default -> {
                    if (this.currentToken.getClassification().equalsIgnoreCase("ID")) {
                        this.identifyAssignment();
                    } else {
                        this.nextToken();
                    }
                }
            }
        }
    }

    private void identifyWrite() {
        boolean breakLine = this.currentToken.getName().equalsIgnoreCase("writeln");
        this.nextToken();
        this.nextToken();

        StringBuilder formatStr = new StringBuilder("\"");
        ArrayList<String> args = new ArrayList<>();

        while (!this.currentToken.getName().equalsIgnoreCase(";")) {
            if (this.currentToken.getClassification().equalsIgnoreCase("ID")) {
                formatStr.append("%s");
                args.add("addr " + this.currentToken.getName());
            } else {
                String literal = this.currentToken.getName().replace("\"", "");
                formatStr.append(literal);
            }
            this.nextToken();
            if (this.currentToken.getName().equals(",")) {
                this.nextToken();
            }
        }
        this.nextToken();

        formatStr.append("\"");
        String dataLabel = "str" + stringCount++;
        String lineEnding = breakLine ? ", 13, 10, 0\n" : ", 0\n";

        dataSection.append(String.format("    %-15s db %s%s", dataLabel, formatStr, lineEnding));
        codeSection.append("    invoke crt_printf, addr ").append(dataLabel);
        for (String arg : args) {
            codeSection.append(", ").append(arg);
        }
        codeSection.append("\n");
    }

    private void identifyRead() {
        this.nextToken();
        this.nextToken();
        String variable = this.currentToken.getName();
        codeSection.append("    invoke crt_gets, addr ").append(variable).append("\n");
        this.nextToken();
        this.nextToken();
    }

    private void identifyWhile() {
        String loopLabel = "_loop" + loopCounter;
        String loopEndLabel = "_fimLoop" + loopCounter++;

        codeSection.append("\n").append(loopLabel).append(":\n");
        this.nextToken();
        String controlVar = this.currentToken.getName();

        codeSection.append("    cmp ").append(controlVar).append(", 0\n");
        codeSection.append("    je ").append(loopEndLabel).append("\n\n");

        this.nextToken();
        this.nextToken();
        this.identifyCommands();
        this.nextToken();

        codeSection.append("\n    jmp ").append(loopLabel).append("\n");
        codeSection.append(loopEndLabel).append(":\n");
    }

    private void identifyIf() {
        String endIfLabel = "_fimIf" + ifCounter++;

        this.nextToken();
        String left = this.currentToken.getName();
        this.nextToken();
        String operator = this.currentToken.getName();
        this.nextToken();
        String right = this.currentToken.getName();
        this.nextToken();

        codeSection.append("\n    cmp ").append(left).append(", ").append(right).append("\n");

        String jumpInstruction = switch (operator) {
            case "<" -> "jge";
            case ">" -> "jle";
            case "==" -> "jne";
            case "!=" -> "je";
            case "<=" -> "jg";
            case ">=" -> "jl";
            default -> "";
        };

        codeSection.append("    ").append(jumpInstruction).append(" ").append(endIfLabel).append("\n");

        this.nextToken();
        this.identifyCommands();
        this.nextToken();
        codeSection.append(endIfLabel).append(":\n");
    }

    private void identifyAssignment() {
        String variable = this.currentToken.getName();
        this.nextToken();
        this.nextToken();

        String firstValue = this.currentToken.getName();
        this.nextToken();

        if (this.currentToken.getName().equals(";")) {
            String valueToMove = firstValue;
            if (firstValue.equalsIgnoreCase("Fh")) {
                valueToMove = "1";
            } else if (firstValue.equalsIgnoreCase("0h")) {
                valueToMove = "0";
            }
            codeSection.append("    mov ").append(variable).append(", ").append(valueToMove).append("\n");
            this.nextToken();
            return;
        }

        String operator = this.currentToken.getName();
        this.nextToken();
        String secondValue = this.currentToken.getName();

        if (variable.equals(firstValue) && secondValue.equals("1")) {
            if (operator.equals("+")) {
                codeSection.append("    add ").append(variable).append(", 1\n");
                this.nextToken();
                this.nextToken();
                return;
            }
        }

        codeSection.append("    mov eax, ").append(firstValue).append("\n");
        String command = operator.equals("+") ? "add" : "sub";
        codeSection.append("    ").append(command).append(" eax, ").append(secondValue).append("\n");
        codeSection.append("    mov ").append(variable).append(", eax\n");
        this.nextToken();
        this.nextToken();
    }
}