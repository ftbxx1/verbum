package dev.verbum.error;

/**
 * A friendly Verbum error. Every message tells the user WHAT was wrong and
 * WHAT to write instead, never raw token dumps.
 * <br>Carries line and column positions (1‑based) and an optional source‑file hint.
 */
public class VerbumError extends RuntimeException {

    private final Integer line;   // 1‑based line, or null if not line‑specific
    private final Integer column; // 1‑based column, or null if not column‑specific
    private final String hint;    // optional "Try: ..."
    private final String source;  // optional "file.vb", e.g. for pretty‑printing

    public VerbumError(String message) {
        super(message);
        this.line = null;
        this.column = null;
        this.hint = null;
        this.source = null;
    }

    public VerbumError(int line, String message) {
        super(message);
        this.line = line;
        this.column = null;
        this.hint = null;
        this.source = null;
    }

    public VerbumError(int line, int column, String message) {
        super(message);
        this.line = line;
        this.column = column;
        this.hint = null;
        this.source = null;
    }

    public VerbumError(int line, String message, String hint) {
        super(message);
        this.line = line;
        this.column = null;
        this.hint = hint;
        this.source = null;
    }

    public VerbumError(int line, int column, String message, String hint) {
        super(message);
        this.line = line;
        this.column = column;
        this.hint = hint;
        this.source = null;
    }

    public VerbumError(String message, String source) {
        super(message);
        this.line = null;
        this.column = null;
        this.hint = null;
        this.source = source;
    }

    /** Formats the error the way a beginner sees it. */
    public String pretty() {
        StringBuilder sb = new StringBuilder();
        if (line != null) {
            sb.append("Problem on line ").append(line);
            if (column != null) {
                sb.append(":").append(column);
            }
            sb.append(":\n");
        } else if (column != null) {
            sb.append("Problem at column ").append(column).append(":\n");
        }
        sb.append("  ").append(getMessage());
        if (source != null && !source.isBlank()) {
            sb.append(" (in ").append(source).append(")");
        }
        if (hint != null && !hint.isBlank()) {
            sb.append("\nTry:\n  ").append(hint);
        }
        return sb.toString();
    }
}