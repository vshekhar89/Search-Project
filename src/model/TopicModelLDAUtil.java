/**
 * 
 */
package model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import beans.TopicModelLDABean;
import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.IDSorter;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelSequence;
import constants.ConfigConstant;

/**
 * This class is not multi-threaded safe.
 * 
 * @author sumit
 *
 */
public final class TopicModelLDAUtil {

	private static Map<Thread, Pipe> LOCAL_CACHE = Collections.synchronizedMap(new HashMap<>());

	public static ParallelTopicModel getEstimatedTopicModel(
			InstanceList instances, double alphaSum, double beta,
			int numOfTopics) throws IOException {

		ParallelTopicModel model = getParallelTopicModel(alphaSum, beta,
				numOfTopics);
		model.addInstances(instances);
		// Use two parallel samplers, which each look at one half the corpus and
		// combine
		// statistics after every iteration.
		// model.setNumThreads(2);
		// model.setSymmetricAlpha(b);
		// Run the model for 50 iterations and stop (this is for testing only,
		// for real applications, use 1000 to 2000 iterations)
		model.setNumIterations(10);
		// model.setTopicDisplay(0, 0);
		model.estimate();

		return model;
	}

	/**
	 * @param alphaSum
	 * @param beta
	 * @param numOfTopics
	 * @return
	 */
	public static ParallelTopicModel getParallelTopicModel(double alphaSum,
			double beta, int numOfTopics) {
		ParallelTopicModel model = new ParallelTopicModel(numOfTopics,
				alphaSum, beta);
		model.setNumIterations(10);

		return model;
	}

	public static InstanceList getNewInstanceContainer(Pipe pipe) {
		if (pipe == null) {
			if (!LOCAL_CACHE.containsKey(Thread.currentThread())) {
				LOCAL_CACHE.put(Thread.currentThread(), new SerialPipes(
						getListOfPipes()));
			}
			pipe = LOCAL_CACHE.get(Thread.currentThread());
		}
		return new InstanceList(pipe);
	}

	private static List<Pipe> getListOfPipes() {
		ArrayList<Pipe> DEFAULT_PIPE_LIST = new ArrayList<Pipe>();

		// Pipes: lowercase, tokenize, remove stopwords, map to features
		//DEFAULT_PIPE_LIST.add(new CharSequenceLowercase()); don't change the case
		DEFAULT_PIPE_LIST.add(new CharSequence2TokenSequence(Pattern
				.compile(ConfigConstant.PATTERN_ENGLISH_ALPHABET_TOKENIZE)));//
		DEFAULT_PIPE_LIST.add(new TokenSequenceRemoveStopwords());
		DEFAULT_PIPE_LIST.add(new TokenSequence2FeatureSequence());
		return DEFAULT_PIPE_LIST;
	}

	public static void addToInstanceConatiner(InstanceList instances,
			Object data, Object target, Object name, Object source) {
		instances.addThruPipe(new Instance(data, target, name, source));
	}

	public static void addToInstanceConatiner(InstanceList instances,
			Iterator<Instance> itrt) {
		instances.addThruPipe(itrt);
	}

	public static Set<TopicModelLDABean> getWordToTopicDistribution(
			ParallelTopicModel model, int recrdIndx) {

		Set<TopicModelLDABean> finalDistribution = new HashSet<TopicModelLDABean>();
		if (recrdIndx >= model.getData().size()) {
			System.out.println("Invalid Record Index Passed !!");
			return finalDistribution;
		}

		FeatureSequence tokens = (FeatureSequence) model.getData().get(
				recrdIndx).instance.getData();
		LabelSequence topics = model.getData().get(recrdIndx).topicSequence;
		Alphabet dataAlphabet = model.getData().get(recrdIndx).instance
				.getDataAlphabet();
		double[] topicDistribution = model.getTopicProbabilities(recrdIndx);
		// Get an array of sorted sets of word ID/count pairs, across the entire
		// data sets not to a particular instance
		ArrayList<TreeSet<IDSorter>> topicSortedWords = model.getSortedWords();

		for (int position = 0; position < tokens.getLength(); position++) {

			if (ConfigConstant.SPACE.equals(((String) dataAlphabet
					.lookupObject(tokens.getIndexAtPosition(position))).trim()))
				continue;

			TopicModelLDABean topicmodel = new TopicModelLDABean();
			topicmodel.setWord((String) dataAlphabet.lookupObject(tokens
					.getIndexAtPosition(position)));

			topicmodel.setTopicDistribution(Double.valueOf(String.format(
					"%.4f",
					topicDistribution[topics.getIndexAtPosition(position)])));
			
			topicmodel.setTopic("" + topics.getIndexAtPosition(position));
			Iterator<IDSorter> iterator = topicSortedWords.get(
					topics.getIndexAtPosition(position)).iterator();
			while (iterator.hasNext()) {
				IDSorter idCountPair = iterator.next();
				if (topicmodel.getWord()
						.equals((String) dataAlphabet.lookupObject(idCountPair
								.getID()))) {
					topicmodel.setWeight(idCountPair.getWeight());
					break;
				}
			}
			finalDistribution.add(topicmodel);

		}

		return finalDistribution;
	}

	public static Set<TopicModelLDABean> getInstancesWordToTopicDistribution(
			ParallelTopicModel model, int recrdIndx) {
		Set<TopicModelLDABean> finalDistribution = new HashSet<TopicModelLDABean>();
		if (recrdIndx >= model.getData().size()) {
			System.out.println("Invalid Record Index Passed !!");
			return finalDistribution;
		}
		// The data alphabet maps word IDs to strings
		Alphabet dataAlphabet = model.getData().get(recrdIndx).instance
				.getDataAlphabet();

		double[] topicDistribution = model.getTopicProbabilities(recrdIndx);
		// Get an array of sorted sets of word ID/count pairs, across the entire
		// data sets not to a particular instance
		ArrayList<TreeSet<IDSorter>> topicSortedWords = model.getSortedWords();

		// topics with proportions for the ith document
		for (int topic = 0; topic < topicSortedWords.size(); topic++) {
			Iterator<IDSorter> iterator = topicSortedWords.get(topic)
					.iterator();
			double distrbtn = topicDistribution[topic];
			while (iterator.hasNext()) {
				IDSorter idCountPair = iterator.next();
				if (ConfigConstant.SPACE.equals(((String) dataAlphabet
						.lookupObject(idCountPair.getID())).trim()))
					continue;

				TopicModelLDABean topicmodel = new TopicModelLDABean();
				topicmodel.setWord((String) dataAlphabet
						.lookupObject(idCountPair.getID()));
				topicmodel.setWeight(idCountPair.getWeight());
				topicmodel.setTopic("" + topic);
				topicmodel.setTopicDistribution(distrbtn);
				finalDistribution.add(topicmodel);
				// System.out.println(topicmodel.getWord());
			}
		}

		// releasing memory
		topicSortedWords.clear();

		return finalDistribution;
	}

	public static void removeThreadCache(Thread t) {
		LOCAL_CACHE.remove(t);
	}
}
