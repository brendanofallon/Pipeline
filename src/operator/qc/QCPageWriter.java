package operator.qc;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

public class QCPageWriter {

	public void writePage(Writer writer, String content) throws IOException {
		writeHeader(writer);
		
		writer.write(content);
		
		writeFooter(writer);
	}
	
	private void writeFooter(Writer writer) throws IOException {
		writer.write("</div>	</div> <!--end wrap--> </body>\n </html>  ");
	}

	
	private void writeHeader(Writer writer) throws IOException {
		//writer.write("<html>\n <head> \n <link rel=\"stylesheet\" type=\"text/css\" href=\"acg.css\" /> \n	</head> <body> \n");
		writer.write("<!DOCTYPE html>");
		writer.write("<html lang=\"en\">	<head>	<meta charset=\"utf-8\"> <title>Quality metrics</title> \n");
		writer.write("<link rel=\"stylesheet\" href=\"styles/style.css\" /> <script src=\"https://ajax.googleapis.com/ajax/libs/jquery/1.6.3/jquery.min.js\"></script>	<script src=\"js/jquery.custom.js\"></script> \n");
		writer.write("</head>   <body>   	<div id=\"wrap\">    	<div id=\"header\"> <a href=\"qc-metrics.html\" title=\"Quality report\"><h1>Quality report</h1></a> \n");
        writer.write("<div id=\"nav\">	<ul> \n");                 	     
        writer.write("<li><a href=\"qc-metrics.html\" title =\"Overview\" class=\"current\">Overview</a><span>/</span></li> \n");
        writer.write("<li><a href=\"basequalities.html\" title =\"Base Qualities\">Base qualities</a><span>/</span></li> \n");
        writer.write("<li><a href=\"alignment.html\" title =\"Alignment Metrics\">Alignment metrics</a><span>/</span></li> \n");
        writer.write("<li><a href=\"variants.html\" title =\"Variant Metrics\">Variant Metrics</a><span>/</span></li> \n");
   		writer.write("<li><a href=\"pipelinestats.html\" title =\"Run metrics\">Pipeline statistics</a></li> \n");
		writer.write("</ul> </div>  </div> <div id=\"content\" > \n");
	}
}
