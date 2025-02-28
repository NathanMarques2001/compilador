public class Main {
    public static void main(String[] args) {
        try {
            LexicalAnalyzer lexer = new LexicalAnalyzer("variavel123");
            LexicalAnalyzer lexer2 = new LexicalAnalyzer("String");
            //LexicalAnalyzer lexer2 = new LexicalAnalyzer("2invalido"); // Não é um ID válido

            System.out.println(lexer.symbolTable.getSymbol("variavel123")); // Tipo: ID, Valor: variavel123
            System.out.println(lexer2.symbolTable.getSymbol("String"));  // Tipo: DESCONHECIDO, Valor: 2invalido
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
