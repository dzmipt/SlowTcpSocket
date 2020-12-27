import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Args {

    public static Properties props;

    public static List<Integer> listenPorts = new ArrayList<>();
    public static List<String> targetHosts = new ArrayList<>();
    public static List<Integer> targetPorts = new ArrayList<>();

    public static long tickInMs = 1000;
    public static long incomingLimitInBytes = -1;
    public static long outgoingLimitInBytes = -1;

    private static final String[] throughputSuffixes = {"Kbps","Mbps","bps","Kb/s", "Mb/s", "b/s"};
    private static final double[] throughputMultipliers = {1000/8.0, 1000000/8.0, 1/8.0, 1024, 1024*1024, 1};


    private static final String FILE_NAME = "proxy.conf";

    private static long get(String key, long defValue) {
        String value = props.getProperty(key);
        if (value == null) return defValue;
        return Long.parseLong(value.trim());
    }

    private static int get(String key, int defValue) {
        String value = props.getProperty(key);
        if (value == null) return defValue;
        return Integer.parseInt(value.trim());
    }

    private static double getValue(String value, String[] suffixes, double[] multipliers) {
        double multiplier = 1;
        value = value.trim().toLowerCase();
        for (int index = 0; index<suffixes.length; index++) {
            if (value.endsWith(suffixes[index].toLowerCase())) {
                value = value.substring(0, value.length() - suffixes[index].length()).trim();
                multiplier = multipliers[index];
                break;
            }
        }
        return multiplier * Double.parseDouble(value);
    }

    private static long getThroughput(String key) {
        String value = props.getProperty(key);
        if (value == null) return -1;
        return (long) getValue(value, throughputSuffixes, throughputMultipliers);
    }

    public static void init(String[] args) {
        try {
            props = new Properties();
            props.load(Proxy.class.getResourceAsStream(FILE_NAME));

            tickInMs = get("tick", 1000L);
            incomingLimitInBytes = getThroughput("throughput.incoming");
            outgoingLimitInBytes = getThroughput("throughput.outgoing");


            for (int index=1; ; index++) {
                String connection = props.getProperty("map" + index + ".target");
                if (connection == null) break;
                connection = connection.trim();
                int i = connection.indexOf(':');
                if (i == -1) throw new IllegalArgumentException("Connection is expected in format host:port");
                targetHosts.add( connection.substring(0,i) );
                int port = Integer.parseInt(connection.substring(i+1));
                targetPorts.add(port);
                listenPorts.add(get("map" + index + ".listen", port));
            }

        } catch (Exception e) {
            System.err.println("Error in initialisation: " + e);
            e.printStackTrace(System.err);
            System.exit(-1);
        }
    }

}
