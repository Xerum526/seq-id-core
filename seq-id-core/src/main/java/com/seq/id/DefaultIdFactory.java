package com.seq.id;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.seq.id.AbstractIdFactory;
import com.seq.id.AbstractSeq;

public class DefaultIdFactory extends AbstractIdFactory<String, Long> {

	private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");

	public DefaultIdFactory(String idEigen, AbstractSeq<Long> seq) {
		super(idEigen, seq);
	}

	public DefaultIdFactory(String key, String idEigen, AbstractSeq<Long> seq) {
		super(key, idEigen, seq);
	}

	public DefaultIdFactory(String idEigen, AbstractSeq<Long> seq, int capacity, int appendThreshold, long getIdTimeout,
			TimeUnit timeUnit) {
		super(idEigen, seq, capacity, appendThreshold, getIdTimeout, timeUnit);
	}

	public DefaultIdFactory(String key, String idEigen, AbstractSeq<Long> seq, int capacity, int appendThreshold,
			long getIdTimeout, TimeUnit timeUnit) {
		super(key, idEigen, seq, capacity, appendThreshold, getIdTimeout, timeUnit);
	}

	@Override
	protected String newId() {
		return String.format("%s%s%s", idEigen, sdf.format(new Date()), seq.newValue());
	}

	@Override
	protected String newId(boolean saveBreakPoint) {
		return String.format("%s%s%s", idEigen, sdf.format(new Date()), seq.newValue(saveBreakPoint));
	}

	@Override
	protected List<String> batchNewId(int count) {
		List<String> list = new ArrayList<String>(count);
		for (int i = 0; i < count; i++) {
			list.add(newId());
		}
		return list;
	}

}
