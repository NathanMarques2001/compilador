package com.compiladores.compilador;

import com.compiladores.compilador.Table.SymbolsTable;

/**
 *
 * @author nathan.brandao
 */
public class Main {

    public static void main(String[] args) {
        ReadLCCode lcReader = new ReadLCCode();
        String code = lcReader.readFile("/home/nathanzin/Documentos/dev/compilador/src/main/java/com/compiladores/main.lc");
        // javac -d out -cp src $(find src -name "*.java")
        // java -cp out com.compiladores.compilador.Main /home/nathanzin/Documentos/dev/compilador/src/main/java/com/compiladores/main.lc

        
        //String code = lcReader.readFile(args[0]);

        SymbolsTable table = new SymbolsTable();
        LexicalAnalyzer lexer = new LexicalAnalyzer(table);

        lexer.analyze(code);

        table.printSymbols();
    }
}
