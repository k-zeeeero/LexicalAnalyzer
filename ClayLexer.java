import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class ClayLexer {

    private static final String OUTPUT_FILE = "symbol_table.txt";
    private static final int MAX_LEXEME_WIDTH = 40;

    public static void main(String[] args) {
        // Print UTF-8 so characters such as é or emoji in strings show correctly.
        PrintStream out = new PrintStream(System.out, true);
        try {
            out = new PrintStream(System.out, true, "UTF-8");
        } catch (IOException ignored) {
            // keep the default console encoding
        }

        if (args.length != 1) {
            out.println("Usage: java ClayLexer <file.cly>");
            System.exit(1);
        }

        String path = args[0];
        File file = new File(path);
        String fileName = file.getName();

        // Only .cly files can be read (checked before reading).
        if (!fileName.toLowerCase().endsWith(".cly")) {
            out.println("Error: '" + path + "' is not a CLAY source file. Only .cly files can be read.");
            System.exit(1);
        }

        // The file must exist.
        if (!file.isFile()) {
            out.println("Error: file '" + path + "' not found.");
            System.exit(1);
        }

        String source;
        try {
            source = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            out.println("Error: file '" + path + "' could not be read.");
            System.exit(1);
            return;
        }

        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.tokenize();
        String table = buildSymbolTable(tokens, fileName);

        out.print(table);

        Writer writer = null;
        try {
            writer = new OutputStreamWriter(Files.newOutputStream(new File(OUTPUT_FILE).toPath()), StandardCharsets.UTF_8);
            writer.write(table);
            out.println();
            out.println("Symbol table saved to " + OUTPUT_FILE);
        } catch (IOException e) {
            out.println("Error: could not write " + OUTPUT_FILE + ".");
            System.exit(1);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignored) {
                    // nothing else to do
                }
            }
        }
    }

    // ---------------------------------------------------------------- symbol table

    /** Builds the symbol table text: title, the table, the total, and a count for each token type. */
    static String buildSymbolTable(List<Token> tokens, String fileName) {
        String[] headers = {"No.", "Line", "Lexeme", "Token"};
        String[][] rows = new String[tokens.size()][];
        for (int i = 0; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            rows[i] = new String[] {
                    String.valueOf(i + 1),
                    String.valueOf(t.getLine()),
                    displayLexeme(t.getLexeme()),
                    t.getType().name()
            };
        }

        // Each column is as wide as its longest entry.
        int[] widths = new int[headers.length];
        for (int c = 0; c < headers.length; c++) {
            widths[c] = headers[c].length();
            for (String[] row : rows) {
                widths[c] = Math.max(widths[c], displayWidth(row[c]));
            }
        }

        StringBuilder sb = new StringBuilder();
        String nl = System.lineSeparator();
        sb.append("CLAY LEXICAL ANALYZER - SYMBOL TABLE").append(nl);
        sb.append("Source file: ").append(fileName).append(nl);
        sb.append(nl);
        sb.append(border(widths, '-')).append(nl);
        sb.append(row(headers, widths)).append(nl);
        sb.append(border(widths, '=')).append(nl);
        for (String[] r : rows) {
            sb.append(row(r, widths)).append(nl);
        }
        sb.append(border(widths, '-')).append(nl);
        sb.append(nl);

        // Total and a count per token type (alphabetical).
        sb.append("Total tokens: ").append(tokens.size()).append(nl);
        Map<String, Integer> counts = new TreeMap<String, Integer>();
        int longestName = 0;
        for (Token t : tokens) {
            String name = t.getType().name();
            Integer old = counts.get(name);
            counts.put(name, old == null ? 1 : old + 1);
            longestName = Math.max(longestName, name.length());
        }
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            sb.append("  ").append(pad(e.getKey(), longestName + 2)).append(e.getValue()).append(nl);
        }
        return sb.toString();
    }

    /**
     * How a lexeme is shown in the table: newlines inside a multi-line comment are shown as \n,
     * tabs as \t, and lexemes longer than 40 characters are cut with "...".
     */
    static String displayLexeme(String lexeme) {
        String shown = lexeme.replace("\n", "\\n").replace("\t", "\\t");
        int length = shown.codePointCount(0, shown.length());
        if (length > MAX_LEXEME_WIDTH) {
            int cut = shown.offsetByCodePoints(0, MAX_LEXEME_WIDTH - 3);
            shown = shown.substring(0, cut) + "...";
        }
        return shown;
    }

    private static String border(int[] widths, char fill) {
        StringBuilder sb = new StringBuilder("+");
        for (int w : widths) {
            for (int k = 0; k < w + 2; k++) {
                sb.append(fill);
            }
            sb.append('+');
        }
        return sb.toString();
    }

    private static String row(String[] cells, int[] widths) {
        StringBuilder sb = new StringBuilder("|");
        for (int c = 0; c < cells.length; c++) {
            sb.append(' ').append(pad(cells[c], widths[c])).append(" |");
        }
        return sb.toString();
    }

    private static String pad(String text, int width) {
        StringBuilder sb = new StringBuilder(text);
        for (int k = displayWidth(text); k < width; k++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    /** Number of characters as a person counts them (an emoji counts as one). */
    private static int displayWidth(String text) {
        return text.codePointCount(0, text.length());
    }
}
