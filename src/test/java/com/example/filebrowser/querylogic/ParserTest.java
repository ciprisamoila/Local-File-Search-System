package com.example.filebrowser.querylogic;

import org.example.filebrowser.querylogic.Lexer;
import org.example.filebrowser.querylogic.Parser;
import org.example.filebrowser.querylogic.parser.*;
import org.example.filebrowser.querymanager.QueryBuilder;
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

    @Test
    public void testSQLName() {
        String input = "name:\"my file\"";
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Expr ast = parser.parseExpression();
        String sql = QueryBuilder.exprToSQL(ast).trim();

        String expected = "(name LIKE '%my file%')";

        assert (sql.equals(expected));
    }

    @Test
    public void testSQLExtension() {
        String input = "extension:mp4";
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Expr ast = parser.parseExpression();
        String sql = QueryBuilder.exprToSQL(ast).trim();

        String expected = "(extension = 'mp4')";

        assert (sql.equals(expected));
    }

    @Test
    public void testSQLPath() {
        String input = "path:\"C:\\Downloads";
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Expr ast = parser.parseExpression();
        String sql = QueryBuilder.exprToSQL(ast).trim();

        String expected = "(path LIKE '%\"C:/Downloads%')";

        assert (sql.equals(expected));
    }

    @Test
    public void testSQLTime() {
        String input = "created:<=2007-10-3T13:00:00 OR modified:2007-10-3T13:00:00..2007-10-13 OR accessed:2007-10-3";
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Expr ast = parser.parseExpression();
        String sql = QueryBuilder.exprToSQL(ast).trim();

        String expected = "( ( (file_creation_time <= '2007-10-03 13:00:00.0') OR (file_last_modified_time BETWEEN '2007-10-03 13:00:00.0' AND '2007-10-13 00:00:00.0') ) OR (file_last_accessed_time >= '2007-10-03' AND file_last_accessed_time < '2007-10-04') )";

        assert (sql.equals(expected));
    }

    @Test
    public void testSQLSize() {
        String input = "size:>20000 OR size:10..100";
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Expr ast = parser.parseExpression();
        String sql = QueryBuilder.exprToSQL(ast).trim();

        String expected = "( (size > 20000) OR (size BETWEEN 10 AND 100) )";

        assert (sql.equals(expected));
    }

    @Test
    public void testSQLReadAccess() {
        String input = "read:true";
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Expr ast = parser.parseExpression();
        String sql = QueryBuilder.exprToSQL(ast).trim();

        String expected = "(read_access = TRUE)";

        assert (sql.equals(expected));
    }

    @Test
    public void testSQLContent() {
        String input = "content:\"ana are mere\"";
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Expr ast = parser.parseExpression();
        String sql = QueryBuilder.exprToSQL(ast).trim();

        String expected = "(ts @@ plainto_tsquery('simple', 'ana are mere'))";

        assert (sql.equals(expected));
    }
}
