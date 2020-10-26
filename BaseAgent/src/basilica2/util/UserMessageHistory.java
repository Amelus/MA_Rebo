package basilica2.util;

import basilica2.agents.events.MessageEvent;
import edu.cmu.cs.lti.basilica2.core.Event;
import org.apache.commons.lang3.StringUtils;
import ratte.texttools;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class UserMessageHistory {

    private final int SHORT_MESSAGE_THRESHOLD = 4;
    private static final String REMIND_USER_MESSAGE = "Heute wieder einmal kurz angebunden.";
    private static final String FEEDBACK_PHRASE = "Das Gespräch war aus meiner Sicht";

    private ArrayList<MessageEvent> messages = new ArrayList<>();
    private ArrayList<MessageEvent> multiMessageResponseArray = new ArrayList<>();

    private boolean userPoked = false;

    public boolean multiMessageActive() {
        return !multiMessageResponseArray.isEmpty();
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
            multiMessageResponseArray.add(event);
        } else if (!multiMessageResponseArray.isEmpty()) {

            Iterator<MessageEvent> iter = multiMessageResponseArray.iterator();
            StringBuilder concatenatedMessage = new StringBuilder(iter.next().getText());
            while (iter.hasNext()) {
                concatenatedMessage.append(" | ").append(iter.next().getText());
            }
            String text = concatenatedMessage.append(" | ").append(message).toString();
            event.setText(text);
            multiMessageResponseArray.clear();
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

    public void analyizeMessages(List<String> tutorTurns) {
        if (hasTooShortAnswers()) {
            tutorTurns.add(0, REMIND_USER_MESSAGE);
        }

        Optional<String> feedBackTurn = tutorTurns
                .stream()
                .filter(turn -> StringUtils.equals(turn, FEEDBACK_PHRASE)).findFirst();

        if (feedBackTurn.isPresent()) {

            String tutorAnswer = getTutorFeedBack();

            tutorTurns.remove(0);
            tutorTurns.add(0, FEEDBACK_PHRASE + tutorAnswer);
        }
    }

    private String getTutorFeedBack() {

        texttools ratteTool = new texttools();

        String allAnswers = messages.stream()
                .map(MessageEvent::getText)
                .collect(Collectors.joining("; "));

        ratteTool.set_text(allAnswers);
        double rix = ratteTool.rix() * 100.0;
        double wordCount = ratteTool.dwords();
        int monosyllables = ratteTool.count_wordswithsyllabes_exact(1);
        int trisyllables = ratteTool.count_wordswithsyllabes_exact(3);

        double score = rix + wordCount + monosyllables + trisyllables;

        if (score <= 200.0) {
            return  " jetzt nicht so berauschend, du könntest dir schon etwas mehr Mühe geben.";
        } else if (score >= 300.0) {
            return " sehr cool, freut mich dass du dir so viel Mühe gibst :D";
        } else {
            return " ganz in Ordnung :)";
        }
    }

    private int countWordsofString(String input) {
        if (StringUtils.isBlank(input)) {
            return 0;
        }

        String[] words = input.split("\\s+");
        return words.length;
    }
}
