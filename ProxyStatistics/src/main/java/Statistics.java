import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.*;

class Statistics {

    static void logValues(String host, int requestCount, int bytesSent, int bytesReceived) {
        File logFile = new File("src/main/resources/statistics.csv");
        if (logFile.exists() && !logFile.isDirectory() && logFile.length() != 0) {
            try (FileInputStream inputStream = new FileInputStream("src/main/resources/statistics.csv")) {
                List<String> hostValuesList = getHostValuesList(inputStream);
                Map<String, List<Integer>> hostsStatsMap = new HashMap<>();
                /* Fill map with hosts and theirs stats */
                for (String hostValue : hostValuesList) {
                    String[] values = hostValue.trim().split("\\s*,\\s*");
                    List<Integer> statsList = new ArrayList<>();
                    statsList.add(Integer.parseInt(values[1]));
                    statsList.add(Integer.parseInt(values[2]));
                    statsList.add(Integer.parseInt(values[3]));
                    hostsStatsMap.put(values[0], statsList);
                }
                /* Add new values */
                if (hostsStatsMap.containsKey(host)) {
                    List<Integer> statsList = new ArrayList<>();
                    statsList.add(hostsStatsMap.get(host).get(0) + requestCount);
                    statsList.add(hostsStatsMap.get(host).get(1) + bytesSent);
                    statsList.add(hostsStatsMap.get(host).get(2) + bytesReceived);
                    hostsStatsMap.put(host, statsList);
                } else
                {
                    List<Integer> statsList = new ArrayList<>();
                    statsList.add(requestCount);
                    statsList.add(bytesSent);
                    statsList.add(bytesReceived);
                    hostsStatsMap.put(host, statsList);
                }
                /* Build log */
                String log = "";
                for (Map.Entry entry : hostsStatsMap.entrySet()) {
                    log += entry.getKey() + ",";
                    log += hostsStatsMap.get(entry.getKey()).get(0) + ",";
                    log += hostsStatsMap.get(entry.getKey()).get(1) + ",";
                    log += hostsStatsMap.get(entry.getKey()).get(2) + "\n";
                }
                /* Save log */
                FileOutputStream output = new FileOutputStream("src/main/resources/statistics.csv", false);
                IOUtils.write(log, output, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            List<Integer> statsList = new ArrayList<>();
            statsList.add(requestCount);
            statsList.add(bytesSent);
            statsList.add(bytesReceived);
            Map<String, List<Integer>> hostsStatsMap = new HashMap<>();
            hostsStatsMap.put(host, statsList);
            String log = host + "," + requestCount + "," + bytesSent + "," + bytesReceived + "\n";
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(logFile, false);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            try {
                IOUtils.write(log, output, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static List<String> getHostValuesList(InputStream inputStream) throws IOException {
        String everything = IOUtils.toString(inputStream, "UTF-8");
        String[] hostValues = everything.split("\\r?\\n");
        List<String> hostValuesList = new ArrayList<>();
        Collections.addAll(hostValuesList, hostValues);

        return hostValuesList;
    }

}
