package com.compiladores.compilador.Assembler;

import com.compiladores.compilador.Lexical.Token;
import com.compiladores.compilador.Table.SymbolsTable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ConvertAssembly {

    private final SymbolsTable symbolsTable;
    private Token currentToken;
    private int currentTokenIndex = 0;
    private final String path = "../out";

    public ConvertAssembly(SymbolsTable symbolsTable) {
        this.symbolsTable = symbolsTable;
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

    private void createAsmFile() {
        File outDir = new File(path);
        if (!outDir.exists()) {
            System.out.println("Diretório 'out' não existe.");
            return;
        }

        File asmFile = new File(outDir, "program.asm");
        try {
            if (asmFile.createNewFile()) {
                System.out.println("Arquivo 'program.asm' criado com sucesso!");
            } else {
                System.out.println("Arquivo 'program.asm' já existe.");
            }
        } catch (IOException e) {
            System.out.println("Erro ao criar o arquivo 'program.asm': " + e.getMessage());
        }
    }

    public void writeAssemblyCode(String assemblyCode) {
        File outDir = new File("out");
        if (!outDir.exists()) {
            System.out.println("Diretório 'out' não existe.");
            return;
        }

        File asmFile = new File(outDir, "program.asm");
        try (FileWriter writer = new FileWriter(asmFile)) {
            writer.write(assemblyCode);
            System.out.println("Código Assembly escrito com sucesso no arquivo 'program.asm'.");
        } catch (IOException e) {
            System.out.println("Erro ao escrever no arquivo 'program.asm': " + e.getMessage());
        }
    }

    public void generateExampleAssemblyCode() {
        StringBuilder assemblyCode = new StringBuilder();

        assemblyCode.append(".386\n")
                .append(".model flat, stdcall\n")
                .append("option casemap :none\n\n")
                .append("include \\masm32\\include\\masm32rt.inc\n")
                .append("include \\masm32\\include\\kernel32.inc\n")
                .append("include \\masm32\\include\\masm32.inc\n")
                .append("include \\masm32\\include\\msvcrt.inc\n")
                .append("includelib \\masm32\\lib\\kernel32.lib\n")
                .append("includelib \\masm32\\lib\\masm32.lib\n")
                .append("includelib \\masm32\\lib\\msvcrt.lib\n")
                .append("include \\masm32\\macros\\macros.asm\n\n")
                .append(".data\n\n")
                .append(".code\n")
                .append("start:\n")
                .append("invoke ExitProcess, 0\n")
                .append("end start\n");

        writeAssemblyCode(assemblyCode.toString());
    }

    public void convert() {
        createOutDirectory();
        createAsmFile();

        generateExampleAssemblyCode();
    }
}