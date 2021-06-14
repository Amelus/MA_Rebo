package basilica2.socketchat;

import basilica2.agents.components.ChatClient;
import basilica2.agents.events.*;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Event;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.swing.JOptionPane;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

//TODO: MoodleChatClient.properties

public class WebsocketChatClient extends Component implements ChatClient {
    private static final Logger LOG = Logger.getLogger(WebsocketChatClient.class);
    public static final String READY = "ready";

    String socketURL = "http://localhost:8000";
    String socketSubURL;
    String agentUserName = "ROBOT";
    String agentRoomName = "ROOM";


    boolean connected;

    Socket socket;


    /* (non-Javadoc)
     * @see basilica2.agents.components.ChatClient#disconnect()
     */
    @Override
    public void disconnect() {
        socket.disconnect();
    }

    //TODO: poll for incoming messages
    //TODO: report presence events
    //TODO: report extant students at login


    public WebsocketChatClient(Agent a, String n, String pf) {
        super(a, n, pf);

        socketURL = myProperties.getProperty("socket_url", socketURL);
        socketSubURL = myProperties.getProperty("socket_suburl", socketSubURL);
        agentUserName = a.getUsername();//myProperties.getProperty("agent_username", agentUserName);

        java.util.logging.Logger sioLogger = java.util.logging.Logger.getLogger("io.socket");
        sioLogger.setLevel(Level.SEVERE);

    }

    @Override
    protected void processEvent(Event event) {
        if (event instanceof ReadyEvent) {
            ReadyEvent readyEvent = (ReadyEvent) event;
            try //almost always unready, global
            {
                shareReady(readyEvent.isReady(), readyEvent.isGlobalReset());
            } catch (Exception e) {
                LOG.error("Couldn't share image: " + readyEvent.toString(), e);
                // TODO Auto-generated catch block
            }
        }
        if (event instanceof WhiteboardEvent) {
            WhiteboardEvent messageEvent = (WhiteboardEvent) event;
            try {
                shareImage(messageEvent.filename);
            } catch (Exception e) {
                LOG.error("couldn't share image: " + messageEvent.toString(), e);
                // TODO Auto-generated catch block
            }
        } else if (event instanceof DisplayHTMLEvent) {
            DisplayHTMLEvent messageEvent = (DisplayHTMLEvent) event;
            try {
                insertHTML(messageEvent.getText());
            } catch (Exception e) {
                LOG.error("couldn't share html: " + messageEvent.toString(), e);
                // TODO Auto-generated catch block
            }
        } else if (event instanceof PrivateMessageEvent) {
            PrivateMessageEvent messageEvent = (PrivateMessageEvent) event;
            try {
                insertPrivateMessage(messageEvent.getText(), messageEvent.getDestinationUser());
            } catch (Exception e) {
                LOG.error("couldn't send message: " + messageEvent.toString(), e);
                // TODO Auto-generated catch block
            }
        } else if (event instanceof MessageEvent) {
            MessageEvent messageEvent = (MessageEvent) event;
            try {
                insertMessage(messageEvent.getText());
            } catch (Exception e) {
                LOG.error("couldn't send message: " + messageEvent.toString(), e);
                // TODO Auto-generated catch block
            }
        }
        //TODO: private messages? "beeps"?

    }

    /* (non-Javadoc)
     * @see basilica2.agents.components.ChatClient#login(java.lang.String)
     */
    @Override
    public void login(String roomName) {
        agentRoomName = roomName;
        LOG.debug("logging in to " + roomName + " at " + socketURL);
        if (socketSubURL != null) {
            LOG.debug("    Using specialized socket.io address " + socketSubURL);
        }
        try {
            if (socketSubURL != null) {
                IO.Options o = new IO.Options();
                o.path = socketSubURL;
                socket = IO.socket(socketURL, o);
            } else {
                socket = IO.socket(socketURL);
            }
            setCallbacks();
            socket.connect();
            socket.emit("adduser", agentRoomName, agentUserName, new Boolean(false));
        } catch (Exception e) {
            LOG.error("Couldn't log in to the chat server...", e);

            JOptionPane.showMessageDialog(null, "Couldn't access chat server: " + e.getMessage(), "Login Failure", JOptionPane.ERROR_MESSAGE);

            connected = false;
        }
    }

    @Override
    public String getType() {
        return "ChatClient";
    }


    protected void insertHTML(String message) {
        socket.emit("sendhtml", message);
    }

    protected void insertMessage(String message) {
        LOG.debug("Sending message=" + message);
        socket.emit("sendchat", message);
    }

    protected void insertPrivateMessage(String message, String toUser) {
        LOG.debug("Emitting message to user" +toUser + " with text=" + message);
        socket.emit("sendpm", message, toUser);
    }

    protected void shareImage(String imageURL) {
        socket.emit("sendimage", imageURL);
    }

    protected void shareReady(boolean ready, boolean global) {
        if (global) {
            socket.emit("global_ready", ready ? READY : "unready");
        } else {
            socket.emit("ready", ready ? "ready" : "unready");
        }
    }

    public void setCallbacks() {
        socket.on(Socket.EVENT_ERROR, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                LOG.debug("an Error occurred...");
                //socketIOException.printStackTrace();

                LOG.debug("attempting to reconnect...");
                try {
                    if (socketSubURL != null) {
                        IO.Options o = new IO.Options();
                        o.path = socketSubURL;
                        socket = IO.socket(socketURL, o);
                    } else {
                        socket = IO.socket(socketURL);
                    }
                    setCallbacks();
                    socket.connect();

                    new Timer().schedule(new TimerTask() {
                        public void run() {
                            LOG.debug("Logging back in to chat room.");
                            socket.emit("adduser", agentRoomName, agentUserName, new Boolean(false));
                            //socket.emit("sendchat" ,"...and I'm back!");
                        }
                    }, 1000L);

                } catch (URISyntaxException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                LOG.debug("Connection terminated.");
            }
        }).on(Socket.EVENT_CONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                LOG.debug("Connection established");
            }
        }).on("updateusers", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                JSONArray names_list = ((JSONObject) args[0]).names();
                JSONArray perspective_list = ((JSONObject) args[1]).names();
                //LOG.debug("Users: "+((JSONObject) args[0]).names() + " " + Integer.toString(names_list.length()));
                LOG.debug("Users: " + ((JSONObject) args[0]).names());
                //LOG.debug("Users: "+ names_list.length());
                JSONObject jObject = (JSONObject) args[0];
                JSONObject jObject_ = (JSONObject) args[1];
                Iterator<String> keys = jObject.keys();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    String value = null;
                    String perspective = null;
                    try {
                        value = jObject.getString(key);
                    } catch (JSONException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    try {
                        perspective = jObject_.getString(key);
                    } catch (JSONException e) {
                        // TODO Auto-generated catch block
                        LOG.debug("[WARN] No Perspective provided");
                    }

                    PresenceEvent pe = new PresenceEvent(WebsocketChatClient.this, key, PresenceEvent.PRESENT, value, perspective, (String) args[2]);
                    WebsocketChatClient.this.broadcast(pe);
                }

            }
        }).on("updatechat", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                String message = (String) args[1];
                message = StringEscapeUtils.unescapeHtml4(message);
                LOG.debug("Updatechat event received message=" + message);
                MessageEvent messageEvent = new MessageEvent(WebsocketChatClient.this, (String) args[0], message);
                WebsocketChatClient.this.broadcast(messageEvent);
            }
        }).on("updateimage", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                String message = (String) args[1];
                WhiteboardEvent messageEvent = new WhiteboardEvent(WebsocketChatClient.this, message, (String) args[0], message);
                WebsocketChatClient.this.broadcast(messageEvent);
            }
        }).on("updatepresence", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                String message = (String) args[1];
                LOG.debug("Perspective : " + args[3]);

                String agentName = Objects.toString(args[0]);
                String type = message.equals("join") ? PresenceEvent.PRESENT : PresenceEvent.ABSENT;
                String agentId = Objects.toString(args[2]);
                String agentPerspective = Objects.toString(args[3]);
                PresenceEvent pe = new PresenceEvent(WebsocketChatClient.this, agentName, type, agentId, agentPerspective);
                WebsocketChatClient.this.broadcast(pe);
            }
        }).on("updateready", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                String state = (String) args[1];
                ReadyEvent readyEvent = new ReadyEvent(WebsocketChatClient.this, state.equals("ready"), (String) args[0]);
                WebsocketChatClient.this.broadcast(readyEvent);
            }
        }).on("dumphistory", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                LOG.debug("Ignoring historical messages.");
            }
        });
    }

}
