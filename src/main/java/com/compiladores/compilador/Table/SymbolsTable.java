package com.compiladores.compilador.Table;

import com.compiladores.compilador.Lexical.Token;

import java.util.Set;
import java.util.ArrayList;

public class SymbolsTable {

    private static final Set<String> reservedWords = Set.of(
            "final", "int", "byte", "string", "while", "if", "else",
            "and", "or", "not", "==", "=", "(", ")",
            "<", ">", "<>", ">=", "<=", ",", "+",
            "-", "*", "/", ";", "begin", "end", "readln",
            "write", "writeln", "true", "false", "boolean");

    private final ArrayList<Token> table;

    public SymbolsTable() {
        this.table = new ArrayList<Token>();
    }

    public Token currentToken(int index) {
        return this.table.get(index);
    }

    public void addToken(Token token) {
        table.add(token);
    }

    public boolean isReservedWord(String word) {
        return reservedWords.contains(word);
    }

    public int getSize() {
        return this.table.size();
    }

    public void printSymbols() {
        System.out.println("Tabela de Simbolos:\n============================================================================");
        int indexSymbol = 0;
        for (var token : table) {
            System.out.printf("[Token: %s, %s\n", indexSymbol++, token);
        }
        System.out.println("============================================================================");
    }
}
