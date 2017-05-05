package mipt.algo.reversi.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
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
            for (int i = 0; i < Integer.BYTES; i++) {
                int res = stream.read();
                if (res < 0) {
                    res = 0;
                    System.out.println("err");
                }
                buffer[i] = (byte)res;
            }
            result = decodeInteger(buffer);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
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
        String[] args = string.split(" ");

        if (args.length != 3 || !args[0].equals("move")) {
            return null;
        }

        Integer x;
        Integer y;
        try {
            x = Integer.parseInt(args[1]);
            y = args[2].charAt(0) - 'a';
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
        byte[] arr = encode(string.length());
        byte[] str = string.getBytes();
        byte[] array = ByteBuffer.allocate(str.length + arr.length).put(arr).put(str).array();
        return array;
    }
}
