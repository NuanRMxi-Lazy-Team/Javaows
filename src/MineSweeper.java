import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Random;

public class MineSweeper extends JFrame {
    // 难度级别枚举
    public enum Difficulty {
        BEGINNER(9, 9, 10, "初级"),
        INTERMEDIATE(16, 16, 40, "中级"),
        EXPERT(30, 16, 99, "高级");

        private final int rows;
        private final int cols;
        private final int mines;
        private final String displayName;

        Difficulty(int rows, int cols, int mines, String displayName) {
            this.rows = rows;
            this.cols = cols;
            this.mines = mines;
            this.displayName = displayName;
        }

        public int getRows() { return rows; }
        public int getCols() { return cols; }
        public int getMines() { return mines; }
        public String getDisplayName() { return displayName; }
    }

    private int ROWS;
    private int COLS;
    private int MINES;
    private Difficulty currentDifficulty;

    private JButton[][] buttons;
    private boolean[][] mines;
    private int[][] numbers;
    private boolean[][] revealed;
    private boolean[][] flagged;
    private boolean gameOver;
    private boolean gameWon;
    private int revealedCount;
    private int flagCount;

    private JLabel statusLabel;
    private JLabel mineCountLabel;
    private JLabel timerLabel;
    private JButton restartButton;
    private Timer gameTimer;
    private int seconds;
    private JPanel gamePanel;
    private JMenuBar menuBar;

    public MineSweeper() {
        try {
            UIManager.setLookAndFeel(new MetalLookAndFeel());
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        // 默认中级难度
        currentDifficulty = Difficulty.INTERMEDIATE;
        setDifficulty(currentDifficulty);

        setupMenuBar();
        initializeGame();
        setupUI();
        generateMines();
        calculateNumbers();
        startTimer();
    }

    private void setupMenuBar() {
        menuBar = new JMenuBar();

        // 游戏菜单
        JMenu gameMenu = new JMenu("游戏");

        // 新游戏
        JMenuItem newGameItem = new JMenuItem("新游戏");
        newGameItem.setAccelerator(KeyStroke.getKeyStroke("F2"));
        newGameItem.addActionListener(e -> restartGame());

        // 分隔符
        JSeparator separator1 = new JSeparator();

        // 难度级别
        JMenu difficultyMenu = new JMenu("难度");

        // 创建难度选择按钮组
        ButtonGroup difficultyGroup = new ButtonGroup();

        for (Difficulty difficulty : Difficulty.values()) {
            JRadioButtonMenuItem difficultyItem = new JRadioButtonMenuItem(difficulty.getDisplayName());
            difficultyItem.setSelected(difficulty == currentDifficulty);
            difficultyItem.addActionListener(e -> changeDifficulty(difficulty));
            difficultyGroup.add(difficultyItem);
            difficultyMenu.add(difficultyItem);
        }

        // 分隔符
        JSeparator separator2 = new JSeparator();

        // 统计信息
        JMenuItem statisticsItem = new JMenuItem("统计信息");
        statisticsItem.addActionListener(e -> showStatistics());

        // 退出
        JMenuItem exitItem = new JMenuItem("退出");
        exitItem.addActionListener(e -> System.exit(0));

        // 添加到游戏菜单
        gameMenu.add(newGameItem);
        gameMenu.add(separator1);
        gameMenu.add(difficultyMenu);
        gameMenu.add(separator2);
        gameMenu.add(statisticsItem);
        gameMenu.addSeparator();
        gameMenu.add(exitItem);

        // 帮助菜单
        JMenu helpMenu = new JMenu("帮助");

        JMenuItem rulesItem = new JMenuItem("游戏规则");
        rulesItem.addActionListener(e -> showGameRules());

        JMenuItem aboutItem = new JMenuItem("关于");
        aboutItem.addActionListener(e -> showAbout());

        helpMenu.add(rulesItem);
        helpMenu.addSeparator();
        helpMenu.add(aboutItem);

        // 添加菜单到菜单栏
        menuBar.add(gameMenu);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    private void setDifficulty(Difficulty difficulty) {
        this.currentDifficulty = difficulty;
        this.ROWS = difficulty.getRows();
        this.COLS = difficulty.getCols();
        this.MINES = difficulty.getMines();
    }

    private void changeDifficulty(Difficulty difficulty) {
        if (difficulty != currentDifficulty) {
            setDifficulty(difficulty);
            restartGameWithNewDifficulty();
        }
    }

    private void restartGameWithNewDifficulty() {
        // 停止当前游戏
        if (gameTimer != null) {
            gameTimer.stop();
        }

        // 移除当前游戏面板
        if (gamePanel != null) {
            remove(gamePanel);
        }

        // 重新初始化游戏
        initializeGame();

        // 重新创建游戏面板
        createGamePanel();

        // 重新生成游戏
        generateMines();
        calculateNumbers();

        // 更新标签
        updateLabels();

        // 重新启动计时器
        startTimer();

        // 重新布局和显示
        revalidate();
        repaint();
        pack();
        setLocationRelativeTo(null);
    }

    private void initializeGame() {
        buttons = new JButton[ROWS][COLS];
        mines = new boolean[ROWS][COLS];
        numbers = new int[ROWS][COLS];
        revealed = new boolean[ROWS][COLS];
        flagged = new boolean[ROWS][COLS];
        gameOver = false;
        gameWon = false;
        revealedCount = 0;
        flagCount = 0;
        seconds = 0;
    }

    private void setupUI() {
        setTitle("扫雷 - " + currentDifficulty.getDisplayName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 创建顶部面板
        JPanel topPanel = new JPanel(new BorderLayout());

        // 状态面板
        JPanel statusPanel = new JPanel(new FlowLayout());
        mineCountLabel = new JLabel("雷数: " + MINES);
        timerLabel = new JLabel("时间: 0");
        restartButton = new JButton("重新开始");
        restartButton.addActionListener(e -> restartGame());

        statusPanel.add(mineCountLabel);
        statusPanel.add(Box.createHorizontalStrut(20));
        statusPanel.add(timerLabel);
        statusPanel.add(Box.createHorizontalStrut(20));
        statusPanel.add(restartButton);

        topPanel.add(statusPanel, BorderLayout.CENTER);

        // 游戏状态标签
        statusLabel = new JLabel("游戏进行中...", SwingConstants.CENTER);
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        topPanel.add(statusLabel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);

        // 创建游戏面板
        createGamePanel();

        pack();
        setLocationRelativeTo(null);
        setResizable(false);
    }

    private void createGamePanel() {
        gamePanel = new JPanel(new GridLayout(ROWS, COLS, 1, 1));
        gamePanel.setBorder(BorderFactory.createLoweredBevelBorder());

        // 计算按钮大小（根据难度调整）
        int buttonSize = 25;
        if (currentDifficulty == Difficulty.EXPERT) {
            buttonSize = 20; // 高级难度使用更小的按钮
        } else if (currentDifficulty == Difficulty.BEGINNER) {
            buttonSize = 30; // 初级难度使用更大的按钮
        }

        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                buttons[i][j] = new JButton();
                buttons[i][j].setPreferredSize(new Dimension(buttonSize, buttonSize));
                buttons[i][j].setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
                buttons[i][j].setMargin(new Insets(0, 0, 0, 0));

                final int row = i;
                final int col = j;

                buttons[i][j].addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (gameOver || gameWon) return;

                        if (SwingUtilities.isLeftMouseButton(e)) {
                            if (!flagged[row][col]) {
                                revealCell(row, col);
                            }
                        } else if (SwingUtilities.isRightMouseButton(e)) {
                            toggleFlag(row, col);
                        }
                    }
                });

                gamePanel.add(buttons[i][j]);
            }
        }

        add(gamePanel, BorderLayout.CENTER);
    }

    private void updateLabels() {
        mineCountLabel.setText("雷数: " + MINES);
        timerLabel.setText("时间: 0");
        statusLabel.setText("游戏进行中...");
        statusLabel.setForeground(Color.BLACK);
        setTitle("扫雷游戏 - Metal 主题 - " + currentDifficulty.getDisplayName());
    }

    private void generateMines() {
        Random random = new Random();
        int minesPlaced = 0;

        while (minesPlaced < MINES) {
            int row = random.nextInt(ROWS);
            int col = random.nextInt(COLS);

            if (!mines[row][col]) {
                mines[row][col] = true;
                minesPlaced++;
            }
        }
    }

    private void calculateNumbers() {
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                if (!mines[i][j]) {
                    numbers[i][j] = countAdjacentMines(i, j);
                }
            }
        }
    }

    private int countAdjacentMines(int row, int col) {
        int count = 0;
        for (int i = row - 1; i <= row + 1; i++) {
            for (int j = col - 1; j <= col + 1; j++) {
                if (i >= 0 && i < ROWS && j >= 0 && j < COLS && mines[i][j]) {
                    count++;
                }
            }
        }
        return count;
    }

    private void revealCell(int row, int col) {
        if (revealed[row][col] || flagged[row][col]) return;

        revealed[row][col] = true;
        revealedCount++;

        if (mines[row][col]) {
            // 游戏结束
            gameOver = true;
            gameTimer.stop();
            statusLabel.setText("游戏结束！你踩到了雷！");
            statusLabel.setForeground(Color.RED);
            revealAllMines();
            return;
        }

        // 更新按钮显示
        updateButtonDisplay(row, col);

        // 如果是空白格子，自动揭开相邻格子
        if (numbers[row][col] == 0) {
            for (int i = row - 1; i <= row + 1; i++) {
                for (int j = col - 1; j <= col + 1; j++) {
                    if (i >= 0 && i < ROWS && j >= 0 && j < COLS) {
                        revealCell(i, j);
                    }
                }
            }
        }

        // 检查胜利条件
        if (revealedCount == ROWS * COLS - MINES) {
            gameWon = true;
            gameTimer.stop();
            statusLabel.setText("恭喜！你赢了！");
            statusLabel.setForeground(Color.GREEN);
        }
    }

    private void updateButtonDisplay(int row, int col) {
        JButton button = buttons[row][col];
        button.setEnabled(false);

        if (mines[row][col]) {
            button.setText("💣");
            button.setBackground(Color.RED);
        } else if (numbers[row][col] > 0) {
            button.setText(String.valueOf(numbers[row][col]));
            button.setBackground(Color.WHITE);
            // 设置数字颜色
            Color[] colors = {Color.BLACK, Color.BLUE, Color.GREEN, Color.RED,
                            Color.MAGENTA, Color.ORANGE, Color.PINK, Color.CYAN, Color.GRAY};
            if (numbers[row][col] < colors.length) {
                button.setForeground(colors[numbers[row][col]]);
            }
        } else {
            button.setText("");
            button.setBackground(Color.WHITE);
        }
    }

    private void toggleFlag(int row, int col) {
        if (revealed[row][col]) return;

        flagged[row][col] = !flagged[row][col];

        if (flagged[row][col]) {
            buttons[row][col].setText("🚩");
            buttons[row][col].setForeground(Color.RED);
            flagCount++;
        } else {
            buttons[row][col].setText("");
            buttons[row][col].setForeground(Color.BLACK);
            flagCount--;
        }

        mineCountLabel.setText("雷数: " + (MINES - flagCount));
    }

    private void revealAllMines() {
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                if (mines[i][j]) {
                    buttons[i][j].setText("💣");
                    buttons[i][j].setBackground(Color.RED);
                    buttons[i][j].setEnabled(false);
                }
            }
        }
    }

    private void startTimer() {
        seconds = 0;
        gameTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                seconds++;
                timerLabel.setText("时间: " + seconds);
            }
        });
        gameTimer.start();
    }

    private void restartGame() {
    if (gameTimer != null) {
        gameTimer.stop();
    }

    // 重置游戏状态
    initializeGame();

    // 确保按钮数组已经初始化，如果没有则创建
    if (buttons == null) {
        createGamePanel();
    }

    // 重置按钮 - 添加空值检查
    for (int i = 0; i < ROWS; i++) {
        for (int j = 0; j < COLS; j++) {
            if (buttons[i][j] != null) {
                buttons[i][j].setText("");
                buttons[i][j].setBackground(UIManager.getColor("Button.background"));
                buttons[i][j].setForeground(Color.BLACK);
                buttons[i][j].setEnabled(true);
            }
        }
    }

    // 更新标签
    updateLabels();

    // 重新生成游戏
    generateMines();
    calculateNumbers();
    startTimer();
}

// 同时建议添加一个辅助方法来确保按钮数组正确初始化
private void ensureButtonsInitialized() {
    if (buttons == null || buttons.length != ROWS) {
        buttons = new JButton[ROWS][COLS];
    }

    for (int i = 0; i < ROWS; i++) {
        if (buttons[i] == null) {
            buttons[i] = new JButton[COLS];
        }
        for (int j = 0; j < COLS; j++) {
            if (buttons[i][j] == null) {
                buttons[i][j] = new JButton();
                // 在这里添加必要的按钮设置
            }
        }
    }
}

    // 菜单功能实现
    private void showStatistics() {
        JDialog dialog = new JDialog(this, "统计信息", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String statsText = "<html><body>" +
                "<h2>游戏统计</h2>" +
                "<p><b>当前难度:</b> " + currentDifficulty.getDisplayName() + "</p>" +
                "<p><b>网格大小:</b> " + ROWS + " × " + COLS + "</p>" +
                "<p><b>地雷数量:</b> " + MINES + "</p>" +
                "<p><b>当前时间:</b> " + seconds + " 秒</p>" +
                "<p><b>已揭开:</b> " + revealedCount + " 个格子</p>" +
                "<p><b>已标记:</b> " + flagCount + " 个标记</p>" +
                "<hr>" +
                "<p><i>提示: 使用菜单栏可以切换不同难度级别</i></p>" +
                "</body></html>";

        JLabel label = new JLabel(statsText);
        panel.add(label, BorderLayout.CENTER);

        JButton closeButton = new JButton("关闭");
        closeButton.addActionListener(e -> dialog.dispose());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void showGameRules() {
        JDialog dialog = new JDialog(this, "游戏规则", true);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String rulesText = "<html><body>" +
                "<h2>扫雷游戏规则</h2>" +
                "<h3>游戏目标:</h3>" +
                "<p>找出所有不含地雷的格子，避免踩到地雷。</p>" +
                "<h3>操作方法:</h3>" +
                "<ul>" +
                "<li><b>左键点击:</b> 揭开格子</li>" +
                "<li><b>右键点击:</b> 标记/取消标记可疑地雷</li>" +
                "</ul>" +
                "<h3>数字含义:</h3>" +
                "<p>格子中的数字表示该格子周围8个相邻格子中地雷的数量。</p>" +
                "<h3>难度级别:</h3>" +
                "<ul>" +
                "<li><b>初级:</b> 9×9 网格，10个地雷</li>" +
                "<li><b>中级:</b> 16×16 网格，40个地雷</li>" +
                "<li><b>高级:</b> 30×16 网格，99个地雷</li>" +
                "</ul>" +
                "<h3>胜利条件:</h3>" +
                "<p>揭开所有不含地雷的格子即可获胜。</p>" +
                "<h3>失败条件:</h3>" +
                "<p>点击到地雷格子即游戏结束。</p>" +
                "</body></html>";

        JLabel label = new JLabel(rulesText);
        JScrollPane scrollPane = new JScrollPane(label);
        panel.add(scrollPane, BorderLayout.CENTER);

        JButton closeButton = new JButton("关闭");
        closeButton.addActionListener(e -> dialog.dispose());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(this,
            "扫雷游戏 - Metal 主题\n" +
            "版本: 2.0\n" +
            "基于 Java Swing 开发\n\n" +
            "特性:\n" +
            "• 三种难度级别\n" +
            "• 经典 Metal 主题\n" +
            "• 完整的游戏功能\n" +
            "• 统计信息显示\n\n" +
            "使用 F2 键快速开始新游戏",
            "关于", JOptionPane.INFORMATION_MESSAGE);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new MetalLookAndFeel());
            } catch (UnsupportedLookAndFeelException e) {
                e.printStackTrace();
            }
            
            new MineSweeper().setVisible(true);
        });
    }
}