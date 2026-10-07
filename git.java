import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
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
                createTree(args[1], null);
            }
        } else if (args[0].equals("rootTree")) {
            if (args.length < 1) {
                System.out.println("Invalid Arguments: " + args[1]);
            } else {
                rootTree();
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
        if (filePath.contains(" ")) {
            System.out.println("Invalid character ' ' in path " + filePath);
            return;
        }
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
     * Creates BLOBs for the entire repository stemming back to the root folder. Begins by sorting a
     * copy of the index list, then
     */
    public static String rootTree() throws IOException {
        List<String> workingList = Files.readAllLines(Path.of("git", "INDEX"));
        workingList.sort(Comparator.comparing(line -> line.split(" ")[1]));

        while (workingList.size() > 1) {
            int maxFileDepth = 0; // The max amount of "/" present in any line.
            String deepestDirectoryPath = "gitAbsInFiveMinutes";
            for (int line = 0; line < workingList.size(); line++) {
                if (!(workingList.get(line).substring(0, 4).equals("blob")
                        || workingList.get(line).substring(0, 4).equals("tree"))) {
                    workingList.set(line, "blob " + workingList.get(line));
                }
                String[] lineParts = workingList.get(line).split(" "); // To handle / in a file name
                int fileDepth = lineParts[lineParts.length - 1].split("/").length;
                if (fileDepth > maxFileDepth) {
                    maxFileDepth = fileDepth;
                    deepestDirectoryPath = lineParts[2].substring(0, lineParts[2].lastIndexOf("/"));
                }
            }

            String dirHash = createTree(deepestDirectoryPath, workingList);
            workingList.add("tree " + dirHash + " " + deepestDirectoryPath);
        }

        String dirHash = createTree("gitAbsInFiveMinutes", workingList);
        workingList.add("tree " + dirHash + " " + "gitAbsInFiveMinutes");

        return workingList.get(0).split(" ")[1];
    }

    /*
     * Generates a tree file containing references to all files and subdirectories in that directory
     * Returns the SHA-1 hash of the generated tree
     */
    public static String createTree(String directoryPath, List<String> workingList)
            throws IOException {
        if (workingList == null) {
            workingList = Files.readAllLines(Path.of("git", "INDEX"));
        }
        StringBuilder childPaths = new StringBuilder();

        for (int lineNumber = 0; lineNumber < workingList.size(); lineNumber++) {
            String line = workingList.get(lineNumber);
            if (line.contains(directoryPath)) {
                childPaths.append(line);
                if (lineNumber != workingList.size() - 1) {
                    childPaths.append("\n");
                }
                workingList.remove(lineNumber);
                lineNumber--;
            }
        }

        String treeContents = childPaths.toString();
        if (treeContents.endsWith("\n")) {
            treeContents = treeContents.substring(0, treeContents.length() - 1);
        }

        String treeHash = hashFile(treeContents.getBytes());

        File tree = new File("git/objects/" + treeHash);

        Files.write(tree.toPath(), treeContents.getBytes(), StandardOpenOption.CREATE);

        return treeHash;
    }

    public static String hashFile(byte[] input) {
        String hashValue = DigestUtils.sha1Hex(input);
        return hashValue;
    }

}
