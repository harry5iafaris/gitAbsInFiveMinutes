import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import org.apache.commons.codec.digest.DigestUtils;

/*
 * USER NOTE: For all commands that you want to run, at the time of writing, the only way to do so
 * is to use terminal. You must write the following to get this code to compile correctly:
 * "java -cp ".:commons-codec-1.22.1/commons-codec-1.22.1.jar" git.java [YOUR COMMAND HERE]"
 */

public class git {
    public static void main(String[] args) throws IOException {
        if (args[0].equals("init")) {
            init();
        } else if (args[0].equals("add")) {
            if (args.length < 2) {
                System.out.println("Invalid Arguments: " + args[2]);
            } else {
                add(args[1]);
            }
        } else if (args[0].equals("createTree")) {
            if (args.length < 2) {
                System.out.println("Invalid Arguments: " + args[2]);
            } else {
                createTree(args[1]);
            }
        }
    }

    public static void init() {
        File gitFolder = new File("git");
        if (gitFolder.exists()) {
            System.out.println("Git Repository Already Exists");
            return;
        }
        gitFolder.mkdir();

        File objectsFolder = new File(gitFolder, "objects");
        objectsFolder.mkdir();

        File indexFile = new File(gitFolder, "INDEX");
        try {
            indexFile.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }

        File headFile = new File(gitFolder, "HEAD");
        try {
            headFile.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void add(String filePath) throws IOException {
        byte[] fileContents = Files.readAllBytes(Path.of(filePath));
        List<String> indexLines = Files.readAllLines(Path.of("git", "INDEX"));

        filePath = "gitAbsInFiveMinutes/" + filePath;

        String hashValue = hashFile(fileContents);
        Path objectsPath = Path.of("git", "objects", hashValue);
        Path indexPath = Path.of("git", "INDEX");

        for (int lineNumber = 0; lineNumber < indexLines.size(); lineNumber++) {
            if (indexLines.get(lineNumber).contains(filePath)) {
                if (indexLines.get(lineNumber).contains(hashValue)) {
                    return;
                }
                indexLines.remove(lineNumber);
                lineNumber--;
            }
        }

        indexLines.add(hashValue + " " + filePath);

        for (int lineNumber = 0; lineNumber < indexLines.size(); lineNumber++) {
            if (lineNumber == indexLines.size() - 1) {
                Files.writeString(indexPath, indexLines.get(lineNumber));
            } else {
                Files.writeString(indexPath, indexLines.get(lineNumber) + "\n");
            }
        }

        Files.writeString(indexPath, String.join("\n", indexLines));

        // StandardOpenOption.CREATE creates the file if it does not already exist
        Files.write(objectsPath, fileContents, StandardOpenOption.CREATE);

    }

    /*
     * Generates a tree file containing references to all files and subdirectories
     * in that directory
     * Returns the SHA-1 hash of the generated tree
     * (Currently only works for creating folders starting from the bottom)
     */
    public static String createTree(String directoryPath) throws IOException {
        List<String> indexLines = Files.readAllLines(Path.of("git", "INDEX"));
        StringBuilder childPaths = new StringBuilder();

        directoryPath = "gitAbsInFiveMinutes/" + directoryPath;

        for (int lineNumber = 0; lineNumber < indexLines.size(); lineNumber++) {
            String line = indexLines.get(lineNumber);
            if (line.contains(directoryPath)) {
                // Is the path section of the line the same depth as the directoryPath => is it
                // a folder or a file
                if (line.substring(line.indexOf(" ") + 1, line.lastIndexOf("/")).equals(directoryPath)) {
                    childPaths.append("blob " + line);
                } else {
                    childPaths.append("tree " + line);
                }
                if (lineNumber != indexLines.size() - 1) {
                    childPaths.append("\n");
                }
                indexLines.remove(lineNumber);
                lineNumber--;
            }
        }

        String treeHash = hashFile(childPaths.toString().getBytes());

        File tree = new File("git/objects/" + treeHash);

        Files.write(tree.toPath(), childPaths.toString().getBytes(), StandardOpenOption.CREATE);

        return treeHash;
    }

    public static String hashFile(byte[] input) {
        String hashValue = DigestUtils.sha1Hex(input);
        return hashValue;
    }

}
