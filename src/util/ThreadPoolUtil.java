/**
 * 
 */
package util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import constants.ConfigConstant;

/**
 * @author sumit
 *
 */
public class ThreadPoolUtil {

	private ThreadPoolUtil() {

	}

	private static final int THREAD_POOL_SIZE = ConfigConstant.NUMBER_OF_ACTIVE_THREADS;

	public static ExecutorService getThreadPool(int poolsize) {
		return Executors.newFixedThreadPool(poolsize <= 0 ? THREAD_POOL_SIZE
				: poolsize);
	}

	public static void waitForThreadsToFinish(ExecutorService threadPool,
			long sleepTimeMiliSecnds) throws InterruptedException {

		// wait for all threads to finish
		// threadPool.awaitTermination(1, TimeUnit.DAYS);
		threadPool.shutdown();
		// waiting for other threads to finish
		while (!threadPool.isTerminated()) {
			cleanUpGarbageRequest();
			Thread.sleep(sleepTimeMiliSecnds);
		}
		cleanUpGarbageRequest();
	}

	public static <T> Future<T> submitJob(ExecutorService threadPool,
			Callable<T> thread) {
		return threadPool.submit(thread);
	}

	public static void cleanUpGarbageRequest() {
		System.runFinalization();
		System.gc();
	}

	/**
	 * 
	 */
	public static void dumpStackTrace(Thread t) {
		System.out.println("Thread Name::" + t.getName());
		System.out.println("Is interuppted::"
				+ (Thread.interrupted() ? "YES" : "NO"));
		System.out.println("Is Alive:: " + (t.isAlive() ? "YES" : "NO"));

		System.out.println("Estimated Number of active threads::"
				+ Thread.activeCount());
		Thread.dumpStack();
	}
}
