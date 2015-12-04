/**
 * 
 */
package constants;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author sumit
 *
 */
public class ConfigConstant {

	public static final String NEW_LINE_CHAR = System
			.getProperty("line.separator");
	public static final String SPACE = " ";
	public static final String UNDERSCORE = "_";
	public static final String PATTERN_ENGLISH_ALPHABET_TOKENIZE = "\\p{L}[\\p{L}\\p{P}]*\\p{L}";
	public static final String COMMA = ",";

	public enum TAGGER_TAGS {
		// adjectives
		JJ, JJR, JJS,
		// noun's
		NN, NNS, NNP, NNPS,
		// Foreign Word
		FW,
		// adverb's
		RB, RBR, RBS, RP,
		// symbols
		SYM,
		// verbs
		VB, VBZ, VBD, VBP, VBN, VBG,
		// unknown tag
		XXXX
	};

	// optimization parameters
	// configurable constants
	public static final int TOP_K_DOCMENTS = 1000;
	public static final int NUMBER_OF_ACTIVE_THREADS = 10; // keep it more than
															// one
	public static final long THREAD_POOL_SLEEP_TIME = 1000; // in msec
	public static final int INDEX_FLUSH_TIME = 1000;
	public static final double INDEX_BUFFER_SIZE_IN_MB = 100.0;

	public static final double TOPIC_MODEL_APLHA_T = 0.01;
	public static final double TOPIC_MODEL_BETA = 0.01;
	public static final int TOPIC_COUNTS = 100;

	public static final Charset DEFAULT_CHAR_SET = StandardCharsets.UTF_8;
	// file optimization parameters
	public static final int HASH_CODE_POOL_SIZE = 197; // prime numbers will be
														// good: 167, 173, 179,
														// 181, 191, 193, 197,
														// 199,223

	// enum for the status of process
	public enum JOB_EXECUTION_STATUS {
		WAITING, BLOCKED, NOT_SATRTED, STARTAED, IN_PROGRESS, COMPLETED, INITIALIZED, FAILED, SUCCESS, SUCCESS_WITH_ERROR;
	};

	public enum JSON_FIELD {
		BUSINESS_ID("business_id"), REVIEW("review"), TEXT("text"), RECORD_TYPE(
				"record_type"), TIPS("tips"), TOPICS_MODEL("topics_model"), WORD(
				"word"), FREQUENCY("freq"), TOPIC_PROB("topic_prob");
		private String fileldName;

		private JSON_FIELD(String fld) {
			this.fileldName = fld;
		}

		public String getFieldName() {
			return this.fileldName;
		}

	}

	/*
	 * change the following parameters
	 */

	// file locations
	// input file locations
	// L:/information retrieval/projects/yelp_dataset_challenge_academic_dataset
	// change the following
	public static final String PROJECT_BASE_LOCATION = "/Volumes/Hoth/projects/";
	// temporary output paths
	// change is mandatory if platform changes, default base path
	public static final String TEMP_DIRECTORY = PROJECT_BASE_LOCATION+"temp/";// change

	// following may not require changes
	// input data sets location
	public static final String DATA_SET_LOCATION = PROJECT_BASE_LOCATION
			+ "yelp_dataset_challenge_academic_dataset/";
	public static final String DICTIONARY_DATA_DIR_PREFIX = PROJECT_BASE_LOCATION
			+ "dictionary/";
	// index creation location
	public static final String INDEX_OUTPUT_LOCATION = PROJECT_BASE_LOCATION
			+ "index/";

	// post tagger model locations
	public static final String POSTAGGER_SIMPLE_MODEL_LOCATION = PROJECT_BASE_LOCATION
			+ "standford-posttagger-models/models/";

	public enum POS_TAGGER_MODEL {
		ENGLISH_MOST_ACCUARTE_BUT_SLOW("english-bidirectional-distsim.tagger"), ENGLISH_FAST_SLIGHT_INACCURATE(
				"english-left3words-distsim.tagger");
		String path;

		POS_TAGGER_MODEL(String pPath) {
			path = pPath;
		}

		public String getPath() {
			return POSTAGGER_SIMPLE_MODEL_LOCATION + path;
		}
	};

	// change the path part of the files if require
	public enum DICTIONARY_TYPE {
		ENGLISH("en.txt", "_en"), GERMAN("de_neu.dic", "_de"), FRENCH("fr.dic",
				"_fr"), GREEK("greek_open.dic", "_greek"), ITALIAN("it.dic",
				"_it"), UNKNOWN("", "_unknown");

		private String path;
		private String suffix;

		private DICTIONARY_TYPE(String pPath, String pSuffix) {
			path = pPath;
			suffix = pSuffix;
		}

		public String getPath() {
			return DICTIONARY_DATA_DIR_PREFIX + path;
		}

		public String getSuffix() {
			return suffix;
		}
	};

	// may not require to change
	public static final String REVIEW_LOCATIONS = DATA_SET_LOCATION
			+ "yelp_academic_dataset_review.json";
	public static final String TIPS_LOCATIONS = DATA_SET_LOCATION
			+ "yelp_academic_dataset_tip.json";

	// change if required
	public static final String TEMP_REVIEW_TIPS_OUT_DIR = TEMP_DIRECTORY
			+ "temp-review-tips/";
	public static final String INDEX_OUTPUT_RAW_REVIEW_TIPS_OUT_DIR = INDEX_OUTPUT_LOCATION
			+ "review-tips-raw/";
	public static final String TEMP_POS_TAGGER_REV_TIPS_OUT_DIR = TEMP_DIRECTORY
			+ "temp-pos-tagger/";
	public static final String TEMP_VOCABULARY_OUTPUT_DIR = TEMP_DIRECTORY
			+ "temp-vocabulary/";
	public static final String TEMP_ML_DIR = TEMP_DIRECTORY + "temp-ml-data/";
	public static final String TEMP_REVIEW_TIPS_ML_TOPIC_DATA_OUT_LOCATIONS = TEMP_ML_DIR
			+ "topic-model-review_tips/";
	public static final String TEMP_FINAL_ML_DATA_DIR = TEMP_DIRECTORY
			+ "final-data-sets/";
	public static final String TEMP_FINAL_ML_DATA_SETS_DIR = TEMP_FINAL_ML_DATA_DIR
			+ "ml-data/";
	public static String TEMP_CORRECTED_VOCABULARY_OUT_PATH = TEMP_FINAL_ML_DATA_DIR
			+ "corrected_vocabulary/";

	// don't change
	public static final String TEMP_REVIEW_TIPS_TEXT_LOCATION = TEMP_REVIEW_TIPS_OUT_DIR
			+ "review_tips_texts_";

	public static final String TEMP_REVIEW_TIPS_ML_TEXT_LOCATION = TEMP_REVIEW_TIPS_ML_TOPIC_DATA_OUT_LOCATIONS
			+ "ml_";

	public static final String TEMP_RAW_ORIGINAL_VOCABULRAY_TEXT_LOCATIONS = TEMP_VOCABULARY_OUTPUT_DIR
			+ "vocabulary_original_raw.txt";

	public static final String TEMP_BASIC_REVIEW_TIPS_ML_DATA_SETS_LOCATION = TEMP_FINAL_ML_DATA_DIR
			+ "noun_basic_review_tips_ml_data_Sets.csv";

	public static final String TEMP_VALID_FILE_NAME_PREFIX = "valid_";
	public static final String TEMP_INVALID_FILE_NAME_PREFIX = "invalid_";

}
