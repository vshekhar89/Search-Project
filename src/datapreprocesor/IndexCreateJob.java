/**
 * 
 */
package datapreprocesor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import org.json.JSONObject;

import util.FileUtils;
import util.JsonUtil;
import util.ThreadPoolUtil;
import constants.ConfigConstant;
import constants.ConfigConstant.JOB_EXECUTION_STATUS;
import constants.ConfigConstant.JSON_FIELD;

/**
 * @author sumit
 *
 */
public class IndexCreateJob implements Callable<JOB_EXECUTION_STATUS> {
	private CreateIndex crtObjct = null;
	private String filePathUrl = null;

	// private

	public IndexCreateJob(String pIndexCreationPath, String pSourceFilePath)
			throws IOException {
		this.crtObjct = new CreateIndex(pIndexCreationPath);
		this.filePathUrl = pSourceFilePath;
	}

	public IndexCreateJob(CreateIndex crtIndex, String pSourceFilePath)
			throws IOException {
		this.crtObjct = crtIndex;
		this.filePathUrl = pSourceFilePath;
	}

	@Override
	public JOB_EXECUTION_STATUS call() throws Exception {

		JOB_EXECUTION_STATUS jobStatus = JOB_EXECUTION_STATUS.STARTAED;

		try {
			// for each json object in the file form the review and tips text
			// pairs
			Stream<String> optimizedReader = FileUtils.readAllLinesOptimized(
					this.filePathUrl, StandardCharsets.ISO_8859_1);
			// to collect reviews and tips text
			final Map<String, StringBuilder[]> reviewTipsText = new HashMap<>();

			optimizedReader.forEach(text -> {
				updateReviewAndTipsDataSet(reviewTipsText, text);
			});

			// iterate through map to add to index
			for (Map.Entry<String, StringBuilder[]> bussRevTip : reviewTipsText
					.entrySet()) {
				JSONObject jsonObj = JsonUtil
						.addField(JSON_FIELD.BUSINESS_ID.getFieldName(),
								bussRevTip.getKey())
						.put(JSON_FIELD.REVIEW.getFieldName(),
								bussRevTip.getValue()[0])
						.put(JSON_FIELD.TIPS.getFieldName(),
								bussRevTip.getValue()[1]);

				this.crtObjct.writeToDoc(jsonObj);
			}
			jobStatus = JOB_EXECUTION_STATUS.SUCCESS;
			reviewTipsText.clear();
			System.out.println("Completed Creation of index for :: "
					+ this.filePathUrl);
		} finally {
			if (!jobStatus.equals(JOB_EXECUTION_STATUS.SUCCESS)) {
				ThreadPoolUtil.dumpStackTrace(Thread.currentThread());
				jobStatus = JOB_EXECUTION_STATUS.FAILED;
				// this.crtObjct.releaseResource();// trying to release all
				// resource
			}
			ThreadPoolUtil.cleanUpGarbageRequest();
		}

		return jobStatus;
	}

	/**
	 * @param reviewTipsText
	 * @param text
	 */
	private void updateReviewAndTipsDataSet(
			final Map<String, StringBuilder[]> reviewTipsText, String text) {
		JSONObject jsonObject = JsonUtil.getJSONObject(text);
		String bussinessID = jsonObject.getString(JSON_FIELD.BUSINESS_ID
				.getFieldName());
		if (!reviewTipsText.containsKey(bussinessID)) {
			reviewTipsText.put(bussinessID, new StringBuilder[] {
					new StringBuilder(), new StringBuilder() });
		}
		String type = jsonObject.getString(JSON_FIELD.RECORD_TYPE
				.getFieldName());
		int index = type.equals(JSON_FIELD.REVIEW.getFieldName()) ? 0 : 1;
		reviewTipsText.get(bussinessID)[index].append(jsonObject
				.getString(JSON_FIELD.TEXT.getFieldName())
				+ ConfigConstant.SPACE);
	}
}
