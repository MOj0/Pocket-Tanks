
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
}
