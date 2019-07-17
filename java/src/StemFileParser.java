import java.io.*;
import java.util.Scanner;

public class StemFileParser {
    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter the FULL path to the stemmed corpus file: (e.g. /Usr/index.txt or c:\\temp\\index.txt)");
        String filePath = br.readLine();

        System.out.println("Enter the FULL path to output folder: (e.g. /Usr/corpus or c:\\temp\\corpus)");
        String outputFolder = br.readLine();

        Scanner scanner = new Scanner(new File(filePath));
        FileWriter fw = null;
        String line;
        int i =1;
        while (scanner.hasNextLine()) {
            line = scanner.nextLine();
            if(line.matches("#\\s*\\d+\\s*")){
                if(fw != null){
                    fw.close();
                }
                fw = new FileWriter(String.format("%s/CACM-%04d.txt", outputFolder, i));
                i++;
            }else{
                fw.write(line+"\n");
            }
        }

        fw.close();
        scanner.close();
    }

}
