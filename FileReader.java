import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

public class FileReader {

    // Static method to read the content of a file as a string
    public static String readFileAsString(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }
}
