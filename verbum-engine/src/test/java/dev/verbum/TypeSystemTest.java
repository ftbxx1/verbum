package dev.verbum;

import dev.verbum.engine.ScriptEngine;
import dev.verbum.error.VerbumError;
import dev.verbum.runtime.MockMcRuntime;
import dev.verbum.type.Type;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 1: a real type system. Functions may declare typed parameters with
 * default values and a return type, and the return value is coerced to it.
 * Backward compatibility is preserved (legacy untyped functions still work).
 */
class TypeSystemTest {

    private static ScriptEngine with(String script) {
        ScriptEngine e = new ScriptEngine(new MockMcRuntime());
        e.load(script, "types.vb");
        return e;
    }

    @Test
    void typedFunctionReturnsCoercedNumber() {
        MockMcRuntime runtime = new MockMcRuntime();
        ScriptEngine engine = new ScriptEngine(runtime);
        engine.load("""
            function double(x as number) returns number
                return x plus x
            on server start
                set {result} to call double with 21
            """, "typed.vb");
        engine.onServerStart();

        Object result = engine.interpreter().store().get(
                dev.verbum.interp.VariableStore.Scope.GLOBAL, "result");
        assertNotNull(result);
        // coerced to a number (Double)
        assertTrue(result instanceof Double d && d == 42.0,
                "double(21) must return 42 as a number, got: " + result);
    }

    @Test
    void functionHasTypedParametersWithDefaults() {
        ScriptEngine engine = with("""
            function add(a as number, b as number = 10) returns number
                return a plus b
            on server start
                set {sum} to call add with 5
                set {sum2} to call add with 5 and 3
            """);
        engine.onServerStart();

        Object sum = engine.interpreter().store().get(
                dev.verbum.interp.VariableStore.Scope.GLOBAL, "sum");
        Object sum2 = engine.interpreter().store().get(
                dev.verbum.interp.VariableStore.Scope.GLOBAL, "sum2");
        assertEquals(15.0, sum);
        assertEquals(8.0, sum2);
    }

    @Test
    void functionReportsMissingArguments() {
        MockMcRuntime runtime = new MockMcRuntime();
        ScriptEngine engine = new ScriptEngine(runtime);
        engine.load("""
            function need(a as number, b as number) returns number
                return a plus b
            on server start
                call need with 1
            """, "missing.vb");
        // A function expecting 2 args given 1 with no default -> friendly error
        VerbumError err = assertThrows(VerbumError.class,
                engine::onServerStart);
        assertTrue(err.getMessage().contains("needs 2") || err.getMessage().contains("needs")
                        || err.getMessage().contains("arguments"),
                "should explain a required argument is missing: " + err.getMessage());
    }

    @Test
    void legacyUntypedFunctionStillWorks() {
        ScriptEngine engine = with("""
            function echo text
                return text
            on server start
                set {echoed} to call echo with hello
            """);
        engine.onServerStart();
        Object out = engine.interpreter().store().get(
                dev.verbum.interp.VariableStore.Scope.GLOBAL, "echoed");
        assertEquals("hello", out);
    }

    @Test
    void typeCoercionConvertsTextToNumber() {
        assertEquals(5.0, Type.NUMBER.coerce("5", 0));
        assertEquals(7.0, Type.NUMBER.coerce(7, 0));
        assertEquals(Boolean.TRUE, Type.BOOLEAN.coerce("yes", 0));
    }

    @Test
    void typeNumberThrowsOnBadText() {
        assertThrows(dev.verbum.error.VerbumError.class, () -> Type.NUMBER.coerce("abc", 1));
    }
}
