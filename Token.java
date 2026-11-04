/**
 * One row of the symbol table: the token type, the exact text (lexeme),
 * and the line where the lexeme starts.
 */
public class Token {
    private final TokenType type;
    private final String lexeme;
    private final int line;

    public Token(TokenType type, String lexeme, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
    }

    public TokenType getType() {
        return type;
    }

    public String getLexeme() {
        return lexeme;
    }

    public int getLine() {
        return line;
    }

    @Override
    public String toString() {
        return line + "  " + lexeme + "  " + type;
    }
}
