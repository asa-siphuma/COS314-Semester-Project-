import java.util.List;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        // Specify the path to the input file and the XML file for tokens
        String filePath = "input/input.txt"; // Input source code file
        String outputXML = "output/tokens.xml"; // Output XML file for tokens

        try {
            // Step 1: Read input file as a string (Lexer input)
            String input = FileReader.readFileAsString(filePath);

            // Step 2: Initialize XMLWriter to write the tokens to an XML file
            XMLWriter xmlWriter = new XMLWriter(outputXML);

            // Step 3: Initialize Lexer to tokenize the input and write tokens to XML
            Lexer lexer = new Lexer(input, xmlWriter);

            // Step 4: Create tokens and write them to the XML file
            lexer.createTokens(); // Lexer writes tokens to the XML file

            // Step 5: Use XMLTokenReader to read the tokens back from the XML file
            List<Token> tokens = XMLTokenReader.readTokensFromXML(outputXML);

            // Print the tokens
            for (Token token : tokens) {
            System.out.println(token);
            }

            // Step 6: Initialize the parser with the tokens from the XML
            Parser parser = new Parser(tokens);

            // Step 7: Start the parsing process
            parser.parseProgram(); // Parses the tokens according to your grammar

            System.out.println("Parsing completed successfully.\n\n");

            SyntaxTreeXMLWriter writer = new SyntaxTreeXMLWriter(tokens);
            SynNode syntaxTree = writer.parseProgram();

            // Display the syntax tree
            System.out.println("Syntax Tree:");
            syntaxTree.display("");

            // Format the syntax tree
            SyntaxTreeFormatter formatter = new SyntaxTreeFormatter();
            formatter.formatAndSaveSyntaxTree(syntaxTree, "output/syntaxTree.xml");

            SemanticAnalyzer analyzer = new SemanticAnalyzer();
            analyzer.analyze(syntaxTree);

            SymbolTable symbolTable = new SymbolTable();
            TypeChecker typeChecker = new TypeChecker(symbolTable);

            // Step 9: Perform type checking
            boolean isTypeCorrect = typeChecker.typecheck(syntaxTree);

            if (isTypeCorrect) {
                System.out.println("Type checking passed successfully.");
            } else {
                System.out.println("Type checking failed.");
            }

        } catch (IOException e) {
            System.err.println("Error reading the file: " + e.getMessage());
        } catch (ParserException e) {
            System.err.println("Parser error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}