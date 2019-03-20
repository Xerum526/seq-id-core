package com.seq.id;

import java.util.concurrent.locks.LockSupport;

class IdSyncAppendQueueListener extends AbstractIdSyncListener {

	private AbstractIdSync<?, ?> sync;

	public IdSyncAppendQueueListener(AbstractIdSync<?, ?> sync) {
		this.sync = sync;
	}

	@Override
	public <T, S> void listen(AbstractIdSync<T, S> sync, IdSyncEventHandler handler) {
		int size = sync.queueSize;
		int appendThreshold = sync.appendThreshold;
		if (size < appendThreshold) {
			handler.handle(sync);
		}
	}

	@Override
	public void run() {
		for (;;) {
			listen(sync, new IdSyncEventHandler() {

				@Override
				public <T, S> void handle(AbstractIdSync<T, S> sync) {
					LockSupport.unpark(sync.appendIdThread);
				}

			});
		}

	}

}
