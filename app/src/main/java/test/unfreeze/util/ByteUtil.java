package test.unfreeze.util;

public class ByteUtil {

    public static final byte[] EMPTY_BYTES = new byte[0];

    protected static final char[] CHARS = "0123456789ABCDEF".toCharArray();

    public static boolean isNull(byte[] bArr) {
        return bArr == null || bArr.length == 0;
    }
    // TODO 含义待定
    public static byte[] buildCrcBytes(int i) {
        byte[] bytes = new byte[4];
        for (int i2 = 0; i2 < 4; i2++) {
            bytes[i2] = (byte) (i >>> (i2 * 8));
        }
        return bytes;
    }

    public static boolean isEquals(byte[] bytes, byte[] bytes1) {
        return compare(bytes, bytes1, Math.min(bytes.length, bytes1.length));
    }

    public static boolean compare(byte[] bytes, byte[] bytes1, int length) {
        if (bytes == bytes1) {
            return true;
        }
        if (bytes == null || bytes1 == null || bytes.length < length || bytes1.length < length) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (bytes[i] != bytes1[i]) {
                return false;
            }
        }
        return true;
    }
}
