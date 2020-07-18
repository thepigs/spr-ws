package spr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.jetty.websocket.api.WriteCallback;

import java.util.*;
import java.util.logging.Logger;

public class MessageHandler {
    public static final ObjectMapper om = new ObjectMapper();

    private Logger LOG = Logger.getLogger(MessageHandler.class.getName());

    private class PlayerState implements WriteCallback {
        public String name;
        private final EventSocket socket;
        public PlayerState versus;
        public String selected;

        public PlayerState(EventSocket socket) {
            this.socket = socket;
        }

        public List<String> outgoing = new ArrayList<String>();
        boolean sending = false;

        public synchronized void sendMessage(String message) {
            if (sending) {
                outgoing.add(message);
                return;
            }
            System.out.println("Sending: " + message);

            socket.getRemote().sendString(message, this);
        }

        @Override
        public void writeFailed(Throwable throwable) {
            LOG.warning(name + ": " + throwable.getMessage());
        }

        @Override
        public synchronized void writeSuccess() {
            if (!outgoing.isEmpty())
                socket.getRemote().sendString(outgoing.remove(0), this);
            else
                sending = false;
        }
    }

    private static final Map<EventSocket, PlayerState> sessions = new HashMap<>();

    public void remove(EventSocket eventSocket) {
        synchronized (sessions) {
            sessions.remove(eventSocket);
            broadcastPlayerListMessage();
        }
    }

    private synchronized PlayerState findByName(String name) {
        for (PlayerState s : sessions.values()) {
            if (s.name != null && s.name.equals(name))
                return s;
        }
        return null;
    }

    private PlayerState getState(EventSocket socket) {
        synchronized (sessions) {
            return sessions.get(socket);
        }
    }

    public void handleMessage(String msgString, EventSocket socket) {
        ObjectNode msg = null;
        try {
            msg = (ObjectNode) om.readTree(msgString);
        } catch (JsonProcessingException e) {
            LOG.warning(msgString + ": " + e.getMessage());
            return;
        }
        switch (msg.get("type").asText()) {
            case "login":
                String name = msg.get("name").asText();
                PlayerState gs = getState(socket);
                if (name.equals(gs.name))
                    gs.sendMessage("{\"type\":\"logged_in\"}");
                else {
                    PlayerState gs2 = this.findByName(name);
                    if (gs2 != null)
                        gs.sendMessage(createError("Name already taken!").toPrettyString());
                    else {
                        gs.name = name;
                        gs.sendMessage("{\"type\":\"logged_in\"}");
                    }
                }
                broadcastPlayerListMessage();
                break;
            case "battle_invite":
                name = msg.get("name").asText();
                PlayerState me = getState(socket);
                PlayerState them = findByName(name);
                if (them == null) {
                    me.sendMessage(createError("User has gone!").toPrettyString());
                    return;
                }
                them.sendMessage(createObject(new String[][]{{"type", "battle_invite"}, {"name", me.name}, {"text", msg.get("text").asText()}}).toPrettyString());
                break;
            case "battle_rsvp":
                name = msg.get("name").asText();
                String action = msg.get("action").asText();
                me = getState(socket);
                them = findByName(name);
                if (them == null) {
                    me.sendMessage(createError("User has gone!").toPrettyString());
                    return;
                }
                synchronized (sessions) {
                    them.versus = me;
                    me.versus = them;
                    them.sendMessage(createObject(new String[][]{{"type", "battle_start"}, {"name", me.name}}).toPrettyString());
                    me.sendMessage(createObject(new String[][]{{"type", "battle_start"}, {"name", them.name}}).toPrettyString());
                }

                break;
            case "battle_select":
                me = getState(socket);
                synchronized (sessions) {
                    me.selected = msg.get("selected").asText();
                    if (me.versus.selected != null) {
                        me.versus.sendMessage(createObject(new String[][]{{"type", "battle_over"}, {"selected", me.selected}}).toPrettyString());
                        me.sendMessage(createObject(new String[][]{{"type", "battle_over"}, {"selected", me.versus.selected}}).toPrettyString());
                    }
                }
        }
    }

    private ObjectNode createObject(String[][] parms) {
        ObjectNode on = om.createObjectNode();
        for (String[] p : parms) {
            on.put(p[0], p[1]);
        }
        return on;
    }

    private void broadcastMessage(String message) {
        synchronized (sessions) {
            for (PlayerState ps : sessions.values()) {
                ps.sendMessage(message);
            }
        }
    }

    public void connect(EventSocket socket) {
        PlayerState gs = getState(socket);
        if (gs == null) {
            synchronized (sessions) {
                gs = new PlayerState(socket);
                sessions.put(socket, gs);
            }
        }
        gs.sendMessage(makePlayerListMessage().toPrettyString());
    }

    private ObjectNode makePlayerListMessage() {
        ObjectNode on = om.createObjectNode();
        on.put("type", "player_list");
        ArrayNode an = on.putArray("players");
        synchronized (sessions) {
            for (PlayerState ps : sessions.values()) {
                if (ps.name != null)
                    an.add(ps.name);
            }
        }
        return on;
    }

    private void broadcastPlayerListMessage() {
        broadcastMessage(makePlayerListMessage().toPrettyString());
    }

    private ObjectNode createError(String s) {
        ObjectNode on = om.createObjectNode();
        on.put("type", "error");
        on.put("message", s);
        return on;
    }

}
