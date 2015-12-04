package practicetest;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.SimpleTaggerSentence2TokenSequence;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.pipe.iterator.LineGroupIterator;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.InstanceList;

public class TrainTopics {
	
	int numTopics = 20;

	public TrainTopics(String trainingFilename, String testingFilename) throws IOException {
		
		ArrayList<Pipe> pipes = new ArrayList<Pipe>();

		pipes.add(new SimpleTaggerSentence2TokenSequence());
		pipes.add(new TokenSequenceRemoveStopwords());
		pipes.add(new TokenSequence2FeatureSequence());

		Pipe pipe = new SerialPipes(pipes);

		InstanceList trainingInstances = new InstanceList(pipe);
		InstanceList testingInstances = new InstanceList(pipe);

		trainingInstances.addThruPipe(new LineGroupIterator(new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(trainingFilename)))), Pattern.compile("^\\s*$"), true));
		testingInstances.addThruPipe(new LineGroupIterator(new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(testingFilename)))), Pattern.compile("^\\s*$"), true));
		
		ParallelTopicModel lda = new ParallelTopicModel(numTopics);
		lda.addInstances(trainingInstances);
		lda.estimate();
		lda.printDocumentTopics(new PrintWriter(System.out));
	}

	public static void main (String[] args) throws Exception {
		TrainTopics trainer = new TrainTopics("C:/Users/sumit/Downloads/mallet/00.txt.gz", "C:/Users/sumit/Downloads/mallet/01.txt.gz");

	}

}