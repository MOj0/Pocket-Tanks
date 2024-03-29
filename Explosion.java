import java.io.Serializable;

@SuppressWarnings("serial")
public class Explosion implements Serializable
{
	public int[] position;
	public int size;
	public boolean hit;
	public int duration;
	public int team;
	
	public Explosion(int t, int[] pos, int s, int d)
	{
		team = t;
		position = pos;
		size = s;
		hit = false;
		duration = d;
	}
	
	public boolean update()
	{
		duration--;
		
		if(duration <= 0)
		{
			return false;
		}
		return true;
	}
	
	public boolean checkCollision(int tankX, int tankY, int tankSize)
	{
		if(!hit)
		{
			int deltaX = position[0] + size / 2 - tankX;
			int deltaY = position[1] + size / 2 - tankY;
			int distance = (int) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
			distance = distance - size / 2 - tankSize / 2;
			if(distance < 0)
			{
				hit = true;
				return true;
			}
		}
		return false;
	}
}
