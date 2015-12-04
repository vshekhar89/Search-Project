/**
 * 
 */
package datapreprocesor;

import util.TempReviewTipsProcessUtil;
import constants.ConfigConstant;

/**
 * @author sumit
 *
 */
public class ReviewTipsJsonPreProcess implements IJsonPreProcess {

	private String reviewFileName;
	private String tipsFileName;

	public ReviewTipsJsonPreProcess(String reviewPath, String tipsPath) {
		this.reviewFileName = reviewPath;
		this.tipsFileName = tipsPath;
	}

	@Override
	public Object process() throws Exception {
		TempReviewTipsProcessUtil.generateReviewTipsUnionFiles(
				ConfigConstant.TEMP_REVIEW_TIPS_TEXT_LOCATION, this.reviewFileName,
				this.tipsFileName);
		return null;
	}
}
