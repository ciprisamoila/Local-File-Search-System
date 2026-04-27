package org.example.filebrowser.querylogic;

import org.example.filebrowser.utils.exceptions.ParserException;

public class Lexer {
    private final String input;
    private int pos = 0;

    public Lexer(String input) {
        this.input = input;
    }

    public Token nextToken() {
        skipWhitespace();

        if (pos >= input.length()) {
            return new Token(TokenType.EOF, "");
        }

        char c = input.charAt(pos);

        if (c == '(') {
            pos++;
            return new Token(TokenType.LPAREN, "(");
        }

        if (c == ')') {
            pos++;
            return new Token(TokenType.RPAREN, ")");
        }

        if (Character.isUpperCase(c)) {
            String word = readUppercaseWord();
            switch (word) {
                case "AND": return new Token(TokenType.AND, "AND");
                case "OR": return new Token(TokenType.OR, "OR");
                case "NOT": return new Token(TokenType.NOT, "NOT");
            }
        }

        if (Character.isLowerCase(c)) {
            String command = readCommand();
            return new Token(TokenType.COMMAND, command);
        }

        throw new ParserException("Unrecognized token: " + c);
    }

    private void skipWhitespace() {
        while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
            pos++;
        }
    }

    private String readUppercaseWord() {
        int start = pos;
        while (pos < input.length() && Character.isUpperCase(input.charAt(pos))) {
            pos++;
        }
        return input.substring(start, pos);
    }

    private String readCommand() {
        int start = pos;
        boolean quotation = false;
        boolean finnish = false;
        while (pos < input.length() && !finnish) {
            if ((Character.isWhitespace(input.charAt(pos)) || input.charAt(pos) == ')') && !quotation) {
                break;
            }

            if (input.charAt(pos) == '"') {
                if (quotation) {
                    finnish = true;
                } else {
                    quotation = true;
                }
            }

            pos++;
        }

        return input.substring(start, pos);
    }

    public static void main(String[] args) {
        Lexer lexer = new Lexer("size:>20000 OR (  path:\"A  /B\" AND content:C )AND NOT accessed:<D");
        Token token = lexer.nextToken();
        while (token.getType() != TokenType.EOF) {
            System.out.println(token.getText());
            token = lexer.nextToken();
        }
    }
}
