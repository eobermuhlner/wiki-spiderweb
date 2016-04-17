package ch.obermuhlner.android.wikibrowser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;
import ch.obermuhlner.android.lib.view.graph.Connection;
import ch.obermuhlner.android.lib.view.graph.Graph;
import ch.obermuhlner.android.lib.view.graph.Node;
import ch.obermuhlner.android.wikibrowser.util.XmlWriter;

public class GraphStorage {

	public static List<StoredGraph> getStoredGraphs(Context context) {
		List<StoredGraph> result = new ArrayList<StoredGraph>();
		
		File dir = context.getFilesDir();
		File[] files = dir.listFiles();
		
		for (File file : files) {
			StoredGraph storedGraph = new StoredGraph();
			
			String name = file.getName();
			if (name.endsWith(".xml")) {
				name = name.substring(0, name.length() - ".xml".length());
			}
			
			storedGraph.name = name;
			result.add(storedGraph);
		}
		
		return result;
	}
	
	public static void saveGraph(Context context, String name, Graph graph) {
		File dir = context.getFilesDir();
		
		File file = new File(dir, name + ".xml");
		file.delete();
		try {
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
			XmlWriter writer = new XmlWriter(bufferedWriter);
			
			writer.header();
			writer.openTag("graph");
			writer.attribute("version", "1");

			writer.openTag("nodes");
			for (Node node : graph.getNodes()) {
				writer.openTag("node");
				writer.attribute("name", node.name);
				writer.attribute("x", String.valueOf(node.x));
				writer.attribute("y", String.valueOf(node.y));
				writer.attribute("image", String.valueOf(node.visited));
				writer.attribute("visited", String.valueOf(node.visited));
				writer.attribute("fix", String.valueOf(node.fix));
				writer.textCDATA(node.content);
				writer.closeTag("node");
			}
			writer.closeTag("nodes");
			
			writer.openTag("connections");
			for (Connection connection : graph.getConnections()) {
				writer.openTag("connection");
				writer.attribute("from", connection.node1.name);
				writer.attribute("to", connection.node2.name);
				writer.closeTag("connection");
			}
			writer.closeTag("connections");
			
			writer.closeTag("graph");
			
			bufferedWriter.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static Graph loadGraph(Context context, String name) {
		File dir = context.getFilesDir();
		
		File file = new File(dir, name + ".xml");
		
		try {
			//System.out.println(toString(new FileInputStream(file)));
			
			FileInputStream stream = new FileInputStream(file);
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser parser = factory.newSAXParser();
			GraphHandler graphHandler = new GraphHandler();
			parser.parse(stream, graphHandler);
			return graphHandler.getGraph();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	

	private static String toString(InputStream stream) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		char[] buffer = new char[1024];
		
		StringBuilder result = new StringBuilder();
		int count = reader.read(buffer);
		while (count != -1) {
			result.append(buffer, 0, count);
			count = reader.read(buffer);
		}
		
		return result.toString();
	}

	public static class StoredGraph {
		public String name;
	}
	
	private static class GraphHandler extends DefaultHandler {

		private Graph graph = new Graph();

		private StringBuilder stringBuilder = new StringBuilder();

		private Node currentNode;

		public Graph getGraph() {
			return graph;
		}
		
		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			super.characters(ch, start, length);
	        stringBuilder.append(ch, start, length);
		}
		
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			super.startElement(uri, localName, qName, attributes);
			
			stringBuilder.setLength(0);
			
			if (localName.equals("node")) {
				String name = attributes.getValue("name");
				float x = Float.parseFloat(attributes.getValue("x"));
				float y = Float.parseFloat(attributes.getValue("y"));
				boolean image = Boolean.parseBoolean(attributes.getValue("image"));
				boolean visited = Boolean.parseBoolean(attributes.getValue("visited"));
				boolean fix = Boolean.parseBoolean(attributes.getValue("fix"));
				
				currentNode = new Node();
				currentNode.name = name;
				currentNode.x = x;
				currentNode.y = y;
				currentNode.image = image;
				currentNode.visited = visited;
				currentNode.fix = fix;
			} else if (localName.equals("connection")) {
				String fromName = attributes.getValue("from");
				String toName = attributes.getValue("to");
				graph.createConnection(fromName, toName);
			}
		}
		
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			super.endElement(uri, localName, qName);

			if (localName.equals("node")) {
				if (currentNode != null) {
					currentNode.content = stringBuilder.toString();
					graph.addNode(currentNode);
					currentNode = null;
				}
			}
		}
	}
}
