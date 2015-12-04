package util;

import org.json.JSONObject;

import constants.ConfigConstant.JSON_FIELD;

public class JsonUtil {

	public static JSONObject getJSONObject(String jSonText) {
		return new JSONObject(jSonText);
	}

	public static JSONObject addField(String fieldName, Object value) {
		return new JSONObject().put(fieldName, value);
	}

	public static JSONObject addField(JSONObject jsonObject, String fieldName,
			Object value) {
		return jsonObject.put(fieldName, value);
	}

	public static JSON_FIELD getJSONField(String name, JSONObject object) {
		if (name.equals(JSON_FIELD.RECORD_TYPE.getFieldName())) {
			String review_tips = object.getString(name);
			return review_tips.equals(JSON_FIELD.REVIEW.getFieldName()) ? JSON_FIELD.REVIEW
					: JSON_FIELD.TIPS;
		}

		for (JSON_FIELD jsonField : JSON_FIELD.values()) {
			if (jsonField.getFieldName().equals(name)) {
				return jsonField;
			}
		}
		
		return null;
	}
}
