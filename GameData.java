import java.io.Serializable;

public class GameData implements Serializable
{
	private static final long serialVersionUID = 1L;
	public Tank tank;
	public Bullet bullet = null; // Has to be null here, otherwise it is not initialized
	public Explosion explosion = null;
	public int turn;
	
	public GameData()
	{
		tank = null;
	}
	
	public GameData(Tank t, int turn)
	{
		tank = t;
		this.turn = turn;
	}
}
