import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.*;
import java.net.*;

public class PocketTanks implements Runnable
{
	public final static int WIDTH = 1024;
	public final static int HEIGHT = 768;
	private JFrame frame;
	private Painter painter;
	private Thread thread;
	
	// Network
	private String[] playerInfoLAN = {"", "", ""}; // name, IP, port
	private int infoCounter;
	private Socket socket;
	private ServerSocket serverSocket;
	private ObjectInputStream ois;
	private ObjectOutputStream oos;
	private boolean accepted;
	
	private int menu;
	private Font arial;
	private Font arialBig;
	private boolean gameStarted;
	private int gameState;
	private int turn;
	private int drawDelta;
	// menu
	private int rectWidth;
	private int rectHeight;
	private int rectStartY;
	private int rectDeltaY;
	// map
	public static int numberOfPoints;
	private int quotient;
	private int[][] stars;
	private int[] terrain;
	
	private Rectangle playerInputRect;
	private String playerName;
	private int fontWidth;
	
	private Tank[] tanks;
//	private Tank bot; //TODO Implement bot
// 	bot = new Tank((int) (Math.random() * WIDTH - WIDTH / 4 - 10), 100);
	
	private int[][] pPoints; // previous points needed for lerping
	private Bullet bullet;
	private Explosion explosion;
	private boolean hitText;
	
	public PocketTanks()
	{
		painter = new Painter();
		
		frame = new JFrame("Pocket Tanks");
		frame.add(painter);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(WIDTH, HEIGHT);
		frame.setLocationRelativeTo(null);
		// frame.setResizable(false);//TODO Uncomment
		frame.setVisible(true);
		
		tanks = new Tank[2];
		menu = 0;
		gameStarted = false;
		gameState = 0; // 0 - Nobody won, 1 - player1 won ...
		turn = 0;
		arial = new Font("Arial", Font.BOLD, 22);
		arialBig = new Font("Arial", Font.BOLD, 30);
		rectWidth = 250;
		rectHeight = 100;
		rectStartY = 200;
		rectDeltaY = 150;
		pPoints = new int[2][2];
		hitText = false;
		drawDelta = 10;
		
		numberOfPoints = 128;
		quotient = WIDTH / numberOfPoints;
		
		// Create the world and assign it to global variables
		World world = new World();
		terrain = world.terrain;
		stars = world.stars;
		
		playerInputRect = new Rectangle(WIDTH / 2 - 250, 110, 500, 75);
		playerName = "";
		
		infoCounter = 0;
		accepted = false;
		
		thread = new Thread(this);
		thread.start();
	}
	
	@SuppressWarnings("unused")
	public static void main(String[] args)
	{
		PocketTanks pt = new PocketTanks();
	}
	
	@SuppressWarnings("static-access")
	@Override
	public void run()
	{
		while(true)
		{
			tick();
			painter.repaint();
			
			try
			{
				thread.sleep(10);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public void tick()
	{
		if(menu == 3 && gameStarted && gameState == 0) // LAN game in progress
		{
			if(turn == 0) // First player sends the data
			{
				try
				{
					oos.writeObject(tanks[0]);
					tanks[0] = (Tank) ois.readObject();
				}
				catch(Exception ex)
				{
					ex.printStackTrace();
				}
			}
			else // Second player reads the data
			{
				try
				{
					tanks[1] = (Tank) ois.readObject();
					oos.writeObject(tanks[1]); // Has to write it back otherwise it doesn't work for SOME reason
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		
		// TODO Generalize for LAN game
		if(explosion != null)
		{
			int index = (turn + 1) % 2;
			boolean collision = explosion.checkCollision(tanks[index].getPosition()[0] + tanks[index].getSize() / 2,
					tanks[index].getPosition()[1] + tanks[index].getSize() / 2, tanks[index].getSize());
			
			if(collision)
			{
				tanks[index].dealDamage(25);
				if(!tanks[index].isAlive())
				{
					gameState = turn + 1;
				}
				hitText = true;
			}
			
			if(!explosion.update())
			{
				explosion = null;
				hitText = false;
				changeTurn();
			}
		}
		
		if(bullet != null)
		{
			bullet.updatePoisition();
			int collide = bullet.checkCollision(quotient, terrain);
			if(collide != -1)
			{
				explosion = new Explosion(new int[] {collide * quotient - 30, terrain[collide] - 30}, 60, 50);
				playSound("src/sounds/explosion.wav");
				bullet = null;
			}
		}
	}
	
	public void render(Graphics g)
	{
		g.setColor(new Color(4, 4, 30, 255)); // Nice background
		g.fillRect(0, 0, WIDTH, HEIGHT);
		
		if(menu == 0) // User is in the menu
		{
			g.setColor(Color.white);
			
			g.setFont(arialBig);
			g.drawString("Welcome to Pocket Tanks", WIDTH / 2 - 160, 100);
			
			g.setFont(arial);
			
			// 1st Button
			g.drawRect(WIDTH / 2 - rectWidth / 2, 200, rectWidth, rectHeight);
			g.setColor(Color.white);
			g.drawString("Play against PC", WIDTH / 2 - 80, rectStartY + rectHeight / 2);
			
			// 2nd Button
			g.drawRect(WIDTH / 2 - rectWidth / 2, 350, rectWidth, rectHeight);
			g.drawString("Play against player", WIDTH / 2 - 100, rectStartY + rectDeltaY + rectHeight / 2);
			
			// 3rd Button
			g.drawRect(WIDTH / 2 - rectWidth / 2, 500, rectWidth, rectHeight);
			g.drawString("LAN Game", WIDTH / 2 - 50, rectStartY + rectDeltaY * 2 + rectHeight / 2);
		}
		else if(menu == 1)
		{
			// TODO
		}
		else if(!gameStarted)
		{
			if(menu == 2) // PvP Game
			{
				g.setColor(Color.white);
				
				g.setFont(arial);
				g.drawString("Player " + (turn + 1) + ", enter your name", WIDTH / 2 - 125, 100);
				
				g.drawRect(playerInputRect.x, playerInputRect.y, playerInputRect.width, playerInputRect.height);
				g.drawString(playerName, playerInputRect.x + 5, playerInputRect.y + playerInputRect.height / 2 + 11);
				
				g.drawRect(WIDTH / 2 - 50, 500, 100, 75);
				g.drawString("OK", WIDTH / 2 - 20, 545);
			}
			else if(menu == 3) // LAN Game
			{
				g.setColor(Color.white);
				g.setFont(arial);
				g.drawString("Enter your name", WIDTH / 2 - 75, 100);
				
				if(infoCounter == 0)
				{
					g.setColor(Color.green);
				}
				g.drawRect(playerInputRect.x, playerInputRect.y, playerInputRect.width, playerInputRect.height);
				g.setColor(Color.white);
				g.drawString(playerInfoLAN[0], playerInputRect.x + 5,
						playerInputRect.y + playerInputRect.height / 2 + 10);
				
				// IP input
				g.drawString("Enter IP:", WIDTH / 2 - 40, 220);
				if(infoCounter == 1)
				{
					g.setColor(Color.green);
				}
				g.drawRect(playerInputRect.x, playerInputRect.y + 125, playerInputRect.width, playerInputRect.height);
				g.setColor(Color.white);
				g.drawString(playerInfoLAN[1], playerInputRect.x + 5,
						playerInputRect.y + playerInputRect.height / 2 + 130);
				
				// Port input
				g.drawString("Enter port:", WIDTH / 2 - 50, 345);
				if(infoCounter == 2)
				{
					g.setColor(Color.green);
				}
				g.drawRect(playerInputRect.x, playerInputRect.y + 250, playerInputRect.width, playerInputRect.height);
				g.setColor(Color.white);
				g.drawString(playerInfoLAN[2], playerInputRect.x + 5,
						playerInputRect.y + playerInputRect.height / 2 + 255);
				
				g.drawRect(WIDTH / 2 - 50, 500, 100, 75);
				g.drawString("OK", WIDTH / 2 - 20, 545);
			}
		}
		
		if(gameStarted && gameState == 0)
		{
			g.setFont(arial);
			g.setColor(Color.white);
			// Draw stars
			for(int i = 0; i < stars.length; i++)
			{
				g.fillOval(stars[i][0], stars[i][1], 5, 5);
			}
			
			// Draw terrain
			for(int i = 0; i < numberOfPoints - 1; i++)
			{
				g.drawLine(i * quotient, terrain[i], (i + 1) * quotient, terrain[i + 1]);
			}
			
			// Draw tanks, SHIT CAN MOVE THROUGH MOUNTAINS LMAO!!!
			for(int i = 0; i < tanks.length; i++)
			{
				g.setColor(Color.green.darker());
				if(i == 1)
				{
					g.setColor(Color.red.darker());
				}
				
				int index = tanks[i].getIndex();
				int[] tankPosition = {lerp(pPoints[i][0], index * quotient, 0.4) - drawDelta,
						lerp(pPoints[i][1], terrain[index], 0.4) - drawDelta};
				tanks[i].setPosition(tankPosition);
				// Draw current tank
				g.fillOval(tankPosition[0], tankPosition[1], tanks[i].getSize(), tanks[i].getSize());
				
				// Draw canon
				Graphics2D g2d = (Graphics2D) g.create(); // with g.create() method we have 2 different object,
															// which are SEPERATE!!!
				Rectangle rect = new Rectangle(tankPosition[0] + drawDelta / 2, tankPosition[1] + drawDelta / 2, 30,
						10);
				g2d.rotate(tanks[i].getAngle() * Math.PI / 180, rect.x + 5, rect.y + 5);
				g2d.fill(rect);
				
				tankPosition[0] += drawDelta;
				tankPosition[1] += drawDelta;
				// Display player name (above the tank)
				fontWidth = g.getFontMetrics(arial).stringWidth(tanks[i].getName());
				g.drawString(tanks[i].getName(), tankPosition[0] - fontWidth / 2,
						tankPosition[1] - g.getFontMetrics(arial).getHeight() - drawDelta);
				pPoints[i] = tankPosition;
				
				int fontWidthHealth = g.getFontMetrics(arial).stringWidth("Health: " + tanks[i].getHealth());
				fontWidth = Math.max(fontWidth, fontWidthHealth);
				if(i == 0)
				{
					g.drawString(tanks[i].getName() + ":", 30, 40);
					g.drawString("Health: " + tanks[i].getHealth(), 30, 65);
				}
				else
				{
					g.drawString(tanks[i].getName() + ":", WIDTH - fontWidth - 30, 40);
					g.drawString("Health: " + tanks[i].getHealth(), WIDTH - fontWidth - 30, 65);
				}
			}
			
			// Draw bullet
			if(bullet != null)
			{
				if(bullet.getTeam() == 0)
				{
					g.setColor(Color.green.darker());
				}
				else
				{
					g.setColor(Color.red.darker());
				}
				g.fillOval(bullet.getPosition()[0], bullet.getPosition()[1], bullet.getSize(), bullet.getSize());
			}
			
			// Draw explosion
			if(explosion != null)
			{
				g.setColor(Color.orange);
				g.fillOval(explosion.position[0], explosion.position[1], explosion.size, explosion.size);
			}
			
			// Display hit text
			if(hitText)
			{
				g.setFont(arialBig);
				g.setColor(Color.orange);
				fontWidth = g.getFontMetrics(arialBig).stringWidth("EPIC HIT");
				g.drawString("EPIC HIT", WIDTH / 2 - fontWidth / 2, 150);
			}
			
			g.setFont(arial);
			
			// UI
			String currentPlayer = "";
			int angle;
			int power;
			if(turn == 0)
			{
				g.setColor(Color.green.darker());
			}
			else
			{
				g.setColor(Color.red.darker());
			}
			currentPlayer = tanks[turn].getName();
			angle = Math.abs(tanks[turn].getAngle());
			power = tanks[turn].getPower();
			
			fontWidth = g.getFontMetrics(arial).stringWidth(currentPlayer);
			g.drawString(currentPlayer + "'s TURN!", WIDTH / 2 - fontWidth, 100);
			
			g.drawRect(WIDTH / 4, HEIGHT - 130, WIDTH / 2, 300);
			fontWidth = g.getFontMetrics(arial).stringWidth("Power: " + power);
			g.drawString("Angle: " + angle + "°", WIDTH / 4 + 35, HEIGHT - 110);
			g.drawString("Power: " + power, 3 * WIDTH / 4 - fontWidth - 35, HEIGHT - 110);
			
			// Buttons
			g.setFont(new Font("arial", Font.BOLD, 30));
			g.drawRect(WIDTH / 4 + 10, HEIGHT - 95, 75, 55);
			g.drawString("<", WIDTH / 4 + 38, HEIGHT - 55);
			g.drawRect(WIDTH / 4 + 85, HEIGHT - 95, 75, 55);
			g.drawString(">", WIDTH / 4 + 115, HEIGHT - 55);
			
			g.drawRect(WIDTH - WIDTH / 4 - 155, HEIGHT - 95, 75, 55);
			g.drawString("-", WIDTH - WIDTH / 4 - 125, HEIGHT - 60);
			g.drawRect(WIDTH - WIDTH / 4 - 80, HEIGHT - 95, 75, 55);
			g.drawString("+", WIDTH - WIDTH / 4 - 45, HEIGHT - 60);
			
			g.drawRect(WIDTH / 2 - 60, HEIGHT - 120, 125, 80);
			g.drawString("FIRE!", WIDTH / 2 - 35, HEIGHT - 70);
			
			g.drawRect(WIDTH / 4 - 115, HEIGHT - 100, 100, 50);
			g.drawString("<<", WIDTH / 4 - 85, HEIGHT - 65);
			
			g.drawRect(3 * WIDTH / 4 + 15, HEIGHT - 100, 100, 50);
			g.drawString(">>", 3 * WIDTH / 4 + 50, HEIGHT - 65);
		}
		else if(gameStarted) // We have a winner
		{
			// Draw stars
			g.setColor(Color.white);
			for(int i = 0; i < stars.length; i++)
			{
				g.fillOval(stars[i][0], stars[i][1], 5, 5);
			}
			
			g.setFont(arialBig);
			if(gameState == 1)
			{
				g.setColor(Color.green);
			}
			else
			{
				g.setColor(Color.red);
			}
			
			fontWidth = g.getFontMetrics(arialBig).stringWidth(tanks[gameState - 1].getName() + " WON!");
			g.drawString(tanks[gameState - 1].getName() + " WON!", WIDTH / 2 - fontWidth / 2, 50);
			
			fontWidth = g.getFontMetrics(arialBig).stringWidth("Click any key to restart");
			g.drawString("Click any key to restart", WIDTH / 2 - fontWidth / 2, 90);
		}
	}
	
	public void buttonOK()
	{
		if(menu == 2) // PvP game
		{
			if(playerName.equals(""))
			{
				playerName = "Player " + (turn + 1);
			}
			if(turn == 0)
			{
				tanks[0] = new Tank(playerName, (int) (Math.random() * numberOfPoints / 4) + 3, 100, 20);
			}
			else
			{
				tanks[1] = new Tank(playerName,
						(int) (Math.random() * numberOfPoints / 4 + (3 * numberOfPoints / 4) - 3), 100, 20);
				gameStarted = true;
			}
			turn = (turn + 1) % 2;
			playerName = "";
		}
		else if(menu == 3) // LAN Game
		{
			if(infoCounter == 0) // player name
			{
				if(playerInfoLAN[0].equals(""))
				{
					playerInfoLAN[0] = "Player " + (turn + 1);
				}
				infoCounter++;
			}
			else if(infoCounter == 1) // IP
			{
				boolean inputError = false;
				String ip1 = playerInfoLAN[infoCounter];
				
				if(ip1.length() == 0 || ip1.length() > 15 || ip1.substring(ip1.length() - 1, ip1.length()).equals("."))
				{
					inputError = true;
				}
				
				String[] ipTable = ip1.split("\\.");
				if(ipTable.length != 4)
				{
					inputError = true;
				}
				else
				{
					for(int i = 0; i < ipTable.length; i++)
					{
						int ipOctet = 0;
						try
						{
							ipOctet = Integer.parseInt(ipTable[i]);
						}
						catch(Exception e)
						{
							inputError = true;
							break;
						}
						if(ipOctet > 255)
						{
							inputError = true;
							break;
						}
					}
				}
				
				if(!inputError)
				{
					infoCounter++;
				}
				else
				{
					playerInfoLAN[infoCounter] = "";
					JOptionPane.showMessageDialog(null, "Wrong IP, try again");
				}
			}
			else if(infoCounter == 2) // port
			{
				boolean inputError = false;
				String portString = playerInfoLAN[infoCounter];
				int port1 = 0;
				try
				{
					port1 = Integer.parseInt(portString);
				}
				catch(Exception ex)
				{
					inputError = true;
				}
				
				if(port1 < 1024 || port1 > 65535)
				{
					inputError = true;
				}
				
				if(!inputError)
				{
					infoCounter++;
				}
				else
				{
					playerInfoLAN[infoCounter] = "";
					JOptionPane.showMessageDialog(null, "Wrong port, try again");
				}
			}
			
			// Initialize the LAN game
			if(infoCounter == 3 && !gameStarted)
			{
				String ip = playerInfoLAN[1];
				int port = Integer.parseInt(playerInfoLAN[2]);
				
				if(!connect(ip, port))
				{
					initializeServer(ip, port);
					System.out.println("Waiting for client...");
				}
				if(!accepted)
				{
					listenForServerRequest();
				}
				
				if(turn == 0) // Player 1
				{
					tanks[0] = new Tank(playerInfoLAN[0], (int) (Math.random() * numberOfPoints / 4) + 3, 100, 20);
				}
				else // Player 2
				{
					tanks[0] = new Tank(playerInfoLAN[0],
							(int) (Math.random() * numberOfPoints / 4 + (3 * numberOfPoints / 4) - 3), 100, 20);
				}
				
				// Exchange info about tanks
				try
				{
					oos.writeObject(tanks[0]);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
				
				try
				{
					tanks[1] = (Tank) ois.readObject();
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
				
				// Send over the "world" so its the same on both clients
				if(turn == 0)
				{
					try
					{
						oos.writeObject(terrain);
						oos.writeObject(stars);
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
				}
				else
				{
					try
					{
						terrain = (int[]) ois.readObject();
						stars = (int[][]) ois.readObject();
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
				}
				gameStarted = true;
			}
		}
		
	}
	
	public void moveTank(String dir)
	{
		int move = 0;
		boolean moveCondition = false;
		
		if(dir.equalsIgnoreCase("left"))
		{
			move = -2;
			if(tanks[turn].getIndex() > Math.abs(move))
			{
				moveCondition = true;
			}
		}
		else if(dir.equalsIgnoreCase("right"))
		{
			move = 2;
			if(tanks[turn].getIndex() < numberOfPoints - move)
			{
				moveCondition = true;
			}
		}
		
		if(moveCondition && !tanks[turn].getMoved())
		{
			tanks[turn].setIndex(tanks[turn].getIndex() + move);
		}
	}
	
	public Bullet fire()
	{
		if(explosion == null)
		{
			playSound("src/sounds/shoot.wav");
			Tank currentTank = tanks[turn];
			return new Bullet(turn, new int[] {currentTank.getIndex() * quotient, terrain[currentTank.getIndex()] - 10},
					currentTank.getAngle(), currentTank.getPower());
		}
		return null;
	}
	
	public void changeTurn()
	{
		turn = (turn + 1) % 2;
		tanks[turn].setMoved(false);
	}
	
	public void restartGame()
	{
		gameState = 0;
		
		for(int i = 0; i < terrain.length; i++)
		{
			terrain[i] = 0;
		}
		
		terrain[0] = (int) (Math.random() * HEIGHT / 2 + HEIGHT / 4);
		for(int i = quotient - 1; i < WIDTH; i += quotient)
		{
			terrain[i] = (int) (Math.random() * HEIGHT / 2 + HEIGHT / 4);
		}
		
		for(int i = 1; i <= terrain.length - (quotient - 1); i += quotient)
		{
			if(terrain[i] == 0)
			{
				int q = i / quotient;
				int diff = terrain[(q + 1) * quotient - 1] - terrain[Math.max(q * quotient - 1, 0)];
				diff = diff / quotient;
				
				for(int j = 0; j < quotient; j++)
				{
					if(i + j > terrain.length - 1)
					{
						break;
					}
					terrain[i + j] = terrain[Math.max(q * quotient - 1, 0)] + j * diff;
				}
			}
		}
		
		for(int i = 0; i < tanks.length; i++)
		{
			String tankName = tanks[i].getName();
			int spawn = (int) (Math.random() * numberOfPoints / 4) + 3;
			if(i != 0)
			{
				spawn = (int) (Math.random() * numberOfPoints / 4 + (3 * numberOfPoints / 4) - 3);
			}
			tanks[i] = new Tank(tankName, spawn, 100, 20);
		}
	}
	
	public void playSound(String sound)
	{
		try
		{
			File musicPath = new File(sound);
			AudioInputStream audioInput = AudioSystem.getAudioInputStream(musicPath);
			Clip clip = AudioSystem.getClip();
			clip.open(audioInput);
			clip.start();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private boolean connect(String ip, int port)
	{
		try
		{
			socket = new Socket(ip, port);
			ois = new ObjectInputStream(socket.getInputStream());
			oos = new ObjectOutputStream(socket.getOutputStream());
			accepted = true;
		}
		catch(IOException e)
		{
			System.out.println("Can't connect to: " + ip + "; starting server...");
			return false;
		}
		turn = 1;
		System.out.println("Connected to server!");
		return true;
	}
	
	private void initializeServer(String ip, int port)
	{
		try
		{
			serverSocket = new ServerSocket(port, 8, InetAddress.getByName(ip));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		turn = 0;
	}
	
	private void listenForServerRequest()
	{
		try
		{
			socket = serverSocket.accept();
			oos = new ObjectOutputStream(socket.getOutputStream());
			ois = new ObjectInputStream(socket.getInputStream());
			accepted = true;
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public int lerp(double v0, double v1, double t)
	{
		return (int) (v0 + t * (v1 - v0));
	}
	
	// Additional classes
	private class World implements Serializable
	{
		private static final long serialVersionUID = 1L;
		int[] terrain = new int[WIDTH];
		int[][] stars = new int[15][2];
		
		public World()
		{
			// Stars
			for(int i = 0; i < stars.length; i++)
			{
				stars[i][0] = (int) (Math.random() * WIDTH);
				stars[i][1] = (int) (Math.random() * HEIGHT / 5);
			}
			
			// Terrain
			terrain[0] = (int) (Math.random() * HEIGHT / 2 + HEIGHT / 4);
			for(int i = quotient - 1; i < WIDTH; i += quotient)
			{
				terrain[i] = (int) (Math.random() * HEIGHT / 2 + HEIGHT / 4);
			}
			for(int i = 1; i <= terrain.length - (quotient - 1); i += quotient)
			{
				if(terrain[i] == 0)
				{
					int q = i / quotient;
					int diff = terrain[(q + 1) * quotient - 1] - terrain[Math.max(q * quotient - 1, 0)];
					diff = diff / quotient;
					
					for(int j = 0; j < quotient; j++)
					{
						if(i + j > terrain.length - 1)
						{
							break;
						}
						terrain[i + j] = terrain[Math.max(q * quotient - 1, 0)] + j * diff;
					}
				}
			}
		}
	}
	
	private class Painter extends JPanel implements MouseListener, KeyListener
	{
		private static final long serialVersionUID = 1L;
		
		public Painter()
		{
			setLayout(null);
			requestFocus();
			setFocusable(true);
			addMouseListener(this);
			addKeyListener(this);
		}
		
		@Override
		public void paintComponent(Graphics g) // Called by painter.repaint() method
		{
			super.paintComponent(g);
			render(g);
		}
		
		@Override
		public void mousePressed(MouseEvent e)
		{
			int x = e.getX();
			int y = e.getY();
			int WIDTH = PocketTanks.WIDTH;
			int HEIGHT = PocketTanks.HEIGHT;
			
			if(menu == 0)
			{
				if(x >= WIDTH / 2 - rectWidth / 2 && x <= WIDTH / 2 - rectWidth / 2 + rectWidth)
				{
					for(int i = 0; i < 3; i++)
					{
						if(y >= rectStartY + rectDeltaY * i && y <= rectStartY + rectDeltaY * i + rectHeight)
						{
							menu = i + 1;
							break;
						}
					}
				}
			}
			else if((menu == 2 || menu == 3) && !gameStarted)
			{
				if(x >= WIDTH / 2 - 50 && x <= WIDTH / 2 + 50 && y >= 500 && y <= 575)
				{
					buttonOK();
				}
			}
			else if(gameStarted && gameState == 0 && ((menu == 3 && turn == 0) || menu == 2))
			{
				if(y >= HEIGHT - 100 && y <= HEIGHT - 50)
				{
					if(x >= WIDTH / 4 - 115 && x <= WIDTH / 4 - 15)
					{
						moveTank("left");
					}
					else if(x >= 3 * WIDTH / 4 + 15 && x <= 3 * WIDTH / 4 + 115)
					{
						moveTank("right");
					}
				}
				if(y >= HEIGHT - 95 && y <= HEIGHT - 40)
				{
					if(x >= WIDTH / 4 + 10 && x <= WIDTH / 4 + 85)
					{
						tanks[turn].changeAngle(-1);
					}
					else if(x >= WIDTH / 4 + 85 && x <= WIDTH / 4 + 140)
					{
						tanks[turn].changeAngle(1);
					}
					else if(x >= WIDTH - WIDTH / 4 - 155 && x <= WIDTH - WIDTH / 4 - 100)
					{
						tanks[turn].changePower(-1);
					}
					else if(x >= WIDTH - WIDTH / 4 - 80 && x <= WIDTH - WIDTH / 4 - 25)
					{
						tanks[turn].changePower(1);
					}
				}
				if(y >= HEIGHT - 120 && y <= HEIGHT - 40)
				{
					if(x >= WIDTH / 2 - 60 && x <= WIDTH / 2 + 65 && bullet == null)
					{
						bullet = fire();
					}
				}
			}
			else if(menu == 2 && gameState != 0)
			{
				restartGame(); // TODO FIX THIS FOR LAN GAME
			}
		}
		
		@Override
		public void mouseClicked(MouseEvent e)
		{
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
		
		@Override
		public void keyTyped(KeyEvent e)
		{
		}
		
		@Override
		public void keyPressed(KeyEvent e)
		{
			int key = e.getKeyCode();
			if(menu == 2 && !gameStarted)
			{
				if(key == 10) // enter
				{
					buttonOK();
				}
				else if(key == 8 && playerName.length() > 0) // backspace
				{
					playerName = playerName.substring(0, playerName.length() - 1);
				}
				else
				{
					playerName += e.getKeyChar();
				}
			}
			else if(menu == 3 && !gameStarted) // LAN game info input
			{
				if(key == 10) // enter
				{
					buttonOK();
				}
				else if(key == 8 && playerInfoLAN[infoCounter].length() > 0) // backspace
				{
					playerInfoLAN[infoCounter] = playerInfoLAN[infoCounter].substring(0,
							playerInfoLAN[infoCounter].length() - 1);
				}
				else
				{
					playerInfoLAN[infoCounter] += e.getKeyChar();
				}
			}
			else if(gameStarted && gameState == 0 && ((menu == 3 && turn == 0) || menu == 2))
			{
				if(key == 37)
				{
					tanks[turn].changeAngle(-1);
				}
				else if(key == 38)
				{
					tanks[turn].changePower(2);
				}
				else if(key == 39)
				{
					tanks[turn].changeAngle(1);
				}
				else if(key == 40)
				{
					tanks[turn].changePower(-2);
				}
				else if(key == 65)
				{
					moveTank("left");
				}
				else if(key == 68)
				{
					moveTank("right");
				}
				else if(key == 32 && bullet == null) // space
				{
					bullet = fire();
				}
//				else if(key == 82) // R
//				{
//					restartGame();
//				}
			}
			else if(gameState != 0)
			{
				restartGame();
			}
		}
		
		@Override
		public void keyReleased(KeyEvent e)
		{
		}
	}
}