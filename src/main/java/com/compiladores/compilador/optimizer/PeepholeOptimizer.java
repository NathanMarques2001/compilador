package com.compiladores.compilador.optimizer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class PeepholeOptimizer {

    /**
     * Otimiza um arquivo .asm aplicando técnicas simples de peephole.
     *
     * @param inputPath Caminho completo do arquivo .asm.
     * @throws IOException Se não encontrar ou falhar na leitura/escrita.
     */
    public static void optimizeFile(String inputPath) throws IOException {
        File inputFile = new File(inputPath);
        if (!inputFile.exists()) {
            throw new FileNotFoundException("Arquivo de entrada não encontrado: " + inputFile.getAbsolutePath());
        }

        // Gera nome do arquivo otimizado com base no original
        String baseName = inputFile.getName().replaceFirst("[.][^.]+$", "");
        File outputFile = new File(inputFile.getParent(), baseName + "_optimized.asm");

        List<String> originalLines = Files.readAllLines(inputFile.toPath());
        List<String> optimizedLines = optimize(originalLines);
        Files.write(outputFile.toPath(), optimizedLines);

        System.out.println("Arquivo otimizado gerado: " + outputFile.getAbsolutePath());
    }

    /**
     * Executa otimizações simples linha a linha no código assembly.
     */
    private static List<String> optimize(List<String> instructions) {
        List<String> optimized = new ArrayList<>();
        int i = 0;

        while (i < instructions.size()) {
            String current = instructions.get(i).trim();

            if (current.matches("ADD .*?, 0") || current.matches("SUB .*?, 0") ||
                    current.matches("MUL .*?, 1") || current.matches("DIV .*?, 1")) {
                i++;
                continue;
            }

            if (current.matches("MUL (\\w+), 2")) {
                String var = current.split(" ")[1].replace(",", "");
                optimized.add("SHL " + var + ", 1");
                i++;
                continue;
            }

            if (current.matches("JMP \\w+") && i + 1 < instructions.size()) {
                String next = instructions.get(i + 1).trim();
                String targetLabel = current.split(" ")[1];
                if (next.equals(targetLabel + ":")) {
                    i++;
                    continue;
                }
            }

            if (current.matches("MOV \\w+, \\w+") && i + 1 < instructions.size()) {
                String[] ops1 = current.split(" ")[1].split(",");
                String next = instructions.get(i + 1).trim();
                if (next.matches("MOV \\w+, \\w+")) {
                    String[] ops2 = next.split(" ")[1].split(",");
                    if (ops1[0].trim().equals(ops2[1].trim()) &&
                            ops1[1].trim().equals(ops2[0].trim())) {
                        i += 2;
                        continue;
                    }
                }
            }

            optimized.add(current);
            i++;
        }

        return optimized;
    }
}