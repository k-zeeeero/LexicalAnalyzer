CLAY LEXICAL ANALYZER (Java)
Group 7 - CLAY: Shape Before You Build

REQUIREMENTS
  Java 8 or newer (JDK). Check with:  javac -version

HOW TO COMPILE AND RUN
  Open a terminal in this folder, then:

    javac *.java
    java ClayLexer demo.cly

  The symbol table is printed to the console and saved to symbol_table.txt
  in the same folder.

  Only .cly files are accepted (.cly or .CLY). Any other file is rejected
  before reading, and no symbol table is created.

FILES
  ClayLexer.java  Main program: checks the input file, runs the lexer, prints
                  the symbol table and saves symbol_table.txt (paper 12.f).
  Lexer.java      The single DFA from paper Section 12.b, written as code:
                    nextState()  = the transition table (same state numbers as the paper)
                    acceptOf()   = the "Accepts" column
                    finish()     = keyword lookup at Q1 and the integer range check at Q198
  Token.java      One row of the symbol table: token type, lexeme, line.
  TokenType.java  The 45 token types (paper Section 12.a).
  demo.cly        A demo program that produces all 45 token types,
                  including 12 INVALID lexemes at the end.

HOW IT WORKS (for the defense)
  1. Blanks (space, tab, newline) are skipped; newlines increase the line count.
  2. From state Q0, the lexer follows one transition per character until no
     transition exists (longest match).
  3. It emits the token of the last accept state it passed, then starts again
     at Q0 with the next character.
  4. Invalid input ends in an accept state that gives INVALID, so the lexer
     lists it and keeps going instead of stopping.
