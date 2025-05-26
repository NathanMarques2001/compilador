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
    private ArrayList<String> expressionValues = new ArrayList<>();
    private ArrayList<String> expressionSymbols = new ArrayList<>();
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

        File asmFile = new File(outDir, fileName);
        try (FileWriter writer = new FileWriter(asmFile)) {
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
            if (dataType.equals("dw")) {
                dataValue = this.formatByteType() + "h";
            }
            this.nextToken();
        }

        if (dataValue.isEmpty()) {
            dataValue = switch (dataType) {
                case "dd", "db" -> "0";
                case "dw" -> "0h";
                default -> "dup(0)";
            };
        }

        this.dataSection.append(dataName).append(" ").append(dataType).append(" ").append(dataValue).append("\n");
        this.nextToken();
        this.identifyDeclarations();
    }

    private String primitiveTypeMASM() {
        return switch (this.currentToken.getName()) {
            case "int" -> "dd";
            case "string" -> "db 256";
            case "boolean" -> "db";
            case "byte" -> "dw";
            default -> "equ";
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
        this.resetRegister();
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
        } else if (this.currentToken.getName().equalsIgnoreCase("while")) {
            this.identifyWhile();
            this.identifyCommand();
        } else if (this.currentToken.getName().equalsIgnoreCase("if")) {
            this.identifyIf();
            this.identifyCommand();
        } else if (this.currentToken.getClassification().equalsIgnoreCase("ID")) {
            this.identifyAssignment();
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
        this.nextToken(); // próximo comando
    }

    private void identifyWhile() {
        String loopLabel = "_loop" + loopCounter;
        String loopEndLabel = "_fimLoop" + loopCounter;
        loopCounter++;

        codeSection.append(loopLabel).append(":\n");

        this.nextToken(); // pega a primeira parte da condição (ex: "cond" ou "x")
        String leftOperand = currentToken.getName();
        this.nextToken(); // verifica se é operador ou já é 'begin'

        // Caso 1: while cond (variável de controle booleana)
        if (currentToken.getName().equalsIgnoreCase("begin")) {
            codeSection.append("cmp ").append(leftOperand).append(", 0\n");
            codeSection.append("je ").append(loopEndLabel).append("\n");

            this.nextToken(); // entra no bloco
            this.identifyCommand();
            codeSection.append("jmp ").append(loopLabel).append("\n");
            codeSection.append(loopEndLabel).append(":\n");

            if (this.currentToken.getName().equalsIgnoreCase("end")) {
                this.nextToken();
            }
            return;
        }

        // Caso 2: while com condição completa (x < 10)
        String operator = currentToken.getName();
        this.nextToken();
        String rightOperand = currentToken.getName();

        codeSection.append("mov al, ").append(leftOperand).append("\n");
        codeSection.append("mov bl, ").append(rightOperand).append("\n");
        codeSection.append("cmp al, bl\n");

        switch (operator) {
            case "==":
                codeSection.append("jne ").append(loopEndLabel).append("\n");
                break;
            case "!=":
                codeSection.append("je ").append(loopEndLabel).append("\n");
                break;
            case "<":
                codeSection.append("jge ").append(loopEndLabel).append("\n");
                break;
            case ">":
                codeSection.append("jle ").append(loopEndLabel).append("\n");
                break;
            case "<=":
                codeSection.append("jg ").append(loopEndLabel).append("\n");
                break;
            case ">=":
                codeSection.append("jl ").append(loopEndLabel).append("\n");
                break;
            default:
                throw new RuntimeException("Operador lógico inválido no while: " + operator);
        }

        this.nextToken(); // deve ser 'begin'
        if (!this.currentToken.getName().equalsIgnoreCase("begin")) {
            throw new RuntimeException("Esperado 'begin' após condição do while");
        }

        this.nextToken(); // comandos internos
        this.identifyCommand();

        codeSection.append("jmp ").append(loopLabel).append("\n");
        codeSection.append(loopEndLabel).append(":\n");

        if (this.currentToken.getName().equalsIgnoreCase("end")) {
            this.nextToken();
        }
    }

    private void identifyIf() {
        String ifLabel = "_if" + ifCounter;
        String elseLabel = "_else" + ifCounter;
        String endIfLabel = "_fimIf" + ifCounter;
        ifCounter++;

        this.nextToken(); // variável ou valor esquerdo
        String left = this.currentToken.getName();

        this.nextToken(); // operador (==, !=, <, >, etc.)
        String operator = this.currentToken.getName();

        this.nextToken(); // valor direito
        String right = this.currentToken.getName();

        codeSection.append("mov al, ").append(left).append("\n");
        codeSection.append("mov bl, ").append(right).append("\n");
        codeSection.append("cmp al, bl\n");

        switch (operator) {
            case "==":
                codeSection.append("jne ").append(elseLabel).append("\n");
                break;
            case "!=":
                codeSection.append("je ").append(elseLabel).append("\n");
                break;
            case "<":
                codeSection.append("jge ").append(elseLabel).append("\n");
                break;
            case ">":
                codeSection.append("jle ").append(elseLabel).append("\n");
                break;
            case "<=":
                codeSection.append("jg ").append(elseLabel).append("\n");
                break;
            case ">=":
                codeSection.append("jl ").append(elseLabel).append("\n");
                break;
        }

        this.nextToken(); // begin
        if (!this.currentToken.getName().equalsIgnoreCase("begin")) {
            throw new RuntimeException("Esperado 'begin' após condição do if");
        }

        this.nextToken(); // comandos dentro do if
        this.identifyCommand();
        codeSection.append("jmp ").append(endIfLabel).append("\n");

        // ELSE
        if (this.currentToken.getName().equalsIgnoreCase("else")) {
            codeSection.append(elseLabel).append(":\n");
            this.nextToken(); // begin
            this.nextToken(); // comandos dentro do else
            this.identifyCommand();
        } else {
            // Sem else, só define o label para pular
            codeSection.append(elseLabel).append(":\n");
        }

        codeSection.append(endIfLabel).append(":\n");

        if (this.currentToken.getName().equalsIgnoreCase("end")) {
            this.nextToken();
        }
    }

    private void identifyAssignment() {
        String valorMemoria = this.currentToken.getName();
        this.nextToken(); // =

        this.getExpressionSymbols();

        if (!this.expressionSymbols.isEmpty()) {
            this.buildExpression();
            codeSection.append("mov ").append(valorMemoria).append(", eax\n");
        } else {
            codeSection.append("mov ").append(valorMemoria).append(this.expressionValues.getFirst()).append("\n");
            expressionValues.clear();
        }

        this.nextToken();
    }

    private void getExpressionSymbols() {
        this.nextToken();
        if (!this.currentToken.getName().equalsIgnoreCase(";")) {
            if (this.arithmeticSymbols()) {
                this.expressionSymbols.add(this.currentToken.getName());
            } else {
                this.expressionValues.add(this.currentToken.getName());
            }
            this.getExpressionSymbols();
        }
    }

    private boolean arithmeticSymbols() {
        return (this.currentToken.getName().equals("+") ||
                this.currentToken.getName().equals("-") ||
                this.currentToken.getName().equals("*") ||
                this.currentToken.getName().equals("/"));
    }

    private void buildExpression() {
        String command = switch (this.expressionSymbols.getFirst()) {
            case "+" -> "add";
            case "-" -> "sub";
            case "*" -> "imul";
            default -> "idiv";
        };

        if (command.equals("add") || command.equals("sub")) {
            codeSection.append("mov eax, ").append(expressionValues.getFirst()).append("\n");
            this.expressionValues.removeFirst();
            codeSection.append(command).append(" eax, ").append(expressionValues.getFirst()).append("\n");
            this.expressionValues.removeFirst();
            this.expressionSymbols.removeFirst();
        }

        if (!this.expressionSymbols.isEmpty()) {
            this.buildExpression();
        }
    }

    private void resetRegister() {
        codeSection.append("mov eax, 0");
        codeSection.append("mov ebx, 0");
        codeSection.append("mov ecx, 0");
        codeSection.append("mov edx, 0");
    }

    public void convert() {
        createOutDirectory();
        generateAssemblyCode();
    }
}
