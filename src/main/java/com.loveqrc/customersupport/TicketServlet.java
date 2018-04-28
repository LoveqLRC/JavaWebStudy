package com.loveqrc.customersupport;

import com.loveqrc.customersupport.entity.Attachment;
import com.loveqrc.customersupport.entity.Ticket;
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;


@WebServlet(
        urlPatterns = {"/tickets"},
        loadOnStartup = 1
)
//提供文件上传
@MultipartConfig(
        //fileSizeThreshold告诉web容器必需达到多大才写入临时目录
        //小于将保存到内存当中
        fileSizeThreshold = 5_242_880, //5MB
        //禁止上传大小
        maxFileSize = 20_971_520L, //20MB
        //禁止请求大小
        maxRequestSize = 41_943_040L //40MB

)
public class TicketServlet extends HttpServlet {

    private volatile int TICKET_ID_SEQUENCE = 1;
    private Map<Integer, Ticket> ticketDatabase = new LinkedHashMap<>();

    @Override
    public void init() throws ServletException {
        System.out.println("init");
    }

    @Override
    public void destroy() {
        System.out.println("destroy");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        if (action == null) {
            action = "list";
        }
        switch (action) {
            case "create":
                showTicketForm(resp);
                break;
            case "view":
                viewTicket(req, resp);
                break;
            case "download":
                downloadAttachment(req, resp);
                break;
            case "list":
            default:
                listTickets(resp);
                break;
        }
    }

    private void downloadAttachment(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String ticketId = req.getParameter("ticketId");
        Ticket ticket = getTicket(ticketId, resp);
        if (ticket == null) {
            return;
        }
        String name = req.getParameter("attachment");
        if (name == null) {
            resp.sendRedirect("tickets?action=view&ticketId=" + ticketId);
            return;
        }
        Attachment attachment = ticket.getAttachment(name);
        if (attachment == null) {
            resp.sendRedirect("tickets?action=view&ticketId=" + ticketId);
            return;
        }

        resp.setHeader("Content-Disposition",
                "attachment; filename=" + attachment.getName());
        resp.setContentType("application/octet-stream");
        ServletOutputStream outputStream = resp.getOutputStream();
        outputStream.write(attachment.getContents());

    }

    private void viewTicket(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String idString = req.getParameter("ticketId");
        Ticket ticket = this.getTicket(idString, resp);
        if (ticket == null)
            return;

        PrintWriter writer = this.writeHeader(resp);

        writer.append("<h2>Ticket #").append(idString)
                .append(": ").append(ticket.getSubject()).append("</h2>\r\n");
        writer.append("<i>Customer Name - ").append(ticket.getCustomerName())
                .append("</i><br/><br/>\r\n");
        writer.append(ticket.getBody()).append("<br/><br/>\r\n");

        if (ticket.getNumberOfAttachments() > 0) {
            writer.append("Attachments: ");
            int i = 0;
            for (Attachment attachment : ticket.getAttachments()) {
                if (i++ > 0)
                    writer.append(", ");
                writer.append("<a href=\"tickets?action=download&ticketId=")
                        .append(idString).append("&attachment=")
                        .append(attachment.getName()).append("\">")
                        .append(attachment.getName()).append("</a>");
            }
            writer.append("<br/><br/>\r\n");
        }

        writer.append("<a href=\"tickets\">Return to list tickets</a>\r\n");

        this.writeFooter(writer);

    }

    private Ticket getTicket(String ticketId, HttpServletResponse resp) throws IOException {
        if (ticketId == null || ticketId.length() == 0) {
            resp.sendRedirect("tickets");
            return null;
        }
        Ticket ticket = ticketDatabase.get(Integer.parseInt(ticketId));
        if (ticket == null) {
            resp.sendRedirect("tickets");
            return null;
        }
        return ticket;
    }


    private void showTicketForm(HttpServletResponse response)
            throws ServletException, IOException {
        PrintWriter writer = this.writeHeader(response);

        writer.append("<h2>Create a Ticket</h2>\r\n");
        writer.append("<form method=\"POST\" action=\"tickets\" ")
                .append("enctype=\"multipart/form-data\">\r\n");
        writer.append("<input type=\"hidden\" name=\"action\" ")
                .append("value=\"create\"/>\r\n");
        writer.append("Your Name<br/>\r\n");
        writer.append("<input type=\"text\" name=\"customerName\"/><br/><br/>\r\n");
        writer.append("Subject<br/>\r\n");
        writer.append("<input type=\"text\" name=\"subject\"/><br/><br/>\r\n");
        writer.append("Body<br/>\r\n");
        writer.append("<textarea name=\"body\" rows=\"5\" cols=\"30\">")
                .append("</textarea><br/><br/>\r\n");
        writer.append("<b>Attachments</b><br/>\r\n");
        writer.append("<input type=\"file\" name=\"file1\"/><br/><br/>\r\n");
        writer.append("<input type=\"submit\" value=\"Submit\"/>\r\n");
        writer.append("</form>\r\n");

        this.writeFooter(writer);
    }

    private void listTickets(HttpServletResponse response)
            throws ServletException, IOException {
        PrintWriter writer = writeHeader(response);

        writer.append("<h2>Tickets</h2>\r\n");
        writer.append("<a href=\"tickets?action=create\">Create Ticket")
                .append("</a><br/><br/>\r\n");

        if (this.ticketDatabase.size() == 0)
            writer.append("<i>There are no tickets in the system.</i>\r\n");
        else {
            for (int id : this.ticketDatabase.keySet()) {
                String idString = Integer.toString(id);
                Ticket ticket = this.ticketDatabase.get(id);
                writer.append("Ticket #").append(idString)
                        .append(": <a href=\"tickets?action=view&ticketId=")
                        .append(idString).append("\">").append(ticket.getSubject())
                        .append("</a> (customer: ").append(ticket.getCustomerName())
                        .append(")<br/>\r\n");
            }
        }

        this.writeFooter(writer);
    }

    private PrintWriter writeHeader(HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        PrintWriter writer = response.getWriter();
        writer.append("<!DOCTYPE html>\r\n")
                .append("<html>\r\n")
                .append("    <head>\r\n")
                .append("        <title>Customer Support</title>\r\n")
                .append("    </head>\r\n")
                .append("    <body>\r\n");
        return writer;
    }


    private void writeFooter(PrintWriter writer) {
        writer.append("    </body>\r\n").append("</html>\r\n");
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        if (action == null) {
            action = "list";
        }
        switch (action) {
            case "create":
                createTicket(req, resp);
                break;
            case "list":
            default:
                break;
        }
    }

    private void createTicket(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        Ticket ticket = new Ticket();
        ticket.setCustomerName(req.getParameter("customerName"));
        ticket.setSubject(req.getParameter("subject"));
        ticket.setBody(req.getParameter("body"));
        Part filePart = req.getPart("file1");
        if (filePart != null && filePart.getSize() > 0) {
            Attachment attachment = this.processAttachment(filePart);
            if (attachment != null)
                ticket.addAttachment(attachment);
        }
        int id;
        synchronized (this) {
            id = TICKET_ID_SEQUENCE++;
            ticketDatabase.put(id, ticket);
        }
        resp.sendRedirect("tickets?action=view&ticketId=" + id);

    }

    private Attachment processAttachment(Part filePart) throws IOException {
        InputStream is = filePart.getInputStream();
        ByteOutputStream bos = new ByteOutputStream();

        int read;
        final byte[] bytes = new byte[1024];
        while ((read = is.read(bytes)) != -1) {
            bos.write(bytes, 0, read);
        }
        Attachment attachment = new Attachment();
        attachment.setName(filePart.getSubmittedFileName());
        attachment.setContents(bos.getBytes());
        return attachment;
    }
}
