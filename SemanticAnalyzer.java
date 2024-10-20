import java.util.ArrayList;
import java.util.List;

public class SemanticAnalyzer {
    private final SymbolTable symbolTable = new SymbolTable();
    private final List<String> errorReports = new ArrayList<>();

    public void analyze(SynNode root) {
        crawlSyntaxTree(root, null);
        System.out.println("Symbol Table after analysis:");
        System.out.println(symbolTable.viewSymbolTable());

        System.out.println("Error reports:");
        System.out.println(errorReports);
    }

    private void crawlSyntaxTree(SynNode node, String currentScope) {
        if (node.isFunctionDefinition()) {
            // Handle function declaration
            String functionName = node.getValue();
            if (symbolTable.isDeclared(functionName)) {
                errorReports.add("Function '" + functionName + "' is already declared.");
            } else {
                System.out.println("Function nameee: "+ functionName + " with type: " + node.getType());
                symbolTable.declareVariable(functionName, node.getType());
                symbolTable.enterScope(); // Enter new scope for function
            } 
        } else if (node.isVariableDeclaration()) {
            // Handle variable declaration
            String variableName = node.getValue();
            if (symbolTable.isFunction(variableName)) {
                errorReports.add("Variable name '" + variableName + "' cannot be the same as function name.");
            } else if (symbolTable.isDeclared(variableName)) {
                errorReports.add("Variable '" + variableName + "' is already declared in the current scope.");
            } else {
                symbolTable.declareVariable(variableName, node.getType());
            }
        } else if (node.isVariableUsage()) {
            // Handle variable usage
            String variableName = node.getValue();
            if (!symbolTable.isDeclared(variableName)) {
                errorReports.add("Variable '" + variableName + "' is used without declaration.");
            }
        } else if (node.isFunctionCall()) {
            // Handle function call
            System.out.println("Function call: " + node.getValue());
            String functionName = node.getValue();
            if (!symbolTable.isDeclared(functionName)) {
                errorReports.add("Function '" + functionName + "' is called without declaration.");
            }
        }

        // Recursively visit children nodes
        for (SynNode child : node.children) {
            crawlSyntaxTree(child, currentScope);
        }

        // Exit the scope when leaving a function
        if (node.isFunctionDefinition()) {
            symbolTable.exitScope();
        }
    }

    public List<String> getErrorReports() {
        return errorReports;
    }
}
