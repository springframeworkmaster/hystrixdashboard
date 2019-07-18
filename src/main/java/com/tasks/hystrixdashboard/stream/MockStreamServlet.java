package com.tasks.hystrixdashboard.stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.Charset;

public class MockStreamServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(MockStreamServlet.class);

    public MockStreamServlet() {
        super();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String filename = request.getParameter("file");
        if (filename == null) {
            filename = "hystrix.stream";
        } else {
            filename = filename.replaceAll("\\.\\.", "");
            filename = filename.replaceAll("/", "");
        }
        int delay = 500;
        String delayArg = request.getParameter("delay");
        if (delayArg != null) {
            delay = Integer.parseInt(delayArg);
        }

        int batch = 1;
        String batchArg = request.getParameter("batch");
        if (batchArg != null) {
            batch = Integer.parseInt(batchArg);
        }

        String data = getFileFromPackage(filename);
        String lines[] = data.split("\n");

        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");

        int batchCount = 0;
        for (;;) {
            for (String s : lines) {
                s = s.trim();
                if (s.length() > 0) {
                    try {
                        response.getWriter().println(s);
                        response.getWriter().println("");
                        response.getWriter().flush();
                        batchCount++;
                    } catch (Exception e) {
                        logger.warn("Exception writing mock data to output.", e);
                        return;
                    }
                    if (batchCount == batch) {
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException e) {
                        }
                        batchCount = 0;
                    }
                }
            }
        }
    }

    private String getFileFromPackage(String filename) {
        try {
            String file = "/" + this.getClass().getPackage().getName().replace('.', '/') + "/" + filename;
            InputStream is = this.getClass().getResourceAsStream(file);
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
                StringWriter s = new StringWriter();
                int c = -1;
                while ((c = in.read()) > -1) {
                    s.write(c);
                }
                return s.toString();
            } finally {
                is.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not find file: " + filename, e);
        }
    }
}
