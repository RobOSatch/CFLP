package ad2.ss17.cflp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CFLPInstanceReader {
    public static class CFLPInstanceWithThreshold {
        public CFLPInstance instance;
        public int threshold;

        public CFLPInstanceWithThreshold(CFLPInstance instance, int threshold) {
            this.instance = instance;
            this.threshold = threshold;
        }
    }

    private static Pattern THRESHOLD_PATTERN = Pattern.compile("THRESHOLD:\\s*([0-9]+)");

    private static Pattern FACILITIES_PATTERN = Pattern.compile("FACILITIES:\\s*([0-9]+)");

    private static Pattern CUSTOMERS_PATTERN = Pattern.compile("CUSTOMERS:\\s*([0-9]+)");

    private static Pattern MAX_BANDWIDTHS_PATTERN = Pattern.compile("MAXBANDWIDTHS:\\s*([0-9\\s]+)");

    private static Pattern OPENINGCOSTS_PATTERN = Pattern.compile("OPENINGCOSTS:\\s*([0-9\\s]+)");
    private static Pattern DISTANCECOSTS_PATTERN = Pattern.compile("DISTANCECOSTS:\\s*([0-9]+)");

    private String filePath;

    public CFLPInstanceReader(String filePath) {
        this.filePath = filePath;
    }

    public CFLPInstanceWithThreshold readInstance() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filePath)) {
            @Override
            public String readLine() throws IOException {
                String l = super.readLine();
                while (l.startsWith("#") || l.trim().isEmpty())
                    l = super.readLine();
                return l;
            }
        };

        Matcher m;
        String line;

        line = reader.readLine();
        m = THRESHOLD_PATTERN.matcher(line);
        m.matches();
        int threshold = Integer.parseInt(m.group(1));

        line = reader.readLine();
        m = FACILITIES_PATTERN.matcher(line);
        m.matches();
        int numFacilities = Integer.parseInt(m.group(1));

        line = reader.readLine();
        m = CUSTOMERS_PATTERN.matcher(line);
        m.matches();
        int numCustomers = Integer.parseInt(m.group(1));

        line = reader.readLine();
        m = MAX_BANDWIDTHS_PATTERN.matcher(line);
        m.matches();
        int[] baseBandwidths = parseIntList(m.group(1).split("\\s+"), numFacilities);

        line = reader.readLine();
        m = DISTANCECOSTS_PATTERN.matcher(line);
        m.matches();
        int distanceCosts = Integer.parseInt(m.group(1));

        line = reader.readLine();
        m = OPENINGCOSTS_PATTERN.matcher(line);
        m.matches();
        int[] openingCosts = parseIntList(m.group(1).split("\\s+"), numFacilities);


        int[] bandwidths = new int[numCustomers];
        int[][] distances = new int[numFacilities][numCustomers];
        for (int i = 0; i < numCustomers; ++i) {
            line = reader.readLine();
            String[] xs = line.split("; +", 2);
            bandwidths[i] = Integer.parseInt(xs[0]);
            String[] distancesStrs = xs[1].split("\\s+");

            if (distancesStrs.length != numFacilities)
                throw new IOException("Anzahl der Distanzen pro Kunde muss der Anzahl der Facilities entsprechen");

            for (int j = 0; j < numFacilities; ++j) {
                distances[j][i] = Integer.parseInt(distancesStrs[j]);
            }
        }

        reader.close();
        return new CFLPInstanceWithThreshold(new CFLPInstance(baseBandwidths, distanceCosts, openingCosts, bandwidths, distances),
                threshold
        );
    }

    private int[] parseIntList(String[] intStrs, int num) throws IOException {
        if (intStrs.length != num)
            throw new IOException("Falsche Anzahl an Ganzzahlen");

        int[] res = new int[num];
        for (int i = 0; i < num; ++i) {
            res[i] = Integer.parseInt(intStrs[i]);
        }
        return res;
    }
}
