import java.util.regex.Pattern;

public class LexicalAnalyzer {
    // Tem que comecar com letra e aceita letras, numeros e undescores, n vezes
    private final Pattern wordCharacter = Pattern.compile("[a-zA-Z_]\\w*", Pattern.CASE_INSENSITIVE);
    private final Pattern number = Pattern.compile("[0-9]", Pattern.CASE_INSENSITIVE);
    private String symbolType;
    public SymbolsTable symbolTable = new SymbolsTable();

    public LexicalAnalyzer(String input) {
        if (this.isID(input)) this.symbolType = "ID";

        if (symbolType == null)
            throw new IllegalArgumentException("Erro léxico: '" + input + "' não é um identificador válido.");

        this.symbolTable.addSymbol(input, this.symbolType);
    }

    private boolean isID(String input) {
        if (wordCharacter.matcher(input).matches()) {
            if (number.matcher(input.substring(0, 1)).matches()) {
                throw new IllegalArgumentException("Erro léxico: ");
            }
            return true;
        }
        return false;
    }
}
