/**
 * 
 */
package practicetest;

import util.POSTaggerUtil;
import constants.ConfigConstant;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

/**
 * @author sumit
 *
 */
public class TestNoun {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String text = "normal other related corporate happy auto half hour rate service dealer garage guy experience Labor maintenance inspection n't approx ";

		MaxentTagger tagger = POSTaggerUtil
				.getPOSTaggerFrom(ConfigConstant.POS_TAGGER_MODEL.ENGLISH_FAST_SLIGHT_INACCURATE
						.getPath());
		System.out.println(POSTaggerUtil.getAllTaggedWordPair(tagger, text));
	}

}
