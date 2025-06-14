package com.compiladores.compilador.lexer;

/**
 * Representa um token, a unidade fundamental de código fonte para o compilador.
 * Cada token possui um nome (o lexema), uma classificação, um tipo de dado,
 * e sua localização (linha e coluna) no arquivo fonte original.
 */
public class Token {

    private final String name;
    private String type;
    private final String classification;
    private final int line;
    private final int column;

    public Token(String name, String classification, String type, int line, int column) {
        this.name = name;
        this.classification = classification;
        this.type = type;
        this.line = line;
        this.column = column;
    }

    public String getName() {
        return this.name;
    }

    public String getType() {
        return this.type;
    }

    public String getClassification() {
        return this.classification;
    }

    public int getLine() {
        return this.line;
    }

    public int getColumn() {
        return this.column;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return String.format("Name: %s, Class: %s, Type: %s, Line: %d, Column: %d]", this.name, this.classification,
                this.type, this.line, this.column);
    }
}