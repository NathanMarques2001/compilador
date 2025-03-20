import java.util.HashMap;
import java.util.Set;

public class SymbolsTable {
    private static final Set<String> reservedWords = Set.of(
            "abstract", "continue", "for", "new", "switch",
            "assert", "default", "goto", "package", "synchronized",
            "boolean", "do", "if", "private", "this",
            "break", "double", "implements", "protected", "throw",
            "byte", "else", "import", "public", "throws",
            "case", "enum", "instanceof", "return", "transient",
            "catch", "extends", "int", "short", "try",
            "char", "final", "interface", "static", "void",
            "class", "finally", "long", "strictfp", "volatile",
            "const", "float", "native", "super", "while");

    private static int id = 0;
    private final HashMap<Integer, Symbol> table;

    public SymbolsTable() {
        this.table = new HashMap<>();
    }

    public void addSymbol(Symbol symbol) {
        id++;
        this.table.put(id, symbol);
    }

    public boolean isReservedWord(String word) {
        return reservedWords.contains(word);
    }
}
