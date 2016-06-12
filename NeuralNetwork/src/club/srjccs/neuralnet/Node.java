package club.srjccs.neuralnet;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;

public class Node
{	
	private static final int ARROW_SIZE = NeuralNetwork.ARROW_SIZE;
	private static final double ARROW_ANGLE = NeuralNetwork.ARROW_ANGLE;
	private ArrayList<Connection> inputs;
	
	private int radius;
	private int x;
	private int y;
	private double value;
	
	Node(int x, int y, int radius, double value)
	{
		inputs = new ArrayList<Connection>();
		
		this.x = x;
		this.y = y;
		this.radius = radius;
		this.value = value;
	}
	
	public void update()
	{
		float totalStrength = 0;
		double totalValue = 0;
		Iterator<Connection> it = inputs.iterator();
		while(it.hasNext())
		{
			Connection conn = it.next();
			totalStrength += conn.strength;
			totalValue += conn.strength * conn.node.value;
		}
		if(totalStrength > 0)
		{
			this.value = totalValue / totalStrength;
		}
	}
	
	public boolean isConnectedTo(Node node)
	{
		return hasInput(node) || node.hasInput(this);
	}
	
	public boolean hasInput(Node node)
	{
		Iterator<Connection> it = inputs.iterator();
		while(it.hasNext())
		{
			Connection conn = it.next();
			if(conn.node == node) return true;
		}
		return false;
	}
	
	public void setValue(double value)
	{
		this.value = value;
	}
	
	public double getValue()
	{
		return this.value;
	}
	
	public void addInput(Node node)
	{
		if(!isConnectedTo(node) )
		{
			inputs.add(new Connection(node, 0.5f));
		}
	}
	
	public boolean removeInput(Node node)
	{
		Iterator<Connection> it = inputs.iterator();
		Connection conn = null;
		while(it.hasNext())
		{
			conn = it.next();
			if(conn.node == node)
			{
				inputs.remove(conn);
				return true;
			}
		}
		return false;
	}
	
	public boolean removeInput(Connection conn)
	{
		Iterator<Connection> it = inputs.iterator();
		while(it.hasNext())
		{
			Connection input = it.next();
			if(input == conn)
			{
				inputs.remove(conn);
				return true;
			}
		}
		return false;
	}
	
	public Iterator<Connection> getIterator()
	{
		return inputs.iterator();
	}
	
	public void setRadius(int radius)
	{
		this.radius = radius;
	}
	
	public int getRadius()
	{
		return radius;
	}
	
	public void setPosition(int x, int y)
	{
		this.x = x;
		this.y = y;
	}

	public void setPosition(Point p)
	{
		setPosition(p.x, p.y);
	}
	
	public Point getPosition()
	{
		return new Point(x, y);
	}
	
	public int getX()
	{
		return x;
	}
	
	public int getY()
	{
		return y;
	}
	
	public void draw(Graphics2D g)
	{
		g.setColor(new Color( (float)value, (float)value, (float)value ));
		g.fillOval(x - radius, y - radius, 2 * radius, 2 * radius);
		
		g.setColor(value <= 0.35 ? Color.WHITE : Color.BLACK);
		g.setStroke(new BasicStroke(2));
		g.drawOval(x - radius, y - radius, 2 * radius, 2 * radius);
		
		
		Iterator<Connection> it = inputs.iterator();
		while(it.hasNext())
		{
			Connection conn = it.next();
			Node node = conn.node;
			Point p1 = node.getPosition();
			Point p2 = this.getPosition();
			double theta = Math.atan2(p2.y - p1.y, p2.x - p1.x);
			
			p1.x += (int)((node.getRadius() + 1) * Math.cos(theta));
			p1.y += (int)((node.getRadius() + 1) * Math.sin(theta));
			p2.x -= (int)((this.getRadius() + 1) * Math.cos(theta));
			p2.y -= (int)((this.getRadius() + 1) * Math.sin(theta));

			Point p3 = new Point(
					(int)(p2.x - ARROW_SIZE * Math.cos(theta - ARROW_ANGLE)),
					(int)(p2.y - ARROW_SIZE * Math.sin(theta - ARROW_ANGLE)));
			
			Point p4 = new Point(
					(int)(p2.x - ARROW_SIZE * Math.cos(theta + ARROW_ANGLE)),
					(int)(p2.y - ARROW_SIZE * Math.sin(theta + ARROW_ANGLE)));
			
			g.setStroke(new BasicStroke(conn.strength * 4));
			float cValue = 0.5f-conn.strength*0.5f;
			if(conn.selected)
			{
				g.setColor(Color.RED);
			}
			else
			{
				g.setColor(new Color(cValue, cValue, cValue));
			}
			g.drawLine(p1.x, p1.y, p2.x, p2.y);
			g.drawLine(p3.x, p3.y, p2.x, p2.y);
			g.drawLine(p4.x, p4.y, p2.x, p2.y);
		}
	}
	
	public void drawSelection(Graphics2D g)
	{
		g.setStroke(new BasicStroke(4));
		g.setColor(Color.RED);
		g.drawOval(x - radius, y - radius, 2 * radius, 2 * radius);
	}
}
