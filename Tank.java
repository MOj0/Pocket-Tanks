import java.io.Serializable;

public class Tank implements Serializable
{
	private static final long serialVersionUID = 1L;
	public String name;
	public int index;
	public int health;
	public int size;
	public int angle;
	public int power;
	public boolean hasMoved;
	public int[] position;
	// Needed for bot
	public boolean wasHit;
	public int explosionDiff;
	
	public Tank(String n, int i, int s)
	{
		name = n;
		index = i;
		health = 100;
		size = s;
		angle = 0;
		power = 50;
		hasMoved = false;
		position = new int[2];
		wasHit = false;
		explosionDiff = 0;
	}
	
	public String printData()
	{
		return name + ", Health: " + health + " angle: " + angle;
	}
	
	public boolean isAlive()
	{
		return health > 0;
	}
	
	public void dealDamage(int d)
	{
		health -= d;
		wasHit = true;
	}
	
	public void changeAngle(int a)
	{
		angle += a;
	}
	
	public void changePower(int p)
	{
		power = Math.max(1, Math.min(power + p, 100));
	}
	
	public void setAnglePower(int a, int p)
	{
		angle = a;
		power = p;
	}
	
	public void setHealth(int h)
	{
		health = h;
	}
	
	public void setPosition(int[] p)
	{
		position = p;
	}
	
	public void setMoved(boolean m)
	{
		hasMoved = m;
	}
	
	public void setHit(boolean h)
	{
		wasHit = h;
	}
	
	public void setExplosionDiff(int ex)
	{
		explosionDiff = ex;
	}
	
	public void moveTank(int dir, int max)
	{
		if(hasMoved)
		{
			return;
		}
		int move = Math.abs(dir);
		boolean moveCondition = false;
		if(dir > 0 && max - index > move)
		{
			moveCondition = true;
		}
		else if(dir < 0 && index > move)
		{
			moveCondition = true;
		}
		
		if(moveCondition)
		{
			index += dir;
			hasMoved = true;
		}
	}
	
	public void setNewIndex(int i)
	{
		index = i;
		health = 100;
		angle = 0;
		power = 50;
		hasMoved = false;
		position = new int[2];
	}
}
