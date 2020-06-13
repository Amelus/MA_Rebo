Usage:

To start repo first create a chat room by visiting: http://rebo.know-center.tugraz.at/bazaar/chat/roomname/group/test/1
 
"test" is the username

Then run "basilica2.myagent.operation.NewAgentRunner" with arguments ' --launch --room="roomname" ' and workingDir "MA_Rebo\MTurkAgent\runtime"

Rebo will show up and greet you to start the conversation


Troubleshooting

In case Rebo just logs in and out again, remove "Rebo_roomname.planstatus.txt" from "MTurkAgent/runtime/planstatus"
This file indicates the last step Rebo has performed, in case "FIRST_REBO_TALK" is written there, Rebo will immediately go to LOG_OUT as the conversation seems do be done already from Rebo's perspective