/**
 * 
 */
package controller;

import java.util.concurrent.ExecutorService;

import util.FileUtils;
import util.ThreadPoolUtil;
import cc.mallet.topics.ParallelTopicModel;
import constants.ConfigConstant;
import datapreprocesor.CreateReviewTipsTopicModelDataSet;

/**
 * This is time consuming data pre-processing class. It's thread pool size must
 * be kept small. It uses topic modeling module LDA, needs lot of computation
 * power. It uses lot of memory band width. Trade off between the thread pool
 * size and memory required is balanced appropriately.
 * 
 * @author sumit
 *
 */
public class GenerateReviewTipsTopicModelController {

	public static void main(String[] args) throws Exception {

		FileUtils.makeDirectory(ConfigConstant.TEMP_REVIEW_TIPS_ML_TOPIC_DATA_OUT_LOCATIONS);
		
		// ParallelTopicModel.logger.addHandler(new
		// FileHandler(ConfigConstant.TEMP_DIRECTORY+"%h/%u.log"));
		ParallelTopicModel.logger.setUseParentHandlers(false);

		final int THREAD_POOL_SIZE = ConfigConstant.NUMBER_OF_ACTIVE_THREADS;
		// get all files in the temp directory of map reduced

		ExecutorService threadPool = ThreadPoolUtil
				.getThreadPool(THREAD_POOL_SIZE);

		final String inputSourceLocation = ConfigConstant.TEMP_POS_TAGGER_REV_TIPS_OUT_DIR;// TEMP_REVIEW_TIPS_DIR

		for (String fileLoctn : FileUtils.getAllFiles(inputSourceLocation)) {
			// ignoring the return type,that can be used for checking the status
			// ignoring the return type,that can be used for checking the status
			// of the tasks.
			CreateReviewTipsTopicModelDataSet crt = new CreateReviewTipsTopicModelDataSet(
					FileUtils.getFullPath(inputSourceLocation, fileLoctn),
					ConfigConstant.TEMP_REVIEW_TIPS_ML_TEXT_LOCATION);
			// crt.setListOfWords(allUniqueWords);
			// crt.call();
			// threadPool.submit(crt);
			ThreadPoolUtil.submitJob(threadPool, crt);
		}

		// wait for all threads to finish
		// wait for other thread to finish
		ThreadPoolUtil.waitForThreadsToFinish(threadPool,
				ConfigConstant.THREAD_POOL_SLEEP_TIME);
		ThreadPoolUtil.cleanUpGarbageRequest();
		System.out.println("Finsihed Ml Data Sets Creations!!!");
	}

}
