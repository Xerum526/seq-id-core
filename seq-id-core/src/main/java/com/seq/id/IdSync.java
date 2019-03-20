package com.seq.id;

public interface IdSync<T> {

	void append();

	T newId(boolean saveBreakPoint) throws Exception;

	T newIdByQueue() throws Exception;

}
