package ghirl.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class SerializationUtil {

	public static void serializeInt(int allbits, ByteArrayOutputStream baos, int bytesPerInt) {
		for (int i=0; i<bytesPerInt; i++) {
			int shift = Byte.SIZE*i;
			baos.write(allbits >> shift);
		}
	}
	public static int deserializeInt(ByteArrayInputStream bais, int bytesPerInt) {
		int r, rebits = 0;
		for (int i=0; i<bytesPerInt && ((r = bais.read()) != -1); i++) {
			int shift = Byte.SIZE*i;
			rebits = rebits | (r << shift);
		}
		return rebits;
	}
}
