package ch.obermuhlner.android.wikibrowser.util;

import java.io.IOException;
import java.io.Writer;

public class XmlWriter {

	private final Writer writer;
	
	private boolean tagOpen;

	private final String encoding;

	public XmlWriter(Writer writer) {
		this(writer, "utf-8");
	}
	
	public XmlWriter(Writer writer, String encoding) {
		this.writer = writer;
		this.encoding = encoding;
	}
	
	public void header() throws IOException {
		writer.append("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>");
	}

	public void singleTag(String name) throws IOException {
		finishOpenTag();

		writer.append("<");
		writer.append(name);
		writer.append("/>");
	}
	
	
	public void openTag(String name) throws IOException {
		finishOpenTag();
		
		writer.append("<");
		writer.append(name);
		
		tagOpen = true;
	}

	public void closeTag(String name) throws IOException {
		finishOpenTag();
		
		writer.append("</");
		writer.append(name);
		writer.append(">");
	}
	
	public void attribute(String name, String value) throws IOException {
		if (!tagOpen) {
			throw new RuntimeException("Attributes only inside an open tag.");
		}
				
		writer.append(" ");
		writer.append(name);
		writer.append("=\"");
		writer.append(value);
		writer.append("\"");
	}
	
	public void text(String text) throws IOException {
		finishOpenTag();
		
		writer.append(text);
	}

	public void textCDATA(String text) throws IOException {
		finishOpenTag();
		
		writer.append("<![CDATA[");
		writer.append(text);
		writer.append("]]>");
	}

	public void close() throws IOException {
		writer.close();
	}
	
	private void finishOpenTag() throws IOException {
		if (tagOpen) {
			writer.append(">");
			tagOpen = false;
		}
	}
}
