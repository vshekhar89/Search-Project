/**
 * 
 */
package practicetest;

import java.io.IOException;

import util.LuceneNLPUtil;
import constants.ConfigConstant.JOB_EXECUTION_STATUS;
import constants.ConfigConstant.TAGGER_TAGS;

/**
 * @author sumit
 *
 */
public class WordDictionaryTest {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		// languageTool();
		// luceneLangugeTool();
		// System.out.println(TAGGER_TAGS.valueOf("AR"));
		String text = "zillionth";// abundantly
		System.out.println(LuceneNLPUtil.getRemovedStopAndStem(text,
				LuceneNLPUtil.getDefaultEnglishStopWordList()));
		// System.out.println(Integer.valueOf(""));
		JOB_EXECUTION_STATUS status = JOB_EXECUTION_STATUS.SUCCESS;
		System.out.println(JOB_EXECUTION_STATUS.SUCCESS == status);
		System.out.println(text.replaceAll("[\\p{P}]+", " "));
	}

	private static void luceneLangugeTool() {

	}

	/**
	 * @throws IOException
	 */
	/*
	 * private static void languageTool() throws IOException { JLanguageTool
	 * langTool = new JLanguageTool(Language.ENGLISH);
	 * langTool.activateDefaultPatternRules(); for (Rule rule :
	 * langTool.getAllRules()) { if (!rule.isSpellingRule()) {
	 * langTool.disableRule(rule.getId()); } } //
	 * langTool.setListUnknownWords(true); String text = "awesome helllo";
	 * List<RuleMatch> matches = langTool.check(text);
	 * //System.out.println(langTool.sentenceTokenize(text)); for (RuleMatch
	 * match : matches) { System.out.println("Potential typo at line " +
	 * match.getLine() + ", column " + match.getColumn() + ": " +
	 * match.getMessage()); System.out.println("Suggested correction(s): " +
	 * match.getSuggestedReplacements()); System.out.println(match.getRule()); }
	 * // System.out.println(langTool.getUnknownWords());
	 * //System.out.println(langTool.getAllRules()); matches =
	 * langTool.check(text, true, ParagraphHandling.NORMAL); for (RuleMatch
	 * match : matches) { System.out.println("Potential typo at line " +
	 * match.getLine() + ", column " + match.getColumn() + ": " +
	 * match.getMessage()); System.out.println("Suggested correction(s): " +
	 * match.getSuggestedReplacements()); System.out.println(match.getRule()); }
	 * }
	 */
}
