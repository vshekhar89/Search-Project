/**
 * 
 */
package util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import beans.TopicModelLDABean;
import constants.ConfigConstant;
import constants.ConfigConstant.JSON_FIELD;

/**
 * @author sumit
 *
 */
public final class TopicModelTemplateUtil {

	private TopicModelTemplateUtil() {

	}

	public static JSONObject createBasicTemplate(String bussinessID,
			TopicModelLDABean tp) {
		JSONObject record = JsonUtil.addField(
				ConfigConstant.JSON_FIELD.BUSINESS_ID.getFieldName(),
				bussinessID);
		JSONObject tpJson = getJSONFrom(tp);
		record.put(JSON_FIELD.TOPICS_MODEL.getFieldName(), tpJson);
		return record;
	}

	public static JSONObject createBasicTemplate(String bussinessID,
			Collection<TopicModelLDABean> tpms) {
		JSONObject record = JsonUtil.addField(
				ConfigConstant.JSON_FIELD.BUSINESS_ID.getFieldName(),
				bussinessID);

		for (TopicModelLDABean tp : tpms) {
			JSONObject tpJson = getJSONFrom(tp);
			record.accumulate(JSON_FIELD.TOPICS_MODEL.getFieldName(), tpJson);
		}
		// record.put(JSON_FIELD.TOPICS_MODEL.getFieldName(), tpJson);
		return record;
	}

	/**
	 * it is a Json of the format {word, frequency,
	 * topic-probability-distribution}
	 * 
	 * @param tp
	 * @return
	 */
	public static JSONObject getJSONFrom(TopicModelLDABean tp) {
		JSONObject jsonBean = new JSONObject();
		jsonBean.put(JSON_FIELD.WORD.getFieldName(), tp.getWord());
		jsonBean.put(JSON_FIELD.FREQUENCY.getFieldName(), tp.getWeight());
		jsonBean.put(JSON_FIELD.TOPIC_PROB.getFieldName(),
				tp.getTopicDistribution());

		return jsonBean;
	}

	public static JSONObject getJSONFromTemplate(String line) {
		return JsonUtil.getJSONObject(line);
	}

	public static List<String> getWordsFromTemplate(String line) {
		JSONObject jsonLine = getJSONFromTemplate(line);
		List<TopicModelLDABean> tpbenas = geTopicModelFromTemplate(jsonLine);
		return getWordsFrom(tpbenas);
	}

	public static List<String> getWordsFrom(List<TopicModelLDABean> tpbenas) {
		List<String> words = new ArrayList<>();
		tpbenas.forEach(e -> {
			words.add(e.getWord());
		});
		return words;
	}

	public static List<TopicModelLDABean> geTopicModelFromTemplate(
			JSONObject jsonLine) {
		if (!jsonLine.has(JSON_FIELD.TOPICS_MODEL.getFieldName())) {
			//System.out.println(jsonLine);
			return Collections.emptyList();
		}
		List<TopicModelLDABean> tpBeans = new ArrayList<TopicModelLDABean>();
		String key = JSON_FIELD.TOPICS_MODEL.getFieldName();
		try {
			JSONArray allTopics = jsonLine.getJSONArray(key);
			allTopics.forEach(e -> {
				tpBeans.add(getBean((JSONObject) e));
			});

		} catch (JSONException ex) {
			// if we are here may be it's not a json Array but a JsonOject
			tpBeans.add(getBean(jsonLine.getJSONObject(key)));
		} finally {

		}
		return tpBeans;
	}

	/**
	 * @param obj
	 * @return
	 */
	private static TopicModelLDABean getBean(JSONObject obj) {
		TopicModelLDABean tpBean = new TopicModelLDABean();

		tpBean.setWord(obj.getString(JSON_FIELD.WORD.getFieldName()));
		tpBean.setWeight(obj.getDouble(JSON_FIELD.FREQUENCY.getFieldName()));
		tpBean.setTopicDistribution(obj.getDouble(JSON_FIELD.TOPIC_PROB
				.getFieldName()));
		return tpBean;
	}

}
