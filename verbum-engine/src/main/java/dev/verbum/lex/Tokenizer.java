package dev.verbum.lex;

import dev.verbum.error.VerbumError;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The tokenizer (lexer) for Verbum.
 *
 * Verbum is a whitespace-delimited, punctuation-free language, so "tokenizing"
 * means: split the file into lines, record indentation, and split each line into
 * words and numbers. Indentation becomes the only notion of structure.
 *
 * Comments are ignored: any line whose content starts with "note" or with "#".
 */
public final class Tokenizer {

    /** A number: optional minus, digits with optional thousands commas, optional fraction. */
    private static final Pattern NUMBER = Pattern.compile(
            "^-?\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?$|-?\\d+(?:\\.\\d+)?$");

    private final String source;
    private final String filename;

    public Tokenizer(String source, String filename) {
        this.source = source;
        this.filename = filename;
    }

    /** Tokenize the whole source into a list of logical lines (comments and blanks removed). */
    public List<Line> tokenize() {
        List<Line> lines = new ArrayList<>();
        String[] raw = source.replace("\r\n", "\n").split("\n", -1);
        for (int i = 0; i < raw.length; i++) {
            int lineNumber = i + 1;
            String original = raw[i];

            // Measure indentation in spaces (tabs treated as 4 spaces).
            int indent = 0;
            int j = 0;
            while (j < original.length()) {
                char c = original.charAt(j);
                if (c == ' ') { indent++; j++; }
                else if (c == '\t') { indent += 4; j++; }
                else if (c == '#') { break; /* inline comment to end of line */ }
                else break;
            }
            String content = original.substring(j).trim();

            if (content.isEmpty()) continue;

            // Full-line comments.
            if (content.equalsIgnoreCase("note") || startsWithWord(content, "note")
                    || content.startsWith("#")) {
                continue;
            }

            List<Token> tokens = tokenizeLine(content, lineNumber, indent);
            if (tokens.isEmpty()) continue;

            lines.add(new Line(lineNumber, indent, tokens));
        }
        return lines;
    }

    private boolean startsWithWord(String content, String word) {
        return content.toLowerCase().startsWith(word + " ");
    }

    private List<Token> tokenizeLine(String content, int lineNumber, int indent) {
        List<Token> tokens = new ArrayList<>();
        String[] parts = content.split("\\s+");
        for (String part : parts) {
            if (part.isEmpty()) continue;
            Matcher m = NUMBER.matcher(part);
            if (m.matches()) {
                String normalized = part.replace(",", "");
                try {
                    tokens.add(Token.number(new BigDecimal(normalized)));
                } catch (NumberFormatException e) {
                    throw new VerbumError(lineNumber,
                            "I could not read the number " + part + ".\n" +
                            "Try writing it with no special symbols, like  100  or  1.5");
                }
            } else {
                tokens.add(Token.word(part));
            }
        }
        return tokens;
    }
}
