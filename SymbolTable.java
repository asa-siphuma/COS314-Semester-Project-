import java.util.*;

public class SymbolTable {
    private final List<Map<String, SymbolInfo>> scopes = new ArrayList<>();
    private final List<String> scopeNames = new ArrayList<>(); // To track the name of each scope

    public SymbolTable() {
        enterScope("global");  // Create global scope at initialization
    }

    // Enter a new scope with a given name
    public void enterScope(String scopeName) {
        scopes.add(new HashMap<>());  // Create a new scope (e.g., function or block)
        scopeNames.add(scopeName);    // Track the name of the scope
    }

    // Exit the current scope
    public void exitScope() {
        if (!scopes.isEmpty()) {
            scopes.remove(scopes.size() - 1);  // Remove the last scope
            scopeNames.remove(scopeNames.size() - 1);  // Remove the last scope name
        }
    }

    // Check if a variable or function is declared in the current or any higher scope
    public boolean isDeclared(String name, String currentScope) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name)) {
                return true;
            }
        }
        return false;
    }

    // Declare a variable with its type in the current scope
    public void declareVariable(String name, String type, String currentScope) {
        if (!scopes.isEmpty()) {
            scopes.get(scopes.size() - 1).put(name, new SymbolInfo(name, type, currentScope, "variable"));
        }
    }

    // Declare a function with its type in the current scope
    public void declareFunction(String name, String type, String currentScope) {
        if (!scopes.isEmpty()) {
            scopes.get(scopes.size() - 1).put(name, new SymbolInfo(name, type, currentScope, "function"));
        }
    }

    // Check if a symbol is a function
    public boolean isFunction(String name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            SymbolInfo symbol = scopes.get(i).get(name);
            if (symbol != null && symbol.getKind().equals("function")) {
                return true;
            }
        }
        return false;
    }

    // Return a string representation of the symbol table with scopes
    public String viewSymbolTable() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < scopes.size(); i++) {
            sb.append("Scope ").append(i).append(" (").append(scopeNames.get(i)).append("): ").append(scopes.get(i)).append("\n");
        }
        return sb.toString();
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

    public String lookup(String variableName) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            SymbolInfo symbol = scopes.get(i).get(variableName);
            if (symbol != null) {
                return symbol.getName();  // Assuming SymbolInfo has a getName method
            }
        }
        return null;  // If variable is not found, return null or throw an exception
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
}
