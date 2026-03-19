package dev.ell.mrgreen.util;

import dev.ell.mrgreen.util.SexprParser.SExpr;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SexprParserTest {

    private static final String EXAMPLE = """
            ((:color . "#38FF00") (:name . "ellg") (:resolution2025 . "i will stop watching twitch") \
            (:identity . "Abbie Kirk") (:rpg-character "ellg" 1 ("Fighter" fig//rpg-ability-scores-str 10 \
            (9 6 7 3 6 6) (18 20 16 17 19)) 9 (15 9 12 11 12 10)) (:element . "heart") \
            (:ancestor nil "Ralph" nil nil "ellg" "Management Analyst" 1900 1904 "succumbed to a childhood illness.
            ") (:room 9 (((2 . 2) . "JoelTeachingHisSonJolHowToSpinWhileWideBorisPassesByButMyWindowsXPKeepsCrashing") \
            ((0 . 3) . "SabaPing") ((3 . 3) . "FishMoley") ((1 . 3) . "badcop3Fish") ((0 . 0) . "badcop3Fish") \
            ((1 . 3) . "BatChest") ((1 . 2) . "JoelbutmywindowsXPiscrashing") ((3 . 1) . "Joel") ((3 . 1) . "jol") \
            ((1 . 2) . "JoelbutmywindowsXPiscrashing") \\...)) (:faction . nate) (:boost . 74))""";

    @Test
    void parsesFullExample() {
        var result = SexprParser.parse(EXAMPLE);
        assertInstanceOf(SExpr.Lst.class, result);
    }

    @Test
    void lookupColor() {
        var result = SexprParser.parse(EXAMPLE);
        assertEquals("#38FF00", result.get(":color").orElseThrow().asStr());
    }

    @Test
    void lookupName() {
        var result = SexprParser.parse(EXAMPLE);
        assertEquals("ellg", result.get(":name").orElseThrow().asStr());
    }

    @Test
    void lookupBoost() {
        var result = SexprParser.parse(EXAMPLE);
        assertEquals(74, result.get(":boost").orElseThrow().asNum());
    }

    @Test
    void lookupFaction() {
        var result = SexprParser.parse(EXAMPLE);
        var faction = result.get(":faction").orElseThrow();
        assertInstanceOf(SExpr.Sym.class, faction);
        assertEquals("nate", ((SExpr.Sym) faction).name());
    }

    @Test
    void lookupRpgCharacter() {
        var result = SexprParser.parse(EXAMPLE);
        var rpg = result.get(":rpg-character").orElseThrow();
        assertInstanceOf(SExpr.Lst.class, rpg);
        var elements = rpg.asList();
        // First element should be "ellg"
        assertEquals("ellg", elements.getFirst().asStr());
        // Second element should be 1
        assertEquals(1, elements.get(1).asNum());
    }

    @Test
    void lookupRoomContainsConsPairs() {
        var result = SexprParser.parse(EXAMPLE);
        var room = result.get(":room").orElseThrow();
        assertInstanceOf(SExpr.Lst.class, room);
        var roomElements = room.asList();
        // First element is 9
        assertEquals(9, roomElements.getFirst().asNum());
        // Second element is the grid list
        var grid = roomElements.get(1);
        assertInstanceOf(SExpr.Lst.class, grid);
        // First grid entry should be a cons pair ((2 . 2) . "Joel...")
        var firstEntry = grid.asList().getFirst();
        assertInstanceOf(SExpr.Cons.class, firstEntry);
    }

    @Test
    void lookupAncestorWithMultilineString() {
        var result = SexprParser.parse(EXAMPLE);
        var ancestor = result.get(":ancestor").orElseThrow();
        assertInstanceOf(SExpr.Lst.class, ancestor);
        var elements = ancestor.asList();
        // First element is nil
        assertTrue(elements.getFirst().isNil());
        // Second element is "Ralph"
        assertEquals("Ralph", elements.get(1).asStr());
        // Last element contains a newline
        var lastStr = elements.getLast().asStr();
        assertTrue(lastStr.contains("\n"), "Ancestor death string should contain newline");
    }

    @Test
    void parseDottedPair() {
        var result = SexprParser.parse("(a . b)");
        assertInstanceOf(SExpr.Cons.class, result);
        var cons = (SExpr.Cons) result;
        assertEquals("a", ((SExpr.Sym) cons.car()).name());
        assertEquals("b", ((SExpr.Sym) cons.cdr()).name());
    }

    @Test
    void parseEmptyList() {
        var result = SexprParser.parse("()");
        assertInstanceOf(SExpr.Lst.class, result);
        assertTrue(((SExpr.Lst) result).elements().isEmpty());
    }

    @Test
    void parseNegativeNumber() {
        var result = SexprParser.parse("-42");
        assertInstanceOf(SExpr.Num.class, result);
        assertEquals(-42, result.asNum());
    }

    @Test
    void parseKeywordSymbol() {
        var result = SexprParser.parse(":foo");
        assertInstanceOf(SExpr.Sym.class, result);
        assertEquals(":foo", ((SExpr.Sym) result).name());
    }

    @Test
    void missingKeyReturnsEmpty() {
        var result = SexprParser.parse("((:a . 1))");
        assertTrue(result.get(":b").isEmpty());
    }

    @Test
    void lookupElement() {
        var result = SexprParser.parse(EXAMPLE);
        assertEquals("heart", result.get(":element").orElseThrow().asStr());
    }
}
