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

/**
 * Classe principal que executa o processo de compilação.
 * Ela inicializa todos os componentes do compilador e executa cada fase
 * em sequência: Léxica, Sintática, Semântica, Geração de Código e Otimização.
 */
public class Main {

    public static void main(String[] args) {
        try {
            // Códigos com erros utilizados para testes
            String[] codigosLCComErros = {
                    "errors/erro_lexico_hex_invalido.lc"
                    , "errors/erro_lexico_identificador_fora_limite.lc"
                    , "errors/erro_lexico_int_fora_limite.lc"
                    , "errors/erro_lexico_quebra_linha_string.lc"
                    , "errors/erro_lexico_simbolo_invalido.lc"
                    , "errors/erro_lexico_string_fora_limite.lc"
                    , "errors/erro_semantico_atribuicao_constante.lc"
                    , "errors/erro_semantico_condicao_while_invalida.lc"
                    , "errors/erro_semantico_tipos_incompativeis.lc"
                    , "errors/erro_semantico_variavel_nao_declarada.lc"
                    , "errors/erro_sintatico_atribuicao_logica.lc"
                    , "errors/erro_sintatico_falta_end.lc"
                    , "errors/erro_sintatico_falta_ponto_virgula.lc"
                    , "errors/erro_sintatico_if_sem_bloco.lc"
            };

            // Códigos corretos utilizados para testes
            String[] codigosLCSemErros = {
                    "successes/calculadora.lc"
                    , "successes/main-corrigido.lc"
                    , "successes/toda-gramatica.lc"
            };

            // Define o caminho completo do arquivo de entrada .lc
            // 7 - erro_semantico_condicao_while_invalida

            String filePath = "src/main/java/com/compiladores/compilador/io/LC_Codes/" + codigosLCComErros[7];

            File inputFile = new File(filePath);
            if (!inputFile.exists()) {
                System.err.println("Arquivo não encontrado: " + inputFile.getAbsolutePath());
                return;
            }

            // Extrai o nome do arquivo sem a extensão para usá-lo no arquivo de saída.
            String fileName = inputFile.getName().replaceFirst("[.][^.]+$", "");

            // Constrói o caminho completo para o arquivo de saída .asm.
            String outputDir = "src/main/java/com/compiladores/compilador/codegen/out";
            String asmFilePath = outputDir + File.separator + fileName + ".asm";

            System.out.println("Lendo o arquivo: " + inputFile.getAbsolutePath());

            // --- Início do Pipeline de Compilação ---

            // Instancia os componentes principais do compilador.
            SymbolsTable table = new SymbolsTable();
            LexicalAnalyzer lexer = new LexicalAnalyzer(table);
            ReadLCCode reader = new ReadLCCode();

            // Executa cada fase sequencialmente.
            runLexicalAnalysis(inputFile.getPath(), reader, lexer, table);
            runSyntacticAnalysis(table);
            runSemanticAnalysis(table);
            runAssemblyGeneration(table, fileName);
            runPeepholeOptimizer(asmFilePath);

            System.out.println("\nCompilação finalizada com sucesso!");

        } catch (CompilerException | IOException e) {
            System.out.println();
            System.err.println(e.getMessage());
        }
    }

    // Encapsula a execução da análise léxica.
    private static void runLexicalAnalysis(String path, ReadLCCode reader, LexicalAnalyzer lexer, SymbolsTable table) throws CompilerException {
        System.out.println("\n=== Iniciando Análise Léxica ===");
        reader.readFileAndAnalyze(path, lexer);
        // table.printSymbols(); // Descomente para depurar a tabela de símbolos após a análise léxica.
        System.out.println("Análise Léxica concluída.");
    }

    // Encapsula a execução da análise sintática.
    private static void runSyntacticAnalysis(SymbolsTable table) throws CompilerException {
        System.out.println("\n=== Iniciando Análise Sintática ===");
        new SyntaticAnalyzer(table).parseProgram();
        System.out.println("Análise Sintática concluída.");
    }

    // Encapsula a execução da análise semântica.
    private static void runSemanticAnalysis(SymbolsTable table) throws CompilerException {
        System.out.println("\n=== Iniciando Análise Semântica ===");
        new SemanticAnalyzer(table).analyze();
        // table.printSymbols(); // Descomente para depurar a tabela de símbolos após a análise semântica.
        System.out.println("Análise Semântica concluída.");
    }

    // Encapsula a execução da geração de código Assembly.
    private static void runAssemblyGeneration(SymbolsTable table, String fileName) throws CompilerException {
        System.out.println("\n=== Iniciando Geração de Código Assembly ===");
        new AssemblyGenerator(table, fileName).convert();
        System.out.println("Geração de Código concluída.");
    }

    // Encapsula a execução do otimizador Peephole.
    private static void runPeepholeOptimizer(String asmFilePath) throws IOException {
        System.out.println("\n=== Iniciando Otimização Peephole ===");
        PeepholeOptimizer.optimizeFile(asmFilePath);
        System.out.println("Otimização concluída.");
    }
}