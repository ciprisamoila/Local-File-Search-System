package org.example.filebrowser.querylogic;

import org.example.filebrowser.querylogic.parser.*;
import org.example.filebrowser.utils.exceptions.ParserException;

public class Parser {
    private final Lexer lexer;
    private Token current;

    public Parser(Lexer lexer) {
        this.lexer = lexer;
        this.current = lexer.nextToken();
    }

    private void consume(TokenType expected) {
        if (current.getType() != expected) {
            throw new ParserException("Expected " + expected + " but got " + current.getType());
        }
        current = lexer.nextToken();
    }

    public Expr parseExpression() {
        return parseOr();
    }

    private Expr parseOr() {
        Expr left = parseAnd();

        while (current.getType() == TokenType.OR) {
            consume(TokenType.OR);
            Expr right = parseAnd();
            left = new OrExpr(left, right);
        }

        return left;
    }

    private Expr parseAnd() {
        Expr left = parseNot();

        while (isStartOfNotExpr(current) || current.getType() == TokenType.AND) {
            if (current.getType() == TokenType.AND) {
                consume(TokenType.AND);
            }
            Expr right = parseNot();
            left = new AndExpr(left, right);
        }

        return left;
    }

    private Expr parseNot() {
        if (current.getType() == TokenType.NOT) {
            consume(TokenType.NOT);
            return new NotExpr(parseNot());
        }
        return parsePrimary();
    }

    private Expr parsePrimary() {
        if (current.getType() == TokenType.COMMAND) {
            String command = current.getText();
            consume(TokenType.COMMAND);
            return new CommandExpr(command);
        }

        if (current.getType() == TokenType.LPAREN) {
            consume(TokenType.LPAREN);
            Expr expr = parseExpression();
            consume(TokenType.RPAREN);
            return expr;
        }

        throw new ParserException("Unexpected token " + current.getType());
    }

    private boolean isStartOfNotExpr(Token token) {
        return token.getType() == TokenType.NOT ||
                token.getType() == TokenType.LPAREN ||
                token.getType() == TokenType.COMMAND;

    }

    public static void main(String[] args) {
        String input = "size:>20000 OR ( path:\"A  /B\" AND content:C ) AND NOT accessed:<D";
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Expr expr = parser.parseExpression();
    }
}
