/**
 * 
 */
package util;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.KStemFilter;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;

/**
 * @author sumit
 *
 */
public final class LuceneNLPUtil {

	private static final int DEFAULT_WORD_TOKEN_FILTER_FLAG = WordDelimiterFilter.STEM_ENGLISH_POSSESSIVE
			//| WordDelimiterFilter.CATENATE_WORDS
			//| WordDelimiterFilter.SUBWORD_DELIM
			|WordDelimiterFilter.GENERATE_WORD_PARTS
			|WordDelimiterFilter.PRESERVE_ORIGINAL
			;

	private LuceneNLPUtil() {

	}

	public static TokenStream removeStopAndStem(final Reader reader,
			CharArraySet stopSet) throws IOException {
		
		Tokenizer tk = new StandardTokenizer();
		tk.setReader(reader);
		TokenStream result = tk;

		result = new WordDelimiterFilter(result, DEFAULT_WORD_TOKEN_FILTER_FLAG,
				null);
		result = new StandardFilter(result);
		result = new StopFilter(result, stopSet);

		// stemmer part
		// new PorterStemFilter(result);
		// new KStemFilter(result)
		result = new KStemFilter(result);// snowball filter,synonym
											// filter,WordDelimeter
											// filter,KStemFilter

		// mixing german, french stemmer, these are slowr version
		// result = new SnowballFilter(result, new German2Stemmer());
		// result = new SnowballFilter(result, new FrenchStemmer());

		return result;
	}

	public static TokenStream removeStopAndStem(final String text,
			CharArraySet stopSet) throws IOException {
		return removeStopAndStem(new StringReader(text), stopSet);
	}

	public static List<String> getRemovedStopAndStem(final String text,
			CharArraySet stopSet) throws IOException {

		TokenStream tokenStream = removeStopAndStem(text, stopSet);
		return getWithoutStopAndDoSteming(tokenStream);
	}

	public static List<String> getRemovedStopAndStem(final Reader reader,
			CharArraySet stopSet) throws IOException {
		TokenStream tokenStream = removeStopAndStem(reader, stopSet);
		return getWithoutStopAndDoSteming(tokenStream);
	}

	/**
	 * @param tokenStream
	 * @return
	 * @throws IOException
	 */
	private static List<String> getWithoutStopAndDoSteming(
			TokenStream tokenStream) throws IOException {
		CharTermAttribute charTermAttribute = tokenStream
				.addAttribute(CharTermAttribute.class);
		tokenStream.reset();

		List<String> tokens = new ArrayList<String>();
		while (tokenStream.incrementToken()) {
			tokens.add(charTermAttribute.toString());
		}
		return tokens;
	}

	public static CharArraySet getDefaultEnglishStopWordList() {
		return EnglishAnalyzer.getDefaultStopSet();
	}
	
	public static boolean isAllASCII(String word) {
		// word should contain only a-z A-Z 0-9 _ .
		return word.trim().replaceAll("\\p{ASCII}*", "").length() == 0;
	}
}
