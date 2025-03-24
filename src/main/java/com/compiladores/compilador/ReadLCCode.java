package com.compiladores.compilador;

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
            System.err.println("Error reading file: " + e.getMessage());
        }

        return content.toString();
    }
}
