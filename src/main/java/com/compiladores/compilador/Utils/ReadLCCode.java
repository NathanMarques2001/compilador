package com.compiladores.compilador.Utils;

import com.compiladores.compilador.Exceptions.CompilerException;
import com.compiladores.compilador.Lexical.LexicalAnalyzer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class ReadLCCode {

    public String readFile(String filePath) {
        StringBuilder content = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler o arquivo: " + e.getMessage());
        }

        return content.toString();
    }

    public void readFileAndAnalyze(String filePath, LexicalAnalyzer lexicalAnalyzer) throws CompilerException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                // Remove espaços em branco à direita para evitar problemas de coluna
                line = line.stripTrailing();

                // Envia a linha para o analisador léxico
                lexicalAnalyzer.analyze(line, lineNumber);

                // Incrementa o número da linha
                lineNumber++;
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler o arquivo: " + e.getMessage());
        }
    }
}
