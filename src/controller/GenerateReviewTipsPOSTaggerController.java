/**
 * 
 */
package controller;

import java.util.concurrent.ExecutorService;

import util.FileUtils;
import util.ThreadPoolUtil;
import constants.ConfigConstant;
import constants.ConfigConstant.DICTIONARY_TYPE;
import datapreprocesor.CreateReviewTipsPOSTaggerDataSets;

/**
 * Supporting the post tagging based on English text.
 * 
 * @author sumit
 *
 */
public class GenerateReviewTipsPOSTaggerController {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		// read one by one line , from the source
		String sourceDir = ConfigConstant.TEMP_REVIEW_TIPS_OUT_DIR;
		String outputDir = ConfigConstant.TEMP_POS_TAGGER_REV_TIPS_OUT_DIR;
		String invalidDir = outputDir;
		FileUtils.createDirectory(invalidDir, outputDir);
		final float accuarcy = 0.6f;
		final int THREAD_POOL_SIZE = ConfigConstant.NUMBER_OF_ACTIVE_THREADS;

		// multi threading started
		ExecutorService threadPool = ThreadPoolUtil
				.getThreadPool(THREAD_POOL_SIZE);

		for (String fileName : FileUtils.getAllFiles(sourceDir)) {
			// for each file get the line from the
			CreateReviewTipsPOSTaggerDataSets posTagger = new CreateReviewTipsPOSTaggerDataSets(
					FileUtils.getFullPath(sourceDir, fileName), outputDir,
					invalidDir);

			posTagger.setAccuarcy(accuarcy);
			posTagger.setDictionaryType(DICTIONARY_TYPE.ENGLISH);
			posTagger
					.setPosTaggerModelLocation(ConfigConstant.POS_TAGGER_MODEL.ENGLISH_FAST_SLIGHT_INACCURATE
							.getPath());
			ThreadPoolUtil.submitJob(threadPool, posTagger);
		}

		// wait for other thread to finish
		ThreadPoolUtil.waitForThreadsToFinish(threadPool,
				ConfigConstant.THREAD_POOL_SLEEP_TIME);
		CreateReviewTipsPOSTaggerDataSets.clearCachedData();
		ThreadPoolUtil.cleanUpGarbageRequest();
		System.out.println("Finsihed POS tagging operations!!!");
	}

}
