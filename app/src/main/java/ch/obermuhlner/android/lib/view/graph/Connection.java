package ch.obermuhlner.android.lib.view.graph;

import ch.obermuhlner.android.lib.util.Check;
import ch.obermuhlner.android.lib.view.graph.Graph;
import ch.obermuhlner.android.lib.view.graph.Node;


public class Connection {

	public Node node1;
	public Node node2;
	
	public Connection(Node node1, Node node2) {
		Check.isNotNull(node1);
		Check.isNotNull(node2);
		
		this.node1 = node1;
		this.node2 = node2;
	}
	
	public float getDistanceSquare() {
		return Graph.getDistanceSquare(node1, node2);
	}
	
	public float getDistance() {
		return Graph.getDistance(node1, node2);
	}

	public void dispose() {
		node1.connectionCount--;
		node2.connectionCount--;
	}
	
	@Override
	public String toString() {
		return "Connection{" + node1 + "," + node2 + "}";
	}
}
