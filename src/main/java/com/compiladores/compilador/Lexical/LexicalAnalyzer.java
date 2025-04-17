package com.compiladores.compilador.Lexical;

import com.compiladores.compilador.Exceptions.ErrorHandler;
import com.compiladores.compilador.Exceptions.CompilerException;
import com.compiladores.compilador.Table.SymbolsTable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LexicalAnalyzer {

    private final SymbolsTable symbolsTable;

    private final Pattern numbers = Pattern.compile("\\d+");
    private final Pattern hexadecimals = Pattern.compile("0h[a-f|0-9]{4}");
    private final Pattern identifiers = Pattern.compile("[a-zA-Z_]\\w*");
    private final Pattern bool = Pattern.compile("true|false");
    private final Pattern operators = Pattern.compile("==|!=|<=|>=|<|>|[+\\-*/=]");
    private final Pattern delimiters = Pattern.compile("[,;()]");
    private final Pattern comments = Pattern.compile("/\\*(.|\\R)*?\\*/|\\{[^\\}]*\\}");
    private final Pattern strings = Pattern.compile("\"([^\"\\\\]|\\\\.)*\"");
    private final Pattern whitespaces = Pattern.compile("\\s+");

    public LexicalAnalyzer(SymbolsTable symbolsTable) {
        this.symbolsTable = symbolsTable;
    }

    public void analyze(String code, int lineNumber) throws CompilerException {
        int columnNumber = 1;

        code = code.stripLeading();

        while (!code.isEmpty()) {
            Matcher matcher;
            boolean matched = false;

            matcher = ignoreLexeme(code);
            if (matcher != null) {
                matched = true;
                columnNumber += matcher.end(); // Atualiza a coluna com base no comprimento ignorado
                code = code.substring(matcher.end()).stripLeading();
                continue;
            }

            if ((matcher = bool.matcher(code)).lookingAt()) {
                symbolsTable.addToken(new Token(this.trueOrFalse(matcher.group()), "CONST", "BOOLEAN", lineNumber, columnNumber));
                matched = true;
            } else if ((matcher = strings.matcher(code)).lookingAt()) {
                symbolsTable.addToken(new Token(matcher.group(), "CONST", "STRING", lineNumber, columnNumber));
                matched = true;
            } else if ((matcher = hexadecimals.matcher(code)).lookingAt()) {
                symbolsTable.addToken(new Token(matcher.group(), "CONST", "BYTE", lineNumber, columnNumber));
                matched = true;
            } else if ((matcher = numbers.matcher(code)).lookingAt()) {
                symbolsTable.addToken(new Token(matcher.group(), "CONST", "INT", lineNumber, columnNumber));
                matched = true;
            } else if ((matcher = isReservedWordsOrID(code)) != null) {
                String lexeme = matcher.group();
                String type;

                if (symbolsTable.isReservedWord(lexeme)) {
                    type = "RESERVED_WORD";
                } else {
                    type = "ID";
                }

                symbolsTable.addToken(new Token(lexeme, type, "NULL", lineNumber, columnNumber));
                matched = true;
            } else {
                ErrorHandler.lexicalError(String.valueOf(code.charAt(0)), lineNumber, columnNumber);
            }

            if (matched) {
                int consumedLength = matcher.end();
                columnNumber += consumedLength; // Atualiza a coluna com base no comprimento do token consumido
                code = code.substring(consumedLength).stripLeading();
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

    private String trueOrFalse(String code) {
        if (code.equals("true")) {
            return "0hFFFF";
        }
        return "0h0000";
    }
}
