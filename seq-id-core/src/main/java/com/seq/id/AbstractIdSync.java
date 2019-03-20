package com.seq.id;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public abstract class AbstractIdSync<T, S> implements IdSync<T>, Runnable {

	protected ArrayBlockingQueue<T> idQueue;

	protected AbstractIdFactory<T, S> idFactory;

	protected int queueCapacity;

	protected Integer appendThreshold;

	protected long getIdTimeout;

	protected TimeUnit timeUnit;

	protected Thread appendIdThread;

	protected volatile boolean queueHaveBeenAppended = false;

	protected volatile int queueSize;

	private volatile int newIdTimes = 0;

	public AbstractIdSync() {

	}

	public AbstractIdSync(AbstractIdFactory<T, S> idFactory) {
		this.idFactory = idFactory;
	}

	protected Thread initAppendIdThread(final String key, final ArrayBlockingQueue<T> idQueue,
			final AbstractSeq<S> seq) {
		appendIdThread = new Thread(new Runnable() {

			public void run() {
				try {
					seq.saveBreakPoint(key, seq.transferValueType(String.valueOf(idQueue.remainingCapacity())));
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				T t;
				for (;;) {
					try {
						t = newId(false);
						if (!idQueue.offer(t)) {
							seq.saveBreakPoint(key, null);
							queueHaveBeenAppended = true;
							LockSupport.park();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});
		return appendIdThread;
	}

	@Override
	public void append() {
		if (appendIdThread == null) {
			return;
		}
		appendIdThread.start();
	}

	@Override
	public void run() {
		append();
	}

	public int getNewIdTimes() {
		return newIdTimes;
	}

	public void setNewIdTimes(int newIdTimes) {
		this.newIdTimes = newIdTimes;
	}

}
