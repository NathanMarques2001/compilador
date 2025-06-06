package com.compiladores.compilador;

import com.compiladores.compilador.Assembly.AssemblyGenerator;
import com.compiladores.compilador.Exceptions.CompilerException;
import com.compiladores.compilador.Grammar.SyntaticAnalyzer;
import com.compiladores.compilador.Lexical.LexicalAnalyzer;
import com.compiladores.compilador.Semantic.SemanticAnalyzer;
import com.compiladores.compilador.Table.SymbolsTable;
import com.compiladores.compilador.Utils.ReadLCCode;

public class Main {

    public static void main(String[] args) {
        // Verifica se o caminho do arquivo foi passado como argumento
//        if (args.length == 0) {
//            System.err.println("Uso: java -cp out com.compiladores.compilador.Main <caminho_do_arquivo>");
//            return;
//        }

        try {
            // Obtém o caminho do arquivo a partir dos argumentos
            String filePath = "C:\\Users\\natha\\dev\\compilador\\src\\main\\java\\com\\compiladores\\compilador\\LC_Codes\\calculadora.lc";
            System.out.println("Lendo o arquivo: " + filePath);
            String fileName = filePath.substring(filePath.lastIndexOf("\\") + 1);
            fileName = fileName.substring(0, fileName.lastIndexOf("."));

            // Instancia os componentes do compilador
            ReadLCCode lcReader = new ReadLCCode();
            SymbolsTable table = new SymbolsTable();
            LexicalAnalyzer lexer = new LexicalAnalyzer(table);

            // Executa a análise léxica
            runLexicalAnalysis(filePath, lcReader, lexer, table);

            // Executa a análise sintática
            runSyntacticAnalysis(table);

            // Executa a análise semântica
            runSemanticAnalysis(table);

            // Executa a geração de código assembly
            runAssemblyGeneration(table, fileName);

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private static void runLexicalAnalysis(String filePath, ReadLCCode lcReader, LexicalAnalyzer lexer, SymbolsTable table) throws CompilerException {
        System.out.println("========================================\nIniciando análise léxica...");
        lcReader.readFileAndAnalyze(filePath, lexer);
        System.out.println("Análise léxica concluída!\n========================================");
        //table.printSymbols();
    }

    private static void runSyntacticAnalysis(SymbolsTable table) throws CompilerException {
        SyntaticAnalyzer parser = new SyntaticAnalyzer(table);
        System.out.println("Iniciando análise sintática...");
        parser.parseProgram();
        System.out.println("Análise sintática concluída!\n========================================");
    }

    private static void runSemanticAnalysis(SymbolsTable table) throws CompilerException {
        SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer(table);
        System.out.println("Iniciando análise semântica...");
        semanticAnalyzer.analyze();
        System.out.println("Análise semântica concluída!\n========================================");
    }

    private static void runAssemblyGeneration(SymbolsTable table, String fileName) throws CompilerException {
        AssemblyGenerator assemblyGenerator = new AssemblyGenerator(table, fileName);
        System.out.println("Iniciando geração de código assembly...");
        assemblyGenerator.convert();
        System.out.println("Geração de código assembly concluída!");
    }
}