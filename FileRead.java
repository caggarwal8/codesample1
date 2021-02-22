import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author CJ Aggarwal, CS10, November 2020
 */
public class FileRead {
    public static List<String[]> makeWordList(String filename) {
        BufferedReader input = null;
        ArrayList<String[]> sentences = new ArrayList<String[]>(); // String holding whole file

        // Read into string
        try {
            input = new BufferedReader(new FileReader(filename));

            // Create string holding whole file
            String read;
            while ((read = input.readLine()) != null) {
                sentences.add(read.substring(0, read.length() - 1).toLowerCase().split(" "));  // Remove \n, lowercase, split into words
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return null; // Returns null if file runs into error
        }

        finally {
            // Make sure to close the file if it's been opened
            if (input != null) {
                try { input.close(); }
                catch (Exception e) { return null; }
            }
        }

        return sentences;  // Make all words lowercase, split into list of words
    }
}
