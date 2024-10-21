import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SemanticAnalyzer {
    private final SymbolTable symbolTable = new SymbolTable();
    private final List<String> errorReports = new ArrayList<>();
    private final Random random = new Random(); // For generating unique identifiers

    public void analyze(SynNode root) {
        crawlSyntaxTree(root, "global"); // Start in the global scope
        System.out.println("Symbol Table after analysis:");
        System.out.println(symbolTable.viewSymbolTable());

        System.out.println("Error reports:");
        errorReports.forEach(System.out::println);
    }

    private void crawlSyntaxTree(SynNode node, String currentScope) {
        if (node.isFunctionDefinition()) {
            // Handle function declaration
            String originalFunctionName = node.getValue(); // Get the original function name
            String uniqueFunctionName = generateUniqueName("F_"); // Generate unique function name

            if (symbolTable.isDeclared(uniqueFunctionName, currentScope)) {
                errorReports.add("Function '" + uniqueFunctionName + "' is already declared in the current scope.");
            } else {
                System.out.println("Original function name: " + originalFunctionName + " -> Unique function name: " + uniqueFunctionName + " with type: " + node.getType());
                symbolTable.declareFunction(uniqueFunctionName, node.getType(), currentScope);
                currentScope = uniqueFunctionName; // Set the new scope for the function
                symbolTable.enterScope(currentScope); // Enter function scope
            }
        } else if (node.isVariableDeclaration()) {
            // Handle variable declaration
            String originalVariableName = node.getValue(); // Get the original variable name
            String uniqueVariableName = generateUniqueName("V_"); // Generate unique variable name

            if (symbolTable.isFunction(originalVariableName)) {
                errorReports.add("Variable name '" + originalVariableName + "' cannot be the same as function name.");
            } else if (symbolTable.isDeclared(uniqueVariableName, currentScope)) {
                errorReports.add("Variable '" + uniqueVariableName + "' is already declared in the current scope.");
            } else {
                System.out.println("Original variable name: " + originalVariableName + " -> Unique variable name: " + uniqueVariableName + " with type: " + node.getType());
                symbolTable.declareVariable(uniqueVariableName, node.getType(), currentScope);
            }
        } else if (node.isVariableUsage()) {
            // Handle variable usage
            String variableName = node.getValue();
            if (!symbolTable.isDeclared(variableName, currentScope)) {
                errorReports.add("Variable '" + variableName + "' is used without declaration.");
            }
        } else if (node.isFunctionCall()) {
            // Handle function call
            String functionName = node.getValue();
            if (!symbolTable.isDeclared(functionName, currentScope)) {
                errorReports.add("Function '" + functionName + "' is called without declaration.");
            }
        }

        // Recursively visit child nodes
        for (SynNode child : node.getChildren()) {
            crawlSyntaxTree(child, currentScope);
        }

        // Exit the scope when leaving a function
        if (node.isFunctionDefinition()) {
            symbolTable.exitScope();
        }
    }

    private String generateUniqueName(String prefix) {
        int uniqueId = random.nextInt(1000); // Generate a random number; adjust as needed
        return prefix + uniqueId;
    }

    public List<String> getErrorReports() {
        return errorReports;
    }
}
