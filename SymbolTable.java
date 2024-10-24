import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;
import java.util.*;

class SymbolTable {
    private Map<String, String> symbolMap = new HashMap<>(); // Global symbol table for variables/functions
    private Stack<Map<String, String>> scopeStack = new Stack<>(); // Stack to manage scopes
    private Set<String> functionNames = new HashSet<>(); // Set of function names to check for variable conflicts
    Set<String> reservedKeywords = Set.of("if", "else", "while", "return", "num", "string", "call", "main"); // Reserved
    private final List<Map<String, SymbolInfo>> scopes = new ArrayList<>();
    private final List<String> scopeNames = new ArrayList<>(); // To track the name of each scope
                                                                                                         // keywords
    private int variableCounter = 0; // Counter for generating unique variable names
    private int functionCounter = 0; // Counter for generating unique function names
    private int scopeCounter = 0; // Counter for generating unique scope names

    // Start a new scope
    public void enterScope() {
        String defaultScopeName = "scope" + (scopeCounter + 1);
        enterScope(defaultScopeName);
    }
    
    // Main implementation that takes a scope name
    public void enterScope(String scopeName) {
        // Initialize new scope maps
        Map<String, SymbolInfo> newScope = new HashMap<>();
        Map<String, String> newScopeStack = new HashMap<>();
        
        // Add the new scope to both tracking structures
        scopes.add(newScope);
        scopeStack.push(newScopeStack);
        
        // Track scope name
        scopeNames.add(scopeName);
        
        // Increment and log scope counter for debugging
        System.out.println("Entering scope " + scopeName + " (scope #" + ++scopeCounter + ")");
    }

    // Exit the current scope
    public void exitScope() {
        if (!scopeStack.isEmpty()) {
            scopeStack.pop();
        }
        System.out.println("Exiting scope " + scopeCounter);
    }

    private void throwError(String message) throws SemanticErrorException {
        throw new SemanticErrorException(message);
    }

    public String getType(String value) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            SymbolInfo symbol = scopes.get(i).get(value);
            if (symbol != null) {
                return symbol.getType();  // Assuming SymbolInfo has a getType method

            }
        }
        return null;  // If symbol is not found, return null or throw an exception
    }

    // Declare a variable in the current scope
    public String declareVariable(String name) {
        String nameSuffix = getVariableName(name);
        if (reservedKeywords.contains(nameSuffix)) {
            throwError("Semantic Error: '" + name + "' is a reserved keyword.");
        }
        if (functionNames.contains(name)) {
            throwError("Semantic Error: Variable name '" + name + "' conflicts with an existing function name.");
        }
        if (scopeStack.isEmpty()) {
            throwError("No active scope to declare a variable.");
        }

        Map<String, String> currentScope = scopeStack.peek();
        if (currentScope.containsKey(name)) {
            throwError("Semantic Error: Variable '" + name + "' is already declared in this scope.");
        }

        // Check for double declaration in the current scope (e.g., string X and number
        // X)
        if (symbolMap.containsKey(name)) {
            throwError(
                    "Semantic Error: Variable '" + name + "' is already declared as another type in the same scope.");
        }

        String internalName = "v" + (++variableCounter); // Generate a unique internal name
        currentScope.put(name, internalName);
        symbolMap.put(name, internalName); // Global map for lookup
        return internalName;
    }

    // Declare a function in the current scope
    public String declareFunction(String name, String returnType) {
        System.out.println("Function name: " + getVariableName(name));
        String nameSuffix = getVariableName(name);
        if (reservedKeywords.contains(nameSuffix)) {
            throwError("Semantic Error: '" + name + "' is a reserved keyword.");
        }
        System.out.println("Symbol map: " + symbolMap);
        if (symbolMap.containsKey(name)) {
            throwError("Semantic Error: Function name '" + name + "' conflicts with an existing variable or function name.");
        }

        if (functionNames.contains(name)) {
            throwError("Semantic Error: Function '" + name + "' is already declared.");
        }

        String internalName = "f" + (++functionCounter); // Generate a unique internal name
        functionNames.add(name); // Store function name
        symbolMap.put(name, internalName); // Global map for lookup
        return internalName;
    }

    // Check if a variable is declared in the current scope or any outer scope
    public boolean isVariableDeclared(String name) {
        for (int i = scopeStack.size() - 1; i >= 0; i--) {
            // Log the entire scope for clarity
            System.out.println("Checking scope " + i + ": " + scopeStack.get(i));

            // Check if the variable exists in this scope
            if (scopeStack.get(i).containsKey(name)) {
                System.out.println("Found variable '" + name + "' in scope " + i);
                return true;
            }
        }

        // System.out.println("Variable '" + name + "' not found in any scope");
        return false;
    }
    
    // Helper method to check if a function is declared in the current scope
    public boolean isFunctionDeclared(String name) {
        return functionNames.contains(name);
    }

    // Helper method to check if a function is declared in the immediate child scope
    public boolean isFunctionDeclaredInChildScope(String name) {
        // Assuming you maintain a map of child scopes for each scope, otherwise adjust
        // this logic
        if (!scopeStack.isEmpty()) {
            Map<String, String> currentScope = scopeStack.peek();
            // Check for function in child scopes (you may need to manage child scopes
            // separately)
            for (Map.Entry<String, String> entry : currentScope.entrySet()) {
                if (entry.getKey().equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Get the internal name of a declared variable
    public String getInternalName(String name) {
        return symbolMap.get(name);
    }

    // Validate scope name uniqueness (not same as parent or sibling)
    public void checkScopeName(String scopeName, Set<String> siblingScopes) {
        if (siblingScopes.contains(scopeName)) {
            throwError("Semantic Error: Scope '" + scopeName + "' conflicts with a sibling scope.");
        }
        if (scopeStack.size() > 1) {
            Map<String, String> parentScope = scopeStack.get(scopeStack.size() - 2);
            if (parentScope.containsKey(scopeName)) {
                throwError("Semantic Error: Scope '" + scopeName + "' conflicts with its parent scope.");
            }
        }
    }

    private String getVariableName(String value) {
        return value.substring(2);
    }

    public String lookup(String variableName) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            SymbolInfo symbol = scopes.get(i).get(variableName);
            if (symbol != null) {
                return symbol.getName();  // Assuming SymbolInfo has a getName method
            }
        }
        return null;  // If variable is not found, return null or throw an exception
    }

    public boolean isFunction(String name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            SymbolInfo symbol = scopes.get(i).get(name);
            if (symbol != null && symbol.getKind().equals("function")) {
                return true;
            }
        }
        return false;
    }

    // Declare a function with its type in the current scope
    public void declareFunction(String name, String type, String currentScope) {
        if (!scopes.isEmpty()) {
            scopes.get(scopes.size() - 1).put(name, new SymbolInfo(name, type, currentScope, "function"));
        }
    }

    public void declareVariable(String name, String type, String currentScope) {
        if (!scopes.isEmpty()) {
            scopes.get(scopes.size() - 1).put(name, new SymbolInfo(name, type, currentScope, "variable"));
        }
    }

    public void linkReference(String valueOf, String uniqueName) {
        // Assuming that linking a reference means updating the name or type of a symbol
        for (int i = scopes.size() - 1; i >= 0; i--) {
            SymbolInfo symbol = scopes.get(i).get(valueOf);
            if (symbol != null) {
                symbol.setName(uniqueName);  // Assuming SymbolInfo has a setName method
                return;


            }
        }
    }

    public Map<String, String> getSymbolMapping() {
        return new HashMap<>(symbolMap);
    }

    // Get or create an identifier for a value
    public String getIdentifier(String value) {
        if (value == null) {
            System.out.println("WARNING: Attempted to get identifier for null value");
            return null;
        }
        
        // First check if it's a direct mapping in the symbol table
        if (symbolMap.containsKey(value)) {
            return symbolMap.get(value);
        }
        
        // If not found and the value starts with a prefix (like "v" or "f")
        // return the value as is since it might already be an internal name
        if ((value.startsWith("v") || value.startsWith("f")) && value.length() > 1) {
            try {
                Integer.parseInt(value.substring(1));
                return value;
            } catch (NumberFormatException e) {
                // Not a valid internal name format
            }
        }
        
        // If no existing identifier found, create a new one
        String newId = "v" + (++variableCounter);
        symbolMap.put(value, newId);
        return newId;
    }
    
    // Link a type to a specific identifier in the symbol table
    public void linkType(String id, String type) {
        if (id == null) {
            System.out.println("WARNING: Attempted to link type '" + type + "' to null identifier");
            return;
        }
        
        if (type == null) {
            System.out.println("WARNING: Attempted to link null type to identifier '" + id + "'");
            return;
        }
        
        // Ensure we have at least one scope
        if (scopes.isEmpty()) {
            enterScope("global");  // Create a default scope if none exists
        }
        
        // Search through all scopes from inner to outer
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Map<String, SymbolInfo> currentScope = scopes.get(i);
            SymbolInfo symbol = currentScope.get(id);
            if (symbol != null) {
                symbol.setType(type);
                return;
            }
        }
        
        // If we haven't found the symbol in any scope, create it in the current scope
        String currentScope = scopeNames.get(scopeNames.size() - 1);
        String kind = (id != null && id.startsWith("f")) ? "function" : "variable";
        scopes.get(scopes.size() - 1).put(id, new SymbolInfo(id, type, currentScope, kind));
    }
}
