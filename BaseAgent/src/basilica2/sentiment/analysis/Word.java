package basilica2.sentiment.analysis;

public class Word {
	
	public Sentiment type;
	public double value;
	public String word_class;
	
	public Sentiment stringToPolarityStrength(String str) {
		if(str.equals("pos")) {
			return Sentiment.POSITIVE;
		} else if(str.equals("neg")) {
			return Sentiment.NEGATIVE;
		} else if(str.equals("neu")) {
			return Sentiment.NEUTRAL;
		} else if(str.equals("int")) {
			return Sentiment.INTENSIFY;
		} 

		return Sentiment.NEUTRAL;
	}
}
