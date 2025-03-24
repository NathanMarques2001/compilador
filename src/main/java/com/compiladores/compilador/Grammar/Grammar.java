package com.compiladores.compilador.Grammar;

import com.compiladores.compilador.Table.SymbolsTable;
import com.compiladores.compilador.Table.Symbol;

public class Grammar {

    private SymbolsTable symbolsTable;
    private Symbol currentToken;
    private int currentTokenIndex = 0;

    public Grammar(SymbolsTable symbolsTable) {
        this.symbolsTable = symbolsTable;
        this.currentToken = symbolsTable.currentToken(this.currentTokenIndex);
    }

    public void analyze() {
        try {
            this.declarationGeneration();
            this.beginGeneration();
            // this.expectName("end");
            System.out.println("Analise sintatica concluida com sucesso!");
        } catch (Exception e) {
            System.err.println("Erro de analise sintatica: " + e.getMessage());
        }
    }

    private void nextToken() {
        System.out.println(this.currentToken.getName());
        this.currentToken = this.symbolsTable.currentToken(++this.currentTokenIndex);
    }

    private void expectClassification(String expected) throws Exception {
        if (!currentToken.getClassification().equals(expected) && !currentToken.getName().equals(expected)) {
            throw new Exception(
                    "Esperado '" + expected + "' mas encontrado '" + currentToken.getClassification() + "'");
        }
    }

    private void expectName(String expected) throws Exception {
        if (!currentToken.getName().equals(expected)) {
            throw new Exception("Esperado '" + expected + "' mas encontrado '" + currentToken.getName() + "'");
        }
    }

    private void declarationGeneration() throws Exception {
        while (currentToken.getName().equals("int") || currentToken.getName().equals("string")
                || currentToken.getName().equals("boolean") || currentToken.getName().equals("final")) {
            this.nextToken();

            // TYPE -> ID
            this.expectClassification("ID");
            this.nextToken();

            // ID -> =
            if (currentToken.getName().equals("=")) {
                this.nextToken();
                // = -> CONST
                this.expectClassification("CONST");
                this.nextToken();
            }
            // ID | CONST -> ;
            this.expectName(";");
            this.nextToken();
        }
    }

    // begin -> bloco de codigo
    private void beginGeneration() throws Exception {
        this.expectName("begin");
        this.nextToken();
        this.statements();
    }

    private void statements() throws Exception {
        while (!currentToken.getName().equals("end")) {
            if (this.currentToken.getName().equals("write") || this.currentToken.getName().equals("writeln")) {
                this.writeGeneration();
            }
            if (this.currentToken.getName().equals("readln")) {
                this.readGeneration();
            }
            if (this.currentToken.getClassification().equals("ID")) {
                this.assignmentGeneration();
            }
            if (this.currentToken.getName().equals("while")) {
                this.whileGeneration();
            }
        }
        System.out.println(this.currentToken.getName());
    }

    private void writeGeneration() throws Exception {
        this.nextToken();
        while (!this.currentToken.getName().equals(";")) {
            // write | writeln -> ,
            this.expectName(",");
            this.nextToken();
            // , -> ID | CONST
            if (!this.currentToken.getClassification().equals("ID")
                    && !this.currentToken.getClassification().equals("CONST")) {
                throw new Exception("Esperado uma STRING mas encontrado '" + currentToken.getName() + "'");
            }
            // ID | CONST -> , | ;
            this.nextToken();
        }
        // ;
        this.expectName(";");
        this.nextToken();
    }

    private void readGeneration() throws Exception {
        this.nextToken();
        // read -> ,
        this.expectName(",");
        this.nextToken();
        // , -> ID
        this.expectClassification("ID");
        this.nextToken();
        // ID -> ;
        this.expectName(";");
        this.nextToken();
    }

    private void assignmentGeneration() throws Exception {
        this.nextToken();
        // ID -> =
        this.expectName("=");
        this.nextToken();
        // = -> CONST | ID | true | false
        if (!this.currentToken.getClassification().equals("CONST")
                && !this.currentToken.getClassification().equals("ID")
                && !this.currentToken.getName().equals("true") && !this.currentToken.getName().equals("false")) {
            throw new Exception(
                    "Atribuicao feita de maneira indevida. Nao eh possivel atribuir '" + currentToken.getName() + "'");
        }
        this.nextToken();
        // CONST -> + | < | <= | > | >= | -
        if (this.currentToken.getName().equals("+") || this.currentToken.getName().equals("<")
                || this.currentToken.getName().equals("<=") || this.currentToken.getName().equals(">")
                || this.currentToken.getName().equals(">=") || this.currentToken.getName().equals("-")) {
            this.nextToken();
            // + | < | <= | > | >= | - -> CONST | ID
            if (!this.currentToken.getClassification().equals("CONST")
                    && !this.currentToken.getClassification().equals("ID")) {
                throw new Exception(
                        "Operacao indevida! Esperado uma CONST ou ID e encontrado '" + currentToken.getName() + "'");
            }
            this.nextToken();
        }
        // CONST -> ;
        this.expectName(";");
        this.nextToken();
    }

    private void whileGeneration() throws Exception {
        this.nextToken();
        // while -> ID
        this.expectClassification("ID");
        this.nextToken();
        // ID -> begin
        this.beginGeneration();
    }
}
