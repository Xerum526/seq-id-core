package com.seq.id;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class IdSyncThreadPoolExecutor extends ThreadPoolExecutor {

	IdSyncThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit timeUnit,
			int workQueueCapacity, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, timeUnit,
				new ArrayBlockingQueue<Runnable>(workQueueCapacity), threadFactory, handler);
	}

	static ThreadFactory initDefaultThreadFactory() {
		return new ThreadFactory() {

			@Override
			public Thread newThread(Runnable r) {

				return new Thread(r);
			}

		};
	}

	static RejectedExecutionHandler initDefaultRejectedExecutionHandler() {
		return new RejectedExecutionHandler() {

			@Override
			public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
				throw new RuntimeException("no more threads or queue slots are available for idsync");

			}

		};
	}

}
