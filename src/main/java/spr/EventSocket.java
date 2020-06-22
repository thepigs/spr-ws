package spr;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

import javax.servlet.http.Cookie;
import java.net.HttpCookie;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

class GameSession {

}
public class EventSocket extends WebSocketAdapter
{
    private static final ConcurrentHashMap<String,GameSession> sessions = new ConcurrentHashMap<String,GameSession>();

    private String id;

    @Override
    public void onWebSocketConnect(Session sess)
    {
        super.onWebSocketConnect(sess);
        for (HttpCookie c:sess.getUpgradeRequest().getCookies()){
            if (c.getName().equals("spr")){
                // TODO check for existing game
                sessions.put(id=c.getValue(),new GameSession());
            }
        }
        System.out.println("Socket Connected: " + sess);
    }
    
    @Override
    public void onWebSocketText(String message)
    {
        super.onWebSocketText(message);
        System.out.println("Received TEXT message: " + message);
    }
    
    @Override
    public void onWebSocketClose(int statusCode, String reason)
    {
        super.onWebSocketClose(statusCode,reason);
        System.out.println("Socket Closed: [" + statusCode + "] " + reason);
    }
    
    @Override
    public void onWebSocketError(Throwable cause)
    {
        super.onWebSocketError(cause);
        cause.printStackTrace(System.err);
    }
}
