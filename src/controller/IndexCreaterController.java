/**
 * 
 */
package controller;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import util.FileUtils;
import util.ThreadPoolUtil;
import constants.ConfigConstant;
import datapreprocesor.CreateIndex;
import datapreprocesor.IndexCreateJob;

/**
 * @author sumit
 *
 */
public class IndexCreaterController {

	/**
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException,
			InterruptedException {
		FileUtils
				.makeDirectory(ConfigConstant.INDEX_OUTPUT_RAW_REVIEW_TIPS_OUT_DIR);

		// get all files in the temp directory of map reduced
		ExecutorService threadPool = ThreadPoolUtil
				.getThreadPool(ConfigConstant.NUMBER_OF_ACTIVE_THREADS);

		CreateIndex createIndex = new CreateIndex(
				ConfigConstant.INDEX_OUTPUT_RAW_REVIEW_TIPS_OUT_DIR);

		final String inputSourceLocation = ConfigConstant.TEMP_REVIEW_TIPS_OUT_DIR;

		for (String fileLoctn : FileUtils.getAllFiles(inputSourceLocation)) {
			// ignoring the return type,that can be used for checking the status
			// of the tasks.
			ThreadPoolUtil.submitJob(
					threadPool,
					new IndexCreateJob(createIndex, FileUtils.getFullPath(
							inputSourceLocation, fileLoctn)));
		}

		// wait for all threads to finish
		// threadPool.awaitTermination(1, TimeUnit.DAYS);
		ThreadPoolUtil.waitForThreadsToFinish(threadPool,
				ConfigConstant.THREAD_POOL_SLEEP_TIME);
		
		createIndex.releaseResource();
		System.out.println("Finished Index Creations:: From Review and Tips Raw Text");

	}

}
