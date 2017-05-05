package mipt.algo.reversi.testing;

import mipt.algo.reversi.protocol.Protocol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;

/**
 * ReversiServer
 * mipt.algo.reversi.server
 * <p>
 * Created by ilya on 03.05.17.
 */
public class GameClient {

    private Socket socket;

    private InputStream in;
    private OutputStream out;
    private Protocol protocol;

    private boolean valid;

    public GameClient(Socket socket, Protocol protocol, Integer timeout) {
        this.socket = socket;
        this.protocol = protocol;

        valid = true;
        try {
            //socket.setSoTimeout(timeout);

            in = socket.getInputStream();
            out = socket.getOutputStream();
        } catch (IOException e) {
            valid = false;
        }
    }

    public void sendMove(Integer x, Integer y) {
        sendMessage("move " + x + " " + (char)('a' + y));
    }

    public void sendMessage(String str) {
        try {
            out.write(protocol.encode(str));
        } catch (IOException e) {
            valid = false;
        }
    }

    public Map.Entry<Integer, Integer> readTurn() {
        if (!valid) {
            return null;
        }
        Map.Entry<Integer, Integer> msg = protocol.decode(in);
        if (msg == null) {
            valid = false;
        }
        return msg;
    }

    public String readToken() {
        return protocol.decodeString(in);
    }

    public void sendTurn() {
        sendMessage("turn");
    }
}
