package ch.obermuhlner.android.lib.view.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.FloatMath;
import ch.obermuhlner.android.lib.util.Check;

public class Graph {

	private static final int MAX_SPEED = 100;

	private static final float NOT_MOVING_THRESHOLD = 0.1f;

	private final static Random random = new Random();

	private static final boolean ADD_IMAGE_NODES_WHEN_LOADED = true;
	
	private float repulsionForce = 250f;

	private float attractionForce = 0.055f;

	private float damping = 0.85f;

	private float minConnectionDistance = 80f;

	private final Collection<Node> nodes = new CopyOnWriteArrayList<Node>();
	
	private final Collection<Connection> connections = new CopyOnWriteArrayList<Connection>();

	private int keepExplodedCount = 3;

	private int maxChildrenCount = 100;
	
	private int maxImageCount = 5;
	
	private List<Node> explodedNodes = new CopyOnWriteArrayList<Node>();
	
	private AsyncImageLoader imageLoader;

	private int imageSizeThreshold = 64;
	
	public void setImageLoader(AsyncImageLoader imageLoader) {
		this.imageLoader = imageLoader;
	}
	
	public void setImageSizeThreshold(int imageSizeThreshold) {
		this.imageSizeThreshold = imageSizeThreshold;
	}
	
	public void setRepulsionForce(float repulsionForce) {
		this.repulsionForce = repulsionForce;
	}
	
	public void setAttractionForce(float attractionForce) {
		this.attractionForce = attractionForce;
	}
	
	public void setDamping(float damping) {
		this.damping = damping;
	}

	public void setKeepExplodedCount(int keepExplodedCount) {
		this.keepExplodedCount = keepExplodedCount;
	}
	
	public void setMaxChildrenCount(int maxChildrenCount) {
		this.maxChildrenCount = maxChildrenCount;
	}
	
	public void setMaxImageCount(int maxImageCount) {
		this.maxImageCount = maxImageCount;
	}
	
	public synchronized Collection<Node> getNodes() {
		return nodes;
	}
	
	public synchronized Collection<Connection> getConnections() {
		return connections;
	}
	
	public synchronized void save(Bundle outState) {
		final int nodeCount = nodes.size();
		String[] nodeNames = new String[nodeCount];
		float[] xCoords = new float[nodeCount];
		float[] yCoords = new float[nodeCount];
		ArrayList<String> image = new ArrayList<String>();
		ArrayList<String> exploded = new ArrayList<String>();
		ArrayList<String> visited = new ArrayList<String>();
		ArrayList<String> fix = new ArrayList<String>();

		Iterator<Node> nodesIterator = nodes.iterator();
		for (int i = 0; i < nodeCount; i++) {
			Node node = nodesIterator.next();
			nodeNames[i] = node.name;
			xCoords[i] = node.x;
			yCoords[i] = node.y;
			if (node.image) {
				image.add(node.name);
			}
			if (node.exploded) {
				exploded.add(node.name);
			}
			if (node.visited) {
				visited.add(node.name);
			}
			if (node.fix) {
				fix.add(node.name);
			}
			
			ArrayList<String> children = new ArrayList<String>();
			for(Connection connection : connections) {
				if (connection.node1 == node) {
					children.add(connection.node2.name);
				}
				if (connection.node2 == node) {
					children.add(connection.node1.name);
				}
			}
			outState.putStringArrayList("children_" + node.name, children);
			if (node.content != null) {
				outState.putString("content_" + node.name, node.content);
			}
		}
		
		outState.putStringArray("nodes", nodeNames);
		outState.putFloatArray("nodes.x", xCoords);
		outState.putFloatArray("nodes.y", yCoords);
		outState.putStringArray("image", image.toArray(new String[0]));
		outState.putStringArray("exploded", exploded.toArray(new String[0]));
		outState.putStringArray("visited", visited.toArray(new String[0]));
		outState.putStringArray("fix", fix.toArray(new String[0]));
	}
	
	public synchronized void load(Bundle inState) {
    	Set<String> imagePages = new HashSet<String>(Arrays.asList(inState.getStringArray("image")));
    	Set<String> explodedPages = new HashSet<String>(Arrays.asList(inState.getStringArray("exploded")));
    	Set<String> visitedPages = new HashSet<String>(Arrays.asList(inState.getStringArray("visited")));
    	Set<String> fixPages = new HashSet<String>(Arrays.asList(inState.getStringArray("fix")));
    	String[] nodeNames = inState.getStringArray("nodes");
    	float[] coordsX = inState.getFloatArray("nodes.x");
    	float[] coordsY = inState.getFloatArray("nodes.y");
    	for (int i = 0; i < nodeNames.length; i++) {
        	ArrayList<String> children = inState.getStringArrayList("children_" + nodeNames[i]);
        	String content = null;
        	if (inState.containsKey("content_" + nodeNames[i])) {
        		inState.getString("content_" + nodeNames[i]);
        	}
    		final Node node = addNodes(nodeNames[i], content, children, Collections.<String> emptySet(), explodedPages.contains(nodeNames[i])); // FIXME images
    		node.x = coordsX[i];
    		node.y = coordsY[i];
    		node.visited = visitedPages.contains(nodeNames[i]);
    		node.fix = fixPages.contains(nodeNames[i]);
    		node.image = imagePages.contains(nodeNames[i]);
    		if (node.image) {
    			imageLoader.loadImage(node.name, new OnImageLoaded() {
    				@Override
    				public void onImageLoaded(String imageName, Bitmap image) {
    					node.bitmap = image;
    				}
    			});
    		}
		}
	}
	
	public synchronized void clear() {
		nodes.clear();
		connections.clear();
		imageLoader.clear();
	}
	
	public synchronized Node addNodes(String nodeName, String nodeContent, Collection<String> childNames, Collection<String> childImages, boolean manageExploding) {
		Check.isNotNull(nodeName);
		//System.out.println("ADDING " + nodeName + " " + childNames);
		Node oldNode = getNode(nodeName);
		final Node node;
		if (oldNode == null) {
			node = createNode(null, nodeName, nodeContent);
		} else {
			node = oldNode;
			node.content = nodeContent;
		}
		
		if (manageExploding && !node.image) {
			node.exploded = true;

			explodedNodes.remove(node);
			explodedNodes.add(node);
			while (explodedNodes.size() > keepExplodedCount) {
				Node nodeToImplode = explodedNodes.remove(0);
				removeDirectConnections(nodeToImplode);
			}
		}
	
		int count = 0;
		for (String childName : childNames) {
			if (++count < maxChildrenCount) { 
				Node childNode = getNode(childName);
				if (childNode == null) {
					childNode = createNode(node, childName, null);
				}
				Connection connection = getConnection(node, childNode);
				if (connection == null) {
					connection = createConnection(node, childNode);
				}
			}
		}

		//System.out.println("Adding images " + nodeName + " : " + childImages);
		int imageCount = 0;
		for (String imageName : childImages) {
			if (imageCount++ < maxImageCount) {
				if (ADD_IMAGE_NODES_WHEN_LOADED) {
					// add image nodes only after loading in background
					imageLoader.loadImage(imageName, new OnImageLoaded() {
						@Override
						public void onImageLoaded(String imageName, Bitmap imageBitmap) {
							if (node.exploded && (imageBitmap.getWidth() > imageSizeThreshold || imageBitmap.getHeight() > imageSizeThreshold)) {
								Node childNode = getNode(imageName);
								if (childNode == null) {
									childNode = createNode(node, imageName, null);
									childNode.image = true;
								}
								childNode.bitmap = imageBitmap;
								Connection connection = getConnection(node, childNode);
								if (connection == null) {
									connection = createConnection(node, childNode);
								}						
							}
						}
					});
				} else {
					// add image nodes now, but load images in background
					Node childNode = getNode(imageName);
					if (childNode == null) {
						childNode = createNode(node, imageName, null);
						childNode.image = true;
					}
					imageLoader.loadImage(imageName, childNode);
					Connection connection = getConnection(node, childNode);
					if (connection == null) {
						connection = createConnection(node, childNode);
					}
				}
			}
		}
		
		moveToFront(node);
		
		return node;
	}
	
	public synchronized void removeNode(Node rootNode) {
		removeNodes(Arrays.asList(rootNode));
	}

	public synchronized void removeNodes(Collection<Node> startNodes) {
		Collection<Node> nodesToRemove = new HashSet<Node>(startNodes);

		while (!nodesToRemove.isEmpty()) {
			Node node = nodesToRemove.iterator().next();
			nodesToRemove.remove(node);
			nodes.remove(node);

			List<Connection> connectionsToRemove = new ArrayList<Connection>();
			for (Connection connection : connections) {
				if (connection.node1 == node || connection.node2 == node) {
					connectionsToRemove.add(connection);
				}
			}
			
			for (Connection connection : connectionsToRemove) {
				connections.remove(connection);
				connection.dispose();

				if (connection.node1.connectionCount == 0) {
					nodesToRemove.add(connection.node1);
				}
				
				if (connection.node2.connectionCount == 0) {
					nodesToRemove.add(connection.node2);
				}
			}
		}
	}
	
	public synchronized void moveToFront(Node node) {
		nodes.remove(node);
		nodes.add(node);
	}

	public synchronized void removeDirectConnections(Node node) {
		node.exploded = false;
		List<Connection> connectionsToRemove = new ArrayList<Connection>();
		for (Connection connection : connections) {
			if (connection.node1 == node && !connection.node2.visited) {
				connectionsToRemove.add(connection);
			} else if (connection.node2 == node && !connection.node1.visited) {
				connectionsToRemove.add(connection);
			}
		}

		for (Connection connection : connectionsToRemove) {
			connections.remove(connection);
			connection.dispose();
			if (connection.node1 == node && connection.node2.connectionCount == 0) {
				nodes.remove(connection.node2);
			}
			if (connection.node2 == node && connection.node1.connectionCount == 0) {
				nodes.remove(connection.node1);
			}
		}
	}
	
	public synchronized void removeConnections(Node node) {
		List<Connection> connectionsToRemove = new ArrayList<Connection>();
		for (Connection connection : connections) {
			if (connection.node1 == node || connection.node2 == node) {
				connectionsToRemove.add(connection);
			}
		}
		
		removeConnections(connectionsToRemove);
	}

	private void removeConnections(Collection<Connection> startConnections) {
		Collection<Connection> connectionsToRemove = new HashSet<Connection>(startConnections);
		
		while (!connectionsToRemove.isEmpty()) {
			Connection connectionToRemove = connectionsToRemove.iterator().next();
			connectionsToRemove.remove(connectionToRemove);
			
			connections.remove(connectionToRemove);
			connectionToRemove.dispose();
			
			Node node1 = connectionToRemove.node1;
			Node node2 = connectionToRemove.node2;
			if (node1.connectionCount == 0) {
				nodes.remove(node1);

				for (Connection connection : connections) {
					if (connection.node1 == node1 || connection.node2 == node1) {
						connectionsToRemove.add(connection);
					}
				}
			}
			if (node2.connectionCount == 0) {
				nodes.remove(node2);

				for (Connection connection : connections) {
					if (connection.node1 == node2 || connection.node2 == node2) {
						connectionsToRemove.add(connection);
					}
				}
			}
		}
	}

	public synchronized Node getNode(String name) {
		for (Node node : nodes) {
			if (node.name.equals(name)) {
				return node;
			}
		}
		return null;
	}
	
	public synchronized Node createNode(Node closeNode, String name, String content) {
		Node node = new Node();
		nodes.add(node);
		
		node.x = (random.nextFloat() - 0.5f) * 100;
		node.y = (random.nextFloat() - 0.5f) * 100;
		if (closeNode != null) {
			node.x += closeNode.x;
			node.y += closeNode.y;
		}
		
		node.name = name;
		node.content = content;
		
		return node;
	}

	private Connection getConnection(Node node1, Node node2) {
		for (Connection connection : connections) {
			if ((node1 == connection.node1 && node2 == connection.node2) || (node2 == connection.node1 && node1 == connection.node2)) {
				return connection;
			}
		}
		return null;
	}

	public synchronized Connection createConnection(String node1Name, String node2Name) {
		Node node1 = getNode(node1Name);
		if (node1 == null) {
			return null;
		}
		Node node2 = getNode(node2Name);
		if (node2 == null) {
			return null;
		}
		return createConnection(node1, node2);
	}
	
	public synchronized Connection createConnection(Node node1, Node node2) {
		Connection connection = new Connection(node1, node2);
		connections.add(connection);
	
		connection.node1.connectionCount++;
		connection.node2.connectionCount++;
		
		return connection;
	}

	public synchronized boolean simulate_complex(float time) {
		for (Node node1 : nodes) {
			for (Node node2 : nodes) {
				if (node1 == node2) {
					continue;
				}
				
				float distanceSquare = getDistanceSquare(node1, node2);
				
				float deltaX = node1.x-node2.x;
				float deltaY = node1.y-node2.y;
				
				node1.forceX += repulsionForce * deltaX / distanceSquare;
				node1.forceY += repulsionForce * deltaY / distanceSquare;
			}
		}

		for (Connection connection : connections) {
			float distanceSquare = getDistanceSquare(connection.node1, connection.node2);

			float preferredConnectionDistance = minConnectionDistance;
			if (connection.node1.visited && connection.node2.visited) {
				preferredConnectionDistance *= 2;
			}
			if (connection.node1.exploded && connection.node2.exploded) {
				preferredConnectionDistance *= 2;
			}
			
			float preferredConnectionDistanceSquare = preferredConnectionDistance * preferredConnectionDistance;
			if (distanceSquare > preferredConnectionDistanceSquare) {
				float deltaX = connection.node2.x - connection.node1.x;
				float deltaY = connection.node2.y - connection.node1.y;
				
				float attractionX = attractionForce * deltaX;
				float attractionY = attractionForce * deltaY;
				
				connection.node1.forceX += attractionX;
				connection.node1.forceY += attractionY;
				
				connection.node2.forceX += -attractionX;
				connection.node2.forceY += -attractionY;
			}
		}

		boolean moving = false;
		for (Node node : nodes) {
			node.speedX = minAbs(MAX_SPEED, (node.speedX + node.forceX) * damping);
			node.speedY = minAbs(MAX_SPEED, (node.speedY + node.forceY) * damping);
			
			if (Math.abs(node.speedX) > NOT_MOVING_THRESHOLD || Math.abs(node.speedY) > NOT_MOVING_THRESHOLD) {
				moving = true;
			}

			if (!node.drag && !node.fix) {
				node.x += node.speedX * time;
				node.y += node.speedY * time;
			}

			node.forceX = 0;
			node.forceY = 0;
		}
		
		return moving;
	}

	public boolean simulate(float time) {
		for (Node node1 : nodes) {
			for (Node node2 : nodes) {
				if (node1 == node2) {
					continue;
				}
				
				float distanceSquare = getDistanceSquare(node1, node2);
				
				float deltaX = node1.x-node2.x;
				float deltaY = node1.y-node2.y;
				
				node1.forceX += repulsionForce * deltaX / distanceSquare;
				node1.forceY += repulsionForce * deltaY / distanceSquare;
			}
		}

		for (Connection connection : connections) {
			float deltaX = connection.node2.x - connection.node1.x;
			float deltaY = connection.node2.y - connection.node1.y;

			float attractionX = attractionForce * deltaX;
			float attractionY = attractionForce * deltaY;

			connection.node1.forceX += attractionX;
			connection.node1.forceY += attractionY;

			connection.node2.forceX += -attractionX;
			connection.node2.forceY += -attractionY;
		}

		boolean moving = false;
		for (Node node : nodes) {
			node.speedX = minAbs(MAX_SPEED, (node.speedX + node.forceX) * damping);
			node.speedY = minAbs(MAX_SPEED, (node.speedY + node.forceY) * damping);
			
			if (Math.abs(node.speedX) > NOT_MOVING_THRESHOLD || Math.abs(node.speedY) > NOT_MOVING_THRESHOLD) {
				moving = true;
			}

			if (!node.drag && !node.fix) {
				node.x += node.speedX * time;
				node.y += node.speedY * time;
			}

			node.forceX = 0;
			node.forceY = 0;
		}
		
		return moving;
	}

	private float minAbs(float v1, float v2) {
		if (v2 < 0) {
			return -v2 < v1 ? v2 : -v1;
		} else {
			return v2 < v1 ? v2 : v1;
		}
	}

	public static float getDistanceSquare(Node node1, Node node2) {
		float dx = node1.x - node2.x;
		float dy = node1.y - node2.y;
		return dx * dx + dy * dy;
	}
	
	public static float getDistance(Node node1, Node node2) {
		return FloatMath.sqrt(getDistanceSquare(node1, node2));
	}

	public static interface OnImageLoaded {
		void onImageLoaded(String imageName, Bitmap image);
	}

	public void setGraph(Graph newGraph) {
		clear();

		for (Node node : newGraph.getNodes()) {
			nodes.add(node);
		}
		for (Connection connection : newGraph.getConnections()) {
			connections.add(connection);
		}
	}

	public void addNode(Node node) {
		nodes.add(node);
	}
	
}
