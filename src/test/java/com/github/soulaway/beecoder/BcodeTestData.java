package com.github.soulaway.beecoder;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.AbstractMap;
import java.util.LinkedList;
import java.util.List;

import org.testng.annotations.DataProvider;

public class BcodeTestData {

	private static List<Object> bData;
	private static final String TEST_BC_CODE = "0:li777e6:StRingi-42e6:!@$#%^"
			+ "i0eei2147483647ed4:key112:string value4:key2i42e"
			+ "4:key3li777e6:StRingi-42e6:!@$#%^i0eeei-2147483648e";
	public static final String TEST_BC_BUF_FILE_URI = "test";

	public static List<Object> readDecodedFromBuf(String bufUri) throws ClassNotFoundException, IOException{
		@SuppressWarnings("resource")
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(bufUri)); 
		List<Object> list = new LinkedList<Object>();
		while (true) {  
			try {
				list.add(ois.readObject());
			} catch (EOFException e) {
				return list;
			}  
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static List<Object> buildData() {
		List<Object> bSimpleList = new LinkedList<Object>();
		bSimpleList.add(777);
		bSimpleList.add("StRing");
		bSimpleList.add(-42);
		bSimpleList.add("!@$#%^");
		bSimpleList.add(0);

		List<Object> bdict = new LinkedList<Object>();
		bdict.add(new AbstractMap.SimpleEntry("key1", "string value"));
		bdict.add(new AbstractMap.SimpleEntry("key2", 42));
		bdict.add(new AbstractMap.SimpleEntry("key3", bSimpleList));

		// todo test for empty string
		// todo test for non UTF-8 encoding
		List<Object> d = new LinkedList<Object>();
		d.add("");
		d.add(bSimpleList);
		d.add(Integer.MAX_VALUE);
		d.add(bdict);
		d.add(Integer.MIN_VALUE);
		return d;
	}

	private static List<Object> getData() {
		if (bData == null) {
			bData = buildData();
		}
		return bData;
	}

	private static void write(Object o, ObjectOutputStream oos) {
		try {
			oos.writeObject(o);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@DataProvider(name = "getSerializedDataExpectedString")
	public static Object[][] getSerializedDataExpectedString() throws FileNotFoundException,
			IOException {
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(
				TEST_BC_BUF_FILE_URI));
		getData().stream().forEach(o -> write(o, oos));
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
				TEST_BC_BUF_FILE_URI));
		return new Object[][] {{ois, TEST_BC_CODE}};
	}

	@DataProvider(name = "getEncodedStringExpectedList")
	public static Object[][] getEncodedStringExpectedList() {
		return new Object[][] {{TEST_BC_CODE, getData()}};
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@DataProvider(name = "getNonBencodableDictionary")
	public static Object[][] getNonBencodableDictionary() throws FileNotFoundException, IOException {
		List<Object> list = new LinkedList<Object>();
		list.add(new AbstractMap.SimpleEntry("This strange","dictionary hasnt next String key"));
		list.add(new AbstractMap.SimpleEntry(666, "fail"));
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(
				TEST_BC_BUF_FILE_URI));
		write(list, oos);
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
				TEST_BC_BUF_FILE_URI));
		return new Object[][] {{ois}};		
	}

	@DataProvider(name = "getNonBencodableList")
	public static Object[][] getNonBencodableList() throws FileNotFoundException, IOException {
		List<Object> list = new LinkedList<Object>();
		list.add(new Exception());
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(
				TEST_BC_BUF_FILE_URI));
		write(list, oos);
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
				TEST_BC_BUF_FILE_URI));
		return new Object[][] {{ois}};		
	}
	
	@DataProvider(name = "getEncodedBisFailedString")
	public static Object[][] getEncodedBisFailedString() {
		return new Object[][] { { new ByteArrayInputStream("3:12".getBytes()) }, };
	}

	@DataProvider(name = "getEncodedBisFailedInt")
	public static Object[][] getEncodedBisFailedInt() {
		return new Object[][] { { new ByteArrayInputStream("i5O5e".getBytes()) }, };
	}

	@DataProvider(name = "getEncodedBisFailedList")
	public static Object[][] getEncodedBisFailedList() {
		return new Object[][] { { new ByteArrayInputStream("l3:qwe".getBytes()) }, };
	}

	@DataProvider(name = "getEncodedBisFailedDict")
	public static Object[][] getEncodedBisFailedDict() {
		return new Object[][] { { new ByteArrayInputStream(
				"d2:123:123i5ee".getBytes()) }, };
	}
}