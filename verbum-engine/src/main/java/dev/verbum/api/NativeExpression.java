package dev.verbum.api;

import dev.verbum.interp.Interpreter;
import java.util.List;

/**
 * A native expression registered by an add-on, usable in scripts as
 * <pre>set x to sqrt of 16</pre>
 * or in conditions like <pre>if x is sqrt of 16</pre>.
 *
 * <pre>
 *   r.registerExpression(NativeExpression.of("sqrt",
 *       (it, words, line) -> Math.sqrt(it.store().asNumber(it.focus(), line))));
 * </pre>
 */
public interface NativeExpression {

    /** The word(s) this expression occupies, e.g. "sqrt", "random element of". */
    String word();

    /** Runs the expression. {@code words} is everything after the expression word(s). */
    double run(Interpreter interp, List<String> words, int line);

    /** Shortcut factory:  expression("sqrt", (it, words, line) -> ...) */
    static NativeExpression of(String w, Runner r) {
        return new NativeExpression() {
            @Override public String word() { return w; }
            @Override public double run(Interpreter interp, List<String> words, int line) { return r.run(interp, words, line); }
        };
    }

    @FunctionalInterface
    interface Runner { double run(Interpreter interp, List<String> words, int line); }
}