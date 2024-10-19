import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Lexer {

    private String input;
    private int position = 0;
    private int line = 1;
    private int column = 1;
    private int tokenID = 1;
    private XMLWriter xmlWriter;

    public Lexer(String input, XMLWriter xmlWriter) {
        this.input = input;
        this.xmlWriter = xmlWriter;
    }

    // peek next character without advancing
    private char peek() {
        if (position >= input.length()) {
            return '\0';
        } else {
            return input.charAt(position);
        }
    }

    // get next character and advance
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

    // skip whitespace
    private void skipWhitespace() {
        while (Character.isWhitespace(peek())) {
            currentChar();
        }
    }

    // read reserved keywords except for symbols and operators - will get their own
    // functions
    private boolean isKeyword(String value) {
        // TODO: what about 'input'
        return value.equals("main") || value.equals("num") || value.equals("text") || value.equals("begin")
                || value.equals("end") || value.equals("skip") || value.equals("halt") || value.equals("print")
                || value.equals("if") || value.equals("then") || value.equals("else") || value.equals("void")
                || value.equals("add")
                || value.equals("sub") || value.equals("mul") || value.equals("div") || value.equals("and")
                || value.equals("or")
                || value.equals("not") || value.equals("eq") || value.equals("grt") || value.equals("=") || 
                value.equals("< input") || value.equals("return"); // TODO:
    }

    private boolean isOperator(String opValue) {
        switch (opValue) {
            case "add":
            case "sub":
            case "mul":
            case "div":
            case "eq":
            case "grt":
            case "and":
            case "or":
            case "not":
            case "sqrt":
            case "=": // assignment
                return true;
            default:
                return false;
        }
    }

    private boolean isSymbol(char ch) {
        return ch == ';' || ch == ',' || ch == '(' || ch == ')' || ch == '{' || ch == '}';
    }

    // read a number
    private Token readNumber() {
        int startCol = column;
        StringBuilder sb = new StringBuilder(); // buffer for number
        char c = currentChar();
        boolean isFloat = false;

        // while the current character is a digit or a decimal point
        // terminates if char isn't a digit or '.' is encountered more than once
        while (Character.isDigit(c) || (c == '.' && !isFloat)) {
            if (c == '.') {
                isFloat = true;
            }
            sb.append(c);
            c = peek();
            if (Character.isDigit(c)) {
                currentChar();
            }
        }
        
        // ensure there's only one decimal point - throws error if not
        if (isFloat && c == '.') {
            throw new LexerException("Invalid number", line, column);
        }

        return new Token(Token.TokenType.NUMBER, sb.toString(), line, startCol);
    }

    // read operator
    // Read operator (like add, sub, div, or the assignment operator `=`)
    private Token readOperator() {
        int startCol = column;
        StringBuilder sb = new StringBuilder();
        char current = peek();

        // Single-character operators like `=`
        if (current == '=') {
            currentChar(); // Consume the `=` character
            return new Token(Token.TokenType.OPERATOR, "=", line, startCol);
        }

        // Multi-character operators (add, sub, etc.)
        while (Character.isLetter(peek())) {
            sb.append(currentChar());
        }

        String opValue = sb.toString();
        if (isOperator(opValue)) {
            return new Token(Token.TokenType.OPERATOR, opValue, line, startCol);
        } else {
            throw new LexerException("Unrecognized operator: " + opValue, line, startCol);
        }
    }

    // read symbol
    private Token readSymbol() {
        int startCol = column;
        char symbol = currentChar();

        return new Token(Token.TokenType.SYMBOL, Character.toString(symbol), line, startCol);
    }

    // read text constant
    private Token readTextConstant() {
        int startCol = column;
        StringBuilder sb = new StringBuilder();
        currentChar(); // Skip the opening quote

        // Read characters until we find a closing quote or reach the end of input
        while (peek() != '"' && peek() != '\0') {
            sb.append(currentChar());
        }

        // Check for the closing quote
        if (peek() == '\0') {
            throw new LexerException("Unterminated string literal", line, column);
        }

        currentChar(); // Skip the closing quote

        String value = sb.toString();

        // Check if the text constant starts with a capital letter
        if (value.length() > 0 && !Character.isUpperCase(value.charAt(0))) {
            throw new LexerException("Text constant must start with a capital letter: " + value, line, startCol);
        }

        return new Token(Token.TokenType.TEXT_CONSTANT, value, line, startCol);
    }

    private Token readKeywordOrIdentifier() throws IOException {
        int startCol = column;
        StringBuilder sb = new StringBuilder();
        char c = currentChar();

        // read letters first, as it could be a keyword or an identifier
        sb.append(c);
        c = peek();

        while (Character.isLetterOrDigit(c) || c == '_') {
            sb.append(currentChar());
            c = peek();
        }

        String value = sb.toString();

        // check if it's a keyword
        if (isKeyword(value)) {
            // System.out.println("Keyword: " + value);
            xmlWriter.writeToken(tokenID++, "reserved_keyword", value, line, startCol);
            return new Token(Token.TokenType.KEYWORD, value, line, startCol);
        }

        if (isOperator(value)) {
            xmlWriter.writeToken(tokenID++, "reserved_keyword", value, line, startCol);
            return new Token(Token.TokenType.OPERATOR, value, line, startCol);
        }

        // (variable or function name)
        if (value.startsWith("V_")) {
            // System.out.println("Variable: " + value);
            xmlWriter.writeToken(tokenID++, "V", value, line, startCol);
            return new Token(Token.TokenType.VARIABLE_NAME, value, line, startCol);
        }

        if (value.startsWith("F_")) {
            // System.out.println("Function: " + value);
            xmlWriter.writeToken(tokenID++, "F", value, line, startCol);
            return new Token(Token.TokenType.FUNCTION_NAME, value, line, startCol);
        }

        throw new LexerException("Unrecognized keyword or identifier: " + value, line, startCol);
    }

    // Create tokens from the input
    public List<Token> createTokens() throws IOException { // Propagate IOException
        List<Token> tokens = new ArrayList<>();

        skipWhitespace(); // ignore whitespace at the start

        char current = peek();

        // Check if there's any input
        if (current == '\0') {
            throw new LexerException("Program cannot be empty.", line, column);
        }

        // Process tokens
        while (peek() != '\0') {
            skipWhitespace(); // ignore whitespace

            current = peek();

            if (Character.isDigit(current)) {
                Token numberToken = readNumber();
                xmlWriter.writeToken(tokenID++, "N", numberToken.getValue(), line, column);
                tokens.add(numberToken); // Read a number
            } else if (Character.isLetter(current) || current == '_') { // Handle identifiers and keywords
                Token keywordOrIdentifier = readKeywordOrIdentifier();
                tokens.add(keywordOrIdentifier);
            } else if (current == '"') {
                Token textConstant = readTextConstant();
                xmlWriter.writeToken(tokenID++, "T", textConstant.getValue(), line, column);
                tokens.add(textConstant); // Read text constant
            } else if (current == '=') {
                Token operatorToken = readOperator();
                xmlWriter.writeToken(tokenID++, "reserved_keyword", operatorToken.getValue(), line, column);
                tokens.add(operatorToken); // Read assignment operator
            } else if (isSymbol(current)) {
                Token symbolToken = readSymbol();
                xmlWriter.writeToken(tokenID++, "reserved_keyword", symbolToken.getValue(), line, column);
                tokens.add(symbolToken); // Read symbol
            } else {
                throw new LexerException("Unrecognized character: " + current, line, column);
            }

            skipWhitespace(); // ignore whitespace at the end
        }

        xmlWriter.close(); // Close the XML file after writing all tokens
        return tokens;
    }
}

/*
 * TODO:
 * There's a token for text/strings, but it only seems
 * to support text starting with a capital letter.
 * What about the ones not starting with a capital letter?
 * 
 * 
 */