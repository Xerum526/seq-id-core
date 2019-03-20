package com.seq.id;

public class CommonUtil {

	public static boolean isStrEmpty(String s) {
		if (s == null) {
			return true;
		}
		s = s.replace(" ", "").replace("\t", "");
		return s.equals("");
	}

	public static String trim(String s) {
		if (isStrEmpty(s)) {
			return "";
		}
		return s.replace(" ", "").replace("\t", "");
	}

}
