package dev.verbum.error;

/**
 * A friendly Verbum error. Every message tells the user WHAT was wrong and
 * WHAT to write instead, never raw token dumps.
 */
public class VerbumError extends RuntimeException {

    private final Integer line;   // 1-based line, or null if not line-specific
    private final String hint;    // optional "Try: ..."

    public VerbumError(String message) {
        super(message);
        this.line = null;
        this.hint = null;
    }

    public VerbumError(int line, String message) {
        super(message);
        this.line = line;
        this.hint = null;
    }

    public VerbumError(int line, String message, String hint) {
        super(message);
        this.line = line;
        this.hint = hint;
    }

    public Integer line() { return line; }
    public String hint() { return hint; }

    /** Formats the error the way a beginner sees it. */
    public String pretty() {
        StringBuilder sb = new StringBuilder();
        if (line != null) {
            sb.append("Problem on line ").append(line).append(":\n");
        }
        sb.append("  ").append(getMessage());
        if (hint != null && !hint.isBlank()) {
            sb.append("\nTry:\n  ").append(hint);
        }
        return sb.toString();
    }
}
