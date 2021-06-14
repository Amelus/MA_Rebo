package basilica2.agents.listeners.plan;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.data.PromptTable;
import edu.cmu.cs.lti.project911.utils.log.Logger;
/**
 * do nothing when this step is executed - PlanExecutor waits for an external StepDoneEvent or a timeout.
 * @author dadamson
 *
 */
class DoNothingStepHandler implements StepHandler
{
	private PromptTable prompter;

	public static String getStepType()
	{
		return "no-op";
	}

	public DoNothingStepHandler()
	{
		
	}

	public void execute(Step step, PlanExecutor overmind, InputCoordinator source)
	{
		Logger.commonLog("DoNothingStepHandler", Logger.LOG_NORMAL, "DoNothingStepHandler does nothing.");
		overmind.stepDone();
	}
}