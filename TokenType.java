/**
 * Every token the CLAY lexer can produce (Section 12.a of the CLAY proposal).
 */
public enum TokenType {
    // Words (Sections 2, 4 and 5)
    KEYWORD,
    RESERVED_WORD,
    NOISE_WORD,
    IDENTIFIER,

    // Constants (Section 4.c)
    INT_LITERAL,
    FLOAT_LITERAL,
    STRING_LITERAL,
    CHAR_LITERAL,
    BOOL_LITERAL,
    NULL_LITERAL,

    // Comments (Section 6)
    COMMENT,

    // Arithmetic operators (Section 3.a)
    ADD_OP,
    SUB_OP,
    MUL_OP,
    DIV_OP,
    MOD_OP,
    EXPONENT_OP,
    INCREMENT_OP,
    DECREMENT_OP,

    // Relational operators (Section 3.b)
    LESS_OP,
    GREATER_OP,
    LESS_EQUAL_OP,
    GREATER_EQUAL_OP,
    EQUAL_OP,
    NOT_EQUAL_OP,

    // Logical operators (Section 3.b)
    NOT_OP,
    AND_OP,
    OR_OP,

    // Assignment operators (Section 3.c)
    ASSIGN_OP,
    ADD_ASSIGN_OP,
    SUB_ASSIGN_OP,
    MUL_ASSIGN_OP,
    DIV_ASSIGN_OP,
    MOD_ASSIGN_OP,

    // Delimiters and brackets (Section 8)
    SEMICOLON,
    COMMA,
    COLON,
    DOT,
    LEFT_PAREN,
    RIGHT_PAREN,
    LEFT_BRACE,
    RIGHT_BRACE,
    LEFT_BRACKET,
    RIGHT_BRACKET,

    // Any lexeme that matches no rule (Section 12.c)
    INVALID
}
