/**
 * 
 */
package controller;

import util.FileUtils;
import constants.ConfigConstant;
import datapreprocesor.IJsonPreProcess;
import datapreprocesor.ReviewTipsJsonPreProcess;

/**
 * @author sumit
 *
 */
public class DataPreRpocessController {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		FileUtils.makeDirectory(ConfigConstant.TEMP_REVIEW_TIPS_OUT_DIR);
		// preprocess data files
		IJsonPreProcess preProcessRevTips = new ReviewTipsJsonPreProcess(
				ConfigConstant.REVIEW_LOCATIONS, ConfigConstant.TIPS_LOCATIONS);
		preProcessRevTips.process();
	}

}
