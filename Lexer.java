import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Lexer {
    private String input;
    private int position = 0;
    private int line = 1;
    private int column = 1;
    private int tokenID = 1;
    private XMLWriter xmlWriter;

    // Regular expression patterns for validation
    private static final Pattern TEXT_PATTERN = Pattern.compile("[A-Z][a-z]{0,7}");
    private static final Pattern NUMBER_PATTERN = Pattern.compile(
        "0|0\\.([0-9])[1-9]|-0\\.([0-9])[1-9]|[1-9]([0-9])*|" +
        "-[1-9]([0-9])|[1-9]([0-9])\\.([0-9])[1-9]|-[1-9]([0-9])\\.([0-9])*[1-9]"
    );

    public Lexer(String input, XMLWriter xmlWriter) {
        this.input = input;
        this.xmlWriter = xmlWriter;
    }

    private char peek() {
        if (position >= input.length()) {
            return '\0';
        }
        return input.charAt(position);
    }

    private char peekNext() {
        if (position + 1 >= input.length()) {
            return '\0';
        }
        return input.charAt(position + 1);
    }

    private char currentChar() {
        char c = peek();
        position++;
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    private void skipWhitespace() {
        while (Character.isWhitespace(peek())) {
            currentChar();
        }
    }

    private boolean isKeyword(String value) {
        return value.equals("main") || value.equals("num") || value.equals("text") || 
               value.equals("begin") || value.equals("end") || value.equals("skip") || 
               value.equals("halt") || value.equals("print") || value.equals("if") || 
               value.equals("then") || value.equals("else") || value.equals("void") || 
               value.equals("add") || value.equals("sub") || value.equals("mul") || 
               value.equals("div") || value.equals("and") || value.equals("or") ||
               value.equals("not") || value.equals("eq") || value.equals("grt") || 
               value.equals("=") || value.equals("&lt; input") || value.equals("return");
    }

    private boolean isOperator(String opValue) {
        return switch (opValue) {
            case "add", "sub", "mul", "div", "eq", "grt", 
                 "and", "or", "not", "sqrt", "=", "&lt; input" -> true;
            default -> false;
        };
    }

    private boolean isSymbol(char ch) {
        return ch == ';' || ch == ',' || ch == '(' || ch == ')' || ch == '{' || ch == '}';
    }

    // Updated readOperator method
    private Token readOperator() {
        int startCol = column;
        StringBuilder sb = new StringBuilder();
        char current = peek();
    
        // Handle single-character operators
        if (current == '=') {
            currentChar(); // Consume the '=' character
            return new Token(Token.TokenType.OPERATOR, "=", line, startCol);
        }
    
        // Handle '< input' special case
        if (current == '<') {
            currentChar(); // consume '<'
            if (peek() == ' ') {
                currentChar(); // consume space
                String remaining = "input";
                for (char c : remaining.toCharArray()) {
                    if (peek() != c) {
                        throw new LexerException("Invalid input operator", line, startCol);
                    }
                    currentChar();
                }
                return new Token(Token.TokenType.OPERATOR, "&lt; input", line, startCol);
            }
            throw new LexerException("Invalid operator", line, startCol);
        }
    
        // Handle multi-character operators
        while (Character.isLetter(peek())) {
            sb.append(currentChar());
        }
    
        String opValue = sb.toString();
        if (isOperator(opValue)) {
            return new Token(Token.TokenType.OPERATOR, opValue, line, startCol);
        }
    
        if (!opValue.isEmpty()) {  // Only throw if we actually read something
            throw new LexerException("Invalid operator: " + opValue, line, startCol);
        }
        
        throw new LexerException("Invalid operator", line, startCol);
    }

    // Updated readSymbol method
    private Token readSymbol() {
        int startCol = column;
        char symbol = peek();
        
        if (!isSymbol(symbol)) {
            throw new LexerException("Invalid symbol: " + symbol, line, startCol);
        }
        
        currentChar(); // consume the symbol
        return new Token(Token.TokenType.SYMBOL, String.valueOf(symbol), line, startCol);
    }

    private Token readNumber() {
        int startCol = column;
        StringBuilder sb = new StringBuilder();
        
        // Handle negative numbers
        if (peek() == '-') {
            sb.append(currentChar());
        }
        
        // Read first digit
        char c = peek();
        if (!Character.isDigit(c)) {
            throw new LexerException("Invalid number format", line, column);
        }
        
        // Handle first digit with special case for 0
        if (c == '0') {
            sb.append(currentChar());
            // After 0, only decimal point is allowed
            if (peek() != '.' && peek() != '\0' && !Character.isWhitespace(peek()) && !isSymbol(peek())) {
                throw new LexerException("Invalid number format: leading zero", line, column);
            }
        } else {
            // Read digits before decimal point
            while (Character.isDigit(peek())) {
                sb.append(currentChar());
            }
        }
        
        // Handle decimal point and following digits
        if (peek() == '.') {
            sb.append(currentChar());
            boolean hasDigitsAfterDecimal = false;
            StringBuilder decimalPart = new StringBuilder();
            
            while (Character.isDigit(peek())) {
                decimalPart.append(currentChar());
                hasDigitsAfterDecimal = true;
            }
            
            // Check for trailing zeros and valid decimal format
            if (!hasDigitsAfterDecimal || decimalPart.toString().endsWith("0")) {
                throw new LexerException("Invalid decimal number format", line, column);
            }
            
            sb.append(decimalPart);
        }
        
        String number = sb.toString();
        if (!NUMBER_PATTERN.matcher(number).matches()) {
            throw new LexerException("Invalid number format: " + number, line, startCol);
        }
        
        return new Token(Token.TokenType.NUMBER, number, line, startCol);
    }

    // private boolean isValidIdentifierPart(String name) {
    //     return name.matches("[a-z]([a-z0-9])*");
    // }
    
    // // Helper method to print token information during debugging
    // private void debugToken(String tokenType, String value, int line, int column) {
    //     System.out.println(String.format("Token: type=%s, value='%s', line=%d, column=%d",
    //         tokenType, value, line, column));
    // }

    private Token readTextConstant() {
        int startCol = column;
        StringBuilder sb = new StringBuilder();
        currentChar(); // Skip the opening quote
        
        int length = 0;
        while (peek() != '"' && peek() != '\0') {
            if (length >= 8) {
                throw new LexerException("Text constant exceeds maximum length of 8 characters", line, startCol);
            }
            sb.append(currentChar());
            length++;
        }
        
        if (peek() == '\0') {
            throw new LexerException("Unterminated string literal", line, column);
        }
        
        currentChar(); // Skip the closing quote
        String value = sb.toString();
        
        if (!TEXT_PATTERN.matcher(value).matches()) {
            throw new LexerException("Invalid text constant format. Must be [A-Z][a-z]{0,7}", line, startCol);
        }
        
        return new Token(Token.TokenType.TEXT_CONSTANT, value, line, startCol);
    }

    private Token readKeywordOrIdentifier() throws IOException {
        int startCol = column;
        StringBuilder sb = new StringBuilder();
        char c = currentChar();
        
        sb.append(c);
        while (Character.isLetterOrDigit(peek()) || peek() == '_') {
            sb.append(currentChar());
        }
        
        String value = sb.toString();
        
        if (isKeyword(value)) {
            xmlWriter.writeToken(tokenID++, "reserved_keyword", value, line, startCol);
            return new Token(Token.TokenType.KEYWORD, value, line, startCol);
        }
        
        if (isOperator(value)) {
            xmlWriter.writeToken(tokenID++, "reserved_keyword", value, line, startCol);
            return new Token(Token.TokenType.OPERATOR, value, line, startCol);
        }
        
        if (value.startsWith("V_")) {
            // Check if the rest of the variable name follows the pattern: [a-z]([a-z]|[0-9])*
            String varName = value.substring(2);
            if (!varName.matches("[a-z]([a-z0-9])*")) {
                throw new LexerException("Invalid variable name format: " + value, line, startCol);
            }
            xmlWriter.writeToken(tokenID++, "V", value, line, startCol);
            return new Token(Token.TokenType.VARIABLE_NAME, value, line, startCol);
        }
        
        if (value.startsWith("F_")) {
            // Check if the rest of the function name follows the pattern: [a-z]([a-z]|[0-9])*
            String funcName = value.substring(2);
            if (!funcName.matches("[a-z]([a-z0-9])*")) {
                throw new LexerException("Invalid function name format: " + value, line, startCol);
            }
            xmlWriter.writeToken(tokenID++, "F", value, line, startCol);
            return new Token(Token.TokenType.FUNCTION_NAME, value, line, startCol);
        }
        
        throw new LexerException("Unrecognized keyword or identifier: " + value, line, startCol);
    }

    public List<Token> createTokens() throws IOException {
        List<Token> tokens = new ArrayList<>();
        
        skipWhitespace();
        
        if (peek() == '\0') {
            throw new LexerException("Program cannot be empty.", line, column);
        }
        
        while (peek() != '\0') {
            skipWhitespace();
            char current = peek();
            
            Token token = null;
            if (current == '-' && Character.isDigit(peekNext())) {
                token = readNumber();
                xmlWriter.writeToken(tokenID++, "N", token.getValue(), line, column);
            } else if (Character.isDigit(current)) {
                token = readNumber();
                xmlWriter.writeToken(tokenID++, "N", token.getValue(), line, column);
            } else if (Character.isLetter(current) || current == '_') {
                token = readKeywordOrIdentifier();
            } else if (current == '"') {
                token = readTextConstant();
                xmlWriter.writeToken(tokenID++, "T", token.getValue(), line, column);
            } else if (current == '=' || current == '<') {
                token = readOperator();
                xmlWriter.writeToken(tokenID++, "reserved_keyword", token.getValue(), line, column);
            } else if (isSymbol(current)) {
                token = readSymbol();
                xmlWriter.writeToken(tokenID++, "reserved_keyword", token.getValue(), line, column);
            } else {
                throw new LexerException("Unrecognized character: " + current, line, column);
            }
            
            tokens.add(token);
            skipWhitespace();
        }
        
        xmlWriter.close();
        return tokens;
    }
}