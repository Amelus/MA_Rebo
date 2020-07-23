package basilica2.util;

import basilica2.agents.events.MessageEvent;
import edu.cmu.cs.lti.basilica2.core.Event;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class UserMessageHistory {

    private final int SHORT_MESSAGE_THRESHOLD = 4;
    private final String remind_user_to_write_whole_sentences = "Heute wieder einmal kurz angebunden.";
    private final String remind_user_to_write_to_be_precise = "Wie genau meinst du das?";
    private ArrayList<MessageEvent> messages = new ArrayList<MessageEvent>();
    private ArrayList<MessageEvent> multi_message_response_array = new ArrayList<MessageEvent>();
    private boolean userPoked = false;

    public boolean multiMessageActive() {
        return !multi_message_response_array.isEmpty() ? true : false;
    }

    private boolean checkMultiMessage(String text) {
        if (text.length() > 3) {
            return text.substring(text.length() - 3).contentEquals("...");
        }
        return false;
    }

    public void handleUserMessage(Event event) {
        if (event instanceof MessageEvent) {
            messages.add((MessageEvent) event);
            handleMultiLineMessage((MessageEvent) event);
        }
    }

    private void handleMultiLineMessage(MessageEvent event) {
        String message = event.getText().trim();
        if (checkMultiMessage(message)) {
            multi_message_response_array.add(event);
        } else if (!multi_message_response_array.isEmpty()) {

            Iterator<MessageEvent> iter = multi_message_response_array.iterator();
            StringBuilder concatenatedMessage = new StringBuilder(iter.next().getText());
            while (iter.hasNext()) {
                concatenatedMessage.append(" | ").append(iter.next().getText());
            }
            String text = concatenatedMessage.append(" | ").append(message).toString();
            event.setText(text);
            multi_message_response_array.clear();
        }

    }

    private boolean hasTooShortAnswers() {
        if (messages.size() > 2 && !userPoked) {
            Iterator<MessageEvent> iter = messages.iterator();
            iter.next(); // skip greeting message
            int wordCount = 0;
            while (iter.hasNext()) {
                wordCount += countWordsofString(iter.next().getText());
            }
            float averageWordsPerMessage = wordCount / (messages.size() - 1);

            if (averageWordsPerMessage < SHORT_MESSAGE_THRESHOLD) {
                userPoked = true;
                return true;
            }
        }
        return false;
    }

    public void analyizeMessages(List<String> tutorTurns, List<String> fakeReflectiveKeywords) {
        if (hasTooShortAnswers()) {
            tutorTurns.add(0, remind_user_to_write_whole_sentences);
        }
        if (hasFakeReflectiveKeywords(fakeReflectiveKeywords)) {
            //tutorTurns.add(0, remind_user_to_write_to_be_precise);
        }
    }

    private boolean hasFakeReflectiveKeywords(List<String> fakeReflectiveKeywords) {
        if (CollectionUtils.isNotEmpty(messages) && CollectionUtils.isNotEmpty(fakeReflectiveKeywords)) {
            return messages.stream()
                    .filter(Objects::nonNull)
                    .filter(msg -> StringUtils.isNotBlank(msg.getText()))
                    .anyMatch(msg -> fakeReflectiveKeywords.stream()
                            .anyMatch( keyword -> StringUtils.contains(msg.getText().toLowerCase(), keyword)));
        }
        return false;
    }

    private int countWordsofString(String input) {
        if (StringUtils.isBlank(input)) {
            return 0;
        }

        String[] words = input.split("\\s+");
        return words.length;
    }
}
