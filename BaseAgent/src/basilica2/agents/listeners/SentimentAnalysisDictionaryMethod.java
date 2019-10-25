package basilica2.agents.listeners;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.org.apache.xalan.internal.xsltc.dom.LoadDocument;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.MessageEvent;
import basilica2.sentiment.analysis.MessageSentiment;
import basilica2.sentiment.analysis.Sentiment;
import basilica2.sentiment.analysis.Word;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;

public class SentimentAnalysisDictionaryMethod implements BasilicaPreProcessor{

	public static String GENERIC_NAME = "SentimentAnalysisDictionaryMethod";
	public static String GENERIC_TYPE = "Filter";
	
	private ArrayList<String> stopwords = new ArrayList<String>();
	private Map<String, Word> lexica;
	
	public SentimentAnalysisDictionaryMethod() {
		loadDictionary();
		loadStopwords();
	}
	
	private void loadDictionary() {
		File lexiconFile = new File("sentimentAnalysis\\lexicon.txt");
		ArrayList<String> lexiconLines = readInDictionary(lexiconFile);
		lexica = buildLexica(lexiconLines);
	}
	
	private void loadStopwords() {
		File stopwordFile = new File("sentimentAnalysis\\stopwords.txt");
		stopwords = readInDictionary(stopwordFile);
	}
	
	private Map<String, Word> buildLexica(ArrayList<String> lines) {
		Map<String, Word> dictionary = new HashMap<String, Word>();
		
		for(String line : lines) {
			Word word = null;
			String[] elements = line.split(" ");
			String wordName = "";
			
			if(elements.length == 3) {
				wordName = elements[0];
				word = parseWordPolarity(elements[1]);
				word.word_class = elements[2];
			} else if(elements.length == 4) {
				wordName = elements[0] + " " + elements[1];
				word = parseWordPolarity(elements[2]);
				word.word_class = elements[3];
			}
			
			if(wordName.length() > 0)
				dictionary.put(wordName, word);
		}
		return dictionary;
	}
	
	private Word parseWordPolarity(String polarity) {
		Word newWord = new Word();
		newWord.type = newWord.stringToPolarityStrength(polarity.split("=")[0]);
		
		try {
			newWord.value = Double.parseDouble(polarity.split("=")[1]);
		} catch (NumberFormatException e) {
			newWord.value = 0;
		}
		
		return newWord;
	}
	
	private ArrayList<String> readInDictionary(File file) {
		ArrayList<String> lines = new ArrayList<String>();
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			
			String line = reader.readLine();
			while(line != null) {
				lines.add(line);
				line = reader.readLine();
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return lines;
	}
	
	@Override
	public void preProcessEvent(InputCoordinator source, Event event) {
		if (event instanceof MessageEvent)
		{
			handleMessageEvent((MessageEvent) event);
		}
	}

	@Override
	public Class[] getPreprocessorEventClasses() {
		return new Class[] { MessageEvent.class };
	}

	private void handleMessageEvent(MessageEvent event) {
		MessageSentiment message_sentiment = event.getMessageSentiment();
		String[] message = event.getText().toLowerCase().split(" ");
		
		double intensify = 0.0;
		
	    for(String word : message) {
	        if(!isStopWord(word)) {
	        	Sentiment type = getWordSentiment(word);
	        	double value = getPolarityValue(word);
	        	
	        	if(type == Sentiment.INTENSIFY) {
	        		intensify = value;
	        	} else if(intensify != 0.0) {
	        		message_sentiment.addSentiment(value * intensify, type);
	        		intensify = 0.0;
	        	} else {
	        		message_sentiment.addSentiment(value, type);
	        	}
	        }
	    }
	}
	
	private boolean isStopWord(String word) {
		return stopwords.contains(word);
	}
	
	private boolean isInLexica(String word) {
		return lexica.containsKey(word);
	}
	
	private Sentiment getWordSentiment(String word) {
		return lexica.get(word).type;
	}
	
	private double getPolarityValue(String word) {
		return lexica.get(word).value;
	}
}
