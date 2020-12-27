import java.net.Socket;

public class Proxy {

    public static void main(String[] args) {
        Args.init(args);
        int count = Args.listenPorts.size();
        for (int index = 0; index<count; index++) {
            new Thread(new Listener(index), "Listener " + index).start();
        }
    }
}
