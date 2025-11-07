import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import javax.swing.*;

public class Pacman extends JFrame {
    
    public Pacman() {
        initUI();
    }
    
    private void initUI() {
        add(new GameBoard());
        setTitle("Pacman");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(380, 450);
        setLocationRelativeTo(null);
        setResizable(false);
    }
    
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            var game = new Pacman();
            game.setVisible(true);
        });
    }
}

/**
 * Main game board that handles all game logic and rendering
 */
class GameBoard extends JPanel implements ActionListener {
    
    // Game constants
    private static final int BLOCK_SIZE = 24;
    private static final int GRID_SIZE = 15;
    private static final int SCREEN_SIZE = GRID_SIZE * BLOCK_SIZE;
    private static final int ANIMATION_DELAY = 2;
    private static final int ANIMATION_FRAMES = 4;
    private static final int MAX_GHOSTS = 12;
    private static final int PACMAN_SPEED = 6;
    
    // Game state
    private boolean isPlaying = false;
    private boolean isDying = false;
    private int lives = 3;
    private int score = 0;
    private int currentLevel = 1;
    
    // Pacman properties
    private int pacmanX, pacmanY;
    private int pacmanDirX, pacmanDirY; // Current direction
    private int requestedDirX, requestedDirY; // Requested direction
    private int animationFrame = 0;
    private int animationCounter = ANIMATION_DELAY;
    
    // Ghost properties
    private Ghost[] ghosts;
    private int activeGhosts = 6;
    
    // Game objects
    private Timer gameTimer;
    private boolean[][] walls;
    private boolean[][] dots;
    
    // Images
    private Image pacmanImages[][]; // [direction][animationFrame]
    private Image ghostImage;
    
    public GameBoard() {
        initializeGame();
        loadResources();
        setupTimer();
        setupInput();
    }
    
    /**
     * Initialize game state and variables
     */
    private void initializeGame() {
        setBackground(Color.BLACK);
        setPreferredSize(new Dimension(SCREEN_SIZE, SCREEN_SIZE + 30));
        
        // Initialize maze data
        walls = new boolean[GRID_SIZE][GRID_SIZE];
        dots = new boolean[GRID_SIZE][GRID_SIZE];
        
        // Initialize ghosts
        ghosts = new Ghost[MAX_GHOSTS];
        for (int i = 0; i < MAX_GHOSTS; i++) {
            ghosts[i] = new Ghost();
        }
        
        loadLevel();
    }
    
    /**
     * Load game resources (images)
     */
    private void loadResources() {
        // Initialize pacman image array [direction][animation]
        pacmanImages = new Image[4][ANIMATION_FRAMES];
        
        // Load pacman images for different directions and animations
        String[] directions = {"up", "down", "left", "right"};
        for (int dir = 0; dir < directions.length; dir++) {
            for (int frame = 0; frame < ANIMATION_FRAMES; frame++) {
                String path = String.format("src/resources/images/%s%d.png", 
                                          directions[dir], frame + 1);
                pacmanImages[dir][frame] = new ImageIcon(path).getImage();
            }
        }
        
        // Load ghost image
        ghostImage = new ImageIcon("src/resources/images/ghost.png").getImage();
    }
    
    /**
     * Set up the game timer
     */
    private void setupTimer() {
        gameTimer = new Timer(40, this); // ~25 FPS
        gameTimer.start();
    }
    
    /**
     * Set up keyboard input handling
     */
    private void setupInput() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyPress(e.getKeyCode());
            }
        });
        setFocusable(true);
    }
    
    /**
     * Handle keyboard input
     */
    private void handleKeyPress(int keyCode) {
        if (isPlaying) {
            handleGameInput(keyCode);
        } else {
            handleMenuInput(keyCode);
        }
    }
    
    /**
     * Handle input during gameplay
     */
    private void handleGameInput(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_W -> requestMove(0, -1);  // Up
            case KeyEvent.VK_S -> requestMove(0, 1);   // Down  
            case KeyEvent.VK_A -> requestMove(-1, 0);  // Left
            case KeyEvent.VK_D -> requestMove(1, 0);   // Right
            case KeyEvent.VK_P -> togglePause();
            case KeyEvent.VK_ESCAPE -> returnToMenu();
        }
    }
    
    /**
     * Handle input in menu
     */
    private void handleMenuInput(int keyCode) {
        if (keyCode == KeyEvent.VK_ENTER) {
            startGame();
        }
    }
    
    /**
     * Request movement in specified direction
     */
    private void requestMove(int dx, int dy) {
        requestedDirX = dx;
        requestedDirY = dy;
    }
    
    /**
     * Toggle game pause state
     */
    private void togglePause() {
        if (gameTimer.isRunning()) {
            gameTimer.stop();
        } else {
            gameTimer.start();
        }
    }
    
    /**
     * Return to main menu
     */
    private void returnToMenu() {
        isPlaying = false;
    }
    
    /**
     * Start a new game
     */
    private void startGame() {
        isPlaying = true;
        lives = 3;
        score = 0;
        currentLevel = 1;
        loadLevel();
    }
    
    /**
     * Load and initialize level
     */
    private void loadLevel() {
        initializeMaze();
        resetCharacters();
        activeGhosts = Math.min(6 + currentLevel - 1, MAX_GHOSTS);
    }
    
    /**
     * Initialize maze walls and dots
     */
    private void initializeMaze() {
        // Simple maze layout - you can replace this with more complex designs
        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                // Border walls
                if (x == 0 || y == 0 || x == GRID_SIZE - 1 || y == GRID_SIZE - 1) {
                    walls[x][y] = true;
                    dots[x][y] = false;
                } 
                // Internal walls and dots
                else {
                    walls[x][y] = (x % 3 == 0 && y % 3 == 0); // Simple pattern
                    dots[x][y] = !walls[x][y];
                }
            }
        }
        
        // Clear starting area
        walls[7][11] = false;
        dots[7][11] = false;
    }
    
    /**
     * Reset character positions
     */
    private void resetCharacters() {
        // Reset pacman
        pacmanX = 7 * BLOCK_SIZE;
        pacmanY = 11 * BLOCK_SIZE;
        pacmanDirX = 0;
        pacmanDirY = 0;
        requestedDirX = 0;
        requestedDirY = 0;
        
        // Reset ghosts
        for (int i = 0; i < activeGhosts; i++) {
            ghosts[i].reset();
            ghosts[i].x = 4 * BLOCK_SIZE;
            ghosts[i].y = 4 * BLOCK_SIZE;
            ghosts[i].speed = 1 + (i % 3); // Varying speeds
        }
        
        isDying = false;
    }
    
    /**
     * Main game loop
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (isPlaying) {
            updateGame();
        }
        repaint();
    }
    
    /**
     * Update game state
     */
    private void updateGame() {
        if (isDying) {
            handleDeath();
        } else {
            updateAnimation();
            movePacman();
            moveGhosts();
            checkCollisions();
            checkLevelCompletion();
        }
    }
    
    /**
     * Update pacman animation
     */
    private void updateAnimation() {
        animationCounter--;
        if (animationCounter <= 0) {
            animationCounter = ANIMATION_DELAY;
            animationFrame = (animationFrame + 1) % ANIMATION_FRAMES;
        }
    }
    
    /**
     * Move pacman based on input and collision detection
     */
    private void movePacman() {
        // Check if we can change direction
        if (canMove(requestedDirX, requestedDirY, pacmanX, pacmanY)) {
            pacmanDirX = requestedDirX;
            pacmanDirY = requestedDirY;
        }
        
        // Move if current direction is valid
        if (canMove(pacmanDirX, pacmanDirY, pacmanX, pacmanY)) {
            pacmanX += pacmanDirX * PACMAN_SPEED;
            pacmanY += pacmanDirY * PACMAN_SPEED;
            
            // Collect dots
            checkDotCollection();
        }
    }
    
    /**
     * Check if movement is possible
     */
    private boolean canMove(int dx, int dy, int x, int y) {
        if (dx == 0 && dy == 0) return true;
        
        // Calculate next grid position
        int nextX = (x + dx * PACMAN_SPEED) / BLOCK_SIZE;
        int nextY = (y + dy * PACMAN_SPEED) / BLOCK_SIZE;
        
        // Check bounds and walls
        return nextX >= 0 && nextX < GRID_SIZE && 
               nextY >= 0 && nextY < GRID_SIZE && 
               !walls[nextX][nextY];
    }
    
    /**
     * Check for dot collection
     */
    private void checkDotCollection() {
        int gridX = pacmanX / BLOCK_SIZE;
        int gridY = pacmanY / BLOCK_SIZE;
        
        if (dots[gridX][gridY]) {
            dots[gridX][gridY] = false;
            score += 10;
        }
    }
    
    /**
     * Move all active ghosts
     */
    private void moveGhosts() {
        for (int i = 0; i < activeGhosts; i++) {
            ghosts[i].move(walls);
            
            // Simple tracking behavior
            if (Math.random() < 0.3) { // 30% chance to move toward pacman
                int dx = Integer.compare(pacmanX, ghosts[i].x);
                int dy = Integer.compare(pacmanY, ghosts[i].y);
                
                if (canMove(dx, dy, ghosts[i].x, ghosts[i].y)) {
                    ghosts[i].dirX = dx;
                    ghosts[i].dirY = dy;
                }
            }
        }
    }
    
    /**
     * Check for collisions with ghosts
     */
    private void checkCollisions() {
        for (int i = 0; i < activeGhosts; i++) {
            if (Math.abs(pacmanX - ghosts[i].x) < BLOCK_SIZE / 2 && 
                Math.abs(pacmanY - ghosts[i].y) < BLOCK_SIZE / 2) {
                isDying = true;
                break;
            }
        }
    }
    
    /**
     * Handle death sequence
     */
    private void handleDeath() {
        lives--;
        if (lives > 0) {
            resetCharacters();
        } else {
            isPlaying = false; // Game over
        }
    }
    
    /**
     * Check if level is completed
     */
    private void checkLevelCompletion() {
        boolean dotsRemaining = false;
        for (boolean[] row : dots) {
            for (boolean dot : row) {
                if (dot) {
                    dotsRemaining = true;
                    break;
                }
            }
            if (dotsRemaining) break;
        }
        
        if (!dotsRemaining) {
            currentLevel++;
            score += 100 * currentLevel; // Level completion bonus
            loadLevel();
        }
    }
    
    /**
     * Render game graphics
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        // Enable anti-aliasing for smoother graphics
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                           RenderingHints.VALUE_ANTIALIAS_ON);
        
        if (isPlaying) {
            drawMaze(g2d);
            drawPacman(g2d);
            drawGhosts(g2d);
            drawHUD(g2d);
        } else {
            drawMenu(g2d);
        }
    }
    
    /**
     * Draw maze walls and dots
     */
    private void drawMaze(Graphics2D g2d) {
        // Draw walls
        g2d.setColor(new Color(0, 100, 200));
        g2d.setStroke(new BasicStroke(3));
        
        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                if (walls[x][y]) {
                    g2d.drawRect(x * BLOCK_SIZE, y * BLOCK_SIZE, 
                               BLOCK_SIZE, BLOCK_SIZE);
                }
            }
        }
        
        // Draw dots
        g2d.setColor(Color.YELLOW);
        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                if (dots[x][y]) {
                    g2d.fillOval(x * BLOCK_SIZE + BLOCK_SIZE / 2 - 2,
                               y * BLOCK_SIZE + BLOCK_SIZE / 2 - 2, 4, 4);
                }
            }
        }
    }
    
    /**
     * Draw pacman with current animation
     */
    private void drawPacman(Graphics2D g2d) {
        int directionIndex = getDirectionIndex();
        g2d.drawImage(pacmanImages[directionIndex][animationFrame], 
                     pacmanX, pacmanY, this);
    }
    
    /**
     * Get image index for current direction
     */
    private int getDirectionIndex() {
        if (pacmanDirY < 0) return 0; // Up
        if (pacmanDirY > 0) return 1; // Down
        if (pacmanDirX < 0) return 2; // Left
        if (pacmanDirX > 0) return 3; // Right
        return 3; // Default to right
    }
    
    /**
     * Draw all active ghosts
     */
    private void drawGhosts(Graphics2D g2d) {
        for (int i = 0; i < activeGhosts; i++) {
            g2d.drawImage(ghostImage, ghosts[i].x, ghosts[i].y, this);
        }
    }
    
    /**
     * Draw heads-up display (score, lives, etc.)
     */
    private void drawHUD(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        
        // Draw score
        g2d.drawString("Score: " + score, 10, SCREEN_SIZE + 20);
        
        // Draw lives
        g2d.drawString("Lives: " + lives, SCREEN_SIZE - 60, SCREEN_SIZE + 20);
        
        // Draw level
        g2d.drawString("Level: " + currentLevel, SCREEN_SIZE / 2 - 20, SCREEN_SIZE + 20);
    }
    
    /**
     * Draw main menu
     */
    private void drawMenu(Graphics2D g2d) {
        g2d.setColor(new Color(0, 32, 48));
        g2d.fillRect(50, SCREEN_SIZE / 2 - 40, SCREEN_SIZE - 100, 80);
        g2d.setColor(Color.WHITE);
        g2d.drawRect(50, SCREEN_SIZE / 2 - 40, SCREEN_SIZE - 100, 80);
        
        String title = "PACMAN by Aiden&Eason";
        String instruction = "Press ENTER to Start";
        String controls = "WASD to Move";
        
        Font titleFont = new Font("Arial", Font.BOLD, 20);
        Font normalFont = new Font("Arial", Font.PLAIN, 14);
        
        // Draw title
        g2d.setFont(titleFont);
        g2d.drawString(title, (SCREEN_SIZE - g2d.getFontMetrics().stringWidth(title)) / 2, 
                      SCREEN_SIZE / 2 - 10);
        
        // Draw instructions
        g2d.setFont(normalFont);
        g2d.drawString(instruction, (SCREEN_SIZE - g2d.getFontMetrics().stringWidth(instruction)) / 2, 
                      SCREEN_SIZE / 2 + 15);
        g2d.drawString(controls, (SCREEN_SIZE - g2d.getFontMetrics().stringWidth(controls)) / 2, 
                      SCREEN_SIZE / 2 + 35);
    }
}

/**
 * Ghost class representing enemy characters
 */
class Ghost {
    int x, y;
    int dirX, dirY;
    int speed;
    
    /**
     * Reset ghost to default state
     */
    void reset() {
        dirX = (Math.random() > 0.5) ? 1 : -1;
        dirY = 0;
    }
    
    /**
     * Move ghost with basic AI
     */
    void move(boolean[][] walls) {
        // random movement with wall avoidance
        if (Math.random() < 0.1) { // 10% chance to change direction
            int newDirX = (int) (Math.random() * 3) - 1; // -1, 0, or 1
            int newDirY = (int) (Math.random() * 3) - 1;
            
            // Avoid zero movement
            if (newDirX == 0 && newDirY == 0) {
                newDirX = (Math.random() > 0.5) ? 1 : -1;
            }
            
            dirX = newDirX;
            dirY = newDirY;
        }
        
        // Apply movement
        x += dirX * speed;
        y += dirY * speed;
        
        // Basic bounds checking
        int gridSize = walls.length;
        int blockSize = 24; // Should match BLOCK_SIZE from GameBoard
        
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        if (x >= gridSize * blockSize) x = gridSize * blockSize - 1;
        if (y >= gridSize * blockSize) y = gridSize * blockSize - 1;
    }
}
