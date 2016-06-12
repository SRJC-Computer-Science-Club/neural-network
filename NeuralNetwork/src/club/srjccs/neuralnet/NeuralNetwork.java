package club.srjccs.neuralnet;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class NeuralNetwork extends JPanel implements Runnable, KeyListener, MouseListener, MouseMotionListener, ChangeListener
{
	private static final long serialVersionUID = 963038068642225389L;
	
	public static final int ARROW_SIZE = 12;
	public static final double ARROW_ANGLE = Math.PI / 8.0;
	
	public static final Dimension DEFAULT_PANEL_SIZE = new Dimension(600, 600);
	public static final Dimension DEFAULT_PROPERTIES_SIZE = new Dimension(200, 600);
	public static final int MIN_RADIUS = 8;
	public static final int MAX_RADIUS = 40;
	public static final int LINE_SELECT_RADIUS = 6;
	
	private Thread thread;
	private JFrame frame;
	private JPanel properties;
	private JSlider radiusSlider;
	private JSlider valueSlider;
	private JSlider strengthSlider;
	private ArrayList<Node> nodes;
	
	private Node spawningNode = null;
	
	private boolean shiftIsDown = false;
	private Node draggingNode = null;
	private int initialDragX = 0;
	private int initialDragY = 0;
	
	private Node connectingNode = null;
	private int arrowX = 0;
	private int arrowY = 0;
	
	private Node selectedNode = null;
	private Connection selectedConnection = null;
	
	private boolean panning = false;
	private int viewX = 0;
	private int viewY = 0;
	private int initialPanX = 0;
	private int initialPanY = 0;
	
	public static void main(String[] agrs)
	{
		new NeuralNetwork();
	}
	
	NeuralNetwork()
	{
		nodes = new ArrayList<Node>();
		
		this.setPreferredSize(DEFAULT_PANEL_SIZE);
		this.setFocusable(false);
		this.addMouseListener(this);
		this.addMouseMotionListener(this);
		this.setBackground(Color.LIGHT_GRAY);
		
		properties = new JPanel();
		properties.setPreferredSize(DEFAULT_PROPERTIES_SIZE);
		properties.setLayout(new BoxLayout(properties, BoxLayout.PAGE_AXIS));
		properties.setFocusable(false);
		properties.setBorder(new EmptyBorder(10, 10, 10, 10));
		
		JLabel rsLabel = new JLabel("Radius");
		rsLabel.setHorizontalAlignment(SwingConstants.CENTER);
		properties.add(rsLabel);
		
		radiusSlider = new JSlider(JSlider.HORIZONTAL, MIN_RADIUS, MAX_RADIUS, MIN_RADIUS);
		radiusSlider.setMinorTickSpacing(4);
		radiusSlider.setMajorTickSpacing(16);
		radiusSlider.setPaintTicks(true);
		radiusSlider.setPaintLabels(true);
		radiusSlider.addChangeListener(this);
		radiusSlider.setFocusable(false);
		properties.add(radiusSlider);
		
		JLabel vLabel = new JLabel("Value");
		vLabel.setHorizontalAlignment(SwingConstants.CENTER);
		properties.add(vLabel);
		
		valueSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 100);
		valueSlider.setMinorTickSpacing(5);
		valueSlider.setMajorTickSpacing(25);
		valueSlider.setPaintTicks(true);
		valueSlider.setPaintLabels(true);
		valueSlider.addChangeListener(this);
		valueSlider.setFocusable(false);
		properties.add(valueSlider);
		
		JLabel strLabel = new JLabel("Strength");
		strLabel.setHorizontalAlignment(SwingConstants.CENTER);
		properties.add(strLabel);
		
		strengthSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 100);
		strengthSlider.setMinorTickSpacing(5);
		strengthSlider.setMajorTickSpacing(25);
		strengthSlider.setPaintTicks(true);
		strengthSlider.setPaintLabels(true);
		strengthSlider.addChangeListener(this);
		strengthSlider.setFocusable(false);
		properties.add(strengthSlider);
		
		updateProperties();
		
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, this, properties);
		splitPane.setResizeWeight(1.0);
		
		frame = new JFrame("Neural Network Editor");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setFocusable(true);
		frame.addKeyListener(this);
		
		frame.add(splitPane);
		frame.pack();
		frame.setVisible(true);
		
		frame.requestFocus();
		
		thread = new Thread(this);
		thread.start();
	}

	@Override
	public void run()
	{
		while(true)
		{
			Iterator<Node> it = nodes.iterator();
			while(it.hasNext())
			{
				it.next().update();
			}
			frame.repaint();
			
			try
			{
				Thread.sleep(10);
			}
			catch(Exception e) {}
		}
	}
	
	private void selectConnection(Connection conn)
	{
		if(selectedConnection != null)
		{
			selectedConnection.selected = false;
		}
		selectedConnection = conn;

		if(selectedConnection != null)
		{
			selectedConnection.selected = true;
		}
	}
	
	private void deleteNode(Node node)
	{
		nodes.remove(node);
		Iterator<Node> it = nodes.iterator();
		while(it.hasNext())
		{
			Node n = it.next();
			n.removeInput(node);
		}
		if(selectedNode == node)
		{
			selectedNode = null;
			updateProperties();
		}
	}
	
	private void deleteConnection(Connection conn)
	{
		Iterator<Node> it = nodes.iterator();
		while(it.hasNext())
		{
			Node node = it.next();
			if(node.removeInput(conn)) break;
		}
		if(selectedConnection == conn)
		{
			selectedConnection = null;
			updateProperties();
		}
	}
	
	private Node findNodeAt(Point p)
	{
		for(int i = nodes.size() - 1; i >= 0; i--)
		{
			Node node = nodes.get(i);
			if(p.distance(node.getPosition()) <= node.getRadius())
			{
				return node;
			}
		}
		return null;
	}
	
	private Connection findConnectionAt(Point p)
	{
		Connection closestConn = null;
		double shortestDist = Double.POSITIVE_INFINITY;
		for(Iterator<Node> nodeIt = nodes.iterator(); nodeIt.hasNext();)
		{
			Node node = nodeIt.next();
			for(Iterator<Connection> connIt = node.getIterator(); connIt.hasNext();)
			{
				Connection conn = connIt.next();
				Point p1 = conn.node.getPosition();
				Point p2 = node.getPosition();
				double theta = Math.atan2(p2.y - p1.y, p2.x - p1.x);
				int r1 = conn.node.getRadius();
				int r2 = node.getRadius();
				p1.x += r1 * Math.cos(theta);
				p1.y += r1 * Math.sin(theta);
				p2.x -= r2 * Math.cos(theta);
				p2.y -= r2 * Math.sin(theta);
				
				if(
						p.x >= Math.min(p1.x, p2.x) - LINE_SELECT_RADIUS &&
						p.x <= Math.max(p1.x, p2.x) + LINE_SELECT_RADIUS &&
						p.y >= Math.min(p1.y, p2.y) - LINE_SELECT_RADIUS &&
						p.y <= Math.max(p1.y, p2.y) + LINE_SELECT_RADIUS)
				{
					double dist = Math.abs((p2.y - p1.y)*p.x - (p2.x - p1.x)*p.y + p2.x*p1.y - p2.y*p1.x)
							/ Math.sqrt((p2.y - p1.y)*(p2.y - p1.y) + (p2.x - p1.x)*(p2.x - p1.x));
					if(dist < shortestDist)
					{
						closestConn = conn;
						shortestDist = dist;
					}
				}
			}
		}
		
		if(shortestDist <= LINE_SELECT_RADIUS)
		{
			return closestConn;
		}
		return null;
	}
	
	private Point getLocalPoint(Point p)
	{
		return new Point(p.x - viewX, p.y - viewY);
	}
	
	private void cancelAction()
	{
		spawningNode = null;
		draggingNode = null;
		connectingNode = null;
		panning = false;
		frame.repaint();
	}
	
	private void updateProperties()
	{
		radiusSlider.setEnabled(selectedNode != null);
		valueSlider.setEnabled(selectedNode != null);
		strengthSlider.setEnabled(selectedConnection != null);
		if(selectedNode != null)
		{
			radiusSlider.setValue(selectedNode.getRadius());
			valueSlider.setValue((int)(selectedNode.getValue()*100));
		}
		if(selectedConnection != null)
		{
			strengthSlider.setValue((int)(selectedConnection.strength*100));
		}
	}
	
	@Override
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		
		Graphics2D g2d = (Graphics2D)(g);
		
		g.translate(viewX, viewY);
		
		Iterator<Node> it = nodes.iterator();
		while(it.hasNext())
		{
			Node node = it.next();
			node.draw(g2d);
		}
		
		if(selectedNode != null)
		{
			selectedNode.drawSelection(g2d);
		}
		
		if(connectingNode != null)
		{
			g.setColor(Color.BLACK);
			g2d.setStroke(new BasicStroke(3));
			Point p1 = connectingNode.getPosition();
			Point p2 = new Point(arrowX, arrowY);
			int radius = connectingNode.getRadius();
			if(p1.distance(p2) > radius)
			{
				double theta = Math.atan2(p2.y - p1.y, p2.x - p1.x);
				p1.x += (radius + 1) * Math.cos(theta);
				p1.y += (radius + 1) * Math.sin(theta);

				Point p3 = new Point(
						(int)(p2.x - ARROW_SIZE * Math.cos(theta - ARROW_ANGLE)),
						(int)(p2.y - ARROW_SIZE * Math.sin(theta - ARROW_ANGLE)));
				
				Point p4 = new Point(
						(int)(p2.x - ARROW_SIZE * Math.cos(theta + ARROW_ANGLE)),
						(int)(p2.y - ARROW_SIZE * Math.sin(theta + ARROW_ANGLE)));
				
				g.drawLine(p1.x, p1.y, p2.x, p2.y);
				g.drawLine(p3.x, p3.y, p2.x, p2.y);
				g.drawLine(p4.x, p4.y, p2.x, p2.y);
			}
		}
	}
	
	@Override
	public void keyPressed(KeyEvent ev)
	{
		if(ev.getKeyCode() == KeyEvent.VK_DELETE)
		{
			if(selectedNode != null)
			{
				deleteNode(selectedNode);
				System.gc();
				frame.repaint();
			}
			if(selectedConnection != null)
			{
				deleteConnection(selectedConnection);
				System.gc();
				frame.repaint();
			}
		}
		if(ev.getKeyCode() == KeyEvent.VK_SHIFT)
		{
			shiftIsDown = true;
		}
	}
	
	@Override
	public void keyReleased(KeyEvent ev)
	{
		if(ev.getKeyCode() == KeyEvent.VK_SHIFT)
		{
			shiftIsDown = false;
		}
	}
	
	@Override
	public void mousePressed(MouseEvent ev)
	{
		cancelAction();
		Point p = getLocalPoint(ev.getPoint());
		if(ev.getButton() == MouseEvent.BUTTON1)
		{
			Node node = findNodeAt(p);
			if(node == null)
			{
				spawningNode = new Node(p.x, p.y, MIN_RADIUS, 1.0);
				nodes.add(spawningNode);
				frame.repaint();
			}
			else
			{
				if(shiftIsDown)
				{
					draggingNode = node;
					initialDragX = p.x - node.getX();
					initialDragY = p.y - node.getY();
				}
				else
				{
					connectingNode = node;
					arrowX = p.x;
					arrowY = p.y;
				}
			}
		}
		else if(ev.getButton() == MouseEvent.BUTTON2)
		{
			panning = true;
			initialPanX = p.x;
			initialPanY = p.y;
		}
		else if(ev.getButton() == MouseEvent.BUTTON3)
		{
			selectedNode = findNodeAt(p);
			if(selectedNode == null)
			{
				selectConnection(findConnectionAt(p));
			}
			else
			{
				selectConnection(null);
			}
			updateProperties();
			frame.repaint();
		}
	}
	
	@Override
	public void mouseDragged(MouseEvent ev)
	{
		Point p = getLocalPoint(ev.getPoint());
		if(spawningNode != null)
		{
			int radius = (int)( p.distance(spawningNode.getPosition()) );
			radius = Math.max(radius, MIN_RADIUS);
			radius = Math.min(radius, MAX_RADIUS);
			spawningNode.setRadius(radius);
			frame.repaint();
		}
		if(draggingNode != null)
		{
			draggingNode.setPosition(p.x - initialDragX, p.y - initialDragY);
			frame.repaint();
		}
		if(connectingNode != null)
		{
			arrowX = p.x;
			arrowY = p.y;
			frame.repaint();
		}
		if(panning)
		{
			viewX = ev.getX() - initialPanX;
			viewY = ev.getY() - initialPanY;
			frame.repaint();
		}
	}
	
	@Override
	public void mouseReleased(MouseEvent ev)
	{
		Point p = getLocalPoint(ev.getPoint());
		if(connectingNode != null)
		{
			Node node = findNodeAt(p);
			if(node != null && node != connectingNode)
			{
				node.addInput(connectingNode);
			}
		}
		cancelAction();
	}
	
	@Override
	public void mouseExited(MouseEvent e)
	{
		cancelAction();
	}

	@Override
	public void stateChanged(ChangeEvent ev)
	{
		if(selectedNode != null)
		{
			if(ev.getSource() == radiusSlider)
			{
				selectedNode.setRadius(radiusSlider.getValue());
				frame.repaint();
			}
			else if(ev.getSource() == valueSlider)
			{
				selectedNode.setValue(valueSlider.getValue()/100.0);
				frame.repaint();
			}
		}
		if(selectedConnection != null)
		{
			if(ev.getSource() == strengthSlider)
			{
				selectedConnection.strength = strengthSlider.getValue() / 100.0f;
				frame.repaint();
			}
		}
	}
	
	@Override
	public void mouseMoved(MouseEvent arg0) {}
	@Override
	public void mouseClicked(MouseEvent e) {}
	@Override
	public void mouseEntered(MouseEvent e) {}
	@Override
	public void keyTyped(KeyEvent e) {}
}
