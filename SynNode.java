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

    // Method to print the tree for debugging (optional)
    public void display(String indent) {
        System.out.println(indent + "[ID: " + id + (value != null ? ", Value: " + value : "") + "]");
        for (SynNode child : children) {
            child.display(indent + "  ");
        }
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

    // get parent by id
    public SynNode getParent() {
        return parent;
    }

    public boolean isVariableDeclaration() {
    }

    public boolean isFunctionDefinition() {
    }   

    public boolean isVariableUsage() {
    }

}
