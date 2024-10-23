import java.util.Map;
import java.util.List;

public class RecSPLTranslator {
    private int labelCounter = 0;
    private Map<String, String> symbolTable;
    private StringBuilder output;

    public RecSPLTranslator(Map<String, String> symbolTable) {
        this.symbolTable = symbolTable;
        this.output = new StringBuilder();
    }

    private String getNewLabel() {
        return "L" + (++labelCounter);
    }

    public String translate(SynNode root) {
        // Start with main program translation
        translateProg(root);
        return output.toString();
    }

    private void translateProg(SynNode prog) {
        // Find ALGO node among children (skip GLOBVARS)
        SynNode algoNode = null;
        for (SynNode child : prog.getChildren()) {
            if (child.getValue() != null && child.getValue().equals("ALGO")) {
                algoNode = child;
                break;
            }
        }

        if (algoNode != null) {
            translateAlgo(algoNode);
            output.append(" STOP ");
        } else {
            output.append("REM Invalid Program Structure");
        }
    }

    private void translateAlgo(SynNode algo) {
        // Find INSTRUC node
        if (!algo.getChildren().isEmpty()) {
            translateInstruc(algo.getChildren().get(1)); // Skip 'begin', get INSTRUC
        }
    }

    private void translateInstruc(SynNode instruc) {
        if (instruc.getChildren().isEmpty()) {
            output.append("REM END");
            return;
        }

        // Get first command and remaining instructions
        SynNode command = instruc.getChildren().get(0);
        translateCommand(command);

        if (instruc.getChildren().size() > 2) { // If there's a semicolon and more instructions
            output.append("\n");
            translateInstruc(instruc.getChildren().get(2));
        }
    }

    private void translateCommand(SynNode command) {
        if (command.getChildren().isEmpty()) {
            return;
        }

        SynNode firstChild = command.getChildren().get(0);
        String cmdType = firstChild.getValue();

        switch (cmdType) {
            case "skip":
                output.append("REM DO NOTHING");
                break;
            case "halt":
                output.append("STOP");
                break;
            case "print":
                translatePrint(command);
                break;
            case "if":
                translateBranch(command);
                break;
            default:
                if (command.isFunctionCall()) {
                    translateCall(command);
                } else {
                    translateAssign(command);
                }
                break;
        }
    }

    private void translatePrint(SynNode printNode) {
        output.append("PRINT ");
        translateAtomic(printNode.getChildren().get(1));
    }

    private void translateAssign(SynNode assign) {
        // Check if it's an input assignment
        if (assign.getChildren().size() > 1 && assign.getChildren().get(1).getValue().equals("<")) {
            String varName = symbolTable.getOrDefault(assign.getChildren().get(0).getValue(), 
                                                    assign.getChildren().get(0).getValue());
            output.append("INPUT ").append(varName);
            return;
        }

        // Regular assignment
        String varName = symbolTable.getOrDefault(assign.getChildren().get(0).getValue(), 
                                                assign.getChildren().get(0).getValue());
        output.append(varName).append(" := ");
        translateTerm(assign.getChildren().get(2));
    }

    private void translateTerm(SynNode term) {
        if (term.isFunctionCall()) {
            translateCall(term);
        } else if (term.getChildren().size() > 0 && 
                  (term.getChildren().get(0).getValue().equals("UNOP") || 
                   term.getChildren().get(0).getValue().equals("BINOP"))) {
            translateOp(term);
        } else {
            translateAtomic(term);
        }
    }

    private void translateAtomic(SynNode atomic) {
        if (atomic.isVariableUsage()) {
            String varName = symbolTable.getOrDefault(atomic.getValue(), atomic.getValue());
            output.append(varName);
        } else {
            // Handle constants
            if (atomic.getType().equals("text")) {
                output.append("\"").append(atomic.getValue()).append("\"");
            } else {
                output.append(atomic.getValue());
            }
        }
    }

    private void translateOp(SynNode op) {
        SynNode opType = op.getChildren().get(0);
        
        if (opType.getValue().equals("UNOP")) {
            String operator = op.getChildren().get(1).getValue();
            String tempVar = "t" + getNewLabel();
            
            output.append(tempVar).append(" := ");
            if (operator.equals("sqrt")) {
                output.append("SQR(");
                translateArg(op.getChildren().get(2));
                output.append(")");
            } else if (operator.equals("not")) {
                output.append("NOT(");
                translateArg(op.getChildren().get(2));
                output.append(")");
            }
        } else {
            // Binary operation
            String operator = translateOperator(op.getChildren().get(1).getValue());
            String tempVar = "t" + getNewLabel();
            
            output.append(tempVar).append(" := ");
            translateArg(op.getChildren().get(2));
            output.append(" ").append(operator).append(" ");
            translateArg(op.getChildren().get(3));
        }
    }

    private void translateArg(SynNode arg) {
        if (arg.getChildren().isEmpty()) {
            translateAtomic(arg);
        } else {
            translateOp(arg);
        }
    }

    private String translateOperator(String op) {
        return switch (op) {
            case "add" -> "+";
            case "sub" -> "-";
            case "mul" -> "*";
            case "div" -> "/";
            case "eq" -> "=";
            case "grt" -> ">";
            default -> op;
        };
    }

    private void translateCall(SynNode call) {
        String funcName = symbolTable.getOrDefault(call.getChildren().get(0).getValue(), 
                                                 call.getChildren().get(0).getValue());
        output.append("CALL_").append(funcName).append("(");
        
        // Translate parameters
        List<SynNode> params = call.getChildren();
        for (int i = 2; i < params.size() - 1; i += 2) { // Skip function name and opening parenthesis
            if (i > 2) output.append(",");
            translateAtomic(params.get(i));
        }
        
        output.append(")");
    }

    private void translateBranch(SynNode branch) {
        String labelTrue = getNewLabel();
        String labelEnd = getNewLabel();

        // Translate condition
        translateCondition(branch.getChildren().get(1), labelTrue, labelEnd);
        
        // Translate else part
        SynNode elseAlgo = findElseAlgo(branch);
        if (elseAlgo != null) {
            translateAlgo(elseAlgo);
        }
        
        output.append("\nGOTO ").append(labelEnd);
        output.append("\n").append(labelTrue).append(": ");
        
        // Translate then part
        SynNode thenAlgo = findThenAlgo(branch);
        if (thenAlgo != null) {
            translateAlgo(thenAlgo);
        }
        
        output.append("\n").append(labelEnd).append(":");
    }

    private void translateCondition(SynNode cond, String labelTrue, String labelFalse) {
        if (cond.getChildren().isEmpty()) return;

        if (isSimpleCondition(cond)) {
            translateSimpleCondition(cond, labelTrue);
        } else {
            translateCompositCondition(cond, labelTrue, labelFalse);
        }
    }

    private boolean isSimpleCondition(SynNode cond) {
        // Check if it's a simple comparison between atomics
        return cond.getChildren().size() == 1 && 
               cond.getChildren().get(0).getValue().equals("SIMPLE");
    }

    private void translateSimpleCondition(SynNode simple, String labelTrue) {
        output.append("IF ");
        translateAtomic(simple.getChildren().get(0));
        output.append(" ").append(translateOperator(simple.getChildren().get(1).getValue())).append(" ");
        translateAtomic(simple.getChildren().get(2));
        output.append(" GOTO ").append(labelTrue);
    }

    private void translateCompositCondition(SynNode composit, String labelTrue, String labelFalse) {
        String operator = composit.getChildren().get(0).getValue();
        
        if (operator.equals("not")) {
            // Handle NOT condition by swapping labels
            translateCondition(composit.getChildren().get(1), labelFalse, labelTrue);
        } else if (operator.equals("and")) {
            String label1 = getNewLabel();
            translateCondition(composit.getChildren().get(1), label1, labelFalse);
            output.append("\n").append(label1).append(": ");
            translateCondition(composit.getChildren().get(2), labelTrue, labelFalse);
        } else if (operator.equals("or")) {
            translateCondition(composit.getChildren().get(1), labelTrue, labelFalse);
            output.append("\n");
            translateCondition(composit.getChildren().get(2), labelTrue, labelFalse);
        }
    }

    private SynNode findThenAlgo(SynNode branch) {
        for (int i = 0; i < branch.getChildren().size(); i++) {
            if (branch.getChildren().get(i).getValue() != null && 
                branch.getChildren().get(i).getValue().equals("then")) {
                return branch.getChildren().get(i + 1);
            }
        }
        return null;
    }

    private SynNode findElseAlgo(SynNode branch) {
        for (int i = 0; i < branch.getChildren().size(); i++) {
            if (branch.getChildren().get(i).getValue() != null && 
                branch.getChildren().get(i).getValue().equals("else")) {
                return branch.getChildren().get(i + 1);
            }
        }
        return null;
    }
}