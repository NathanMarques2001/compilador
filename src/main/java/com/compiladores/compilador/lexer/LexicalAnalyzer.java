package com.compiladores.compilador.lexer;

import com.compiladores.compilador.exceptions.CompilerException;
import com.compiladores.compilador.exceptions.ErrorHandler;
import com.compiladores.compilador.symboltable.SymbolsTable;
import com.compiladores.compilador.utils.TokenUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Realiza a análise léxica do código fonte.
 * Responsável por ler o código caractere por caractere (através de uma linha inteira),
 * identificar padrões (lexemas) e convertê-los em tokens.
 */
public class LexicalAnalyzer {

    private final SymbolsTable symbolsTable;

    // Expressões Regulares para identificar os padrões da linguagem

    private final Pattern numbers = Pattern.compile("\\d+"); // Encontra sequências de dígitos.
    private final Pattern hexadecimals = Pattern.compile("0h[a-zA-Z0-9]*"); // Encontra hexadecimais no formato 0hHH.
    private final Pattern identifiers = Pattern.compile("[a-zA-Z_]\\w*"); // Encontra identificadores (começa com letra ou _, seguido por letras, dígitos ou _).
    private final Pattern bool = Pattern.compile("true|false", Pattern.CASE_INSENSITIVE); // Encontra "true" ou "false".
    private final Pattern operators = Pattern.compile("==|<>|<=|>=|<|>|[+\\-*/=]"); // Encontra operadores relacionais e aritméticos.
    private final Pattern delimiters = Pattern.compile("[,;()]"); // Encontra delimitadores.
    private final Pattern comments = Pattern.compile("/\\*(.|\\R)*?\\*/|\\{[^\\}]*\\}"); // Encontra comentários de bloco (/*...*/ ou {...}).
    private final Pattern strings = Pattern.compile("\"([^\"\\\\\\r\\n]|\\\\.)*\""); // Encontra literais string entre aspas.
    private final Pattern whitespaces = Pattern.compile("\\s+"); // Encontra espaços em branco.

    public LexicalAnalyzer(SymbolsTable symbolsTable) {
        this.symbolsTable = symbolsTable;
    }

    // Analisa uma única linha de código, transformando-a em tokens.
    public void analyze(String code, int lineNumber) throws CompilerException {
        int columnNumber = 1;
        code = code.stripLeading();

        while (!code.isEmpty()) {
            Matcher matcher = null;
            boolean matched = false;
            int consumedLength = 0;

            // Ignorar espaços e comentários
            matcher = ignoreLexeme(code);
            if (matcher != null && matcher.lookingAt()) {
                consumedLength = matcher.end();
                matched = true;
            } else if (code.startsWith("\"")) {
                Matcher stringMatcher = strings.matcher(code);
                if (stringMatcher.lookingAt()) {
                    String lexeme = stringMatcher.group();

                    if (lexeme.length() > 255) {
                        ErrorHandler.lexicalErrorStringTooLong(lineNumber, columnNumber);
                    }

                    symbolsTable.addToken(new Token(lexeme, "const", "string", lineNumber, columnNumber));
                    consumedLength = stringMatcher.end();
                    matched = true;
                } else {
                    ErrorHandler.lexicalErrorBreakLine(lineNumber, columnNumber);
                }
            } else if ((matcher = bool.matcher(code)).lookingAt()) {
                symbolsTable.addToken(new Token(resolveBoolean(matcher.group()), "const", "boolean", lineNumber, columnNumber));
                consumedLength = matcher.end();
                matched = true;
            } else if ((matcher = hexadecimals.matcher(code)).lookingAt()) {
                String hexLexeme = matcher.group();
                String hexValue = hexLexeme.substring(2);
                if (!hexValue.matches("[a-fA-F0-9]{1,2}")) {
                    ErrorHandler.lexicalErrorInvalidHexByte(hexLexeme, lineNumber, columnNumber);
                }
                symbolsTable.addToken(new Token(hexLexeme, "const", "byte", lineNumber, columnNumber));
                consumedLength = matcher.end();
                matched = true;
            } else if ((matcher = numbers.matcher(code)).lookingAt()) {
                String intLexeme = matcher.group();
                int value = Integer.parseInt(intLexeme);
                if (value < -32768 || value > 32767) {
                    ErrorHandler.lexicalErrorIntOutOfRange(intLexeme, lineNumber, columnNumber);
                }
                symbolsTable.addToken(new Token(intLexeme, "const", "int", lineNumber, columnNumber));
                consumedLength = matcher.end();
                matched = true;
            } else if ((matcher = matchReservedOrID(code)) != null && matcher.lookingAt()) {
                String lexeme = matcher.group();
                String lexemeLower = lexeme.toLowerCase();

                if (lexemeLower.length() > 255) {
                    ErrorHandler.lexicalErrorIdentifierTooLong(lexeme, lineNumber, columnNumber);
                }

                String type = symbolsTable.isReservedWord(lexemeLower) ? "reserved_word" : "id";
                symbolsTable.addToken(new Token(lexeme, type, "null", lineNumber, columnNumber));
                consumedLength = matcher.end();
                matched = true;
            } else {
                ErrorHandler.lexicalErrorInvalidSymbol(code.charAt(0), lineNumber, columnNumber);
            }

            if (matched) {
                columnNumber += consumedLength;
                code = code.substring(consumedLength).stripLeading();
            }
        }
    }

    // Tenta encontrar correspondência com padrões que devem ser ignorados (espaços e comentários).
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

    // Tenta encontrar correspondência com identificadores, operadores ou delimitadores.
    private Matcher matchReservedOrID(String code) {
        Matcher matcher = identifiers.matcher(code);
        if (matcher.lookingAt()) return matcher;

        matcher = operators.matcher(code);
        if (matcher.lookingAt()) return matcher;

        matcher = delimiters.matcher(code);
        if (matcher.lookingAt()) return matcher;

        return null;
    }

    // Converte os literais "true" e "false" para seus equivalentes hexadecimais, conforme a especificação.
    private String resolveBoolean(String code) {
        return code.equalsIgnoreCase("true") ? "Fh" : "0h";
    }
}