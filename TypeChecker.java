import java.util.List;
import java.util.ArrayList;

public class TypeChecker {
    private final SymbolTable symbolTable;
    private String currentFunctionScope = null;
    private int indentLevel = 0;
    private static final String INDENT = "  ";

    public TypeChecker(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    private void log(String message) {
        System.out.println(INDENT.repeat(indentLevel) + message);
    }

    private void enterScope(String scopeName) {
        log("Entering " + scopeName);
        indentLevel++;
    }

    private void exitScope(String scopeName, boolean result) {
        indentLevel--;
        log("Exiting " + scopeName + " -> " + result);
    }

    // Main entry point for type checking according to PROG rule
    public boolean typecheck(SynNode prog) {
        enterScope("PROG");
        
        // Add null check with detailed logging
        if (prog == null) {
            log("ERROR: Root node is null");
            symbolTable.exitScope();
            exitScope("PROG", false);
            return false;
        }
    
        // Enhanced root node logging
        log("Root node details:");
        log("Type: '" + (prog.getType() == null ? "null" : prog.getType()) + "'");
        log("Value: '" + (prog.getValue() == null ? "null" : prog.getValue()) + "'");
        log("Number of children: " + prog.getChildren().size());
        
        // Log children with more detail
        if (!prog.getChildren().isEmpty()) {
            log("Children details:");
            for (int i = 0; i < prog.getChildren().size(); i++) {
                SynNode child = prog.getChildren().get(i);
                log(String.format("Child %d: Type='%s', Value='%s'", 
                    i + 1,
                    child.getType() == null ? "null" : child.getType(),
                    child.getValue() == null ? "null" : child.getValue()));
            }
        }
    
        // Stricter type validation
        if (prog.getType() == null || prog.getType().trim().isEmpty()) {
            log("ERROR: Root node type is null or empty");
            exitScope("PROG", false);
            return false;
        }
    
        if (!prog.getType().equals("PROG")) {
            log(String.format("ERROR: Root node is not PROG (found '%s' instead)", prog.getType()));
            exitScope("PROG", false);
            return false;
        }
    
        // Continue with existing type checking
        boolean globVarsResult = typecheckGlobVars(findFirstChildOfType(prog, "GLOBVARS"));
        log("GLOBVARS check result: " + globVarsResult);
    
        boolean algoResult = typecheckAlgo(findFirstChildOfType(prog, "ALGO"));
        log("ALGO check result: " + algoResult);
    
        boolean functionsResult = typecheckFunctions(findFirstChildOfType(prog, "FUNCTIONS"));
        log("FUNCTIONS check result: " + functionsResult);
    
        boolean finalResult = globVarsResult && algoResult && functionsResult;
        symbolTable.exitScope();
        exitScope("PROG", finalResult);
        return finalResult;
    }

    // private void validateNodeProperties(SynNode node, String context) {
    //     if (node == null) {
    //         log(context + ": Node is null");
    //         return;
    //     }
        
    //     log(context + " properties:");
    //     log("- Type: '" + (node.getType() == null ? "null" : node.getType()) + "'");
    //     log("- Value: '" + (node.getValue() == null ? "null" : node.getValue()) + "'");
    //     log("- Children count: " + node.getChildren().size());
        
    //     if (node.getType() == null || node.getType().trim().isEmpty()) {
    //         log("WARNING: " + context + " has null or empty type");
    //     }
    // }

    public boolean typecheckHeader(SynNode header) {
        enterScope("HEADER");
        
        if (header == null) {
            log("ERROR: Header is null");
            exitScope("HEADER", false);
            return false;
        }
        
        // Debug the entire header structure
        log("Header node structure:");
        logNodeStructure(header, 1);
        
        SynNode ftypNode = findFirstChildOfType(header, "FTYP");
        SynNode fnameNode = findFirstChildOfType(header, "FNAME");
        
        if (ftypNode == null || fnameNode == null) {
            log("ERROR: Missing FTYP or FNAME node");
            log("FTYP: " + (ftypNode == null ? "null" : "present"));
            log("FNAME: " + (fnameNode == null ? "null" : "present"));
            exitScope("HEADER", false);
            return false;
        }
        
        String functionType = getFunctionType(ftypNode);
        if (functionType == null || functionType.equals("u")) {
            log("ERROR: Could not determine function type");
            log("FTYP node details:");
            logNodeStructure(ftypNode, 1);
            exitScope("HEADER", false);
            return false;
        }
        
        String functionName = fnameNode.getValue();
        if (functionName == null || functionName.trim().isEmpty()) {
            log("ERROR: Function name is null or empty");
            exitScope("HEADER", false);
            return false;
        }
    
        log("Function details - Type: " + functionType + ", Name: " + functionName);
        
        symbolTable.enterScope(functionName);
        String functionId = symbolTable.getIdentifier(functionName);
        symbolTable.linkType(functionId, functionType);
        
        // Process parameters
        List<SynNode> parameters = new ArrayList<>();
        for (SynNode child : header.getChildren()) {
            if ("VNAME".equals(child.getType())) {
                parameters.add(child);
            }
        }
        
        if (parameters.size() != 3) {
            log("ERROR: Expected 3 parameters, found " + parameters.size());
            symbolTable.exitScope();
            exitScope("HEADER", false);
            return false;
        }
        
        log("Processing parameters:");
        boolean paramsValid = true;
        for (SynNode param : parameters) {
            if (param == null || param.getValue() == null) {
                log("ERROR: Parameter or parameter value is null");
                paramsValid = false;
                continue;
            }
            
            String paramValue = param.getValue();
            log("Processing parameter: " + paramValue);
            
            String paramId = symbolTable.getIdentifier(paramValue);
            symbolTable.linkType(paramId, "n");  // All parameters are numeric
            
            String paramType = symbolTable.getType(paramValue);
            if (paramType == null || !paramType.equals("n")) {
                log("ERROR: Invalid type for parameter " + paramValue + ": " + paramType);
                paramsValid = false;
            } else {
                log("Successfully processed parameter " + paramValue + " with type " + paramType);
            }
        }
        
        if (!paramsValid) {
            symbolTable.exitScope();
            exitScope("HEADER", false);
            return false;
        }
        
        exitScope("HEADER", true);
        return true;
    }
    
    // New helper method to specifically handle function type determination
    private String getFunctionType(SynNode ftypNode) {
        log("Determining function type from FTYP node:");
        logNodeStructure(ftypNode, 1);
        
        // First try the direct value
        String directValue = ftypNode.getValue();
        if (isValidType(directValue)) {
            log("Found valid type in direct value: " + directValue);
            return convertToTypeCode(directValue);
        }
        
        // Check children
        for (SynNode child : ftypNode.getChildren()) {
            // Check the child's value
            String childValue = child.getValue();
            if (isValidType(childValue)) {
                log("Found valid type in child value: " + childValue);
                return convertToTypeCode(childValue);
            }
            
            // If child has children, check them too
            for (SynNode grandchild : child.getChildren()) {
                String grandchildValue = grandchild.getValue();
                if (isValidType(grandchildValue)) {
                    log("Found valid type in grandchild value: " + grandchildValue);
                    return convertToTypeCode(grandchildValue);
                }
            }
        }
        
        log("Could not determine valid type");
        return "u";
    }
    
    // Helper method to check if a value represents a valid type
    private boolean isValidType(String value) {
        if (value == null) return false;
        value = value.toLowerCase();
        return value.equals("num") || value.equals("number") || 
               value.equals("text") || value.equals("string") || 
               value.equals("void");
    }
    
    // Helper method to convert type strings to type codes
    private String convertToTypeCode(String type) {
        if (type == null) return "u";
        type = type.toLowerCase();
        switch (type) {
            case "num":
            case "number":
                return "n";
            case "text":
            case "string":
                return "t";
            case "void":
                return "v";
            default:
                return "u";
        }
    }
    
    // Helper method to log node structure
    private void logNodeStructure(SynNode node, int depth) {
        if (node == null) {
            log("null node");
            return;
        }
        
        String indent = "  ".repeat(depth);
        log(indent + "Node Type: '" + node.getType() + "'");
        log(indent + "Node Value: '" + node.getValue() + "'");
        
        if (!node.getChildren().isEmpty()) {
            log(indent + "Children:");
            for (SynNode child : node.getChildren()) {
                logNodeStructure(child, depth + 1);
            }
        }
    }

    // BODY type checking rules
    private boolean typecheckBody(SynNode body) {
        enterScope("BODY");
        
        if (body == null) {
            log("ERROR: Body is null");
            exitScope("BODY", false);
            return false;
        }
        
        SynNode prolog = findFirstChildOfType(body, "PROLOG");
        if (prolog == null || !prolog.getValue().equals("{")) {
            log("ERROR: Invalid or missing PROLOG");
            exitScope("BODY", false);
            return false;
        }
        
        boolean locvarsResult = typecheckLocVars(findFirstChildOfType(body, "LOCVARS"));
        log("LOCVARS check result: " + locvarsResult);
        if (!locvarsResult) {
            exitScope("BODY", false);
            return false;
        }
        
        boolean algoResult = typecheckAlgo(findFirstChildOfType(body, "ALGO"));
        log("ALGO check result: " + algoResult);
        if (!algoResult) {
            exitScope("BODY", false);
            return false;
        }
        
        SynNode epilog = findFirstChildOfType(body, "EPILOG");
        if (epilog == null || !epilog.getValue().equals("}")) {
            log("ERROR: Invalid or missing EPILOG");
            exitScope("BODY", false);
            return false;
        }
        
        boolean subfuncsResult = typecheckFunctions(findFirstChildOfType(body, "SUBFUNCS"));
        log("SUBFUNCS check result: " + subfuncsResult);
        
        exitScope("BODY", subfuncsResult);
        return subfuncsResult;
    }

    // LOCVARS type checking rules
    private boolean typecheckLocVars(SynNode locvars) {
        enterScope("LOCVARS");
        
        if (locvars == null) {
            log("No local variables declared");
            exitScope("LOCVARS", true);
            return true;
        }
        
        List<SynNode> types = new ArrayList<>();
        List<SynNode> names = new ArrayList<>();
        
        for (SynNode child : locvars.getChildren()) {
            if (child.getType().equals("VTYP")) types.add(child);
            else if (child.getType().equals("VNAME")) names.add(child);
        }
        
        if (types.size() != 3 || names.size() != 3) {
            log("ERROR: Expected 3 variable declarations, found types=" + types.size() + ", names=" + names.size());
            exitScope("LOCVARS", false);
            return false;
        }
        
        for (int i = 0; i < 3; i++) {
            String varType = typeof(types.get(i));
            String varName = names.get(i).getValue();
            String varId = symbolTable.getIdentifier(varName);
            
            log("Checking local variable: " + varName + " (type: " + varType + ")");
            
            symbolTable.linkType(varId, varType);
            
            if (!varType.equals("n") && !varType.equals("t")) {
                log("ERROR: Invalid type for " + varName + ": " + varType);
                exitScope("LOCVARS", false);
                return false;
            }
        }
        
        exitScope("LOCVARS", true);
        return true;
    }

    // Helper method for FTYP type determination
    

    private SynNode findFirstChildOfType(SynNode node, String type) {
        if (node == null || type == null) return null;
        for (SynNode child : node.getChildren()) {
            if (child != null && type.equals(child.getType())) {
                return child;
            }
            // Also check children with empty types but matching values
            if (child != null && child.getType().trim().isEmpty() && 
                type.equals(child.getValue())) {
                return child;
            }
        }
        return null;
    }

    // GLOBVARS type checking rules
    private boolean typecheckGlobVars(SynNode globVars) {
        enterScope("GLOBVARS");
        
        if (globVars == null) {
            log("No global variables declared");
            exitScope("GLOBVARS", true);
            return true;
        }
        
        log("Checking global variables:");
        log("Number of children: " + globVars.getChildren().size());
        
        for (SynNode decl : globVars.getChildren()) {
            log("Processing declaration: " + decl.getType());
            
            // Debug node structure
            log("Declaration children:");
            for (SynNode child : decl.getChildren()) {
                log("- Child type: " + child.getType() + ", value: " + child.getValue());
            }
            
            SynNode vtyp = findFirstChildOfType(decl, "VTYP");
            SynNode vname = findFirstChildOfType(decl, "VNAME");
            
            if (vtyp == null || vname == null) {
                log("ERROR: Invalid global variable declaration");
                log("VTYP: " + (vtyp == null ? "null" : "present"));
                log("VNAME: " + (vname == null ? "null" : "present"));
                exitScope("GLOBVARS", false);
                return false;
            }
            
            String type = typeof(vtyp);
            log("Variable type determined: " + type);
            log("Variable name: " + vname.getValue());
            
            if (!type.equals("n") && !type.equals("t")) {
                log("ERROR: Invalid type for global variable: " + vname.getValue() + " (type: " + type + ")");
                exitScope("GLOBVARS", false);
                return false;
            }
            
            String id = symbolTable.getIdentifier(vname.getValue());
            symbolTable.linkType(id, type);
            log("Successfully linked type " + type + " to variable " + vname.getValue());
        }
        
        exitScope("GLOBVARS", true);
        return true;
    }
    

    // Type determination helper following the specification
    private String typeof(SynNode node) {
        if (node == null) {
            log("WARNING: typeof called with null node");
            return "u";
        }
        
        String result = "u";
        log("Checking type of " + node.getType() + " node: " + node.getValue());
        
        if (!node.getChildren().isEmpty()) {
            log("- Children:");
            for (SynNode child : node.getChildren()) {
                log("  * " + child.getType() + ": " + child.getValue());
            }
        }

        log("- Type: " + node.getType());
    log("- Value: " + node.getValue());
        SynNode unop;
        switch (node.getType()) {
            case "SIMPLE":
                SynNode binop = findFirstChildOfType(node, "BINOP");
                if (binop == null) return "u";
                String atomic1Type = typeof(findFirstChildOfType(node, "ATOMIC1"));
                String atomic2Type = typeof(findFirstChildOfType(node, "ATOMIC2"));
                
                if (binop.getValue().equals("eq") || binop.getValue().equals("grt")) {
                    return (atomic1Type.equals("n") && atomic2Type.equals("n")) ? "b" : "u";
                }
                return (atomic1Type.equals("b") && atomic2Type.equals("b")) ? "b" : "u";
            
            case "COMPOSIT":
                binop = findFirstChildOfType(node, "BINOP");
                unop = findFirstChildOfType(node, "UNOP");
                
                if (binop != null) {
                    String simple1Type = typeof(findFirstChildOfType(node, "SIMPLE1"));
                    String simple2Type = typeof(findFirstChildOfType(node, "SIMPLE2"));
                    return (simple1Type.equals("b") && simple2Type.equals("b")) ? "b" : "u";
                }
                if (unop != null) {
                    String simpleType = typeof(findFirstChildOfType(node, "SIMPLE"));
                    return simpleType.equals("b") ? "b" : "u";
                }
                return "u";    
            case "VTYP":
                if (node.getValue().equals("num")) {
                    result = "n";
                    log("Found numeric type");
                }
                else if (node.getValue().equals("text")) {
                    result = "t";
                    log("Found text type");
                }
                else if (node.getValue().equals("void")) {
                    result = "v";
                    log("Found void type");
                }
                else {
                    log("WARNING: Unknown VTYP value: " + node.getValue());
                }
                break;
            case "ATOMIC":
                if (node.isConstant()) {
                    if (node.getValue().startsWith("\"")) result = "t";
                    else {
                        try {
                            Double.parseDouble(node.getValue());
                            result = "n";
                        } catch (NumberFormatException e) {
                            log("WARNING: Invalid numeric constant: " + node.getValue());
                        }
                    }
                } else {
                    result = symbolTable.getType(node.getValue());
                }
                break;
            case "CALL":
                result = typecheckCall(node) ? symbolTable.getType(findFirstChildOfType(node, "FNAME").getValue()) : "u";
                break;
            case "OP":
                result = getOperationType(node);
                break;
        }
        
        log("Type determined: " + result);
        return result;
    }

    // ALGO type checking rule
    private boolean typecheckAlgo(SynNode algo) {
        if (algo == null) return true;
        return typecheckInstruc(findFirstChildOfType(algo, "INSTRUC"));
    }

    // INSTRUC type checking rules
    private boolean typecheckInstruc(SynNode instruc) {
        if (instruc == null) return true; // Base case
        
        for (SynNode command : instruc.getChildren()) {
            if (command.getType().equals("COMMAND") && !typecheckCommand(command)) {
                return false;
            }
        }
        return true;
    }

    // COMMAND type checking rules
    private boolean typecheckCommand(SynNode command) {
        if (command == null) return true;
        
        String cmdType = command.getValue();
        if ("skip".equals(cmdType) || "halt".equals(cmdType)) {
            return true;
        }

        // Handle print command
        if (command.getType().equals("PRINT")) {
            String atomicType = typeof(findFirstChildOfType(command, "ATOMIC"));
            return atomicType.equals("n") || atomicType.equals("t");
        }

        // Handle return command
        if (command.getType().equals("RETURN")) {
            if (currentFunctionScope == null) return false;
            String atomicType = typeof(findFirstChildOfType(command, "ATOMIC"));
            String functionType = symbolTable.getType(currentFunctionScope);
            return atomicType.equals(functionType) && functionType.equals("n");
        }

        // Handle other command types
        SynNode child = command.getChildren().get(0);
        switch (child.getType()) {
            case "ASSIGN": return typecheckAssign(child);
            case "CALL": return typeof(child).equals("v");
            case "BRANCH": return typecheckBranch(child);
            default: return false;
        }
    }

    // ASSIGN type checking rules
    private boolean typecheckAssign(SynNode assign) {
        enterScope("ASSIGN");
        
        SynNode vname = findFirstChildOfType(assign, "VNAME");
        String vnameType = symbolTable.getType(vname.getValue());
        log("Assignment target: " + vname.getValue() + " (type: " + vnameType + ")");

        if (assign.hasInput()) {
            boolean result = vnameType.equals("n");
            log("Input assignment - target must be numeric: " + result);
            exitScope("ASSIGN", result);
            return result;
        }

        String termType = typeof(findFirstChildOfType(assign, "TERM"));
        log("Assignment source type: " + termType);
        boolean result = vnameType.equals(termType);
        log("Type match: " + result);
        
        exitScope("ASSIGN", result);
        return result;
    }

    // CALL type checking
    private boolean typecheckCall(SynNode call) {
        List<SynNode> atomics = new ArrayList<>();
        
        // Get all ATOMIC nodes
        for (SynNode child : call.getChildren()) {
            if (child.getType().equals("ATOMIC")) {
                atomics.add(child);
            }
        }
        
        // Must have exactly 3 parameters
        if (atomics.size() != 3) {
            log("ERROR: Function call must have exactly 3 parameters, found " + atomics.size());
            return false;
        }
        
        // All parameters must be numeric
        for (int i = 0; i < atomics.size(); i++) {
            String paramType = typeof(atomics.get(i));
            if (!paramType.equals("n")) {
                log("ERROR: Parameter " + (i+1) + " must be numeric, found type " + paramType);
                return false;
            }
        }
        
        return true;
    }

    // BRANCH type checking rules
    private boolean typecheckBranch(SynNode branch) {
        enterScope("BRANCH");
        
        SynNode cond = findFirstChildOfType(branch, "COND");
        if (cond == null) {
            log("ERROR: Missing condition in branch");
            exitScope("BRANCH", false);
            return false;
        }
        
        String condType = typeof(cond);
        if (!condType.equals("b")) {
            log("ERROR: Branch condition must be boolean, found " + condType);
            exitScope("BRANCH", false);
            return false;
        }
        
        SynNode thenAlgo = findFirstChildOfType(branch, "ALGO");
        SynNode elseAlgo = findFirstChildOfType(branch.getChildren().get(1), "ALGO");
        
        if (thenAlgo == null || elseAlgo == null) {
            log("ERROR: Missing then/else block in branch");
            exitScope("BRANCH", false);
            return false;
        }
        
        boolean result = typecheckAlgo(thenAlgo) && typecheckAlgo(elseAlgo);
        exitScope("BRANCH", result);
        return result;
    }

    // FUNCTIONS type checking rules
    private boolean typecheckFunctions(SynNode functions) {
        if (functions == null) return true; // Base case
        
        for (SynNode decl : functions.getChildren()) {
            if (!typecheckDecl(decl)) return false;
        }
        return true;
    }

    // DECL type checking rules
    private boolean typecheckDecl(SynNode decl) {
        SynNode header = findFirstChildOfType(decl, "HEADER");
        SynNode body = findFirstChildOfType(decl, "BODY");
        
        String oldScope = currentFunctionScope;
        currentFunctionScope = findFirstChildOfType(header, "FNAME").getValue();
        
        boolean headerResult = typecheckHeader(header);
        boolean bodyResult = false;
        
        if (headerResult) {
            bodyResult = typecheckBody(body);
        }
        
        // Exit function scope
        symbolTable.exitScope();
        
        currentFunctionScope = oldScope;
        return headerResult && bodyResult;
    }

    // Helper method for operation type checking
    private String getOperationType(SynNode op) {
        SynNode unop = findFirstChildOfType(op, "UNOP");
        if (unop != null) {
            String argType = typeof(findFirstChildOfType(op, "ARG"));
            if (unop.getValue().equals("not")) return argType.equals("b") ? "b" : "u";
            if (unop.getValue().equals("sqrt")) return argType.equals("n") ? "n" : "u";
            return "u";
        }

        SynNode binop = findFirstChildOfType(op, "BINOP");
        if (binop != null) {
            String arg1Type = typeof(findFirstChildOfType(op, "ARG1"));
            String arg2Type = typeof(findFirstChildOfType(op, "ARG2"));
            String opType = binop.getValue();
            
            if (opType.equals("or") || opType.equals("and")) {
                return (arg1Type.equals("b") && arg2Type.equals("b")) ? "b" : "u";
            }
            if (opType.equals("eq") || opType.equals("grt")) {
                return (arg1Type.equals("n") && arg2Type.equals("n")) ? "b" : "u";
            }
            if (opType.equals("add") || opType.equals("sub") || 
                opType.equals("mul") || opType.equals("div")) {
                return (arg1Type.equals("n") && arg2Type.equals("n")) ? "n" : "u";
            }
        }
        return "u";
    }

    // private boolean verifyReturnStatements(SynNode body, String functionType) {
    //     if (functionType.equals("v")) return true;  // Void functions don't need return
        
    //     // Find ALGO node in body
    //     SynNode algo = findFirstChildOfType(body, "ALGO");
    //     if (algo == null) return false;
        
    //     // Check if any instruction contains a return
    //     boolean hasReturn = false;
    //     for (SynNode instruc : algo.getChildren()) {
    //         hasReturn |= hasReturnStatement(instruc);
    //     }
        
    //     return hasReturn;
    // }
    
    // private boolean hasReturnStatement(SynNode node) {
    //     if (node == null) return false;
    //     if (node.getType().equals("RETURN")) return true;
        
    //     for (SynNode child : node.getChildren()) {
    //         if (hasReturnStatement(child)) return true;
    //     }
    //     return false;
    // }
}