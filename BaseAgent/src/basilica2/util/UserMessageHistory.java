package basilica2.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import basilica2.agents.events.MessageEvent;
import edu.cmu.cs.lti.basilica2.core.Event;

public class UserMessageHistory {

	private final int SHORT_MESSAGE_THRESHOLD = 4;
	private final String remind_user_to_write_whole_sentences = "Heute wieder einmal kurz angebunden.";
	private ArrayList<MessageEvent> messages = new ArrayList<MessageEvent>();
	private ArrayList<MessageEvent> multi_message_response_array = new ArrayList<MessageEvent>();
	private boolean userPoked = false;
	
	public boolean multiMessageActive() {
		return !multi_message_response_array.isEmpty() ? true : false;
	}
	
    private boolean checkMultiMessage(String text) {
    	if(text.length() > 3) {
    		return text.substring(text.length() - 3).contentEquals("...");
    	}
    	return false;
    }
    
    public void handleUserMessage(Event event) {
    	if(event instanceof MessageEvent) {
    		messages.add((MessageEvent) event);
    		handleMultiLineMessage((MessageEvent) event);
    	}
    }
    
    private void handleMultiLineMessage(MessageEvent event) {
    	String message = event.getText().trim();
    	if(checkMultiMessage(message)) {
    		multi_message_response_array.add(event);
    	} else if(multi_message_response_array.size() > 0){
    		
    		Iterator<MessageEvent> iter = multi_message_response_array.iterator(); 
    		String concatenatedMessage =  iter.next().getText();
    		while (iter.hasNext()) {
    			concatenatedMessage += " | " + iter.next().getText();
    		}
    		
    		event.setText(concatenatedMessage + " | " + message);
    		multi_message_response_array.clear();
    	}
        
    }
    
    private boolean checkForToShortAnswers() {
    	if(messages.size() > 2 && userPoked == false) {
    		Iterator<MessageEvent> iter = messages.iterator(); 
    		iter.next(); // skip greeting message
    		int word_count = 0;
    		while (iter.hasNext()) {
    			word_count += countWordsofString(iter.next().getText());
    		}
    		float average_words_per_message = word_count / (messages.size() - 1);
    		
    		if(average_words_per_message < SHORT_MESSAGE_THRESHOLD) {
    			userPoked = true;
    			return true;
    		}
    	} 
    	return false;
    }
    
    public void handleToShortMessages(List<String> tutorTurns) {
    	if(checkForToShortAnswers()) {
    		tutorTurns.add(0, remind_user_to_write_whole_sentences);
    	}
    }
    
    private int countWordsofString(String input) {
        if (input == null || input.isEmpty()) {
          return 0;
        }

        String[] words = input.split("\\s+");
        return words.length;
    }
}
