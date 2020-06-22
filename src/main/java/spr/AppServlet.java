package spr;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SecureRandom;

/**
 * Hello world!
 *
 */
public class AppServlet extends HttpServlet
{
    private static final SecureRandom sr = new SecureRandom();
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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
    }
}
