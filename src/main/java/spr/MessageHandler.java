package spr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.jetty.websocket.api.WriteCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import java.util.logging.Logger;

public class MessageHandler {
    public static final ObjectMapper om = new ObjectMapper();

    private Logger LOG = Logger.getLogger(MessageHandler.class.getName());

    private class PlayerState implements WriteCallback {
        private final String name;
        private final EventSocket socket;

        public PlayerState(String name,EventSocket socket){
            this.name= name;
            this.socket = socket;
        }
        public List<String> outgoing=new ArrayList<String>();
        boolean sending= false;
        public synchronized void sendMessage(String message){
            if (sending) {
                outgoing.add(message);
                return;
            }
            socket.getRemote().sendString(message,this);
        }

        @Override
        public void writeFailed(Throwable throwable) {
            LOG.warning(name+": "+throwable.getMessage());
        }

        @Override
        public synchronized void writeSuccess() {
            if (!outgoing.isEmpty())
                socket.getRemote().sendString(outgoing.remove(0),this);
            else
                sending = false;
        }
    }
    private static final WeakHashMap<EventSocket, PlayerState> sessions = new WeakHashMap<>();

    public void remove(EventSocket eventSocket) {
        synchronized(sessions){
            sessions.remove(eventSocket);
            sendPlayerListMessage();
        }
    }


    private synchronized PlayerState findByName(String name){
        for (PlayerState s:sessions.values()){
            if (s.name.equals(name))
                return s;
        }
        return null;
    }
    public void handleMessage(String msgString,EventSocket socket){
        ObjectNode msg = null;
        try {
            msg = (ObjectNode)om.readTree(msgString);
        } catch (JsonProcessingException e) {
            LOG.warning(msgString+": "+e.getMessage());
            return;
        }
        switch(msg.get("type").asText()){
            case "login":
                String name = msg.get("name").asText();
                PlayerState gs = findByName(name);
                if (gs!=null)
                    gs.sendMessage(createError("Name already taken!").toPrettyString());
                else {
                    gs = new PlayerState(name, socket);
                    sessions.put(socket, gs);
                    sendPlayerListMessage();
                }
        }
    }

    private void broadcastMessage(String message){
        synchronized (sessions){
            for (PlayerState ps:sessions.values()){
                System.out.println("sending: "+message);
                ps.sendMessage(message);
            }
        }
    }
    private void sendPlayerListMessage() {
        ObjectNode on = om.createObjectNode();
        on.put("type","player_list");
        ArrayNode an = on.putArray("players");
        synchronized (sessions){
            for (PlayerState ps:sessions.values()){
                an.add(ps.name);
            }
        }
        broadcastMessage(on.toPrettyString());
    }

    private ObjectNode createError(String s) {
        ObjectNode on = om.createObjectNode();
        on.put("type","error");
        on.put("message",s);
        return on;
    }

}
