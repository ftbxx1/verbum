package dev.verbum.api;

import dev.verbum.interp.Interpreter;

import java.util.List;

/**
 * A live condition registered by an add-on, e.g. the words "player is mining
 * titanium". {@code matches} decides whether the phrase belongs to us;
 * {@code eval} returns the answer.
 */
public interface NativeCondition {

    /** Whether this phrase is one of ours (should be checked before built-ins fail). */
    boolean matches(List<String> words);

    boolean eval(Interpreter interp, List<String> words);
}