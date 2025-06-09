package com.compiladores.compilador.io;

import com.compiladores.compilador.exceptions.CompilerException;
import com.compiladores.compilador.lexer.LexicalAnalyzer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

// Responsável pela leitura do arquivo de código-fonte (.LC).
public class ReadLCCode {

    /**
     * Lê um arquivo linha por linha e envia cada linha para o analisador léxico.
     * Esta é a abordagem principal para iniciar a compilação.
     */
    public void readFileAndAnalyze(String filePath, LexicalAnalyzer lexicalAnalyzer) throws CompilerException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                // Remove espaços em branco à direita para evitar problemas de contagem de colunas.
                line = line.stripTrailing();

                // Envia a linha para o analisador léxico para ser tokenizada.
                lexicalAnalyzer.analyze(line, lineNumber);

                // Incrementa o contador de linha para a próxima iteração.
                lineNumber++;
            }
        } catch (IOException e) {
            // Em caso de erro de leitura de arquivo, exibe uma mensagem no erro padrão.
            System.err.println("Erro ao ler o arquivo: " + e.getMessage());
        }
    }
}