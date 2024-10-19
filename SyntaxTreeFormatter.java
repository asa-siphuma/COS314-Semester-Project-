import java.io.FileWriter;
import java.io.IOException;

public class SyntaxTreeFormatter {

    // Method to format the syntax tree and automatically save to an XML file
    public void formatAndSaveSyntaxTree(SynNode root, String filePath) throws IOException {
        StringBuilder output = new StringBuilder();

        // Wrap the entire output within a <SYNTREE> tag
        output.append("<SYNTREE>\n");

        // Start with the ROOT node
        output.append("    <ROOT>\n");
        output.append("        <UNID>").append(root.id).append("</UNID>\n");
        output.append("        <CHILDREN>\n");
        for (SynNode child : root.children) {
            output.append("            <ID>").append(child.id).append("</ID>\n");
        }
        output.append("        </CHILDREN>\n");
        output.append("    </ROOT>\n");

        // Process Inner Nodes
        output.append("    <INNERNODES>\n");
        for (SynNode child : root.children) {
            formatInnerNode(child, output);
        }
        output.append("    </INNERNODES>\n");

        // Process Leaf Nodes
        output.append("    <LEAFNODES>\n");
        formatLeafNodes(root, output);
        output.append("    </LEAFNODES>\n");

        // Close the <SYNTREE> tag
        output.append("</SYNTREE>\n");

        // Save the result to an XML file immediately after formatting
        saveToFile(output.toString(), filePath);
    }

    // Recursive method to format inner nodes
    private void formatInnerNode(SynNode node, StringBuilder output) {
        if (node.children.isEmpty()) {
            return; // Skip leaf nodes
        }
        output.append("        <IN>\n");
        output.append("            <PARENT>").append(node.parent != null ? node.parent.id : "ROOT").append("</PARENT>\n");
        output.append("            <UNID>").append(node.id).append("</UNID>\n");
        output.append("            <CHILDREN>\n");
        for (SynNode child : node.children) {
            output.append("                <ID>").append(child.id).append("</ID>\n");
        }
        output.append("            </CHILDREN>\n");
        output.append("        </IN>\n");

        // Recursively process children
        for (SynNode child : node.children) {
            formatInnerNode(child, output);
        }
    }

    // Method to format leaf nodes (terminals)
    private void formatLeafNodes(SynNode node, StringBuilder output) {
        if (node.children.isEmpty()) { // It's a leaf node
            output.append("        <LEAF>\n");
            output.append("            <PARENT>").append(node.parent != null ? node.parent.id : "ROOT").append("</PARENT>\n");
            output.append("            <UNID>").append(node.id).append("</UNID>\n");
            output.append("            <TERMINAL>").append(node.value != null ? node.value : "").append("</TERMINAL>\n");
            output.append("        </LEAF>\n");
        } else {
            // Recursively process children
            for (SynNode child : node.children) {
                formatLeafNodes(child, output);
            }
        }
    }

    // Method to save the formatted syntax tree to an XML file
    private void saveToFile(String formattedTree, String filePath) throws IOException {
        try (FileWriter fileWriter = new FileWriter(filePath)) { // Auto-close using try-with-resources
            fileWriter.write(formattedTree); // Write formatted tree to file
        }
    }
}
