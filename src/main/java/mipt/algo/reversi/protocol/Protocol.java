package mipt.algo.reversi.protocol;

import java.io.InputStream;
import java.util.Map;

/**
 * ReversiServer
 * mipt.algo.reversi.protocol
 * <p>
 * Created by ilya on 03.05.17.
 */
public interface Protocol {

    Map.Entry<Integer, Integer> decode(InputStream stream);

    String decodeString(InputStream stream);

    byte[] encode(String string);
}
