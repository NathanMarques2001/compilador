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
    private StringBuilder dataSection = new StringBuilder();
    private StringBuilder codeSection = new StringBuilder();
    private int stringCount = 1;

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
            boolean created = outDir.mkdirs();
            if (created) {
                System.out.println("Diretório 'out' criado com sucesso!");
            } else {
                System.out.println("Falha ao criar diretório 'out'.");
            }
        }
    }

    private void writeAssemblyCode(String assemblyCode) {
        File outDir = new File(path);
        if (!outDir.exists()) {
            System.out.println("Diretório 'out' não existe.");
            return;
        }

        File asmFile = new File(outDir, fileName);  // usa o fileName passado no construtor
        try (FileWriter writer = new FileWriter(asmFile)) { // FileWriter padrão sobrescreve o arquivo
            writer.write(assemblyCode);
            System.out.println("Código Assembly escrito com sucesso no arquivo '" + fileName + "'.");
        } catch (IOException e) {
            System.out.println("Erro ao escrever no arquivo '" + fileName + "': " + e.getMessage());
        }
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

        this.identifyDeclarations();

        return dataSection.toString();
    }

    private void identifyDeclarations() {
        if (this.isPrimitiveType() || this.currentToken.getName().equalsIgnoreCase("final")) {
            this.identifyDeclaration();
        }
    }

    private void identifyDeclaration() {
        String dataType = this.primitiveTypeMASM();

        this.nextToken();
        String dataName = this.currentToken.getName();

        String dataValue = "";

        this.nextToken();
        if (this.currentToken.getName().equals("=")) {
            this.nextToken();
            dataValue = this.currentToken.getName();

            // byte
            if (dataType.equals("dw")) {
                dataValue = this.formatByteType() + "h";
            }

            this.nextToken();
        }

        if (dataValue.isEmpty()) {
            // int, boolean
            if (dataType.equals("dd") || dataType.equals("db")) {
                dataValue = "0";
                // byte
            } else if (dataType.equals("dw")) {
                dataValue = "0h";
                // string
            } else {
                dataValue = "dup(0)";
            }
        }

        // escreve no string builder
        this.dataSection.append(dataName).append(" ").append(dataType).append(" ").append(dataValue).append("\n");

        // vai para a proxima linha
        this.nextToken();
        this.identifyDeclarations();
    }

    private String primitiveTypeMASM() {
        return switch (this.currentToken.getName()) {
            case "int" -> "dd";
            case "string" -> "db 256";
            case "boolean" -> "db";
            case "byte" -> "dw";
            default -> "equ"; // final
        };
    }

    private boolean isPrimitiveType() {
        return (this.currentToken.getName().equalsIgnoreCase("int") ||
                this.currentToken.getName().equalsIgnoreCase("byte") ||
                this.currentToken.getName().equalsIgnoreCase("string") ||
                this.currentToken.getName().equalsIgnoreCase("boolean"));
    }

    private String formatByteType() {
        StringBuilder str = new StringBuilder();
        boolean leftZero = true;
        String tokenName = this.currentToken.getName();

        // comeca em 2 pra desconsiderar o 0h do hexa
        if (tokenName.length() > 2) {
            for (int i = 2; i < tokenName.length(); i++) {
                char c = tokenName.charAt(i);

                if (c != '0') leftZero = false;

                if (!leftZero) str.append(c);
            }
        }
        if (str.isEmpty()) str.append("0");

        return str.toString();
    }

    private String generateCodeSection() {
        this.codeSection.append(".code\n").append("start:\n");
        this.beginGeneration();

        return this.codeSection.append("invoke ExitProcess, 0\n").append("end start\n").toString();
    }

    private void beginGeneration() {
        if (this.currentToken.getName().equalsIgnoreCase("begin")) {
            this.nextToken();
            this.identifyCommand();
        }
    }

    private void identifyCommand() {
        if (this.currentToken.getName().equalsIgnoreCase("write") ||
                this.currentToken.getName().equalsIgnoreCase("writeln")) {
            this.identifyWrite();
            this.identifyCommand();
        } else if (this.currentToken.getName().equalsIgnoreCase("readln")) {
            this.identifyRead();
            this.identifyCommand();
        } else if (this.currentToken.getName().equalsIgnoreCase("end")) {
            return;
        } else {
            this.nextToken();
            this.identifyCommand();
        }
    }


    private void identifyWrite() {
        String breakLine = this.currentToken.getName().equalsIgnoreCase("writeln") ? ", 13, 10, 0\n" : ", 0\n";
        this.nextToken(); // ,
        this.writeGeneration(breakLine);
        this.nextToken(); // ;
    }

    private void writeGeneration(String breakLine) {
        StringBuilder formatStr = new StringBuilder("\"");
        ArrayList<String> args = new ArrayList<>();

        while (!this.currentToken.getName().equalsIgnoreCase(";")) {
            this.nextToken();

            if (this.currentToken.getClassification().equalsIgnoreCase("ID")) {
                formatStr.append("%s");
                args.add("addr " + this.currentToken.getName());
            } else {
                String literal = this.currentToken.getName().replace("\"", "");
                formatStr.append(literal);
            }

            this.nextToken();
            if (this.currentToken.getName().equals(",")) {
                continue;
            }
        }

        formatStr.append("\"");

        String dataLabel = "str" + stringCount++;

        dataSection.append(dataLabel).append(" db ").append(formatStr).append(breakLine);

        codeSection.append("invoke crt_printf, addr ").append(dataLabel);
        for (String arg : args) {
            codeSection.append(", ").append(arg);
        }
        codeSection.append("\n");
    }

    private void identifyRead() {
        this.nextToken(); // ,
        this.nextToken(); // valor
        if (this.currentToken.getClassification().equalsIgnoreCase("ID")) {
            String variable = this.currentToken.getName();
            codeSection.append("invoke crt_gets, addr ").append(variable).append("\n");
        }
        this.nextToken(); // ;
        this.nextToken(); // proximo comando
    }

    public void convert() {
        createOutDirectory();

        generateAssemblyCode();
    }
}
