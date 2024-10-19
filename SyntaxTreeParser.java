import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import java.io.File;
import java.util.HashMap;

public class SyntaxTreeParser {

    public SynNode parseSyntaxTree(String filePath) {
        try {
            File inputFile = new File(filePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            // Starting point: retrieve the root node
            Element rootElement = (Element) doc.getElementsByTagName("ROOT").item(0);
            return buildNodeFromElement(rootElement);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private SynNode buildNodeFromElement(Element element) {
        SynNode node = new SynNode();
        node.id = Integer.parseInt(element.getElementsByTagName("UNID").item(0).getTextContent());

        NodeList childNodes = element.getElementsByTagName("ID");
        for (int i = 0; i < childNodes.getLength(); i++) {
            Element childElement = (Element) childNodes.item(i);
            SynNode childNode = buildNodeFromElement(childElement);
            node.addChild(childNode);
        }
        return node;
    }
}
