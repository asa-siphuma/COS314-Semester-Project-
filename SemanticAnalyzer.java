import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SemanticAnalyzer {
    private final SymbolTable symbolTable = new SymbolTable();
    private final List<String> errorReports = new ArrayList<>();
    private final Map<String, Map<String, String>> scopeNameMap = new HashMap<>();
    private int nextVarId = 0;
    private int nextFuncId = 0;
    private String currentScope = "global";

    public void analyze(SynNode root) {
        crawlTree(root);
        
        // Print results
        System.out.println("Symbol Table after analysis:");
        System.out.println(symbolTable.viewSymbolTable());
        
        System.out.println("Error reports:");
        errorReports.forEach(System.out::println);
    }

    private void crawlTree(SynNode node) {
        if (node == null) return;
    
        // Process all children first
        for (SynNode child : node.getChildren()) {
            crawlTree(child);
        }
    
        // Process current node
        processNode(node);
    
        // Handle scope exit if needed
        if (node.isFunctionDefinition()) {
            symbolTable.exitScope();
            currentScope = "global";  // Return to global scope
        }
    }

    private void processNode(SynNode node) {
        // Handle function definitions
        if (node.isFunctionDefinition()) {
            handleFunctionDefinition(node);
        }
        // Handle variable declarations
        else if (node.isVariableDeclaration()) {
            handleVariableDeclaration(node);
        }
        // Handle variable usage
        else if (node.isVariableUsage()) {
            handleVariableUsage(node);
        }
        // Handle function calls
        else if (node.isFunctionCall()) {
            handleFunctionCall(node);
        }
    }

    private void handleFunctionDefinition(SynNode node) {
        String functionName = node.getValue();
        
        // Validate function name format
        if (!functionName.matches("F_[a-z]([a-z]|[0-9])*")) {
            errorReports.add("Invalid function name format: " + functionName);
            return;
        }

        String uniqueName = getOrCreateUniqueName("global", functionName, true);
        
        if (symbolTable.isDeclared(uniqueName, "global")) {
            errorReports.add("Function '" + functionName + "' is already declared.");
            return;
        }

        // Register function in symbol table with node ID as reference
        symbolTable.declareFunction(String.valueOf(node.id), uniqueName, node.getType());
        
        // Create new scope for function body
        currentScope = uniqueName;
        symbolTable.enterScope(currentScope);

        // Count parameters and local variables in children
        validateFunctionStructure(node);
    }

    private void validateFunctionStructure(SynNode funcNode) {
        int paramCount = 0;
        int localVarCount = 0;
    
        // Traverse children to find parameters and local variables
        for (SynNode child : funcNode.getChildren()) {
            if (child.isParameterSection()) { // Check for the HEADER node
                for (SynNode param : child.getChildren()) {
                    if (param.isVariableDeclaration()) {
                        paramCount++;
                    }
                }
            } else if (child.isVariableDeclaration()) {
                localVarCount++;
            }
        }
    
        // Validate counts
        if (paramCount != 3) {
            errorReports.add("Function must have exactly 3 parameters, found: " + paramCount);
        }
        if (localVarCount != 3) {
            errorReports.add("Function must have exactly 3 local variables, found: " + localVarCount);
        }
    }

    private boolean isParameter(SynNode node) {
        // Implement logic to determine if a variable declaration is a parameter
        // This might depend on your specific AST structure
        return node.getParent() != null && isParameterSection(node.getParent());
    }

    private boolean isParameterSection(SynNode node) {
        // Implement logic to determine if this is the parameter section of a function
        // This might depend on your specific AST structure
        return node.getValue() != null && node.getValue().equals("HEADER");
    }

    private void handleVariableDeclaration(SynNode node) {
        String varName = node.getValue();
        
        // Validate variable name format
        if (!varName.matches("V_[a-z]([a-z]|[0-9])*")) {
            errorReports.add("Invalid variable name format: " + varName);
            return;
        }

        String uniqueName = getOrCreateUniqueName(currentScope, varName, false);
        
        if (symbolTable.isDeclared(uniqueName, currentScope)) {
            errorReports.add("Variable '" + varName + "' is already declared in current scope.");
            return;
        }

        // Register variable in symbol table with node ID as reference
        symbolTable.declareVariable(String.valueOf(node.id), uniqueName, node.getType());
    }

    private void handleVariableUsage(SynNode node) {
        String varName = node.getValue();
        String uniqueName = lookupUniqueName(currentScope, varName);
        
        if (uniqueName == null) {
            errorReports.add("Variable '" + varName + "' used without declaration in scope " + currentScope);
            return;
        }

        // Link usage to declaration through node ID
        symbolTable.linkReference(String.valueOf(node.id), uniqueName);
    }

    private void handleFunctionCall(SynNode node) {
        String funcName = node.getValue();
        String uniqueName = lookupUniqueName("global", funcName);
        
        if (uniqueName == null) {
            errorReports.add("Function '" + funcName + "' called without declaration");
            return;
        }

        // Validate argument count (should be exactly 3 in RecSPL)
        int argCount = countArguments(node);
        if (argCount != 3) {
            errorReports.add("Function call to '" + funcName + "' must have exactly 3 arguments, found: " + argCount);
        }

        // Link call to declaration through node ID
        symbolTable.linkReference(String.valueOf(node.id), uniqueName);
    }

    private int countArguments(SynNode callNode) {
        // Count the number of ATOMIC nodes under this function call
        return (int) callNode.getChildren().stream()
            .filter(child -> child.getValue() != null)  // Assuming ATOMIC nodes have values
            .count();
    }

    private String getOrCreateUniqueName(String scope, String originalName, boolean isFunction) {
        scopeNameMap.putIfAbsent(scope, new HashMap<>());
        Map<String, String> nameMap = scopeNameMap.get(scope);
        
        return nameMap.computeIfAbsent(originalName, k -> {
            if (isFunction) {
                return "f" + (nextFuncId++);
            } else {
                return "v" + (nextVarId++);
            }
        });
    }

    private String lookupUniqueName(String scope, String originalName) {
        // Look in current scope
        if (scopeNameMap.containsKey(scope)) {
            String uniqueName = scopeNameMap.get(scope).get(originalName);
            if (uniqueName != null) return uniqueName;
        }
        
        // Look in global scope if not in current scope and not already in global
        if (!scope.equals("global")) {
            return scopeNameMap.get("global").get(originalName);
        }
        
        return null;
    }

    public List<String> getErrorReports() {
        return errorReports;
    }

    public Object getSymbolTable() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSymbolTable'");
    }
}