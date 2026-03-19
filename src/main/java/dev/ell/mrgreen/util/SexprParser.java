package dev.ell.mrgreen.util;

/*
Example message:

((:color . "#38FF00") (:name . "ellg") (:resolution2025 . "i will stop watching twitch") (:identity . "Abbie Kirk") (:rpg-character "ellg" 1 ("Fighter" fig//rpg-ability-scores-str 10 (9 6 7 3 6 6) (18 20 16 17 19)) 9 (15 9 12 11 12 10)) (:element . "heart") (:ancestor nil "Ralph" nil nil "ellg" "Management Analyst" 1900 1904 "succumbed to a childhood illness.
") (:room 9 (((2 . 2) . "JoelTeachingHisSonJolHowToSpinWhileWideBorisPassesByButMyWindowsXPKeepsCrashing") ((0 . 3) . "SabaPing") ((3 . 3) . "FishMoley") ((1 . 3) . "badcop3Fish") ((0 . 0) . "badcop3Fish") ((1 . 3) . "BatChest") ((1 . 2) . "JoelbutmywindowsXPiscrashing") ((3 . 1) . "Joel") ((3 . 1) . "jol") ((1 . 2) . "JoelbutmywindowsXPiscrashing") \...)) (:faction . nate) (:boost . 74))
*/

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SexprParser {

    public sealed interface SExpr {

        record Str(String value) implements SExpr {
        }

        record Num(long value) implements SExpr {
        }

        record Sym(String name) implements SExpr {
        }

        record Cons(SExpr car, SExpr cdr) implements SExpr {
        }

        record Lst(List<SExpr> elements) implements SExpr {
        }

        default Optional<SExpr> get(String key) {
            if (!(this instanceof Lst(List<SExpr> elements))) return Optional.empty();
            for (var el : elements) {
                if (el instanceof Cons(SExpr car, SExpr cdr) && car instanceof Sym(String name1) && name1.equals(key)) {
                    return Optional.of(cdr);
                }
                if (el instanceof Lst(List<SExpr> elements1) && !elements1.isEmpty()
                        && elements1.getFirst() instanceof Sym(String name) && name.equals(key)) {
                    return Optional.of(new Lst(elements1.subList(1, elements1.size())));
                }
            }
            return Optional.empty();
        }

        default String asStr() {
            return ((Str) this).value();
        }

        default long asNum() {
            return ((Num) this).value();
        }

        default List<SExpr> asList() {
            return ((Lst) this).elements();
        }

        default boolean isNil() {
            return this instanceof Sym(String name) && name.equals("nil");
        }
    }

    private final String input;
    private int pos;

    private SexprParser(String input) {
        this.input = input;
        this.pos = 0;
    }

    public static SExpr parse(String input) {
        var parser = new SexprParser(input);
        return parser.parseExpr();
    }

    private SExpr parseExpr() {
        skipWhitespace();
        if (pos >= input.length()) {
            throw new IllegalArgumentException("Unexpected end of input");
        }
        char c = input.charAt(pos);
        if (c == '(') {
            return parseList();
        } else if (c == '"') {
            return parseString();
        } else if (c == '-' || c == '+' || Character.isDigit(c)) {
            return parseNumberOrSymbol();
        } else {
            return parseSymbol();
        }
    }

    private SExpr parseList() {
        pos++; // skip '('
        skipWhitespace();

        var elements = new ArrayList<SExpr>();

        while (pos < input.length() && input.charAt(pos) != ')') {
            // Check for dot in dotted pair
            if (input.charAt(pos) == '.' && pos + 1 < input.length() && isDelimiter(input.charAt(pos + 1))) {
                if (elements.size() == 1) {
                    pos++; // skip '.'
                    skipWhitespace();
                    var cdr = parseExpr();
                    skipWhitespace();
                    expect();
                    return new SExpr.Cons(elements.getFirst(), cdr);
                }
            }

            elements.add(parseExpr());
            skipWhitespace();
        }

        expect();
        return new SExpr.Lst(List.copyOf(elements));
    }

    private SExpr parseString() {
        pos++; // skip opening '"'
        var sb = new StringBuilder();
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (c == '\\' && pos + 1 < input.length()) {
                pos++;
                char escaped = input.charAt(pos);
                switch (escaped) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    default -> {
                        sb.append('\\');
                        sb.append(escaped);
                    }
                }
            } else if (c == '"') {
                pos++; // skip closing '"'
                return new SExpr.Str(sb.toString());
            } else {
                sb.append(c);
            }
            pos++;
        }
        throw new IllegalArgumentException("Unterminated string");
    }

    private SExpr parseNumberOrSymbol() {
        int start = pos;
        if (input.charAt(pos) == '-' || input.charAt(pos) == '+') {
            pos++;
        }
        if (pos < input.length() && Character.isDigit(input.charAt(pos))) {
            while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
                pos++;
            }
            // Verify next char is a delimiter — otherwise it's a symbol like "-foo"
            if (pos >= input.length() || isDelimiter(input.charAt(pos))) {
                return new SExpr.Num(Long.parseLong(input.substring(start, pos)));
            }
        }
        // Not a number, rewind and parse as symbol
        pos = start;
        return parseSymbol();
    }

    private SExpr parseSymbol() {
        int start = pos;
        while (pos < input.length() && !isDelimiter(input.charAt(pos))) {
            pos++;
        }
        if (pos == start) {
            throw new IllegalArgumentException("Unexpected character at position " + pos + ": '" + input.charAt(pos) + "'");
        }
        return new SExpr.Sym(input.substring(start, pos));
    }

    private void skipWhitespace() {
        while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
            pos++;
        }
    }

    private void expect() {
        if (pos >= input.length() || input.charAt(pos) != ')') {
            throw new IllegalArgumentException(
                    "Expected '" + ')' + "' at position " + pos +
                            (pos < input.length() ? " but found '" + input.charAt(pos) + "'" : " but reached end of input"));
        }
        pos++;
    }

    private boolean isDelimiter(char c) {
        return c == '(' || c == ')' || c == '"' || Character.isWhitespace(c);
    }
}
