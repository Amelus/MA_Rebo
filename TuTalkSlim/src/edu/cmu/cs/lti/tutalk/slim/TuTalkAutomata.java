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
package edu.cmu.cs.lti.tutalk.slim;

import edu.cmu.cs.lti.tutalk.script.*;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author rohitk
 */
public class TuTalkAutomata {

    private Scenario currentScenario = null;
    private ExecutionState currentState = null;
    private String tutorId, tuteeId;
    private TurnEvaluator evaluator;
    private ReplacementVariables templater;

    public TuTalkAutomata(String tutorId, String tuteeId) {
        tutorId = tutorId;
        tuteeId = tuteeId;
        evaluator = new TurnEvaluator();
        templater = new ReplacementVariables();
    }

    public void addReplacementVariable(String pKey, String pValue) {
        templater.addReplacementVariable(pKey, pValue);
    }

    public void resetReplacementVariables() {
        templater.resetReplacementVariables();
    }

    public void setEvaluator(TurnEvaluator t) {
        evaluator = t;
    }

    public void setScenario(Scenario s) {
        currentScenario = s;
    }

    public List<String> start() {
        if (currentScenario != null) {
            currentState = new ExecutionState();
            currentState.push(currentScenario.getStartGoal());
        }

        return progress();
    }

    public List<String> progress() {
        List<Concept> executionTrail = new ArrayList<>();
        if (currentScenario != null) {
            do {
                Goal goal = currentState.getCurrentGoal();
                if (goal != null) {
                    Concept c = goal.execute(currentState);
                    if (c != null) {
                        if (c instanceof ResponseExpected) {
                            currentState.setExpected(((ResponseExpected) c).getValidResponses());
                            break;
                        } else {
                            executionTrail.add(c);
                        }
                    } else {
                        if (currentState.getTodos().isEmpty()) {
                            break;
                        }
                    }
                } else {
                    break;
                }
            }
            while (true);
        }

        return getTutorTurns(executionTrail);
    }

    private List<String> getTutorTurns(List<Concept> executionTrail) {
        List<String> tutorTurns = new ArrayList<>();
        if (!executionTrail.isEmpty()) {
            for (Concept c : executionTrail) {
                if (!c.getText().equalsIgnoreCase(StringUtils.EMPTY)) {
                    tutorTurns.add(templater.renderTemplate(c.getText()));
                }
            }
        }
        return tutorTurns;
    }

    public List<EvaluatedConcept> evaluateTuteeTurn(String turn, Collection<String> annotations) {
        List<Concept> concepts = new ArrayList<>();
        List<Response> expected = currentState.getExpected();
        for (Response response : expected) {
            concepts.add(response.getConcept());
        }
        concepts.add(currentScenario.getConceptLibrary().getConcept("_dont_know_"));
        return evaluator.evaluateTurn(turn, concepts, annotations);
    }

    public List<EvaluatedConcept> evaluateTuteeTurn(String turn) {
        return evaluateTuteeTurn(turn, new ArrayList<>());
    }

    public List<String> progress(Concept inputConcept) {
        List<Concept> executionTrail = new ArrayList<>();
        if (currentScenario != null) {
            do {
                Goal goal = currentState.getCurrentGoal();
                if (goal != null) {
                    Concept c = goal.execute(inputConcept, currentState);
                    if (c != null) {
                        if (c instanceof ResponseExpected) {
                            currentState.setExpected(((ResponseExpected) c).getValidResponses());
                            break;
                        } else {
                            executionTrail.add(c);
                        }
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            } while (true);
        }

        List<String> tutorTurns = getTutorTurns(executionTrail);
        List<String> moreTurns = progress();

        tutorTurns.addAll(moreTurns);

        return tutorTurns;
    }

    public void reset() {
    }

    public ExecutionState getState() {
        return currentState;
    }
}
