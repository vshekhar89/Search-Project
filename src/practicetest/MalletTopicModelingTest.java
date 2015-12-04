/**
 * 
 */
package practicetest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Iterator;
import java.util.Locale;
import java.util.TreeSet;
import java.util.regex.Pattern;

import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.CharSequenceLowercase;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.pipe.iterator.StringArrayIterator;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.topics.TopicInferencer;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.IDSorter;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelSequence;

/**
 * @author sumit
 *
 */
public class MalletTopicModelingTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		// Begin by importing documents from text to feature sequences
		ArrayList<Pipe> pipeList = new ArrayList<Pipe>();

		// Pipes: lowercase, tokenize, remove stopwords, map to features
		pipeList.add(new CharSequenceLowercase());
		pipeList.add(new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]*\\p{L}")));//
		pipeList.add(new TokenSequenceRemoveStopwords());
		pipeList.add(new TokenSequence2FeatureSequence());

		InstanceList instances = new InstanceList(new SerialPipes(pipeList));
		String[] sampleData = new String[] { "The East End Food Co-op has just about everything you would expect to find in a food co-op, except for--sadly--a great selection of local beers.  This tragic fault does not rest with the co-op, of course, but with the state of Pennsylvania.  But you probably don't care because you know where all of these elusive \"beer distributors\" are located.\n\nSo, to get back on track, I like the local produce focus here, and some of the cheeses appeared to be local, as well.  Nice staff, decent prices, good little salad bar.  I love co-ops, and if you do, too, then you will love the East End Food Co-op.","As a displaced west-coaster, I appreciate a store like this in Pittsburgh. \n\nI like the bulk goods and spices because you can take as much as you need, you get the spices whole and they put them in big jars so you can sniff them if you want before you buy (the downside to that is that every time the jars get opened the spices probably lose some oils), and there's a huge selection. \n\nThis place has my quinoa, pearled barley, spelt flour, and nutritional yeast. \n\nSome of the vegetables pop out at me and I'm tempted to buy a lemon or some really ripe piece of fruit. Usually I don't though. During the summer they were selling fresh herbs in pots. That was really tempting. Once they sold kittens. That was amazing. \n\nIt doesn't hurt that its right by the climbing wall, fencing, gymnastics, and bike shop." };
		instances.addThruPipe(new StringArrayIterator(sampleData));
		// Create a model with 100 topics, alpha_t = 0.01, beta_w = 0.01
		// Note that the first parameter is passed as the sum over topics, while
		// the second is the parameter for a single dimension of the Dirichlet
		// prior.
		int numTopics = 100;
		ParallelTopicModel model = new ParallelTopicModel(numTopics, 1.0, 0.01);

		model.addInstances(instances);
		// Use two parallel samplers, which each look at one half the corpus and
		// combine
		// statistics after every iteration.
		model.setNumThreads(2);

		//model.setSymmetricAlpha(b);
		// Run the model for 50 iterations and stop (this is for testing only,
		// for real applications, use 1000 to 2000 iterations)
		model.setNumIterations(50);
		model.estimate();

		// Show the words and topics in the first instance

		// The data alphabet maps word IDs to strings
		Alphabet dataAlphabet = instances.getDataAlphabet();

		FeatureSequence tokens = (FeatureSequence) model.getData().get(0).instance
				.getData();
		LabelSequence topics = model.getData().get(0).topicSequence;
		System.out.println(tokens.get(0));
		System.out.println(topics.getLength());
		
		Formatter out = new Formatter(new StringBuilder(), Locale.US);
		for (int position = 0; position < tokens.getLength(); position++) {
			out.format("%s-%d ", dataAlphabet.lookupObject(tokens
					.getIndexAtPosition(position)), topics
					.getIndexAtPosition(position));
		}
		System.out.println(out);

		// Estimate the topic distribution of the first instance,
		// given the current Gibbs state.
		double[] topicDistribution = model.getTopicProbabilities(0);
		
		// Get an array of sorted sets of word ID/count pairs
		ArrayList<TreeSet<IDSorter>> topicSortedWords = model.getSortedWords();
		
		System.out.println(topicDistribution.length+"---"+topicSortedWords.size());
		
		// Show top 5 words in topics with proportions for the first document
		for (int topic = 0; topic < topicSortedWords.size(); topic++) {
			Iterator<IDSorter> iterator = topicSortedWords.get(topic)
					.iterator();

			out = new Formatter(new StringBuilder(), Locale.US);
			out.format("%d\t%.6f\t",topic, topicDistribution[topic]);
			int rank = 0;
			while (iterator.hasNext()) {
				IDSorter idCountPair = iterator.next();
				out.format("%s (%.0f) ",
						dataAlphabet.lookupObject(idCountPair.getID()),
						idCountPair.getWeight());
				rank++;
			}
			System.out.println(out);
		}

		// Create a new instance with high probability of topic 0/89
		StringBuilder topicZeroText = new StringBuilder();
		Iterator<IDSorter> iterator = topicSortedWords.get(0).iterator();

		int rank = 0;
		while (iterator.hasNext() && rank < 5) {
			IDSorter idCountPair = iterator.next();
			topicZeroText.append(dataAlphabet.lookupObject(idCountPair.getID())
					+ " ");
			rank++;
		}

		// Create a new instance named "test instance" with empty target and
		// source fields.
		InstanceList testing = new InstanceList(instances.getPipe());
		testing.addThruPipe(new Instance(topicZeroText.toString(), null,
				"test instance", null));

		TopicInferencer inferencer = model.getInferencer();
		double[] testProbabilities = inferencer.getSampledDistribution(
				testing.get(0), 100, 1, 50);
		System.out.println(topicZeroText+" -\t" + testProbabilities[89]);
		System.out.println(Arrays.toString(testProbabilities));
		
		
		
	}

}
