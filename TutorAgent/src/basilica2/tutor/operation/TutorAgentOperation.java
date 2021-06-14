package basilica2.tutor.operation;

import basilica2.agents.operation.BaseAgentOperation;
import basilica2.agents.operation.BaseAgentUI;
import basilica2.agents.operation.ConditionAgentUI;

public class TutorAgentOperation extends BaseAgentOperation
{	
	public static void main(String[] args) 
    {
        initializeSystemProperties("system.properties");
        java.awt.EventQueue.invokeLater(new Runnable() 
        {

            @Override
            public void run() 
            {
                BaseAgentOperation thisOperation = new BaseAgentOperation("operation.properties");
                String[] conditions = thisOperation.getProperties().getProperty("operation.conditions", "").split("[\\s,]+");
                String room = thisOperation.getProperties().getProperty("operation.room", "Test01");
                BaseAgentUI thisUI = new ConditionAgentUI(thisOperation, room, conditions);
                thisOperation.setUI(thisUI);
                thisOperation.startOperation();
                thisUI.operationStarted();
            }
        });
    }
}
