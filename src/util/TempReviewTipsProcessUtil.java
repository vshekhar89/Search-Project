/**
 * 
 */
package util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.json.JSONObject;

import constants.ConfigConstant;
import datapreprocesor.ILineProcessor;

/**
 * @author sumit
 *
 */
public class TempReviewTipsProcessUtil {

	public static ILineProcessor getLineProcessor() {
		return new ILineProcessor() {
			@Override
			public Object processLine(String jsonText) {
				// get the json Object
				JSONObject lineObject = JsonUtil.getJSONObject(jsonText);
				return lineObject;
			}
		};
	}

	public static void generateReviewTipsUnionFiles(String tempDirLocn,
			String reviewFileName, String tipsFileName) throws IOException {

		// stream read one by one line and create the temporary documents
		// for
		// reviews and tips text-hashed by business id's

		FileWriter[] fileWriterPool = FileUtils.getPoolOfFileWriter(
				ConfigConstant.HASH_CODE_POOL_SIZE,
				ConfigConstant.TEMP_REVIEW_TIPS_TEXT_LOCATION);

		// iterate through the lines of the review and tips file and
		// generate
		// the temporary files contains the JSonobject on each line : with
		// the
		// text per line : 'business_id', type: 'review'/'tips',
		// text:<actual
		// text>

		// perform temp generation of all files

		ILineProcessor lineProcessor = TempReviewTipsProcessUtil
				.getLineProcessor();

		// iterate through the files and get the bussiness ids and the text
		// file
		// locations
		// iterate through the review file, first
		try {
			iterateReviewTipsFile(fileWriterPool, reviewFileName,
					lineProcessor,
					ConfigConstant.JSON_FIELD.REVIEW.getFieldName());
			iterateReviewTipsFile(fileWriterPool, tipsFileName, lineProcessor,
					ConfigConstant.JSON_FIELD.TIPS.getFieldName());
		} finally {
			FileUtils.cleanUpFileWriterPool(fileWriterPool);
		}
	}

	private static void iterateReviewTipsFile(FileWriter[] fileWriterPool,
			String reviewFileName, ILineProcessor lineProcessor, String type)
			throws IOException {
		String line = null;
		int linesCompleted = 0;
		try (BufferedReader rdr = new BufferedReader(new FileReader(
				reviewFileName))) {
			while ((line = rdr.readLine()) != null) {
				line = line.trim();

				linesCompleted++;
				if (linesCompleted % 100000 == 0) {
					System.out.println("Lines completed :Type :" + type + " : "
							+ linesCompleted);
				}
				// get the hashcode of the object using the bussiness_id
				JSONObject lineObject = (JSONObject) lineProcessor
						.processLine(line);
				// get the hashcode for bussiness id.
				String bussinessID = lineObject
						.getString(ConfigConstant.JSON_FIELD.BUSINESS_ID
								.getFieldName());
				// get the review text or tip text
				String text = lineObject
						.getString(ConfigConstant.JSON_FIELD.TEXT
								.getFieldName());

				int hashCode = getHashCode(bussinessID);
				// dump the string into the file
				JSONObject newJson = new JSONObject();
				newJson.put(
						ConfigConstant.JSON_FIELD.BUSINESS_ID.getFieldName(),
						bussinessID);
				// newJson.put(type, text);
				newJson.put(
						ConfigConstant.JSON_FIELD.RECORD_TYPE.getFieldName(),
						type);
				newJson.put(ConfigConstant.JSON_FIELD.TEXT.getFieldName(), text);

				FileUtils.flushToFile(fileWriterPool[hashCode],
						newJson.toString());
				FileUtils.flushToFile(fileWriterPool[hashCode],
						ConfigConstant.NEW_LINE_CHAR);

			}
		}
		System.out.println("Lines completed :Type :" + type + " : "
				+ linesCompleted);
	}

	private static int getHashCode(String object) {
		// take the last three characters and find the hashcode for it and do
		// mod
		// the prime number
		/*
		 * int asciicode = 0;
		 * 
		 * for (char c : object.substring(object.length() - 6).toCharArray()) {
		 * asciicode += Math.abs((c - 127) * (c - 127)); } return asciicode %
		 * ConfigConstant.HASH_CODE_POOL_SIZE;
		 */
		return Math.abs(object.hashCode() % ConfigConstant.HASH_CODE_POOL_SIZE);
	}

}
