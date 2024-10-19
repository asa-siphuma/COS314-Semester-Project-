public class LexerException extends RuntimeException {
    public LexerException(String message, int line, int column) {
        super(String.format("Lexer Error: %s at line %d, column %d", message, line, column));
    }
}
