package basilica2.agents.listeners;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.MessageEvent;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;

public class SentimentAnalysisDictionaryMethod implements BasilicaPreProcessor{

	public static String GENERIC_NAME = "SentimentAnalysisDictionaryMethod";
	public static String GENERIC_TYPE = "Filter";
	
	public SentimentAnalysisDictionaryMethod() {
		
	}
	
	@Override
	public void preProcessEvent(InputCoordinator source, Event event) {
		if (event instanceof MessageEvent)
		{
			//doSomething
			Logger.commonLog(getClass().getSimpleName(), Logger.LOG_ERROR, "doSomething SentimentAnalysisDictionaryMethod");
		}
	}

	@Override
	public Class[] getPreprocessorEventClasses() {
		return new Class[] { MessageEvent.class };
	}

}
