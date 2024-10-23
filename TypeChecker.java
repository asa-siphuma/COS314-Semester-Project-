import java.util.List;
import java.util.ArrayList;

public class TypeChecker {
    private final SymbolTable symbolTable;
    private String currentFunctionScope = "global";

    public TypeChecker(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    // Main entry point for type checking
    public boolean typecheck(SynNode prog) {
        return typecheckGlobVars(findFirstChildOfType(prog, "GLOBVARS")) &&
               typecheckAlgo(findFirstChildOfType(prog, "ALGO")) &&
               typecheckFunctions(findFirstChildOfType(prog, "FUNCTIONS"));
    }

    // Helper method to find first child node of a specific type
    private SynNode findFirstChildOfType(SynNode node, String type) {
        for (SynNode child : node.getChildren()) {
            if (child.getType().equals(type)) {
                return child;
            }
        }
        return null;
    }

    // Type check global variable declarations
    private boolean typecheckGlobVars(SynNode globVars) {
        if (globVars == null || globVars.getChildren().isEmpty()) {
            return true;
        }

        for (SynNode child : globVars.getChildren()) {
            if (child.isVariableDeclaration()) {
                String varType = getTypeFromVTYP(findFirstChildOfType(child, "VTYP"));
                String varName = findFirstChildOfType(child, "VNAME").getValue();
                
                symbolTable.declareVariable(varName, varType, currentFunctionScope);
                if (!varType.equals("n") && !varType.equals("t")) {
                    return false;
                }
            }
        }
        return true;
    }

    // Get type from VTYP node
    private String getTypeFromVTYP(SynNode vtypNode) {
        if (vtypNode == null) return "u";
        String value = vtypNode.getValue();
        switch (value) {
            case "num": return "n";
            case "text": return "t";
            case "void": return "v";
            default: return "u";
        }
    }

    // Type check algorithms (statements)
    private boolean typecheckAlgo(SynNode algo) {
        if (algo == null) return true;
        return typecheckInstruc(findFirstChildOfType(algo, "INSTRUC"));
    }

    // Type check instructions
    private boolean typecheckInstruc(SynNode instruc) {
        if (instruc == null || instruc.getChildren().isEmpty()) {
            return true;
        }

        boolean result = true;
        for (SynNode child : instruc.getChildren()) {
            if (child.getType().equals("COMMAND")) {
                result &= typecheckCommand(child);
            }
        }
        return result;
    }

    // Type check individual commands
    private boolean typecheckCommand(SynNode command) {
        if (command == null) return true;

        // Handle simple commands
        if (command.getValue() != null) {
            switch (command.getValue()) {
                case "skip":
                case "halt":
                    return true;
            }
        }

        // Handle complex commands
        for (SynNode child : command.getChildren()) {
            switch (child.getType()) {
                case "PRINT":
                    return typecheckPrint(child);
                case "ASSIGN":
                    return typecheckAssign(child);
                case "CALL":
                    return typecheckCall(child);
                case "BRANCH":
                    return typecheckBranch(child);
                case "RETURN":
                    return typecheckReturn(child);
            }
        }
        return false;
    }

    // Type check print statements
    private boolean typecheckPrint(SynNode print) {
        SynNode atomic = findFirstChildOfType(print, "ATOMIC");
        String atomicType = getAtomicType(atomic);
        return atomicType.equals("n") || atomicType.equals("t");
    }

    // Type check assignments
    private boolean typecheckAssign(SynNode assign) {
        SynNode vname = findFirstChildOfType(assign, "VNAME");
        String vnameType = symbolTable.getType(vname.getValue());

        // Check for input assignment
        if (assign.getValue() != null && assign.getValue().contains("input")) {
            return vnameType.equals("n"); // Only numeric input allowed
        }

        // Check for regular assignment
        SynNode term = findFirstChildOfType(assign, "TERM");
        String termType = getTermType(term);
        return vnameType.equals(termType);
    }

    public void checkAssignment(String variableName, String assignedType) {
        String varType = symbolTable.lookup(variableName);
        if (varType == null) {
            System.out.println("Error: Variable " + variableName + " not declared.");
        } else if (!varType.equals(assignedType)) {
            System.out.println("Error: Type mismatch in assignment to " + variableName + ": expected " + varType + " but got " + assignedType);
        } else {
            System.out.println("Assignment to " + variableName + " is valid.");
        }
    }

    // Get type of a term
    private String getTermType(SynNode term) {
        if (term == null) return "u";

        if (term.isVariableUsage()) {
            return symbolTable.getType(term.getValue());
        }

        if (term.isFunctionCall()) {
            return typecheckCall(term) ? symbolTable.getType(term.getValue()) : "u";
        }

        // Handle operations
        SynNode op = findFirstChildOfType(term, "OP");
        if (op != null) {
            return getOperationType(op);
        }

        return "u";
    }

    // Type check function calls
    private boolean typecheckCall(SynNode call) {
        if (!call.isFunctionCall()) return false;

        String funcName = findFirstChildOfType(call, "FNAME").getValue();
        List<SynNode> params = new ArrayList<>();
        for (SynNode child : call.getChildren()) {
            if (child.getType().equals("ATOMIC")) {
                params.add(child);
            }
        }

        // Check if all parameters are numeric
        for (SynNode param : params) {
            if (!getAtomicType(param).equals("n")) {
                return false;
            }
        }

        return symbolTable.isFunction(funcName);
    }

    // Get type of an atomic value
    private String getAtomicType(SynNode atomic) {
        if (atomic.isVariableUsage()) {
            return symbolTable.getType(atomic.getValue());
        }
        // Handle constants
        if (atomic.getValue().startsWith("\"")) {
            return "t";
        }
        try {
            Double.parseDouble(atomic.getValue());
            return "n";
        } catch (NumberFormatException e) {
            return "u";
        }
    }

    // Type check branches (if statements)
    private boolean typecheckBranch(SynNode branch) {
        SynNode cond = findFirstChildOfType(branch, "COND");
        SynNode algo1 = findFirstChildOfType(branch, "ALGO1");
        SynNode algo2 = findFirstChildOfType(branch, "ALGO2");

        return getConditionType(cond).equals("b") &&
               typecheckAlgo(algo1) &&
               typecheckAlgo(algo2);
    }

    // Get type of a condition
    private String getConditionType(SynNode cond) {
        if (cond == null) return "u";

        SynNode simple = findFirstChildOfType(cond, "SIMPLE");
        if (simple != null) {
            return getSimpleConditionType(simple);
        }

        SynNode composit = findFirstChildOfType(cond, "COMPOSIT");
        if (composit != null) {
            return getCompositConditionType(composit);
        }

        return "u";
    }

    // Get type of a simple condition
    private String getSimpleConditionType(SynNode simple) {
        SynNode binop = findFirstChildOfType(simple, "BINOP");
        if (binop == null) return "u";

        String opType = binop.getValue();
        List<SynNode> atomics = new ArrayList<>();
        for (SynNode child : simple.getChildren()) {
            if (child.getType().equals("ATOMIC")) {
                atomics.add(child);
            }
        }

        if (atomics.size() != 2) return "u";

        String type1 = getAtomicType(atomics.get(0));
        String type2 = getAtomicType(atomics.get(1));

        if (opType.equals("eq") || opType.equals("grt")) {
            return (type1.equals("n") && type2.equals("n")) ? "b" : "u";
        }

        return (type1.equals("b") && type2.equals("b")) ? "b" : "u";
    }

    // Get type of a composite condition
    private String getCompositConditionType(SynNode composit) {
        SynNode unop = findFirstChildOfType(composit, "UNOP");
        if (unop != null) {
            SynNode simple = findFirstChildOfType(composit, "SIMPLE");
            return getSimpleConditionType(simple).equals("b") ? "b" : "u";
        }

        SynNode binop = findFirstChildOfType(composit, "BINOP");
        if (binop != null) {
            List<SynNode> simples = new ArrayList<>();
            for (SynNode child : composit.getChildren()) {
                if (child.getType().equals("SIMPLE")) {
                    simples.add(child);
                }
            }

            if (simples.size() != 2) return "u";

            return (getSimpleConditionType(simples.get(0)).equals("b") &&
                    getSimpleConditionType(simples.get(1)).equals("b")) ? "b" : "u";
        }

        return "u";
    }

    // Get type of an operation
    private String getOperationType(SynNode op) {
        SynNode unop = findFirstChildOfType(op, "UNOP");
        if (unop != null) {
            String opType = unop.getValue();
            SynNode arg = findFirstChildOfType(op, "ARG");
            String argType = getArgType(arg);

            if (opType.equals("not")) {
                return argType.equals("b") ? "b" : "u";
            }
            if (opType.equals("sqrt")) {
                return argType.equals("n") ? "n" : "u";
            }
        }

        SynNode binop = findFirstChildOfType(op, "BINOP");
        if (binop != null) {
            String opType = binop.getValue();
            List<SynNode> args = new ArrayList<>();
            for (SynNode child : op.getChildren()) {
                if (child.getType().equals("ARG")) {
                    args.add(child);
                }
            }

            if (args.size() != 2) return "u";

            String type1 = getArgType(args.get(0));
            String type2 = getArgType(args.get(1));

            switch (opType) {
                case "add":
                case "sub":
                case "mul":
                case "div":
                    return (type1.equals("n") && type2.equals("n")) ? "n" : "u";
                case "and":
                case "or":
                    return (type1.equals("b") && type2.equals("b")) ? "b" : "u";
                case "eq":
                case "grt":
                    return (type1.equals("n") && type2.equals("n")) ? "b" : "u";
            }
        }

        return "u";
    }

    // Get type of an argument
    private String getArgType(SynNode arg) {
        if (arg == null) return "u";

        SynNode atomic = findFirstChildOfType(arg, "ATOMIC");
        if (atomic != null) {
            return getAtomicType(atomic);
        }

        SynNode op = findFirstChildOfType(arg, "OP");
        if (op != null) {
            return getOperationType(op);
        }

        return "u";
    }

    // Type check return statements
    private boolean typecheckReturn(SynNode returnNode) {
        SynNode atomic = findFirstChildOfType(returnNode, "ATOMIC");
        String atomicType = getAtomicType(atomic);
        String functionType = symbolTable.getType(currentFunctionScope);
        return atomicType.equals(functionType);
    }

    // Type check functions
    private boolean typecheckFunctions(SynNode functions) {
        if (functions == null || functions.getChildren().isEmpty()) {
            return true;
        }

        boolean result = true;
        for (SynNode child : functions.getChildren()) {
            if (child.isFunctionDefinition()) {
                result &= typecheckFunction(child);
            }
        }
        return result;
    }

    // Type check individual function
    private boolean typecheckFunction(SynNode function) {
        if (!function.isFunctionDefinition()) return false;

        SynNode header = findFirstChildOfType(function, "HEADER");
        SynNode body = findFirstChildOfType(function, "BODY");

        String prevScope = currentFunctionScope;
        currentFunctionScope = findFirstChildOfType(header, "FNAME").getValue();

        boolean result = typecheckHeader(header) && typecheckBody(body);

        currentFunctionScope = prevScope;
        return result;
    }

    // Type check function header
    private boolean typecheckHeader(SynNode header) {
        SynNode ftyp = findFirstChildOfType(header, "FTYP");
        String funcType = getTypeFromVTYP(ftyp);
        
        String funcName = findFirstChildOfType(header, "FNAME").getValue();
        symbolTable.declareFunction(funcName, funcType, currentFunctionScope);

        // Check parameters are all numeric
        List<SynNode> params = new ArrayList<>();
        for (SynNode child : header.getChildren()) {
            if (child.getType().equals("VNAME")) {
                params.add(child);
                symbolTable.declareVariable(child.getValue(), "n", currentFunctionScope);
            }
        }

        return params.size() == 3; // RecSPL requires exactly 3 parameters
    }

    // Type check function body
    private boolean typecheckBody(SynNode body) {
        return typecheckLocVars(findFirstChildOfType(body, "LOCVARS")) &&
               typecheckAlgo(findFirstChildOfType(body, "ALGO")) &&
               typecheckFunctions(findFirstChildOfType(body, "SUBFUNCS"));
    }

    // Type check local variables
    private boolean typecheckLocVars(SynNode locVars) {
        if (locVars == null) return true;

        List<SynNode> types = new ArrayList<>();
        List<SynNode> names = new ArrayList<>();

        for (SynNode child : locVars.getChildren()) {
            if (child.getType().equals("VTYP")) {
                types.add(child);
            } else if (child.getType().equals("VNAME")) {
                names.add(child);
            }
        }

        if (types.size() != 3 || names.size() != 3) return false;

        for (int i = 0; i < 3; i++) {
            String varType = getTypeFromVTYP(types.get(i));
            String varName = names.get(i).getValue();
            symbolTable.declareVariable(varName, varType, currentFunctionScope);
        }

        return true;
    }
}