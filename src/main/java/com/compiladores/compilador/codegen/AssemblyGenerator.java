package com.compiladores.compilador.codegen;

import com.compiladores.compilador.lexer.Token;
import com.compiladores.compilador.symboltable.SymbolsTable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Stack;

public class AssemblyGenerator {

    private final SymbolsTable symbolsTable;
    private Token currentToken;
    private int currentTokenIndex = 0;
    private final String path = "./src/main/java/com/compiladores/compilador/codegen/out";
    private String fileName = "";
    private final StringBuilder headerSection = new StringBuilder();
    private final StringBuilder dataSection = new StringBuilder();
    private final StringBuilder codeSection = new StringBuilder();
    private int stringCount = 1;
    private int loopCounter = 1;
    private int ifCounter = 1;
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

    private void nextToken() {
        this.currentTokenIndex++;
        if (this.currentTokenIndex < this.symbolsTable.getSize()) {
            this.currentToken = this.symbolsTable.currentToken(this.currentTokenIndex);
        } else {
            this.currentToken = null;
        }
    }

    private void createOutDirectory() {
        File outDir = new File(this.path);
        if (!outDir.exists()) {
            outDir.mkdirs();
        }
    }

    private void writeAssemblyCode(String assemblyCode) {
        File asmFile = new File(this.path, this.fileName);
        try (FileWriter writer = new FileWriter(asmFile)) {
            writer.write(assemblyCode);
            System.out.println("Código Assembly escrito com sucesso no arquivo '" + this.fileName + "'.");
        } catch (IOException e) {
            System.out.println("Erro ao escrever no arquivo '" + this.fileName + "': " + e.getMessage());
        }
    }

    public void convert() {
        this.createOutDirectory();
        this.generateAssemblyCode();
    }

    private void generateAssemblyCode() {
        StringBuilder assemblyCode = new StringBuilder();
        this.generateHeader();
        this.generateDataSection();
        this.generateCodeSection();

        assemblyCode.append(this.headerSection);
        assemblyCode.append(this.dataSection);
        assemblyCode.append(this.codeSection);

        this.writeAssemblyCode(assemblyCode.toString());
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
        int originalIndex = this.currentTokenIndex;
        this.currentTokenIndex = 0;
        if (this.symbolsTable.getSize() > 0) {
            this.currentToken = this.symbolsTable.currentToken(this.currentTokenIndex);
            while (this.currentToken != null && isDeclarationScope()) {
                identifyDeclaration();
            }
        }
        this.currentTokenIndex = originalIndex;
        if (this.currentTokenIndex < this.symbolsTable.getSize()) {
            this.currentToken = this.symbolsTable.currentToken(this.currentTokenIndex);
        } else {
            this.currentToken = null;
        }
    }

    private boolean isDeclarationScope() {
        return this.currentToken != null && (isPrimitiveType() || this.currentToken.getName().equalsIgnoreCase("final"));
    }

    private String formatValue(String value, String type) {
        if (value == null) return "0";
        if (type.equalsIgnoreCase("boolean")) {
            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("Fh")) return "1";
            if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("0h")) return "0";
        }
        if (value.toLowerCase().startsWith("0h") && type.equalsIgnoreCase("byte")) {
            return value.substring(2) + "h";
        }
        return value;
    }

    private void identifyDeclaration() {
        if (this.currentToken.getName().equalsIgnoreCase("final")) {
            nextToken(); // Consumes 'final'
            String constName = this.currentToken.getName();
            nextToken(); // Consumes constant name
            nextToken(); // Consumes '='
            String constValue = this.currentToken.getName();

            if (constValue.startsWith("\"")) {
                String actualString = constValue.substring(1, constValue.length() - 1);
                String strLabel = "const_str_" + constName;
                this.dataSection.append(String.format("    %-15s db \"%s\", 0\n", strLabel, actualString));
                this.dataSection.append(String.format("    %-15s equ addr %s\n", constName, strLabel));
            } else {
                if (constValue.equalsIgnoreCase("true") || constValue.equalsIgnoreCase("false") ||
                        constValue.equalsIgnoreCase("Fh") || constValue.equalsIgnoreCase("0h")) {
                    this.dataSection.append(String.format("    %-15s equ %s\n", constName, formatValue(constValue, "boolean")));
                } else {
                    this.dataSection.append(String.format("    %-15s equ %s\n", constName, formatValue(constValue, "int")));
                }
            }
            nextToken(); // Consumes value
            nextToken(); // Consumes ';'
        } else {
            String type = this.currentToken.getName();
            String dataTypeMASM = primitiveTypeMASM(type);
            nextToken(); // Consumes type

            while (this.currentToken != null && !this.currentToken.getName().equals(";")) {
                String dataName = this.currentToken.getName();
                nextToken(); // Consumes variable name
                String dataValue = "0";

                if (type.equalsIgnoreCase("string")) {
                    this.dataSection.append(String.format("    %-15s db 256 dup(0)\n", dataName));
                } else {
                    if (this.currentToken.getName().equals("=")) {
                        nextToken(); // Consumes '='
                        dataValue = formatValue(this.currentToken.getName(), type);
                        nextToken(); // Consumes value
                    }
                    this.dataSection.append(String.format("    %-15s %-5s %s\n", dataName, dataTypeMASM, dataValue));
                }

                if (this.currentToken != null && this.currentToken.getName().equals(",")) {
                    nextToken(); // Consumes ','
                }
            }
            if (this.currentToken != null && this.currentToken.getName().equals(";")) {
                nextToken(); // Consumes ';'
            }
        }
    }

    private String primitiveTypeMASM(String type) {
        return switch (type.toLowerCase()) {
            case "int" -> "dd";
            case "boolean", "byte" -> "db";
            default -> "";
        };
    }

    private boolean isPrimitiveType() {
        if (this.currentToken == null) return false;
        String name = this.currentToken.getName().toLowerCase();
        return name.equals("int") || name.equals("string") || name.equals("boolean") || name.equals("byte");
    }

    private void generateCodeSection() {
        this.codeSection.append(".code\n").append("start:\n");
        this.beginGeneration();
        this.codeSection.append("\n    invoke ExitProcess, 0\n").append("end start\n");
    }

    private void beginGeneration() {
        while (this.currentToken != null && !this.currentToken.getName().equalsIgnoreCase("begin")) {
            nextToken();
        }
        if (this.currentToken != null && this.currentToken.getName().equalsIgnoreCase("begin")) {
            nextToken(); // Consumes 'begin'
        }

        while (this.currentToken != null && !this.currentToken.getName().equalsIgnoreCase("end")) {
            identifyCommands();
        }
        if (this.currentToken != null && this.currentToken.getName().equalsIgnoreCase("end")) {
            nextToken(); // Consumes 'end'
        }
    }

    private void identifyCommands() {
        if (this.currentToken == null) return;

        switch (this.currentToken.getName().toLowerCase()) {
            case "write", "writeln" -> identifyWrite();
            case "readln" -> identifyRead();
            case "while" -> identifyWhile();
            case "if" -> identifyIf();
            case ";" -> nextToken();
            default -> {
                if (this.currentToken.getClassification().equalsIgnoreCase("ID")) {
                    identifyAssignment();
                } else {
                    if (!this.currentToken.getName().equalsIgnoreCase("end")) {
                        nextToken();
                    }
                }
            }
        }
    }

    private void identifyWrite() {
        boolean breakLine = this.currentToken.getName().equalsIgnoreCase("writeln");
        nextToken(); // Consumes 'write' or 'writeln'
        nextToken(); // Consumes ','

        StringBuilder formatStr = new StringBuilder();
        ArrayList<String> args = new ArrayList<>();

        while (this.currentToken != null && !this.currentToken.getName().equals(";")) {
            if (this.currentToken.getClassification().equalsIgnoreCase("ID")) {
                String varName = this.currentToken.getName();
                String varType = this.symbolsTable.getSymbolType(varName);
                if (varType == null) varType = "int";

                if (varType.equalsIgnoreCase("string")) {
                    formatStr.append("%s");
                    args.add("addr " + varName);
                } else {
                    formatStr.append("%d");
                    args.add(varName);
                }
            } else {
                String literal = this.currentToken.getName().replace("\"", "");
                formatStr.append(literal);
            }
            nextToken();
            if (this.currentToken != null && this.currentToken.getName().equals(",")) {
                nextToken(); // Consumes ','
            }
        }

        if (this.currentToken != null && this.currentToken.getName().equals(";")) {
            nextToken(); // Consumes ';'
        }

        String dataLabel = "str" + this.stringCount++;
        String lineEnding = breakLine ? ", 13, 10, 0" : ", 0";
        this.dataSection.append(String.format("    %-15s db \"%s\"%s\n", dataLabel, formatStr.toString(), lineEnding));

        this.codeSection.append("    invoke crt_printf, addr ").append(dataLabel);
        for (String arg : args) {
            this.codeSection.append(", ").append(arg);
        }
        this.codeSection.append("\n");
    }

    private void identifyRead() {
        nextToken(); // Consumes 'readln'
        nextToken(); // Consumes ','
        String variableName = this.currentToken.getName();
        String varType = this.symbolsTable.getSymbolType(variableName);
        if (varType == null) varType = "int";

        if (varType.equalsIgnoreCase("int") || varType.equalsIgnoreCase("byte") || varType.equalsIgnoreCase("boolean")) {
            if (!this.formatDSDeclared) {
                this.dataSection.append(String.format("    %-15s db \"%%d\", 0\n", "format_d"));
                this.formatDSDeclared = true;
            }
            this.codeSection.append("    invoke crt_scanf, addr format_d, addr ").append(variableName).append("\n");
        } else { // string
            this.codeSection.append("    invoke crt_gets, addr ").append(variableName).append("\n");
        }

        nextToken(); // Consumes variable name
        if (this.currentToken != null && this.currentToken.getName().equals(";")) {
            nextToken(); // Consumes ';'
        }
    }

    private void identifyWhile() {
        int localLoopCounter = this.loopCounter++;
        String loopLabel = "_loop" + localLoopCounter;
        String loopEndLabel = "_fimLoop" + localLoopCounter;

        this.codeSection.append("\n").append(loopLabel).append(":\n");
        nextToken(); // Consumes 'while'

        String controlVarOrExprStartName = this.currentToken.getName();
        String controlVarOrExprType = this.symbolsTable.getSymbolType(controlVarOrExprStartName);

        if (controlVarOrExprType != null && controlVarOrExprType.equalsIgnoreCase("boolean") &&
                (this.currentTokenIndex + 1 < this.symbolsTable.getSize() &&
                        this.symbolsTable.currentToken(this.currentTokenIndex + 1).getName().equalsIgnoreCase("begin"))) {
            this.codeSection.append("    mov al, ").append(controlVarOrExprStartName).append("\n");
            this.codeSection.append("    cmp al, 1\n");
            this.codeSection.append("    jne ").append(loopEndLabel).append("\n\n");
            nextToken();
        } else {
            generateConditionalExpression(loopEndLabel, true);
        }

        nextToken(); // Consumes 'begin'

        while (this.currentToken != null && !this.currentToken.getName().equalsIgnoreCase("end")) {
            identifyCommands();
        }

        if (this.currentToken != null && this.currentToken.getName().equalsIgnoreCase("end")) {
            nextToken(); // Consumes 'end'
        }

        this.codeSection.append("\n    jmp ").append(loopLabel).append("\n");
        this.codeSection.append(loopEndLabel).append(":\n");
    }

    private void identifyIf() {
        int localIfCounter = this.ifCounter++;
        String elseLabel = "_else" + localIfCounter;
        String endIfLabel = "_fimIf" + localIfCounter;
        nextToken(); // Consumes 'if'

        generateConditionalExpression(elseLabel, true);
        nextToken(); // Consumes 'begin'

        while (this.currentToken != null && !this.currentToken.getName().equalsIgnoreCase("end") &&
                !this.currentToken.getName().equalsIgnoreCase("else")) {
            identifyCommands();
        }

        boolean hasElse = this.currentToken != null && this.currentToken.getName().equalsIgnoreCase("else");

        if (this.currentToken != null && this.currentToken.getName().equalsIgnoreCase("end")) {
            nextToken(); // Consumes 'end' from if-block
        }

        if (hasElse) {
            this.codeSection.append("    jmp ").append(endIfLabel).append("\n");
            this.codeSection.append(elseLabel).append(":\n");
            nextToken(); // Consumes 'else'
            nextToken(); // Consumes 'begin' from else-block

            while (this.currentToken != null && !this.currentToken.getName().equalsIgnoreCase("end")) {
                identifyCommands();
            }
            if (this.currentToken != null && this.currentToken.getName().equalsIgnoreCase("end")) {
                nextToken(); // Consumes 'end' from else-block
            }
            this.codeSection.append(endIfLabel).append(":\n");
        } else {
            this.codeSection.append(elseLabel).append(":\n");
        }
    }

    private void generateConditionalExpression(String targetLabel, boolean jumpIfConditionFalse) {
        String firstOperand = this.currentToken.getName();
        String firstOperandType = this.symbolsTable.getSymbolType(firstOperand);
        if (firstOperandType == null) {
            firstOperandType = "int"; // Fallback
        }

        nextToken();

        if (this.currentToken != null && this.currentToken.getName().equalsIgnoreCase("begin")) {
            String reg = "al";
            String valueToCompare = firstOperand;
            if (firstOperandType.equalsIgnoreCase("boolean")) {
                if (firstOperand.equalsIgnoreCase("true") || firstOperand.equalsIgnoreCase("Fh")) valueToCompare = "1";
                else if (firstOperand.equalsIgnoreCase("false") || firstOperand.equalsIgnoreCase("0h")) valueToCompare = "0";
            }

            if (this.symbolsTable.getSymbolType(firstOperand) != null) {
                this.codeSection.append("    mov ").append(reg).append(", ").append(firstOperand).append("\n");
                this.codeSection.append("    cmp ").append(reg).append(", 1\n");
            } else {
                this.codeSection.append("    cmp byte ptr ").append(valueToCompare).append(", 1\n");
            }

            String jumpInstruction = jumpIfConditionFalse ? "jne" : "je";
            this.codeSection.append("    ").append(jumpInstruction).append(" ").append(targetLabel).append("\n");
            return;
        }

        String operator = this.currentToken.getName();
        nextToken();
        String secondOperand = this.currentToken.getName();

        String reg = firstOperandType.equalsIgnoreCase("boolean") || firstOperandType.equalsIgnoreCase("byte") ? "al" : "eax";
        String formattedFirstOperand = firstOperand;
        if (firstOperandType.equalsIgnoreCase("boolean")) {
            if (firstOperand.equalsIgnoreCase("true") || firstOperand.equalsIgnoreCase("Fh")) formattedFirstOperand = "1";
            else if (firstOperand.equalsIgnoreCase("false") || firstOperand.equalsIgnoreCase("0h")) formattedFirstOperand = "0";
        }

        String formattedSecondOperand = secondOperand;
        if (secondOperand.equalsIgnoreCase("true") || secondOperand.equalsIgnoreCase("Fh")) formattedSecondOperand = "1";
        else if (secondOperand.equalsIgnoreCase("false") || secondOperand.equalsIgnoreCase("0h")) formattedSecondOperand = "0";

        this.codeSection.append("    mov ").append(reg).append(", ").append(formattedFirstOperand).append("\n");
        this.codeSection.append("    cmp ").append(reg).append(", ").append(formattedSecondOperand).append("\n");

        String jumpInstruction = getJumpInstruction(operator, jumpIfConditionFalse);
        this.codeSection.append("    ").append(jumpInstruction).append(" ").append(targetLabel).append("\n");
    }

    private String getJumpInstruction(String operator, boolean jumpIfConditionFalse) {
        return switch (operator) {
            case "==" -> jumpIfConditionFalse ? "jne" : "je";
            case "<>" -> jumpIfConditionFalse ? "je" : "jne";
            case "<" -> jumpIfConditionFalse ? "jge" : "jl";
            case ">" -> jumpIfConditionFalse ? "jle" : "jg";
            case "<=" -> jumpIfConditionFalse ? "jg" : "jle";
            case ">=" -> jumpIfConditionFalse ? "jl" : "jge";
            default -> "";
        };
    }

    private void identifyAssignment() {
        String variableName = this.currentToken.getName();
        String varType = this.symbolsTable.getSymbolType(variableName);
        if (varType == null) varType = "int";

        nextToken(); // Consumes variable name
        nextToken(); // Consumes '='

        if (varType.equalsIgnoreCase("string")) {
            String stringLiteral = this.currentToken.getName();
            String actualStringValue = stringLiteral.substring(1, stringLiteral.length() - 1);
            String stringLabelInData = "str_assign_" + actualStringValue.replaceAll("[^a-zA-Z0-9_]", "") + this.stringCount++;
            this.dataSection.append(String.format("    %-15s db \"%s\", 0\n", stringLabelInData, actualStringValue));
            this.codeSection.append("    invoke crt_strcpy, addr ").append(variableName).append(", addr ").append(stringLabelInData).append("\n");
            nextToken();
        } else {
            ArrayList<Token> expressionTokens = new ArrayList<>();
            while (this.currentToken != null && !this.currentToken.getName().equals(";")) {
                expressionTokens.add(this.currentToken);
                nextToken();
            }
            evaluateExpression(expressionTokens);

            this.codeSection.append("    pop eax\n");
            if (varType.equalsIgnoreCase("boolean") || varType.equalsIgnoreCase("byte")) {
                this.codeSection.append("    mov ").append(variableName).append(", al\n");
            } else {
                this.codeSection.append("    mov ").append(variableName).append(", eax\n");
            }
        }

        if (this.currentToken != null && this.currentToken.getName().equals(";")) {
            nextToken(); // Consumes ';'
        }
    }


    private void evaluateExpression(ArrayList<Token> tokens) {
        Stack<String> ops = new Stack<>();

        for (Token token : tokens) {
            String name = token.getName();
            String classification = token.getClassification();

            if (name.equalsIgnoreCase("Fh")) {
                this.codeSection.append("    push 1\n");
            } else if (name.equalsIgnoreCase("0h")) {
                this.codeSection.append("    push 0\n");
            } else if (classification.matches("ID|CONST") || name.equalsIgnoreCase("true") || name.equalsIgnoreCase("false")) {
                String valueToPush = name;
                if (name.equalsIgnoreCase("true")) valueToPush = "1";
                else if (name.equalsIgnoreCase("false")) valueToPush = "0";
                this.codeSection.append("    push ").append(valueToPush).append("\n");
            } else if (name.equals("(")) {
                ops.push(name);
            } else if (name.equals(")")) {
                while (!ops.empty() && !ops.peek().equals("(")) {
                    generateOp(ops.pop());
                }
                if (!ops.empty()) ops.pop();
            } else if (isOperator(name)) {
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
        this.codeSection.append("    pop ebx\n");
        this.codeSection.append("    pop eax\n");
        switch (op) {
            case "+" -> this.codeSection.append("    add eax, ebx\n");
            case "-" -> this.codeSection.append("    sub eax, ebx\n");
            case "*" -> this.codeSection.append("    imul eax, ebx\n");
            case "/" -> {
                this.codeSection.append("    cdq\n");
                this.codeSection.append("    idiv ebx\n");
            }
        }
        this.codeSection.append("    push eax\n");
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