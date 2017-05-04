package mipt.algo.reversi.protocol;

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.Map;

/**
 * ReversiServer
 * mipt.algo.reversi.protocol
 * <p>
 * Created by ilya on 03.05.17.
 */
public class StringProtocol implements Protocol {

    public Integer decodeInteger(byte[] array) {
        Integer result = 0;
        Integer maxByteValue = 1 << Byte.SIZE;
        for (int i = 0; i < Integer.BYTES; i++) {
            result = (result << Byte.SIZE) + (array[i] + maxByteValue) % maxByteValue;
        }
        return result;
    }

    public Integer decodeInteger(InputStream stream) {
        byte[] buffer = new byte[Integer.BYTES];
        Integer result;
        try {
            if (stream.read(buffer) == Integer.BYTES) {
                result = decodeInteger(buffer);
            } else {
                result = -1;
            }
        } catch (IOException ex) {
            result = -1;
        }
        return result;
    }

    @Override
    public String decodeString(InputStream stream) {
        Integer length = decodeInteger(stream);
        byte[] buffer = new byte[length];
        String result = null;
        try {
            if (stream.read(buffer) == length) {
                result = new String(buffer);
            }
        } catch (IOException e) {
            result = null;
        }
        return result;
    }

    @Override
    public Map.Entry<Integer, Integer> decode(InputStream stream) {
        String string = decodeString(stream);

        if (string == null) {
            return null;
        }
        Map.Entry<Integer, Integer> result;

        String[] args = string.split(" ");

        if (args.length != 3 || !args[0].equals("move")) {
            return null;
        }

        Integer x;
        Integer y;
        try {
            x = Integer.parseInt(args[1]);
            y = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            return null;
        }

        return new AbstractMap.SimpleEntry<>(x, y);
    }

    public byte[] encode(Integer value) {
        byte[] bytes = new byte[Integer.BYTES];
        for (int i = 0; i < Integer.BYTES; i++) {
            bytes[Integer.BYTES - i - 1] = value.byteValue();
            value >>= Byte.SIZE;
        }
        return bytes;
    }

    @Override
    public byte[] encode(String string) {
        ByteOutputStream bos = new ByteOutputStream();
        bos.write(encode(string.length()));
        bos.write(string.getBytes());
        return bos.getBytes();
    }
}
