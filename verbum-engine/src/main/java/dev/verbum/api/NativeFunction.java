package dev.verbum.api;

import dev.verbum.interp.Interpreter;

import java.util.List;

/**
 * A native function registered by an add-on, callable from scripts as
 * <pre>  call <name> with a and b</pre>
 * or used bare in expressions:  set x to <name> 5 . Arguments arrive as
 * already-split, value-evaluated strings.
 */
public interface NativeFunction {

    String name();

    Object run(Interpreter interp, List<String> args, int line);

    static NativeFunction of(String name, Callable f) {
        return new NativeFunction() {
            @Override public String name() { return name; }
            @Override public Object run(Interpreter interp, List<String> args, int line) { return f.run(interp, args, line); }
        };
    }

    @FunctionalInterface
    interface Callable { Object run(Interpreter interp, List<String> args, int line); }
}