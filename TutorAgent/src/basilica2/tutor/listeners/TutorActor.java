/*
 *  Copyright (c), 2009 Carnegie Mellon University.
 *  All rights reserved.
 *
 *  Use in source and binary forms, with or without modifications, are permitted
 *  provided that that following conditions are met:
 *
 *  1. Source code must retain the above copyright notice, this list of
 *  conditions and the following disclaimer.
 *
 *  2. Binary form must reproduce the above copyright notice, this list of
 *  conditions and the following disclaimer in the documentation and/or
 *  other materials provided with the distribution.
 *
 *  Permission to redistribute source and binary forms, with or without
 *  modifications, for any purpose must be obtained from the authors.
 *  Contact Rohit Kumar (rohitk@cs.cmu.edu) for such permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY CARNEGIE MELLON UNIVERSITY ``AS IS'' AND
 *  ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 *  THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL CARNEGIE MELLON UNIVERSITY
 *  NOR ITS EMPLOYEES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package basilica2.tutor.listeners;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.PresenceEvent;
import basilica2.agents.events.PromptEvent;
import basilica2.agents.events.priority.BlacklistSource;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.agents.events.priority.PriorityEvent.Callback;
import basilica2.agents.listeners.BasilicaAdapter;
import basilica2.tutor.events.DoTutoringEvent;
import basilica2.tutor.events.DoneTutoringEvent;
import basilica2.tutor.events.MoveOnEvent;
import basilica2.tutor.events.StudentTurnsEvent;
import basilica2.tutor.events.TutorTurnsEvent;
import basilica2.tutor.events.TutoringStartedEvent;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import edu.cmu.cs.lti.project911.utils.time.TimeoutReceiver;
import edu.cmu.cs.lti.project911.utils.time.Timer;
import edu.cmu.cs.lti.tutalk.script.Concept;
import edu.cmu.cs.lti.tutalk.script.Response;
import edu.cmu.cs.lti.tutalk.script.Scenario;
import edu.cmu.cs.lti.tutalk.slim.EvaluatedConcept;
import edu.cmu.cs.lti.tutalk.slim.FuzzyTurnEvaluator;
import edu.cmu.cs.lti.tutalk.slim.TuTalkAutomata;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

//import basilica2.social.data.TurnCounts;

/**
 * @author rohitk --> dadamson --> gtomar
 */
public class TutorActor extends BasilicaAdapter implements TimeoutReceiver {
    private static final String UNANTICIPATED_RESPONSE = "unanticipated-response";
    private boolean isTutoring = false;
    private boolean expectingResponse = false;
    private boolean nullExpected = false;
    private TuTalkAutomata currentAutomata = null;
    private String currentConcept = null;
    private int noMatchingResponseCount = 0;
    private List<String> answerers = new ArrayList<String>();
    private Map<String, Dialog> pendingDialogs = new HashMap<String, Dialog>();
    private Map<String, Dialog> proposedDialogs = new HashMap<String, Dialog>();
    private InputCoordinator source;

    private double tutorMessagePriority = 0.75;
    private boolean interruptForNewDialogues = false;
    private boolean startAnyways = true;
    private String dialogueFolder = "dialogs";

    private String dialogueConfigFile = "dialogues/dialogues-config.xml";
    private int introduction_cue_timeout = 600;
    private int introduction_cue_timeout2 = 600;
    private int tutorTimeout = 120;
    private String request_poke_prompt_text = "I am waiting for your response to start. Please ask for help if you are stuck.";
    private String goahead_prompt_text = "Let's go ahead with this.";
    private String response_poke_prompt_text = "Can you rephrase your response?";
    private String dont_know_prompt_text = "Anybody?";
    private String moving_on_text = "Okay, let's move on.";
    private String tutorialCondition = "tutorial";
    private int numUser = 1;

    private String keywordsFile = "dictionaries/fake_reflect.txt";
    private List<String> fakeReflectiveKeywords = new LinkedList<>();

    class Dialog {

        public String conceptName;
        public String scenarioName;
        public String introText;
        public String acceptAnnotation;
        public String acceptText;
        public String cancelAnnotation;
        public String cancelText;

        public Dialog(String conceptName, String scenarioName, String introText, String cueAnnotation, String cueText, String cancelAnnotation, String cancelText) {
            this.conceptName = conceptName;
            this.scenarioName = scenarioName;
            this.introText = introText;
            this.acceptAnnotation = cueAnnotation;
            this.acceptText = cueText;
            this.cancelAnnotation = cancelAnnotation;
            this.cancelText = cancelText;
        }
    }

    public TutorActor(Agent a) {
        super(a);
        introduction_cue_timeout = Integer.parseInt(properties.getProperty("timeout1"));
        introduction_cue_timeout2 = Integer.parseInt(properties.getProperty("timeout2"));
        request_poke_prompt_text = properties.getProperty("requestpokeprompt", request_poke_prompt_text);
        goahead_prompt_text = properties.getProperty("goaheadprompt", goahead_prompt_text);
        response_poke_prompt_text = properties.getProperty("responsepokeprompt", response_poke_prompt_text);
        dont_know_prompt_text = properties.getProperty("dontknowprompt", dont_know_prompt_text);
        moving_on_text = properties.getProperty("moveonprompt", moving_on_text);
        dialogueConfigFile = properties.getProperty("dialogue_config_file", dialogueConfigFile);
        dialogueFolder = properties.getProperty("dialogue_folder", dialogueFolder);
        startAnyways = properties.getProperty("start_anyways", "false").equals("true");
        tutorialCondition = properties.getProperty("tutorial_condition", tutorialCondition);

        loadDialogConfiguration(dialogueConfigFile);
        loadFakeReflectiveKeywords();

        prioritySource = new BlacklistSource("TUTOR_DIALOG", "");
        ((BlacklistSource) prioritySource).addExceptions("TUTOR_DIALOG");
    }

    private void loadFakeReflectiveKeywords() {
        try (Scanner scanner = new Scanner(new File(this.keywordsFile))) {
            while (scanner.hasNext()) {
                this.fakeReflectiveKeywords.add(scanner.nextLine());
            }
        } catch (FileNotFoundException fnfe) {
            log(Logger.LOG_WARNING, "Keyword File could not be opened: " + fnfe.getMessage());
        }
    }

    private void loadDialogConfiguration(String f) {
        try {
            DOMParser parser = new DOMParser();
            parser.parse(f);
            Document dom = parser.getDocument();
            NodeList dialogsNodes = dom.getElementsByTagName("dialogs");
            if ((dialogsNodes != null) && (dialogsNodes.getLength() != 0)) {
                Element dialogsNode = (Element) dialogsNodes.item(0);
                NodeList dialogNodes = dialogsNode.getElementsByTagName("dialog");
                if ((dialogNodes != null) && (dialogNodes.getLength() != 0)) {
                    for (int i = 0; i < dialogNodes.getLength(); i++) {
                        Element dialogElement = (Element) dialogNodes.item(i);
                        String conceptName = dialogElement.getAttribute("concept");
                        String name = dialogElement.getAttribute("scenario");
                        String introText = null;
                        String cueText = null;
                        String cueAnnotation = null;
                        String cancelText = "Dialog Abbruch durch Timeout";
                        String cancelAnnotation = null;
                        NodeList introNodes = dialogElement.getElementsByTagName("intro");
                        if ((introNodes != null) && (introNodes.getLength() != 0)) {
                            Element introElement = (Element) introNodes.item(0);
                            introText = introElement.getTextContent();
                        }
                        NodeList cueNodes = dialogElement.getElementsByTagName("accept");
                        if ((cueNodes != null) && (cueNodes.getLength() != 0)) {
                            Element cueElement = (Element) cueNodes.item(0);
                            cueAnnotation = cueElement.getAttribute("annotation");
                            cueText = cueElement.getTextContent();
                        }

                        NodeList cancelNodes = dialogElement.getElementsByTagName("cancel");
                        if ((cancelNodes != null) && (cancelNodes.getLength() != 0)) {
                            Element cancelElement = (Element) cancelNodes.item(0);
                            cancelAnnotation = cancelElement.getAttribute("annotation");
                            cancelText = cancelElement.getTextContent();
                        }
                        Dialog d = new Dialog(conceptName, name, introText, cueAnnotation, cueText, cancelAnnotation, cancelText);
                        proposedDialogs.put(conceptName, d);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected void processEvent(Event e) {
        if (e instanceof DoTutoringEvent) {
            handleDoTutoringEvent((DoTutoringEvent) e);
        } else if (e instanceof TutoringStartedEvent) {
            handleTutoringStartedEvent((TutoringStartedEvent) e);
        } else if (e instanceof PresenceEvent) {
            PresenceEvent event = (PresenceEvent) e;
            numUser = event.getNumUsers();
        } else if (e instanceof MessageEvent) {
            handleRequestDetectedEvent((MessageEvent) e);
        } else if (e instanceof StudentTurnsEvent) {
            handleStudentTurnsEvent((StudentTurnsEvent) e);
        } else if (e instanceof MoveOnEvent) {
            handleMoveOnEvent((MoveOnEvent) e);
        }
    }

    private void handleDoTutoringEvent(DoTutoringEvent dte) {
        Dialog d = proposedDialogs.get(dte.getConcept());
        if (d == null) {
            return;
        }

        if (isTutoring && !interruptForNewDialogues) {
            log(Logger.LOG_WARNING, "Won't start dialogue " + dte.getConcept() + " while current dialog is running - ask again later!");
            return;
        }

        if (isTutoring) {
            if (interruptForNewDialogues) {
                sendTutorMessage(moving_on_text);
                DoneTutoringEvent doneEvent = new DoneTutoringEvent(source, currentConcept, true);
                source.queueNewEvent(doneEvent);
                prioritySource.setBlocking(false);

                currentAutomata = null;
                currentConcept = null;
                isTutoring = false;
            }
        }

        launchDialogOffer(d, ((InputCoordinator) dte.getSender()).user);
    }

    private synchronized void handleRequestDetectedEvent(MessageEvent e) {
        for (String concept : e.getAllAnnotations()) {
            Dialog killMeNow = null;

            for (Dialog d : pendingDialogs.values()) {

                if (d.acceptAnnotation.equals(concept)) {

                    killMeNow = d;
                    sendTutorMessage(d.acceptText);
                    startDialog(d);
                } else if (d.cancelAnnotation.equals(concept)) {

                    killMeNow = d;
                    sendTutorMessage(d.cancelText);
                    prioritySource.setBlocking(false);

                }
                if (killMeNow != null) {
                    pendingDialogs.remove(killMeNow.acceptAnnotation);
                    break;
                }
            }
        }
    }

    public void handleTutoringStartedEvent(TutoringStartedEvent tse) {
        if (currentConcept.equals(tse.getConcept())) {
            List<String> tutorTurns = currentAutomata.start();
            processTutorTurns(tutorTurns);
        } else {
            log(Logger.LOG_WARNING, "Received start event for " + tse + " but current concept is " + currentConcept);
        }
    }


    public void startDialog(Dialog d) {
        isTutoring = true;

        currentConcept = d.conceptName;
        currentAutomata = new TuTalkAutomata("tutor", "students");
        currentAutomata.setEvaluator(new FuzzyTurnEvaluator());
        currentAutomata.setScenario(Scenario.loadScenario(dialogueFolder + File.separator + d.scenarioName + ".xml"));

        TutoringStartedEvent tse = new TutoringStartedEvent(source, d.scenarioName, d.conceptName);
        source.queueNewEvent(tse);
    }

    private void handleMoveOnEvent(MoveOnEvent mve) {
        if (checkEventShouldBeHandled()) {
            expectingResponse = false;
            noMatchingResponseCount = 0;
            List<Response> expected = currentAutomata.getState().getExpected();
            Response response = expected.get(expected.size() - 1);

            if (!UNANTICIPATED_RESPONSE.equalsIgnoreCase(response.getConcept().getLabel())) {
                log(Logger.LOG_ERROR, "Moving on without an Unanticipated-Response Handler. Could be weird!");
            }
            List<String> tutorTurns = currentAutomata.progress(response.getConcept());
            processTutorTurns(tutorTurns);
        }
    }

    private boolean checkEventShouldBeHandled() {
        return expectingResponse && currentAutomata != null;
    }

    private boolean checkStudentEventShouldBeHandled(List<String> studentTurn) {
        return checkEventShouldBeHandled() && !studentTurn.isEmpty();
    }

    private synchronized void handleStudentTurnsEvent(StudentTurnsEvent studentTurnsEvent) {
        if (checkStudentEventShouldBeHandled(studentTurnsEvent.getStudentTurns())) {
            StringBuilder studentTurn = new StringBuilder();
            for (String turn : studentTurnsEvent.getStudentTurns()) {
                studentTurn.append(turn).append(" | ");
            }
            List<EvaluatedConcept> matchingConcepts =
                    currentAutomata.evaluateTuteeTurn(studentTurn.toString(), studentTurnsEvent.getAnnotations());

            if (!matchingConcepts.isEmpty()) {
                System.out.println(matchingConcepts.get(0).getClass().getSimpleName());
                System.out.println(matchingConcepts.get(0).concept.getClass().getSimpleName());
                Concept concept = matchingConcepts.get(0).concept;
                if (UNANTICIPATED_RESPONSE.equalsIgnoreCase(concept.getLabel())) {
                    if (!nullExpected) {
                        noMatchingResponseCount++;

                        if (matchingConcepts.size() == 1 || noMatchingResponseCount >= numUser + 2) {
                            moveOn(concept);
                        } else if (noMatchingResponseCount <= numUser + 2) {
                            System.out.println("unanticipated");
                        }
                        return;
                    }
                } else {
                    answerers.addAll(studentTurnsEvent.getContributors());
                }

                moveOn(concept);
            }
        }
    }

    private void moveOn(Concept concept) {
        expectingResponse = false;
        noMatchingResponseCount = 0;
        List<String> tutorTurns = currentAutomata.progress(concept);
        processTutorTurns(tutorTurns);
    }

    private void messageAccepted() {
        if (currentAutomata != null) {
            List<Response> expected = currentAutomata.getState().getExpected();
            if (CollectionUtils.isNotEmpty(expected)) {
                expectingResponse = true;
                nullExpected = false;
                noMatchingResponseCount = 0;

                if (expected.size() == 1
                        && UNANTICIPATED_RESPONSE.equalsIgnoreCase(expected.get(0).getConcept().getLabel())) {
                    nullExpected = true;
                }
            }
        } else {
            answerers = new ArrayList<>();

            DoneTutoringEvent dte = new DoneTutoringEvent(source, currentConcept);
            prioritySource.setBlocking(false);
            source.queueNewEvent(dte);

            currentAutomata = null;
            currentConcept = null;
            isTutoring = false;
        }
    }


    private void launchDialogOffer(Dialog d, String userName) {
        prioritySource.setBlocking(true);
        sendTutorMessage(selectIntroString(d.introText, userName));
        pendingDialogs.put(d.acceptAnnotation, d);
        Timer t = new Timer(introduction_cue_timeout, d.conceptName, this);
        t.start();
    }

    private String selectIntroString(String intro, String userName) {
        String replacedText;
        if (userName != null) {
            replacedText = intro.replaceAll("\\[USERNAME\\]", userName);
        } else {
            replacedText = intro.replaceAll("\\[USERNAME\\]", "");
        }

        String[] texts;
        texts = replacedText.split(" \\| ");

        return texts[(int) (Math.random() * texts.length)];
    }

    private void processTutorTurns(List<String> tutorTurns) {
        source.userMessages.analyizeMessages(tutorTurns, this.fakeReflectiveKeywords);
        String[] turns = tutorTurns.toArray(new String[0]);

        if (turns.length == 0) {
            return;
        }

        TutorTurnsEvent tte = new TutorTurnsEvent(source, turns);
        source.queueNewEvent(tte);
        PriorityEvent pete = new PriorityEvent(source, new MessageEvent(source, getAgent().getUsername(), join(turns), "TUTOR"), tutorMessagePriority, prioritySource, tutorTimeout);
        pete.addCallback(new Callback() {
            @Override
            public void rejected(PriorityEvent p) {
                log(Logger.LOG_ERROR, "Tutor Turn event was rejected. Proceeding anyway...");
                messageAccepted();
            }

            @Override
            public void accepted(PriorityEvent p) {
                messageAccepted();
            }
        });
        source.pushProposal(pete);
    }

    public void timedOut(String id) {
        Dialog d = null;
        if (proposedDialogs.get(id) == null) {
            log(Logger.LOG_WARNING, "No dialog for " + id);
        } else {
            d = pendingDialogs.get(proposedDialogs.get(id).acceptAnnotation);
        }

        log(Logger.LOG_NORMAL, "Timeout for " + id + ":" + d);
        if (d != null) {
            sendTutorMessage(request_poke_prompt_text);
            Timer t = new Timer(introduction_cue_timeout2, "CANCEL:" + id, this);
            t.start();
            log(Logger.LOG_NORMAL, "Delaying dialog " + id + " once...");
        } else {
            if (isDialogTimedOutTwice(id)) {
                killPendingDialog(id);
            }
        }
    }

    private boolean isDialogTimedOutTwice(String id) {
        return id.startsWith("CANCEL:");
    }

    private void killPendingDialog(String id) {
        String[] tokens = id.split(":");
        Dialog dialog = proposedDialogs.get(tokens[1]);

        dialog = pendingDialogs.remove(dialog.acceptAnnotation);
        if (dialog != null) {
            if (startAnyways) {
                sendTutorMessage(goahead_prompt_text);

                log(Logger.LOG_NORMAL, "Not delaying " + tokens[1] + " anymore - beginning dialog");
                startDialog(dialog);
                return;
            } else {
                sendTutorMessage(dialog.cancelText);
            }
        }

        prioritySource.setBlocking(false);
    }

    private void sendTutorMessage(String... promptStrings) {
        String combo = join(promptStrings);

        if (!StringUtils.isBlank(combo)) {
            PriorityEvent pete = new PriorityEvent(source, new MessageEvent(source, getAgent().getUsername(), combo, "TUTOR"), tutorMessagePriority, prioritySource, 45);
            source.pushProposal(pete);
        }
    }

    public String join(String... promptStrings) {
        StringBuilder combo = new StringBuilder();
        for (String text : promptStrings) {
            combo.append("|").append(text);
        }
        return combo.substring(1);
    }

    public void log(String from, String level, String msg) {
        log(level, from + ": " + msg);
    }

    @Override
    public void processEvent(InputCoordinator source, Event event) {
        this.source = source;
        processEvent(event);

    }

    @Override
    public Class[] getListenerEventClasses() {
        return new Class[]{MessageEvent.class, DoTutoringEvent.class, StudentTurnsEvent.class, MoveOnEvent.class, TutoringStartedEvent.class, PresenceEvent.class, PromptEvent.class};
    }

    @Override
    public void preProcessEvent(InputCoordinator source, Event event) {
    }

    @Override
    public Class[] getPreprocessorEventClasses() {
        return new Class[0];
    }
}
