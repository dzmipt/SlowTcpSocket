import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class TestClient {

    public static void main(String[] args) throws Exception {
        Socket s = new Socket("localhost", 7777);
        InputStream inp = s.getInputStream();
        OutputStream out = s.getOutputStream();

        byte[] buffer = new byte[4];
        for (int i=0; i<4; i++) buffer[i] = (byte)i;

        byte[] incoming = new byte[4];

        for(;;) {
            out.write(buffer);
            int count = inp.read(incoming);
            System.out.print("Read:");
            for (int i=0; i<count; i++) System.out.print(" "+incoming[i]);
            System.out.println();
            for(int i=0; i<4; i++) buffer[i]++;
        }
    }
}
