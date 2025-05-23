package com.compiladores.compilador;

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
// teste
        try {
            // Obtém o caminho do arquivo a partir dos argumentos
            String filePath = "C:\\Users\\natha\\dev\\compilador\\src\\main\\java\\com\\compiladores\\compilador\\LC_Codes\\teste.lc";
            System.out.println("Lendo o arquivo: " + filePath);

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

        } catch (CompilerException e) {
            System.err.println("Erro no compilador: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erro inesperado: " + e.getMessage());
        }
    }

    private static void runLexicalAnalysis(String filePath, ReadLCCode lcReader, LexicalAnalyzer lexer, SymbolsTable table) throws CompilerException {
        lcReader.readFileAndAnalyze(filePath, lexer);
        System.out.println("Analise lexica concluida!");
        table.printSymbols();
    }

    private static void runSyntacticAnalysis(SymbolsTable table) throws CompilerException {
        SyntaticAnalyzer parser = new SyntaticAnalyzer(table);
        System.out.println("Iniciando análise sintática...");
        parser.parseProgram();
        System.out.println("Análise sintática concluída!");
    }

    private static void runSemanticAnalysis(SymbolsTable table) throws CompilerException {
        SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer(table);
        semanticAnalyzer.analyze();
        System.out.println("Análise semântica concluída!");
    }
}
