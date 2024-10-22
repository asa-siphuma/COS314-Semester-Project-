import java.io.IOException;
import java.util.List;
import java.util.Set;

public class Parser {

    private List<Token> tokens;
    private int currentTokenIndex = 0;
    private int unid = 0;
    private SymbolTable symbolTable = new SymbolTable();
    private Set<String> reservedKeywords = Set.of("if", "else", "while", "return", "print", "main"); // Reserved
                                                                                                     // keywords

    public Parser(List<Token> tokens) throws IOException {
        this.tokens = tokens;
    }

    // Get the current token
    private Token currentToken() {
        if (currentTokenIndex < tokens.size()) {
            System.out.println("Token: " + tokens.get(currentTokenIndex).getValue());
            return tokens.get(currentTokenIndex);
        }
        return new Token(Token.TokenType.EOF, "", -1, -1); // End of file token
    }

    // Advance to the next token
    private void nextToken() {
        currentTokenIndex++;
    }

    // Get the n-th token after the current
    private Token lookahead(int n) {
        return tokens.get(currentTokenIndex + n);
    }

    // Throw a syntax error
    private void syntaxError(String message) {
        Token current = currentToken();
        throw new ParserException("Syntax error at line " + current.getLineNumber() + ", column "
                + current.getColumnNumber() + ": " + message);
    }

    // Match the current token with an expected type and value
    private void match(Token.TokenType expectedType, String expectedValue) {
        Token current = currentToken();
        if (current.getType() == expectedType && current.getValue().equals(expectedValue)) {
            nextToken();
        } else {
            syntaxError("Expected '" + expectedValue + "', but found '" + current.getValue() + "'");
        }
    }

    // Entry point for parsing the program (PROG ::= main GLOBVARS ALGO FUNCTIONS)
    public void parseProgram() {
        match(Token.TokenType.KEYWORD, "main"); // Must start with 'main'
        parseGlobVars();
        parseAlgo();
        parseFunctions();
        System.out.println("Parsing program: " + currentToken().getValue());
        match(Token.TokenType.KEYWORD, "end");

        if (currentToken().getType() != Token.TokenType.EOF) {
            syntaxError("Unexpected token after program end");
        }
    }

    // Parse global variables (GLOBVARS ::= VTYP VNAME , GLOBVARS | ε)
    private void parseGlobVars() {
        if (currentToken().getValue().equals("num") || currentToken().getValue().equals("text")) {
            parseVarDeclaration(false); // Handle variable declaration
        }
        // If no 'num' or 'text', global vars are empty
    }

    // Parse a variable declaration (VTYP VNAME) - TODO
    // the boolean stops the parser form consuming the commas in func parameters or
    // locvars
    private void parseVarDeclaration(boolean isInFunctionParameters) {
        System.out.println("Parsing variable declaration: " + currentToken().getValue());
        Token varType = currentToken();
        if (varType.getValue().equals("num") || varType.getValue().equals("text")) {
            nextToken(); // Consume 'num' or 'text'
            if (currentToken().getType() == Token.TokenType.VARIABLE_NAME) {
                if (Character.isDigit(currentToken().getValue().charAt(2))) {
                    syntaxError("Variable name cannot start with a digit");
                }
                
                // if the characters after the V_ is a keyword give an error
                String subString = currentToken().getValue().substring(2);
                if (reservedKeywords.contains(subString)) {
                    syntaxError("Variable name cannot be a keyword");
                }

                nextToken(); // Consume variable name
            } else {
                syntaxError("Expected variable name after type");
            }
            if (!isInFunctionParameters && currentToken().getValue().equals(",")) {
                nextToken(); // Consume comma
                parseVarDeclaration(false); // Recursively handle the next variable
            }

        } else if (currentToken().getValue().equals("begin")) {
            return;
        } else {
            syntaxError("Expected 'num' or 'text' for variable type");
        }
    }

    // Parse algorithm block (ALGO ::= begin INSTRUC end)
    private void parseAlgo() {
        match(Token.TokenType.KEYWORD, "begin");
        parseInstruc(); // Handle instructions inside begin-end block
        match(Token.TokenType.KEYWORD, "end");
    }

    // Parse instructions (INSTRUC ::= COMMAND ; INSTRUC | ε)
    private void parseInstruc() {
        // If the current token is 'end', we're done with instructions
        if (currentToken().getValue().equals("end")) {
            return; // End of instruction list
        }

        // Parse a command
        parseCommand();

        // Match a semicolon
        if (currentToken().getValue().equals(";")) {
            nextToken(); // Consume the semicolon
        } else {
            syntaxError("Expected ';' after command, but found '" + currentToken().getValue() + "'");
        }

        // Recursively parse the next instruction
        parseInstruc();
    }

    // Parse a command (COMMAND ::= skip | halt | print ATOMIC | ASSIGN | CALL |
    // BRANCH)
    private void parseCommand() {
        System.out.println("Parsing command: ");
        Token command = currentToken();

        // if command include "F_"
        if (command.getValue().startsWith("F_")) {
            parseFunctionCall();
            return;
        }

        switch (command.getValue()) {
            case "skip":
            case "halt":
                nextToken(); // Simple commands: 'skip' or 'halt'
                break;
            case "print":
                nextToken(); // Consume 'print'
                parseAtomic(); // Expect an atomic value after 'print'
                break;
            case "return":
                nextToken(); // Consume 'return'
                parseAtomic(); // Expect an atomic value after 'return'
                break;
            case "if":
                parseBranch(); // Conditional (if-else)
                break;
            default:
                // System.out.println("Assignment: " + command.getValue());
                parseAssign(); // Handle variable assignment
                break;
        }
    }

    // Parse an assignment (ASSIGN ::= VNAME = TERM)
    private void parseAssign() {
        if (currentToken().getType() == Token.TokenType.VARIABLE_NAME) {
            if (Character.isDigit(currentToken().getValue().charAt(2))) {
                syntaxError("Variable name cannot start with a digit");
            }
            // if the characters after the V_ is a keyword give an error
            String subString = currentToken().getValue().substring(2);
            if (reservedKeywords.contains(subString)) {
                syntaxError("Variable name cannot be a keyword");
            }
            nextToken(); // Consume the variable name
            match(Token.TokenType.KEYWORD, "="); // Consume '='
            parseTerm(); // Parse the term being assigned
        } else {
            syntaxError("Expected variable name for assignment");
        }
    }

    // Parse a term (TERM ::= ATOMIC | CALL | OP)
    private void parseTerm() {
        System.out.println("Parsing term: " + currentToken().getValue());
        System.out.println(currentToken().getType());
        if (currentToken().getType() == Token.TokenType.VARIABLE_NAME
                || currentToken().getType() == Token.TokenType.NUMBER
                || currentToken().getType() == Token.TokenType.TEXT_CONSTANT) {
            parseAtomic(); // Parse atomic term (simple values like variables or constants)
        } else if (currentToken().getType() == Token.TokenType.FUNCTION_NAME) {
            parseFunctionCall(); // Parse function call as a term
        } else if (isBinOp(currentToken()) || isUnOp(currentToken())) {
            parseOp(); // Parse binary/unary operation
        } else {
            syntaxError("Invalid term");
        }
    }

    // Parse atomic values (ATOMIC ::= VNAME | CONST)
    private void parseAtomic() {
        System.out.println("Parsing atomic: " + currentToken().getValue());
        Token atomic = currentToken();
        if (atomic.getType() == Token.TokenType.VARIABLE_NAME || atomic.getType() == Token.TokenType.NUMBER
                || atomic.getType() == Token.TokenType.TEXT_CONSTANT) {

            if (atomic.getValue().startsWith("V_")) {
                if (Character.isDigit(atomic.getValue().charAt(2))) {
                    syntaxError("Variable name cannot start with a digit");
                }

                // if the characters after the V_ is a keyword give an error
                String subString = currentToken().getValue().substring(2);
                if (reservedKeywords.contains(subString)) {
                    syntaxError("Variable name cannot be a keyword");
                }
            }

            nextToken(); // Consume atomic value
        } else {
            syntaxError("Expected an atomic value (variable, number, or text constant)");
        }
    }

    // Parse unary or binary operations
    private void parseOp() {
        Token op = currentToken();
        System.out.println("Parsing operator: " + op.getValue());

        if (isUnOp(op)) {
            // Unary operation
            nextToken(); // Consume the unary operator
            match(Token.TokenType.KEYWORD, "("); // Expect an opening parenthesis

            parseArg(); // Parse the argument for the unary operation

            match(Token.TokenType.KEYWORD, ")"); // Expect closing parenthesis
        } else if (isBinOp(op)) {
            // Binary operation
            parseOpBinop(); // Parse binary operation
        } else {
            syntaxError("Expected an operator");
        }
    }

    // Parse binary operation with two arguments
    private void parseOpBinop() {
        Token op = currentToken();
        System.out.println("Parsing binary operator: " + op.getValue());

        nextToken(); // Move past the binary operator
        match(Token.TokenType.KEYWORD, "("); // Expect opening parenthesis

        // Parse first argument
        parseArg();

        match(Token.TokenType.KEYWORD, ","); // Expect a comma

        // Parse second argument
        parseArg();

        match(Token.TokenType.KEYWORD, ")"); // Expect closing parenthesis

        // if (lookahead(1).getValue().equals("(")) {
        // System.out.println("oo");
        // nextToken();
        // }

        System.out.println("DONE parsing binary operation: " + currentToken().getValue());
    }

    // Parse an argument, which could be an atomic value or another operation
    private void parseArg() {
        System.out.println("Parsing argument: " + currentToken().getValue());

        if (isAtomic(currentToken())) {
            System.out.println("Parsing atomic arg: " + currentToken().getValue());
            parseAtomic(); // Argument is atomic
        } else if (isBinOp(currentToken()) || isUnOp(currentToken())) {
            // nextToken();
            parseOp(); // Argument is a nested operation
        } else if (currentToken().getValue().equals("(")) { // Argument is a nested operation
            System.out.println("Parsing NESTED arg: " + currentToken().getValue());
            match(Token.TokenType.KEYWORD, "(");
            parseOp();
            match(Token.TokenType.KEYWORD, ")");
        } else {
            syntaxError("Expected an argument (atomic or operation)");
        }
    }

    private void parseBinOp() {
        Token op = currentToken();
        if (isBinOp(op)) {
            nextToken(); // Consume operator
            match(Token.TokenType.KEYWORD, "(");

            if (isAtomic(currentToken())) {
                parseAtomic(); // Left operand
            } else if (isBinOp(currentToken())) {
                System.out.println("Parsing NESTED binOp: " + currentToken().getValue());
                parseBinOp(); // Left operand
            } else if (isUnOp(currentToken())) {
                System.out.println("Parsing NESTED unOp: " + currentToken().getValue());
                parseUnOp(); // Left operand
            }

            match(Token.TokenType.KEYWORD, ",");

            if (isAtomic(currentToken())) {
                parseAtomic(); // Left operand
            } else if (isBinOp(currentToken())) {
                System.out.println("Parsing NESTED binOp: " + currentToken().getValue());
                parseBinOp(); // Left operand
            } else if (isUnOp(currentToken())) {
                System.out.println("Parsing NESTED unOp: " + currentToken().getValue());
                parseUnOp(); // Left operand
            }

            match(Token.TokenType.KEYWORD, ")");
        } else {
            syntaxError("Expected a binary operator");
        }
    }

    // Parse unary operation (UNOP)
    private void parseUnOp() {
        Token op = currentToken();
        if (isUnOp(op)) {
            nextToken(); // Consume operator
            match(Token.TokenType.KEYWORD, "(");
            parseSimple(); // Operand
            match(Token.TokenType.KEYWORD, ")");
        } else {
            syntaxError("Expected a unary operator");
        }
    }

    // Parse a branch (BRANCH ::= if COND then ALGO else ALGO)
    private void parseBranch() {
        match(Token.TokenType.KEYWORD, "if");
        parseCond(); // Parse the condition

        match(Token.TokenType.KEYWORD, "then");

        parseAlgo(); // Parse the 'then' part

        match(Token.TokenType.KEYWORD, "else");

        parseAlgo(); // Parse the 'else' part
    }

    // Parse condition (COND ::= SIMPLE | COMPOSIT)
    private void parseCond() {
        if (isBinOp(currentToken())) {
            if (isBinOp(lookahead(2))) { // Check if it's a composite condition: e.g or(eq(3,2), lt(3,2)) - second char
                                         // after the first binop is a binop, hence its composit
                parseComposit(); // Handling composite conditions
            } else {
                parseSimple();
            }
        } else if (currentToken().getValue().equals("not")) {
            // Handling unary operation for composite condition
            System.out.println("handling unop " + currentToken().getValue());
            // match(Token.TokenType.KEYWORD, "not");
            parseComposit(); // Expect a simple condition after 'not'
        } else if (currentToken().getValue().equals("sqrt")) {
            // Handling unary operation for composite condition
            parseComposit(); // Expect a simple condition after 'sqrt'
            // match(Token.TokenType.KEYWORD, "sqrt");
        } else {
            syntaxError("Expected a simple or composite condition");
        }
    }

    // Parse a simple condition (SIMPLE ::= BINOP( ATOMIC, ATOMIC ))
    private void parseSimple() {
        System.out.println("Parsing simple condition: " + currentToken().getValue());
        parseBinOp(); // Simple binary condition
    }

    // Parse a composite condition (COMPOSIT ::= BINOP(SIMPLE, SIMPLE) |
    // UNOP(SIMPLE))
    private void parseComposit() {
        System.out.println("Parsing composite condition: " + currentToken().getValue());
        Token op = currentToken();

        // Check for unary operation (e.g., "not")
        if (isUnOp(op)) {
            nextToken(); // Consume the unary operator (e.g., "not")
            match(Token.TokenType.KEYWORD, "("); // Expecting an opening parenthesis
            parseSimple(); // Parse the simple condition that follows the unary operator
            match(Token.TokenType.KEYWORD, ")"); // Expecting a closing parenthesis
        }
        // Check for binary operation
        else if (isBinOp(op)) {
            nextToken(); // Consume the binary operator
            match(Token.TokenType.KEYWORD, "("); // Expecting an opening parenthesis
            parseSimple(); // Left operand
            match(Token.TokenType.KEYWORD, ",");
            parseSimple(); // Right operand
            match(Token.TokenType.KEYWORD, ")"); // Expecting a closing parenthesis
        } else {
            syntaxError("Expected a unary or binary operator for composite condition");
        }
    }

    // Helper method to determine if the current token is a unary operator
    private boolean isUnOp(Token token) {
        return token.getValue().equals("not") || token.getValue().equals("sqrt");
    }

    // Parse a function call (CALL ::= FNAME( ATOMIC , ATOMIC , ATOMIC ))
    private void parseFunctionCall() {
        System.out.println("Parsing function call: " + currentToken().getValue());
        if (currentToken().getType() == Token.TokenType.FUNCTION_NAME) {
            if (Character.isDigit(currentToken().getValue().charAt(2))) {
                syntaxError("Function name cannot start with a digit");
            }
            // if the characters after the V_ is a keyword give an error
            String subString = currentToken().getValue().substring(2);
            if (reservedKeywords.contains(subString)) {
                syntaxError("Variable name cannot be a keyword");
            }
            nextToken(); // Consume function name
            match(Token.TokenType.KEYWORD, "(");
            parseAtomic(); // First argument
            match(Token.TokenType.KEYWORD, ",");
            parseAtomic(); // Second argument
            match(Token.TokenType.KEYWORD, ",");
            parseAtomic(); // Third argument
            match(Token.TokenType.KEYWORD, ")");
        } else {
            syntaxError("Expected function call");
        }
    }

    // Parse functions (FUNCTIONS ::= DECL FUNCTIONS | ε)
    private void parseFunctions() {
        System.out.println("Parsing function: " + currentToken().getType());
        if (currentToken().getType() == Token.TokenType.EOF) {
            System.out.println("No functions found");
            // return;
        } else if (currentToken().getValue().equals("num") || currentToken().getValue().equals("void")) {
            parseDecl(); // Parse a function declaration
            parseFunctions(); // Recursively handle more functions
        }
        // Functions can be nullable tho
    }

    // Parse a function declaration (DECL ::= HEADER BODY)
    private void parseDecl() {
        System.out.println("Parsing function declaration: " + currentToken().getValue());
        parseHeader(); // Parse function header
        parseBody(); // Parse function body
    }

    // Parse function header (HEADER ::= FTYP FNAME( VNAME , VNAME , VNAME ))
    private void parseHeader() {
        System.out.println("Parsing function header: " + currentToken().getValue());
        if (currentToken().getValue().equals("num") || currentToken().getValue().equals("void")) {
            nextToken(); // Consume return type ('num' or 'void')
            if (currentToken().getType() == Token.TokenType.FUNCTION_NAME) {
                System.out.println("Parsing function name: " + currentToken().getValue());
                if (Character.isDigit(currentToken().getValue().charAt(2))) {
                    syntaxError("Function name cannot start with a digit");
                }
                // if the characters after the V_ is a keyword give an error
                String subString = currentToken().getValue().substring(2);
                if (reservedKeywords.contains(subString)) {
                    syntaxError("Function name cannot be a keyword");
                }
                nextToken(); // Consume function name
                match(Token.TokenType.KEYWORD, "(");

                // Parse 1st argument
                if (currentToken().getValue().startsWith("V_")) {
                    System.out.println("Parsing variable name: " + currentToken().getValue().charAt(2));
                    if (Character.isDigit(currentToken().getValue().charAt(2))) {
                        syntaxError("Variable name cannot start with a digit");
                    }
                    // if the characters after the V_ is a keyword give an error
                    subString = currentToken().getValue().substring(2);
                    if (reservedKeywords.contains(subString)) {
                        syntaxError("Variable name cannot be a keyword");
                    }
                    nextToken(); // Consume variable name
                } else {
                    syntaxError("Expected variable name in function header");
                }

                match(Token.TokenType.KEYWORD, ",");

                // Parse 2nd argument
                if (currentToken().getValue().startsWith("V_")) {
                    System.out.println("Parsing variable name: " + currentToken().getValue());
                    if (Character.isDigit(currentToken().getValue().charAt(2))) {
                        syntaxError("Variable name cannot start with a digit");
                    }
                    // if the characters after the V_ is a keyword give an error
                    subString = currentToken().getValue().substring(2);
                    System.out.println("SUB" + subString);
                    if (reservedKeywords.contains(subString)) {
                        syntaxError("Variable name cannot be a keyword");
                    }
                    nextToken(); // Consume variable name
                } else {
                    syntaxError("Expected variable name in function header");
                }

                match(Token.TokenType.KEYWORD, ",");

                // Parse 3rd argument
                if (currentToken().getValue().startsWith("V_")) {
                    System.out.println("Parsing variable name: " + currentToken().getValue());
                    if (Character.isDigit(currentToken().getValue().charAt(2))) {
                        syntaxError("Variable name cannot start with a digit");
                    }
                    // if the characters after the V_ is a keyword give an error
                    subString = currentToken().getValue().substring(2);
                    if (reservedKeywords.contains(subString)) {
                        syntaxError("Variable name cannot be a keyword");
                    }
                    nextToken(); // Consume variable name
                } else {
                    syntaxError("Expected variable name in function header");
                }

                match(Token.TokenType.KEYWORD, ")");
                System.out.println("Done parsing function header: " + currentToken().getValue());
            } else {
                syntaxError("Expected function name in function header");
            }
        } else {
            syntaxError("Expected return type ('num' or 'void') in function header");
        }
    }

    // Parse function body (BODY ::= PROLOG LOCVARS ALGO EPILOG SUBFUNCS end)
    private void parseBody() {
        System.out.println("Parsing function body: " + currentToken().getValue());
        match(Token.TokenType.KEYWORD, "{"); // Prolog
        parseLocVars(); // Local variables
        parseAlgo(); // Function body (algo)
        match(Token.TokenType.KEYWORD, "}"); // Epilog
        // call subfunc if next token is num or void
        if (currentToken().getValue().equals("num") || currentToken().getValue().equals("void")) {
            parseSubFuncs();
        }

        System.out.println("Done parsing function body: " + currentToken().getValue());
    }

    // Parse local variables (LOCVARS ::= VTYP VNAME , VTYP VNAME , VTYP VNAME)
    private void parseLocVars() {
        parseVarDeclaration(true);
        match(Token.TokenType.KEYWORD, ",");
        parseVarDeclaration(true);
        match(Token.TokenType.KEYWORD, ",");
        parseVarDeclaration(true);
        match(Token.TokenType.KEYWORD, ",");
    }

    // Parse sub-functions (SUBFUNCS ::= FUNCTIONS)
    private void parseSubFuncs() {
        parseFunctions();
    }

    // Check if current token is a binary operator
    private boolean isBinOp(Token token) {
        return token.getValue().equals("add") || token.getValue().equals("sub") ||
                token.getValue().equals("mul") || token.getValue().equals("div") ||
                token.getValue().equals("and") || token.getValue().equals("or") ||
                token.getValue().equals("eq") || token.getValue().equals("grt");
    }

    private boolean isAtomic(Token token) {
        return token.getTokenClass().equals("V") || token.getTokenClass().equals("N")
                || token.getTokenClass().equals("T");
    }
}
