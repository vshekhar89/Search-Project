/**
 * 
 */
package spellchecker;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import util.FileUtils;

import com.swabunga.spell.event.SpellChecker;

import constants.ConfigConstant;
import constants.ConfigConstant.DICTIONARY_TYPE;

/**
 * This class implements the spell check functionality required by the yelp data
 * sets challenge. It support English, German, French spell check. It acts as
 * the purpose of demoing for using Jazzy and Lucene Spell Checker Routines.
 * Generates dfferent vocabulary files based on the raw vocabulary.
 * 
 * @author sumit
 *
 */
public class YelpSpellCheckerUtil {

	private static DICTIONARY_TYPE[] RULE_PRECEDENCE_ORDER = new DICTIONARY_TYPE[] {
			DICTIONARY_TYPE.ENGLISH, DICTIONARY_TYPE.FRENCH,
			DICTIONARY_TYPE.GERMAN, DICTIONARY_TYPE.ITALIAN,
			DICTIONARY_TYPE.GREEK };

	private static final String DEFAULT_OUTPUT_FILE_PREFIX = "vocubalary_";

	private YelpSpellCheckerUtil() {
	}

	/**
	 * Generate all the possible valid vocabulary output files for each language
	 * that are supported.
	 * 
	 * @param pathToVocubalryText
	 *            - path to the file that contains the each word.
	 * @param dirWithFilePrefix
	 *            - output directory where the corrected suggestion words will
	 *            be dumped with different language support.
	 * @param prefixFileOutPut
	 *            - prefix of the file : if not provided than
	 *            "vocabuary_<language>.txt will be generated."
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static void generateAllCorrectedWords(String pathToVocubalryText,
			String dirOfOutputCorrectedFile, String prefixFileOutPut)
			throws IOException {
		// read the vocabulary text and create different text file for each
		// case.
		FileUtils.makeDirectory(dirOfOutputCorrectedFile);
		
		Map<ConfigConstant.DICTIONARY_TYPE, Set<String>> correctedOutPut = new HashMap<>();
		// this is an optimized addition of dictionary to the set
		Map<ConfigConstant.DICTIONARY_TYPE, JazzySpellCheckerListener> dictionary = new HashMap<>();

		try (BufferedReader rdr = Files.newBufferedReader(Paths
				.get(pathToVocubalryText))) {
			String line = null;
			// for each word identify the corrected possible output
			while ((line = rdr.readLine()) != null) {
				updateWord(line, correctedOutPut, dictionary);
			}
		}
		// dump them to the file
		writeAllDifferentVocabularyWords(dirOfOutputCorrectedFile,
				prefixFileOutPut, correctedOutPut);
		
		correctedOutPut.clear();
		dictionary.clear();		
	}

	private static void writeAllDifferentVocabularyWords(
			String dirOfOutputCorrectedFile, String prefixFileOutPut,
			Map<DICTIONARY_TYPE, Set<String>> correctedOutPut)
			throws IOException {

		for (Map.Entry<DICTIONARY_TYPE, Set<String>> words : correctedOutPut
				.entrySet()) {
			FileUtils.writeLinesToFile(
					new TreeSet<String>(words.getValue()),
					getFileName(dirOfOutputCorrectedFile, prefixFileOutPut,
							words.getKey().getSuffix()));
		}
	}

	private static String getFileName(String dirOfOutputCorrectedFile,
			String prefixFileOutPut, String suffix) {

		return dirOfOutputCorrectedFile
				+ ((prefixFileOutPut == null || prefixFileOutPut.trim()
						.length() == 0) ? DEFAULT_OUTPUT_FILE_PREFIX
						: prefixFileOutPut) + suffix + ".txt";
	}

	private static void updateWord(String word,
			Map<DICTIONARY_TYPE, Set<String>> correctedOutPut,
			Map<DICTIONARY_TYPE, JazzySpellCheckerListener> dictionary)
			throws IOException {

		String changedWord = preProcessWord(word);
		if (changedWord.length() == 0) {
			return;
		}

		// check if any of the dictionary contains it
		for (DICTIONARY_TYPE dictionary_type : RULE_PRECEDENCE_ORDER) {
			JazzySpellCheckerListener splChkLstnr = getSpellCheckListener(
					dictionary_type, dictionary);
			if (splChkLstnr.isCorrect(changedWord)) {
				// System.out.println(changedWord + " :: " + word);
				addToCorrectedOutput(dictionary_type, correctedOutPut,
						changedWord);
				return;
			}
		}

		// System.out.println(changedWord + "::" + word);
		// otherwise add the old word
		addToCorrectedOutput(DICTIONARY_TYPE.UNKNOWN, correctedOutPut, word);

		// Supporting only English words we are ignoring the following type's
		// check for time being
		// check whether it is an English or other language words.

		// extractCorrectSuggestedWord(word, dictionary, correctedOutPut);

	}

	/**
	 * @param word
	 * @param dictionary
	 * @param correctedOutPut
	 * @throws IOException
	 */
	private static void extractCorrectSuggestedWord(String word,
			Map<DICTIONARY_TYPE, JazzySpellCheckerListener> dictionary,
			Map<DICTIONARY_TYPE, Set<String>> correctedOutPut)
			throws IOException {
		for (DICTIONARY_TYPE dictionary_type : RULE_PRECEDENCE_ORDER) {
			JazzySpellCheckerListener splChkLstnr = getSpellCheckListener(
					dictionary_type, dictionary);
			// check if it is right representation of the word in this current
			// dictionary.
			List<String> suggestedWord = getCorrectedWords(word, splChkLstnr);
			filterWordBasedOnSimilarity(suggestedWord, word);
		}
	}

	private static void addToCorrectedOutput(DICTIONARY_TYPE dictionary_type,
			Map<DICTIONARY_TYPE, Set<String>> correctedOutPut, String word) {
		if (!correctedOutPut.containsKey(dictionary_type)) {
			correctedOutPut.put(dictionary_type, new HashSet<String>());
		}
		correctedOutPut.get(dictionary_type).add(word);
	}

	private static void filterWordBasedOnSimilarity(List<String> suggestedWord,
			String line) {

		// if there is no suggested word
		if (suggestedWord.size() == 0 || line.trim().length() == 0) {
			return;
		}

		// 2.2.1. get all suggested word and find the best representation of
		// this word by computing the similarity between the wrong word and the
		// corrects word.

	}

	public static String preProcessWord(String word) {
		if (word.trim().length() == 0)
			return word;
		// it contains the domain named form word remove all of them
		word = word.replaceAll("[\\w.]*\\.[\\w]{1,3}", "");
		// remove any character which are of the form punctuation except
		word = word.replaceAll("[\\p{P}]+", "");
		return word;
	}

	public static List<String> getCorrectedWords(String word,
			JazzySpellCheckerListener splChkLstnr) {
		List<String> result = new ArrayList<>();
		// 1. check if this is the correct word.
		if (splChkLstnr.isCorrect(word)) {
			result.add(word);
			return result;
		}

		// 2. if this is not the correct word.

		// 2.1. check if it is the net word, it is
		// assumed to be UNKNOWN TYPE, for our case
		if (SpellChecker.isINETWord(word)) {
			return result;
		}

		List<String> suggestedWord = splChkLstnr.getSuggestions(word, 600);

		if (suggestedWord.size() == 0) {
			// fallback to the lucene dictionary
		}
		return suggestedWord;
	}

	private static JazzySpellCheckerListener getSpellCheckListener(
			DICTIONARY_TYPE dictionary_type,
			Map<DICTIONARY_TYPE, JazzySpellCheckerListener> dictionary)
			throws IOException {
		if (!dictionary.containsKey(dictionary_type)) {
			dictionary.put(dictionary_type, new JazzySpellCheckerListener(
					dictionary_type.getPath()));
		}
		return dictionary.get(dictionary_type);
	}

	public static JazzySpellCheckerListener getSpellChekerFor(
			DICTIONARY_TYPE dictionary_type) throws IOException {
		return new JazzySpellCheckerListener(dictionary_type.getPath());
	}
}
