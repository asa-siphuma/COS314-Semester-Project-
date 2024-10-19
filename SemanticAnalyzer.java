import java.util.ArrayList;
import java.util.List;

public class SemanticAnalyzer {
    private final SymbolTable symbolTable = new SymbolTable();
    private final List<String> errorReports = new ArrayList<>();

    public void analyze(SynNode root) {
        crawlSyntaxTree(root, null);
    }

    private void crawlSyntaxTree(SynNode node, String currentScope) {
        if (node.isFunctionDefinition()) {
            // Handle function declaration
            String functionName = node.getValue();
            if (symbolTable.isDeclared(functionName)) {
                errorReports.add("Function '" + functionName + "' is already declared.");
            } else {
                symbolTable.declareVariable(functionName, "function");
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
                symbolTable.declareVariable(variableName, "variable");
            }
        } else if (node.isVariableUsage()) {
            // Handle variable usage
            String variableName = node.getValue();
            if (!symbolTable.isDeclared(variableName)) {
                errorReports.add("Variable '" + variableName + "' is used without declaration.");
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
