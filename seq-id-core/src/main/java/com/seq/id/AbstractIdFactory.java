package com.seq.id;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public abstract class AbstractIdFactory<T, S> {

	protected AbstractSeq<S> seq;

	protected Sync sync;

	protected ConcurrentLinkedQueue<StandByIdSync> standByIdSyncQueue = new ConcurrentLinkedQueue<StandByIdSync>();

	protected String key;

	protected String idEigen;

	private AbstractIdFactory() {

	}

	public AbstractIdFactory(String idEigen, AbstractSeq<S> seq) {
		this();
		this.key = seq.getKey();
		if (CommonUtil.isStrEmpty(idEigen)) {
			idEigen = "";
		}
		this.idEigen = idEigen;
		this.seq = seq;
	}

	public AbstractIdFactory(String key, String idEigen, AbstractSeq<S> seq) {
		this();
		if (CommonUtil.isStrEmpty(key)) {
			key = AbstractSeq.DEFAULT_KEY;
		}
		this.key = key;
		if (CommonUtil.isStrEmpty(idEigen)) {
			idEigen = "";
		}
		this.idEigen = idEigen;
		this.seq = seq;
		this.seq.setKey(key);
	}

	public AbstractIdFactory(String idEigen, AbstractSeq<S> seq, int queueCapacity, int appendThreshold,
			long getIdTimeout, TimeUnit timeUnit) {
		this(idEigen, seq);
		sync = new Sync(this, queueCapacity, appendThreshold, getIdTimeout, timeUnit);
	}

	public AbstractIdFactory(String key, String idEigen, AbstractSeq<S> seq, int queueCapacity, int appendThreshold,
			long getIdTimeout, TimeUnit timeUnit) {
		this(key, idEigen, seq);
		sync = new Sync(this, queueCapacity, appendThreshold, getIdTimeout, timeUnit);
	}

	public AbstractSeq<S> getSeq() {
		return seq;
	}

	public Sync getSync() {
		return sync;
	}

	public ConcurrentLinkedQueue<StandByIdSync> getStandByIdSyncQueue() {
		return standByIdSyncQueue;
	}

	public String getKey() {
		return key;
	}

	public class Sync extends AbstractIdSync<T, S> {

		public Sync() {

		}

		Sync(AbstractIdFactory<T, S> idFactory, int queueCapacity, int appendThreshold, long getIdTimeout,
				TimeUnit timeUnit) {
			this.idFactory = idFactory;
			this.queueCapacity = queueCapacity;
			this.appendThreshold = appendThreshold;
			this.getIdTimeout = getIdTimeout;
			this.timeUnit = timeUnit;
			this.idQueue = new ArrayBlockingQueue<T>(queueCapacity);
			this.queueSize = idQueue.size();
			this.appendIdThread = initAppendIdThread(idFactory.getKey(), idQueue, idFactory.getSeq());
		}

		@Override
		public T newId(boolean saveBreakPoint) throws Exception {
			return idFactory.newId(saveBreakPoint);
		}

		public T newIdByQueue() throws Exception {
			queueSize = idQueue.size();
			if (queueSize < appendThreshold) {
				LockSupport.unpark(appendIdThread);
			}
			if (!queueHaveBeenAppended) {
				setNewIdTimes(getNewIdTimes() + 1);
				T t = newId(true);
				return t;
			}
			T t = idQueue.poll(getIdTimeout, timeUnit);
			if (t == null) {
				if (!standByIdSyncQueue.isEmpty()) {
					Iterator<StandByIdSync> iterator = standByIdSyncQueue.iterator();
					StandByIdSync _sync;
					while (iterator.hasNext()) {
						_sync = iterator.next();
						t = _sync.newIdByQueue();
						if (t != null) {
							return t;
						}
					}
				}
				queueHaveBeenAppended = false;
				setNewIdTimes(getNewIdTimes() + 1);
				t = newId(true);
				return t;
			}
			return t;
		}

		public ArrayBlockingQueue<T> getIdQueue() {
			return idQueue;
		}

		public void setIdQueue(ArrayBlockingQueue<T> idQueue) {
			this.idQueue = idQueue;
		}

		public int getQueueCapacity() {
			return queueCapacity;
		}

	}

	public class StandByIdSync extends Sync {

		public StandByIdSync(AbstractIdFactory<T, S> idFactory) {
			this.idFactory = idFactory;
			this.appendThreshold = idFactory.sync.appendThreshold;
			this.getIdTimeout = idFactory.sync.getIdTimeout;
			this.timeUnit = idFactory.sync.timeUnit;
			this.idQueue = new ArrayBlockingQueue<T>(idFactory.sync.getQueueCapacity());
			this.appendIdThread = initAppendIdThread(idFactory.key, idQueue, (AbstractSeq<S>) idFactory.seq);
		}

		@Override
		public T newIdByQueue() throws Exception {
			queueSize = idQueue.size();
			if (queueSize < appendThreshold) {
				LockSupport.unpark(appendIdThread);
			}
			T t = idQueue.poll(getIdTimeout, timeUnit);
			if (t == null) {
				queueHaveBeenAppended = false;
			}
			return t;
		}
	}

	protected T newIdByQueue() throws Exception {
		return sync.newIdByQueue();
	}

	protected abstract T newId();

	protected abstract T newId(boolean saveBreakPoint);

	protected abstract List<T> batchNewId(int count);

}
