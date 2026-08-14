package dev.verbum;

import dev.verbum.engine.ScriptEngine;
import dev.verbum.i18n.KeywordResolver;
import dev.verbum.runtime.MockMcRuntime;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The 100-language keyword system: the first word of every line is normalized
 * from the active language into canonical English before parsing, so a German
 * "wenn Steve beitritt / gib Steve einen Diamanten" behaves exactly like its
 * English version. Free text (names, items, messages) is never rewritten.
 */
class I18nTest {

    private MockMcRuntime run(String lang, String source) {
        MockMcRuntime r = new MockMcRuntime();
        ScriptEngine e = new ScriptEngine(r).setLanguage(lang);
        e.load(source, "test.vb");
        e.onServerStart();
        e.trigger("join", "Steve");
        e.tick();
        return r;
    }

    @Test void spanishHeaderAndVerbWork() {
        MockMcRuntime r = run("es", """
                cuando Steve joins
                    da Steve un diamante
                """);
        assertTrue(r.log.stream().anyMatch(l -> l.contains("give Steve")),
                "cuando/da should act as when/give, got " + r.log);
    }

    @Test void spanishKeepFreeText() {
        MockMcRuntime r = run("es", """
                cuando Steve joins
                    dile a Steve Bienvenido al servidor
                """);
        assertTrue(r.chatter.stream().anyMatch(l -> l.contains("Bienvenido al servidor")),
                "message must stay Spanish, got " + r.chatter);
    }

    @Test void germanWennBecomeWhen() {
        MockMcRuntime r = run("de", """
                wenn Steve joins
                    gib Steve einen Diamanten
                """);
        assertTrue(r.log.stream().anyMatch(l -> l.contains("give Steve")),
                "wenn/gib should act as when/give, got " + r.log);
    }

    @Test void frenchQuandDOnne() {
        MockMcRuntime r = run("fr", """
                quand Steve joins
                    donne Steve un diamant
                """);
        assertTrue(r.log.stream().anyMatch(l -> l.contains("give Steve")),
                "quand/donne should act as when/give, got " + r.log);
    }

    @Test void italianQuandoDai() {
        MockMcRuntime r = run("it", """
                quando Steve joins
                    dai Steve un diamante
                """);
        assertTrue(r.log.stream().anyMatch(l -> l.contains("give Steve")),
                "quando/dai should act as when/give, got " + r.log);
    }

    @Test void englishStillWorks() {
        MockMcRuntime r = run("en", """
                when Steve joins
                    give Steve 1 diamond
                """);
        assertTrue(r.log.stream().anyMatch(l -> l.contains("give Steve")));
    }

    @Test void unknownLanguageFallsBackToEnglish() {
        MockMcRuntime r = run("xx", """
                when Steve joins
                    give Steve 1 diamond
                """);
        assertTrue(r.log.stream().anyMatch(l -> l.contains("give Steve")));
    }

    @Test void languageByName() {
        MockMcRuntime r = new MockMcRuntime();
        ScriptEngine e = new ScriptEngine(r).setLanguageByName("espa\u00F1ol");
        e.load("""
                cuando Steve joins
                    da Steve un diamante
                """, "test.vb");
        e.onServerStart();
        e.trigger("join", "Steve");
        e.tick();
        assertTrue(r.log.stream().anyMatch(l -> l.contains("give Steve")));
    }

    @Test void atLeastNinetyNineLanguages() {
        assertTrue(KeywordResolver.codes().size() >= 99,
                "expected >= 99 languages, got " + KeywordResolver.codes().size());
        assertEquals("when", KeywordResolver.forCode("es").resolve("cuando"));
        assertEquals("when", KeywordResolver.forCode("de").resolve("wenn"));
        assertEquals("give", KeywordResolver.forCode("fr").resolve("donne"));
        assertEquals("when", KeywordResolver.forCode("zh").resolve("\u5F53")); // 当
        assertEquals("when", KeywordResolver.forCode("ar").resolve("\u0639\u0646\u062F\u0645\u0627")); // عندما
        assertEquals("kill", KeywordResolver.forCode("ru").resolve("\u0443\u0431\u0435\u0439")); // убей
    }

    @Test void everyLanguageHasAWhen() {
        for (String code : KeywordResolver.codes()) {
            if (code.equals("en")) continue;
            Map<String, String> t = tableFor(code);
            assertNotNull(t, "no table for " + code);
            assertTrue(t.containsValue("when"), code + " must translate its header to when");
        }
    }

    @Test void headerWordsComeFirstSoHeaderWinsCollisions() {
        // German "wenn" means both when and if; the header meaning must win.
        assertEquals("when", KeywordResolver.forCode("de").resolve("wenn"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> tableFor(String code) {
        try {
            java.lang.reflect.Field f = KeywordResolver.class.getDeclaredField("TABLES");
            f.setAccessible(true);
            java.util.Map<String, Map<String, String>> all =
                    (java.util.Map<String, Map<String, String>>) f.get(null);
            Map<String, String> t = all.get(code);
            return t == null ? null : new LinkedHashMap<>(t);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}