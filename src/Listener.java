import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

public class Listener implements Runnable {

    private int port;
    private String host;
    private int targetPort;

    private int index = 0;

    public Listener(int index) {
        port = Args.listenPorts.get(index);
        host = Args.targetHosts.get(index);
        targetPort = Args.targetPorts.get(index);
    }

    @Override
    public void run() {
        try {
            System.out.println("Start listener on port " + port);
            ServerSocket ss = new ServerSocket(port);
            while (true) {
                Socket s = ss.accept();
                System.out.printf("Start stream %d on port %d%n",index,port);
                new Thread(new StreamCreator(index, s), "Stream " + index + " on port " + port).start();
                index++;
            }
        } catch (Exception e) {
            System.err.printf("Error on port %d: %s%n", port, e);
            e.printStackTrace(System.err);
        }
    }

    class StreamCreator implements Runnable {
        int index;
        Socket sourceSocket;
        StreamCreator(int index, Socket sourceSocket) {
            this.index = index;
            this.sourceSocket = sourceSocket;
        }
        @Override
        public void run() {
            try {
                Socket targetSocket = new Socket(host, targetPort);

                String name = String.format("%d - %d->%s:%d",index, port, host, targetPort);
                new Thread(new Stream(name, sourceSocket.getInputStream(), targetSocket.getOutputStream(),
                                        Args.outgoingLimitInBytes, Args.tickInMs,
                                        sourceSocket, targetSocket),
                            name).start();

                name = String.format("%d - %s:%d->%d",index, host, targetPort, port);
                new Thread(new Stream(name,targetSocket.getInputStream(), sourceSocket.getOutputStream(),
                        Args.incomingLimitInBytes, Args.tickInMs,
                        sourceSocket, targetSocket),
                        name).start();

            } catch (Exception e) {
                System.err.printf("Error on stream creator %d for port %d: %s%n", index, port, e);
                e.printStackTrace(System.err);
            }
        }
    }

    class Stream implements Runnable {

        String name;
        InputStream from;
        OutputStream to;
        long tick;
        byte[] buffer;
        int bufOff,bufLen;
        int size;
        int counter;
        long timeFrame;
        Socket sourceSocket, targetSocket;

        Stream(String name, InputStream from, OutputStream to, long throughput, long tick, Socket sourceSocket, Socket targetSocket) {
            this.name = name;
            this.from = from;
            this.to = to;
            this.tick = tick;
            this.sourceSocket = sourceSocket;
            this.targetSocket = targetSocket;


            if (throughput == -1) {
                size = Integer.MAX_VALUE;
                buffer = new byte[16*1024*1024];
            } else {
                double ratio = 1000.0 / tick;
                size = (int) (throughput / ratio);
//                buffer = new byte[size];
                buffer = new byte[16*1024*1024];

            }

            counter = size;
            timeFrame = 0;
            bufOff = 0;
            bufLen = 0;
        }

        @Override
        public void run() {
            try {
                for(;;) {
                    if (bufLen > 0) {
                        int toSend = Math.min(counter, bufLen);
                        to.write(buffer, bufOff, toSend);
//                        System.out.printf("[%s] Write %d%n", name, toSend);
                        bufOff += toSend;
                        bufLen -= toSend;
                        counter -= toSend;
                    }

                    if (bufLen == 0) {
                        bufLen = from.read(buffer);
//                        System.out.printf("[%s] Read %d%n",name, bufLen);
                        if (bufLen == -1) break;
                        bufOff = 0;
                    }

                    if (counter > 0) continue;

                    // now bufLen > 0 - we have something to publish
                    // now counter == 0 - we publish everything during current time frame
                    long toSleep = timeFrame - System.currentTimeMillis();
                    if (toSleep > 0) {
                        Thread.sleep(toSleep);
                    }
                    timeFrame = System.currentTimeMillis() + tick;
                    counter = size;
                }
            } catch (IOException e) {
                System.out.println("Likely socket is broken: " + name);
                e.printStackTrace();
            } catch (InterruptedException e) {
                System.out.println("Interrupted");
            }

            try {
                targetSocket.close();
            } catch (IOException e) {}

            try {
                sourceSocket.close();
            } catch (IOException e) {}

            System.out.println("Finished " + name);
        }
    }
}
