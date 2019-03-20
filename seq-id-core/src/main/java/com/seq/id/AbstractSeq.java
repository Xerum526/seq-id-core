package com.seq.id;

public abstract class AbstractSeq<T> implements Seq<T> {

	protected static final String BREAKPOINT_FILE_SUFFIX = ".seq";

	protected String key;

	protected long initValue;

	protected static final String DEFAULT_KEY = "default";

	AbstractSeq(long initValue) {
		this.initValue = initValue;
	}

	AbstractSeq(String key, long initValue) {
		if (CommonUtil.isStrEmpty(key)) {
			key = DEFAULT_KEY;
		}
		this.key = key;
		this.initValue = initValue;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public long getInitValue() {
		return initValue;
	}

	public void setInitValue(long initValue) {
		this.initValue = initValue;
	}

}
