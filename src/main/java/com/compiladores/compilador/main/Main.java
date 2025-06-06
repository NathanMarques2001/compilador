package com.compiladores.compilador.main;

import com.compiladores.compilador.codegen.AssemblyGenerator;
import com.compiladores.compilador.exceptions.CompilerException;
import com.compiladores.compilador.parser.SyntaticAnalyzer;
import com.compiladores.compilador.lexer.LexicalAnalyzer;
import com.compiladores.compilador.semantic.SemanticAnalyzer;
import com.compiladores.compilador.symboltable.SymbolsTable;
import com.compiladores.compilador.io.ReadLCCode;
import com.compiladores.compilador.optimizer.PeepholeOptimizer;

import java.io.File;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        try {
            // Caminho completo do arquivo de entrada .lc
            String filePath = "src/main/java/com/compiladores/compilador/io/LC_Codes/calculadora.lc";

            File inputFile = new File(filePath);
            if (!inputFile.exists()) {
                System.err.println("Arquivo não encontrado: " + inputFile.getAbsolutePath());
                return;
            }

            // Diretório raiz do projeto
            String projectRoot = new File("src/main/java").getAbsolutePath();

            // Nome base do arquivo (sem extensão)
            String fileName = inputFile.getName().replaceFirst("[.][^.]+$", "");

            // Diretório de saída para o .asm
            String outputDir = projectRoot + File.separator + "com" + File.separator +
                    "compiladores" + File.separator +
                    "compilador" + File.separator +
                    "codegen" + File.separator +
                    "out";

            // Caminho completo do arquivo ASM de saída
            String asmFilePath = outputDir + File.separator + fileName + ".asm";

            System.out.println("Lendo o arquivo: " + inputFile.getAbsolutePath());

            // Executa o pipeline de compilação
            SymbolsTable table = new SymbolsTable();
            LexicalAnalyzer lexer = new LexicalAnalyzer(table);
            ReadLCCode reader = new ReadLCCode();

            runLexicalAnalysis(inputFile.getPath(), reader, lexer, table);
            runSyntacticAnalysis(table);
            runSemanticAnalysis(table);
            runAssemblyGeneration(table, fileName);
            runPeepholeOptimizer(asmFilePath);

        } catch (Exception e) {
            System.err.println("Erro: " + e.getMessage());
        }
    }

    private static void runLexicalAnalysis(String path, ReadLCCode reader, LexicalAnalyzer lexer, SymbolsTable table) throws CompilerException {
        System.out.println("\n=== Análise Léxica ===");
        reader.readFileAndAnalyze(path, lexer);
        System.out.println("Concluída.");
    }

    private static void runSyntacticAnalysis(SymbolsTable table) throws CompilerException {
        System.out.println("\n=== Análise Sintática ===");
        new SyntaticAnalyzer(table).parseProgram();
        System.out.println("Concluída.");
    }

    private static void runSemanticAnalysis(SymbolsTable table) throws CompilerException {
        System.out.println("\n=== Análise Semântica ===");
        new SemanticAnalyzer(table).analyze();
        System.out.println("Concluída.");
    }

    private static void runAssemblyGeneration(SymbolsTable table, String fileName) throws CompilerException {
        System.out.println("\n=== Geração de Código Assembly ===");
        new AssemblyGenerator(table, fileName).convert();
        System.out.println("Concluída.");
    }

    private static void runPeepholeOptimizer(String asmFilePath) throws IOException {
        System.out.println("\n=== Otimização Peephole ===");
        PeepholeOptimizer.optimizeFile(asmFilePath);
        System.out.println("Concluída.");
    }
}
