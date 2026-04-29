package org.example.filebrowser.querylogic.parser.expression;

public record AndExpr(Expr left, Expr right) implements Expr {
}
