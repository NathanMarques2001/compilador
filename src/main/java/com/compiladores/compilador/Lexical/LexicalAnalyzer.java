package com.compiladores.compilador.Lexical;

import com.compiladores.compilador.Exceptions.CompilerException;
import com.compiladores.compilador.Exceptions.ErrorHandler;
import com.compiladores.compilador.Table.SymbolsTable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LexicalAnalyzer {

    private final SymbolsTable symbolsTable;

    private final Pattern numbers = Pattern.compile("\\d+");
    private final Pattern hexadecimals = Pattern.compile("0h[a-fA-F0-9]+");
    private final Pattern identifiers = Pattern.compile("[a-zA-Z_]\\w*");
    private final Pattern bool = Pattern.compile("true|false", Pattern.CASE_INSENSITIVE);
    private final Pattern operators = Pattern.compile("==|<>|<=|>=|<|>|[+\\-*/=]");
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
                columnNumber += matcher.end();
                code = code.substring(matcher.end()).stripLeading();
                continue;
            }

            if ((matcher = bool.matcher(code)).lookingAt()) {
                symbolsTable.addToken(new Token(resolveBoolean(matcher.group()), "const", "boolean", lineNumber, columnNumber));
                matched = true;

            } else if ((matcher = strings.matcher(code)).lookingAt()) {
                String lexeme = matcher.group();

                if (lexeme.length() > 255) {
                    ErrorHandler.lexicalErrorStringTooLong(lineNumber, columnNumber);
                }
                if (lexeme.contains("\n") || lexeme.contains("\r")) {
                    ErrorHandler.lexicalErrorBreakLine(lineNumber, columnNumber);
                }

                symbolsTable.addToken(new Token(lexeme, "const", "string", lineNumber, columnNumber));
                matched = true;

            } else if ((matcher = hexadecimals.matcher(code)).lookingAt()) {
                String hexLexeme = matcher.group();
                String hexValue = hexLexeme.substring(2);

                if (hexValue.length() > 2) {
                    ErrorHandler.lexicalErrorInvalidHexByte(hexLexeme, lineNumber, columnNumber);
                }

                symbolsTable.addToken(new Token(hexLexeme, "const", "byte", lineNumber, columnNumber));
                matched = true;

            } else if ((matcher = numbers.matcher(code)).lookingAt()) {
                String intLexeme = matcher.group();

                int value = Integer.parseInt(intLexeme);
                if (value < -32768 || value > 32767) {
                    ErrorHandler.lexicalErrorIntOutOfRange(intLexeme, lineNumber, columnNumber);
                }

                symbolsTable.addToken(new Token(intLexeme, "const", "int", lineNumber, columnNumber));
                matched = true;

            } else if ((matcher = matchReservedOrID(code)) != null) {
                String lexeme = matcher.group();
                String lexemeLower = lexeme.toLowerCase();

                if (lexemeLower.length() > 255) {
                    ErrorHandler.lexicalErrorIdentifierTooLong(lexeme, lineNumber, columnNumber);
                }

                String type = symbolsTable.isReservedWord(lexemeLower) ? "reserved_word" : "id";
                symbolsTable.addToken(new Token(lexeme, type, "null", lineNumber, columnNumber));
                matched = true;

            } else {
                ErrorHandler.lexicalErrorInvalidSymbol(code.charAt(0), lineNumber, columnNumber);
            }

            if (matched) {
                int consumedLength = matcher.end();
                columnNumber += consumedLength;
                code = code.substring(consumedLength).stripLeading();
            }
        }
    }

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

    private Matcher matchReservedOrID(String code) {
        Matcher matcher = identifiers.matcher(code);
        if (matcher.lookingAt()) return matcher;

        matcher = operators.matcher(code);
        if (matcher.lookingAt()) return matcher;

        matcher = delimiters.matcher(code);
        if (matcher.lookingAt()) return matcher;

        return null;
    }

    private String resolveBoolean(String code) {
        return code.equalsIgnoreCase("true") ? "Fh" : "0h";
    }
}
