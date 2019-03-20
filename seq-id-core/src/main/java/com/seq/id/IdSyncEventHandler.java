package com.seq.id;

public interface IdSyncEventHandler {

	<T, S> void handle(AbstractIdSync<T, S> sync);

}
