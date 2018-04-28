package com.loveqrc.chapter1;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(
        urlPatterns = "/contextParameter"
)
public class ContextParameterServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ServletContext context = getServletContext();
        System.out.println(context.getInitParameter("settingOne"));
        System.out.println(context.getInitParameter("settingTwo"));

        System.out.println(getServletConfig().getInitParameter("database"));
        System.out.println(getServletConfig().getInitParameter("server"));
    }
}
