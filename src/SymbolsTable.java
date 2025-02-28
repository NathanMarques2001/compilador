import java.util.HashMap;
import java.util.Set;

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

    // Conjunto de palavras reservadas do Java
    private static final Set<String> RESERVED_WORDS = Set.of(
            "abstract", "continue", "for", "new", "switch",
            "assert", "default", "goto", "package", "synchronized",
            "boolean", "do", "if", "private", "this",
            "break", "double", "implements", "protected", "throw",
            "byte", "else", "import", "public", "throws",
            "case", "enum", "instanceof", "return", "transient",
            "catch", "extends", "int", "short", "try",
            "char", "final", "interface", "static", "void",
            "class", "finally", "long", "strictfp", "volatile",
            "const", "float", "native", "super", "while"
    );

    public void addSymbol(String symbol, String symbolType) {
        if (isReservedWord(symbol)) throw new IllegalArgumentException("Erro: '" + symbol + "' é uma palavra reservada e não pode ser um identificador.");

        lexemes.put(symbol, new Lexeme(symbolType, symbol));
    }

    public Lexeme getSymbol(String symbol) {
        return lexemes.get(symbol);
    }

    private boolean isReservedWord(String symbol) {
        return RESERVED_WORDS.contains(symbol);
    }
}
