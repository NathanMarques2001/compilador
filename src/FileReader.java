import java.io.FileInputStream;
import java.io.IOException;

public class FileReader {

    private int currentByte;

    public FileReader() {}

    public void readFile(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath)) {

            int byteData;
            while ((byteData = fis.read()) != -1) {
                this.currentByte = (byte) byteData;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getCurrentByte() {
        return this.currentByte;
    }
}
