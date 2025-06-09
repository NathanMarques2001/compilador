package com.compiladores.compilador.optimizer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementa um otimizador Peephole simples para o código Assembly gerado.
 * Este otimizador examina uma pequena "janela" (peephole) de instruções
 * e substitui sequências conhecidas por outras mais eficientes.
 */
public class PeepholeOptimizer {

    //Otimiza um arquivo .asm de entrada e salva o resultado em um novo arquivo.
    public static void optimizeFile(String inputPath) throws IOException {
        File inputFile = new File(inputPath);
        if (!inputFile.exists()) {
            throw new FileNotFoundException("Arquivo de entrada não encontrado: " + inputFile.getAbsolutePath());
        }

        // Gera o nome do arquivo de saída (ex: "programa.asm" -> "programa_optimized.asm").
        String baseName = inputFile.getName().replaceFirst("[.][^.]+$", "");
        File outputFile = new File(inputFile.getParent(), baseName + "_optimized.asm");

        List<String> originalLines = Files.readAllLines(inputFile.toPath());
        List<String> optimizedLines = optimize(originalLines);
        Files.write(outputFile.toPath(), optimizedLines);

        System.out.println("Arquivo otimizado gerado: " + outputFile.getAbsolutePath());
    }

    // Aplica as regras de otimização Peephole a uma lista de instruções Assembly.
    private static List<String> optimize(List<String> instructions) {
        List<String> optimized = new ArrayList<>();
        int i = 0;

        while (i < instructions.size()) {
            String current = instructions.get(i).trim();

            // Otimização: Remoção de operações de identidade.
            // Ex: ADD EAX, 0 ou MUL EBX, 1 são inúteis.
            if (current.toUpperCase().startsWith("ADD ") && current.endsWith(", 0") ||
                    current.toUpperCase().startsWith("SUB ") && current.endsWith(", 0") ||
                    current.toUpperCase().startsWith("IMUL ") && current.endsWith(", 1")) {
                i++; // Simplesmente pula a instrução.
                continue;
            }

            // Otimização: Redução de força.
            // Multiplicar por 2 é mais lento que um deslocamento de bits para a esquerda (SHL).
            // Ex: IMUL EAX, 2 -> SHL EAX, 1
            if (current.toUpperCase().matches("IMUL \\w+, 2")) {
                String reg = current.split(" ")[1].split(",")[0];
                optimized.add("    shl " + reg + ", 1");
                i++;
                continue;
            }

            // Otimização: Remoção de saltos redundantes.
            // Um salto para a linha imediatamente seguinte é desnecessário.
            // Ex: JMP _label1
            //     _label1:
            if (current.toUpperCase().startsWith("JMP ") && i + 1 < instructions.size()) {
                String next = instructions.get(i + 1).trim();
                String targetLabel = current.split(" ")[1];
                if (next.equalsIgnoreCase(targetLabel + ":")) {
                    i++; // Pula a instrução JMP.
                    continue;
                }
            }

            // Otimização: Remoção de movimentações redundantes (troca inútil).
            // Ex: MOV EAX, EBX
            //     MOV EBX, EAX
            // A sequência acima não altera os valores de EAX e EBX se eles já eram iguais,
            // ou realiza uma troca inútil se os valores eram diferentes e não são mais usados.
            // Esta otimização assume que a troca é redundante.
            if (current.toUpperCase().startsWith("MOV ") && i + 1 < instructions.size()) {
                String[] ops1 = current.split(" ")[1].split(",");
                String next = instructions.get(i + 1).trim();
                if (next.toUpperCase().startsWith("MOV ")) {
                    String[] ops2 = next.split(" ")[1].split(",");
                    // Verifica se a segunda instrução desfaz a primeira: MOV R1, R2 -> MOV R2, R1
                    if (ops1[0].trim().equalsIgnoreCase(ops2[1].trim()) &&
                            ops1[1].trim().equalsIgnoreCase(ops2[0].trim())) {
                        optimized.add("    " + current); // Mantém a primeira instrução e remove a segunda.
                        i += 2; // Pula as duas instruções originais.
                        continue;
                    }
                }
            }

            // Se nenhuma otimização for aplicada, mantém a instrução original.
            optimized.add("    " + current);
            i++;
        }

        return optimized;
    }
}