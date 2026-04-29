package org.example.filebrowser.querymanager;

import org.example.filebrowser.querylogic.parser.Lexer;
import org.example.filebrowser.querylogic.parser.Parser;
import org.example.filebrowser.querylogic.parser.expression.*;
import org.example.filebrowser.utils.exceptions.ParserException;

import java.sql.Date;
import java.sql.Timestamp;

public class QueryBuilder {
    private static String exprToSQL(OrExpr orExpr) {
        return " (" + exprToSQL(orExpr.left()) + "OR" + exprToSQL(orExpr.right()) + ") ";
    }
    private static String exprToSQL(AndExpr andExpr) {
        return " (" + exprToSQL(andExpr.left()) + "AND" + exprToSQL(andExpr.right()) + ") ";
    }
    private static String exprToSQL(NotExpr notExpr) {
        return " ( NOT" + exprToSQL(notExpr.expr()) + ") ";
    }

    private static String parseNameCommand(String command) {
        if (command.startsWith("\"") && command.endsWith("\"")) {
            command = command.substring(1, command.length() - 1);
        } else if (command.endsWith("\"")) {
            throw new ParserException("Wrong quotations");
        }
        return "name LIKE '%" + command + "%'";
    }
    private static String parseExtensionCommand(String command) {
        return "extension = '" + command + "'";
    }
    private static String parsePathCommand(String command) {
        if (command.startsWith("\"") && command.endsWith("\"")) {
            command = command.substring(1, command.length() - 1);
        } else if (command.endsWith("\"")) {
            throw new ParserException("Wrong quotations");
        }
        return "path LIKE '%" + command.replace("\\", "/") + "%'";
    }
    private static boolean containsTime(String command) {
        return command.contains("T");
    }
    private static Timestamp getTimestamp(String str) {
        if (containsTime(str)) {
            return Timestamp.valueOf(str.replace("T", " "));
        }
        return Timestamp.valueOf(str + " 00:00:00");
    }
    private static String parseTimeCommand(String field, String command) {
        try {
            if (command.contains("..")) {
                String[] split = command.split("\\.\\.");

                Timestamp timestamp1 = getTimestamp(split[0]);
                Timestamp timestamp2 = getTimestamp(split[1]);

                return field + " BETWEEN '" + timestamp1 + "' AND '" + timestamp2 + "'";

            }

            String op;
            String remaining;
            if (Character.isDigit(command.charAt(0))) {
                op = "=";
                remaining = command;
            } else if (Character.isDigit(command.charAt(1))) {
                op = command.substring(0, 1);
                remaining = command.substring(1);
            } else  {
                op = command.substring(0, 2);
                remaining = command.substring(2);
            }

            if (containsTime(remaining)) {
                Timestamp timestamp = Timestamp.valueOf(remaining.replace("T", " "));

                if (op.equals("=")) {
                    Timestamp after1second = new Timestamp(timestamp.getTime() + 1000);
                    return field + " >= '" + timestamp + "' AND " + field + " < '" + after1second + "'";
                }

                return field + " " + op + " '" + timestamp + "'";
            } else {
                Date date = Date.valueOf(remaining);

                if (op.equals("=")) {
                    Date after1day = new Date(date.getTime() + 24 * 60 * 60 * 1000);
                    return field + " >= '" + date + "' AND " + field + " < '" + after1day + "'";
                }

                return field + " " + op + " '" + date + "'";
            }
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            throw new ParserException("Not a date in " + command);
        }
    }
    private static String parseSizeCommand(String command) {
        try {
            if (command.contains("..")) {
                // between operator
                String[] split = command.split("\\.\\.");
                long size1 = Long.parseLong(split[0]);
                long size2 = Long.parseLong(split[1]);

                return "size BETWEEN " + size1 + " AND " + size2;
            }

            String op;
            String remaining;
            // add support for negative numbers
            if (Character.isDigit(command.charAt(0)) || command.charAt(0) == '-') {
                op = "=";
                remaining = command;
            } else if (Character.isDigit(command.charAt(1)) || command.charAt(1) == '-') {
                op = command.substring(0, 1);
                remaining = command.substring(1);
            } else  {
                op = command.substring(0, 2);
                remaining = command.substring(2);
            }
            long size = Long.parseLong(remaining);

            return "size " + op + " " + size;
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            throw new ParserException("Not a number in " + command);
        }
    }
    private static String parseReadCommand(String command) {
        if (command.equals("true")) {
            return "read_access = TRUE";
        } else if (command.equals("false")) {
            return "read_access = FALSE";
        }

        throw new ParserException("Wrong read command");
    }
    private static String parseContentCommand(String command) {
        if (command.startsWith("\"") && command.endsWith("\"")) {
            command = command.substring(1, command.length() - 1);
        } else if (command.endsWith("\"")) {
            throw new ParserException("Wrong quotations");
        }
        return "ts @@ plainto_tsquery('simple', '" + command + "')";
    }

    private static String exprToSQL(CommandExpr commandExpr) {
        String[] tokens = commandExpr.command().split(":", 2); // time may contain ":"
        if (tokens.length < 2 || tokens[1].isEmpty()) {
            throw new ParserException("Invalid command: " + commandExpr.command());
        }
        String parsedCommand = switch (tokens[0]) {
            case "name" -> parseNameCommand(tokens[1]);
            case "extension" -> parseExtensionCommand(tokens[1]);
            case "path" -> parsePathCommand(tokens[1]);
            case "created" -> parseTimeCommand("file_creation_time", tokens[1]);
            case "modified" -> parseTimeCommand("file_last_modified_time", tokens[1]);
            case "accessed" -> parseTimeCommand("file_last_accessed_time", tokens[1]);
            case "size" -> parseSizeCommand(tokens[1]);
            case "read" -> parseReadCommand(tokens[1]);
            case "content" -> parseContentCommand(tokens[1]);
            default -> throw new ParserException("Unexpected value: " + tokens[0]);
        };
        return " (" + parsedCommand + ") ";
    }

    public static String exprToSQL(Expr expr) {
        return switch (expr) {
            case OrExpr orExpr -> exprToSQL(orExpr);
            case AndExpr andExpr -> exprToSQL(andExpr);
            case NotExpr notExpr -> exprToSQL(notExpr);
            default -> exprToSQL((CommandExpr) expr);
        };
    }

    public static void main(String[] args) {
        String input = "name:\"adi e\" OR created:2026-03-29T10:10:10..2026-03-30 AND read:true NOT content:\"ana are mere\"";
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Expr ast = parser.parseExpression();
        //Time.valueOf("qwwq");
        System.out.println(QueryBuilder.exprToSQL(ast));
    }
}
