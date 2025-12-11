package com.wells.bill.assistant.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Builds Spring AI string filter expressions that are accepted by the
 * ANTLR-based FilterExpressionTextParser.
 * <p>
 * Examples produced:
 * vendor == 'ACME'
 * amount >= 100
 * vendor in ['A', 'B']
 * (vendor == 'ACME' || vendor == 'OTHER') && amount >= 100
 */
public final class FilterExpressionBuilder {

    private final StringBuilder sb = new StringBuilder();

    private FilterExpressionBuilder() {
    }

    public static FilterExpressionBuilder start() {
        return new FilterExpressionBuilder();
    }

    private static String serialize(Object v) {
        if (v == null) {
            return "null";
        }
        if (v instanceof Number || v instanceof Boolean) {
            return v.toString();
        }
        // single-quote strings; escape inner quotes
        return "'" + v.toString().replace("'", "\\'") + "'";
    }

    /**
     * Append a top-level expression. If there is already content, join with " && "
     * so multiple calls (eq / in / etc) result in a valid boolean expression.
     */
    private void appendTopLevel(String expr) {
        if (!sb.isEmpty()) {
            sb.append(" && ");
        }
        sb.append(expr);
    }

    // -------------------------------------------------------------------------
    // Comparison operators – use Spring AI DSL: ==, !=, >, >=, <, <=
    // -------------------------------------------------------------------------
    public FilterExpressionBuilder eq(String key, Object value) {
        appendTopLevel(key + " == " + serialize(value));
        return this;
    }

    public FilterExpressionBuilder ne(String key, Object value) {
        appendTopLevel(key + " != " + serialize(value));
        return this;
    }

    public FilterExpressionBuilder gt(String key, Object value) {
        appendTopLevel(key + " > " + serialize(value));
        return this;
    }

    public FilterExpressionBuilder gte(String key, Object value) {
        appendTopLevel(key + " >= " + serialize(value));
        return this;
    }

    public FilterExpressionBuilder lt(String key, Object value) {
        appendTopLevel(key + " < " + serialize(value));
        return this;
    }

    public FilterExpressionBuilder lte(String key, Object value) {
        appendTopLevel(key + " <= " + serialize(value));
        return this;
    }

    // -------------------------------------------------------------------------
    // IN operator – Spring AI expects: key in ['A', 'B']
    // -------------------------------------------------------------------------
    public FilterExpressionBuilder in(String key, Collection<?> values) {
        String joined = values.stream()
                .map(FilterExpressionBuilder::serialize)
                .collect(Collectors.joining(", "));
        appendTopLevel(key + " in [" + joined + "]");
        return this;
    }

    // -------------------------------------------------------------------------
    // Boolean combinations – use || and && between subexpressions
    // -------------------------------------------------------------------------
    public FilterExpressionBuilder or(FilterExpressionBuilder... parts) {
        String expr = Arrays.stream(parts)
                .map(FilterExpressionBuilder::build)
                .collect(Collectors.joining(" || "));
        appendTopLevel("(" + expr + ")");
        return this;
    }

    public FilterExpressionBuilder and(FilterExpressionBuilder... parts) {
        String expr = Arrays.stream(parts)
                .map(FilterExpressionBuilder::build)
                .collect(Collectors.joining(" && "));
        appendTopLevel("(" + expr + ")");
        return this;
    }

    // Raw injection – mostly for tests where you already know the DSL is valid.
    public FilterExpressionBuilder raw(String raw) {
        appendTopLevel(raw);
        return this;
    }

    public String build() {
        return sb.toString().trim();
    }

    @Override
    public String toString() {
        return build();
    }
}
