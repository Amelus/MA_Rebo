package basilica2.tutor.listeners;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.listeners.plan.PlanExecutor;
import basilica2.agents.listeners.plan.Step;
import basilica2.agents.listeners.plan.StepHandler;
import edu.cmu.cs.lti.project911.utils.log.Logger;

public class ConditionStepHandler implements StepHandler
{

	@Override
	public void execute(Step currentStep, PlanExecutor overmind, InputCoordinator source)
	{
		//TODO: stop being global
		String condition = System.getProperty("basilica2.agents.condition", "inactive");
		if(condition.equals("active")||condition.equals("target"))
		{
			Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"starting conditionalized step... "+currentStep.name);
			String type = "prompt";
			if(currentStep.attributes.containsKey("gated_type"))
			{
				type = currentStep.attributes.get("gated_type");
			}
			   
			currentStep.executeStepHandlers(source, type);
		}
		else
		{
			Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"skipping conditionalized step "+currentStep.name);
			overmind.stepDone();
		}
	}

}
