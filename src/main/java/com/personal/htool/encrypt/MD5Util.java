package com.personal.htool.encrypt;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 主要用于验证数据的完整性，可以为数据创建“数字指纹”(散列值),结果具有唯一性 并且计算结果不可逆
 * 
 * @author long
 * 
 */
public class MD5Util {
	public static String encrypt(String s) throws NoSuchAlgorithmException {
		MessageDigest messageDigest = MessageDigest.getInstance("MD5");
		// byte[] buffer = new byte[1024];
		byte[] btInput = s.getBytes();
		// 使用指定的字节更新摘要
		messageDigest.update(btInput);
		// 获得密文
		byte[] result = messageDigest.digest();
		// 把密文转换成十六进制的字符串形式
		return bytes2HexString(result);
	}

	/**
	 * 把字节数组转换为16进制的形式
	 * 
	 * @param b
	 * @return
	 */
	public static String bytes2HexString(byte[] b) {
		String ret = "";
		for (int i = 0; i < b.length; i++) {
			String hex = Integer.toHexString(b[i] & 0xFF);
			if (hex.length() == 1) {
				hex = '0' + hex;
			}
			ret += hex.toUpperCase();
		}
		return ret;
	}
	/**
	 * MD5 32位加密大写
	 * @param sourceStr
	 * @return
	 */
	public static String MD5To32UpperCase(String sourceStr) {
		String result = "";
		try {
		    MessageDigest md = MessageDigest.getInstance("MD5");
		    md.update(sourceStr.getBytes());
		    byte b[] = md.digest();
		    int i;
		    StringBuffer buf = new StringBuffer("");
		    for (int offset = 0; offset < b.length; offset++) {
		        i = b[offset];
		        if (i < 0)
		            i += 256;
		        if (i < 16)
		            buf.append("0");
		        buf.append(Integer.toHexString(i));
		    }
		    result = buf.toString();
		   } catch (NoSuchAlgorithmException e) {
		        System.out.println(e);
		   }
		   return result.toUpperCase();
	 }
	
	/**
	 * MD5 16位加密大写
	 * @param sourceStr
	 * @return
	 */
	public static String MD5To16UpperCase(String sourceStr) {
		String result = "";
		try {
		    MessageDigest md = MessageDigest.getInstance("MD5");
		    md.update(sourceStr.getBytes());
		    byte b[] = md.digest();
		    int i;
		    StringBuffer buf = new StringBuffer("");
		    for (int offset = 0; offset < b.length; offset++) {
		        i = b[offset];
		        if (i < 0)
		            i += 256;
		        if (i < 16)
		            buf.append("0");
		        buf.append(Integer.toHexString(i));
		    }
		    result = buf.toString();
		   } catch (NoSuchAlgorithmException e) {
		        System.out.println(e);
		   }
		   return result.substring(8, 24).toUpperCase();
	 }
	    
}
