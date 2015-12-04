/**
 * 
 */
package datapreprocesor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import model.TopicModelLDAUtil;

import org.json.JSONObject;

import util.FileUtils;
import util.JsonUtil;
import util.ThreadPoolUtil;
import util.TopicModelTemplateUtil;
import beans.TopicModelLDABean;
import cc.mallet.pipe.iterator.StringArrayIterator;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.InstanceList;
import constants.ConfigConstant;
import constants.ConfigConstant.JOB_EXECUTION_STATUS;
import constants.ConfigConstant.JSON_FIELD;

/**
 * @author sumit
 *
 */
public class CreateReviewTipsTopicModelDataSet implements
		Callable<JOB_EXECUTION_STATUS> {

	private static final int THRESHOLD_DUMP_SIZE = 1000;

	private String inputFileURL;
	private String outputDirectory;
	private Set<String> listOfWords;

	private List<JSONObject> texts = new ArrayList<>();
	private int lineNumber = 0;

	public void setListOfWords(Set<String> listOfWords) {
		this.listOfWords = listOfWords;
	}

	/**
	 * 
	 */
	public CreateReviewTipsTopicModelDataSet(String pFileURL, String pOutDir) {
		this.inputFileURL = pFileURL;
		this.outputDirectory = pOutDir;
	}

	/**
	 * @deprecated ("non-optimized form")
	 * @return
	 * @throws Exception
	 */
	private Object process() throws Exception {
		JOB_EXECUTION_STATUS status = JOB_EXECUTION_STATUS.STARTAED;
		try {
			Stream<String> inputText = FileUtils.readAllLinesOptimized(
					this.inputFileURL, StandardCharsets.ISO_8859_1);
			// final List<JSONObject> texts = new ArrayList<>();
			// inputText.forEach(e -> computeDataSet(e, texts));
			String pathToOutpt = getPath();
			try (BufferedWriter bfrdWriter = new BufferedWriter(new FileWriter(
					pathToOutpt))) {
				inputText.forEach(e -> computeDataSet(e, bfrdWriter));
				// dump contents to file
				dumpToFile(bfrdWriter, 1);
			}
			status = JOB_EXECUTION_STATUS.SUCCESS;
			System.out.println("Completed Ml data sets operations :: "
					+ pathToOutpt);
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (!status.equals(JOB_EXECUTION_STATUS.SUCCESS)) {
				ThreadPoolUtil.dumpStackTrace(Thread.currentThread());
				status = JOB_EXECUTION_STATUS.SUCCESS_WITH_ERROR;
			}
			TopicModelLDAUtil.removeThreadCache(Thread.currentThread());
			System.gc();

		}
		return status;
	}

	private JOB_EXECUTION_STATUS processOptimized() throws Exception {
		JOB_EXECUTION_STATUS status = JOB_EXECUTION_STATUS.STARTAED;

		try (BufferedReader bfrdRdr = Files.newBufferedReader(
				Paths.get(this.inputFileURL), StandardCharsets.ISO_8859_1)) {
			String line = null;
			String pathToOutpt = getPath();
			try (BufferedWriter bfrdWriter = new BufferedWriter(new FileWriter(
					pathToOutpt))) {

				List<String> lines = new ArrayList<String>();
				while ((line = bfrdRdr.readLine()) != null) {
					// computeDataSet(line, bfrdWriter);
					lines.add(line);
					if (lines.size() >= THRESHOLD_DUMP_SIZE) {
						computeDataSet(lines, bfrdWriter);
						lines.clear();
					}
				}

				// dump remaining contents to file
				computeDataSet(lines, bfrdWriter);
				dumpToFile(bfrdWriter, 1);
				lines.clear();
			}
			status = JOB_EXECUTION_STATUS.SUCCESS;
			System.out.println("Completed Ml data sets operations :: "
					+ pathToOutpt);
		} finally {
			if (!status.equals(JOB_EXECUTION_STATUS.SUCCESS)) {
				ThreadPoolUtil.dumpStackTrace(Thread.currentThread());
				status = JOB_EXECUTION_STATUS.SUCCESS_WITH_ERROR;
			}
			releaseResource();
		}
		return status;
	}

	/**
	 * @return
	 */
	private String getPath() {
		return this.outputDirectory + new File(this.inputFileURL).getName();
	}

	private void computeDataSet(Collection<String> lines,
			BufferedWriter bfrdWriter) throws IOException {
		try {
			InstanceList instances = TopicModelLDAUtil
					.getNewInstanceContainer(null);// default pipelining

			ParallelTopicModel model = TopicModelLDAUtil.getParallelTopicModel(
					ConfigConstant.TOPIC_MODEL_APLHA_T
							* ConfigConstant.TOPIC_COUNTS,
					ConfigConstant.TOPIC_MODEL_BETA,
					ConfigConstant.TOPIC_COUNTS);

			// adding all the instances at one go
			List<String> allReviewTips = new ArrayList<>();
			for (String textLines : lines) {
				JSONObject reviewTipRecord = JsonUtil.getJSONObject(textLines);
				String text = reviewTipRecord.getString(JSON_FIELD.TEXT
						.getFieldName());
				allReviewTips.add(text);
			}

			TopicModelLDAUtil.addToInstanceConatiner(
					instances,
					new StringArrayIterator(allReviewTips
							.toArray(new String[0])));

			model.addInstances(instances);
			model.estimate();

			// dumping to files
			int recordIndex = 0;
			for (String textLines : lines) {
				JSONObject reviewTipRecord = JsonUtil.getJSONObject(textLines);
				String busnID = reviewTipRecord
						.getString(JSON_FIELD.BUSINESS_ID.getFieldName());

				Set<TopicModelLDABean> tpmdls = TopicModelLDAUtil
						.getWordToTopicDistribution(model, recordIndex);

				texts.add(TopicModelTemplateUtil.createBasicTemplate(busnID,
						tpmdls));

				if (texts.size() >= THRESHOLD_DUMP_SIZE)
					dumpToFile(bfrdWriter, THRESHOLD_DUMP_SIZE);

				if (this.listOfWords != null) {
					for (TopicModelLDABean tp : tpmdls) {
						// 3.update the unique word lists
						// iterate through
						this.listOfWords.add(tp.getWord());
					}
				}
				recordIndex++;
			}
		} finally {

		}
	}

	/**
	 * @deprecated ("non-optimized transformation computation")
	 * @param e
	 * @param bfrdWriter
	 */
	private void computeDataSet(String e, BufferedWriter bfrdWriter) {
		// 1.create a Json Object out of the parameter
		JSONObject reviewTipRecord = JsonUtil.getJSONObject(e);
		// 2.get all topic distribution's
		String text = reviewTipRecord.getString(JSON_FIELD.TEXT.getFieldName());
		String busnID = reviewTipRecord.getString(JSON_FIELD.BUSINESS_ID
				.getFieldName());
		InstanceList instances = TopicModelLDAUtil
				.getNewInstanceContainer(null);// default pipelining

		TopicModelLDAUtil.addToInstanceConatiner(instances,
				new StringArrayIterator(new String[] { text }));

		try {

			ParallelTopicModel model = TopicModelLDAUtil
					.getEstimatedTopicModel(instances,
							ConfigConstant.TOPIC_MODEL_APLHA_T
									* ConfigConstant.TOPIC_COUNTS,
							ConfigConstant.TOPIC_MODEL_BETA,
							ConfigConstant.TOPIC_COUNTS);

			Set<TopicModelLDABean> tpmdls = TopicModelLDAUtil
					.getWordToTopicDistribution(model, 0);

			texts.add(TopicModelTemplateUtil
					.createBasicTemplate(busnID, tpmdls));

			if (texts.size() >= THRESHOLD_DUMP_SIZE)
				dumpToFile(bfrdWriter, THRESHOLD_DUMP_SIZE);

			if (this.listOfWords != null) {
				for (TopicModelLDABean tp : tpmdls) {
					// texts.add(TopicModelTemplateUtil.createBasicTemplate(busnID,
					// tp));
					// 3.update the unique word lists
					// iterate through
					this.listOfWords.add(tp.getWord());
				}
			}

			// releasing memory
			tpmdls.clear();
			model = null;

		} catch (IOException e1) {
			e1.printStackTrace();
		} finally {
			// System.gc();
		}

	}

	/**
	 * @param bfrdWriter
	 * @param thresold
	 * @throws IOException
	 */
	private void dumpToFile(BufferedWriter bfrdWriter, int thresold)
			throws IOException {
		// System.out.println(Thread.currentThread().getName()+" :: "+texts.size());
		if (texts.size() >= thresold) {
			this.lineNumber += this.texts.size();
			System.out.println("Completed lines [" + this.inputFileURL + " ]: "
					+ this.lineNumber/*
									 * +":: global vocubalry size :: "+this.
									 * listOfWords.size()
									 */);
			for (JSONObject obj : texts) {
				FileUtils.writeToFileWithNewLine(bfrdWriter, obj.toString());
			}

			texts.clear();
			releaseResource();

		}
	}

	private void releaseResource() {
		ThreadPoolUtil.cleanUpGarbageRequest();
	}

	@Override
	public JOB_EXECUTION_STATUS call() throws Exception {
		return (JOB_EXECUTION_STATUS) processOptimized();
	}

	@Override
	public void finalize() {
		this.texts.clear();
		this.texts = null;
		this.inputFileURL = null;
		this.outputDirectory = null;
	}
}
