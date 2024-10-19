public class Token {

    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }

    /**
     * Token types
     */
    public enum TokenType {
        KEYWORD,
        VARIABLE_NAME,
        FUNCTION_NAME,
        TEXT_CONSTANT,
        NUMBER,
        OPERATOR,
        SYMBOL,
        EOF
    }

    
    private TokenType type;
    private String value;
    private int lineNumber;   // used to track vertical position in the input stream
    private int columnNumber; // used to track horizontal position in the input stream - handy for error reporting
    private String tokenClass;

    public Token(TokenType type, String value, int lineNumber, int columnNumber) {
        this.type = type;
        this.value = value;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    // getters
    public TokenType getType() {
        return type;
    }


    public String getValue() {
        return value;
    }
    public int getLineNumber() {
        return lineNumber;
    }

    public int getColumnNumber() {
        return columnNumber;
    }

    public String getTokenClass() {
        return tokenClass;
    }

    public void setTokenClass(String tokenClass) {
        this.tokenClass = tokenClass;
    }

    // print token
    @Override
    public String toString() {
        return String.format("Type: " + type + ", Value: " + value + ", Line Number: " + lineNumber, "Column Number: " + columnNumber);
    }
}
