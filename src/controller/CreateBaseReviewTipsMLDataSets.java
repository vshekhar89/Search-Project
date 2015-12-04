/**
 * 
 */
package controller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import util.FileUtils;
import util.LuceneNLPUtil;
import util.TopicModelTemplateUtil;
import constants.ConfigConstant;

/**
 * @deprecated
 * @author sumit
 *
 */
public class CreateBaseReviewTipsMLDataSets {
	// insertion order implementation is required, but not accessed order which
	// is default
	private final static Map<String, Integer> VOCABULARY_MAP = new LinkedHashMap<>();

	/**
	 * @param args
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static void main(String[] args) throws IOException {
		// initiallize the vocubalry map
		loadVocubalry(ConfigConstant.TEMP_RAW_ORIGINAL_VOCABULRAY_TEXT_LOCATIONS);
		createBasicMLDataSets(
				ConfigConstant.TEMP_REVIEW_TIPS_ML_TOPIC_DATA_OUT_LOCATIONS,
				ConfigConstant.TEMP_BASIC_REVIEW_TIPS_ML_DATA_SETS_LOCATION);
	}

	private static void createBasicMLDataSets(
			String tempReviewTipsMlDataLocations,
			String tempBasicReviewTipsMlDataSetsLocation) throws IOException {
		try (BufferedWriter wrtr = new BufferedWriter(new FileWriter(
				tempBasicReviewTipsMlDataSetsLocation))) {
			//print header
			printHeader(wrtr);
			// read on by one file
			for (String fileName : FileUtils
					.getAllFiles(tempReviewTipsMlDataLocations)) {
				
				FileUtils.readAllLinesOptimized(
						getFileName(tempReviewTipsMlDataLocations, fileName),
						StandardCharsets.ISO_8859_1).forEach(line -> {
					try {
						processText(line, wrtr);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				});
				System.out.println("Done::"+fileName);
			}
		}
	}

	private static void printHeader(BufferedWriter wrtr) throws IOException {
		StringBuilder bldr = new StringBuilder();
		bldr.append("#BUSNS-ID" + ConfigConstant.COMMA);
		int size = VOCABULARY_MAP.size();
		for (Map.Entry<String, Integer> x : VOCABULARY_MAP.entrySet()) {
			bldr.append(x.getKey() + (size == 1 ? "" : ConfigConstant.COMMA));
			size--;
			//reset the map entry
			VOCABULARY_MAP.replace(x.getKey(), 0);
		}
		wrtr.write(bldr.toString());
		wrtr.newLine();		
	}

	private static void processText(String line, BufferedWriter wrtr)
			throws JSONException, IOException {

		// get the converted json lines
		JSONObject topicModel = TopicModelTemplateUtil
				.getJSONFromTemplate(line);
		// populate the WORD vector for the current topic model
		TopicModelTemplateUtil.geTopicModelFromTemplate(topicModel).forEach(
				tpm -> {
					try {
						LuceneNLPUtil.getRemovedStopAndStem(tpm.getWord(),
								LuceneNLPUtil.getDefaultEnglishStopWordList())
								.forEach(word -> {
									if (VOCABULARY_MAP.containsKey(word)) {
										VOCABULARY_MAP.replace(word, 1);
									}
								});
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
		// dump it to the file
		dumpTextLine(topicModel.getString(ConfigConstant.JSON_FIELD.BUSINESS_ID
				.getFieldName()), wrtr);
	}

	private static void dumpTextLine(String busnsID, BufferedWriter wrtr)
			throws IOException {
		StringBuilder bldr = new StringBuilder();
		bldr.append(busnsID + ConfigConstant.COMMA);
		int size = VOCABULARY_MAP.size();
		for (Map.Entry<String, Integer> x : VOCABULARY_MAP.entrySet()) {
			bldr.append(x.getValue() + (size == 1 ? "" : ConfigConstant.COMMA));
			size--;
			//reset the map entry
			VOCABULARY_MAP.replace(x.getKey(), 0);
		}
		wrtr.write(bldr.toString());
		wrtr.newLine();
	}

	private static String getFileName(String tempReviewTipsMlDataLocations,
			String fileName) {

		return tempReviewTipsMlDataLocations + fileName;
	}

	private static void loadVocubalry(String tempVocabulrayTextLocations)
			throws IOException {
		System.out.println("loading vocubalry::");
		try (BufferedReader rdr = new BufferedReader(new FileReader(
				tempVocabulrayTextLocations))) {
			String text = null;
			while ((text = rdr.readLine()) != null) {
				VOCABULARY_MAP.put(text, 0);
			}
			System.out.println("Loaded Vocubalry::size ="+VOCABULARY_MAP.size());
		}
	}

}
