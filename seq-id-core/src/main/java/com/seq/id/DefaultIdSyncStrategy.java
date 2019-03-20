package com.seq.id;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import com.seq.id.AbstractIdFactory.StandByIdSync;

public class DefaultIdSyncStrategy implements IdSyncStrategy {

	protected String key;

	protected Map<String, Object> paramMap;

	protected AtomicBoolean queueHaveBeenAppended;

	protected int standbyIdSyncQueueSizeMax;

	public <T, S> DefaultIdSyncStrategy(String key, Map<String, Object> paramMap) {
		this.key = key;
		if (paramMap == null) {
			paramMap = new HashMap<String, Object>();
		}
		paramMap.put("startMillisecond", System.currentTimeMillis());
		this.paramMap = paramMap;
		this.queueHaveBeenAppended = new AtomicBoolean(false);
		String standbyIdSyncQueueSizeMaxStr = IdConfiguration.get(key + ".standbyIdSyncQueueSize.max");
		if (CommonUtil.isStrEmpty(standbyIdSyncQueueSizeMaxStr)) {
			standbyIdSyncQueueSizeMaxStr = IdConfiguration.get(AbstractSeq.DEFAULT_KEY + ".standbyIdSyncQueueSize.max");
		}
		this.standbyIdSyncQueueSizeMax = Integer.parseInt(standbyIdSyncQueueSizeMaxStr);
	}

	@Override
	public <T, S> int evaluate(AbstractIdFactory<T, S> idFactory) {
		if (standbyIdSyncQueueSizeMax == 0) {
			return 0;
		}
		long startMillisecond = (Long) paramMap.get("startMillisecond");
		long currentMillisecond = System.currentTimeMillis();
		AbstractIdSync<T, S> sync = idFactory.getSync();
		long newIdTimes = sync.getNewIdTimes();
		ConcurrentLinkedQueue<AbstractIdFactory<T, S>.StandByIdSync> standByIdSyncQueue = idFactory.standByIdSyncQueue;
		Iterator<AbstractIdFactory<T, S>.StandByIdSync> iterator = null;
		if (!standByIdSyncQueue.isEmpty()) {
			iterator = standByIdSyncQueue.iterator();
			while (iterator.hasNext()) {
				newIdTimes += iterator.next().getNewIdTimes();
			}
		}
		BigDecimal intervalSeconds = BigDecimal.valueOf(currentMillisecond - startMillisecond)
				.divide(BigDecimal.valueOf(1000), RoundingMode.HALF_UP);
		if (sync.queueHaveBeenAppended) {
			queueHaveBeenAppended.compareAndSet(false, sync.queueHaveBeenAppended);
		}
		if (intervalSeconds.compareTo(BigDecimal.valueOf(60)) <= 0 && !queueHaveBeenAppended.get()) {
			sync.setNewIdTimes(0);
			return 0;
		}
		int score = BigDecimal.valueOf(newIdTimes).divide(intervalSeconds, RoundingMode.HALF_UP).intValue();
		if (score >= 1) {
			if (standByIdSyncQueue.size() < standbyIdSyncQueueSizeMax) {
				score = 1;
			} else {
				score = 0;
			}
		} else {
			if (intervalSeconds.compareTo(BigDecimal.valueOf(30 * 60)) > 0) {
				score = -1;
			} else {
				score = 0;
			}
		}
		sync.setNewIdTimes(0);
		if (!standByIdSyncQueue.isEmpty()) {
			while (iterator.hasNext()) {
				iterator.next().setNewIdTimes(0);
			}
		}
		return score;
	}

	@Override
	public <T, S> void execute(final AbstractIdFactory<T, S> idFactory) {
		final Id id = Id.getInstance();
		final IdSyncThreadPoolExecutor idSyncThreadPoolExecutor = id.getIdSyncThreadPoolExecutor();
		idSyncThreadPoolExecutor.submit(new Runnable() {

			@SuppressWarnings({ "rawtypes", "unchecked" })
			@Override
			public void run() {
				Map<String, Object> paramMap = new HashMap<String, Object>();
				paramMap.put("startMillisecond", System.currentTimeMillis());
				int score;
				for (;;) {
					score = evaluate(idFactory);
					switch (score) {
					case 1:
						StandByIdSync standByIdSync = idFactory.new StandByIdSync(idFactory);
						idFactory.standByIdSyncQueue.offer(standByIdSync);
						id.registIdSyncListener(standByIdSync, new IdSyncAppendQueueListener(standByIdSync));
						idSyncThreadPoolExecutor.submit(standByIdSync);
						break;
					case 0:
						break;
					case -1:
						ConcurrentLinkedQueue<AbstractIdFactory<T, S>.StandByIdSync> standByIdSyncQueue = idFactory.standByIdSyncQueue;
						if (!standByIdSyncQueue.isEmpty()) {
							AbstractIdFactory<T, S>.StandByIdSync sync = standByIdSyncQueue.poll();
							idSyncThreadPoolExecutor.remove(id.getIdSyncListenerMap().get(sync));
							idSyncThreadPoolExecutor.remove(sync);
						}
						break;
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});
	}

}
