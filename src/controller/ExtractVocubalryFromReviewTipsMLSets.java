/**
 * 
 */
package controller;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import util.FileUtils;
import util.LuceneNLPUtil;
import util.TopicModelTemplateUtil;
import constants.ConfigConstant;

/**
 * It will just extract the original word list from the topic model distribution
 * pattern generated in preprocess phase. Removes less occurred words from the
 * text.
 * 
 * @author sumit
 *
 */
public class ExtractVocubalryFromReviewTipsMLSets {

	private static final String START_WITH_NONALPHA_CHAR = "^[^a-zA-Z0-9].*";
	// how many times a word appeared
	private static final Map<String, Integer> vocabDist = new HashMap<>();
	private static final int DEFAULT_WORD_COUNT_ALLOWED = 1;
	//maximum allowed string length
	private static final int MAX_STRING_LENGTH = 30;

	/**
	 * @param args
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static void main(String[] args) throws IOException {
		FileUtils.makeDirectory(ConfigConstant.TEMP_VOCABULARY_OUTPUT_DIR);
		// read one by one line from the review tips location and extract the
		// words
		Set<String> vocubalary = new HashSet<>();
		for (String fileNames : FileUtils
				.getAllFiles(ConfigConstant.TEMP_REVIEW_TIPS_ML_TOPIC_DATA_OUT_LOCATIONS)) {

			updateVocabulary(FileUtils.getFullPath(
					ConfigConstant.TEMP_REVIEW_TIPS_ML_TOPIC_DATA_OUT_LOCATIONS,
					fileNames), vocubalary);
		}

		// remove all words that appeared very less often
		vocabDist.forEach((word, value) -> {
			if (value <= DEFAULT_WORD_COUNT_ALLOWED) {
				vocubalary.remove(word);
			}
		});

		vocabDist.clear();
		
		// write the remaining files
		writeToFile(new TreeSet<String>(vocubalary),
				ConfigConstant.TEMP_RAW_ORIGINAL_VOCABULRAY_TEXT_LOCATIONS);
	}

	private static void writeToFile(Set<String> vocabulary,
			String tempVocabulrayTextLocations) throws IOException {

		FileUtils.writeLinesToFile(vocabulary, tempVocabulrayTextLocations);

	}

	private static void updateVocabulary(String fileNames,
			Set<String> vocabulary) throws IOException {

		try (BufferedReader rdr = new BufferedReader(new FileReader(fileNames))) {
			String line = null;
			while ((line = rdr.readLine()) != null) {
				LuceneNLPUtil.getRemovedStopAndStem(
						String.join(ConfigConstant.SPACE,
								TopicModelTemplateUtil
										.getWordsFromTemplate(line)),
						LuceneNLPUtil.getDefaultEnglishStopWordList()).forEach(
						e -> {
							filteredUpdate(vocabulary, e);
						});

				/*
				 * vocabulary.addAll(TopicModelTemplateUtil
				 * .getWordsFromTemplate(line));
				 */
			}

		}

		System.out.println("completed vocabulary extractions::" + fileNames);
	}

	private static void filteredUpdate(Set<String> vocabulary, String word) {

		if (word.trim().length() > MAX_STRING_LENGTH
				|| ((LuceneNLPUtil.isAllASCII(word) || word.trim().length() <= 1) && word
						.matches(START_WITH_NONALPHA_CHAR))) {
			return;
		}
		vocabulary.add(word);
		vocabDist.put(word, vocabDist.getOrDefault(word, 0) + 1);
	}

}
