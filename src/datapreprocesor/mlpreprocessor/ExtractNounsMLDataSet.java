/**
 * 
 */
package datapreprocesor.mlpreprocessor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.json.JSONObject;

import util.FileUtils;
import util.JsonUtil;
import util.POSTaggerUtil;
import util.ThreadPoolUtil;
import constants.ConfigConstant;
import constants.ConfigConstant.JOB_EXECUTION_STATUS;
import constants.ConfigConstant.JSON_FIELD;
import constants.ConfigConstant.TAGGER_TAGS;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

/**
 * Extracts only nouns and generate the data sets for ml based learning. output
 * format:: <bussiness_id>,<noun-word1>...<noun-word_n> first line contains the
 * header: #bussines_id,NOUN_1,NOUN_2,NOUN_3...,NOUN_n Note: when all nouns are
 * exausted:it will generate the empty string, in the remaining column, which is
 * like missing.
 * 
 * All words are lowered case, thereby provides normalization on words.
 * 
 * It outputs multiple files, corresponding to each input source file in
 * previous chain of pre-processing.
 * 
 * @author sumit
 *
 */
public class ExtractNounsMLDataSet {

	private static final int NOUN_COLUMN_SIZE = 6;// just a guess

	private static final int DUMP_THRESHOLD_SIZE = 3000;

	private static final String NOUN_COLUMN_PREFIX = "NOUN_";

	private final static Set<ConfigConstant.TAGGER_TAGS> FILTERED_TAGS = new HashSet<>();

	static {
		// noun
		FILTERED_TAGS.add(TAGGER_TAGS.NN);
		FILTERED_TAGS.add(TAGGER_TAGS.NNP);
		FILTERED_TAGS.add(TAGGER_TAGS.NNPS);
		FILTERED_TAGS.add(TAGGER_TAGS.NNS);
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		FileUtils.makeDirectory(ConfigConstant.TEMP_FINAL_ML_DATA_DIR);
		FileUtils.makeDirectory(ConfigConstant.TEMP_FINAL_ML_DATA_SETS_DIR);

		// where the temp post-tagger output
		final String inputSrc = ConfigConstant.TEMP_POS_TAGGER_REV_TIPS_OUT_DIR;
		// name of the output final location
		// final String outputSrc =
		// ConfigConstant.TEMP_BASIC_REVIEW_TIPS_ML_DATA_SETS_LOCATION;

		final int THREAD_POOL_SIZE = ConfigConstant.NUMBER_OF_ACTIVE_THREADS;

		MaxentTagger tagger = POSTaggerUtil
				.getPOSTaggerFrom(ConfigConstant.POS_TAGGER_MODEL.ENGLISH_FAST_SLIGHT_INACCURATE
						.getPath());

		// read all file one by one.
		ExecutorService threadPool = ThreadPoolUtil
				.getThreadPool(THREAD_POOL_SIZE);

		for (String fileName : FileUtils.getAllFiles(inputSrc)) {

			processFile(FileUtils.getFullPath(inputSrc, fileName),
					ConfigConstant.TEMP_FINAL_ML_DATA_SETS_DIR, threadPool,
					tagger);
		}

		ThreadPoolUtil.waitForThreadsToFinish(threadPool,
				ConfigConstant.THREAD_POOL_SLEEP_TIME);

		ThreadPoolUtil.cleanUpGarbageRequest();

	}

	private static void processFile(String inputFile, String outPutDir,
			final ExecutorService threadPool, MaxentTagger tagger) {

		String outFileIndex = inputFile
				.substring(inputFile.lastIndexOf("_") + 1);

		NounExtractorMLData thread = new NounExtractorMLData(inputFile,
				FileUtils.getFullPath(outPutDir,
						ConfigConstant.TEMP_VALID_FILE_NAME_PREFIX
								+ outFileIndex), tagger);

		ThreadPoolUtil.submitJob(threadPool, thread);
	}

	static class NounExtractorMLData implements
			Callable<ConfigConstant.JOB_EXECUTION_STATUS> {
		private String inputFile;
		String outputFile;
		MaxentTagger tagger;

		// optimization
		BufferedWriter wrtr;

		NounExtractorMLData(String pInputFile, String outFile,
				MaxentTagger pTagger) {
			this.inputFile = pInputFile;
			this.outputFile = outFile;
			this.tagger = pTagger;
		}

		@Override
		public JOB_EXECUTION_STATUS call() throws Exception {
			JOB_EXECUTION_STATUS sts = JOB_EXECUTION_STATUS.IN_PROGRESS;
			try (BufferedReader rdr = new BufferedReader(new FileReader(
					this.inputFile))) {
				String line = null;

				Map<String, List<List<String>>> dumpingResultSet = new LinkedHashMap<>();
				int recordSize = 0;
				// priniting header
				printHeader();
				while ((line = rdr.readLine()) != null) {
					JSONObject posTaggerJson = JsonUtil.getJSONObject(line);
					Map<TAGGER_TAGS, Set<String>> taggedWords = POSTaggerUtil
							.getAllFilteredTaggedWordPair(tagger, posTaggerJson
									.getString(JSON_FIELD.TEXT.getFieldName()),
									FILTERED_TAGS);

					List<String> tempResult = new ArrayList<>();
					// sort all the words and normalize them by converting
					// them to the lowercase

					taggedWords.values().forEach(
							e -> {
								e.stream().forEachOrdered(
										t -> tempResult.add(t.toLowerCase()));
							});

					Collections.sort(tempResult);
					addToLocalCahe(dumpingResultSet, posTaggerJson, tempResult);
					recordSize++;

					if (recordSize >= DUMP_THRESHOLD_SIZE) {
						dumpToFile(dumpingResultSet);
						dumpingResultSet.clear();
						recordSize = 0;
					}
				}

				// dump remaining results
				dumpToFile(dumpingResultSet);
				dumpingResultSet.clear();

				sts = JOB_EXECUTION_STATUS.SUCCESS;
			} catch (Exception ex) {
				ex.printStackTrace();
			} finally {
				if (sts != JOB_EXECUTION_STATUS.SUCCESS) {
					ThreadPoolUtil.dumpStackTrace(Thread.currentThread());
					sts = JOB_EXECUTION_STATUS.SUCCESS_WITH_ERROR;
				}
				System.out.println("Completed noun extraction ml-data sets:: ["
						+ this.inputFile + "], Status:: " + sts);
				releaseResource();
			}
			return sts;
		}

		private void printHeader() throws IOException {

			this.wrtr = this.wrtr != null ? this.wrtr
					: createDataResource(this.outputFile);

			StringBuilder bldr = new StringBuilder();
			int columnCount = NOUN_COLUMN_SIZE;
			while (columnCount > 0) {
				bldr.append(NOUN_COLUMN_PREFIX + ConfigConstant.UNDERSCORE
						+ (NOUN_COLUMN_SIZE - columnCount + 1)
						+ (columnCount != 1 ? ConfigConstant.COMMA : ""));
				columnCount--;
			}

			FileUtils.writeToFileWithNewLine(this.wrtr, "BUSSINESS_ID"
					+ ConfigConstant.COMMA + bldr.toString());

		}

		private void releaseResource() throws IOException {
			try {
				if (this.wrtr != null) {
					this.wrtr.close();
				}
			} finally {
				ThreadPoolUtil.cleanUpGarbageRequest();
			}

		}

		private void dumpToFile(Map<String, List<List<String>>> dumpingResultSet)
				throws IOException {
			if (dumpingResultSet.size() < 1) {
				return;
			}
			this.wrtr = this.wrtr != null ? this.wrtr
					: createDataResource(this.outputFile);

			for (Map.Entry<String, List<List<String>>> d : dumpingResultSet
					.entrySet()) {
				for (List<String> t : d.getValue()) {
					StringBuilder bldr = new StringBuilder();
					Iterator<String> s = t.iterator();
					int countColumn = 1;
					while (s.hasNext()) {
						if (countColumn == NOUN_COLUMN_SIZE) {
							bldr.append(s.next());
							FileUtils.writeToFileWithNewLine(
									this.wrtr,
									d.getKey() + ConfigConstant.COMMA
											+ bldr.toString());
							countColumn = 1;
							bldr.delete(0, bldr.length());
							continue;
						}
						bldr.append(s.next() + ConfigConstant.COMMA);
						countColumn++;
					}
					// pending data to write
					if (bldr.length() != 0) {
						// append remaining comma only
						while (countColumn < NOUN_COLUMN_SIZE) {
							bldr.append(ConfigConstant.COMMA);
							countColumn++;
						}
						// dump to the file
						FileUtils.writeToFileWithNewLine(this.wrtr, d.getKey()
								+ ConfigConstant.COMMA + bldr.toString());
					}
				}
			}
		}

		private BufferedWriter createDataResource(String outputFile)
				throws IOException {
			return new BufferedWriter(new FileWriter(outputFile));
		}

		private void addToLocalCahe(
				Map<String, List<List<String>>> dumpingResultSet,
				JSONObject posTaggerJson, List<String> tempResult) {

			String bussinessID = posTaggerJson.getString(JSON_FIELD.BUSINESS_ID
					.getFieldName());

			if (!dumpingResultSet.containsKey(bussinessID)) {
				dumpingResultSet
						.put(bussinessID, new ArrayList<List<String>>());
			}
			dumpingResultSet.get(bussinessID).add(tempResult);
		}
	};
}
