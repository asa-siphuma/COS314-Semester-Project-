import java.io.IOException;
import java.util.List;

public class SyntaxTreeXMLWriter {

    private List<Token> tokens;
    private int currentTokenIndex = 0;

    public SyntaxTreeXMLWriter(List<Token> tokens) throws IOException {
        this.tokens = tokens;
    }

    private Token currentToken() {
        if (currentTokenIndex < tokens.size()) {
            return tokens.get(currentTokenIndex);
        }
        return new Token(Token.TokenType.EOF, "", -1, -1);
    }

    // Get the n-th token after the current
    private Token lookahead(int n) {
        return tokens.get(currentTokenIndex + n);
    }

    private void nextToken() {
        currentTokenIndex++;
    }

    private void syntaxError(String message) {
        Token current = currentToken();
        throw new ParserException("Syntax error at line " + current.getLineNumber() + ", column "
                + current.getColumnNumber() + ": " + message);
    }

    // Modified match method to return a terminal node for the matched symbol
    private SynNode match(Token.TokenType expectedType, String expectedValue) {
        Token current = currentToken();
        if (current.getType() == expectedType && current.getValue().equals(expectedValue)) {
            SynNode terminalNode = new SynNode(current.getValue()); // Create terminal node
            nextToken();
            return terminalNode; // Return the terminal node
        } else {
            syntaxError("Expected '" + expectedValue + "', but found '" + currentToken().getValue() + "'");
            return null;
        }
    }

    // Entry point to parse the program (PROG ::= main GLOBVARS ALGO FUNCTIONS)
    public SynNode parseProgram() {
        SynNode root = new SynNode("Program");
        root.addChild(match(Token.TokenType.KEYWORD, "main")); // Must start with 'main');
        root.addChild(parseGlobVars());
        root.addChild(parseAlgo());
        root.addChild(parseFunctions());

        return root;
    }

    // Parse global variables (GLOBVARS ::= VTYP VNAME , GLOBVARS | ε)
    private SynNode parseGlobVars() {
        SynNode globVarsNode = new SynNode("GLOBVARS");

        if (currentToken().getValue().equals("num") || currentToken().getValue().equals("text")) {
            globVarsNode.addChild(parseVarDeclaration(false));
        }

        return globVarsNode;
    }

    // Parse a variable declaration (VTYP VNAME)
    private SynNode parseVarDeclaration(boolean isInFunctionParameters) {
        SynNode varDeclNode = new SynNode("VARDECL");

        Token varType = currentToken();
        if (varType.getValue().equals("num") || varType.getValue().equals("text")) {
            SynNode varTypeNode = new SynNode("VTYPE");
            varTypeNode.addChild(new SynNode(varType.getValue()));
            varDeclNode.addChild(varTypeNode);
            nextToken();

            Token varName = currentToken();
            if (varName.getType() == Token.TokenType.VARIABLE_NAME) {
                SynNode varNameNode = new SynNode("VNAME");
                varNameNode.addChild(new SynNode(varName.getValue()));
                varDeclNode.addChild(varNameNode);
                nextToken();
            } else {
                syntaxError("Expected variable name after type");
            }

            if (!isInFunctionParameters && currentToken().getValue().equals(",")) {
                varDeclNode.addChild(new SynNode(","));
                nextToken();
                varDeclNode.addChild(parseVarDeclaration(false));
            }

        } else if (currentToken().getValue().equals("begin")) {
            varDeclNode.addChild(new SynNode("begin"));
            // return varDeclNode;
        } else {
            syntaxError("Expected 'num' or 'text' for variable type");
        }

        return varDeclNode;
    }

    // Parse algorithm block (ALGO ::= begin INSTRUC end)
    private SynNode parseAlgo() {

        SynNode algoNode = new SynNode("ALGO");
        algoNode.addChild(match(Token.TokenType.KEYWORD, "begin"));
        algoNode.addChild(parseInstruc());
        algoNode.addChild(match(Token.TokenType.KEYWORD, "end"));
        return algoNode;
    }

    // Parse instructions (INSTRUC ::= COMMAND ; INSTRUC | ε)
    private SynNode parseInstruc() {
        System.out.println("Parsing instruction: " + currentToken().getValue());
        SynNode instrucNode = new SynNode("INSTRUC");

        if (currentToken().getValue().equals("end")) {
            instrucNode.addChild(new SynNode("epsilon"));
            return instrucNode;
        }

        instrucNode.addChild(parseCommand());
        System.out.println("INSTRUC: " + instrucNode.toString());
        instrucNode.addChild(match(Token.TokenType.KEYWORD, ";"));
        instrucNode.addChild(parseInstruc());

        return instrucNode;
    }

    // Parse a command (COMMAND ::= skip | halt | print ATOMIC | ASSIGN | CALL |
    // BRANCH)
    private SynNode parseCommand() {
        Token command = currentToken();
        SynNode commandNode = new SynNode("COMMAND");
        System.out.println("Parsing command: " + command.getValue());
        if (command.getValue().startsWith("F_")) {
            commandNode.addChild(parseFunctionCall());
            return commandNode;
        }

        switch (command.getValue()) {
            case "skip":
            case "halt":
                commandNode.addChild(new SynNode(command.getValue()));
                nextToken();
                break;
            case "print":
                commandNode.addChild(new SynNode(command.getValue()));
                nextToken();
                commandNode.addChild(parseAtomic());
                break;
            case "if":
                commandNode.addChild(parseBranch());
                break;
            default:
                commandNode.addChild(parseAssign());
                break;
        }

        return commandNode;
    }

    // Parse an assignment (ASSIGN ::= VNAME = TERM)
    private SynNode parseAssign() {
        SynNode assignNode = new SynNode("ASSIGN");

        Token varName = currentToken();
        SynNode nameNode = (new SynNode("VNAME"));
        nameNode.addChild(match(Token.TokenType.VARIABLE_NAME, varName.getValue()));
        assignNode.addChild(nameNode);
        System.out.println("Variable name: " + varName.getValue());

        assignNode.addChild(match(Token.TokenType.KEYWORD, "="));

        assignNode.addChild(parseTerm());
        return assignNode;
    }

    // Parse a term (TERM ::= ATOMIC | CALL | OP)
    private SynNode parseTerm() {
        System.out.println("Parsing term: " + currentToken().getValue());
        System.out.println(currentToken().getType());
        SynNode termNode = new SynNode("TERM");

        if (currentToken().getType() == Token.TokenType.VARIABLE_NAME
                || currentToken().getType() == Token.TokenType.NUMBER
                || currentToken().getType() == Token.TokenType.TEXT_CONSTANT) {
            termNode.addChild(parseAtomic());
        } else if (currentToken().getType() == Token.TokenType.FUNCTION_NAME) {
            System.out.println("Parsing function call: " + currentToken().getValue());
            termNode.addChild(parseFunctionCall());
        } else if (isBinOp(currentToken()) || isUnOp(currentToken())) {
            System.out.println("Parsing binary operation: " + currentToken().getValue());
            termNode.addChild(parseOp());
        } else {
            syntaxError("Invalid term");
        }

        return termNode;
    }

    // Parse atomic values (ATOMIC ::= VNAME | CONST)
    private SynNode parseAtomic() {
        Token atomic = currentToken();
        nextToken();
        SynNode atomicNode = new SynNode("ATOMIC");
        atomicNode.addChild(new SynNode(atomic.getValue()));
        return atomicNode;
    }

    // Parse unary or binary operations
    private SynNode parseOp() {
        SynNode opNode = new SynNode("OP");
        Token op = currentToken();
        System.out.println("Parsing operator: " + op.getValue());

        if (isUnOp(op)) {
            SynNode unaryNode = new SynNode("UNOP");
            unaryNode.addChild(new SynNode(op.getValue()));
            opNode.addChild(unaryNode);
            // Unary operation
            nextToken(); // Consume the unary operator
            opNode.addChild(match(Token.TokenType.KEYWORD, "(")); // Expect an opening parenthesis

            opNode.addChild(parseArg()); // Parse the argument for the unary operation

            opNode.addChild(match(Token.TokenType.KEYWORD, ")")); // Expect closing parenthesis
        } else if (isBinOp(op)) {
            // Binary operation
            opNode.addChild(parseOpBinop()); // Parse binary operation
        } else {
            syntaxError("Expected an operator");
        }

        return opNode;
    }

    // Parse binary operation with two arguments
    private SynNode parseOpBinop() {
        SynNode opNode = new SynNode("BINOP");
        Token op = currentToken();
        System.out.println("Parsing binary operator: " + op.getValue());

        opNode.addChild(new SynNode(op.getValue()));
        nextToken(); // Move past the binary operator
        opNode.addChild(match(Token.TokenType.KEYWORD, "(")); // Expect opening parenthesis

        // Parse first argument
        opNode.addChild(parseArg());

        opNode.addChild(match(Token.TokenType.KEYWORD, ",")); // Expect a comma

        // Parse second argument
        opNode.addChild(parseArg());

        opNode.addChild(match(Token.TokenType.KEYWORD, ")")); // Expect closing parenthesis

        // if (lookahead(1).getValue().equals("(")) {
        // System.out.println("oo");
        // nextToken();
        // }

        System.out.println("DONE parsing binary operation: " + currentToken().getValue());
        return opNode;
    }

    // Parse an argument, which could be an atomic value or another operation
    private SynNode parseArg() {
        SynNode argNode = new SynNode("ARG");
        System.out.println("Parsing argument: " + currentToken().getValue());

        if (isAtomic(currentToken())) {
            System.out.println("Parsing atomic arg: " + currentToken().getValue());
            argNode.addChild(parseAtomic()); // Argument is atomic
        } else if (isBinOp(currentToken()) || isUnOp(currentToken())) {
            // nextToken();
            argNode.addChild(parseOp()); // Argument is a nested operation
        } else if (currentToken().getValue().equals("(")) { // Argument is a nested operation
            System.out.println("Parsing NESTED arg: " + currentToken().getValue());
            argNode.addChild(match(Token.TokenType.KEYWORD, "("));
            argNode.addChild(parseOp());
            argNode.addChild(match(Token.TokenType.KEYWORD, ")"));
        } else {
            syntaxError("Expected an argument (atomic or operation)");
        }

        return argNode;
    }

    // Parse binary operation (BINOP)
    private SynNode parseBinOp() {
        SynNode binOpNode = new SynNode("BINOP");
        Token op = currentToken();
        if (isBinOp(op)) {
            binOpNode.addChild(new SynNode(op.getValue()));
            nextToken(); // Consume operator
            binOpNode.addChild(match(Token.TokenType.KEYWORD, "("));

            if (isAtomic(currentToken())) {
                binOpNode.addChild(parseAtomic()); // Left operand
            } else if (isBinOp(currentToken())) {
                System.out.println("Parsing NESTED binOp: " + currentToken().getValue());
                binOpNode.addChild(parseBinOp()); // Left operand
            } else if (isUnOp(currentToken())) {
                System.out.println("Parsing NESTED unOp: " + currentToken().getValue());
                binOpNode.addChild(parseUnOp()); // Left operand
            }

            binOpNode.addChild(match(Token.TokenType.KEYWORD, ","));

            if (isAtomic(currentToken())) {
                binOpNode.addChild(parseAtomic()); // Left operand
            } else if (isBinOp(currentToken())) {
                System.out.println("Parsing NESTED binOp: " + currentToken().getValue());
                binOpNode.addChild(parseBinOp()); // Left operand
            } else if (isUnOp(currentToken())) {
                System.out.println("Parsing NESTED unOp: " + currentToken().getValue());
                binOpNode.addChild(parseUnOp()); // Left operand
            }

            binOpNode.addChild(match(Token.TokenType.KEYWORD, ")"));
        } else {
            syntaxError("Expected a binary operator");
        }

        return binOpNode;
    }

    // Parse unary operation (UNOP)
    private SynNode parseUnOp() {
        SynNode unOpNode = new SynNode("UNOP");
        Token op = currentToken();
        if (isUnOp(op)) {
            unOpNode.addChild(new SynNode(op.getValue()));
            nextToken(); // Consume operator
            unOpNode.addChild(match(Token.TokenType.KEYWORD, "("));
            unOpNode.addChild(parseSimple()); // Operand
            unOpNode.addChild(match(Token.TokenType.KEYWORD, ")"));
        } else {
            syntaxError("Expected a unary operator");
        }

        return unOpNode;
    }

    // Parse a branch (BRANCH ::= if COND then ALGO else ALGO)
    private SynNode parseBranch() {
        SynNode branchNode = new SynNode("BRANCH");

        branchNode.addChild(match(Token.TokenType.KEYWORD, "if"));
        branchNode.addChild(parseCond());

        branchNode.addChild(match(Token.TokenType.KEYWORD, "then"));
        branchNode.addChild(parseAlgo());

        branchNode.addChild(match(Token.TokenType.KEYWORD, "else"));
        branchNode.addChild(parseAlgo());

        return branchNode;
    }

    // Parse conditions (COND ::= ATOMIC OP ATOMIC)
    private SynNode parseCond() {
        SynNode condNode = new SynNode("COND");

        if (isBinOp(currentToken())) {
            if (isBinOp(lookahead(2))) { // Check if it's a composite condition: e.g or(eq(3,2), lt(3,2)) - second char
                condNode.addChild(parseComposit()); // Handling composite conditions
            } else {
                condNode.addChild(parseSimple());
            }
        } else if (currentToken().getValue().equals("not")) {
            condNode.addChild(new SynNode("not"));
            System.out.println("handling unop " + currentToken().getValue());
            condNode.addChild(parseComposit()); // Expect a simple condition after 'not'
        } else if (currentToken().getValue().equals("sqrt")) {
            condNode.addChild(new SynNode("sqrt"));
            condNode.addChild(parseComposit()); // Expect a simple condition after 'sqrt'
        } else {
            syntaxError("Expected a simple or composite condition");
        }

        return condNode;
    }

    // Parse a simple condition (SIMPLE ::= BINOP( ATOMIC, ATOMIC ))
    private SynNode parseSimple() {

        SynNode simpleNode = new SynNode("SIMPLE");
        System.out.println("Parsing simple condition: " + currentToken().getValue());
        simpleNode.addChild(parseBinOp()); // Simple binary condition
        return simpleNode;
    }

    private SynNode parseComposit() {
        SynNode compositNode = new SynNode("COMPOSIT");
        System.out.println("Parsing composite condition: " + currentToken().getValue());
        Token op = currentToken();

        // Check for unary operation (e.g., "not")
        if (isUnOp(op)) {
            compositNode.addChild(new SynNode(op.getValue()));
            nextToken(); // Consume the unary operator (e.g., "not")
            compositNode.addChild(match(Token.TokenType.KEYWORD, "(")); // Expecting an opening parenthesis
            compositNode.addChild(parseSimple()); // Parse the simple condition that follows the unary operator
            compositNode.addChild(match(Token.TokenType.KEYWORD, ")")); // Expecting a closing parenthesis
        }
        // Check for binary operation
        else if (isBinOp(op)) {
            compositNode.addChild(new SynNode(op.getValue()));
            nextToken(); // Consume the binary operator
            compositNode.addChild(match(Token.TokenType.KEYWORD, "(")); // Expecting an opening parenthesis
            compositNode.addChild(parseSimple()); // Left operand
            compositNode.addChild(match(Token.TokenType.KEYWORD, ","));
            compositNode.addChild(parseSimple()); // Right operand
            compositNode.addChild(match(Token.TokenType.KEYWORD, ")")); // Expecting a closing parenthesis
        } else {
            syntaxError("Expected a unary or binary operator for composite condition");
        }

        return compositNode;
    }

    // Parse function calls (CALL ::= FNAME(ARGS))
    private SynNode parseFunctionCall() {
        System.out.println("Parsing function call: " + currentToken().getValue());
        SynNode functionCallNode = new SynNode("CALL");

        if (currentToken().getType() == Token.TokenType.FUNCTION_NAME) {
            functionCallNode.addChild(new SynNode(currentToken().getValue()));
            nextToken(); // Consume function name
            functionCallNode.addChild(match(Token.TokenType.KEYWORD, "("));
            functionCallNode.addChild(parseAtomic()); // First argument
            functionCallNode.addChild(match(Token.TokenType.KEYWORD, ","));
            functionCallNode.addChild(parseAtomic()); // Second argument
            functionCallNode.addChild(match(Token.TokenType.KEYWORD, ","));
            functionCallNode.addChild(parseAtomic()); // Third argument
            functionCallNode.addChild(match(Token.TokenType.KEYWORD, ")"));
        } else {
            syntaxError("Expected function call");
        }
        return functionCallNode;
    }

    // Parse functions (FUNCTIONS ::= DECL FUNCTIONS | ε)
    private SynNode parseFunctions() {
        SynNode functionsNode = new SynNode("FUNCTIONS");
        if (currentToken().getType() == Token.TokenType.KEYWORD) {
            functionsNode.addChild(new SynNode(currentToken().getValue()));
            functionsNode.addChild(parseDecl()); // Parse a function declaration
            functionsNode.addChild(parseFunctions()); // Recursively handle more functions
        } else {
            functionsNode.addChild(new SynNode("epsilon"));
        }

        return functionsNode;
    }

    // Parse a function declaration (DECL ::= HEADER BODY)
    private SynNode parseDecl() {
        SynNode declNode = new SynNode("DECL");
        System.out.println("Parsing function declaration: " + currentToken().getValue());
        declNode.addChild(parseHeader()); // Parse function header
        declNode.addChild(parseBody()); // Parse function body

        return declNode;
    }

    // Parse function header (HEADER ::= FTYP FNAME( VNAME , VNAME , VNAME ))
    private SynNode parseHeader() {
        System.out.println("Parsing function header: " + currentToken().getValue());
        SynNode headerNode = new SynNode("HEADER");
        if (currentToken().getValue().equals("num") || currentToken().getValue().equals("void")) {
            SynNode ftypNode = new SynNode("FTYP");
            ftypNode.addChild(new SynNode(currentToken().getValue()));
            headerNode.addChild(ftypNode);
            nextToken(); // Consume return type ('num' or 'void')
            if (currentToken().getType() == Token.TokenType.FUNCTION_NAME) {
                System.out.println("Parsing function name: " + currentToken().getValue());
                SynNode fnameNode = new SynNode("FNAME");
                fnameNode.addChild(new SynNode(currentToken().getValue()));
                headerNode.addChild(fnameNode);
                nextToken(); // Consume function name
                headerNode.addChild(match(Token.TokenType.KEYWORD, "("));

                // Parse 1st argument
                if (currentToken().getValue().startsWith("V_")) {
                    SynNode vnameNode = new SynNode("VNAME");
                    headerNode.addChild(vnameNode);
                    vnameNode.addChild(new SynNode(currentToken().getValue()));
                    nextToken(); // Consume variable name
                } else {
                    syntaxError("Expected variable name in function header");
                }

                headerNode.addChild(match(Token.TokenType.KEYWORD, ","));

                // Parse 2nd argument
                if (currentToken().getValue().startsWith("V_")) {
                    SynNode vnameNode = new SynNode("VNAME");
                    headerNode.addChild(vnameNode);
                    vnameNode.addChild(new SynNode(currentToken().getValue()));
                    System.out.println("Parsing variable name: " + currentToken().getValue());
                    nextToken(); // Consume variable name
                } else {
                    syntaxError("Expected variable name in function header");
                }

                headerNode.addChild(match(Token.TokenType.KEYWORD, ","));

                // Parse 3rd argument
                if (currentToken().getValue().startsWith("V_")) {
                    SynNode vnameNode = new SynNode("VNAME");
                    headerNode.addChild(vnameNode);
                    vnameNode.addChild(new SynNode(currentToken().getValue()));
        
                    System.out.println("Parsing variable name: " + currentToken().getValue());
                    nextToken(); // Consume variable name
                } else {
                    syntaxError("Expected variable name in function header");
                }

                headerNode.addChild(match(Token.TokenType.KEYWORD, ")"));
            } else {
                syntaxError("Expected function name in function header");
            }
        } else {
            syntaxError("Expected return type ('num' or 'void') in function header");
        }

        return headerNode;
    }

    // Parse function body (BODY ::= PROLOG LOCVARS ALGO EPILOG SUBFUNCS end)
    private SynNode parseBody() {
        System.out.println("Parsing function body: " + currentToken().getValue());
        SynNode bodyNode = new SynNode("BODY");

        SynNode prologNode = new SynNode("PROLOG");
        prologNode.addChild(match(Token.TokenType.KEYWORD, "{")); // Prolog

        bodyNode.addChild(prologNode);
        bodyNode.addChild(parseLocVars()); // Local variables
        bodyNode.addChild(parseAlgo()); // Function body (algo)

        SynNode epilogNode = new SynNode("EPILOG");
        epilogNode.addChild(match(Token.TokenType.KEYWORD, "}")); // Epilog
        bodyNode.addChild(epilogNode);

        // call subfunc if next token is num or void
        if (currentToken().getValue().equals("num") || currentToken().getValue().equals("void")) {
            bodyNode.addChild(match(Token.TokenType.KEYWORD, currentToken().getValue()));
            bodyNode.addChild(parseSubFuncs());
        }
        bodyNode.addChild(match(Token.TokenType.KEYWORD, "end"));

        return bodyNode;
    }

    // Parse local variables (LOCVARS ::= VTYP VNAME , VTYP VNAME , VTYP VNAME)
    private SynNode parseLocVars() {
        SynNode locVarsNode = new SynNode("LOCVARS");
        locVarsNode.addChild(parseVarDeclaration(true));
        locVarsNode.addChild(match(Token.TokenType.KEYWORD, ","));
        locVarsNode.addChild(parseVarDeclaration(true));
        locVarsNode.addChild(match(Token.TokenType.KEYWORD, ","));
        locVarsNode.addChild(parseVarDeclaration(true));
        locVarsNode.addChild(match(Token.TokenType.KEYWORD, ","));

        return locVarsNode;
    }

    // Parse sub-functions (SUBFUNCS ::= FUNCTIONS)
    private SynNode parseSubFuncs() {
        System.out.println("Parsing sub-functions: " + currentToken().getValue());
        SynNode subFuncsNode = new SynNode("SUBFUNCS");
        subFuncsNode.addChild(parseFunctions());
        return subFuncsNode;
    }

    // Helper to check if the token is a binary operator
    private boolean isBinOp(Token token) {
        return token.getValue().equals("add") || token.getValue().equals("sub") ||
                token.getValue().equals("mul") || token.getValue().equals("div") ||
                token.getValue().equals("and") || token.getValue().equals("or") ||
                token.getValue().equals("eq") || token.getValue().equals("grt");
    }

    // Helper to check if the token is a unary operator
    private boolean isUnOp(Token token) {
        return token.getValue().equals("not") || token.getValue().equals("sqrt");
    }

    private boolean isAtomic(Token token) {
        return token.getTokenClass().equals("V") || token.getTokenClass().equals("N")
                || token.getTokenClass().equals("T");
    }
}
