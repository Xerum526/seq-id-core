package com.seq.id;

public interface IdSyncStrategy {

	public <T, S> int evaluate(AbstractIdFactory<T, S> idFactory);

	public <T, S> void execute(AbstractIdFactory<T, S> idFactory);

}
