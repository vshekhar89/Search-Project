/**
 * 
 */
package util;

import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import constants.ConfigConstant;
import constants.ConfigConstant.TAGGER_TAGS;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.WordLemmaTag;
import edu.stanford.nlp.ling.WordTag;
import edu.stanford.nlp.process.Morphology;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

/**
 * @author sumit
 *
 */
public class POSTaggerUtil {
	private POSTaggerUtil() {

	}

	public static String lemmatize(String word,String tag, boolean lowercase){
		return Morphology.lemmaStatic(word, tag, lowercase);
	}
	
	public static WordLemmaTag lemmatize(WordTag wordTag){
		return Morphology.lemmatizeStatic(wordTag);
	}
	
	public static MaxentTagger getPOSTaggerFrom(String pModelTaggerLocation) {
		return new MaxentTagger(pModelTaggerLocation);
	}

	public static Map<ConfigConstant.TAGGER_TAGS, Set<String>> getAllTaggedWordPair(
			MaxentTagger tagger, String text) {
		return getAllTaggedWordPair(tagger, text, null);
	}

	public static Map<ConfigConstant.TAGGER_TAGS, Set<String>> getAllFilteredTaggedWordPair(
			MaxentTagger tagger, String text,
			Set<ConfigConstant.TAGGER_TAGS> filtered) {
		return getAllTaggedWordPair(tagger, text, filtered);
	}

	/**
	 * This will only extract nonun, adjectives and foreign words if any. Ignore
	 * other tags
	 * 
	 * @param tagger
	 * @param line
	 * @return
	 */
	private static Map<ConfigConstant.TAGGER_TAGS, Set<String>> getAllTaggedWordPair(
			MaxentTagger tagger, String text, Set<TAGGER_TAGS> filtered) {

		List<List<HasWord>> sentences = MaxentTagger
				.tokenizeText(new StringReader(text));
		// System.out.println(tagger.tagString(text));
		Map<ConfigConstant.TAGGER_TAGS, Set<String>> taggWordsPair = new HashMap<ConfigConstant.TAGGER_TAGS, Set<String>>();
		for (List<HasWord> sentence : sentences) {
			List<TaggedWord> tSentence = tagger.tagSentence(sentence);
			// System.out.println(Sentence.listToString(tSentence, false));
			for (TaggedWord tags_meta : tSentence) {
				if (filtered == null || filtered.size() == 0
						|| filtered.contains(getTagFrom(tags_meta.tag()))) {
					addToTaggedWordPair(taggWordsPair, tags_meta);
					/*
					 * System.out.println("Tags::" + tags_meta.tag() +
					 * ", Word::" + tags_meta.word() + " ,Value::" +
					 * tags_meta.value());
					 */
				}
			}
		}
		return taggWordsPair;
	}

	private static void addToTaggedWordPair(
			Map<TAGGER_TAGS, Set<String>> taggWordsPair, TaggedWord tags_meta) {
		TAGGER_TAGS tag = getTagFrom(tags_meta.tag());
		if (!taggWordsPair.containsKey(tag)) {
			taggWordsPair.put(tag, new HashSet<String>());
		}
		taggWordsPair.get(tag).add(tags_meta.word());
	}

	private static TAGGER_TAGS getTagFrom(String tag) {
		TAGGER_TAGS tag_e = TAGGER_TAGS.XXXX;
		try {
			tag_e = TAGGER_TAGS.valueOf(tag);
		} catch (IllegalArgumentException ex) {
			// setting the default
		}
		return tag_e;
	}

	
}
