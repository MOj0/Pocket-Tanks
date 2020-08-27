
public class Explosion
{
	public int[] position;
	public int size;
	public boolean hit;
	public int duration;
	
	public Explosion(int[] pos, int s, int d)
	{
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
	
	public boolean checkCollision(int[] tankPos, int tankSize)
	{
		if(!hit)
		{
			int deltaX = position[0] - tankPos[0];
			int deltaY = position[1] - tankPos[1];
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
