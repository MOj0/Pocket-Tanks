import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.*;

public class Test
{
	final int WIDTH = 800, HEIGHT = 600;
	JFrame frame;
	Painter painter;
	Explosion[] explosions;
	int counter = 0;
	int size = 200;
	
	public Test()
	{
		painter = new Painter();
		
		frame = new JFrame("T E S T");
		frame.add(painter);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(WIDTH, HEIGHT);
		frame.setLocationRelativeTo(null);
		frame.setResizable(false);
		frame.setVisible(true);
		
		explosions = new Explosion[2];
	}
	
	public void render(Graphics g)
	{
		
		for(int i = 0; i < explosions.length; i++)
		{
			if(explosions[i] != null)
			{
				g.setColor(Color.black);
				g.fillOval(explosions[i].position[0], explosions[i].position[1], explosions[i].size,
						explosions[i].size);
				
				g.setColor(Color.red);
				g.fillOval(explosions[i].position[0] + explosions[i].size / 2,
						explosions[i].position[1] + explosions[i].size / 2, 4, 4);
			}
		}
	}
	
	public static void main(String[] args)
	{
		Test t = new Test();
	}
	
	private class Painter extends JPanel implements MouseListener
	{
		public Painter()
		{
			setLayout(null);
			requestFocus();
			setFocusable(true);
			addMouseListener(this);
		}
		
		@Override
		public void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			render(g);
		}
		
		@Override
		public void mouseClicked(MouseEvent e)
		{
		}
		
		@Override
		public void mousePressed(MouseEvent e)
		{
			int[] pos = {e.getX() - size / 2, e.getY() - size / 2};
			explosions[counter] = new Explosion(pos, size, 50);
			if(counter == 1)
			{
				boolean collision = explosions[0].checkCollision(explosions[1].position, explosions[1].size);
				System.out.println(collision);
			}
			counter = (counter + 1) % explosions.length;
			
			repaint();
		}
		
		@Override
		public void mouseReleased(MouseEvent e)
		{
		}
		
		@Override
		public void mouseEntered(MouseEvent e)
		{
		}
		
		@Override
		public void mouseExited(MouseEvent e)
		{
		}
	}
}
