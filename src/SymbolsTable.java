import java.util.HashMap;

class Lexeme {
    String type;
    String value;

    public Lexeme(String type, String value) {
        this.type = type;
        this.value = value;
    }

    @Override
    public String toString() {
        return "Tipo: " + type + ", Valor: " + value;
    }
}

public class SymbolsTable {
    private HashMap<String, Lexeme> lexemes = new HashMap<>();

    public void addSymbol(String symbol, String symbolType) {
        lexemes.put(symbol, new Lexeme(symbolType, symbol));
    }

    public Lexeme getSymbol(String symbol) {
        return lexemes.get(symbol);
    }
}
