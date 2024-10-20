import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;

public class SymbolTable {
    private final Stack<Map<String, String>> scopes = new Stack<>();
    private final Map<String, String> globalSymbols = new HashMap<>();
    private final Map<String, String> functionSymbols = new HashMap<>();
    private int uniqueCounter = 1; // Counter to generate unique names

    public SymbolTable() {
        enterScope(); // Create global scope
    }

    public void enterScope() {
        scopes.push(new HashMap<>());
    }

    public void exitScope() {
        if (!scopes.isEmpty()) {
            scopes.pop();
        }
    }

    public String generateUniqueName(String originalName) {
        return originalName + "_" + UUID.randomUUID().toString().substring(0, 4);
    }

    // Add a symbol to the symbol table
    public void addSymbol(String name, String uniqueName, String type, String currentScope) {
        if (currentScope == null || currentScope.isEmpty()) {
            if (globalSymbols.containsKey(name)) {
                throw new SemanticError("Symbol '" + name + "' is already declared in the global scope.");
            }
            globalSymbols.put(uniqueName, type);
        } else {
            Map<String, String> currentScopeMap = scopes.peek();
            if (currentScopeMap.containsKey(name)) {
                throw new SemanticError("Symbol '" + name + "' is already declared in this scope.");
            }
            currentScopeMap.put(uniqueName, type);
        }
    }

    // Lookup a symbol in the symbol table
    public String lookupTable(String name) {
        // Check in the local scopes first
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Map<String, String> currentScope = scopes.get(i);
            if (currentScope.containsKey(name)) {
                return currentScope.get(name);
            }
        }
        // Check in the global symbols
        return globalSymbols.get(name);
    }

    public void declareVariable(String name, String type) {
        String uniqueName = generateUniqueName(name);
        addSymbol(name, uniqueName, type, null);
        System.out.println("Declared variable: " + name + " with type: " + type);
    }

    public void declareFunction(String name, String type) {
        System.out.println("Declaring function: " + name + " with type: " + type);
        if (functionSymbols.containsKey(name)) {
            throw new SemanticError("Function '" + name + "' is already declared.");
        }
        String uniqueName = generateUniqueName(name);
        functionSymbols.put(uniqueName, type);
        addSymbol(name, uniqueName, type, null); // Add to global symbols
    }

    public boolean isDeclared(String name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name)) {
                return true;
            }
        }
        return globalSymbols.containsKey(name);
    }

    public boolean isFunction(String name) {
        return functionSymbols.containsKey(name);
    }

    public String getType(String name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name)) {
                return scopes.get(i).get(name);
            }
        }
        return globalSymbols.get(name);
    }

    // Return a string representation of the symbol table
    public String viewSymbolTable() {
        StringBuilder sb = new StringBuilder();
        int level = 0;

        for (Map<String, String> scope : scopes) {
            sb.append("Scope Level ").append(level++).append(":\n");
            for (Map.Entry<String, String> entry : scope.entrySet()) {
                sb.append("    ").append(entry.getKey()).append(" : ").append(entry.getValue()).append("\n");
            }
        }
        return sb.toString();
    }
}

// Custom exception class for semantic errors
class SemanticError extends RuntimeException {
    public SemanticError(String message) {
        super(message);
    }
}
