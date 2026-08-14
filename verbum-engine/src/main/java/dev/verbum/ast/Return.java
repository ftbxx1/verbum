package dev.verbum.ast;

import java.util.List;

/**
 * A  return  statement used inside a  function  to hand a value back:
 *   function double number
 *       return number * 2
 */
public final class Return implements Stmt {

    private final int line;
    private final List<String> valueWords;

    public Return(int line, List<String> valueWords) {
        this.line = line;
        this.valueWords = valueWords;
    }

    @Override public int line() { return line; }
    public List<String> valueWords() { return valueWords; }
}
