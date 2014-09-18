package com.github.soulaway.beecoder;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.AbstractMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

/**
 * Presents the file stream parser that de/encoding "B-encode" data format. That
 * used by the peer-to-peer file sharing system (like BitTorrent) for storing
 * and transmitting loosely structured data.
 * 
 * @author Dmitry G. Soloviev (soulaway)
 * @see <a href="http://en.wikipedia.org/wiki/Bencode">more about Bencode</a>
 * @see <a href="https://github.com/soulaway/beecoder">sources</a>
 */

public enum BeeCoder {

	INSTANCE;

	private static final char BC_PREFIX_INT = 'i';
	private static final char BC_SUFIX_NEGATIVE = '-';
	private static final char BC_POSTFIX_NONSTR = 'e';
	private static final char BC_DELEMETER_STR = ':';
	private static final char BC_PREFIX_ARR = 'l';
	private static final char BC_PREFIX_DIC = 'd';
	private static final String BC_MSG_EXCEPTION_WRONG_CHAR = " value contains unexpected character - ";
	private static final String BC_MSG_EXCEPTION_STREAM_END = "unexpected end of stream while encoding ";

	/**
	 * Provides the encoding of the Bencode compatible java objects, by reading
	 * them from the ObjectInputStream and writing encoded to
	 * ByteArrayOutputStream.
	 * 
	 * @param ois
	 *            - ObjectInputStream to read from
	 * @param bos
	 *            - ByteArrayOutputStream to write to
	 * @throws ClassNotFoundException
	 *             when the Class of a serialized object cannot be found
	 * @throws IOException
	 *             when data is malformed
	 */
	public void encodeStream(ObjectInputStream ois, OutputStream bos)
			throws ClassNotFoundException, IOException {
		Object obj;
		try {
			while ((obj = ois.readObject()) != null) {
				Utils.encodeObject(obj, bos);
			}
		} catch (EOFException e) {
			ois.close();
			bos.flush();
			bos.close();
		}
	}

	/**
	 * Provides the serialization of the Bencode compatible java objects by
	 * encoding the ByteArrayInputStream and writing to ObjectOutputStream.
	 * 
	 * @param bis
	 *            - ByteArrayInputStream to read from
	 * @param oos
	 *            - ObjectOutputStream to write to
	 * @throws IOException
	 *             when data is malformed
	 */
	public void decodeStream(InputStream bis, ObjectOutputStream oos)
			throws IOException {
		while (true) {
			Object obj = Utils.decodeObject(bis);
			if (obj == null) {
				oos.close();
				break;
			} else {
				oos.writeObject(obj);
			}
		}
	}

	/**
	 * @author Dmitry G. Soloviev
	 *
	 */
	private static class Utils {
		
		/*** encoding utils ***/

		/**
		 * Provides the recursive deserialization of the Bencode compatible
		 * objects, writing it to byte output stream.
		 * 
		 * @param obj
		 *            - object that needs to be encoded
		 * @param bos
		 *            - ByteArrayInputStream where to write to
		 */
		public static void encodeObject(Object obj, OutputStream bos) {

			try {
				if (obj instanceof String) {
					String s = (String) obj;
					bos.write(String.valueOf(s.length()).getBytes());
					bos.write(BC_DELEMETER_STR);
					bos.write(s.getBytes());
				} else if (obj instanceof Integer) {
					Integer i = (Integer) obj;
					bos.write(BC_PREFIX_INT);
					bos.write(i.toString().getBytes());
					bos.write(BC_POSTFIX_NONSTR);
				} else if (obj instanceof LinkedList) {
					encodeCollection(obj, bos);
				} else {
					throw new IllegalArgumentException("The type of the encodable object isn't Bencodable: " + obj.toString());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Deserialize the Bencoded collection object content to bytes.
		 * 
		 * @param obj
		 *            - List or Dictionary with Bencoded objects inside
		 * @param bos
		 *            - ByteArrayInputStream where to write to
		 * @throws IOException
		 *             when data is malformed
		 */
		public static void encodeCollection(Object obj, OutputStream bos)
				throws IOException {
			@SuppressWarnings("unchecked")
			LinkedList<Object> list = (LinkedList<Object>) obj;
			Object o = list.get(0);
			if (o instanceof Entry) {
				bos.write(BC_PREFIX_DIC);
				@SuppressWarnings("unchecked")
				LinkedList<Entry<Object, Object>> dict = (LinkedList<Entry<Object, Object>>) obj;
				dict.stream().forEach(e -> encodeDictEntry(e, bos));
			} else {
				bos.write(BC_PREFIX_ARR);
				list.stream().forEach(e -> encodeObject(e, bos));
			}
			bos.write(BC_POSTFIX_NONSTR);
		}

		/**
		 * Deserialize the Bencoded dictionary entry to bytes.
		 * 
		 * @param e
		 *            - Bencoded dictionary entry
		 * @param bos
		 *            - ByteArrayInputStream where to write to
		 */
		public static void encodeDictEntry(Entry<Object, Object> e,
				OutputStream bos) {
			if (e.getKey() instanceof String){
				encodeObject(e.getKey(), bos);
				encodeObject(e.getValue(), bos);
			} else {
				throw new IllegalArgumentException("Bencoded dictionary key is not String type " + e.getKey());				
			}
		}

		/*** decoding utils ***/

		/**
		 * Provides the recursive serialization of the Bencode compatible
		 * objects from encoded byte stream.
		 * 
		 * @param bis
		 *            - the ByteArrayInputStream where to read from
		 * @return
		 * @throws IOException
		 *             when data is malformed
		 */
		public static Object decodeObject(InputStream bis) throws IOException {
			char ch = (char) bis.read();
			if (Character.isDigit(ch)) {
				int length = decodeStringLength(ch, bis);
				String s = decodeString(bis, length);
				return s;
			} else if (ch == BC_PREFIX_INT) {
				Integer i = decodeInt(bis);
				return i;
			} else if (ch == BC_PREFIX_ARR) {
				List<Object> array = decodeList(bis);
				return array;
			} else if (ch == BC_PREFIX_DIC) {
				List<Entry<String, Object>> dict = decodeDict(bis);
				return dict;
			} else if ((byte) ch == -1) {
				// successful stream end
				bis.close();
				return null;
			} else {
				throw new IOException("Object" + BC_MSG_EXCEPTION_WRONG_CHAR
						+ ch + " Bytes left " + bis.available());
			}
		}

		/**
		 * Serialize the Bencode compatible String object from encoded byte
		 * stream. It is supposed that the the String length is already decoded,
		 * and transferred as 'startedFrom' parameter.
		 * 
		 * @param length
		 *            - the first digit of the String length
		 * @param bis
		 *            - the ByteArrayInputStream where to read from
		 * @return
		 * @throws IOException
		 *             when data is malformed
		 */
		private static String decodeString(InputStream bis, int length)
				throws IOException {
			char[] value = new char[length];
			int b;
			for (int i = 0; i < length; i++) {
				b = bis.read();
				if (b == -1) {
					throw new IOException(BC_MSG_EXCEPTION_STREAM_END
							+ "String");
				} else {
					value[i] = (char) b;
				}
			}
			String s = new String(value);
			return s;
		}

		/**
		 * Returns the length of Bencode compatible String object from encoded
		 * byte stream. It is supposed that the the first digit of the String
		 * length is already read and transferred as 'startedFrom' parameter
		 * 
		 * @param startedFrom
		 *            - the first digit of the String length
		 * @param bis
		 *            - the ByteArrayInputStream where to read from
		 * @return
		 * @throws IOException
		 *             when data is malformed
		 */
		private static int decodeStringLength(char startedFrom, InputStream bis)
				throws IOException {
			int i = Character.getNumericValue(startedFrom);
			char ch;
			while (true) {
				ch = (char) bis.read();
				if (ch == BC_DELEMETER_STR) {
					break;
				} else if ((byte) ch == -1) {
					throw new IOException(BC_MSG_EXCEPTION_STREAM_END
							+ "String length");
				} else if (!Character.isDigit(ch)) {
					throw new IOException("String"
							+ BC_MSG_EXCEPTION_WRONG_CHAR + ch + " Bytes left "
							+ bis.available());
				} else {
					i = i * 10 + Character.getNumericValue(ch);
				}
			}
			return i;
		}

		/**
		 * Serialize the Bencode compatible Integer object from encoded byte
		 * stream It is supposed that the int indicator symbol 'i' is already
		 * read
		 * 
		 * @param bis
		 *            - the ByteArrayInputStream where to read from
		 * @return - the Integer java Object
		 * @throws IOException
		 *             when data is malformed
		 */
		private static Integer decodeInt(InputStream bis) throws IOException {
			int neg = 1;
			int i = 0;
			char ch;
			while (true) {
				ch = (char) bis.read();
				if (Character.isDigit(ch)) {
					i = i * 10 + Character.getNumericValue(ch);
				} else if (ch == BC_SUFIX_NEGATIVE && i == 0) {
					neg = neg * -1;
				} else if (ch == BC_POSTFIX_NONSTR) {
					break;
				} else if ((byte) ch == -1) {
					throw new IOException(BC_MSG_EXCEPTION_STREAM_END
							+ "Integer");
				} else {
					throw new IOException("Integer"
							+ BC_MSG_EXCEPTION_WRONG_CHAR + ch + " Bytes left "
							+ bis.available());
				}
			}
			return new Integer(i * neg);
		}

		/**
		 * Serialize the Bencode compatible List object from encoded byte
		 * stream. It is supposed that the list indicator symbol 'l' is already
		 * read
		 * 
		 * @param bis
		 *            - the ByteArrayInputStream where to read from
		 * @return - the LinkedList as a list of BCodeble objects
		 * @throws IOException
		 *             when data is malformed
		 */
		private static List<Object> decodeList(InputStream bis)
				throws IOException {
			List<Object> list = new LinkedList<Object>();
			char ch;
			while (true) {
				ch = (char) bis.read();
				if (Character.isDigit(ch)) {
					int length = decodeStringLength(ch, bis);
					String s = decodeString(bis, length);
					list.add(s);
				} else if (ch == BC_PREFIX_INT) {
					Integer i = decodeInt(bis);
					list.add(i);
				} else if (ch == BC_PREFIX_ARR) {
					list.add(decodeList(bis));
				} else if (ch == BC_PREFIX_DIC) {
					list.add(decodeDict(bis));
				} else if (ch == BC_POSTFIX_NONSTR) {
					break;
				} else if ((byte) ch == -1) {
					throw new IOException(BC_MSG_EXCEPTION_STREAM_END + "List");
				} else {
					throw new IOException("List" + BC_MSG_EXCEPTION_WRONG_CHAR
							+ ch + " Bytes left " + bis.available());
				}
			}
			return list;
		}

		/**
		 * Serialize the Bencode compatible Dictionary object from encoded byte
		 * stream. It is supposed that the dictionary indicator symbol 'd' is
		 * already read
		 * 
		 * @param bis
		 *            - the ByteArrayInputStream where to read from
		 * @return - the LinkedList as a dictionary of BCodeble Entries
		 * @throws IOException
		 *             when data is malformed
		 */
		private static List<Entry<String, Object>> decodeDict(InputStream bis)
				throws IOException {
			List<Entry<String, Object>> list = new LinkedList<Entry<String, Object>>();
			char ch;
			while (true) {
				ch = (char) bis.read();
				if (Character.isDigit(ch)) {
					int length = decodeStringLength(ch, bis);
					String s = decodeString(bis, length);
					Object o = decodeObject(bis);
					Entry<String, Object> e = new AbstractMap.SimpleEntry<String, Object>(
							s, o);
					list.add(e);
				} else if (ch == BC_POSTFIX_NONSTR) {
					break;
				} else if ((byte) ch == -1) {
					throw new IOException(BC_MSG_EXCEPTION_STREAM_END + "Dict");
				} else {
					throw new IOException("Dict" + BC_MSG_EXCEPTION_WRONG_CHAR
							+ ch + " Bytes left " + bis.available());
				}
			}
			return list;
		}
	}
}
