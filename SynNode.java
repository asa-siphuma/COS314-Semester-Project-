import java.util.ArrayList;
import java.util.List;

// SynNode class to represent each node of the syntax tree
class SynNode {
    static int uniqueIdCounter = 1;  // To keep track of unique IDs
    List<SynNode> children;          // List of child nodes
    int id;                          // Unique ID for each node
    SynNode parent;                  // Reference to the parent node
    String value;                    // Value for terminal nodes (e.g., "num", "V_a")
    Boolean isVariableDeclaration = false;
    Boolean isFunctionDefinition = false;
    Boolean isFunctionCall = false;
    Boolean isVariableUsage = false;
    String type = "";

    // Constructor for non-terminal nodes (inner nodes)
    public SynNode() {
        this.children = new ArrayList<>();
        this.id = uniqueIdCounter++;  // Assign a unique ID to the node
        this.parent = null;           // Initialize parent to null
    }

    // Constructor for terminal nodes (leaf nodes)
    public SynNode(String value) {
        this();  // Call the default constructor
        this.value = value;
    }

    // Method to add a child node and set parent
    public void addChild(SynNode child) {
        child.parent = this;  // Set the parent reference
        this.children.add(child);
    }

    public boolean isParameterSection() {
        // Assuming your HEADER nodes have a specific value that identifies them
        return "HEADER".equals(value);
    }

    // Method to print the tree for debugging (optional)
    public void display(String indent) {
        System.out.println(indent + "[ID: " + id + (value != null ? ", Value: " + value : "") + "]");
        for (SynNode child : children) {
            child.display(indent + "  ");
        }
    }

    // get children
    public List<SynNode> getChildren() {
        return children;
    }

    // retrun valu
    public String getValue() {
        return value;
    }

    public Boolean setIsVariableDeclaration(Boolean isVariableDeclaration) {
        this.isVariableDeclaration = isVariableDeclaration;
        return isVariableDeclaration;
    }

    public Boolean setIsFunctionDefinition(Boolean isFunctionDefinition) {
        this.isFunctionDefinition = isFunctionDefinition;
        return isFunctionDefinition;
    }

    public Boolean setIsVariableUsage(Boolean isVariableUsage) {
        this.isVariableUsage = isVariableUsage;
        return isVariableUsage;
    }

    public Boolean setIsFunctionCall(Boolean isFunctionCall) {
        this.isFunctionCall = isFunctionCall;
        return isFunctionCall;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    // get parent by id
    public SynNode getParent() {
        return parent;
    }

    public boolean isVariableDeclaration() {
        return isVariableDeclaration;
    }

    public boolean isFunctionDefinition() {
        return isFunctionDefinition;
    }   

    public boolean isVariableUsage() {
        return isVariableUsage;
    }

    public boolean isFunctionCall() {
        return isFunctionCall;
    }
}
