import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class Lexer {

    /** Returned by nextState() when a state has no transition for a character. */
    private static final int NO_TRANSITION = -1;

    private static final int NL = '\n';

    // reserved words (Section 4)

    private static final Set<String> KEYWORDS = new HashSet<String>(Arrays.asList(
            "mold", "blueprint", "from", "new",                                 // instance-first
            "if", "else", "for", "while", "return", "break", "continue",        // control flow
            "print", "scan"));                                                  // input/output

    private static final Set<String> RESERVED_WORDS = new HashSet<String>(Arrays.asList(
            "int", "float", "string", "bool", "char", "void", "object"));

    private static final Set<String> NOISE_WORDS = new HashSet<String>(Arrays.asList(
            "then", "with", "instance", "named"));

    private static final int MAX_IDENTIFIER_LENGTH = 64;
    private static final BigInteger MAX_INT = BigInteger.valueOf(2147483647L);

    // input

    private final int[] src;
    private int pos = 0;
    private int line = 1;

    public Lexer(String sourceCode) {

        String normalized = sourceCode.replace("\r\n", "\n").replace('\r', '\n');
       
        if (normalized.startsWith("\uFEFF﻿")) {
            normalized = normalized.substring(1);
        }
        this.src = normalized.codePoints().toArray();
    }

    //main loop

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<Token>();

        while (pos < src.length) {
            int c = src[pos];

            // blanks are skipped at Q0 and newlines only count lines
            if (c == ' ' || c == '\t') {
                pos++;
                continue;
            }
            if (c == NL) {
                line++;
                pos++;
                continue;
            }

            // run the DFA from Q0, remembering the last accept state (longest match)
            int state = 0;
            int lastAcceptState = NO_TRANSITION;
            int lastAcceptEnd = pos;
            int i = pos;

            while (i < src.length) {
                int next = nextState(state, src[i]);
                if (next == NO_TRANSITION) {
                    break;
                }
                state = next;
                i++;
                if (acceptOf(state) != null) {
                    lastAcceptState = state;
                    lastAcceptEnd = i;
                }
            }

            if (lastAcceptState == NO_TRANSITION) {
                lastAcceptState = 233;
                lastAcceptEnd = pos + 1;
            }

            String lexeme = new String(src, pos, lastAcceptEnd - pos);
            TokenType type = finish(lastAcceptState, lexeme);
            tokens.add(new Token(type, lexeme, line));

            // A multi-line comment (or an unclosed one) can contain newlines.
            line += countNewlines(lexeme);
            pos = lastAcceptEnd;
        }
        return tokens;
    }

    // transition table 

    private static int nextState(int state, int c) {
        switch (state) {

            //  Q0: start (overview and start-state table)
            case 0:
                if (isLetter(c)) return 1;
                if (c == '_') return 208;
                if (isDigit(c)) return 198;
                switch (c) {
                    case '.':  return 199;
                    case '"':  return 201;
                    case '\'': return 204;
                    case '/':  return 172;
                    case '+':  return 2;
                    case '-':  return 3;
                    case '*':  return 4;
                    case '%':  return 222;
                    case '=':  return 195;
                    case '!':  return 225;
                    case '<':  return 227;
                    case '>':  return 229;
                    case '&':  return 9;
                    case '|':  return 10;
                    case ';':  return 178;
                    case ',':  return 185;
                    case ':':  return 184;
                    case '(':  return 181;
                    case ')':  return 182;
                    case '{':  return 179;
                    case '}':  return 180;
                    case '[':  return 186;
                    case ']':  return 187;
                    default:   return 233;   // any other character (# ? \ @ $ ` ~ ^ ...)
                }

            // Part 1: words and numbers
            case 1:     // word starting with a letter
                if (isLetter(c) || isDigit(c) || c == '_') return 1;
                return NO_TRANSITION;

            case 208:   // word starting with _
                if (isLetter(c) || isDigit(c) || c == '_') return 208;
                return NO_TRANSITION;

            case 198:   // digits
                if (isDigit(c)) return 198;
                if (c == '.') return 200;
                if (isLetter(c) || c == '_') return 209;
                return NO_TRANSITION;

            case 199:   // .
                if (isDigit(c)) return 200;
                return NO_TRANSITION;

            case 200:   // digits with one dot
                if (isDigit(c)) return 200;
                if (isLetter(c) || c == '_' || c == '.') return 209;
                return NO_TRANSITION;

            case 209:   // number followed by a letter, _ or a second dot
                if (isLetter(c) || isDigit(c) || c == '_' || c == '.') return 209;
                return NO_TRANSITION;

            // Part 2: string literals
            case 201:   // inside a string
                if (c == '\\') return 202;
                if (c == '"') return 203;
                if (c != NL) return 201;
                return NO_TRANSITION;

            case 202:   // string, after \
                if (isEscapeChar(c)) return 201;
                if (c != NL) return 210;
                return NO_TRANSITION;

            case 210:   // string with a bad escape
                if (c == '\\') return 211;
                if (c == '"') return 212;
                if (c != NL) return 210;
                return NO_TRANSITION;

            case 211:   // bad-escape string, after \
                if (c != NL) return 210;
                return NO_TRANSITION;

            //Part 3: character literals
            case 204:   // after '
                if (c == '\\') return 206;
                if (c == '\'') return 213;
                if (c != NL) return 205;
                return NO_TRANSITION;

            case 205:   // char literal, one character read
                if (c == '\'') return 207;
                if (c != NL) return 214;
                return NO_TRANSITION;

            case 206:   // char literal, after \
                if (isEscapeChar(c)) return 205;
                if (c != NL) return 214;
                return NO_TRANSITION;

            case 214:   // char literal with too many characters or a bad escape
                if (c == '\'') return 213;
                if (c != NL) return 214;
                return NO_TRANSITION;

            //  Part 4: division and comments
            case 172:   // /
                if (c == '/') return 173;
                if (c == '*') return 175;
                if (c == '=') return 215;
                return NO_TRANSITION;

            case 173:   // single-line comment
                if (c != NL) return 173;
                return NO_TRANSITION;

            case 175:   // inside a multi-line comment
                if (c == '*') return 176;
                return 175;

            case 176:   // multi-line comment, after *
                if (c == '*') return 176;
                if (c == '/') return 177;
                return 175;

            // Part 5: arithmetic and compound assignment operators
            case 2:     // +
                if (c == '+') return 216;
                if (c == '=') return 217;
                return NO_TRANSITION;

            case 3:     // -
                if (c == '-') return 218;
                if (c == '=') return 219;
                return NO_TRANSITION;

            case 4:     // *
                if (c == '*') return 220;
                if (c == '=') return 221;
                return NO_TRANSITION;

            case 222:   // %
                if (c == '=') return 223;
                return NO_TRANSITION;

            // Part 6: assignment, relational and logical operators
            case 195:   // =
                if (c == '=') return 224;
                return NO_TRANSITION;

            case 225:   // !
                if (c == '=') return 226;
                return NO_TRANSITION;

            case 227:   // <
                if (c == '=') return 228;
                return NO_TRANSITION;

            case 229:   // >
                if (c == '=') return 230;
                return NO_TRANSITION;

            case 9:     // lone &
                if (c == '&') return 231;
                return NO_TRANSITION;

            case 10:    // lone |
                if (c == '|') return 232;
                return NO_TRANSITION;

            // states with no transitions
            // Q203, Q212, Q207, Q213, Q215, Q177, Q216-Q221, Q223, Q224, Q226, Q228, Q230,
            // Q231, Q232, the delimiters Q178-Q182 and Q184-Q187, and Q233
            default:
                return NO_TRANSITION;
        }
    }

    // accept states

    private static TokenType acceptOf(int state) {
        switch (state) {
            // Part 1: words and numbers
            case 1:   return TokenType.IDENTIFIER;      // changed by the keyword lookup in finish()
            case 208: return TokenType.INVALID;
            case 198: return TokenType.INT_LITERAL;     // checked for range in finish()
            case 199: return TokenType.DOT;
            case 200: return TokenType.FLOAT_LITERAL;
            case 209: return TokenType.INVALID;

            // Part 2: string literals
            case 201: return TokenType.INVALID;         // unclosed string
            case 202: return TokenType.INVALID;
            case 203: return TokenType.STRING_LITERAL;
            case 210: return TokenType.INVALID;         // bad escape
            case 211: return TokenType.INVALID;
            case 212: return TokenType.INVALID;

            // Part 3: character literals
            case 204: return TokenType.INVALID;
            case 205: return TokenType.INVALID;
            case 206: return TokenType.INVALID;
            case 207: return TokenType.CHAR_LITERAL;
            case 214: return TokenType.INVALID;
            case 213: return TokenType.INVALID;

            // Part 4: division and comments
            case 172: return TokenType.DIV_OP;
            case 215: return TokenType.DIV_ASSIGN_OP;
            case 173: return TokenType.COMMENT;
            case 175: return TokenType.INVALID;         // unclosed multi-line comment
            case 176: return TokenType.INVALID;
            case 177: return TokenType.COMMENT;

            // Part 5: arithmetic and compound assignment operators
            case 2:   return TokenType.ADD_OP;
            case 216: return TokenType.INCREMENT_OP;
            case 217: return TokenType.ADD_ASSIGN_OP;
            case 3:   return TokenType.SUB_OP;
            case 218: return TokenType.DECREMENT_OP;
            case 219: return TokenType.SUB_ASSIGN_OP;
            case 4:   return TokenType.MUL_OP;
            case 220: return TokenType.EXPONENT_OP;
            case 221: return TokenType.MUL_ASSIGN_OP;
            case 222: return TokenType.MOD_OP;
            case 223: return TokenType.MOD_ASSIGN_OP;

            // Part 6: assignment, relational and logical operators
            case 195: return TokenType.ASSIGN_OP;
            case 224: return TokenType.EQUAL_OP;
            case 225: return TokenType.NOT_OP;
            case 226: return TokenType.NOT_EQUAL_OP;
            case 227: return TokenType.LESS_OP;
            case 228: return TokenType.LESS_EQUAL_OP;
            case 229: return TokenType.GREATER_OP;
            case 230: return TokenType.GREATER_EQUAL_OP;
            case 9:   return TokenType.INVALID;         // lone &
            case 231: return TokenType.AND_OP;
            case 10:  return TokenType.INVALID;         // lone |
            case 232: return TokenType.OR_OP;

            // Part 7: delimiters, brackets and unknown symbols
            case 178: return TokenType.SEMICOLON;
            case 185: return TokenType.COMMA;
            case 184: return TokenType.COLON;
            case 181: return TokenType.LEFT_PAREN;
            case 182: return TokenType.RIGHT_PAREN;
            case 179: return TokenType.LEFT_BRACE;
            case 180: return TokenType.RIGHT_BRACE;
            case 186: return TokenType.LEFT_BRACKET;
            case 187: return TokenType.RIGHT_BRACKET;
            case 233: return TokenType.INVALID;         // unknown symbol

            default:  return null;                      // Q0 is not an accept state
        }
    }

    // checks at accept states

    private static TokenType finish(int state, String lexeme) {
        if (state == 1) {
            // Keyword lookup, in this order: KEYWORD, RESERVED_WORD, BOOL/NULL_LITERAL, NOISE_WORD, IDENTIFIER
            if (KEYWORDS.contains(lexeme)) return TokenType.KEYWORD;
            if (RESERVED_WORDS.contains(lexeme)) return TokenType.RESERVED_WORD;
            if (lexeme.equals("true") || lexeme.equals("false")) return TokenType.BOOL_LITERAL;
            if (lexeme.equals("null")) return TokenType.NULL_LITERAL;
            if (NOISE_WORDS.contains(lexeme)) return TokenType.NOISE_WORD;
            if (lexeme.length() > MAX_IDENTIFIER_LENGTH) return TokenType.INVALID;
            return TokenType.IDENTIFIER;
        }
        if (state == 198) {
            // An integer must fit in a 32-bit signed int.
            if (new BigInteger(lexeme).compareTo(MAX_INT) > 0) return TokenType.INVALID;
            return TokenType.INT_LITERAL;
        }
        return acceptOf(state);
    }

    // character classes 

    // ALPHABET: A-Z and a-z only. 
    private static boolean isLetter(int c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    // DIGIT: 0-9 only. 
    private static boolean isDigit(int c) {
        return c >= '0' && c <= '9';
    }

    // The characters allowed after \ in an escape sequence: n t " ' \ (Section 4.c). 
    private static boolean isEscapeChar(int c) {
        return c == 'n' || c == 't' || c == '"' || c == '\'' || c == '\\';
    }

    private static int countNewlines(String text) {
        int count = 0;
        for (int k = 0; k < text.length(); k++) {
            if (text.charAt(k) == '\n') {
                count++;
            }
        }
        return count;
    }
}
