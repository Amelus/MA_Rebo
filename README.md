## Usage:

To start repo first create a chat room by visiting: http://rebo.know-center.tugraz.at/bazaar/chat/roomname/group/test/1
 
"test" is the username

Then run "basilica2.myagent.operation.NewAgentRunner" with arguments ' --launch --room="roomname" ' and workingDir 
"MA_Rebo\MTurkAgent\runtime"

Rebo will show up and greet you to start the conversation

## Structure

In "scenario-rebo.xml" whole behaviour of Rebo is configured.

#### \<concept>
There are two kinds of \<concept>s in the scenario structure: 
* normal 
* annotation

The normal ones contain all actions and reactions of Rebo for specific steps in the conversation.
Those of type "annotation" are triggered by specific keywords from the user Rebo is talking to. 
E.g. "pos_annotation" is triggered by a "POSITIVE" phrase, that phrase is contained in the corresponding dictionary file
"positive.txt". 

#### \<goal>
Goals define the flow of the conversation. Every goal contains at least one \<step> and every step must contain 
exactly one \<initiation> which refers to one \<concept> (basically it defines what Rebo says before 
expecting an answer).
If there is a \<response> defined then it will try to match a concept of type "annotation" and trigger the goal defined
in the \<response push="someGoal"> tag. Otherwise it will jump to the next goal defined in the main goal "start".

### TuTalkScenario.dtd
For more information about the scenario manipulation have a look at "MTurkAgent/runtime/dialogues/TuTalkScenario.dtd"

## Useful classes

* "BaseAgent\src\basilica2\util\UserMessageHistory.java"
* "TutorAgent\src\basilica2\tutor\listeners\TutorActor.java"

## Deployment

Run the Ant task "create_run_jar" (availble from MA_Rebo\build.xml) to create the ReboAgent runnable in 
MTurkAgent\runtime.
Everything inside MTurkAgent\runtime needs to be put inside the "mturkagent" directory in 
[Rebo Server](https://github.com/Amelus/Rebo_Server) project and pushed to your GitHub repo.
On the remote server checkout or pull the project and follow the instructions from README there. 

## Troubleshooting

In case Rebo just logs in and out again, remove "Rebo_roomname.planstatus.txt" from "MTurkAgent/runtime/planstatus"
This file indicates the last step Rebo has performed, in case "FIRST_REBO_TALK" is written there, 
Rebo will immediately go to LOG_OUT as the conversation seems do be done already from Rebo's perspective