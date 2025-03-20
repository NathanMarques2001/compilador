package com.compiladores.compilador.Table;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SymbolsTable {

    private static final Set<String> reservedWords = Set.of(
            "final", "int", "byte", "string", "while", "if", "else",
            "and", "or", "not", "==", "=", "(", ")",
            "<", ">", "<>", ">=", "<=", ",", "+",
            "-", "*", "/", ";", "begin", "end", "readln",
            "write", "writeln", "true", "false", "boolean");

    private int nextId = 1;
    private final Map<Integer, Symbol> table;

    public SymbolsTable() {
        this.table = new ConcurrentHashMap<>();
    }

    public void addSymbol(Symbol symbol) {
        table.put(nextId++, symbol);
    }

    public boolean isReservedWord(String word) {
        return reservedWords.contains(word);
    }

    public void printSymbols() {
        System.out.println("Tabela de Simbolos:\n===================\n");
        for (var entry : table.entrySet()) {
            System.out.printf("[Token: %s, %s\n", entry.getKey(), entry.getValue());
        }
    }
}
