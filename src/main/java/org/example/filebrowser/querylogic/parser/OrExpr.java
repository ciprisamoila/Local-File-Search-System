package org.example.filebrowser.querylogic.parser;

public record OrExpr(Expr left, Expr right) implements Expr {
}
