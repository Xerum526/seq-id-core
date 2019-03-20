package com.seq.id;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

public class DefaultAtomicLongSeq extends AbstractSeq<Long> {

	private AtomicLong atomicLong;

	private String breakPointFileBasePath;

	private String breakPointFilePath;

	private JedisCluster jedis;

	public DefaultAtomicLongSeq(String key, long initValue) throws Exception {
		super(key, initValue);
		this.key = key;
		this.atomicLong = new AtomicLong(initValue);
		this.breakPointFileBasePath = IdConfiguration.get(key + ".breakPointFileBasePath");
		if (CommonUtil.isStrEmpty(this.breakPointFileBasePath)) {
			this.breakPointFileBasePath = IdConfiguration.get(DEFAULT_KEY + ".breakPointFileBasePath");
		}
		this.breakPointFilePath = breakPointFileBasePath + "/" + key + BREAKPOINT_FILE_SUFFIX;
		Set<HostAndPort> nodes = new HashSet<HostAndPort>();
		String[] redisNodesStrAry = IdConfiguration.get("seq-id.redis.cluster.nodes").split(",");
		String[] hostAndPortStrAry;
		for (String hostAndPortStr : redisNodesStrAry) {
			hostAndPortStrAry = hostAndPortStr.split(":");
			nodes.add(new HostAndPort(hostAndPortStrAry[0], Integer.parseInt(hostAndPortStrAry[1])));
		}
		this.jedis = new JedisCluster(nodes);
		if (atomicLong.get() == 0L) {
			loadBreakPoint(key);
		}
	}

	@Override
	public Long newValue() {
		long value = atomicLong.getAndIncrement();
		saveBreakPoint(key, null);
		return value;
	}

	@Override
	public Long newValue(boolean saveBreakPoint) {
		if (saveBreakPoint)
			return newValue();
		else
			return atomicLong.getAndIncrement();
	}

	@Override
	public Long currentValue() {
		return atomicLong.get();
	}

	@Override
	public void initValue(Long initValue) {
		atomicLong.set(initValue);
	}

	@Override
	public synchronized void saveBreakPoint(String key, Long value) {
		long breakPoint = currentValue();
		if (value != null) {
			breakPoint += value;
		}
		try {
			jedis.set(key + BREAKPOINT_FILE_SUFFIX, String.valueOf(breakPoint));
		} catch (Exception e) {
			e.printStackTrace();
		}
		File f = new File(breakPointFilePath);
		PrintWriter pw;
		try {
			pw = new PrintWriter(f);
			pw.print(breakPoint);
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void loadBreakPoint(String key) throws Exception {
		Long initValue1 = 0L;
		Long initValue2 = 0L;
		String initValueStr1 = jedis.get(key + BREAKPOINT_FILE_SUFFIX);
		if (!CommonUtil.isStrEmpty(initValueStr1)) {
			initValue1 = transferValueType(initValueStr1);
		}
		File f = new File(breakPointFilePath);
		if (f.exists() && f.isFile()) {
			InputStreamReader in = new InputStreamReader(new FileInputStream(f));
			BufferedReader br = new BufferedReader(in);
			String initValueStr2 = br.readLine();
			br.close();
			if (!CommonUtil.isStrEmpty(initValueStr2)) {
				initValue2 = transferValueType(initValueStr2);
			}
		}
		initValue(Math.max(initValue1, initValue2));
	}

	@Override
	public Long transferValueType(String value) {
		return Long.valueOf(value);
	}

}
