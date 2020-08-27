
public class Bullet
{
	private int team;
	private int[] position;
	private int size;
	private int yAccel;
	private int xAccel;
	
	public Bullet(int t, int[] startPos, int angle, int power)
	{
		team = t;
		int xkoord = (int) (Math.cos(angle * Math.PI / 180) * 1);
		int ykoord = (int) (Math.sin(angle * Math.PI / 180) * 1);
		startPos[0] += xkoord;
		startPos[1] += ykoord;
		position = startPos;
		size = 10;
		power = power / 2;
		yAccel = (int) (Math.sin(angle * Math.PI / 180) * power);
		xAccel = (int) (Math.cos(angle * Math.PI / 180) * power);
	}
	
	public void updatePoisition()
	{
		position[1] += yAccel;
		position[0] += xAccel;
		yAccel++;
	}
	
	public int checkCollision(int quotient, int[] terrain)
	{
		if(position[1] > PocketTanks.HEIGHT) // Bullet is off screen
		{
			return PocketTanks.numberOfPoints + 1; // Nobody can collide at this point
		}
		
		for(int i = 1; i < terrain.length; i++)
		{
			// TODO: When bullet collides with the ground create an explosion (check for the
			// enemy tank in a radius)
			
			if(Math.abs(position[0] - i * quotient) <= quotient) // Check x coordinate
			{
				if(position[1] > terrain[i]) // If it is under the ground return immediately
				{
					return i;
				}
				if(Math.abs(position[1] - terrain[i]) <= Math.abs(terrain[i] - terrain[i + 1])) // Check y coord
				{
					return i;
				}
			}
		}
		return -1;
	}
	
	public int getTeam()
	{
		return team;
	}
	
	public int[] getPosition()
	{
		return position;
	}
	
	public int getSize()
	{
		return size;
	}
}
