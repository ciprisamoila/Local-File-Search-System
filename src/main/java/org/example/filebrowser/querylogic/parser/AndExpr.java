package org.example.filebrowser.querylogic.parser;

public record AndExpr(Expr left, Expr right) implements Expr {
}
