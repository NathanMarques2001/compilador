package com.compiladores.compilador;

import com.compiladores.compilador.Table.Symbol;
import com.compiladores.compilador.Table.SymbolsTable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LexicalAnalyzer {

    private final SymbolsTable symbolsTable;

    // Regex para cada tipo de token
    private final Pattern numbers = Pattern.compile("\\d+");
    private final Pattern identifiers = Pattern.compile("[a-zA-Z_]\\w*");
    private final Pattern operators = Pattern.compile("==|!=|<=|>=|<|>|[+\\-*/=]");
    private final Pattern delimiters = Pattern.compile("[,;()]");
    private final Pattern comments = Pattern.compile("/\\*(.|\\R)*?\\*/|\\{[^\\}]*\\}");
    private final Pattern strings = Pattern.compile("\"([^\"\\\\]|\\\\.)*\"");
    private final Pattern whitespaces = Pattern.compile("\\s+");

    public LexicalAnalyzer(SymbolsTable symbolsTable) {
        this.symbolsTable = symbolsTable;
    }

    public void analyze(String code) {
        code = code.stripLeading();

        while (!code.isEmpty()) {
            Matcher matcher;
            boolean matched = false;

            matcher = ignoreLexeme(code);
            if (matcher != null) {
                matched = true;
            } else if ((matcher = strings.matcher(code)).lookingAt()) {
                symbolsTable.addSymbol(new Symbol(matcher.group(), "CONST", "STRING"));
                matched = true;
            } else if ((matcher = numbers.matcher(code)).lookingAt()) {
                symbolsTable.addSymbol(new Symbol(matcher.group(), "CONST", "NUMBER"));
                matched = true;
            } else if ((matcher = isReservedWordsOrID(code)) != null) {
                String lexeme = matcher.group();
                String type;

                if (symbolsTable.isReservedWord(lexeme)) {
                    type = "RESERVED_WORD";
                } else if (delimiters.matcher(lexeme).matches()) {
                    type = "DELIMITER";  // Agora identificando delimitadores corretamente
                } else {
                    type = "ID";
                }

                symbolsTable.addSymbol(new Symbol(lexeme, type, "NULL"));
                matched = true;
            } else {
                System.err.println("Invalid token found: " + code.charAt(0));
                code = code.substring(1).stripLeading();
                continue;
            }

            // Avançar no código se um token foi reconhecido
            if (matched) {
                code = code.substring(matcher.end()).stripLeading();
            }
        }
    }

    /**
     * Ignora espaços em branco e comentários.
     */
    private Matcher ignoreLexeme(String code) {
        Matcher matcher = whitespaces.matcher(code);
        if (matcher.lookingAt()) {
            return matcher;
        }
        matcher = comments.matcher(code);
        if (matcher.lookingAt()) {
            return matcher;
        }
        return null;
    }

    /**
     * Identifica palavras reservadas, identificadores, operadores e
     * delimitadores.
     */
    private Matcher isReservedWordsOrID(String code) {
        Matcher matcher = identifiers.matcher(code);
        if (matcher.lookingAt()) {
            return matcher;
        }
        matcher = operators.matcher(code);
        if (matcher.lookingAt()) {
            return matcher;
        }
        matcher = delimiters.matcher(code);
        if (matcher.lookingAt()) {
            return matcher;
        }
        return null;
    }

    public static void main(String[] args) {
        ReadLCCode lcReader = new ReadLCCode();
        String code = lcReader.readFile("C:\\Users\\NATHAN.BRANDAO\\Documents\\NetBeansProjects\\compilador\\src\\main\\java\\com\\compiladores\\main.lc");

        SymbolsTable table = new SymbolsTable();
        LexicalAnalyzer lexer = new LexicalAnalyzer(table);

        lexer.analyze(code);

        table.printSymbols();
    }
}
