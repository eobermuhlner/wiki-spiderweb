package ch.obermuhlner.android.lib.view.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import ch.obermuhlner.android.lib.util.ScaleGestureDetector;
import ch.obermuhlner.android.lib.util.ScaleGestureDetector.SimpleOnScaleGestureListener;

public class GraphView extends View {

	private static final float NODE_HEIGHT_TOP_FACTOR = 1.0f;
	private static final float NODE_HEIGHT_BOTTOM_FACTOR = 1.5f;

	private static final boolean DEBUG = false;

	private final Paint paintConnection = new Paint();
	private final Paint paintVisitedConnection = new Paint();
	private final Paint paintNodeBackground = new Paint();
	private final Paint paintNodeNormal = new Paint();
	private final Paint paintNodeSelected = new Paint();
	private final Paint paintNodeVisited = new Paint();

	private final Paint paintInfo = new Paint();
	private final Paint paintInfoShadow = new Paint();

	private GestureDetector gestureDetector;
	private ScaleGestureDetector scaleGestureDetector;

	private Graph graph = new Graph();

	private long timeLastRendering;
	private long timeLastInteraction;

	private float timeFactor = 0.001f;

	private float offsetX;
	private float offsetY;
	private float scaleFactor = 1.0f;
	
	private float targetOffsetX;
	private float targetOffsetY;
	private float targetScaleFactor = scaleFactor;
	
	private float imageMaxScaleFactor = 20.0f;

	private float maxImageSize = 64f;
	
	private boolean autoClose = false;

	private OnNodeSelectListener onNodeSelectListener;

	private Node currentNode;

	private float maxTextSize = 28f;

	private float textSize;

	private volatile String message;

	private long simulationTimeout;

	private Vibrator vibrator;

	private boolean vibrate;
	
	public GraphView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		init(context);
	}

	public GraphView(Context context, AttributeSet attrs) {
		super(context, attrs);
	
		init(context);
	}

	public GraphView(Context context) {
		super(context);
		
		init(context);
	}
	
	private void init(Context context) {
		if (!isInEditMode()) {
			vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
		}

    	paintConnection.setColor(Color.BLACK);
		paintConnection.setAntiAlias(true);
		paintConnection.setAlpha(0x20);
		paintConnection.setStyle(Style.STROKE);
		paintConnection.setStrokeWidth(2);

    	paintVisitedConnection.setColor(Color.BLUE);
		paintVisitedConnection.setAntiAlias(true);
		paintVisitedConnection.setAlpha(0x80);
		paintVisitedConnection.setStyle(Style.STROKE);
		paintVisitedConnection.setStrokeWidth(2);

		paintNodeNormal.setColor(Color.BLACK);
		paintNodeNormal.setAntiAlias(true);
		paintNodeNormal.setStyle(Style.STROKE);

		paintNodeSelected.setColor(Color.RED);
		paintNodeSelected.setAntiAlias(true);
		paintNodeSelected.setStyle(Style.STROKE);

		paintNodeVisited.setColor(Color.BLUE);
		paintNodeVisited.setAntiAlias(true);
		paintNodeVisited.setStyle(Style.STROKE);

		paintNodeBackground.setColor(Color.LTGRAY);
		paintNodeBackground.setStyle(Style.FILL);

		paintInfo.setColor(Color.GRAY);
		paintInfo.setAntiAlias(true);
		paintInfo.setStyle(Style.STROKE);
		paintInfo.setTextSize(20);

		paintInfoShadow.setColor(Color.WHITE);
		paintInfoShadow.setAntiAlias(true);
		paintInfoShadow.setStyle(Style.STROKE);
		paintInfoShadow.setTextSize(20);

		gestureDetector = new GestureDetector(context, new MyGestureListener());
		//gestureDetector.setIsLongpressEnabled(false);

		scaleGestureDetector = new ScaleGestureDetector(context, new MyScaleGestureListener());
	}

	public void setOnNodeSelectListener(OnNodeSelectListener onNodeSelectListener) {
		this.onNodeSelectListener = onNodeSelectListener;
	}

	public void setStatusMessage(String message) {
		this.message = message;
	}

	public void setVibrate(boolean vibrate) {
		this.vibrate = vibrate;
	}
	
	public void setTextSize(float textSize) {
		this.textSize = textSize;
	}
	
	public void setMaxImageSize(float maxImageSize) {
		this.maxImageSize = maxImageSize;
	}

	public void setSimulationTimeout(long simulationTimeout) {
		this.simulationTimeout = simulationTimeout;
	}

	public void setTimeFactor(float timeFactor) {
		this.timeFactor = timeFactor;
	}

	public float getScaleFactor() {
		return scaleFactor;
	}

	public void setScaleFactor(float scaleFactor) {
		this.scaleFactor = scaleFactor;
	}
	
	public float getOffsetX() {
		return offsetX;
	}
	
	public void setOffsetX(float offsetX) {
		this.offsetX = offsetX;
	}
	
	public float getOffsetY() {
		return offsetY;
	}
	
	public void setOffsetY(float offsetY) {
		this.offsetY = offsetY;
	}
	
	public float getTargetOffsetX() {
		return targetOffsetX;
	}
	
	public void setTargetOffsetX(float targetOffsetX) {
		this.targetOffsetX = targetOffsetX;
	}
	
	public float getTargetOffsetY() {
		return targetOffsetY;
	}
	
	public void setTargetOffsetY(float targetOffsetY) {
		this.targetOffsetY = targetOffsetY;
	}
	
	public float getTargetScaleFactor() {
		return targetScaleFactor;
	}
	
	public void setTargetScaleFactor(float targetScaleFactor) {
		this.targetScaleFactor = targetScaleFactor;
	}

	public void setGraph(Graph newGraph) {
		graph.setGraph(newGraph);
		
		fitAllNodesIntoScreen();
	}
	
	public Graph getGraph() {
		return graph;
	}
	
	public Node selectNode(String name) {
		currentNode = graph.getNode(name);
		return currentNode;
	}
	
	public String getSelectedNode() {
		return currentNode == null ? null : currentNode.name;
	}
	
	public void centerCurrentNode() {
		if (currentNode != null) {
			targetOffsetX = -toPixelDeltaX(currentNode.x);
			targetOffsetY = -toPixelDeltaY(currentNode.y);
		}
	}

	public void resetInteractionTime() {
		timeLastInteraction = System.currentTimeMillis();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		boolean handled = scaleGestureDetector.onTouchEvent(event);
		if (!scaleGestureDetector.isInProgress()) {
			return gestureDetector.onTouchEvent(event);
		}
		return handled;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

		int width = measureWidth(widthMeasureSpec);
		int height = measureHeight(heightMeasureSpec);

		setMeasuredDimension(width, height);
	}

	private int measureWidth(int widthMeasureSpec) {
		int specMode = MeasureSpec.getMode(widthMeasureSpec);
		int specSize = MeasureSpec.getSize(widthMeasureSpec);
		
		switch(specMode) {
		case MeasureSpec.UNSPECIFIED:
			specSize = measureContentWidth();
			break;
		case MeasureSpec.AT_MOST:
			specSize = Math.min(specSize, measureContentWidth());
			break;
		case MeasureSpec.EXACTLY:
			// nothing to do
			break;
		}
		
		return specSize;
	}

	private int measureHeight(int heightMeasureSpec) {
		int specMode = MeasureSpec.getMode(heightMeasureSpec);
		int specSize = MeasureSpec.getSize(heightMeasureSpec);

		switch(specMode) {
		case MeasureSpec.UNSPECIFIED:
			specSize = measureContentHeight();
			break;
		case MeasureSpec.AT_MOST:
			specSize = Math.min(specSize, measureContentHeight());
			break;
		case MeasureSpec.EXACTLY:
			// nothing to do
			break;
		}

		return specSize;
	}

	private int measureContentWidth() {
		return 200;
	}

	private int measureContentHeight() {
		return 300;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawRGB(255, 255, 255);

//		canvas.drawCircle(toPixelX(0), toPixelY(0), 50, paintNodeSelected);
//		canvas.drawLine(toPixelX(0), toPixelY(0), toPixelX(FloatMath.sin(rotationAngle) * 100), toPixelY(FloatMath.cos(rotationAngle) * 100), paintNodeSelected);
//		canvas.drawText("rot="+rotationAngle, toPixelX(0), toPixelY(0), paintInfo);

		float scaledTextSize = Math.min(maxTextSize, textSize * scaleFactor);
		scaledTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
				scaledTextSize, getResources().getDisplayMetrics());
		paintNodeNormal.setTextSize(scaledTextSize);
		paintNodeSelected.setTextSize(scaledTextSize);
		paintNodeVisited.setTextSize(scaledTextSize);
		paintNodeBackground.setTextSize(scaledTextSize);

		float aInfoTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
				16, getResources().getDisplayMetrics());
		paintInfo.setTextSize(aInfoTextSize);
		paintInfoShadow.setTextSize(aInfoTextSize);
		
		long timeNow = System.currentTimeMillis();
		long deltaTimeRendering = timeLastRendering == 0 ? 0 : timeNow - timeLastRendering;
		long deltaTimeInteraction = timeLastInteraction == 0 ? 0 : timeNow - timeLastInteraction;

		// determine position of current node
		float currentNodePixelX = 0;
		float currentNodePixelY = 0;
		if (currentNode != null && !currentNode.drag && !currentNode.fix) {
			currentNodePixelX = toPixelX(currentNode.x);
			currentNodePixelY = toPixelY(currentNode.y);
		}

		// simulate
		boolean moving = graph.simulate(Math.min(deltaTimeRendering * timeFactor, 1.0f));

		// fix position of current node
		if (currentNode != null && !currentNode.drag && !currentNode.fix) {
			float deltaX = currentNodePixelX - toPixelX(currentNode.x);
			float deltaY = currentNodePixelY - toPixelY(currentNode.y);
			targetOffsetX += deltaX;
			targetOffsetY += deltaY;
			offsetX += deltaX;
			offsetY += deltaY;
		}

		// paint connections
		for (Connection connection : graph.getConnections()) {
			Paint paint = (connection.node1.visited && connection.node2.visited) ? paintVisitedConnection : paintConnection;
			canvas.drawLine(toPixelX(connection.node1.x), toPixelY(connection.node1.y), toPixelX(connection.node2.x), toPixelY(connection.node2.y), paint);
		}

		// paint nodes
		for (Node node : graph.getNodes()) {
			float nodeWidth = toNodePixelWidth(node);
			float nodeHeight = toNodePixelHeight(node);
			
			RectF nodeRect = toNodePixelRect(node);
			float nodeCornerRadius = nodeHeight / 8;
			float pinSize = nodeHeight / 4;
			
			Paint paint = node == currentNode ? paintNodeSelected : node.visited ? paintNodeVisited : paintNodeNormal;

			if (node.image) {
				if (node.bitmap != null) {
					canvas.drawRect(nodeRect, paintNodeBackground);
					canvas.drawBitmap(node.bitmap, null, nodeRect, paint);
				} else {
				}
				canvas.drawRect(nodeRect, paint);
			} else {
				canvas.drawRoundRect(nodeRect, nodeCornerRadius, nodeCornerRadius, paintNodeBackground);
				if (node.exploded) {
					paint.setStrokeWidth(2);
				}
				canvas.drawRoundRect(nodeRect, nodeCornerRadius, nodeCornerRadius, paint);
				paint.setStrokeWidth(0);
				canvas.drawText(node.name, toPixelX(node.x) - nodeWidth / 2, toPixelY(node.y), paint);
			}
			
			if (node.fix) {
				float pinX = nodeRect.right - nodeCornerRadius;
				float pinY = nodeRect.top;
				canvas.drawLine(pinX, pinY, pinX + pinSize, pinY - pinSize, paint);
				canvas.drawCircle(pinX + pinSize, pinY - pinSize, pinSize / 2, paintNodeBackground);
				canvas.drawCircle(pinX + pinSize, pinY - pinSize, pinSize / 2, paint);
			}
		}

		timeLastRendering = timeNow;

		// paint info text
		float textY = paintInfo.getFontSpacing();
		if (message != null) {
			textY = drawShadowText(canvas, message, 0, textY, paintInfo, paintInfoShadow);
		}
		if (DEBUG) {
			textY = drawShadowText(canvas, "scale=" + scaleFactor, 0, textY, paintInfo, paintInfoShadow);
			textY = drawShadowText(canvas, "offset=" + offsetX + "," + offsetY, 0, textY, paintInfo, paintInfoShadow);
			textY = drawShadowText(canvas, "time=" + deltaTimeRendering, 0, textY, paintInfo, paintInfoShadow);
			textY = drawShadowText(canvas, "nodes=" + graph.getNodes().size(), 0, textY, paintInfo, paintInfoShadow);
			textY = drawShadowText(canvas, "connections=" + graph.getConnections().size(), 0, textY, paintInfo, paintInfoShadow);
			if (currentNode != null) {
				textY = drawShadowText(canvas, "selected=" + currentNode, 0, textY, paintInfo, paintInfoShadow);
			}
		}

		boolean zooming = offsetX != targetOffsetX || offsetY != targetOffsetY || scaleFactor != targetScaleFactor;
		if (zooming) {
			offsetX = filterValue(offsetX, targetOffsetX);
			offsetY = filterValue(offsetY, targetOffsetY);
			scaleFactor = filterValue(scaleFactor, targetScaleFactor);
		}
		
		if ((moving && deltaTimeInteraction < simulationTimeout) || zooming) {
			invalidate();
		}
	}

	private float filterValue(float value, float targetValue) {
		if (Math.abs(value - targetValue) < Math.abs(targetValue) * 0.0001f) {
			return targetValue;
		} else {
			return value * 0.9f + targetValue * 0.1f;
		}
	}
	
	private float drawShadowText(Canvas canvas, String text, float x, float y, Paint paintText, Paint paintShadow) {
		canvas.drawText(text, x - 1, y - 1, paintShadow);
		canvas.drawText(text, x + 1, y - 1, paintShadow);
		canvas.drawText(text, x - 1, y + 1, paintShadow);
		canvas.drawText(text, x + 1, y + 1, paintShadow);

		canvas.drawText(text, x, y, paintText);

		return y + paintText.getFontSpacing();
	}

	private float toGraphX(float pixelX) {
		return toGraphDeltaX(pixelX - offsetX - getWidth() / 2);
	}

	private float toGraphY(float pixelY) {
		return toGraphDeltaY(pixelY - offsetY - getHeight() / 2);
	}

	private float toGraphDeltaX(float pixelX) {
		return pixelX / scaleFactor;
	}

	private float toGraphDeltaY(float pixelY) {
		return pixelY / scaleFactor;
	}

	private float toPixelX(float x) {
		return x * scaleFactor + offsetX + getWidth() / 2;
	}

	private float toPixelY(float y) {
		return y * scaleFactor + offsetY + getHeight() / 2;
	}

	private float toPixelDeltaX(float x) {
		return x * scaleFactor;
	}

	private float toPixelDeltaY(float y) {
		return y * scaleFactor;
	}

	private float toNodePixelWidth(Node node) {
		return paintNodeNormal.measureText(node.name);
	}

	private float toNodePixelHeight(Node node) {
		return -paintNodeNormal.getFontMetrics().top * NODE_HEIGHT_TOP_FACTOR + paintNodeNormal.getFontMetrics().bottom * NODE_HEIGHT_BOTTOM_FACTOR;
	}

	private RectF toNodePixelRect(Node node) {
		float pixelX = toPixelX(node.x);
		float pixelY = toPixelY(node.y);
		
		if (node.image) {
			float width = 16;
			float height = 16;
			if (node.bitmap != null) {
				int imageWidth = node.bitmap.getWidth();
				int imageHeight = node.bitmap.getHeight();
				if (imageWidth != 0 && imageHeight != 0) {
					width = node.bitmap.getWidth();
					height = node.bitmap.getHeight();

					if (width > maxImageSize) {
						height = height / width * maxImageSize;
						width = maxImageSize;
					}
					if (height > maxImageSize) {
						width = width / height * maxImageSize;
						height = maxImageSize;
					}

					width *= Math.min(scaleFactor, imageMaxScaleFactor);
					height *= Math.min(scaleFactor, imageMaxScaleFactor);
					width = Math.min(width, imageWidth);
					height = Math.min(height, imageHeight);
				}
			}
			return new RectF(pixelX - width/2, pixelY - height/2, pixelX + width/2, pixelY + height/2);
		} else {
			float width2 = toNodePixelWidth(node) / 2 + paintNodeNormal.measureText(" ");
			float heightUp = -paintNodeNormal.getFontMetrics().top * NODE_HEIGHT_TOP_FACTOR;
			float heightDown = paintNodeNormal.getFontMetrics().bottom * NODE_HEIGHT_BOTTOM_FACTOR;

			return new RectF(pixelX - width2, pixelY - heightUp, pixelX + width2, pixelY + heightDown);
		}
	}

	public Node findNode(float x, float y) {
		List<Node> reverseNodes = new ArrayList<Node>(graph.getNodes());
		Collections.reverse(reverseNodes);
		for (Node node : reverseNodes) {
			RectF nodePixelRect = toNodePixelRect(node);
			if (nodePixelRect.contains(x, y)) {
				graph.moveToFront(node);
				return node;
			}
		}
		return null;
	}

	private class MyGestureListener extends SimpleOnGestureListener {

		@Override
		public boolean onDown(MotionEvent e) {
			//System.out.println("DOWN ");
			if (currentNode != null) {
				currentNode.drag = false; // just to be sure
			}
			currentNode = findNode(e.getX(), e.getY());
			if (currentNode != null) {
				if (onNodeSelectListener != null) {
					onNodeSelectListener.selectNode(currentNode);
				}
				if (vibrate) {
					vibrator.vibrate(20);
				}
			}
			GraphView.this.invalidate();
			return true;
		}
		
		@Override
		public void onLongPress(MotionEvent e) {
			//System.out.println("LONG " + currentNode);
			if (currentNode != null) {
				if (currentNode.fix) {
					if (vibrate) {
						vibrator.vibrate(20);
					}
					currentNode.fix = false;
				}
				GraphView.this.invalidate();
			}
		}
		
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			//System.out.println("SCROLL " + distanceX + " " + distanceY);
			if (e1.getPointerId(0) != e2.getPointerId(0)) {
				return false;
			}
			if (currentNode != null) {
				resetInteractionTime();
				currentNode.fix = true;
				currentNode.drag = true;
				currentNode.x -= toGraphDeltaX(distanceX);
				currentNode.y -= toGraphDeltaY(distanceY);
				if (e2.getAction() == MotionEvent.ACTION_UP || e2.getAction() == MotionEvent.ACTION_CANCEL) {
					currentNode.drag = false;
				}
			} else {
				offsetX -= distanceX;
				offsetY -= distanceY;
				targetOffsetX = offsetX;
				targetOffsetY = offsetY;
			}
			GraphView.this.invalidate();
			return true;
		}
		
		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			if (currentNode != null) {
				currentNode.visited = true;
				if (currentNode.exploded) {
					currentNode.exploded = false;
					resetInteractionTime();
					graph.removeDirectConnections(currentNode);
				} else {
					currentNode.exploded = true;
					if (onNodeSelectListener != null) {
						resetInteractionTime();
						onNodeSelectListener.singleTapNode(currentNode);
						if (autoClose) {
							for (Node node : graph.getNodes()) {
								graph.removeDirectConnections(node);
							}
						}
					}
				}
				GraphView.this.invalidate();
			}

			return true;
		}

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			if (currentNode == null) {
				fitAllNodesIntoScreen();
			} else {
				currentNode.visited = true;

				if (onNodeSelectListener != null) {
					onNodeSelectListener.doubleTapNode(currentNode);
				}
			}

			return true;
		}
	}

	public class MyScaleGestureListener extends SimpleOnScaleGestureListener {
		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector) {
			//System.out.println("SCALEBEGIN ");
			if (currentNode != null) {
				currentNode.drag = false;
				currentNode = null;
			}
			
			float focusX = toGraphX(detector.getFocusX());
			float focusY = toGraphY(detector.getFocusY());

			if (focusX != 0 && focusY != 0) {
				offsetX = detector.getFocusX() - getWidth() / 2;
				offsetY = detector.getFocusY() - getHeight() / 2;
				targetOffsetX = offsetX;
				targetOffsetY = offsetY;

				for (Node node : graph.getNodes()) {
					node.x -= focusX;
					node.y -= focusY;
				}
			}

			return true;
		}

		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			//System.out.println("SCALE ");

			offsetX = detector.getFocusX() - getWidth() / 2;
			offsetY = detector.getFocusY() - getHeight() / 2;
			targetOffsetX = offsetX;
			targetOffsetY = offsetY;
			
//			rotationAngle = detector.getRotationAngle();
//			System.out.println("ROTATION " + rotationAngle * 2 * (float)Math.PI);
//			
//			for (Node node : graph.nodes) {
//				float angle = (float) Math.atan2(node.x, node.y) - rotationAngle;
//				float radius = FloatMath.sqrt(node.x * node.x + node.y  * node.y);
//				
//				node.x = radius * FloatMath.sin(angle);
//				node.y = radius * FloatMath.cos(angle);
//			}

			scaleFactor *= detector.getScaleFactor();
			targetScaleFactor = scaleFactor;
			
			GraphView.this.invalidate();
			return true;
		}

		@Override
		public void onScaleEnd(ScaleGestureDetector detector) {
			//System.out.println("SCALEEND ");
		}
	}

	public interface OnNodeSelectListener {
		boolean selectNode(Node node);
		boolean singleTapNode(Node node);
		boolean doubleTapNode(Node node);
	}

	public void clear() {
		graph.clear();
		offsetX = 0;
		offsetY = 0;
		scaleFactor = 1;
		
		targetOffsetX = offsetX;
		targetOffsetY = offsetY;
		targetScaleFactor = scaleFactor;
	}

	public void fitAllNodesIntoScreen() {
		Collection<Node> nodes = graph.getNodes();
		
		if (nodes.size() == 0) {
			return;
		}

		Node firstNode = nodes.iterator().next();

		if (nodes.size() == 1) {
			targetScaleFactor = 1;
			targetOffsetX = -firstNode.x;
			targetOffsetY = -firstNode.y;
			return;
		}

		float minX = firstNode.x;
		float maxX = firstNode.x;
		float minY = firstNode.y;
		float maxY = firstNode.y;

		for (Node node : nodes) {
			if (node.x < minX) {
				minX = node.x;
			}
			if (node.y < minY) {
				minY = node.y;
			}
			if (node.x > maxX) {
				maxX = node.x;
			}
			if (node.y > maxY) {
				maxY = node.y;
			}
		}
		
		float width = maxX - minX;
		float height = maxY - minY;

		float pixelWidth = getWidth();
		float pixelHeight = getHeight();

		float newScaleFactor = Math.min(pixelWidth / width, pixelHeight / height);

		targetScaleFactor = newScaleFactor * 0.8f;
		targetOffsetX = -minX * newScaleFactor - pixelWidth / 2;
		targetOffsetY = -minY * newScaleFactor - pixelHeight / 2;
	}
}
