package com.seq.id;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.seq.id.AbstractIdFactory.Sync;

public class Id {

	private Map<String, AbstractIdFactory<?, ?>> idMap = new HashMap<String, AbstractIdFactory<?, ?>>();

	private Map<String, IdSyncStrategy> idSyncStrategyMap = new HashMap<String, IdSyncStrategy>();

	private Map<AbstractIdSync<?, ?>, AbstractIdSyncListener> idSyncListenerMap = new HashMap<AbstractIdSync<?, ?>, AbstractIdSyncListener>();

	private IdSyncThreadPoolExecutor idSyncThreadPoolExecutor;

	private Id() {
		int corePoolSize = Integer.valueOf(IdConfiguration.get("idSyncThreadPoolExecutor.corePoolSize"));
		int maximumPoolSize = Integer.valueOf(IdConfiguration.get("idSyncThreadPoolExecutor.maximumPoolSize"));
		int keepAliveTime = Integer.valueOf(IdConfiguration.get("idSyncThreadPoolExecutor.keepAliveSeconds"));
		TimeUnit timeUnit = TimeUnit.SECONDS;
		int workQueueCapacity = Integer
				.valueOf(IdConfiguration.get("idSyncThreadPoolExecutor.workQueue.workQueueCapacity"));
		ThreadFactory threadFactory = IdSyncThreadPoolExecutor.initDefaultThreadFactory();
		RejectedExecutionHandler rejectedExecutionHandler = IdSyncThreadPoolExecutor
				.initDefaultRejectedExecutionHandler();
		this.idSyncThreadPoolExecutor = new IdSyncThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime,
				timeUnit, workQueueCapacity, threadFactory, rejectedExecutionHandler);
	}

	public static Id getInstance() {
		return Singleton.ID.id;
	}

	public static Id getInstance(Properties properties) throws Exception {
		IdConfiguration.loadExtConfig(properties);
		return Singleton.ID.id;
	}

	private static enum Singleton {
		ID;

		private Id id;

		private Singleton() {
			id = new Id();
		}

	}

	public Map<String, AbstractIdFactory<?, ?>> getIdMap() {
		return idMap;
	}

	public Map<String, IdSyncStrategy> getIdSyncStrategyMap() {
		return idSyncStrategyMap;
	}

	public Map<AbstractIdSync<?, ?>, AbstractIdSyncListener> getIdSyncListenerMap() {
		return idSyncListenerMap;
	}

	public IdSyncThreadPoolExecutor getIdSyncThreadPoolExecutor() {
		return idSyncThreadPoolExecutor;
	}

	@SuppressWarnings("unchecked")
	public <T, S> AbstractIdFactory<T, S> getFactory(String key) {
		return (AbstractIdFactory<T, S>) idMap.get(key);
	}

	@SuppressWarnings("unchecked")
	public <T, S> T newId(String key) {
		if (CommonUtil.isStrEmpty(key)) {
			return null;
		}
		return (T) getFactory(key).newId();
	}

	@SuppressWarnings("unchecked")
	public <T, S> T newIdByQueue(String key) throws Exception {
		if (CommonUtil.isStrEmpty(key)) {
			return null;
		}
		return (T) getFactory(key).newIdByQueue();
	}

	@SuppressWarnings("rawtypes")
	public synchronized <T, S> void registIdFactory(String key, AbstractIdFactory<T, S> idFactory) throws Exception {
		if (idMap.containsKey(key)) {
			return;
		}
		idMap.put(key, idFactory);
		Sync sync = idFactory.getSync();
		if (sync == null) {
			return;
		}
		idSyncThreadPoolExecutor.submit(sync);
	}

	public synchronized <T, S> void registIdSyncStrategy(final String key, final DefaultIdSyncStrategy strategy) {
		if (idSyncStrategyMap.containsKey(key)) {
			return;
		}
		if (!idMap.containsKey(key)) {
			return;
		}
		idSyncStrategyMap.put(key, strategy);
		strategy.execute(getFactory(key));
	}

	public synchronized <T, S> void registIdSyncListener(AbstractIdSync<T, S> sync, AbstractIdSyncListener listener) {
		if (idSyncListenerMap.containsKey(sync)) {
			return;
		}
		idSyncListenerMap.put(sync, listener);
		idSyncThreadPoolExecutor.submit(listener);
	}

}
