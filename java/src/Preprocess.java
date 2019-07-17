import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class Preprocess {
    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter the FULL path to the corpus : (e.g. /Usr/index.txt or c:\\temp\\index.txt)");
        String corpusPath = br.readLine();

        System.out.println("Enter the FULL path to output folder: (e.g. /Usr/corpus or c:\\temp\\corpus)");
        String outputFolder = br.readLine();

        Files.walkFileTree(Paths.get(corpusPath), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(file.toString()));
                    BufferedWriter writer = new BufferedWriter(new FileWriter(outputFolder+"/"+file.getFileName()));
                    String line = reader.readLine();
                    while (line!=null){
                        if(!line.matches("</?[a-zA-Z]+>") && !line.matches("[\\s\\d]+")){
                            writer.write(line+"\n");
                        }
                        line = reader.readLine();
                    }

                    reader.close();
                    writer.close();
                } catch (IOException e) {
                    System.out.println(e);
                }

                return FileVisitResult.CONTINUE;
            }
        });
    }
}
