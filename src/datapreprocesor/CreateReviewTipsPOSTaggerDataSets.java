/**
 * 
 */
package datapreprocesor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.json.JSONObject;

import spellchecker.JazzySpellCheckerListener;
import util.FileUtils;
import util.JsonUtil;
import util.LuceneNLPUtil;
import util.POSTaggerUtil;
import util.ThreadPoolUtil;

import com.swabunga.spell.engine.SpellDictionary;

import constants.ConfigConstant;
import constants.ConfigConstant.DICTIONARY_TYPE;
import constants.ConfigConstant.JOB_EXECUTION_STATUS;
import constants.ConfigConstant.JSON_FIELD;
import constants.ConfigConstant.TAGGER_TAGS;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

/**
 * 
 * 
 * @author sumit
 *
 */
public class CreateReviewTipsPOSTaggerDataSets implements
		Callable<JOB_EXECUTION_STATUS> {

	private String inputSourceFile;
	private String validOutputDir;
	private String invalidOutputDir;
	// between 1 and 0 , one means 100% accuracy, for exact match with
	// dictionary keyword
	private float accuarcy;

	private String posTaggerModelLocation;
	private DICTIONARY_TYPE dictionaryType;

	// optimization
	private MaxentTagger tagger;
	private JazzySpellCheckerListener spellCheckListener;
	List<JSONObject> finalTextJsonResult = new ArrayList<>();
	private BufferedWriter wrtr;

	// shared across threads
	// location to tagger cache
	private static final Map<String, MaxentTagger> POS_TAGGER_CAHCE = new HashMap<String, MaxentTagger>();
	// dictionary cache
	private static final Map<DICTIONARY_TYPE, SpellDictionary> DICTIONARIES = new HashMap<DICTIONARY_TYPE, SpellDictionary>();

	private final static int DEFAULT_THRESHOLD_DUMP = 2000;

	// file index count , update in a synchronized way.
	private static int file_index = 0;

	private final static Set<ConfigConstant.TAGGER_TAGS> FILTERED_TAGS = new HashSet<>();

	static {
		// adjective
		FILTERED_TAGS.add(TAGGER_TAGS.JJ);
		FILTERED_TAGS.add(TAGGER_TAGS.JJR);
		FILTERED_TAGS.add(TAGGER_TAGS.JJS);
		// noun
		FILTERED_TAGS.add(TAGGER_TAGS.NN);
		FILTERED_TAGS.add(TAGGER_TAGS.NNP);
		FILTERED_TAGS.add(TAGGER_TAGS.NNPS);
		FILTERED_TAGS.add(TAGGER_TAGS.NNS);
		// adverb
		FILTERED_TAGS.add(TAGGER_TAGS.RB);
		FILTERED_TAGS.add(TAGGER_TAGS.RBR);
		FILTERED_TAGS.add(TAGGER_TAGS.RBS);
		FILTERED_TAGS.add(TAGGER_TAGS.RP);
		// foreign word, required for foreign language filter
		FILTERED_TAGS.add(TAGGER_TAGS.FW);// may omit this one later point of
											// time
	}

	public CreateReviewTipsPOSTaggerDataSets(String pInptSrcFile,
			String pValidOutputDir, String pInvalidOutputDir) {
		this.inputSourceFile = pInptSrcFile;
		this.invalidOutputDir = pInvalidOutputDir;// in future update the
													// invalid files output
		this.validOutputDir = pValidOutputDir;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.Callable#call()
	 */
	@Override
	public JOB_EXECUTION_STATUS call() throws Exception {
		return processOptimized();
	}

	private JOB_EXECUTION_STATUS processOptimized()
			throws FileNotFoundException, IOException {
		JOB_EXECUTION_STATUS sts = JOB_EXECUTION_STATUS.IN_PROGRESS;
		// read the file at that location
		// int lineCount = 0;
		try (BufferedReader rdr = new BufferedReader(new FileReader(
				this.inputSourceFile))) {

			String line = null;
			List<JSONObject> texts = new ArrayList<>(DEFAULT_THRESHOLD_DUMP);
			// read one by one line
			while ((line = rdr.readLine()) != null) {
				texts.add(JsonUtil.getJSONObject(line));
				if (texts.size() >= DEFAULT_THRESHOLD_DUMP) {
					computeDatsSet(texts);

					// lineCount += texts.size();
					// System.out.println("completed [" + this.inputSourceFile
					// + "]:lines:: " + lineCount);

					texts.clear();
				}
			}
			// for remaining texts
			computeDatsSet(texts);

			// lineCount += texts.size();
			// System.out.println("completed [" + this.inputSourceFile
			// + "]:lines:: " + lineCount);

			texts.clear();
			sts = JOB_EXECUTION_STATUS.SUCCESS;
		} catch (Throwable ex) {
			ex.printStackTrace();
		} finally {
			if (sts != JOB_EXECUTION_STATUS.SUCCESS) {
				ThreadPoolUtil.dumpStackTrace(Thread.currentThread());
				sts = JOB_EXECUTION_STATUS.SUCCESS_WITH_ERROR;
			}

			releaseResource();
			clearUnusedMemoryRequest();
			System.out.println("Completed file Name::" + this.inputSourceFile
					+ ", Status::" + sts);
		}
		return sts;
	}

	/**
	 * 
	 */
	private void clearUnusedMemoryRequest() {
		ThreadPoolUtil.cleanUpGarbageRequest();
	}

	private void releaseResource() throws IOException {
		try {
			if (this.wrtr != null) {
				this.wrtr.close();
				this.wrtr = null;
			}
		} finally {
			// do nothing
			clearUnusedMemoryRequest();
		}

	}

	private void computeDatsSet(List<JSONObject> texts)
			throws FileNotFoundException, IOException {
		if (texts.size() < 1 || this.posTaggerModelLocation == null) {
			return;
		}

		// 1. perform pos-tagging of each record line.
		this.tagger = this.tagger == null ? loadTaggerFromCahche()
				: this.tagger;

		for (JSONObject textJson : texts) {
			if (textJson.has(JSON_FIELD.TEXT.getFieldName())) {
				Map<TAGGER_TAGS, Set<String>> taggedWords = getTaggedWordsPair(
						textJson.getString(JSON_FIELD.TEXT.getFieldName()),
						FILTERED_TAGS);

				// for each of the tagged words see how many are accurate, and
				// if it is below threshold, reject them
				filteredByDictionary(taggedWords);
				// now perform stemming
				filteredByStemming(taggedWords);

				// filter again on stemmed words for verb and other words
				String sentencedAfterStemming = getSentenceFromTaggedWordsPairs(taggedWords);

				taggedWords = getTaggedWordsPair(sentencedAfterStemming,
						FILTERED_TAGS);
				filteredByDictionary(taggedWords);

				// create the final json object to dump for other learnings
				// format : {bussinessID,Text:[space separated]}
				updateFinalResult(this.finalTextJsonResult, taggedWords,
						textJson);

				if (this.finalTextJsonResult.size() == 0) {
					// dump to invalid file
				}
			} else {
				// dump to invalid file
			}
		}

		// dump the filtered text to the file
		dumpToValidFile(this.finalTextJsonResult);
		clearUnusedMemoryRequest();
	}

	private Map<TAGGER_TAGS, Set<String>> getTaggedWordsPair(String text,
			Set<TAGGER_TAGS> fileteredSet) {
		return POSTaggerUtil.getAllFilteredTaggedWordPair(this.tagger, text,
				fileteredSet);
	}

	private void dumpToValidFile(List<JSONObject> finalResult)
			throws IOException {
		if (finalResult.size() < 1) {
			return;
		}
		// for optimization reasons, the following check
		this.wrtr = this.wrtr == null ? createFileResource() : this.wrtr;

		for (JSONObject t : finalResult) {
			FileUtils.writeToFileWithNewLine(this.wrtr, t.toString());
		}
		finalResult.clear();
	}

	private BufferedWriter createFileResource() throws IOException {
		int k = 0;
		String p = this.inputSourceFile.substring(
				this.inputSourceFile.lastIndexOf("_") + 1).trim();
		if (p.length() == 0) {
			// small synchronization boundary
			synchronized (CreateReviewTipsPOSTaggerDataSets.class) {
				k = file_index;
				file_index++;
			}
		} else {
			k = Integer.valueOf(p);
		}

		return new BufferedWriter(new FileWriter(FileUtils.getFullPath(
				this.validOutputDir, ConfigConstant.TEMP_VALID_FILE_NAME_PREFIX
						+ k)));
	}

	private void updateFinalResult(final List<JSONObject> finalResult,
			Map<TAGGER_TAGS, Set<String>> taggedWords, JSONObject oldJson) {

		if (taggedWords.size() < 1) {
			return;
		}

		JSONObject newJson = new JSONObject();
		newJson.put(JSON_FIELD.BUSINESS_ID.getFieldName(),
				oldJson.get(JSON_FIELD.BUSINESS_ID.getFieldName()));
		String text = getSentenceFromTaggedWordsPairs(taggedWords);

		newJson.put(JSON_FIELD.TEXT.getFieldName(), text);
		newJson.put(JSON_FIELD.RECORD_TYPE.getFieldName(),
				oldJson.getString(JSON_FIELD.RECORD_TYPE.getFieldName()));
		finalResult.add(newJson);
	}

	/**
	 * @param taggedWords
	 * @return
	 */
	private String getSentenceFromTaggedWordsPairs(
			Map<TAGGER_TAGS, Set<String>> taggedWords) {
		StringBuilder text = new StringBuilder();

		for (Set<String> str : taggedWords.values()) {
			for (String s : str) {
				text.append(s).append(ConfigConstant.SPACE);
			}
		}
		return text.toString();
	}

	private void filteredByStemming(Map<TAGGER_TAGS, Set<String>> taggedWords)
			throws FileNotFoundException, IOException {
		if (taggedWords.size() < 1) {
			return;
		}
		setSpellCheckListener();

		List<String> changedWords = new ArrayList<String>();
		List<String> finalWordSet = new ArrayList<>();
		for (Map.Entry<TAGGER_TAGS, Set<String>> tagWrds : taggedWords
				.entrySet()) {
			// stem the word and see if it is correct
			Iterator<String> wordItrt = tagWrds.getValue().iterator();
			while (wordItrt.hasNext()) {
				String originalWord = wordItrt.next();
				List<String> stemmedAndStopWordSet = LuceneNLPUtil
						.getRemovedStopAndStem(originalWord,
								LuceneNLPUtil.getDefaultEnglishStopWordList());
				// validate with dictionary each stemmed word
				for (String text : stemmedAndStopWordSet) {
					quickFixCorrectWord(changedWords, text);
				}
				// if stemming occurred and the stemming cause an empty word
				// if no stemming occurred
				if (changedWords.size() == 0
						|| (changedWords.size() == 1 && changedWords.get(0)
								.equals(originalWord))) {
					// remove an empty word dude to stemming, since its a valid
					// word and
					// stemming may or may not give rise to invalid word.
					// *********cautious operation.**********
					if (changedWords.size() == 0) {
						/*
						 * System.out.println("word:: " + originalWord +
						 * ", stemmed words:: " + changedWords);
						 */
						wordItrt.remove();
					}
				}
				// we have to keep balance between the number of word generated
				// by the stem and the original valid word.
				// we will re-check the stemmed word, if they are noun add them.
				else if (changedWords.size() > 0) {
					wordItrt.remove();// may be stemmed word is better
					// System.out.println("word::"+originalWord+", removed word:: "
					// + changedWords);
					finalWordSet.addAll(changedWords);
				}
				// resetting changed words for next iterations
				changedWords.clear();
			}
			// update current word sets if any
			tagWrds.getValue().addAll(finalWordSet);

			// reset the final word set
			finalWordSet.clear();
		}
	}

	/**
	 * @param changedWords
	 * @param text
	 * @return
	 */
	private void quickFixCorrectWord(List<String> changedWords, String text) {
		if (text.trim().length() <= 1) {
			return;
		}
		if (this.spellCheckListener.isCorrect(text)) {
			changedWords.add(text);
		} else {
			// pr-process the word, split on boundary and check
			// whether
			String[] possibleCorrectWord = getReducedWords(text);
			for (String t : possibleCorrectWord) {
				if (t.trim().length() <= 1) {
					continue;
				}
				if (this.spellCheckListener.isCorrect(t)) {
					changedWords.add(t);
				}
			}
		}

	}

	/**
	 * @param text
	 * @return
	 */
	private String[] getReducedWords(String text) {
		// remove domained words
		text = text.replaceAll("[\\w.]*\\.[\\w]{1,3}", "");
		// remove punctuation if and split and verify word
		String[] possibleCorrectWord = text.replaceAll("[\\p{P}]+", " ").split(
				"\\s+");
		return possibleCorrectWord;
	}

	private void filteredByDictionary(Map<TAGGER_TAGS, Set<String>> taggedWords)
			throws FileNotFoundException, IOException {
		if (taggedWords.size() < 1) {
			return;
		}
		// total number of words
		int totalWordCount = taggedWords.values().stream()
				.mapToInt(v -> v.size()).sum();

		// count the number of foreign word
		int fw = taggedWords.getOrDefault(TAGGER_TAGS.FW,
				Collections.emptySet()).size();
		if (isBelowAccuracy(fw, totalWordCount)) {
			taggedWords.clear();
			return;
		}

		// find the total number of incorrect words that do not match in the
		// dictionary, remove those bad words also
		int wrongword = 0;

		setSpellCheckListener();

		int emptyWordsSet = 0;
		List<String> possiblyValidWords = new ArrayList<String>();
		for (Map.Entry<TAGGER_TAGS, Set<String>> tgWrd : taggedWords.entrySet()) {
			// ignoring the foreign words for iterations as we know it is wrong
			// any way, depending upon the pos-tagger model we learned on
			if (tgWrd.getKey() == TAGGER_TAGS.FW) {
				continue;
			}
			Iterator<String> wordsItrt = tgWrd.getValue().iterator();
			while (wordsItrt.hasNext()) {
				String word = wordsItrt.next();
				if (!this.spellCheckListener.isCorrect(word)) {
					String[] validWrdPsbly = getReducedWords(word);
					for (String t : validWrdPsbly) {
						if (t.length() > 1
								&& this.spellCheckListener.isCorrect(t)) {
							possiblyValidWords.add(t);
						}
					}
					if (possiblyValidWords.size() == 0) {
						wrongword += 1;
						wordsItrt.remove();
					}
				}
			}

			tgWrd.getValue().addAll(possiblyValidWords);
			possiblyValidWords.clear();

			emptyWordsSet += (tgWrd.getValue().size() == 0 ? 1 : 0);
		}

		// if all words are wrong and only foreign word is remaining, clear the
		// map
		if (taggedWords.size() - emptyWordsSet <= 1) {
			if (taggedWords.containsKey(TAGGER_TAGS.FW)
					|| isBelowAccuracy(fw + wrongword, totalWordCount)) {
				// so that only word is foreign word
				taggedWords.clear();
			}
		}
	}

	/**
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void setSpellCheckListener() throws FileNotFoundException,
			IOException {
		this.spellCheckListener = this.spellCheckListener == null ? loadFromCachedDictionary()
				: this.spellCheckListener;
	}

	private JazzySpellCheckerListener loadFromCachedDictionary()
			throws FileNotFoundException, IOException {
		JazzySpellCheckerListener jazzySpellChecker = null;
		synchronized (DICTIONARIES) {
			if (!DICTIONARIES.containsKey(this.dictionaryType)) {
				System.out.println(Thread.currentThread().getName()
						+ " :: Try loading Dictionary");
				DICTIONARIES.put(dictionaryType, JazzySpellCheckerListener
						.getJazzSpellCheckDictionary(this.dictionaryType
								.getPath()));
			}
		}
		jazzySpellChecker = new JazzySpellCheckerListener();
		jazzySpellChecker.addDitionary(DICTIONARIES.get(this.dictionaryType));
		return jazzySpellChecker;
	}

	private boolean isBelowAccuracy(int fw, int totalWordCount) {
		return ((totalWordCount - fw) * 1f) / totalWordCount < this.accuarcy;
	}

	private synchronized MaxentTagger loadTaggerFromCahche() {
		synchronized (POS_TAGGER_CAHCE) {
			if (!POS_TAGGER_CAHCE.containsKey(this.posTaggerModelLocation)) {
				System.out.println(Thread.currentThread().getName()
						+ " :: loading posTagger model");
				POS_TAGGER_CAHCE.put(this.posTaggerModelLocation,
						new MaxentTagger(this.posTaggerModelLocation));
			}
		}
		return POS_TAGGER_CAHCE.get(this.posTaggerModelLocation);
	}

	public String getPosTaggerModelLocation() {
		return posTaggerModelLocation;
	}

	public void setPosTaggerModelLocation(String posTaggerModelLocation) {
		this.posTaggerModelLocation = posTaggerModelLocation;
	}

	public DICTIONARY_TYPE getDictionaryType() {
		return dictionaryType;
	}

	public void setDictionaryType(DICTIONARY_TYPE dictionaryType) {
		this.dictionaryType = dictionaryType;
	}

	public float getAccuarcy() {
		return accuarcy;
	}

	/**
	 * its value should be between one and zero. This is used to perform the
	 * match with the dictionary keywords for a given dictionary.
	 * 
	 * @param accuarcy
	 *            one means exact match or partial threshold math required.
	 */
	public void setAccuarcy(float accuarcy) {
		this.accuarcy = Math.abs(accuarcy) > 1 ? 100f / Math.abs(accuarcy)
				: Math.abs(accuarcy);
	}

	/**
	 * 
	 */
	public static void clearCachedData() {
		DICTIONARIES.clear();
		POS_TAGGER_CAHCE.clear();
	}
}
