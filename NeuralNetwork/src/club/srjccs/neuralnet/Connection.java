package club.srjccs.neuralnet;

public class Connection
{
	public Node node;
	public float strength;
	public boolean selected = false;
	
	Connection(Node node)
	{
		this.node = node;
		this.strength = 1.0f;
	}
	
	Connection(Node node, float strength)
	{
		this.node = node;
		this.strength = strength;
	}
}
