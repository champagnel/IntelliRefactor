package org.jetbrains.research.intellijdeodorant.core.distance;

import java.io.*;


public class WriteHtml {

    public static void writeHTML(String filePath, String html){
        File file = new File(filePath);

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        PrintStream printStream = null;
        try {
            printStream = new PrintStream(new FileOutputStream(file));
            printStream.print(html);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        System.out.println("Write html successful");
    }

    public static String generateHTML(String graph) {
        String html="<!DOCTYPE html>\r\n" +
                "<html>\r\n" +
                "    <head>\r\n" +
                "        <meta charset=\"UTF-8\">\r\n" +
                "        <title>graph</title>\r\n" +
                "        <script src=\"https://cdn.jsdelivr.net/npm/viz.js@1.8.0/viz.js\"></script>\r\n" +
                "        <script src=\"https://cdn.bootcss.com/jquery/3.3.1/jquery.js\"></script>\r\n" +
                "    </head>\r\n" +
                "    <body>\r\n" +
                "        <center id=\"messages_center\">\r\n" +
                "    \r\n" +
                "        </center>\r\n" +
                "        <script type=\"text/vnd.graphviz\" id=\"view_div\">"+
                graph +
                "</script>   \r\n" +
                "        <script type=\"text/javascript\">\r\n" +
                "            $(function(){\r\n" +
                "                    document.getElementById(\"messages_center\").innerHTML=Viz(document.getElementById(\"view_div\").innerHTML, \"SVG\");\r\n" +
                "                })\r\n" +
                "        </script>\r\n" +
                "</body>\r\n" +
                "</html>";

        return html;
    }
}
