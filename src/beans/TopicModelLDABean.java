/**
 * 
 */
package beans;

/**
 * @author sumit
 *
 */
public class TopicModelLDABean implements Comparable<TopicModelLDABean> {

	private String word;
	private double weight;
	private String topic;
	private double topicDistribution;

	/**
	 * 
	 */
	public TopicModelLDABean() {

	}

	public String getWord() {
		return word;
	}

	public void setWord(String word) {
		this.word = word;
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public double getTopicDistribution() {
		return topicDistribution;
	}

	public void setTopicDistribution(double topicDistribution) {
		this.topicDistribution = topicDistribution;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof TopicModelLDABean)) {
			return false;
		}
		TopicModelLDABean tpcMdlBean = (TopicModelLDABean) obj;
		return (this.word == null && this.word == tpcMdlBean.word)
				|| (this.word != null && this.word.equals(tpcMdlBean.word));
	}

	@Override
	public int hashCode() {
		return this.word == null ? 0 : this.word.hashCode();
	}

	@Override
	public int compareTo(TopicModelLDABean o2) {
		if (this.topicDistribution == o2.topicDistribution) {
			return this.topic.compareTo(o2.topic);

		}
		return (this.topicDistribution - o2.topicDistribution) > 0 ? 1 : -1;
	}
}
