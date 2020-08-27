
public class Tank
{
	private String name;
	private int index;
	private int health;
	private int size;
	private int angle;
	private int power;
	private boolean hasMoved;
	private int[] position;
	
	public Tank(String n, int i, int h, int s)
	{
		name = n;
		index = i;
		health = h;
		size = s;
		angle = 0;
		power = 50;
		hasMoved = false;
		position = new int[2];
	}
	
	public boolean isAlive()
	{
		return health > 0;
	}
	
	public void dealDamage(int d)
	{
		health -= d;
	}
	
	public void changeAngle(int a)
	{
		angle += a;
	}
	
	public void changePower(int p)
	{
		power = Math.max(1, Math.min(power + p, 100));
	}
	
	public void setIndex(int i)
	{
		index = i;
		// hasMoved = true; //TODO UNCOMMENT THIS
	}
	
	public void setMoved(boolean m)
	{
		hasMoved = m;
	}
	
	public void setHealth(int h)
	{
		health = h;
	}
	
	public void setPosition(int[] p)
	{
		position = p;
	}
	
	public int[] getPosition()
	{
		return position;
	}
	
	public String getName()
	{
		return name;
	}
	
	public int getIndex()
	{
		return index;
	}
	
	public int getHealth()
	{
		return health;
	}
	
	public int getSize()
	{
		return size;
	}
	
	public int getAngle()
	{
		return angle;
	}
	
	public int getPower()
	{
		return power;
	}
	
	public boolean getMoved()
	{
		return hasMoved;
	}
}
