import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Random;
import java.util.Vector;

public class AutoFighter
{
	private String bot_type;
	private int width = 500;
	private int height = 320;
	private int x_center = 0;
	private int y_center = 0;
	private int x_corner = 0;
	private int y_corner = 0;
	public int x_precision = 20;
	public int y_precision = 20;
	public int grid_width = (int) (width / x_precision);
	public int grid_height = (int) (height / y_precision);
	public int grid_obstacle[][] = new int[grid_width][grid_height];
	public int grid_NPC[][] = new int[grid_width][grid_height];
	public int minimap[][] = new int[148][150];
	public int x_grid_center = 0;
	public int y_grid_center = 0;
	public int[] NPC_x_locs = new int[50];
	public int[] NPC_y_locs = new int[50];
	public int rock_count = 0;
	public int miss_count = 0;
	public int run_count = 0;
	public String inventory[][] = new String[4][7];
	
	
	
	private long wait_end;
	private long last_time_active;
	private long last_time_alligned;
	
	public int number_of_NPCs = 4;
	public boolean[] NPC_used = new boolean[number_of_NPCs];
	public int[] NPC_red = new int[number_of_NPCs];
	public int[] NPC_green = new int[number_of_NPCs];
	public int[] NPC_blue = new int[number_of_NPCs];
	public int[] NPC_tolerance = new int[number_of_NPCs];
	public int number_of_obstacles = 4;
	public boolean[] obstacle_used = new boolean[number_of_obstacles];
	public int[] obstacle_red = new int[number_of_obstacles];
	public int[] obstacle_green = new int[number_of_obstacles];
	public int[] obstacle_blue = new int[number_of_obstacles];
	public int[] obstacle_tolerance = new int[number_of_obstacles];
	private int number_of_items = 2;
	private boolean[] item_used = new boolean[number_of_items];
	private int[] item_red = new int[number_of_items];
	private int[] item_green = new int[number_of_items];
	private int[] item_blue = new int[number_of_items];
	private int[] item_tolerance = new int[number_of_items];
	private String[] item_category = new String[number_of_items];
	private boolean multi_combat_area = false;
	private boolean minimap_used = false;
	private boolean run_on = false;
	
	private Vector<String> activity_queue = new Vector<String>(0);
	private ArrayList<String> item_names = new ArrayList<String>();
	private ArrayList<Integer> items_red = new ArrayList<Integer>();
	private ArrayList<Integer> items_green = new ArrayList<Integer>();
	private ArrayList<Integer> items_blue = new ArrayList<Integer>();

	private Rectangle screen_rectangle;
	private BufferedImage screen_capture;
	private Rectangle minimap_rectangle;
	private BufferedImage minimap_capture;


	public AutoFighter(Robot robot, String type) throws InterruptedException
	{
		initializeLocation(robot);
		this.width = 500;
		this.height = 320;
		this.screen_rectangle = new Rectangle(x_corner, y_corner, width, height);
		this.screen_capture = robot.createScreenCapture(screen_rectangle);
		this.minimap_rectangle = new Rectangle(x_corner + 563, y_corner + 4, 148, 150);
		this.minimap_capture = robot.createScreenCapture(minimap_rectangle);
		this.wait_end = System.currentTimeMillis();
		this.last_time_active = System.currentTimeMillis();
		this.last_time_alligned = System.currentTimeMillis();
		this.bot_type = type;
		this.x_precision = 20;
		this.y_precision = 20;
		this.grid_width = (int) width / x_precision; // 25
		this.grid_height = (int) height / y_precision; // 16
		this.multi_combat_area = false;
		this.miss_count = 0;
		this.rock_count = 0;
		this.addItems();
		this.addObstacles();
		this.addNPCs();
		if (type == "Tan")
		{
			this.tan(robot);
		}
		else if (type == "Mine")
		{
			this.mine();
		}
		for (int x = 0; x < 4; x++)
		{
			for (int y = 0; y < 7; y++)
			{
				inventory[x][y] = "Empty";
			}
		}
	}

	public void initializeLocation(Robot robot) throws InterruptedException
	{
		System.out.println("Hover over the corner... Waiting 5 seconds...");
		Thread.sleep(5000);
		PointerInfo a = MouseInfo.getPointerInfo();
		Point b = a.getLocation();
		this.x_corner = (int) b.getX();
		this.y_corner = (int) b.getY();
		System.out.println("Corner at (" + x_corner + ", " + y_corner + ")");
		System.out.println("Hover over the center... Waiting 5 seconds...");
		Thread.sleep(5000);
		a = MouseInfo.getPointerInfo();
		Point c = a.getLocation();
		this.x_center = (int) c.getX();
		this.y_center = (int) c.getY();
		System.out.println("Corner at (" + x_center + ", " + y_center + ")");
		this.x_grid_center = (int) ((x_center - x_corner) / x_precision);
		this.y_grid_center = (int) ((y_center - y_corner) / y_precision);
	}

	public void updateScreen(Robot robot)
	{
		screen_capture = robot.createScreenCapture(screen_rectangle);
		if (minimap_used)
		{
			minimap_capture = robot.createScreenCapture(minimap_rectangle);
		}
	}
	
	public void scanGrid()
	{
		for (int x = 0; x < grid_width; x++)
		{
			for (int y = 0; y < grid_height; y++)
			{
				grid_obstacle[x][y] = 0;
				grid_NPC[x][y] = 0;
			}
		}
		for (int i = 0; i < 20; i++)
		{
			NPC_x_locs[i] = 0;
			NPC_y_locs[i] = 0;
		}
		
		int NPC_count = 0;	
		for (int x = 0; x < width; x++)
		{
			for (int y = 0; y < height; y++)
			{
				int color = screen_capture.getRGB(x, y);
				if (scanForObstacles(color))
				{
					int grid_x = (int) (x / x_precision);
					int grid_y = (int) (y / y_precision);
					grid_obstacle[grid_x][grid_y] = 1;
				}
				if (scanForNPCs(color))
				{
					int grid_x = (int) (x / x_precision);
					int grid_y = (int) (y / y_precision);
					if (grid_NPC[grid_x][grid_y] == 0)
					{
						grid_NPC[grid_x][grid_y] = NPC_count + 1;
						NPC_x_locs[NPC_count] = x;
						NPC_y_locs[NPC_count] = y;
						NPC_count++;
					}
				}
			}
		}
		for (int x = 0; x < grid_width; x++)
		{
			for (int y = 0; y < grid_height; y++)
			{
				if (grid_obstacle[x][y] == 1)
				{
					grid_NPC[x][y] = 0;
				}
			}
		}
		grid_obstacle[x_grid_center][y_grid_center] = 0;
	}
	
	public boolean scanForObstacles(int color)
	{
		for (int i = 0; i < number_of_obstacles; i++)
		{
			if (obstacle_used[i] && colorMatch(color, obstacle_red[i], obstacle_green[i], obstacle_blue[i], obstacle_tolerance[i]))
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean scanForNPCs(int color)
	{
		for (int i = 0; i < number_of_NPCs; i++)
		{
			if (NPC_used[i] && colorMatch(color, NPC_red[i], NPC_green[i], NPC_blue[i], NPC_tolerance[i]))
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean scanForDrops(int color)
	{
		for (int i = 0; i < number_of_items; i++)
		{
			if (item_used[i] && item_category[i] == "Drop" && colorMatch(color, item_red[i], item_green[i], item_blue[i], item_tolerance[i]))
			{
				return true;
			}
		}
		return false;
	}
	
	public void displayBothGrids()
	{
		for (int y = 0; y < grid_height; y++)
		{
			for (int x = 0; x < grid_width; x++)
			{
				if (x == x_grid_center && y == y_grid_center)
				{
					System.out.print("*");
				}
				if (grid_obstacle[x][y] == 0 && grid_NPC[x][y] == 0)
				{
					System.out.print(".");
				}
				else if (grid_NPC[x][y] > 0)
				{
					System.out.print(grid_NPC[x][y]);
				}
				else
				{
					System.out.print("x");
				}
			}
			System.out.println();
		}
	}
	
	public boolean colorMatch(int input_color, int red_match, int green_match, int blue_match, int tolerance)
	{
		int color = input_color;
		int blue = color & 0xff;
		int green = (color & 0xff00) >> 8;
		int red = (color & 0xff0000) >> 16;
		int red_diff = Math.abs(red - red_match);
		int green_diff = Math.abs(green - green_match);
		int blue_diff = Math.abs(blue - blue_match);
		if (red_diff < tolerance && green_diff < tolerance && blue_diff < tolerance)
		{
			return true;
		}
		return false;
	}
	
	public boolean isEngaged()
	{
		for (int x = x_center - x_corner - 30; x < x_center - x_corner + 30; x++)
		{
			for (int y = y_center - y_corner - 30; y < y_center - y_corner; y++)
			{
				int color = screen_capture.getRGB(x, y);
				if (colorMatch(color, 0, 255, 0, 5))
				{
					if (System.currentTimeMillis() - last_time_active > 0)
					{
						this.last_time_active = System.currentTimeMillis();
					}
					return true;
				}
				else if (colorMatch(color, 255, 0, 0, 5))
				{
					if (System.currentTimeMillis() - last_time_active > 0)
					{
						this.last_time_active = System.currentTimeMillis();
					}
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean isLowHealth(Robot robot)
	{
		Rectangle healthRect = new Rectangle(x_corner + 518, y_corner + 51, 15, 15);
		BufferedImage healthCapture = robot.createScreenCapture(healthRect);
		
		for (int x = 0; x < 15; x++)
		{
			for (int y = 0; y < 10; y++)
			{
				int color = healthCapture.getRGB(x, y);
				int green = (color & 0xff00) >> 8;
				if (green > 250)
				{
					return false;
				}
			}
		}
		return true;
	}
	
	
	
	public void eatFood(Robot robot) throws AWTException, InterruptedException
	{
		Random random = new Random();
		readInventory(robot);
		boolean found_food = false;
		for (int x = 0; x < 4; x++)
		{
			for (int y = 0; y < 7; y++)
			{
				if (inventory[x][y].equals("Trout") || inventory[x][y].equals("Salmon"))
				{
					found_food = true;
					System.out.println("Found food");
					int x_click = 571 + 42 * x + x_corner;
					int y_click = 220 + 37 * y + y_corner;
					leftClickRandom(robot, x_click, y_click, 5);
					setWait(1000 + random.nextInt(500));
					x = 4;
					y = 7;
					break;
				}
			}
		}
		/*if (!found_food)
		{
			for (int i = 0; i < 5; i++)
			{
				System.out.println("Out of food");
				activity_queue.add("Run North");
			}
    	}*/
		/*
		Rectangle inventory_rect = new Rectangle(x_corner + 555, y_corner + 210, 160, 240);
		BufferedImage inventory_capture = robot.createScreenCapture(inventory_rect);
		boolean found_food = false;
		for (int x = 0; x < 160; x++)
		{
			for (int y = 0; y < 240; y++)
			{
				int color = inventory_capture.getRGB(x, y);
				for (int i = 0; i < number_of_items; i++)
				{
					if (item_used[i] && item_category[i] == "Food" && colorMatch(color, item_red[i], item_green[i], item_blue[i], item_tolerance[i]))
					{
						found_food = true;
						leftClickRandom(robot, x_corner + 555 + x, y_corner + 210 + y, 10);
						Random random = new Random();
						setWait(300 + random.nextInt(300));
						x = 160;
						y = 240;
						break;
					}
				}
			}
		}
		if (!found_food)
		{
			for (int i = 0; i < 5; i++)
			{
				System.out.println("Out of food");
				activity_queue.add("Run North");
			}
    	}*/
	}
	
	public void renewAggressiveness(Robot robot) throws AWTException, InterruptedException
	{
		this.last_time_active = System.currentTimeMillis() + 120 * 1000;
		this.activity_queue.addElement("Center View");
		for (int i = 0; i < 15; i++)
		{
			this.activity_queue.addElement("Run North");
		}
		for (int i = 0; i < 15; i++)
		{
			this.activity_queue.addElement("Run South");
		}
		this.activity_queue.addElement("Run Center");
	}
	
	public void tan(Robot robot)
	{
		for (int i = 0; i < 500; i++)
		{
			this.activity_queue.addElement("Run to Tanner 1");
			this.activity_queue.addElement("Run to Tanner 2");
			this.activity_queue.addElement("Find Tanner");
			this.activity_queue.addElement("Tan Hides");
			this.activity_queue.addElement("Run to Bank 1");
			this.activity_queue.addElement("Find Banker");
			this.activity_queue.addElement("Click Banker");
			this.activity_queue.addElement("Bank Leather");
			this.activity_queue.addElement("Withdraw Cowhide");
		}
	}
	
	/*public boolean findTanner(Robot robot) throws AWTException, InterruptedException
	{
		Random random = new Random();
		for (int x = 5; x < width - 5; x++)
		{
			for(int y = 5; y < height - 5; y++)
			{
				int color = screen_capture.getRGB(x, y);
				if (colorMatch(color, 184, 102, 66, 3))
				{
					/*int top_left = 0;
					int top_right = 0;
					int bottom_left = 0;
					int bottom_right = 0;
					for (int m = -5; m < 5; m++)
					{
						for (int n = -5; n < 5; n++)
						{
							int color2 = screen_capture.getRGB(x + m, y + n);
							if (colorMatch(color2, 184, 102, 66, 30))
							{
								if (m < 0 && n < 0)
								{
									top_left++;
								}
								if (m < 0 && n > 0)
								{
									bottom_left++;
								}
								if (m > 0 && n < 0)
								{
									top_right++;
								}
								if (m > 0 && n > 0)
								{
									bottom_right++;
								}
							}
						}
					}
					int x_offset = 0;
					int y_offset = 0;
					if (top_left >= top_right && top_left >= bottom_left && top_left >= bottom_right)
					{
						x_offset = -4;
						y_offset = -3;
					}
					else if (top_right >= bottom_left && top_right >= bottom_right)
					{
						x_offset = 4;
						y_offset = -3;
					}
					else if (bottom_left >= bottom_right)
					{
						x_offset = -4;
						y_offset = 3;
					}
					else
					{
						x_offset = 4;
						y_offset = 3;
					}
					
					int x_offset = 5;
					int y_offset = 4;
					Rectangle right_click_rectangle = new Rectangle(x_corner + x_offset + x - 50, y_corner + y_offset + y - 5, 100, 60);
					BufferedImage right_click_capture = returnClickWindow(robot, x_corner + x + x_offset, y_corner + y + y_offset, right_click_rectangle);
					Thread.sleep(200 + random.nextInt(150));
					for (int m = 0; m < 100; m++)
					{
						for (int n = 0; n < 60; n++)
						{
							int color2 = right_click_capture.getRGB(m, n);
							if (colorMatch(color2, 255, 255, 0, 3))
							{
								leftClickPrecise(robot, x_corner + x + x_offset, y_corner + y + y_offset + 38);
								setWait(1500 + random.nextInt(1000));
								return true;
							}
						}
					}
					robot.mouseMove(0, 0);
					setWait(500 + random.nextInt(300));
				}
			}
		}
		return false;
	}*/
	
	public boolean findTannerPro(Robot robot) throws AWTException, InterruptedException
	{
		Random random = new Random();
		for (int x = 0; x < width; x++)
		{
			for (int y = 0; y < height; y++)
			{
				int color = screen_capture.getRGB(x, y);
				if (colorMatch(color, 173, 94, 62, 5))///*colorMatch(color, 217, 119, 78, 5) || colorMatch(color, 210, 115, 75, 5)) || */colorMatch(color, 181, 106, 69, 5))
				{
					int x_offset = 0;
					int y_offset = 3;
					if (x < x_center - x_corner - 100)
					{
						x_offset = 10;
						System.out.println("positive offset");
					}
					else if (x < x_center - x_corner - 50)
					{
						x_offset = 5;
					}
					
					else if (x > x_center - x_corner + 100)
					{
						x_offset = -7;
						System.out.println("negative offset");
					}
					else if (x > x_center - x_corner + 50)
					{
						
					}
					if (y < y_center - y_corner - 50)
					{
						y_offset = 4;
					}
					else if (y > y_center - y_corner + 50)
					{
						y_offset = -3;
					}
					int x_click = x + x_corner + x_offset;
					int y_click = y + y_corner + y_offset;
					rightClick(robot, x_click, y_click);
					Thread.sleep(300);// + random.nextInt(100));
					Rectangle options_rectangle = new Rectangle(x_click - 50, y_click, 100, 50);
					BufferedImage options_screen = robot.createScreenCapture(options_rectangle);
					boolean found_tanner = false;
					for (int m = 0; m < 100; m++)
					{
						for (int n = 0; n < 50; n++)
						{
							int color2 = options_screen.getRGB(m, n);
							if (colorMatch(color2, 255, 255, 0, 3))
							{
								found_tanner = true;
								m = 100;
								n = 50;
								break;
							}
						}
					}
					if (found_tanner)
					{
						leftClickPrecise(robot, x + x_corner + x_offset, y + y_corner + y_offset + 38);
						System.out.println("Found tanner");
						x = width;
						y = height;
						return true;
					}
					else
					{
						robot.mouseMove(0, 0);
					}
				}
			}
		}
		
		return false;
	}
	
	public boolean findBanker(Robot robot) throws AWTException, InterruptedException
	{
		Random random = new Random();
		int check_limit = 148;
		if (bot_type == "Tan" && miss_count < 40)
		{
			check_limit = 75;
		}
		for (int x = 0; x < check_limit; x++)
		{
			for (int y = 0; y < 150; y++)
			{
				int count = 0;
				int color = minimap_capture.getRGB(x, y);
				if (colorMatch(color, 252, 252, 10, 10))
				{
					for (int m = -3; m < 3; m++)
					{
						for (int n = 0; n < 30; n++)
						{
							if (x + m >= 0 && x + m < 145 && y + n >= 0 && y + n < 150)
							{
								int color2 = minimap_capture.getRGB(x + m, y + n);
								if (colorMatch(color2, 252, 252, 10, 10))
								{
									count++;
								}
							}
						}
					}
				}
				int threshold = 22;
				if (bot_type == "Mine")
				{
					threshold = 25;
				}
				if (count > threshold)
				{
					leftClickPrecise(robot, x_corner + 563 + x + 6, y_corner + 4 + y + 9);
					if (!run_on)
					{
						setWait(5000 + random.nextInt(1000));
					}
					else
					{
						setWait(3000 + random.nextInt(500));
					}
					return true;
				}
			}
		}
		return false;
	}
	
	public void allignScreen(Robot robot) throws InterruptedException
	{
		boolean alligned = false;
		Random random = new Random();
		boolean check_left = false;
		int right_count = 0;
		while (!alligned)
		{
			minimap_capture = robot.createScreenCapture(minimap_rectangle);
			
			for (int y = 0; y < 150; y++)
			{
				int column_count = 0;
				for (int x = 0; x < 148; x++)
				{
					int color = minimap_capture.getRGB(x, y);
					if (colorMatch(color, 231, 239, 229, 20))
					{
						column_count++;
					}
				}
				System.out.println(column_count);
				if (column_count > 18)
				{
					alligned = true;
					System.out.println("found allignment");
				}
			}
			
			if (!check_left)
			{
				robot.keyPress(KeyEvent.VK_RIGHT);
				Thread.sleep(10);
				robot.keyRelease(KeyEvent.VK_RIGHT);
				Thread.sleep(40 + random.nextInt(20));
				right_count++;
				if (right_count > 50)
				{
					check_left = true;
				}
			}
			else
			{
				right_count++;
				if (right_count > 150)
				{
					right_count = 0;
					check_left = false;
				}
				robot.keyPress(KeyEvent.VK_LEFT);
				Thread.sleep(10);
				robot.keyRelease(KeyEvent.VK_LEFT);
				Thread.sleep(50 + random.nextInt(20));
			}
		}
	}
	
	public void turnRunOn(Robot robot) throws AWTException, InterruptedException
	{
		Rectangle run_rectangle = new Rectangle(x_corner + 540, y_corner + 131, 50, 15);
		BufferedImage run_capture = robot.createScreenCapture(run_rectangle);
		
		// Checks if run is currently off
		boolean run_off = true;
		for (int x = 25; x < 50; x++)
		{
			for (int y = 0; y < 15; y++)
			{
				int color = run_capture.getRGB(x, y);
				if (colorMatch(color, 236, 218, 103, 20))
				{
					run_off = false;
					x = 50;
					y = 15;
					break;
				}
			}
		}
		
		if (run_off)
		{
			boolean run_above_50 = false;
			for (int x = 0; x < 15; x++)
			{
				for (int y = 0; y < 15; y++)
				{
					int color = run_capture.getRGB(x, y);
					int green = (color & 0xff00) >> 8;
					if (green > 200)
					{
						run_above_50 = true;
						x = 15;
						y = 15;
						break;
					}
				}
			}
			if (run_above_50)
			{
				run_on = true;
				leftClickRandom(robot, x_corner + 576, y_corner + 133, 8);
				setWait(200);
			}
			else
			{
				run_on = false;
			}
		}
	}
	
	public boolean clickBanker(Robot robot) throws AWTException, InterruptedException
	{
		Random random = new Random();
		for (int x = 15; x < 50; x++)
		{
			for (int y = -20; y < 30; y++)
			{
				int color = screen_capture.getRGB(x_center - x_corner - x, y_center - y_corner + y);
				if (colorMatch(color, 89, 86, 83, 1))
				{
					leftClickPrecise(robot, x_center - x, y_center + y);
					if (!run_on)
					{
						setWait(800 + random.nextInt(500));
					}
					else
					{
						setWait(700 + random.nextInt(500));
					}
					return true;
				}
			}
		}
		return false;
	}

	public boolean tanHides(Robot robot) throws AWTException, InterruptedException
	{

		for (int i = 0; i < 20; i++)
		{
			for (int j = 0; j < 30; j++)
			{
				int color = screen_capture.getRGB(471 + i, 8 + j);
				if (colorMatch(color, 0, 0, 1, 2))
				{
					Random random = new Random();
					int x = x_corner + 90 + random.nextInt(40) - 20;
					int y = y_corner + 102 + random.nextInt(30) - 15;
					rightClick(robot, x, y);
					Thread.sleep(250 + random.nextInt(100));
					leftClickRandom(robot, x, y + 72, 4);
					setWait(400);
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean bankLeather(Robot robot) throws AWTException, InterruptedException
	{
		for (int i = 0; i < 20; i++)
		{
			for (int j = 0; j < 20; j++)
			{
				int color = screen_capture.getRGB(471 + i, 8 + j);
				if (colorMatch(color, 0, 0, 1, 2))
				{
					Random random = new Random();
					// Deposit
					int x1 = x_corner + 619 + random.nextInt(20) - 10;
					int y1 = y_corner + 225 + random.nextInt(20) - 10;
					rightClick(robot, x1, y1);
					Thread.sleep(300 + random.nextInt(100));
					leftClickPrecise(robot, x1 + random.nextInt(20) - 10, y1 + 98);
					setWait(800 + random.nextInt(500));
					System.out.println("In bank window");
					return true;
				}
			}
		}
		return false;
	}
		
	public void withdrawCowhides(Robot robot) throws AWTException, InterruptedException
	{
		Random random = new Random();
		// Withdraw
		int x2 = x_corner + 86 + random.nextInt(20) - 10;
		int y2 = y_corner + 94 + random.nextInt(20) - 10;
		rightClick(robot, x2, y2);
		Thread.sleep(300 + random.nextInt(200));
		leftClickPrecise(robot, x2 + random.nextInt(20) - 10, y2 + 98);
	}
	
	public void readInventory(Robot robot)
	{
		Rectangle inventory_rect = new Rectangle(x_corner + 565, y_corner + 218, 141, 235);
		BufferedImage inventory_capture = robot.createScreenCapture(inventory_rect);
		int grid[][] = new int[141][235];
		/*for (int x = 0; x < 141; x++)
		{
			for (int y = 0; y < 235; y++)
			{
				grid[x][y] = 0;
			}
		}*/
		
		for (int y = 0; y < 7; y++)
		{
			for (int x = 0; x < 4; x++)
			{
				inventory[x][y] = "Empty";
			}
		}
		
		for (int x = 0; x <= 126; x += 42)
		{
			for (int y = 0; y <= 222; y += 37)
			{
				for (int m = 0; m < 15; m++)
				{
					for (int n = 0; n < 13; n++)
					{
						grid[x + m][y + n] = 1;
						int color = inventory_capture.getRGB(x + m, y + n);
						for (int i = 0; i < item_names.size(); i++)
						{
							if (!item_names.get(i).equals("Empty"))
							{
								if (colorMatch(color, items_red.get(i), items_green.get(i), items_blue.get(i), 3))
								{
									grid[x + m][y+n] = 8;
									inventory[x / 42][y / 37] = item_names.get(i);
									break;
								}
							}
						}
					}
				}
			}
		}
		
		for (int y = 0; y < 7; y++)
		{
			for (int x = 0; x < 4; x++)
			{
				System.out.print(inventory[x][y] + ", ");
			}
			System.out.println();
		}
		/*
		for (int y = 0; y < 235; y++)
		{
			for (int x = 0; x < 141; x++)
			{
				System.out.print(grid[x][y]);;
			}
			System.out.println();
		}*/
		System.out.println();
		
	}
	
	public void readDataIntoVector() throws IOException
	{
		ArrayList<String> lines = (ArrayList<String>) Files.readAllLines(Paths.get("Data.txt"));
		for (int i = 0; i < lines.size(); i++)
		{
			String line = lines.get(i);
			//System.out.println(line);
			String item_name = line.substring(0, line.indexOf(":"));
			line = line.substring(line.indexOf(":") + 1);
			//System.out.println(line);
			int red = Integer.parseInt((line.substring(0, line.indexOf(","))));
			line = line.substring(line.indexOf(",") + 1);
			//System.out.println(line);
			int green = Integer.parseInt((line.substring(0, line.indexOf(","))));
			line = line.substring(line.indexOf(",") + 1);
			//System.out.println(line);
			int blue = Integer.parseInt(line);
			item_names.add(item_name);
			items_red.add(red);
			items_green.add(green);
			items_blue.add(blue);
		}
		System.out.println("Printing arraylists: ");
		for (int j = 0; j < item_names.size(); j++)
		{
			System.out.println(item_names.get(j) + ": " + items_red.get(j) + ", " + items_green.get(j) + ", " + items_blue.get(j));
		}
		System.out.println();
	}
	
	public void runToTanner1(Robot robot) throws AWTException, InterruptedException
	{
		Random random = new Random();
		int x = x_corner + 672;
		int y = y_corner + 25;
		leftClickRandom(robot, x, y, 1);
		if (!run_on)
		{
			setWait(9300 + random.nextInt(1000));
		}
		else
		{
			setWait(5300 + random.nextInt(500));
		}
		
	}
	
	public void runToTanner2(Robot robot) throws AWTException, InterruptedException
	{
		Random random = new Random();
		int x = x_corner + 643;
		int y = y_corner + 28;//18;
		leftClickRandom(robot, x, y, 1);
		if (!run_on)
		{
			setWait(5000 + random.nextInt(1500));
		}
		else
		{
			setWait(2600 + random.nextInt(800));
		}
		
	}
	
	public void runToBank1(Robot robot) throws AWTException, InterruptedException
	{
		Random random = new Random();
		leftClickPrecise(robot, x_corner + 635, y_corner + 153);
		if (!run_on)
		{
			setWait(6800 + random.nextInt(1500));
		}
		else
		{
			setWait(3400 + random.nextInt(1100));
		}
	}
	
	public void centerView(Robot robot) throws AWTException, InterruptedException
	{
		Random random = new Random();
		leftClickRandom(robot, x_corner + 553, y_corner + 16, 10);
		robot.keyPress(KeyEvent.VK_UP);
		Thread.sleep(800 + random.nextInt(600));
		robot.keyRelease(KeyEvent.VK_UP);
		Thread.sleep(60 + random.nextInt(20));
	}

	public void runNorth(Robot robot) throws AWTException, InterruptedException
	{
		Random random = new Random();
		int x = x_corner + 638;
		int y = y_corner + 20;
		leftClickRandom(robot, x, y, 12);
		setWait(1300 + random.nextInt(200));
	}
	
	public void runSouth(Robot robot) throws AWTException, InterruptedException
	{
		Random random = new Random();
		int x = x_corner + 638;
		int y = y_corner + 135;
		leftClickRandom(robot, x, y, 12);
		setWait(1300 + random.nextInt(200));
	}
	
	public void runLittleSouth(Robot robot) throws AWTException, InterruptedException
	{
		Random random = new Random();
		int x = x_corner + 638;
		int y = y_corner + 90;
		leftClickRandom(robot, x, y, 6);
		setWait(1300 + random.nextInt(300));
	}
	
	public void runCenter(Robot robot) throws AWTException, InterruptedException
	{
		Random random = new Random();
		int x = x_corner + 638;;
		int y = y_corner + 60;
		// real center: int y = y_corner + 81;
		leftClickRandom(robot, x, y, 6);
		setWait(1300 + random.nextInt(200));
	}
	
	public void moveAround(Robot robot) throws AWTException, InterruptedException
	{
		Random random = new Random();
		int x = x_corner + 638;
		int y = y_corner + 81;
		leftClickRandom(robot, x, y, 20);
		setWait(1300 + random.nextInt(200));
	}
	
	public void detectRandoms(Robot robot) throws AWTException, InterruptedException
	{
		boolean found_dialogue = false;
		int x_length = 0;
		int x_location = 0;
		int y_location = 0;
		for (int x = 0; x < 490; x += 5)
		{
			for (int y = 0; y < 310; y += 1)
			{
				int color = screen_capture.getRGB(x, y);
				if (colorMatch(color, 255, 255, 0, 1))
				{
					found_dialogue = true;
					x_location = x + x_corner;
					y_location = y + y_corner;
					for (int k = 0; k < 490; k += 10)
					{
						for (int m = 0; m < 10; m++)
						{
							for (int n = 0; n < 10; n++)
							{
								int color2 = screen_capture.getRGB(k + m, y + n);
								if (colorMatch(color2, 255, 255, 0, 1))
								{
									x_length += 10;
									m = 10;;
									n = 10;
									break;
								}
							}
						}
					}
					x = 490;
					y = 310;
					break;
				}
			}
		}

		int chat_x_center = x_location + x_length / 2 - 10; // the center of the chat
		int chat_y_center = y_location + 15;
		if (found_dialogue && x_length > 50 && Math.abs(chat_x_center - x_center) < 100 && Math.abs(chat_y_center - y_center) < 75)
		{
			boolean found_random = false;
			boolean found_pickpocket = false;

			Rectangle dialogue_rect = new Rectangle(chat_x_center - 100, chat_y_center + 35, 200, 5);
			BufferedImage dialogue_capture = returnClickWindow(robot, chat_x_center, chat_y_center, dialogue_rect);
			int x_click = 0;
			int y_click = 0;
			
			for (int x = 0; x < 200; x++)
			{
				for (int y = 0; y < 5; y++)
				{
					int color = dialogue_capture.getRGB(x, y);
					if (colorMatch(color, 255, 255, 0, 1))
					{
						found_random = true;
						if (x_click == 0)
						{	
							x_click = chat_x_center + 100 - x;
							y_click = chat_y_center + 34 + y;
						}
					}
					else if (colorMatch(color, 0, 255, 0, 1))
					{
						found_pickpocket = true;
						x = 200;
						y = 5;
						break;
					}
				}
			}
			Random random = new Random();
			if (found_random && !found_pickpocket) 
			{
				Thread.sleep(300 + random.nextInt(200));
				leftClickPrecise(robot, x_click, y_click);
				Thread.sleep(2200 + random.nextInt(800));
			}
			else
			{
				int x_hover = random.nextInt(500) + x_corner;
				int y_hover = random.nextInt(320) + y_corner;
				robot.mouseMove(x_hover, y_hover);
				Thread.sleep(1200 + random.nextInt(300));
			}
		}
	}
	
	public void findClosestNPC(Robot robot) throws AWTException, InterruptedException // for now, find any accessible NPC
	{
		Random random = new Random();
		double[] NPC_distances = new double[20];
		for (int i = 0; i < 20; i++)
		{
			NPC_distances[i] = 999;
		}
		
		int count = 0;
		for (int x = 0; x < grid_width; x++)
		{
			for (int y = 0; y < grid_height; y++)
			{
				if (grid_NPC[x][y] > 0)
				{
					int[][] grid_obstacle_copy = new int[grid_width][grid_height]; 
					for (int m = 0; m < grid_width; m++)
					{
						for (int n = 0; n < grid_height; n++)
						{
							grid_obstacle_copy[m][n] = grid_obstacle[m][n];
						}
					}
					if (multi_combat_area)
					{
						if (searchPath(grid_obstacle_copy, x_grid_center, y_grid_center, x, y))
						{
							count++;
							NPC_distances[grid_NPC[x][y] - 1] = Math.sqrt(Math.pow((x_grid_center - x), 2) + Math.pow((y_grid_center - y), 2));				
							System.out.println("Found NPC with distance " + NPC_distances[grid_NPC[x][y] - 1]);
						}
					}
					else
					{
						if (searchPath(grid_obstacle_copy, x_grid_center, y_grid_center, x, y) && isNPCTaken(x, y, 30) == false)
						{
							count++;
							NPC_distances[grid_NPC[x][y] - 1] = Math.sqrt(Math.pow((x_grid_center - x), 2) + Math.pow((y_grid_center - y), 2));				
							System.out.println("Found NPC with distance " + NPC_distances[grid_NPC[x][y] - 1]);
						}
					}
				}
			}
		}
		
		if (count > 0)
		{
			double minimum = NPC_distances[0];
			int index_of_minimum = 0;
			for (int i = 1; i < 20; i++)
			{
				if (NPC_distances[i] < minimum)
				{
					index_of_minimum = i;
					minimum = NPC_distances[i];
				}
			}
			
			int NPC_x_loc = NPC_x_locs[index_of_minimum] + x_corner;
			int NPC_y_loc = NPC_y_locs[index_of_minimum] + y_corner;
			System.out.println("NPC found at (" + NPC_x_loc + ", " + NPC_y_loc + ")");
			leftClickRandom(robot, NPC_x_loc, NPC_y_loc, 4);
			setWait(4000 + random.nextInt(2000));
		}
		else
		{
			System.out.println("No NPCs were found");
		}
	}
	
	public boolean isNPCTaken(int x_loc, int y_loc, int radius)
	{
		for (int x = x_loc - radius; x < x_loc + radius; x++)
		{
			for (int y = y_loc - radius; y < y_loc + radius; y++)
			{
				if (x > 0 && x < width && y > 0 && y < height)
				{
					int color = screen_capture.getRGB(x, y);
					if (colorMatch(color, 0, 255, 0, 1) || colorMatch(color, 0, 255, 0, 1))
					{
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public boolean searchPath(int[][] search_grid, int x, int y, int x_end, int y_end) // Returns true if path exists
	{
		if (x == x_end && y == y_end)
		{
			return true;
		}
		else if (x < 0 || y < 0 || x >= grid_width || y >= grid_height) 
		{
			return false;
		}
		else if (search_grid[x][y] > 0) 
		{
			return false;
		}
		else
		{
			search_grid[x][y] = 1;
			if (searchPath(search_grid, x, y - 1, x_end, y_end))
			{
				return true;
			}
			if (searchPath(search_grid, x + 1, y, x_end, y_end))
			{
				return true;
			}
			if (searchPath(search_grid, x, y + 1, x_end, y_end))
			{
				return true;
			}
			if (searchPath(search_grid, x - 1, y, x_end, y_end))
			{
				return true;
			}
			search_grid[x][y] = 1;
			return false;
		}
	}
	
	public void leftClickRandom(Robot robot, int x, int y, int tolerance) throws AWTException, InterruptedException
	{
		Random random = new Random();
		robot.mouseMove(x + random.nextInt(tolerance) - tolerance / 2, y + random.nextInt(tolerance) - tolerance / 2);
		Thread.sleep(40 + random.nextInt(20));
		robot.mousePress(InputEvent.BUTTON1_MASK);
		Thread.sleep(40 + random.nextInt(20));
		robot.mouseRelease(InputEvent.BUTTON1_MASK);
	}
	
	public void leftClickPrecise(Robot robot, int x, int y) throws AWTException, InterruptedException
	{
		Random random = new Random();
		robot.mouseMove(x, y);
		Thread.sleep(40 + random.nextInt(20));
		robot.mousePress(InputEvent.BUTTON1_MASK);
		Thread.sleep(40 + random.nextInt(20));
		robot.mouseRelease(InputEvent.BUTTON1_MASK);
	}
	
	public void rightClick(Robot robot, int x, int y) throws AWTException, InterruptedException
	{
		Random random = new Random();
		robot.mouseMove(x, y);
		Thread.sleep(40 + random.nextInt(20));
		robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
		Thread.sleep(60 + random.nextInt(20));
		robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
	}
	
	public BufferedImage returnClickWindow(Robot robot, int x, int y, Rectangle dialogueRect) throws InterruptedException
	{
		Random random = new Random();
		robot.mouseMove(x, y);
		Thread.sleep(40 + random.nextInt(20));
		robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
		BufferedImage dialogueWindow = robot.createScreenCapture(dialogueRect);
		Thread.sleep(40 + random.nextInt(20));
		robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
		return dialogueWindow;
	}
	
	public void displayCenter()
	{
		System.out.println("Center: " + x_center + ", " + y_center + ")");
		System.out.println("Corner: " + x_corner + ", " + y_corner + ")");
	}
	
	public void addItems()
	{
		// Trout
		item_used[0] = true;
		item_red[0] = 164;
		item_green[0] = 135;
		item_blue[0] = 130;
		item_tolerance[0] = 3;
		item_category[0] = "Food";
		
		// Salmon
		item_used[1] = true;
		item_red[1] = 243;
		item_green[1] = 80;
		item_blue[1] = 28;
		item_tolerance[1] = 3;
		item_category[1] = "Food";
	}
	
	public void addObstacles()
	{
		
		for (int i = 0; i < number_of_obstacles; i++)
		{
			obstacle_used[i] = false;
		}
		if (bot_type == "Warrior")
		{
			obstacle_used[0] = true;
			obstacle_used[1] = true;
			obstacle_used[2] = true;
			obstacle_used[3] = true;
		}
		if (bot_type == "Tan" || bot_type == "Mine")
		{
			minimap_used = true;
		}
		
		// Al-Kharid castle wall bright
		obstacle_red[0] = 186;
		obstacle_green[0] = 185;
		obstacle_blue[0] = 175;
		obstacle_tolerance[0] = 5;
		
		// Al-Kharid castle wall dark
		obstacle_red[1] = 174;
		obstacle_green[1] = 174;
		obstacle_blue[1] = 162;
		obstacle_tolerance[1] = 2;
		
		// Door + Wooden furniture
		obstacle_red[2] = 106;
		obstacle_green[2] = 72;
		obstacle_blue[2] = 38;
		obstacle_tolerance[2] = 5;

		// Al-Kharid castle wall darkest
		obstacle_red[3] = 168;
		obstacle_green[3] = 168;
		obstacle_blue[3] = 156;
		obstacle_tolerance[3] = 1;
	}
	
	public void addNPCs()
	{
		for (int i = 0; i < number_of_NPCs; i++)
		{
			NPC_used[i] = false;
		}
		
		if (bot_type == "Warrior")
		{
			NPC_used[0] = true;
			multi_combat_area = true;
		}
		else if (bot_type == "Cow")
		{
			NPC_used[1] = true;
			NPC_used[2] = true;
			NPC_used[3] = true;
		}
		
		// Al-Kharid Warrior (Level 9)
		NPC_red[0] = 129;
		NPC_green[0] = 14;
		NPC_blue[0] = 128;
		NPC_tolerance[0] = 3;

		// Homogeneous Cow
		NPC_red[1] = 104;
		NPC_green[1] = 89;
		NPC_blue[1] = 84;
		NPC_tolerance[1] = 2;
		
		// Mixed light cow
		NPC_red[2] = 144;
		NPC_green[2] = 126;
		NPC_blue[2] = 113;
		NPC_tolerance[2] = 2;
		// Mixed dark cow
		
		NPC_red[3] = 89;
		NPC_green[3] = 81;
		NPC_blue[3] = 60;
		NPC_tolerance[3] = 2;
	}
	
	public void setWait(int desired_wait)
	{
		wait_end = System.currentTimeMillis() + desired_wait;
	}
	
	public boolean doneWaiting()
	{
		if (System.currentTimeMillis() >= wait_end)
		{
			return true;
		}
		return false;
	}
	
	public void addToActivityQueue(String activity)
	{
		this.activity_queue.add(activity);
	}
	
	public void allignMap(Robot robot) throws InterruptedException
	{
		Random random = new Random();
		if ((System.currentTimeMillis() - last_time_alligned) / 1000 / 60 > 6 + random.nextInt(3))
		{
			allignScreen(robot);
			last_time_alligned = System.currentTimeMillis();
		}
	}
	
	public void executeActivityQueue(Robot robot) throws AWTException, InterruptedException
	{
		if (!activity_queue.isEmpty())
		{
			System.out.println(activity_queue.elementAt(0));
			if (activity_queue.elementAt(0) == "Center View")
			{
				System.out.println("Centering view...");
				centerView(robot);
			}
			else if (activity_queue.elementAt(0) == "Run North")
			{
				System.out.println("Running north...");
				runNorth(robot);
			}
			else if (activity_queue.elementAt(0) == "Run South")
			{
				System.out.println("Running south...");
				runSouth(robot);
			}
			else if (activity_queue.elementAt(0) == "Run Center")
			{
				runCenter(robot);
			}
			else if (activity_queue.elementAt(0) == "Move Around")
			{
				moveAround(robot);
			}
			else if (activity_queue.elementAt(0).equals("Run to Tanner 1"))
			{
				runToTanner1(robot);
				miss_count = 0;
				this.activity_queue.remove(0);
			}
			else if (activity_queue.elementAt(0).equals("Run to Tanner 2"))
			{
				runToTanner2(robot);
				miss_count = 0;
				this.activity_queue.remove(0);
			}
			else if (activity_queue.elementAt(0).equals("Run to Bank 1"))
			{
				runToBank1(robot);
				miss_count = 0;
				this.activity_queue.remove(0);
			}
			else if (activity_queue.elementAt(0).equals("Click Banker"))
			{
				if (clickBanker(robot))
				{
					miss_count = 0;
					this.activity_queue.remove(0);
				}
				else
				{
					miss_count++;
				}
				if (miss_count > 30)
				{
					miss_count = 0;
					this.activity_queue.insertElementAt("Find Banker", 0);
				}
			}
			else if (activity_queue.elementAt(0).equals("Find Tanner"))
			{
				if (findTannerPro(robot))
				{
					miss_count = 0;
					this.activity_queue.remove(0);
				}
			/*	else
				{
					miss_count++;
				}
				if (miss_count > 100)
				{
					miss_count = 0;
					this.activity_queue.insertElementAt("Move Around", 0);
				}*/
			}
			else if (activity_queue.elementAt(0).equals("Tan Hides"))
			{
				if (tanHides(robot))
				{
					miss_count = 0;
					this.activity_queue.remove(0);
				}
				else
				{
					miss_count++;
				}
				if (miss_count > 30)
				{
					miss_count = 0;
					this.activity_queue.insertElementAt("Find Tanner", 0);
				}
			}
			else if (activity_queue.elementAt(0).equals("Find Banker"))
			{
				if (findBanker(robot))
				{
					miss_count = 0;
					this.activity_queue.remove(0);
				}
				else
				{
					miss_count++;
				}
				if (miss_count > 50)
				{
					centerView(robot);
					miss_count = 0;
					this.activity_queue.insertElementAt("Run Little South", 0);
				}
			}
			
			else if (activity_queue.elementAt(0).equals("Run Little South"))
			{
				runLittleSouth(robot);
				miss_count = 0;
				this.activity_queue.remove(0);
			}
			
			else if (activity_queue.elementAt(0).equals("Bank Leather"))
			{
				if (bankLeather(robot))
				{
					miss_count = 0;
					this.activity_queue.remove(0);
				}
				else
				{
					miss_count++;
				}
				if (miss_count > 10)
				{
					miss_count = 0;
					this.activity_queue.insertElementAt("Click Banker", 0);
				}
			}
			else if (activity_queue.elementAt(0).equals("Withdraw Cowhide"))
			{
				miss_count = 0;
				withdrawCowhides(robot);
				allignMap(robot);
				this.activity_queue.remove(0);
			}
			
			else if (activity_queue.elementAt(0).equals("Run from Bank to Mine"))
			{
				if (findMine(robot) && run_count > 8)
				{
					run_count = 0;
					System.out.println("Found mine");
					this.activity_queue.remove(0);
				}
				else
				{
					System.out.println(run_count);
					run_count++;
					runFromBankToMine(robot);
				}
			}
			else if (activity_queue.elementAt(0).equals("Run from Mine to Bank"))
			{
				
				// Only stop running south if bank is detected
				if (findBanker(robot))
				{
					System.out.println("Found banker");
					this.activity_queue.remove(0);
				}
				else
				{
					runFromMineToBank(robot);
				}
			}
			else if (activity_queue.elementAt(0) == "Mine Iron")
			{
				if (isInventoryFull(robot))
				{
					System.out.println("Inventory Full");
					this.activity_queue.remove(0);
				}
				else
				{
					mineIron(robot);
				}
			}
			else if (activity_queue.elementAt(0).equals("Run to Rock"))
			{
				runToRock(robot);
				this.activity_queue.remove(0);
			}
			else if (activity_queue.elementAt(0).equals("Find Rock"))
			{
				if (isInventoryFull(robot))
				{
					System.out.println("Inventory Full");
					this.activity_queue.remove(0);
				}
				else
				{
					System.out.println("Mining Rock");
					findRock(robot);
				}
			}
			else if (activity_queue.elementAt(0).equals("Find Mine Test"))
			{
				findMine(robot);
			}
		}
	}
	
	public void mine()
	{
		for (int i = 0; i < 10; i++)
		{
		
			//
			//this.activity_queue.addElement("Find Mine Test");
			
			
			this.activity_queue.addElement("Find Rock");
			this.activity_queue.addElement("Run from Mine to Bank");
			this.activity_queue.addElement("Click Banker");
			this.activity_queue.addElement("Bank Leather"); // Bank Iron, but uses same function
			this.activity_queue.addElement("Run from Bank to Mine");
			this.activity_queue.addElement("Run to Rock");
			//this.activity_queue.addElement("Mine");
		}
	}

	public void mineIron(Robot robot) throws AWTException, InterruptedException
	{
		Random random = new Random();
		int x_north_rock = x_center;
		int y_north_rock = y_center - 26;
		int x_east_rock = x_center + 25;
		int y_east_rock = y_center;
		
		if (rock_count % 2 == 0)
		{
			leftClickRandom(robot, x_north_rock, y_north_rock, 10);
		}
		else
		{
			leftClickRandom(robot, x_east_rock, y_east_rock, 10);
		}
		rock_count++;
		setWait(6000 + random.nextInt(500));
	}
	
	public void runFromMineToBank(Robot robot) throws AWTException, InterruptedException
	{
			Random random = new Random();
			int x = x_corner + 613;
			int y = y_corner + 135;
			leftClickRandom(robot, x, y, 6);
			if (run_on)
			{
				setWait(3000 + random.nextInt(500));
			}
			else
			{
				setWait(5500 + random.nextInt(1000));
			}
	}
	
	public boolean findMine(Robot robot) throws AWTException, InterruptedException
	{
		for (int x = 0; x < 148; x++)
		{
			for (int y = 0; y < 150; y++)
			{
				int color = minimap_capture.getRGB(x, y);
				if (colorMatch(color, 134, 78, 35, 5))
				{
					int count = 0;
					for (int m = -10; m < 18; m++)
					{
						for (int n = 0; n < 25; n++)
						{
							if (x + m >= 0 && x + m < 148 && y + n >= 0 && y + n < 150)
							{
								if ((m >= 0 && m <= 11) || (n >= 8 && n <= 17))
								{
									int color2 = minimap_capture.getRGB(x + m, y + n);
									if (colorMatch(color2, 134, 78, 35, 5))
									{
										count++;
									}
								}
							}
						}
					}
					if (count > 25)
					{
						System.out.println("count: " + count);
						leftClickPrecise(robot, 563 + x_corner + x, 4 + y_corner + y);
						Random random = new Random();
						setWait(9000 + random.nextInt(500));
						System.out.println("Found mine");
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public void runToRock(Robot robot) throws AWTException, InterruptedException
	{
		Random random = new Random();
		int x = x_corner + 658;
		int y = y_corner + 14;
		leftClickPrecise(robot, x, y);
		if (run_on)
		{
			setWait(5000 + random.nextInt(500));
		}
		else
		{
			setWait(10000 + random.nextInt(1000));
		}
	}
	
	public boolean findRock(Robot robot) throws AWTException, InterruptedException
	{
		Random random = new Random();
		for (int x = 0; x < width; x++)
		{
			for (int y = 0; y < height; y++)
			{
				int color = screen_capture.getRGB(x, y);
				if (colorMatch(color, 74, 45, 32, 3))
				{
					leftClickPrecise(robot, x_corner + x, y_corner + y);
					setWait(4000 + random.nextInt(1000));
					return true;
				}
			}
		}
		return false;
	}
	
	public void runFromBankToMine(Robot robot) throws AWTException, InterruptedException
	{
		Random random = new Random();
		int x = x_corner + 654;
		int y = y_corner + 10;
		leftClickRandom(robot, x, y, 6);
		if (run_on)
		{
			setWait(3000 + random.nextInt(500));
		}
		else
		{
			setWait(5500 + random.nextInt(1000));
		}
	}
	
	public boolean isInventoryFull(Robot robot)
	{
		Rectangle last_item_rect = new Rectangle(x_corner + 703, y_corner + 439, 1, 1);
		BufferedImage last_item = robot.createScreenCapture(last_item_rect);
		
		int color = last_item.getRGB(0, 0);
		if (colorMatch(color, 75, 66, 58, 20))
		{
			return false;
		}
		return true;
	}

	public String botType()
	{
		return bot_type;
	}
	
	
	public boolean isIdle(int time, int i) 
	{
		System.out.println("Bot " + i + " time inactive " + (System.currentTimeMillis() - last_time_active) / 1000);
		if ((System.currentTimeMillis() - last_time_active) / 1000 > time)
		{
			return true;
		}
		return false;
	}
}
