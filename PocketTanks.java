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
	
	private Tank[] tanks;
	private Bullet bullet;
	private Explosion explosion;
	
	// Network
	private GameData gameData;
	private GameData enemyGameData;
	private String[] playerInfo = {"", "", ""}; // name, IP, port
	private int infoCounter;
	private Socket socket;
	private ServerSocket serverSocket;
	private ObjectInputStream ois;
	private ObjectOutputStream oos;
	private boolean joined;
	private int moveAmount;
	private boolean explosionChanged;
	
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
	private int fontWidth;
	
	private int[][] pPoints; // previous points needed for lerping
	private boolean hitText;
	private World world;
	private boolean secondPlayer;
	private boolean restartedGame;
	private boolean BvB; // Bot vs Bot
	
	public PocketTanks()
	{
		painter = new Painter();
		
		frame = new JFrame("Pocket Tanks");
		frame.add(painter);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(WIDTH, HEIGHT);
		frame.setLocationRelativeTo(null);
		frame.setResizable(false);
		frame.setVisible(true);
		
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
		drawDelta = 10; // Half of tank's size
		
		tanks = new Tank[2];
		
		numberOfPoints = 128;
		quotient = WIDTH / numberOfPoints;
		
		// Create the world and assign it to global variables
		world = new World();
		terrain = world.terrain;
		stars = world.stars;
		
		playerInputRect = new Rectangle(WIDTH / 2 - 250, 110, 500, 75);
		
		infoCounter = 0;
		joined = false;
		secondPlayer = false;
		restartedGame = false;
		moveAmount = 4;
		explosionChanged = false;
		BvB = false;
		
		gameData = new GameData();
		enemyGameData = new GameData();
		
		thread = new Thread(this);
		thread.start();
	}
	
	public static void main(String[] args)
	{
		new PocketTanks();
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
		if(menu != 3)
		{
			// Update the local bullet and explosion, tanks[] is already local, NEEDED FOR
			// RENDER
			bullet = gameData.bullet;
			explosion = gameData.explosion;
		}
		
		// Bot's turn
		if(gameStarted && gameState == 0 && menu == 1 && (turn == 1 || BvB))
		{
			int distance = tanks[turn].index - tanks[(turn + 1) % 2].index; // Distance between player and bot
			int dir = distance / Math.abs(distance); // 1 - player is to the LEFT, -1 - RIGHT
			distance = Math.abs(distance);
			int slope = terrain[tanks[turn].index + dir * 2] - terrain[tanks[turn].index]; // Slope of the terrain next
																							// to bot
			// Calculate average value of terrain ahead
			int sum = 0;
			int n = 10;
			for(int i = 1; i < n; i++)
			{
				sum += (terrain[tanks[turn].index] - terrain[tanks[turn].index - i * dir]);
			}
			double avgTerrain = sum / n;
			
			// If tank has been hit or terrain is too steep, MOVE
			if(!tanks[turn].hasMoved && (tanks[turn].wasHit || slope >= 20 || avgTerrain >= 40))
			{
				tanks[turn].moveTank(-dir * moveAmount, numberOfPoints);
				tanks[turn].setHit(false);
				tanks[turn].setAnglePower(tanks[turn].angle, 50); // reset the power to 50
			}
			
			if(gameData.bullet == null && gameData.explosion == null)
			{
				int[] shootData = calculateAnglePower(distance, dir);
				tanks[turn].setAnglePower(-shootData[0], shootData[1]); // for angle: just invert that shit lmao
				gameData.bullet = fire();
			}
		}
		
		// LAN Stuff
		if(menu == 3 && gameStarted && gameState == 0) // LAN Game
		{
			if(restartedGame) // Restart game procedure
			{
				if(turn == 1)
				{
					try
					{
						oos.writeObject(gameData);
						gameData = (GameData) ois.readObject();
					}
					catch(Exception ex)
					{
						ex.printStackTrace();
					}
				}
				else
				{
					try
					{
						enemyGameData = (GameData) ois.readObject();
						oos.writeObject(enemyGameData);
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
					tanks[1] = enemyGameData.tank;
				}
				
				restartedGame = false;
			}
			
			if(turn == 0) // First player sends the data
			{
				// Update the local variables
				tanks[0] = gameData.tank;
				bullet = gameData.bullet;
				explosion = gameData.explosion;
				
				try
				{
					oos.writeObject(gameData);
					gameData = (GameData) ois.readObject();
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
					enemyGameData = (GameData) ois.readObject();
					oos.writeObject(enemyGameData); // Has to write it back otherwise it doesn't work for SOME reason
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
				tanks[1] = enemyGameData.tank;
				bullet = enemyGameData.bullet;
				explosion = enemyGameData.explosion;
				gameData.explosion = explosion;
				turn = (enemyGameData.turn + 1) % 2;
				gameData.turn = turn;
			}
		}
		
		if(gameData.explosion != null)
		{
			int team = gameData.explosion.team;
			int index = team;
			if(turn == team)
			{
				index = (turn + 1) % 2;
			}
			boolean collision = gameData.explosion.checkCollision(tanks[index].position[0] + tanks[index].size / 2,
					tanks[index].position[1] + tanks[index].size / 2, tanks[index].size);
			
			if(menu == 1 && (turn == 1 || BvB))
			{
				tanks[turn].setExplosionDiff(gameData.explosion.position[0] - tanks[index].position[0]);
				explosionChanged = true;
			}
			
			if(collision)
			{
				hitText = true;
				if(turn == team) // Do calculations locally (Local game)
				{
					// tanks[index] -> enemy tank
					tanks[index].dealDamage(25);
					if(!tanks[index].isAlive())
					{
						gameState = turn + 1;
					}
				}
				else // Do calculations on an object (LAN)
				{
					gameData.tank.dealDamage(25);
					if(!gameData.tank.isAlive())
					{
						gameState = turn + 1;
					}
				}
			}
			
			if(!gameData.explosion.update())
			{
				gameData.explosion = null;
				hitText = false;
				if(gameState == 0)
				{
					changeTurn();
				}
			}
		}
		
		if(gameData.bullet != null)
		{
			gameData.bullet.updatePoisition();
			int collide = gameData.bullet.checkCollision(quotient, terrain);
			if(collide != -1)
			{
				gameData.explosion = new Explosion(turn, new int[] {collide * quotient - 30, terrain[collide] - 30}, 60,
						50);
				if(menu != 3)
				{
					playSound("src/sounds/explosion.wav");
				}
				gameData.bullet = null;
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
		else if(!gameStarted)
		{
			if(menu != 3) // Not LAN game
			{
				g.setColor(Color.white);
				
				g.setFont(arial);
				g.drawString("Player " + (turn + 1) + ", enter your name", WIDTH / 2 - 125, 100);
				
				g.drawRect(playerInputRect.x, playerInputRect.y, playerInputRect.width, playerInputRect.height);
				g.drawString(playerInfo[0], playerInputRect.x + 5, playerInputRect.y + playerInputRect.height / 2 + 11);
				
				g.drawRect(WIDTH / 2 - 50, 500, 100, 75);
				g.drawString("OK", WIDTH / 2 - 20, 545);
			}
			else // LAN Game input
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
				g.drawString(playerInfo[0], playerInputRect.x + 5, playerInputRect.y + playerInputRect.height / 2 + 10);
				
				// IP input
				g.drawString("Enter IP:", WIDTH / 2 - 40, 220);
				if(infoCounter == 1)
				{
					g.setColor(Color.green);
				}
				g.drawRect(playerInputRect.x, playerInputRect.y + 125, playerInputRect.width, playerInputRect.height);
				g.setColor(Color.white);
				g.drawString(playerInfo[1], playerInputRect.x + 5,
						playerInputRect.y + playerInputRect.height / 2 + 130);
				
				// Port input
				g.drawString("Enter port:", WIDTH / 2 - 50, 345);
				if(infoCounter == 2)
				{
					g.setColor(Color.green);
				}
				g.drawRect(playerInputRect.x, playerInputRect.y + 250, playerInputRect.width, playerInputRect.height);
				g.setColor(Color.white);
				g.drawString(playerInfo[2], playerInputRect.x + 5,
						playerInputRect.y + playerInputRect.height / 2 + 255);
				
				g.drawRect(WIDTH / 2 - 50, 500, 100, 75);
				g.drawString("OK", WIDTH / 2 - 20, 545);
			}
		}
		else if(gameStarted && gameState == 0) // Game in progress
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
			
			// Draw tanks
			for(int i = 0; i < tanks.length; i++)
			{
				g.setColor(Color.green.darker());
				if(i == 1)
				{
					g.setColor(Color.red.darker());
				}
				
				int index = tanks[i].index;
				int[] tankPosition = {lerp(pPoints[i][0], index * quotient, 0.4) - drawDelta,
						lerp(pPoints[i][1], terrain[index], 0.4) - drawDelta};
				tanks[i].setPosition(tankPosition);
				// Draw current tank
				g.fillOval(tankPosition[0], tankPosition[1], tanks[i].size, tanks[i].size);
				
				// Draw canon
				Graphics2D g2d = (Graphics2D) g.create(); // with g.create() method we have 2 different object,
															// which are SEPERATE!!!
				Rectangle rect = new Rectangle(tankPosition[0] + drawDelta / 2, tankPosition[1] + drawDelta / 2, 30,
						10);
				g2d.rotate(tanks[i].angle * Math.PI / 180, rect.x + 5, rect.y + 5);
				g2d.fill(rect);
				
				tankPosition[0] += drawDelta;
				tankPosition[1] += drawDelta;
				// Display player name (above the tank)
				fontWidth = g.getFontMetrics(arial).stringWidth(tanks[i].name);
				g.drawString(tanks[i].name, tankPosition[0] - fontWidth / 2,
						tankPosition[1] - g.getFontMetrics(arial).getHeight() - drawDelta);
				pPoints[i] = tankPosition;
				
				int fontWidthHealth = g.getFontMetrics(arial).stringWidth("Health: " + tanks[i].health);
				fontWidth = Math.max(fontWidth, fontWidthHealth);
				if(i == 0 && !secondPlayer || i == 1 && secondPlayer)
				{
					g.drawString(tanks[i].name + ":", 30, 40);
					g.drawString("Health: " + tanks[i].health, 30, 65);
				}
				else
				{
					g.drawString(tanks[i].name + ":", WIDTH - fontWidth - 30, 40);
					g.drawString("Health: " + tanks[i].health, WIDTH - fontWidth - 30, 65);
				}
			}
			
			// Draw bullet
			if(bullet != null)
			{
				if(menu != 3)
				{
					if(bullet.getTeam() == 0)
					{
						g.setColor(Color.green.darker());
					}
					else
					{
						g.setColor(Color.red.darker());
					}
				}
				else // LAN Game
				{
					if(bullet.getTeam() == turn)
					{
						g.setColor(Color.green.darker());
					}
					else
					{
						g.setColor(Color.red.darker());
					}
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
			currentPlayer = tanks[turn].name + "'s";
			if(menu == 3 && turn == 0)
			{
				currentPlayer = "YOUR";
			}
			angle = Math.abs(tanks[turn].angle);
			power = tanks[turn].power;
			
			fontWidth = g.getFontMetrics(arial).stringWidth(currentPlayer);
			g.drawString(currentPlayer + " TURN!", WIDTH / 2 - fontWidth, 100);
			
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
		else if(gameStarted) // Someone won
		{
			// Draw stars
			g.setColor(Color.white);
			for(int i = 0; i < stars.length; i++)
			{
				g.fillOval(stars[i][0], stars[i][1], 5, 5);
			}
			
			g.setFont(arialBig);
			String winString = tanks[gameState - 1].name + " WON";
			if(gameState == 1)
			{
				g.setColor(Color.green);
				winString = "YOU WON!";
			}
			else
			{
				g.setColor(Color.red);
			}
			
			fontWidth = g.getFontMetrics(arialBig).stringWidth(winString);
			g.drawString(winString, WIDTH / 2 - fontWidth / 2, 50);
			
			fontWidth = g.getFontMetrics(arialBig).stringWidth("Click any key to restart");
			g.drawString("Click any key to restart", WIDTH / 2 - fontWidth / 2, 90);
		}
	}
	
	public void buttonOK()
	{
		if(menu == 1)
		{
			if(playerInfo[0].equals(""))
			{
				playerInfo[0] = "Player " + (turn + 1);
			}
			// Player's tank
			Tank tank = new Tank(playerInfo[0], (int) (Math.random() * numberOfPoints / 4) + 3, 20);
			tanks[0] = tank;
			
			// Bot's tank
			tank = new Tank("Bot", (int) (Math.random() * numberOfPoints / 4 + (3 * numberOfPoints / 4) - 3), 20);
			tanks[1] = tank;
			
			gameStarted = true;
		}
		else if(menu == 2) // PvP game
		{
			if(playerInfo[0].equals(""))
			{
				playerInfo[0] = "Player " + (turn + 1);
			}
			
			if(turn == 0)
			{
				Tank tank = new Tank(playerInfo[0], (int) (Math.random() * numberOfPoints / 4) + 3, 20);
				tanks[0] = tank;
			}
			else
			{
				Tank tank = new Tank(playerInfo[0],
						(int) (Math.random() * numberOfPoints / 4 + (3 * numberOfPoints / 4) - 3), 20);
				tanks[1] = tank;
				
				gameStarted = true;
			}
			
			turn = (turn + 1) % 2;
			playerInfo[0] = "";
		}
		else if(menu == 3) // LAN Game
		{
			if(infoCounter == 0) // player name
			{
				if(playerInfo[0].equals(""))
				{
					playerInfo[0] = "Player " + (turn + 1);
				}
				infoCounter++;
			}
			else if(infoCounter == 1) // IP
			{
				boolean inputError = false;
				String ip1 = playerInfo[infoCounter];
				
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
					playerInfo[infoCounter] = "";
					JOptionPane.showMessageDialog(null, "Wrong IP, try again");
				}
			}
			else if(infoCounter == 2) // port
			{
				boolean inputError = false;
				String portString = playerInfo[infoCounter];
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
					playerInfo[infoCounter] = "";
					JOptionPane.showMessageDialog(null, "Wrong port, try again");
				}
			}
			
			// Initialize the LAN game
			if(infoCounter == 3 && !gameStarted)
			{
				String ip = playerInfo[1];
				int port = Integer.parseInt(playerInfo[2]);
				
				if(!connect(ip, port))
				{
					initializeServer(ip, port);
					System.out.println("Waiting for client...");
				}
				if(!joined)
				{
					listenForServerRequest();
				}
				
				createTank();
				
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
				
				exchangeMapInfo();
				gameStarted = true;
			}
		}
	}
	
	public int[] calculateAnglePower(int distance, int dir) // TODO There is room for improvement
	{
		// Angle is 90 +- 30 based on where the player is, min = 60, max = 120
		int delta = 30 * distance / numberOfPoints;
		int angle = 90 + delta * dir;
		int angleDifference = Math.abs(90 - angle);
		int power = tanks[turn].power;
		if(power == 50)
		{
			power = (100 + angleDifference) * distance / numberOfPoints;
		}
		else if(explosionChanged)
		{
			if(tanks[turn].explosionDiff >= 800)
			{
				tanks[turn].setExplosionDiff(tanks[turn].explosionDiff * -1);
			}
			if(Math.abs(tanks[turn].explosionDiff) < 10)
			{
				power = tanks[turn].power + tanks[turn].explosionDiff / 2;
			}
			else if(Math.abs(tanks[turn].explosionDiff) < 100)
			{
				power = tanks[turn].power + tanks[turn].explosionDiff / 12;
			}
			else
			{
				power = tanks[turn].power + tanks[turn].explosionDiff / 100;
			}
			explosionChanged = false;
		}
		return new int[] {angle, power};
	}
	
	public Bullet fire()
	{
		if(explosion == null)
		{
			if(menu != 3)
			{
				playSound("src/sounds/shoot.wav");
			}
			Tank currentTank = tanks[turn];
			return new Bullet(turn, new int[] {currentTank.index * quotient, terrain[currentTank.index] - 10},
					currentTank.angle, currentTank.power);
		}
		return null;
	}
	
	public void changeTurn()
	{
		if(menu != 3)
		{
			tanks[turn].setMoved(false);
		}
		else
		{
			gameData.tank.setMoved(false);
		}
		turn = (turn + 1) % 2;
		gameData.turn = turn;
	}
	
	public void createTank()
	{
		int spawn = 0;
		if(turn == 0) // Player 1
		{
			spawn = (int) (Math.random() * numberOfPoints / 4) + 3;
		}
		else // Player 2
		{
			spawn = (int) (Math.random() * numberOfPoints / 4 + (3 * numberOfPoints / 4) - 3);
		}
		tanks[0] = new Tank(playerInfo[0], spawn, 20);
		gameData = new GameData(tanks[0], turn);
	}
	
	public void exchangeMapInfo() // Send over the "world" so its the same on both clients
	{
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
	}
	
	public void botVsBot()
	{
		world = new World();
		terrain = world.terrain;
		stars = world.stars;
		
		for(int i = 0; i < tanks.length; i++)
		{
			String tankName = "Bot " + (i + 1);
			int spawn = (int) (Math.random() * numberOfPoints / 4 + (i * 3 * numberOfPoints / 4));
			tanks[i] = new Tank(tankName, spawn, 20);
		}
		
		BvB = true;
	}
	
	public void restartGame()
	{
		if(menu != 3)
		{
			world = new World();
			terrain = world.terrain;
			stars = world.stars;
			
			for(int i = 0; i < tanks.length; i++)
			{
				String tankName = tanks[i].name;
				int spawn = (int) (Math.random() * numberOfPoints / 4 + (i * 3 * numberOfPoints / 4));
				if(i != 0 && menu == 1)
				{
					tankName = "Bot";
				}
				tanks[i] = new Tank(tankName, spawn, 20);
			}
		}
		else
		{
			if(!secondPlayer)
			{
				turn = 0;
				gameData.turn = turn;
				world = new World();
				terrain = world.terrain;
				stars = world.stars;
			}
			else
			{
				turn = 1;
				gameData.turn = turn;
			}
			
			int spawn = 0;
			if(turn == 0) // Player 1
			{
				spawn = (int) (Math.random() * numberOfPoints / 4) + 3;
			}
			else // Player 2
			{
				spawn = (int) (Math.random() * numberOfPoints / 4 + (3 * numberOfPoints / 4) - 3);
			}
			gameData.tank.setNewIndex(spawn);
			tanks[0] = gameData.tank;
			
			exchangeMapInfo();
			restartedGame = true;
		}
		gameState = 0;
	}
	
	public void getKeyInput(int key)
	{
		if(menu != 3)
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
				tanks[turn].moveTank(-moveAmount, numberOfPoints);
			}
			else if(key == 68)
			{
				tanks[turn].moveTank(moveAmount, numberOfPoints);
			}
		}
		else if(menu == 3)
		{
			if(key == 37)
			{
				// You have to change the tank object inside the object you are sending over the
				// network for SOME reason
				gameData.tank.changeAngle(-1);
			}
			else if(key == 38)
			{
				gameData.tank.changePower(2);
			}
			else if(key == 39)
			{
				gameData.tank.changeAngle(1);
			}
			else if(key == 40)
			{
				gameData.tank.changePower(-2);
			}
			else if(key == 65)
			{
				gameData.tank.moveTank(-moveAmount, numberOfPoints);
			}
			else if(key == 68)
			{
				gameData.tank.moveTank(moveAmount, numberOfPoints);
			}
		}
		
		if(key == 32 && bullet == null) // space
		{
			gameData.bullet = fire();
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
			joined = true;
		}
		catch(IOException e)
		{
			System.out.println("Can't connect to: " + ip + "; starting server...");
			return false;
		}
		turn = 1;
		secondPlayer = true;
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
			joined = true;
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
				if(x >= WIDTH / 2 - rectWidth / 2 && x <= WIDTH / 2 - rectWidth / 2 + rectWidth) // Check x axis
				{
					for(int i = 0; i < 3; i++) // Determine which button user clicked
					{
						if(y >= rectStartY + rectDeltaY * i && y <= rectStartY + rectDeltaY * i + rectHeight)
						{
							menu = i + 1; // Set state of the menu to corresponding button
							break;
						}
					}
				}
			}
			else if(!gameStarted)
			{
				if(x >= WIDTH / 2 - 50 && x <= WIDTH / 2 + 50 && y >= 500 && y <= 575)
				{
					buttonOK();
				}
			}
			else if(gameState == 0)
			{
				if(menu == 2)
				{
					if(y >= HEIGHT - 100 && y <= HEIGHT - 50)
					{
						if(x >= WIDTH / 4 - 115 && x <= WIDTH / 4 - 15)
						{
							tanks[turn].moveTank(-moveAmount, numberOfPoints);
						}
						else if(x >= 3 * WIDTH / 4 + 15 && x <= 3 * WIDTH / 4 + 115)
						{
							tanks[turn].moveTank(moveAmount, numberOfPoints);
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
							tanks[turn].changePower(-2);
						}
						else if(x >= WIDTH - WIDTH / 4 - 80 && x <= WIDTH - WIDTH / 4 - 25)
						{
							tanks[turn].changePower(2);
						}
					}
				}
				else if(menu == 3 && turn == 0)
				{
					if(y >= HEIGHT - 100 && y <= HEIGHT - 50)
					{
						if(x >= WIDTH / 4 - 115 && x <= WIDTH / 4 - 15)
						{
							gameData.tank.moveTank(-moveAmount, numberOfPoints);
						}
						else if(x >= 3 * WIDTH / 4 + 15 && x <= 3 * WIDTH / 4 + 115)
						{
							gameData.tank.moveTank(moveAmount, numberOfPoints);
						}
					}
					if(y >= HEIGHT - 95 && y <= HEIGHT - 40)
					{
						if(x >= WIDTH / 4 + 10 && x <= WIDTH / 4 + 85)
						{
							gameData.tank.changeAngle(-1);
						}
						else if(x >= WIDTH / 4 + 85 && x <= WIDTH / 4 + 140)
						{
							gameData.tank.changeAngle(1);
						}
						else if(x >= WIDTH - WIDTH / 4 - 155 && x <= WIDTH - WIDTH / 4 - 100)
						{
							gameData.tank.changePower(-2);
						}
						else if(x >= WIDTH - WIDTH / 4 - 80 && x <= WIDTH - WIDTH / 4 - 25)
						{
							gameData.tank.changePower(2);
						}
					}
				}
				
				if(y >= HEIGHT - 120 && y <= HEIGHT - 40)
				{
					if(x >= WIDTH / 2 - 60 && x <= WIDTH / 2 + 65 && bullet == null)
					{
						gameData.bullet = fire();
					}
				}
			}
			else if(gameState != 0)
			{
				restartGame();
			}
		}
		
		@Override
		public void keyPressed(KeyEvent e)
		{
			int key = e.getKeyCode();
			if(!gameStarted)
			{
				if(key == 10) // enter
				{
					buttonOK();
				}
				else if(key == 8 && playerInfo[infoCounter].length() > 0) // backspace
				{
					playerInfo[infoCounter] = playerInfo[infoCounter].substring(0,
							playerInfo[infoCounter].length() - 1);
				}
				else
				{
					playerInfo[infoCounter] += e.getKeyChar();
				}
			}
			else if(gameState == 0) // Game in progress
			{
				getKeyInput(key);
				
				if(menu == 1) // TODO Delete?
				{
					if(key == 82)
					{
						restartGame();
					}
					else if(key == 66)
					{
						botVsBot();
					}
				}
			}
			else if(gameState != 0)
			{
				restartGame();
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
		public void keyReleased(KeyEvent e)
		{
		}
	}
}