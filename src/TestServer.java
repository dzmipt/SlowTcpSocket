import java.io.OutputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class TestServer {

    public static void main(String[] args) throws Exception {
        ServerSocket ss = new ServerSocket(8888);
        Socket s = ss.accept();
        InputStream inp = s.getInputStream();
        OutputStream out = s.getOutputStream();

        byte[] buffer = new byte[4];
        for(int i=0; i<10; i++) {
            int count = inp.read(buffer);

            out.write(buffer,0, count);
        }

        s.close();

    }
}
