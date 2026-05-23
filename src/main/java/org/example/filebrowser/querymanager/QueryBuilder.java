package org.example.filebrowser.querymanager;

import org.example.filebrowser.model.ImageColor;
import org.example.filebrowser.model.QueryInducedType;
import org.example.filebrowser.querylogic.parser.expression.*;
import org.example.filebrowser.querymanager.decorator.IQueryBuilder;
import org.example.filebrowser.utils.exceptions.ParserException;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.*;

import static java.lang.Math.*;

public class QueryBuilder {

    private final IQueryBuilder contentQueryBuilder;
    private final Map<QueryInducedType, Float> typeScores;

    public QueryBuilder(IQueryBuilder contentQueryBuilder) {
        this.contentQueryBuilder = contentQueryBuilder;
        typeScores = new HashMap<>();
        clearTypeScores();
    }

    private void clearTypeScores() {
        for (QueryInducedType queryInducedType : QueryInducedType.values()) {
            typeScores.put(queryInducedType, 0f);
        }
    }

    private void incrementType(QueryInducedType queryInducedType, ImpactOnType impactOnType) {
        typeScores.put(queryInducedType, min(typeScores.get(queryInducedType) + impactOnType.getValue(), 1.0f));
    }

    private String exprToSQL(OrExpr orExpr) {
        return " (" + exprToSQL(orExpr.left()) + "OR" + exprToSQL(orExpr.right()) + ") ";
    }
    private String exprToSQL(AndExpr andExpr) {
        return " (" + exprToSQL(andExpr.left()) + "AND" + exprToSQL(andExpr.right()) + ") ";
    }
    private String exprToSQL(NotExpr notExpr) {
        return " ( NOT" + exprToSQL(notExpr.expr()) + ") ";
    }

    private String parseNameCommand(String command) {
        if (command.startsWith("\"") && command.endsWith("\"")) {
            command = command.substring(1, command.length() - 1);
        } else if (command.endsWith("\"")) {
            throw new ParserException("Wrong quotations");
        }
        return "name LIKE '%" + command + "%'";
    }
    private String parseExtensionCommand(String command) {

        if (command.equals("txt")) {
            incrementType(QueryInducedType.TXT, ImpactOnType.HIGH);
        }

        return "extension = '" + command + "'";
    }
    private String parsePathCommand(String command) {
        if (command.startsWith("\"") && command.endsWith("\"")) {
            command = command.substring(1, command.length() - 1);
        } else if (command.endsWith("\"")) {
            throw new ParserException("Wrong quotations");
        }
        return "path LIKE '%" + command.replace("\\", "/") + "%'";
    }
    private boolean containsTime(String command) {
        return command.contains("T");
    }
    private Timestamp getTimestamp(String str) {
        if (containsTime(str)) {
            return Timestamp.valueOf(str.replace("T", " "));
        }
        return Timestamp.valueOf(str + " 00:00:00");
    }
    private String parseTimeCommand(String field, String command) {
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
    private String parseSizeCommand(String command) {
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
    private String parseReadCommand(String command) {
        if (command.equals("true")) {
            return "read_access = TRUE";
        } else if (command.equals("false")) {
            return "read_access = FALSE";
        }

        throw new ParserException("Wrong read command");
    }
    private String parseContentCommand(String command) {
        if (command.startsWith("\"") && command.endsWith("\"")) {
            command = command.substring(1, command.length() - 1);
        } else if (command.endsWith("\"")) {
            throw new ParserException("Wrong quotations");
        }

        if (command.contains("image")) {
            incrementType(QueryInducedType.IMAGE, ImpactOnType.MEDIUM);
        } else if (command.contains("log")) {
            incrementType(QueryInducedType.LOGS, ImpactOnType.MEDIUM);
        }

        // Query Decorator applies only on content commands
        command = contentQueryBuilder.buildQuery(command);
        return "ts @@ to_tsquery('simple', '" + command + "')";
    }

    private String parseColorCommand(String command) {
        incrementType(QueryInducedType.IMAGE, ImpactOnType.HIGH);

        command = command.toUpperCase();
        Set<String> availableColors = new HashSet<>();
        Arrays.stream(ImageColor.values()).forEach(color -> availableColors.add(color.toString()));
        if (availableColors.contains(command)) {
            return "color = '" + command + "'";
        }

        throw new ParserException("Wrong color command");
    }

    private String exprToSQL(CommandExpr commandExpr) {
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
            case "color" -> parseColorCommand(tokens[1]);
            default -> throw new ParserException("Unexpected value: " + tokens[0]);
        };
        return " (" + parsedCommand + ") ";
    }

    public String exprToSQL(Expr expr) {
        clearTypeScores();
        return switch (expr) {
            case OrExpr orExpr -> exprToSQL(orExpr);
            case AndExpr andExpr -> exprToSQL(andExpr);
            case NotExpr notExpr -> exprToSQL(notExpr);
            default -> exprToSQL((CommandExpr) expr);
        };
    }

    public QueryInducedType getQueryInducedType() {
        // get biggest two values; if they are two close, return OTHER, otherwise, return max
        Map.Entry<QueryInducedType, Float> maxEntry = null;
        Map.Entry<QueryInducedType, Float> secondMaxEntry = null;
        for (Map.Entry<QueryInducedType, Float> entry : typeScores.entrySet()) {
            if (maxEntry == null || entry.getValue() > maxEntry.getValue()) {
                secondMaxEntry = maxEntry;
                maxEntry = entry;
            }
            else if (secondMaxEntry == null || entry.getValue() > secondMaxEntry.getValue()) {
                secondMaxEntry = entry;
            }
        }

        assert secondMaxEntry != null;

        float TYPE_SCORE_THRESHOLD = 0.2f;
        if (abs(secondMaxEntry.getValue() - maxEntry.getValue()) > TYPE_SCORE_THRESHOLD) {
            return maxEntry.getKey();
        }

        return QueryInducedType.OTHER;
    }

    public static void main(String[] args) {
//        String input = "name:\"adi e\" OR created:2026-03-29T10:10:10..2026-03-30 AND read:true NOT content:\"ana are mere\"";
//        Lexer lexer = new Lexer(input);
//        Parser parser = new Parser(lexer);
//        Expr ast = parser.parseExpression();
//        //Time.valueOf("qwwq");
//        QueryBuilder queryBuilder = new QueryBuilder(new BaseQueryBuilder());
//        System.out.println(queryBuilder.exprToSQL(ast));

        String s = "con    tent";
        s = s.replaceAll("\\s+", " ");
        System.out.println(s);
    }
}

enum ImpactOnType {
    NONE(0f), LOW(0.1f), MEDIUM(0.3f), HIGH(0.5f);

    private final float impact;

    ImpactOnType(float impact) {
        this.impact = impact;
    }

    float getValue() {
        return impact;
    }
}
