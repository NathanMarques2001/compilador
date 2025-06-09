package com.compiladores.compilador.symboltable;

import com.compiladores.compilador.lexer.Token;

import java.util.Set;
import java.util.ArrayList;

/**
 * Gerencia todos os tokens (símbolos) do código fonte.
 * Funciona como um repositório central que armazena os tokens na ordem em que aparecem
 * e fornece métodos para acessá-los e validá-los.
 */
public class SymbolsTable {

    // Conjunto de todas as palavras reservadas da linguagem LC
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

    // Busca na tabela de símbolos o tipo de um determinado identificador.
    public String getSymbolType(String symbolName) {
        for (int i = this.table.size() - 1; i >= 0; i--) {
            Token token = this.table.get(i);
            if (token.getName().equals(symbolName)) {
                if (token.getType() != null) {
                    return token.getType();
                }
            }
        }
        return null; // Retorna nulo se o símbolo não for encontrado.
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
