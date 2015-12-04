package spellchecker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.swabunga.spell.engine.GenericSpellDictionary;
import com.swabunga.spell.engine.SpellDictionary;
import com.swabunga.spell.engine.SpellDictionaryHashMap;
import com.swabunga.spell.engine.Word;
import com.swabunga.spell.event.SpellCheckEvent;
import com.swabunga.spell.event.SpellCheckListener;
import com.swabunga.spell.event.SpellChecker;
import com.swabunga.spell.event.StringWordTokenizer;
import com.swabunga.spell.event.TeXWordFinder;

public class JazzySpellCheckerListener implements SpellCheckListener {

	List<String> misspelledWord;
	SpellChecker spellChecker;

	public void spellingError(SpellCheckEvent event) {
		event.ignoreWord(true);
		this.misspelledWord.add(event.getInvalidWord());
	}

	public JazzySpellCheckerListener() {
		this.misspelledWord = new ArrayList<>();
	}

	public JazzySpellCheckerListener(String pathToDictionary)
			throws IOException {
		this();
		init(pathToDictionary);
	}

	private void init(String pathToDictionary) throws IOException {
		this.spellChecker = /*
							 * new SpellChecker(new GenericSpellDictionary( new
							 * File(pathToDictionary)));
							 */
		new SpellChecker(getJazzSpellCheckDictionary(pathToDictionary));
		this.spellChecker.addSpellCheckListener(this);
	}

	/**
	 * 
	 * @param text
	 * @return - unmodifiable list of misspelled words
	 */
	public List<String> getMisspelledWord(String text) {
		StringWordTokenizer strTokenizer = new StringWordTokenizer(text,
				new TeXWordFinder());
		this.spellChecker.checkSpelling(strTokenizer);
		return Collections.unmodifiableList(this.misspelledWord);
	}

	/**
	 * 
	 * @param text
	 *            - suggestions to get
	 * @param maxCostValueScore
	 *            - the cost score to be used to decide the suggested word.
	 *            Other Words will be thrown away for more costs.
	 * @return
	 */

	public List<String> getSuggestions(String text, int maxCostValueScore) {
		List<String> suggestedWord = new ArrayList<>();
		@SuppressWarnings("unchecked")
		List<Word> suggestsObtained = this.spellChecker.getSuggestions(text,
				maxCostValueScore);
		for (Word word : suggestsObtained) {
			suggestedWord.add(word.getWord());
		}
		return suggestedWord;
	}

	public boolean isCorrect(String text) {
		return this.spellChecker.isCorrect(text);
	}

	public SpellChecker getSpellChecker() {
		return this.spellChecker;
	}

	/**
	 * Add multiple dictionary
	 * 
	 * @param dictionary
	 */
	public void addDitionary(SpellDictionary dictionary) {
		defaullSpellCheker();
		this.spellChecker.addDictionary(dictionary);
	}

	public void addDictionary(String pathtoDictionary)
			throws FileNotFoundException, IOException {
		defaullSpellCheker();
		this.spellChecker.addDictionary(new SpellDictionaryHashMap(new File(
				pathtoDictionary)));
	}

	/**
	 * 
	 */
	private void defaullSpellCheker() {
		if (this.spellChecker == null) {
			this.spellChecker = new SpellChecker();
			this.spellChecker.addSpellCheckListener(this);
		}
	}

	/**
	 * 
	 * @param line
	 * @return - updated correct text line, with best suggestions
	 */
	public String getCorrectedLine(String line) {
		List<String> misSpelledWords = getMisspelledWord(line);

		for (String misSpelledWord : misSpelledWords) {
			List<String> suggestions = getSuggestions(misSpelledWord, 0);
			System.out.println("suugested::" + suggestions);
			if (suggestions.size() == 0)
				continue;
			String bestSuggestion = suggestions.get(0);
			line = line.replace(misSpelledWord, bestSuggestion);
		}
		return line;
	}

	/**
	 * 
	 * @param line
	 * @return
	 */
	public String getCorrectedText(String line) {
		StringBuilder builder = new StringBuilder();
		String[] tempWords = line.split("\\s+");
		for (String tempWord : tempWords) {
			if (!spellChecker.isCorrect(tempWord)) {
				List<String> suggestions = getSuggestions(tempWord, 0);
				if (suggestions.size() > 0) {
					builder.append(suggestions.get(0));// adding the text with
														// least cost, may be
														// not right way to do
														// it.
				} else
					builder.append(tempWord);
			} else {
				builder.append(tempWord);
			}
			builder.append(" ");
		}
		return builder.toString().trim();
	}

	public void rest() {
		this.misspelledWord.clear();
	}

	/**
	 * 
	 * @param pathToDictionary
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static SpellDictionary getJazzSpellCheckDictionary(
			String pathToDictionary) throws FileNotFoundException, IOException {
		return new SpellDictionaryHashMap(new File(pathToDictionary));
	}
}