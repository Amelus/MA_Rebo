package basilica2.sentiment.analysis;

public class MessageSentiment {
	
	private double positve;
	private double negative;
	private double neutral;
	
	public MessageSentiment() {
		positve = 0.0;
		negative = 0.0;
		neutral = 0.0;
	}
	
	public Sentiment getSentiment() {
		if((positve > negative) && (positve > neutral)) {
			return Sentiment.POSITIVE;
		}
		else if((negative > positve) && (negative > neutral)) {
			return Sentiment.NEGATIVE;
		}
		return Sentiment.NEUTRAL;
	}
	
	public void addSentiment(double value, Sentiment sentiment) {
		switch (sentiment) {
			case POSITIVE:
				positve = positve + value;
				break;
			case NEGATIVE:
				negative = negative + value;
				break;
			default:
				neutral = neutral + value;
				break;
		}
	}
}
