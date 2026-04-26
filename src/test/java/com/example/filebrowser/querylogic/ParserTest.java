package com.example.filebrowser.querylogic;

import org.example.filebrowser.querylogic.Lexer;
import org.example.filebrowser.querylogic.Parser;
import org.example.filebrowser.querylogic.parser.*;
import org.junit.jupiter.api.Test;

public class ParserTest {
    @Test
    public void testSingleCommand() {
        String input = "a";
        Expr ast = new Parser(new Lexer(input)).parseExpression();

        Expr expected = new CommandExpr("a");

        assert ast.equals(expected);
    }

    @Test
    public void testOrAssociativity() {
        String input = "a OR b OR c";
        Expr ast = new Parser(new Lexer(input)).parseExpression();

        Expr expected = new OrExpr(
                new OrExpr(
                        new CommandExpr("a"),
                        new CommandExpr("b")
                ),
                new CommandExpr("c")
        );

        assert ast.equals(expected);
    }

    @Test
    public void testAndAssociativityExplicit() {
        String input = "a AND b AND c";
        Expr ast = new Parser(new Lexer(input)).parseExpression();

        Expr expected = new AndExpr(
                new AndExpr(
                        new CommandExpr("a"),
                        new CommandExpr("b")
                ),
                new CommandExpr("c")
        );

        assert ast.equals(expected);
    }

    @Test
    public void testImplicitAnd() {
        String input = "a b c";
        Expr ast = new Parser(new Lexer(input)).parseExpression();

        Expr expected = new AndExpr(
                new AndExpr(
                        new CommandExpr("a"),
                        new CommandExpr("b")
                ),
                new CommandExpr("c")
        );

        assert ast.equals(expected);
    }

    @Test
    public void testMixedAnd() {
        String input = "a AND b c";
        Expr ast = new Parser(new Lexer(input)).parseExpression();

        Expr expected = new AndExpr(
                new AndExpr(
                        new CommandExpr("a"),
                        new CommandExpr("b")
                ),
                new CommandExpr("c")
        );

        assert ast.equals(expected);
    }

    @Test
    public void testNotPrecedence() {
        String input = "NOT a AND b";
        Expr ast = new Parser(new Lexer(input)).parseExpression();

        Expr expected = new AndExpr(
                new NotExpr(new CommandExpr("a")),
                new CommandExpr("b")
        );

        assert ast.equals(expected);
    }

    @Test
    public void testMultipleNot() {
        String input = "NOT NOT a";
        Expr ast = new Parser(new Lexer(input)).parseExpression();

        Expr expected = new NotExpr(
                new NotExpr(
                        new CommandExpr("a")
                )
        );

        assert ast.equals(expected);
    }

    @Test
    public void testComplexExpression() {
        String input = "a b OR NOT c AND NOT (d OR e)";
        Expr ast = new Parser(new Lexer(input)).parseExpression();

        Expr expected = new OrExpr(
                new AndExpr(
                        new CommandExpr("a"),
                        new CommandExpr("b")
                ),
                new AndExpr(
                        new NotExpr(new CommandExpr("c")),
                        new NotExpr(
                            new OrExpr(
                                    new CommandExpr("d"),
                                    new CommandExpr("e")
                            )
                        )
                )
        );

        assert ast.equals(expected);
    }

    @Test
    public void testRealValues() {
        String input = "size:>20000 OR (path:A/B AND content:C) AND NOT accessed:<D";
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Expr ast = parser.parseExpression();

        Expr realAst = new OrExpr(
                new CommandExpr("size:>20000"),
                new AndExpr(
                        new AndExpr(
                                new CommandExpr("path:A/B"),
                                new CommandExpr("content:C")
                        ),
                        new NotExpr(new CommandExpr("accessed:<D"))
                )
        );

        assert (ast.equals(realAst));
    }

    @Test
    public void testRealValuesWithSpaces() {
        String input = "size:>20000 OR (path:\"A  /B ()\" AND content:\"C  C\") AND NOT accessed:<D";
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Expr ast = parser.parseExpression();

        Expr realAst = new OrExpr(
                new CommandExpr("size:>20000"),
                new AndExpr(
                        new AndExpr(
                                new CommandExpr("path:\"A  /B ()\""),
                                new CommandExpr("content:\"C  C\"")
                        ),
                        new NotExpr(new CommandExpr("accessed:<D"))
                )
        );

        assert (ast.equals(realAst));
    }
}
