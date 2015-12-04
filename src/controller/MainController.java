/**
 * 
 */
package controller;

import spellchecker.YelpSpellCheckerUtil;
import util.FileUtils;
import constants.ConfigConstant;
import datapreprocesor.mlpreprocessor.ExtractNounsMLDataSet;

/**
 * @author sumit
 *
 */
public class MainController {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		preProcessData(args);
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	private static void preProcessData(String[] args) throws Exception {
		//createAllDirectories();
		System.out.println("Data Preprocessing Started::");
		//DataPreRpocessController.main(args);
		System.out.println("Index Creation Strated::");
		// IndexCreaterController.main(args);
		System.out.println("POS-Tagging Started");
		//GenerateReviewTipsPOSTaggerController.main(args);
		System.out.println("Topic model Data Sets creation Started::");
		//GenerateReviewTipsTopicModelController.main(args);
		System.out
				.println("Extract unique vocubalry with stemming and stopwords removal::");
		//ExtractVocubalryFromReviewTipsMLSets.main(args);
		System.out
				.println("Yellp Spell checking and different vocubalry language support::");
		//YelpSpellCheckerUtil.generateAllCorrectedWords(
			//	ConfigConstant.TEMP_RAW_ORIGINAL_VOCABULRAY_TEXT_LOCATIONS,
				//ConfigConstant.TEMP_CORRECTED_VOCABULARY_OUT_PATH, null);

		System.out.println("Generating the ml data sets::");
		// CreateBaseReviewTipsMLDataSets.main(args);
		ExtractNounsMLDataSet.main(args);
	}

	private static void createAllDirectories() {
		// create all temporary file locations
		FileUtils.createDirectory(ConfigConstant.TEMP_DIRECTORY,
				ConfigConstant.TEMP_REVIEW_TIPS_OUT_DIR,
				ConfigConstant.TEMP_ML_DIR,
				ConfigConstant.TEMP_REVIEW_TIPS_ML_TOPIC_DATA_OUT_LOCATIONS,
				ConfigConstant.TEMP_FINAL_ML_DATA_DIR,
				ConfigConstant.TEMP_CORRECTED_VOCABULARY_OUT_PATH);
	}

}
