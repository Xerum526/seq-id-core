package com.seq.id;

public interface Seq<T> {

	T newValue();

	T newValue(boolean saveBreakPoint);

	T currentValue();

	void initValue(T initValue);

	void saveBreakPoint(String key, T value) throws Exception;

	void loadBreakPoint(String key) throws Exception;

	T transferValueType(String value);

}
