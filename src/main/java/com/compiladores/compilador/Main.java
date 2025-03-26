package com.compiladores.compilador;

import com.compiladores.compilador.Exceptions.CompilerException;
import com.compiladores.compilador.Grammar.Grammar;
import com.compiladores.compilador.Table.SymbolsTable;

public class Main {

    public static void main(String[] args) throws CompilerException {
        ReadLCCode lcReader = new ReadLCCode();
        String code = lcReader.readFile("C:\\Users\\NATHAN.BRANDAO\\Documents\\dev\\compilador\\src\\main\\java\\com\\compiladores\\main.lc");
        // javac -d out -cp src $(find src -name "*.java")
        // java -cp out com.compiladores.compilador.Main /home/nathanzin/Documentos/dev/compilador/src/main/java/com/compiladores/main.lc

        
        //String code = lcReader.readFile(args[0]);

        SymbolsTable table = new SymbolsTable();
        
        LexicalAnalyzer lexer = new LexicalAnalyzer(table);
        lexer.analyze(code);
        table.printSymbols();
        
        Grammar parser = new Grammar(table);
        parser.analyze();
    }
}
