import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class XMLTokenReader {

    public static List<Token> readTokensFromXML(String filePath) {
        List<Token> tokens = new ArrayList<>();
        try {
            // Set up XML parser
            File xmlFile = new File(filePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();
            
            // Get all <TOK> elements
            NodeList nList = doc.getElementsByTagName("TOK");

            // Iterate through tokens in XML
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    
                    // Extract <ID>, <CLASS>, and <WORD> values
                    int id = Integer.parseInt(eElement.getElementsByTagName("ID").item(0).getTextContent());
                    String tokenClass = eElement.getElementsByTagName("CLASS").item(0).getTextContent();
                    String value = eElement.getElementsByTagName("WORD").item(0).getTextContent();
                    int line = Integer.parseInt(eElement.getElementsByTagName("LINE").item(0).getTextContent());
                    int column = Integer.parseInt(eElement.getElementsByTagName("COLUMN").item(0).getTextContent());

                    // Determine TokenType based on the <CLASS>
                    Token.TokenType tokenType;
                    if (tokenClass.equals("reserved_keyword")) {
                        tokenType = Token.TokenType.KEYWORD; // Treat operators and symbols as reserved_keyword
                    } else if (tokenClass.equals("V")) {
                        tokenType = Token.TokenType.VARIABLE_NAME;
                    } else if (tokenClass.equals("F")) {
                        tokenType = Token.TokenType.FUNCTION_NAME;
                    } else if (tokenClass.equals("N")) {
                        tokenType = Token.TokenType.NUMBER;
                    } else if (tokenClass.equals("T")) {
                        tokenType = Token.TokenType.TEXT_CONSTANT;
                    } else {
                        throw new IllegalArgumentException("Unknown token class: " + tokenClass);
                    }

                    // Create and add the token with default line and column numbers
                    Token token = new Token(tokenType, value, line, column); // Default line/column since they are missing
                    token.setTokenClass(tokenClass);
                    tokens.add(token);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return tokens;
    }
}
