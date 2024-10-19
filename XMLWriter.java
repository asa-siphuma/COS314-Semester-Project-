import java.io.FileWriter;
import java.io.IOException;

public class XMLWriter {

    private FileWriter fileWriter;

    public XMLWriter(String filePath) throws IOException {
        fileWriter = new FileWriter(filePath);
        fileWriter.write("<TOKENSTREAM>\n");
    }

    public void writeToken(int id, String tokenClass, String lexeme, int line, int column) throws IOException {
        fileWriter.write("    <TOK>\n");
        fileWriter.write("        <ID>" + id + "</ID>\n");
        fileWriter.write("        <CLASS>" + tokenClass + "</CLASS>\n");
        fileWriter.write("        <WORD>" + lexeme + "</WORD>\n");
        fileWriter.write("        <LINE>" + line + "</LINE>\n");
        fileWriter.write("        <COLUMN>" + column + "</COLUMN>\n");
        fileWriter.write("    </TOK>\n");
    }

    public void close() throws IOException {
        fileWriter.write("</TOKENSTREAM>\n");
        fileWriter.close();
    }
}
