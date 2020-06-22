package spr;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@SuppressWarnings("serial")
public class EventServlet extends WebSocketServlet
{
    @Override
    public void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        boolean hasCookie = false;
        for (Cookie c:req.getCookies()){
            if (c.getName().equals("spr")){
                hasCookie = true;
            }
        }
        if (!hasCookie){
            Cookie c = new Cookie("spr",Long.toString(sr.nextLong(),16)+Long.toString(sr.nextLong(),16));
            c.setMaxAge(1*24*60*60);
            resp.addCookie(c);
        }

        super.service(req, res);
    }

    @Override
    public void configure(WebSocketServletFactory factory)
    {
        factory.register(EventSocket.class);
    }

}
