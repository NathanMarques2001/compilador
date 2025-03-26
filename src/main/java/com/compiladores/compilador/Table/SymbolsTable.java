package com.compiladores.compilador.Table;

import java.util.Set;
import java.util.ArrayList;

public class SymbolsTable {

    private static final Set<String> reservedWords = Set.of(
            "final", "int", "byte", "string", "while", "if", "else",
            "and", "or", "not", "==", "=", "(", ")",
            "<", ">", "<>", ">=", "<=", ",", "+",
            "-", "*", "/", ";", "begin", "end", "readln",
            "write", "writeln", "true", "false", "boolean");

    private final ArrayList<Symbol> table;

    public SymbolsTable() {
        this.table = new ArrayList<Symbol>();
    }

    public Symbol currentToken(int index) {
        return this.table.get(index);
    }

    public void addSymbol(Symbol symbol) {
        table.add(symbol);
    }

    public boolean isReservedWord(String word) {
        return reservedWords.contains(word);
    }

    public int getSize() {
        return this.table.size();
    }

    public void printSymbols() {
        System.out.println("Tabela de Simbolos:\n===================\n");
        int indexSymbol = 0;
        for (var element : table) {
            System.out.printf("[Token: %s, %s\n", indexSymbol++, element);
        }
    }
}
