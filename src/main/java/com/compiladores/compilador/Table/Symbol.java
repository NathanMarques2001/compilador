package com.compiladores.compilador.Table;

public class Symbol {

    private final String name;
    private final String type;
    private final String classification;

    public Symbol(String name, String classification, String type) {
        this.name = name;
        this.classification = classification;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getClassification() {
        return classification;
    }

    @Override
    public String toString() {
        return String.format("Name: %s, Class: %s, Type: %s]", name, classification, type);
    }
}
