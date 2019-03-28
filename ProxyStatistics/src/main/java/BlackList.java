import java.io.*;
import java.util.LinkedList;

public class BlackList {
    BlackList() {
        getBlackList();
    }

    private LinkedList<String> blackList = new LinkedList<String>();

    public LinkedList<String> getBlackList() {
        try {

            FileInputStream inputStream = new FileInputStream("src/main/resources/blacklist.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            System.out.println("Read blacklist **************");

            String line = reader.readLine();

            while(line != null) {
                if (!blackList.contains(line)) {
                    blackList.add(line);
                    System.out.println("BLACKLIST: " + line);
                }
                line = reader.readLine();
            }

            reader.close();
            inputStream.close();
            System.out.println("Read blacklist END **********");
            System.out.println();

        } catch (FileNotFoundException ef) {
            ef.fillInStackTrace();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return blackList;
    }

    public boolean checkUrl(String url) {
        for (String blackUrl : blackList) {
            if (url.endsWith(blackUrl)) {
                return true;
            }
        }
        return false;
    }
}
