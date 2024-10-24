import java.util.HashMap;
import java.util.Map;

public class Translator {

    int lineNumber = 10;
    SynNode parseTree;
    String scope = "GLOBAL";
    StringBuilder basicProg = new StringBuilder();
    private int labelCounter = 0; // counter to generate unique labels
    private static Map<String, Integer> labelMap = new HashMap<>();
    // private HashMap<String, HashMap<String, String>> varMap = new HashMap<>();

    public Translator(SynNode parseTree) {
        this.parseTree = parseTree;
    }

    public void translate() {
        SynNode prog = parseTree;
        StringBuilder algoCode = new StringBuilder();
        StringBuilder funcCode = new StringBuilder();

        for (SynNode child : prog.getChildren()) {
            if (child.getType().equals("GLOBVARS")) {
                // GLOBVARS are not translated according to the specification
                // Skip global variable declarations
            } else if (child.getType().equals("ALGO")) {
                algoCode.append(translateALGO(child)); // ALGO translation
            } else if (child.getType().equals("FUNCTIONS")) {
                funcCode.append(translateFUNCTIONS(child)); // FUNCTIONS translation
            }
        }

        // Append STOP after ALGO to prevent main code from running into function code
        basicProg.append(algoCode).append("STOP\n").append(funcCode);
    }

    private String translateALGO(SynNode algo) {
        StringBuilder algoCode = new StringBuilder();
        for (SynNode child : algo.getChildren()) {
            if (child.getType().equals("INSTRUC")) {
                algoCode.append(translateINSTRUC(child));
            }
        }
        return algoCode.toString();
    }

    private String translateINSTRUC(SynNode instruc) {
        StringBuilder instrucCode = new StringBuilder();
        for (SynNode child : instruc.getChildren()) {
            if (child.getType().equals("COMMAND")) {
                instrucCode.append(translateCOMMAND(child)).append(";\n");
            }
        }
        return instrucCode.toString();
    }

    private String translateCOMMAND(SynNode command) {
        switch (command.getChildren().get(0).getType()) {
            case "skip":
                return "REM DO NOTHING";
            case "halt":
                return "STOP";
            case "print":
                return "PRINT " + translateATOMIC(command.getChildren().get(1));
            case "ASSIGN":
                return translateASSIGN(command);
            case "CALL":
                return translateCALL(command);
            case "BRANCH":
                return translateBRANCH(command);
            default:
                return "";
        }
    }

    private String translateASSIGN(SynNode assign) {
        String varName = translateVNAME(assign.getChildren().get(0));
        if (assign.getChildren().get(1).getValue().equals("<")) {
            // Input assignment: VNAME < input
            return "INPUT " + varName;
        } else if (assign.getChildren().get(1).getValue().equals("=")) {
            // VNAME = TERM
            return "LET " + varName + " = " + translateTERM(assign.getChildren().get(2));
        }
        return "";
    }

    private String translateCALL(SynNode call) {
        String fname = translateFNAME(call.getChildren().get(0));
        String arg1 = translateATOMIC(call.getChildren().get(1));
        String arg2 = translateATOMIC(call.getChildren().get(2));
        String arg3 = translateATOMIC(call.getChildren().get(3));
        return "CALL_" + fname + "(" + arg1 + "," + arg2 + "," + arg3 + ")";
    }

    private String translateBRANCH(SynNode branch) {
        String ifLabel = generateLabel();
        String endLabel = generateLabel();

        String condCode = translateCOND(branch.getChildren().get(1));
        String thenCode = translateALGO(branch.getChildren().get(3));
        String elseCode = translateALGO(branch.getChildren().get(5));

        return "IF " + condCode + " THEN " + thenCode + " GOTO " + ifLabel + "\n"
                + elseCode + " GOTO " + endLabel + "\n"
                + ifLabel + ":\n"
                + thenCode + "\n"
                + endLabel + ":\n";
    }

    private String translateTERM(SynNode term) {
        SynNode child = term.getChildren().get(0);
        switch (child.getType()) {
            case "ATOMIC":
                return translateATOMIC(child);
            case "CALL":
                return translateCALL(child);
            case "OP":
                return translateOP(child);
            default:
                return "";
        }
    }

    private String translateOP(SynNode op) {
        String opType = op.getChildren().get(0).getType();
        String arg1 = translateARG(op.getChildren().get(1));
        if (opType.equals("UNOP")) {
            return translateUNOP(op.getChildren().get(0)) + "(" + arg1 + ")";
        } else if (opType.equals("BINOP")) {
            String arg2 = translateARG(op.getChildren().get(2));
            return arg1 + " " + translateBINOP(op.getChildren().get(0)) + " " + arg2;
        }
        return "";
    }

    private String translateARG(SynNode arg) {
        if (arg.getChildren().get(0).getType().equals("ATOMIC")) {
            return translateATOMIC(arg.getChildren().get(0));
        } else {
            return translateOP(arg.getChildren().get(0));
        }
    }

    private String translateUNOP(SynNode unop) {
        if (unop.getValue().equals("not")) {
            return "NOT";
        } else if (unop.getValue().equals("sqrt")) {
            return "SQR";
        }
        return "";
    }

    private String translateBINOP(SynNode binop) {
        switch (binop.getValue()) {
            case "or":
                return "OR";
            case "and":
                return "AND";
            case "eq":
                return "=";
            case "grt":
                return ">";
            case "add":
                return "+";
            case "sub":
                return "-";
            case "mul":
                return "*";
            case "div":
                return "/";
            default:
                return "";
        }
    }

    private String translateCOND(SynNode cond) {
        SynNode child = cond.getChildren().get(0);
        if (child.getType().equals("SIMPLE")) {
            return translateSIMPLE(cond);
        } else {
            return translateCOMPOSIT(cond);
        }
    }

    private String translateSIMPLE(SynNode simple) {
        String atomic1 = translateATOMIC(simple.getChildren().get(0));
        String op = translateBINOP(simple.getChildren().get(1));
        String atomic2 = translateATOMIC(simple.getChildren().get(2));
        return atomic1 + " " + op + " " + atomic2;
    }

    private String translateCOMPOSIT(SynNode composit) {
        SynNode child = composit.getChildren().get(0);
        if (child.getType().equals("UNOP")) {
            return translateUNOP(composit.getChildren().get(0)) + "(" + translateSIMPLE(composit.getChildren().get(1)) + ")";
        } else {
            return translateSIMPLE(composit.getChildren().get(0)) + " " + translateBINOP(composit.getChildren().get(1)) + " " + translateSIMPLE(composit.getChildren().get(2));
        }
    }

    private String translateFUNCTIONS(SynNode functions) {
        StringBuilder funcCode = new StringBuilder();
        for (SynNode child : functions.getChildren()) {
            if (child.getType().equals("DECL")) {
                funcCode.append(translateDECL(child));
            } else if (child.getType().equals("FUNCTIONS")) {
                funcCode.append(translateFUNCTIONS(child));
            }
        }
        return funcCode.toString();
    }

    private String translateDECL(SynNode decl) {
        String header = translateHEADER(decl.getChildren().get(0));
        String body = translateBODY(decl.getChildren().get(1));
        return header + body;
    }

    private String translateHEADER(SynNode header) {
        String fname = translateFNAME(header.getChildren().get(0));
        return "REM Function " + fname + "\n";
    }

    private String translateBODY(SynNode body) {
        String prolog = translatePROLOG(body.getChildren().get(0));
        String locvars = translateLOCVARS(body.getChildren().get(1));
        String algo = translateALGO(body.getChildren().get(2));
        String epilog = translateEPILOG(body.getChildren().get(3));
        String subfuncs = translateFUNCTIONS(body.getChildren().get(4));
        return prolog + locvars + algo + epilog + subfuncs;
    }

    private String translatePROLOG(SynNode prolog) {
        return "REM BEGIN\n";
    }

    private String translateEPILOG(SynNode epilog) {
        return "REM END\n";
    }

    private String translateLOCVARS(SynNode locvars) {
        // Local variables are ignored in the translation
        return "";
    }

    private String translateVNAME(SynNode vname) {
        return vname.getValue();
    }

    private String translateFNAME(SynNode fname) {
        return fname.getValue();
    }

    private String translateATOMIC(SynNode atomic) {
        if (atomic.getType().equals("CONST")) {
            return translateCONST(atomic.getChildren().get(0));
        } else {
            return translateVNAME(atomic.getChildren().get(0));
        }
    }

    private String translateCONST(SynNode constant) {
        return constant.getValue();
    }

    private String generateLabel() {
        return "LBL" + (labelCounter++);
    }

    // Main method to test the translation
    public String translateToBasicWithLineNumbers() {
        String[] lines = basicProg.toString().split("\n");
        StringBuilder translatedCode = new StringBuilder();
        int lineNumber = 10;

        // First pass: Identify labels and their corresponding line numbers
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("DEF") || line.startsWith("LBL")) {
                String[] parts = line.split(" ");
                if (parts.length > 1) {
                    String label = parts[1];
                    labelMap.put(label, lineNumber);
                } else {
                    String label = line;
                    labelMap.put(label, lineNumber);
                }
            }
            lineNumber += 10;
        }

        // Reset line number for second pass
        lineNumber = 10;

        // Second pass: Replace labels and add line numbers
        for (String line : lines) {
            if (line.startsWith("DEF")) {
                String label = line.split(" ")[1];
                translatedCode.append(lineNumber).append(" REM ").append(label).append("\n");
            } else if (line.contains("GOTO")) {
                String[] parts = line.split(" ");
                if (parts.length > 2) {
                    translatedCode.append(lineNumber).append(" ").append(parts[0]).append(" ").append(parts[1]).append(" ");
                    translatedCode.append(parts[2]).append(" ").append(getLineNumberForLabel(parts[3])).append("\n");
                } else {
                    translatedCode.append(lineNumber).append(" ").append(parts[0]).append(" ");
                    translatedCode.append(getLineNumberForLabel(parts[1])).append("\n");
                }
            } else if (line.contains("GOSUB")) {
                String[] parts = line.split(" ");
                String command = parts[0];
                String label = parts[1];
                translatedCode.append(lineNumber).append(" ").append(command).append(" ").append(getLineNumberForLabel(label)).append("\n");
            } else if (line.startsWith("LBL")) {
                translatedCode.append(lineNumber).append("\n");
            } else {
                translatedCode.append(lineNumber).append(" ").append(line).append("\n");
            }
            lineNumber += 10;
        }

        return translatedCode.toString();
    }

    private int getLineNumberForLabel(String label) {
        return labelMap.getOrDefault(label, -1);
    }
}
