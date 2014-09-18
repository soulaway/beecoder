package com.github.soulaway.beecoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit test for BeeCoder.
 */
public class BeeCoderTest extends Assert {

	@Test(dataProvider = "getSerializedDataExpectedString", dataProviderClass = BcodeTestData.class)
	public void testEncodeOk(ObjectInputStream ois, String expected)
			throws Exception {
		OutputStream bos = new ByteArrayOutputStream();
		BeeCoder.INSTANCE.encodeStream(ois, bos);
		assertEquals(bos.toString(), expected);
	}

	@Test(dataProvider = "getEncodedStringExpectedList", dataProviderClass = BcodeTestData.class)
	public void testDecodeOk(String encoded, List<Object> expected)
			throws Exception {
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(
				BcodeTestData.TEST_BC_BUF_FILE_URI));
		BeeCoder.INSTANCE.decodeStream(
				new ByteArrayInputStream(encoded.getBytes()), oos);
		assertEquals(BcodeTestData.readDecodedFromBuf(BcodeTestData.TEST_BC_BUF_FILE_URI), expected);
	}

	 @Test(dataProvider = "getNonBencodableDictionary", dataProviderClass = BcodeTestData.class, 
			 expectedExceptions = IllegalArgumentException.class, 
			 expectedExceptionsMessageRegExp = "Bencoded dictionary key is not String type*.*")
	 public void testFailEncodeDict(ObjectInputStream ois) throws ClassNotFoundException, IOException{
		OutputStream bos = new ByteArrayOutputStream();
		BeeCoder.INSTANCE.encodeStream(ois, bos);
	 }
	 
	 @Test(dataProvider = "getNonBencodableList", dataProviderClass = BcodeTestData.class,
			 expectedExceptions = IllegalArgumentException.class, 
			 expectedExceptionsMessageRegExp = "The type of the encodable object isn't Bencodable*.*")
	 public void testFailEncodeList(ObjectInputStream ois) throws ClassNotFoundException, IOException{
		OutputStream bos = new ByteArrayOutputStream();
		BeeCoder.INSTANCE.encodeStream(ois, bos);
	 }

	@Test(dataProvider = "getEncodedBisFailedString", dataProviderClass = BcodeTestData.class,
			expectedExceptions = IOException.class, 
			expectedExceptionsMessageRegExp = "unexpected end of stream while encoding*.*")
	public void testDecodeFailString(ByteArrayInputStream bis) throws FileNotFoundException, IOException{
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(
				BcodeTestData.TEST_BC_BUF_FILE_URI));
		BeeCoder.INSTANCE.decodeStream(bis, oos);
	 }

	@Test(dataProvider = "getEncodedBisFailedInt", dataProviderClass = BcodeTestData.class,
			expectedExceptions = IOException.class, 
			expectedExceptionsMessageRegExp = "Integer value contains unexpected character*.*")
	public void testDecodeFailInt(ByteArrayInputStream bis) throws FileNotFoundException, IOException{
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(
				BcodeTestData.TEST_BC_BUF_FILE_URI));
		BeeCoder.INSTANCE.decodeStream(bis, oos);
	 }

	@Test(dataProvider = "getEncodedBisFailedList", dataProviderClass = BcodeTestData.class,
			expectedExceptions = IOException.class, 
			expectedExceptionsMessageRegExp = "unexpected end of stream while encoding List")
	public void testDecodeFailList(ByteArrayInputStream bis) throws FileNotFoundException, IOException{
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(
				BcodeTestData.TEST_BC_BUF_FILE_URI));
		BeeCoder.INSTANCE.decodeStream(bis, oos);
	 }

	@Test(dataProvider = "getEncodedBisFailedDict", dataProviderClass = BcodeTestData.class,
			expectedExceptions = IOException.class, 
			expectedExceptionsMessageRegExp = "Dict value contains unexpected character*.*")
	public void testDecodeFailDict(ByteArrayInputStream bis) throws FileNotFoundException, IOException{
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(
				BcodeTestData.TEST_BC_BUF_FILE_URI));
		BeeCoder.INSTANCE.decodeStream(bis, oos);
	 }

}
