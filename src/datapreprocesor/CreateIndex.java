package datapreprocesor;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.store.FSDirectory;
import org.json.JSONException;
import org.json.JSONObject;

import util.JsonUtil;
import constants.ConfigConstant;
import constants.ConfigConstant.JSON_FIELD;

public class CreateIndex {

	// public String pathToIndex = "/Users/shardendu/info_ret/index";
	private IndexWriter writer;
	private IndexWriterConfig iwc;
	private Analyzer analyzer;
	// local cache of Document Objects, due to optimization on memory needs
	private final Map<String, Document> LOCAL_CACHE;

	public CreateIndex(String pPath) throws IOException {
		analyzer = new StandardAnalyzer();
		iwc = new IndexWriterConfig(analyzer);
		iwc.setOpenMode(OpenMode.CREATE);
		iwc.setRAMBufferSizeMB(ConfigConstant.INDEX_BUFFER_SIZE_IN_MB);
		//iwc.setCommitOnClose(true);
		writer = new IndexWriter(FSDirectory.open(Paths.get(pPath)), iwc);
		LOCAL_CACHE = new HashMap<>();
	}

	public void releaseResource() throws IOException {
		LOCAL_CACHE.clear();
		writer.close();
	}

	public void writeToDoc(JSONObject object) throws IOException,
			ParseException, JSONException {
		String currentThreadName = Thread.currentThread().getName();
		if (!LOCAL_CACHE.containsKey(currentThreadName)) {
			LOCAL_CACHE.put(currentThreadName, new Document());
		}
		Document doc = LOCAL_CACHE.get(currentThreadName);
		updateDoc(object, doc);

		writer.addDocument(doc);
		// optimization reasons
		/*
		 * count++; if (count % ConfigConstant.INDEX_FLUSH_TIME == 0) { count =
		 * 0; writer.commit(); System.gc(); }
		 */

	}

	private void updateDoc(JSONObject object, Document doc) {
		String[] names = JSONObject.getNames(object);
		for (String name : names) {
			IndexableField fieldtype = checkFieldType(name, object,
					doc.getField(name));
			if (doc.getField(name) == null) {
				doc.add(fieldtype);
			}
		}

	}

	public IndexableField checkFieldType(String name, JSONObject object,
			IndexableField indexableField) throws JSONException {
		IndexableField fieldtype = null;
		JSON_FIELD jsonField = JsonUtil.getJSONField(name, object);

		if (indexableField != null) {
			((Field) indexableField).setStringValue(object.get(name)
					.toString());
			return indexableField;
		}
		
		switch (jsonField) {
		case BUSINESS_ID:
			fieldtype = new StringField(name, object.getString(name),
					Field.Store.YES);
			break;
		case REVIEW:
		case TIPS:
			fieldtype = new TextField(name, object.get(name).toString(),
					Field.Store.YES);
			break;
		default:
			throw new IllegalArgumentException(
					"Index-Creation :: Invalid Json Filed Type Passed :"
							+ jsonField);

		}
		return fieldtype;
	}

}
