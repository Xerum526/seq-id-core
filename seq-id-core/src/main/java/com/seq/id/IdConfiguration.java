package com.seq.id;

import java.io.InputStream;
import java.util.Properties;

public class IdConfiguration {

	private static Properties properties;

	static {
		try {
			loadConfig();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void loadConfig() throws Exception {
		properties = new Properties();
		InputStream in = IdConfiguration.class.getResourceAsStream("../../../resources/seq-id-config.properties");
		if (in == null) {
			return;
		}
		properties.load(in);
	}

	public static void loadExtConfig(Properties extProperties) throws Exception {
		if (extProperties == null || extProperties.isEmpty()) {
			return;
		}
		properties.putAll(extProperties);
	}

	public static String get(String key) {
		return CommonUtil.trim(properties.getProperty(key));
	}

}
