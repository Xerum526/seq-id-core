package com.seq.id;

public interface IdSyncListener {

	<T, S> void listen(AbstractIdSync<T, S> sync, IdSyncEventHandler handler);

}
