import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

public class ToolLauncher extends JFrame {
    private final JDesktopPane desktop = new JDesktopPane();
    private int windowCount = 0;
    private JPanel taskBar;
    private Map<JInternalFrame, JButton> taskBarButtons = new HashMap<>();

    // 在 ToolLauncher 类中添加这个方法
    public JDesktopPane getDesktopPane() {
        return desktop;
        }

    public ToolLauncher() {
        super("Javaows 3.1");

        // 设置外观和基本属性
        setClassicLookAndFeel();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        // 创建菜单栏
        createMenuBar();

        // 创建工具栏
        createToolBar();

        // 添加桌面
        add(desktop, BorderLayout.CENTER);

        // 创建任务栏（包含开始按钮）
        createTaskBar();

        // 创建状态栏
        createStatusBar();
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // 文件菜单
        JMenu fileMenu = new JMenu("文件(F)");
        fileMenu.setMnemonic('F');
        fileMenu.add(createMenuItem("退出(X)", 'X', e -> System.exit(0)));
        /*fileMenu.add(createMenuItem("保存布局(L)", 'S', this::saveLayout));
        fileMenu.add(createMenuItem("还原布局(R)", 'R', this::restoreLayout));*/

        // 编辑工具菜单
        JMenu editMenu = new JMenu("编辑工具(E)");
        editMenu.setMnemonic('E');
        editMenu.add(createMenuItem("文本编辑器", 'T', this::launchTextEditor));
        editMenu.add(createMenuItem("翻译包编辑器", 'R', this::launchTranslatorApp));

        // 系统工具菜单
        JMenu systemMenu = new JMenu("系统工具(S)");
        systemMenu.setMnemonic('S');
        systemMenu.add(createMenuItem("CMD 终端", 'C', this::launchCmdTerminal));
        systemMenu.add(createMenuItem("Java 启动器", 'J', this::launchJavaLauncher));
        systemMenu.add(createMenuItem("任务管理器", 'T', this::launchTaskmgr));
        systemMenu.add(createMenuItem("计算器",'A',this::launchCalculator));
        systemMenu.add(createMenuItem("文件资源管理器", 'E', this::launchExplorer));
        systemMenu.add(createMenuItem("日期和时间",'L',this::launchClockAndCalendar));
        systemMenu.add(createMenuItem("控制面板",'P',this::launchControlPanel));

        // 网络工具菜单
        JMenu networkMenu = new JMenu("网络工具(N)");
        networkMenu.setMnemonic('N');
        networkMenu.add(createMenuItem("Minecraft 版本查看器", 'M', this::launchMinecraftViewer));
        networkMenu.add(createMenuItem("API 数据获取器", 'A', this::launchAPIFetcher));
        networkMenu.add(createMenuItem("网络浏览器",'B',this::launchWebBrowser));
        networkMenu.add(createMenuItem("避雷针下载器",'D',this::launchDownloader));

        // 娱乐工具菜单
        JMenu entertainmentMenu = new JMenu("娱乐工具(L)");
        entertainmentMenu.setMnemonic('L');
        entertainmentMenu.add(createMenuItem("音乐播放器",'M',this::launchMusicPlayer));
        entertainmentMenu.add(createMenuItem("照片查看器",'P',this::launchImgViewer));
        entertainmentMenu.add(createMenuItem("扫雷", 'S', this::launchMineSweeper));
        entertainmentMenu.add(createMenuItem("视频播放器", 'V', this::launchVideoPlayer));

        // 办公工具菜单
        JMenu officeMenu = new JMenu("办公工具(O)");
        officeMenu.setMnemonic('O');
        officeMenu.add(createMenuItem("表格编辑器", 'G', this::launchCSVExcel));
        officeMenu.add(createMenuItem("文档编辑器", 'D', this::launchJavaWord));

        // 窗口菜单
        JMenu windowMenu = new JMenu("窗口(W)");
        windowMenu.setMnemonic('W');
        windowMenu.add(createMenuItem("层叠窗口", 'C', this::cascadeWindows));
        windowMenu.add(createMenuItem("平铺窗口", 'T', this::tileWindows));
        windowMenu.add(createMenuItem("关闭所有窗口", 'A', this::closeAllWindows));

        // 帮助菜单
        JMenu helpMenu = new JMenu("帮助(H)");
        helpMenu.setMnemonic('H');
        helpMenu.add(createMenuItem("关于", 'A', this::showAbout));

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(systemMenu);
        menuBar.add(networkMenu);
        menuBar.add(entertainmentMenu);
        menuBar.add(officeMenu);
        menuBar.add(windowMenu);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    private void createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        // 添加工具按钮
        toolBar.add(createToolButton("文本编辑器", this::launchTextEditor));
        toolBar.add(createToolButton("翻译包编辑器", this::launchTranslatorApp));
        toolBar.addSeparator();
        toolBar.add(createToolButton("CMD 终端", this::launchCmdTerminal));
        toolBar.add(createToolButton("Java 启动器", this::launchJavaLauncher));
        toolBar.addSeparator();
        toolBar.add(createToolButton("MC 版本查看器", this::launchMinecraftViewer));
        toolBar.add(createToolButton("API 数据获取器", this::launchAPIFetcher));

        add(toolBar, BorderLayout.NORTH);
    }

    private void createTaskBar() {
        taskBar = new JPanel(new BorderLayout());
        taskBar.setBorder(BorderFactory.createRaisedBevelBorder());
        taskBar.setPreferredSize(new Dimension(0, 40));

        // 创建开始按钮面板
        JPanel startPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        JButton startButton = new JButton("开始");
        startButton.setPreferredSize(new Dimension(80, 36));
        startButton.addActionListener(this::showStartMenu);
        startPanel.add(startButton);

        // 创建任务栏按钮面板
        JPanel taskButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        taskButtonPanel.setBackground(taskBar.getBackground());

        // 创建系统信息面板
        JPanel systemPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 2));
        JLabel timeLabel = new JLabel();
        updateTime(timeLabel);
        systemPanel.add(timeLabel);

        // 启动时间更新定时器
        Timer timer = new Timer(1000, e -> updateTime(timeLabel));
        timer.start();

        taskBar.add(startPanel, BorderLayout.WEST);
        taskBar.add(taskButtonPanel, BorderLayout.CENTER);
        taskBar.add(systemPanel, BorderLayout.EAST);

        // 将任务栏放在状态栏之上
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(taskBar, BorderLayout.NORTH);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void updateTime(JLabel timeLabel) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        timeLabel.setText(sdf.format(new java.util.Date()));
    }

    private void showStartMenu(ActionEvent e) {
        JButton startButton = (JButton) e.getSource();
        JPopupMenu startMenu = new JPopupMenu();
        startMenu.setBorder(BorderFactory.createRaisedBevelBorder());

        // 编辑工具子菜单
        JMenu editSubmenu = new JMenu("📝 编辑工具");
        editSubmenu.add(createMenuItem("文本编辑器", 'T', this::launchTextEditor));
        editSubmenu.add(createMenuItem("翻译包编辑器", 'R', this::launchTranslatorApp));

        // 系统工具子菜单
        JMenu systemSubmenu = new JMenu("⚙️ 系统工具");
        systemSubmenu.add(createMenuItem("CMD 终端", 'C', this::launchCmdTerminal));
        systemSubmenu.add(createMenuItem("Java 启动器", 'J', this::launchJavaLauncher));
        systemSubmenu.add(createMenuItem("计算器", 'A', this::launchCalculator));
        systemSubmenu.add(createMenuItem("任务管理器", 'T', this::launchTaskmgr));
        systemSubmenu.add(createMenuItem("文件资源管理器", 'E', this::launchExplorer));
        systemSubmenu.add(createMenuItem("日期和时间", 'L', this::launchClockAndCalendar));

        // 网络工具子菜单
        JMenu networkSubmenu = new JMenu("🌐 网络工具");
        networkSubmenu.add(createMenuItem("Minecraft 版本查看器", 'M', this::launchMinecraftViewer));
        networkSubmenu.add(createMenuItem("API 数据获取器", 'A', this::launchAPIFetcher));
        networkSubmenu.add(createMenuItem("网络浏览器", 'B', this::launchWebBrowser));
        networkSubmenu.add(createMenuItem("避雷针下载器",'D',this::launchDownloader));

        // 娱乐工具子菜单
        JMenu entertainmentSubmenu = new JMenu("🎵 娱乐工具");
        entertainmentSubmenu.add(createMenuItem("音乐播放器", 'M', this::launchMusicPlayer));
        entertainmentSubmenu.add(createMenuItem("照片查看器",'P',this::launchImgViewer));
        entertainmentSubmenu.add(createMenuItem("扫雷", 'S', this::launchMineSweeper));
        entertainmentSubmenu.add(createMenuItem("视频播放器", 'V', this::launchVideoPlayer));

        // 办公工具子菜单
        JMenu officeSubmenu = new JMenu("📊 办公工具");
        officeSubmenu.add(createMenuItem("表格编辑器", 'G', this::launchCSVExcel));
        officeSubmenu.add(createMenuItem("文档编辑器", 'D', this::launchJavaWord));

        // 窗口管理子菜单
        JMenu windowSubmenu = new JMenu("🪟 窗口管理");
        windowSubmenu.add(createMenuItem("层叠窗口", 'C', this::cascadeWindows));
        windowSubmenu.add(createMenuItem("平铺窗口", 'T', this::tileWindows));
        windowSubmenu.add(createMenuItem("关闭所有窗口", 'A', this::closeAllWindows));

        startMenu.add(editSubmenu);
        startMenu.add(systemSubmenu);
        startMenu.add(networkSubmenu);
        startMenu.add(entertainmentSubmenu);
        startMenu.add(officeSubmenu);
        startMenu.addSeparator();
        startMenu.add(windowSubmenu);
        startMenu.addSeparator();
        startMenu.add(createMenuItem("❓ 关于", 'A', this::showAbout));
        startMenu.add(createMenuItem("❌ 退出", 'X', e1 -> System.exit(0)));

        // 显示开始菜单
        startMenu.show(startButton, 0, -startMenu.getPreferredSize().height);
    }

    private void createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createLoweredBevelBorder());

        JLabel statusLabel = new JLabel("就绪");
        statusBar.add(statusLabel, BorderLayout.WEST);

        JLabel windowCountLabel = new JLabel("窗口数: 0");
        statusBar.add(windowCountLabel, BorderLayout.EAST);

        // 将状态栏添加到底部面板
        JPanel bottomPanel = (JPanel) ((BorderLayout) getContentPane().getLayout()).getLayoutComponent(BorderLayout.SOUTH);
        bottomPanel.add(statusBar, BorderLayout.SOUTH);
    }

    // 工具启动方法
    private void launchTextEditor(ActionEvent e) {
        createInternalFrame("文本编辑器", "📝", () -> {
            TextEditor editor = new TextEditor();
            editor.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            return editor;
        });
    }

    private void launchTranslatorApp(ActionEvent e) {
        createInternalFrame("翻译包编辑器", "🌐", () -> {
            TranslatorAppSwing translator = new TranslatorAppSwing();
            translator.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            return translator;
        });
    }

    private void launchCmdTerminal(ActionEvent e) {
        createInternalFrame("CMD 终端", "⚡", () -> {
            MDICmd cmd = new MDICmd();
            cmd.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            return cmd;
        });
    }

    private void launchJavaLauncher(ActionEvent e) {
        createInternalFrame("Java 启动器", "☕", () -> {
            MDILauncher launcher = new MDILauncher();
            launcher.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            return launcher;
        });
    }

    private void launchMinecraftViewer(ActionEvent e) {
        createInternalFrame("Minecraft 版本查看器", "🎮", () -> {
            MinecraftVersionViewerSwing viewer = new MinecraftVersionViewerSwing();
            viewer.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            return viewer;
        });
    }

    private void launchAPIFetcher(ActionEvent e) {
        createInternalFrame("API 数据获取器", "🔗", () -> {
            APIDataFetcher fetcher = new APIDataFetcher();
            fetcher.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            return fetcher;
        });
    }

    private void launchCalculator(ActionEvent e) {
        createInternalFrame("计算器", "🔢", () -> {
            Calc calc = new Calc();
            calc.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            return calc;
        });
    }

    private void launchMusicPlayer(ActionEvent e) {
        createInternalFrame("音乐播放器", "🎵", () -> {
            MusicPlayer player = new MusicPlayer();
            player.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            return player;
        });
    }

    private void launchWebBrowser(ActionEvent e) {
        createInternalFrame("网络浏览器", "🌐", () -> {
            WebBrowser browser = new WebBrowser();
            browser.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            return browser;
        });
    }

    private void launchExplorer(ActionEvent e) {
        createInternalFrame("文件资源管理器", "📁", () -> {
            Explorer explorer = new Explorer();
            explorer.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            return explorer;
        });
    }

    private void launchTaskmgr(ActionEvent e) {
        createInternalFrame("任务管理器", "🛠️", () -> {
            RealWindowsNTTaskManager taskManager = new RealWindowsNTTaskManager();
            taskManager.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            return taskManager;
        });
    }

    private void launchImgViewer(ActionEvent e) {
        createInternalFrame("照片查看器", "🖼️", () -> {
            ImgViewer imgViewer = new ImgViewer();
            imgViewer.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            return imgViewer;
        });
    }

    private void  launchClockAndCalendar(ActionEvent e) {
        createInternalFrame("日期和时间", "🕒", () -> {
            ClockAndCalender clockAndCalendar = new ClockAndCalender();
            clockAndCalendar.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            return clockAndCalendar;
        });
    }

    private void launchMineSweeper(ActionEvent e) {
        createInternalFrame("扫雷", "💣", () -> {
            MineSweeper mineSweeper = new MineSweeper();
            mineSweeper.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            return mineSweeper;
        });
    }

    private void launchControlPanel(ActionEvent e) {
    createInternalFrame("控制面板", "⚙️", () -> {
        ControlPanel controlPanel = new ControlPanel();
        controlPanel.setMainWindow(this); // 设置主窗口引用
        controlPanel.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        return controlPanel;
    });
}

    private void launchVideoPlayer(ActionEvent e) {
        createInternalFrame("视频播放器", "🎥", () -> {
            VideoPlayer videoPlayer = new VideoPlayer();
            videoPlayer.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            return videoPlayer;
        });
    }

    private void launchDownloader(ActionEvent e) {
        createInternalFrame("避雷针下载器", "⬇️", () -> {
            Downloader downloader = new Downloader();
            downloader.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            return downloader;
        });
    }

    private void launchCSVExcel(ActionEvent e) {
        createInternalFrame("Javaows Office Excel", "📊", () -> {
            CSVExcel csvExcel = new CSVExcel();
            csvExcel.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            return csvExcel;
        });
    }

    private void launchJavaWord(ActionEvent e) {
        createInternalFrame("Javaows Office Word", "📄", () -> {
            JavaWord javaWord = new JavaWord();
            javaWord.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            return javaWord;
        });
    }

    // 保存布局



    // 创建内部框架的通用方法
    private void createInternalFrame(String title, String icon, java.util.function.Supplier<JFrame> frameSupplier) {
        try {
            JInternalFrame internalFrame = new JInternalFrame(
                title + " #" + (++windowCount),
                true, true, true, true
            );

            // 创建工具窗口
            JFrame toolFrame = frameSupplier.get();

            // 将工具窗口的内容复制到内部框架
            internalFrame.setContentPane(toolFrame.getContentPane());
            internalFrame.setJMenuBar(toolFrame.getJMenuBar());

            // 设置大小和位置
            internalFrame.setSize(800, 600);
            internalFrame.setLocation(30 * (windowCount % 10), 30 * (windowCount % 10));

            // 添加到任务栏
            addToTaskBar(internalFrame, title, icon);

            // 添加内部框架监听器
            internalFrame.addInternalFrameListener(new javax.swing.event.InternalFrameAdapter() {
                @Override
                public void internalFrameClosed(javax.swing.event.InternalFrameEvent e) {
                    removeFromTaskBar(internalFrame);
                    updateWindowCount();
                }

                @Override
                public void internalFrameActivated(javax.swing.event.InternalFrameEvent e) {
                    updateTaskBarButton(internalFrame, true);
                }

                @Override
                public void internalFrameDeactivated(javax.swing.event.InternalFrameEvent e) {
                    updateTaskBarButton(internalFrame, false);
                }
            });

            desktop.add(internalFrame);
            internalFrame.setVisible(true);
            internalFrame.setSelected(true);

            updateWindowCount();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "启动工具失败: " + ex.getMessage(),
                "出错了喵",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    // 任务栏相关方法
    private void addToTaskBar(JInternalFrame frame, String title, String icon) {
        JButton taskButton = new JButton(icon + " " + title);
        taskButton.setPreferredSize(new Dimension(150, 32));
        taskButton.setMaximumSize(new Dimension(150, 32));

        taskButton.addActionListener(e -> {
            try {
                if (frame.isIcon()) {
                    frame.setIcon(false);
                }
                frame.setSelected(true);
                frame.toFront();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // 右键菜单
        JPopupMenu contextMenu = new JPopupMenu();
        contextMenu.add(createMenuItem("还原", 'R', e -> {
            try {
                if (frame.isIcon()) frame.setIcon(false);
                frame.setSelected(true);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }));
        contextMenu.add(createMenuItem("最小化", 'M', e -> {
            try {
                frame.setIcon(true);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }));
        contextMenu.add(createMenuItem("关闭", 'C', e -> frame.dispose()));

        taskButton.setComponentPopupMenu(contextMenu);

        taskBarButtons.put(frame, taskButton);

        // 获取任务栏按钮面板
        JPanel taskButtonPanel = (JPanel) ((BorderLayout) taskBar.getLayout()).getLayoutComponent(BorderLayout.CENTER);
        taskButtonPanel.add(taskButton);
        taskButtonPanel.revalidate();
        taskButtonPanel.repaint();
    }

    private void removeFromTaskBar(JInternalFrame frame) {
        JButton button = taskBarButtons.remove(frame);
        if (button != null) {
            JPanel taskButtonPanel = (JPanel) ((BorderLayout) taskBar.getLayout()).getLayoutComponent(BorderLayout.CENTER);
            taskButtonPanel.remove(button);
            taskButtonPanel.revalidate();
            taskButtonPanel.repaint();
        }
    }

    private void updateTaskBarButton(JInternalFrame frame, boolean active) {
        JButton button = taskBarButtons.get(frame);
        if (button != null) {
            if (active) {
                button.setBackground(UIManager.getColor("Button.select"));
                button.setBorder(BorderFactory.createLoweredBevelBorder());
            } else {
                button.setBackground(UIManager.getColor("Button.background"));
                button.setBorder(BorderFactory.createRaisedBevelBorder());
            }
        }
    }

    // 窗口管理方法
    private void cascadeWindows(ActionEvent e) {
        JInternalFrame[] frames = desktop.getAllFrames();
        for (int i = 0; i < frames.length; i++) {
            frames[i].setLocation(i * 25, i * 25);
            frames[i].setSize(600, 400);
            try {
                frames[i].setSelected(true);
            } catch (Exception ignored) {}
        }
    }

    private void tileWindows(ActionEvent e) {
        JInternalFrame[] frames = desktop.getAllFrames();
        if (frames.length == 0) return;

        int rows = (int) Math.sqrt(frames.length);
        int cols = (frames.length + rows - 1) / rows;

        Dimension desktopSize = desktop.getSize();
        int frameWidth = desktopSize.width / cols;
        int frameHeight = desktopSize.height / rows;

        for (int i = 0; i < frames.length; i++) {
            int row = i / cols;
            int col = i % cols;
            frames[i].setLocation(col * frameWidth, row * frameHeight);
            frames[i].setSize(frameWidth, frameHeight);
        }
    }

    private void closeAllWindows(ActionEvent e) {
        JInternalFrame[] frames = desktop.getAllFrames();
        for (JInternalFrame frame : frames) {
            frame.dispose();
        }
        windowCount = 0;
        updateWindowCount();
    }

    private void updateWindowCount() {
        // 获取底部面板中的状态栏
        JPanel bottomPanel = (JPanel) ((BorderLayout) getContentPane().getLayout()).getLayoutComponent(BorderLayout.SOUTH);
        JPanel statusBar = (JPanel) ((BorderLayout) bottomPanel.getLayout()).getLayoutComponent(BorderLayout.SOUTH);
        if (statusBar != null) {
            JLabel windowCountLabel = (JLabel) statusBar.getComponent(1);
            windowCountLabel.setText("窗口数: " + desktop.getAllFrames().length);
        }
    }

    // 帮助方法
    private void showAbout(ActionEvent e) {
    String about = String.format("""
        Javaows 3.1 - Java虚拟机（迫真）桌面环境

        ┌─ JVM信息 ───────────────────────────────
        • Java 版本  : %s
        • Java 提供商: %s
        • 操作系统   : %s %s (%s)
        • 用户目录   : %s
        • 最大内存   : %.1f MB

        ┌─ 已集成工具 ────────────────────────────
        • 文本编辑器         • 翻译包编辑器
        • CMD 终端           • Java 启动器
        • Minecraft 版本查看器
        • API 数据获取器     • 网络浏览器
        • 避雷针下载器       • 音乐播放器
        • 照片查看器         • 视频播放器
        • 扫雷               • 文件资源管理器
        • 任务管理器         • 日期和时间
        • 控制面板           • 表格编辑器 (CSV/Excel)
        • 文档编辑器 (MarkDown/Word)

        ┌─ 新增功能 ─────────────────────────────
        • 布局保存 / 还原           • 最近文件列表
        • 全局快捷键 Ctrl+N/O/S/W   • 系统托盘最小化
        • 暗黑 / 亮色主题切换       • 实时系统监视
        • 动态壁纸 / 锁屏面板       • 插件热加载
        • 一键更新 (GitHub Release)

        ┌─ 快捷键速查 ───────────────────────────
        Ctrl+N   新建文本
        Ctrl+O   打开文件
        Ctrl+S   保存
        Ctrl+W   关闭窗口
        Win+M    最小化全部
        Alt+F4   退出

        ┌─ 鸣谢 ──────────────────────────────────
        萌雨社 / 沙雕翻译包
        辉夜星瞳 & 全体贡献者
        GitHub 开源社区

        作者主页：https://moerain.cn
        """,
        System.getProperty("java.version"),
        System.getProperty("java.vendor"),
        System.getProperty("os.name"),
        System.getProperty("os.version"),
        System.getProperty("os.arch"),
        System.getProperty("user.home"),
        Runtime.getRuntime().maxMemory() / 1024.0 / 1024.0
    );

    JTextArea ta = new JTextArea(about);
    ta.setEditable(false);
    ta.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
    ta.setBackground(UIManager.getColor("Panel.background"));
    ta.setCaretPosition(0);
    JScrollPane sp = new JScrollPane(ta);
    sp.setPreferredSize(new Dimension(600, 400));

    JOptionPane.showMessageDialog(this, sp, "关于 Javaows 3.1", JOptionPane.INFORMATION_MESSAGE);
}

    // 工具方法
    private JMenuItem createMenuItem(String text, char mnemonic, java.util.function.Consumer<ActionEvent> action) {
        JMenuItem item = new JMenuItem(text);
        item.setMnemonic(mnemonic);
        item.addActionListener(action::accept);
        return item;
    }

    private JButton createToolButton(String text, java.util.function.Consumer<ActionEvent> action) {
        JButton button = new JButton(text);
        button.addActionListener(action::accept);
        return button;
    }

    private static void setClassicLookAndFeel() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
            UIManager.put("swing.boldMetal", Boolean.FALSE);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void setGlobalFont() {
        try {
            Font font = new Font("Microsoft YaHei", Font.PLAIN, 12);

            java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                Object value = UIManager.get(key);
                if (value instanceof javax.swing.plaf.FontUIResource) {
                    UIManager.put(key, font);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // 设置系统属性支持UTF-8
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("sun.jnu.encoding", "UTF-8");

        // 设置全局字体
        setGlobalFont();

        SwingUtilities.invokeLater(() -> new ToolLauncher().setVisible(true));
    }
}