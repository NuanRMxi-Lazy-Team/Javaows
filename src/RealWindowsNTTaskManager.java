
import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.MetalTheme;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.OceanTheme;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class RealWindowsNTTaskManager extends JFrame {
    private JTabbedPane tabbedPane;
    private JTable processTable;
    private DefaultTableModel processTableModel;
    private JLabel statusLabel;
    private JLabel performanceLabel;
    private JProgressBar memoryBar;
    private JProgressBar cpuBar;
    private Timer updateTimer;
    private List<ProcessInfo> processes;

    // 线程池用于异步处理
    private ExecutorService executor = Executors.newFixedThreadPool(2);
    private volatile boolean isLoading = false;

    // 系统监控相关
    private OperatingSystemMXBean osBean;
    private MemoryMXBean memoryBean;
    private RuntimeMXBean runtimeBean;
    private long lastCpuTime = 0;
    private long lastUpTime = 0;
    private int processorCount;

    // Metal主题颜色
    private static final Color METAL_BLUE = new Color(102, 153, 204);
    private static final Color METAL_GRAY = new Color(204, 204, 204);
    private static final Color METAL_DARK_GRAY = new Color(102, 102, 102);
    private static final Color METAL_LIGHT_GRAY = new Color(238, 238, 238);

    // 进程信息类
    static class ProcessInfo {
        String imageName;
        String pid;
        String sessionName;
        String sessionId;
        String memUsage;
        String cpuUsage;
        String status;
        String user;

        ProcessInfo(String imageName, String pid, String sessionName, String sessionId,
                   String memUsage, String cpuUsage, String status, String user) {
            this.imageName = imageName;
            this.pid = pid;
            this.sessionName = sessionName;
            this.sessionId = sessionId;
            this.memUsage = memUsage;
            this.cpuUsage = cpuUsage;
            this.status = status;
            this.user = user;
        }
    }

    public RealWindowsNTTaskManager() {
        // 设置Metal外观
        setupMetalLookAndFeel();

        initializeSystemBeans();
        processes = new ArrayList<>();
        setupUI();
        setupTimer();

        // 异步加载进程数据，避免阻塞EDT
        loadRealProcessDataAsync();
    }

    private void setupMetalLookAndFeel() {
        try {
            // 设置Metal主题
            MetalLookAndFeel.setCurrentTheme(new OceanTheme());
            UIManager.setLookAndFeel(new MetalLookAndFeel());

            // 自定义Metal样式
            UIManager.put("TabbedPane.selected", METAL_BLUE);
            UIManager.put("TabbedPane.background", METAL_LIGHT_GRAY);
            UIManager.put("TabbedPane.foreground", Color.BLACK);
            UIManager.put("TabbedPane.focus", METAL_BLUE);

            // 表格样式
            UIManager.put("Table.selectionBackground", METAL_BLUE);
            UIManager.put("Table.selectionForeground", Color.WHITE);
            UIManager.put("Table.gridColor", METAL_GRAY);
            UIManager.put("Table.background", Color.WHITE);
            UIManager.put("Table.foreground", Color.BLACK);
            UIManager.put("TableHeader.background", METAL_LIGHT_GRAY);
            UIManager.put("TableHeader.foreground", Color.BLACK);

            // 进度条样式
            UIManager.put("ProgressBar.foreground", METAL_BLUE);
            UIManager.put("ProgressBar.background", METAL_LIGHT_GRAY);
            UIManager.put("ProgressBar.selectionForeground", Color.WHITE);
            UIManager.put("ProgressBar.selectionBackground", METAL_BLUE);

            // 按钮样式
            UIManager.put("Button.background", METAL_LIGHT_GRAY);
            UIManager.put("Button.foreground", Color.BLACK);
            UIManager.put("Button.focus", METAL_BLUE);

            // 菜单样式
            UIManager.put("Menu.background", METAL_LIGHT_GRAY);
            UIManager.put("Menu.foreground", Color.BLACK);
            UIManager.put("MenuItem.background", METAL_LIGHT_GRAY);
            UIManager.put("MenuItem.foreground", Color.BLACK);
            UIManager.put("MenuBar.background", METAL_LIGHT_GRAY);
            UIManager.put("MenuBar.foreground", Color.BLACK);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initializeSystemBeans() {
        osBean = ManagementFactory.getOperatingSystemMXBean();
        memoryBean = ManagementFactory.getMemoryMXBean();
        runtimeBean = ManagementFactory.getRuntimeMXBean();
        processorCount = osBean.getAvailableProcessors();
    }

    private void setupUI() {
        setTitle("任务管理器 - " + System.getProperty("os.name") + " [Metal主题]");
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 设置窗口图标和背景
        getContentPane().setBackground(METAL_LIGHT_GRAY);

        // 创建菜单栏
        createMenuBar();

        // 创建选项卡面板
        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(METAL_LIGHT_GRAY);
        tabbedPane.setForeground(Color.BLACK);
        tabbedPane.setFont(new Font("SansSerif", Font.BOLD, 12));

        // 创建进程选项卡
        createProcessTab();

        // 创建性能选项卡
        createPerformanceTab();

        add(tabbedPane, BorderLayout.CENTER);

        // 创建状态栏
        createStatusBar();

        // 应用Metal主题到所有组件
        SwingUtilities.updateComponentTreeUI(this);
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(METAL_LIGHT_GRAY);
        menuBar.setBorder(BorderFactory.createRaisedBevelBorder());

        // 文件菜单
        JMenu fileMenu = new JMenu("文件(F)");
        fileMenu.setMnemonic('F');
        fileMenu.setFont(new Font("SansSerif", Font.BOLD, 12));
        styleMenu(fileMenu);

        JMenuItem newTaskItem = new JMenuItem("新建任务(运行...)");
        newTaskItem.setAccelerator(KeyStroke.getKeyStroke("ctrl N"));
        newTaskItem.addActionListener(e -> showNewTaskDialog());
        styleMenuItem(newTaskItem);

        JMenuItem exitItem = new JMenuItem("退出任务管理器");
        exitItem.addActionListener(e -> {
            cleanup();
            System.exit(0);
        });
        styleMenuItem(exitItem);

        fileMenu.add(newTaskItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // 选项菜单
        JMenu optionsMenu = new JMenu("选项(O)");
        optionsMenu.setMnemonic('O');
        optionsMenu.setFont(new Font("SansSerif", Font.BOLD, 12));
        styleMenu(optionsMenu);

        JMenuItem alwaysOnTopItem = new JMenuItem("总在最前面");
        alwaysOnTopItem.addActionListener(e -> setAlwaysOnTop(!isAlwaysOnTop()));
        styleMenuItem(alwaysOnTopItem);

        JMenuItem refreshItem = new JMenuItem("立即刷新");
        refreshItem.setAccelerator(KeyStroke.getKeyStroke("F5"));
        refreshItem.addActionListener(e -> refreshProcesses());
        styleMenuItem(refreshItem);

        optionsMenu.add(alwaysOnTopItem);
        optionsMenu.add(refreshItem);

        // 查看菜单
        JMenu viewMenu = new JMenu("查看(V)");
        viewMenu.setMnemonic('V');
        viewMenu.setFont(new Font("SansSerif", Font.BOLD, 12));
        styleMenu(viewMenu);

        JMenuItem updateSpeedItem = new JMenuItem("更新速度");
        updateSpeedItem.addActionListener(e -> showUpdateSpeedDialog());
        styleMenuItem(updateSpeedItem);

        viewMenu.add(updateSpeedItem);

        // 帮助菜单
        JMenu helpMenu = new JMenu("帮助(H)");
        helpMenu.setMnemonic('H');
        helpMenu.setFont(new Font("SansSerif", Font.BOLD, 12));
        styleMenu(helpMenu);

        JMenuItem aboutItem = new JMenuItem("关于任务管理器");
        aboutItem.addActionListener(e -> showAboutDialog());
        styleMenuItem(aboutItem);

        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(optionsMenu);
        menuBar.add(viewMenu);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    private void styleMenu(JMenu menu) {
        menu.setBackground(METAL_LIGHT_GRAY);
        menu.setForeground(Color.BLACK);
        menu.setBorder(BorderFactory.createRaisedBevelBorder());
    }

    private void styleMenuItem(JMenuItem item) {
        item.setBackground(METAL_LIGHT_GRAY);
        item.setForeground(Color.BLACK);
        item.setFont(new Font("SansSerif", Font.PLAIN, 11));
    }

    private void createProcessTab() {
        JPanel processPanel = new JPanel(new BorderLayout());
        processPanel.setBackground(METAL_LIGHT_GRAY);

        // 创建进程表格
        String[] columnNames = {"映像名称", "PID", "用户名", "会话名", "内存使用", "CPU使用率", "状态"};
        processTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        processTable = new JTable(processTableModel);
        processTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        processTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // 设置表格Metal样式
        processTable.setBackground(Color.WHITE);
        processTable.setForeground(Color.BLACK);
        processTable.setSelectionBackground(METAL_BLUE);
        processTable.setSelectionForeground(Color.WHITE);
        processTable.setGridColor(METAL_GRAY);
        processTable.setShowGrid(true);
        processTable.setRowHeight(20);
        processTable.setFont(new Font("SansSerif", Font.PLAIN, 11));

        // 设置表格头样式
        JTableHeader header = processTable.getTableHeader();
        header.setBackground(METAL_LIGHT_GRAY);
        header.setForeground(Color.BLACK);
        header.setFont(new Font("SansSerif", Font.BOLD, 12));
        header.setReorderingAllowed(false);
        header.setBorder(BorderFactory.createRaisedBevelBorder());

        // 设置列宽
        int[] columnWidths = {150, 80, 100, 80, 100, 80, 80};
        for (int i = 0; i < columnWidths.length; i++) {
            TableColumn column = processTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(columnWidths[i]);
        }

        // 自定义单元格渲染器
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (isSelected) {
                    c.setBackground(METAL_BLUE);
                    c.setForeground(Color.WHITE);
                } else {
                    if (row % 2 == 0) {
                        c.setBackground(Color.WHITE);
                    } else {
                        c.setBackground(new Color(248, 248, 248));
                    }
                    c.setForeground(Color.BLACK);
                }

                setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
                return c;
            }
        };

        // 应用渲染器到所有列
        for (int i = 0; i < processTable.getColumnCount(); i++) {
            processTable.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        // 添加右键菜单
        createProcessContextMenu();

        JScrollPane scrollPane = new JScrollPane(processTable);
        scrollPane.setBackground(METAL_LIGHT_GRAY);
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setBorder(BorderFactory.createLoweredBevelBorder());

        processPanel.add(scrollPane, BorderLayout.CENTER);

        // 添加按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(METAL_LIGHT_GRAY);

        JButton endProcessButton = new JButton("结束进程");
        styleButton(endProcessButton);
        endProcessButton.addActionListener(e -> endSelectedProcess());
        buttonPanel.add(endProcessButton);

        processPanel.add(buttonPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("进程", processPanel);
    }

    private void createPerformanceTab() {
        JPanel performancePanel = new JPanel(new BorderLayout());
        performancePanel.setBackground(METAL_LIGHT_GRAY);

        JPanel metricsPanel = new JPanel(new GridBagLayout());
        metricsPanel.setBackground(METAL_LIGHT_GRAY);
        metricsPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createRaisedBevelBorder(), "系统性能",
            0, 0, new Font("SansSerif", Font.BOLD, 14), Color.BLACK));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);

        // CPU使用率
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel cpuLabel = new JLabel("CPU使用率:");
        cpuLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        cpuLabel.setForeground(Color.BLACK);
        metricsPanel.add(cpuLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 0;
        cpuBar = new JProgressBar(0, 100);
        cpuBar.setStringPainted(true);
        cpuBar.setPreferredSize(new Dimension(250, 25));
        cpuBar.setForeground(METAL_BLUE);
        cpuBar.setBackground(METAL_LIGHT_GRAY);
        cpuBar.setFont(new Font("SansSerif", Font.BOLD, 11));
        cpuBar.setBorder(BorderFactory.createLoweredBevelBorder());
        metricsPanel.add(cpuBar, gbc);

        // 内存使用率
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel memLabel = new JLabel("内存使用率:");
        memLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        memLabel.setForeground(Color.BLACK);
        metricsPanel.add(memLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        memoryBar = new JProgressBar(0, 100);
        memoryBar.setStringPainted(true);
        memoryBar.setPreferredSize(new Dimension(250, 25));
        memoryBar.setForeground(new Color(34, 139, 34)); // 绿色
        memoryBar.setBackground(METAL_LIGHT_GRAY);
        memoryBar.setFont(new Font("SansSerif", Font.BOLD, 11));
        memoryBar.setBorder(BorderFactory.createLoweredBevelBorder());
        metricsPanel.add(memoryBar, gbc);

        // 性能信息标签
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        performanceLabel = new JLabel();
        performanceLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        performanceLabel.setForeground(Color.BLACK);
        performanceLabel.setVerticalAlignment(SwingConstants.TOP);
        updatePerformanceInfo();
        metricsPanel.add(performanceLabel, gbc);

        performancePanel.add(metricsPanel, BorderLayout.CENTER);

        tabbedPane.addTab("性能", performancePanel);
    }

    private void styleButton(JButton button) {
        button.setBackground(METAL_LIGHT_GRAY);
        button.setForeground(Color.BLACK);
        button.setFont(new Font("SansSerif", Font.BOLD, 11));
        button.setBorder(BorderFactory.createRaisedBevelBorder());
        button.setPreferredSize(new Dimension(100, 28));

        // 添加鼠标悬停效果
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(METAL_BLUE);
                button.setForeground(Color.WHITE);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(METAL_LIGHT_GRAY);
                button.setForeground(Color.BLACK);
            }
        });
    }

    private void createStatusBar() {
        statusLabel = new JLabel("进程: 0");
        statusLabel.setBackground(METAL_LIGHT_GRAY);
        statusLabel.setForeground(Color.BLACK);
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        statusLabel.setBorder(BorderFactory.createLoweredBevelBorder());
        statusLabel.setOpaque(true);
        add(statusLabel, BorderLayout.SOUTH);
    }

    private void createProcessContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();
        contextMenu.setBackground(METAL_LIGHT_GRAY);
        contextMenu.setBorder(BorderFactory.createRaisedBevelBorder());

        JMenuItem endProcessItem = new JMenuItem("结束进程");
        endProcessItem.addActionListener(e -> endSelectedProcess());
        styleMenuItem(endProcessItem);

        JMenuItem refreshItem = new JMenuItem("刷新");
        refreshItem.addActionListener(e -> refreshProcesses());
        styleMenuItem(refreshItem);

        contextMenu.add(endProcessItem);
        contextMenu.addSeparator();
        contextMenu.add(refreshItem);

        processTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = processTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        processTable.setRowSelectionInterval(row, row);
                        contextMenu.show(processTable, e.getX(), e.getY());
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = processTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        processTable.setRowSelectionInterval(row, row);
                        contextMenu.show(processTable, e.getX(), e.getY());
                    }
                }
            }
        });
    }

    // 异步加载进程数据，避免阻塞EDT
    private void loadRealProcessDataAsync() {
        if (isLoading) return;

        isLoading = true;
        statusLabel.setText("正在加载进程...");

        executor.submit(() -> {
            try {
                List<ProcessInfo> newProcesses = new ArrayList<>();

                if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                    loadWindowsProcesses(newProcesses);
                } else {
                    loadUnixProcesses(newProcesses);
                }

                // 在EDT中更新UI
                SwingUtilities.invokeLater(() -> {
                    synchronized (processes) {
                        processes.clear();
                        processes.addAll(newProcesses);
                    }
                    refreshProcessTable();
                    isLoading = false;
                });

            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("加载进程失败: " + e.getMessage());
                    isLoading = false;
                });
            }
        });
    }

    private void loadWindowsProcesses(List<ProcessInfo> processList) {
        try {
            ProcessBuilder pb = new ProcessBuilder("tasklist", "/fo", "csv", "/v");
            Process process = pb.start();

            // 设置超时，避免无限等待
            Future<?> future = executor.submit(() -> {
                try {
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), "GBK")
                    );

                    String line;
                    int count = 0;
                    while ((line = reader.readLine()) != null) {
                        if (count++ == 0) continue; // 跳过标题

                        String[] parts = parseCSVLine(line);
                        if (parts.length >= 8) {
                            processList.add(new ProcessInfo(
                                parts[0], parts[1], parts[2], parts[3],
                                parts[4], "0", parts[5], parts[6]
                            ));
                        }
                    }
                    reader.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            // 等待最多5秒
            try {
                future.get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                future.cancel(true);
                process.destroyForcibly();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadUnixProcesses(List<ProcessInfo> processList) {
        try {
            ProcessBuilder pb = new ProcessBuilder("ps", "-eo", "pid,ppid,user,comm,pmem,pcpu,stat");
            Process process = pb.start();

            // 设置超时，避免无限等待
            Future<?> future = executor.submit(() -> {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    boolean firstLine = true;

                    while ((line = reader.readLine()) != null) {
                        if (firstLine) {
                            firstLine = false;
                            continue; // 跳过标题行
                        }

                        String[] parts = line.trim().split("\\s+");
                        if (parts.length >= 7) {
                            String pid = parts[0];
                            String user = parts[2];
                            String comm = parts[3];
                            String mem = parts[4] + "%";
                            String cpu = parts[5];
                            String stat = parts[6];

                            processList.add(new ProcessInfo(comm, pid, "Console", "0",
                                                          mem, cpu, stat, user));
                        }
                    }
                    reader.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            // 等待最多5秒
            try {
                future.get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                future.cancel(true);
                process.destroyForcibly();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String[] parseCSVLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        result.add(current.toString().trim());

        return result.toArray(new String[0]);
    }

    private void refreshProcessTable() {
        processTableModel.setRowCount(0);

        synchronized (processes) {
            for (ProcessInfo process : processes) {
                processTableModel.addRow(new Object[]{
                    process.imageName,
                    process.pid,
                    process.user,
                    process.sessionName,
                    process.memUsage,
                    process.cpuUsage + "%",
                    process.status
                });
            }
            statusLabel.setText("进程: " + processes.size());
        }
    }

    private void setupTimer() {
        // 修复Timer循环依赖问题
        updateTimer = new Timer(3000, e -> {
            try {
                updatePerformanceData();
                if (tabbedPane.getSelectedIndex() == 0 && !isLoading) {
                    loadRealProcessDataAsync();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        updateTimer.start();
    }

    private void updatePerformanceData() {
        try {
            // 获取内存使用情况
            long maxMemory = Runtime.getRuntime().maxMemory();
            long totalMem = Runtime.getRuntime().totalMemory();
            long freeMem = Runtime.getRuntime().freeMemory();
            long usedMem = totalMem - freeMem;

            int memoryPercentage = (int) ((usedMem * 100) / maxMemory);

            memoryBar.setValue(memoryPercentage);
            memoryBar.setString(memoryPercentage + "% (" + formatBytes(usedMem) + " / " + formatBytes(maxMemory) + ")");

            // 获取CPU使用率（简化计算）
            double cpuUsage = getCPUUsage();
            cpuBar.setValue((int) cpuUsage);
            cpuBar.setString(String.format("%.1f%%", cpuUsage));

            // 更新性能信息
            updatePerformanceInfo();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private double getCPUUsage() {
        try {
            // 使用反射获取CPU使用率（仅适用于某些JVM实现）
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsBean =
                    (com.sun.management.OperatingSystemMXBean) osBean;
                double cpuLoad = sunOsBean.getProcessCpuLoad();
                return cpuLoad >= 0 ? cpuLoad * 100 : 0;
            }
        } catch (Exception e) {
            // 如果无法获取，返回模拟值
        }

        // 如果无法获取真实CPU使用率，返回基于线程活动的估计值
        return Math.min(100, Thread.activeCount() * 2);
    }

    private void updatePerformanceInfo() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        synchronized (processes) {
            performanceLabel.setText(String.format(
                "<html>" +
                "<div style='font-family: SansSerif; font-size: 11px; color: black;'>" +
                "<b>系统信息:</b><br>" +
                "物理内存: %s / %s<br>" +
                "可用内存: %s<br>" +
                "处理器数: %d<br>" +
                "运行时间: %s<br>" +
                "总计进程: %d<br>" +
                "<i>Metal主题已激活</i>" +
                "</div>" +
                "</html>",
                formatBytes(usedMemory),
                formatBytes(totalMemory),
                formatBytes(freeMemory),
                processorCount,
                formatUptime(runtimeBean.getUptime()),
                processes.size()
            ));
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    private String formatUptime(long uptime) {
        long seconds = uptime / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return String.format("%d天 %02d:%02d:%02d", days, hours % 24, minutes % 60, seconds % 60);
        } else {
            return String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60);
        }
    }

    private void endSelectedProcess() {
        int selectedRow = processTable.getSelectedRow();
        if (selectedRow >= 0) {
            String processName = (String) processTableModel.getValueAt(selectedRow, 0);
            String pid = (String) processTableModel.getValueAt(selectedRow, 1);

            int result = JOptionPane.showConfirmDialog(
                this,
                "您确定要结束进程 \"" + processName + "\" (PID: " + pid + ") 吗？",
                "任务管理器警告",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );

            if (result == JOptionPane.YES_OPTION) {
                executor.submit(() -> {
                    try {
                        ProcessBuilder pb;
                        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                            pb = new ProcessBuilder("taskkill", "/F", "/PID", pid);
                        } else {
                            pb = new ProcessBuilder("kill", "-9", pid);
                        }

                        Process process = pb.start();
                        boolean finished = process.waitFor(3, TimeUnit.SECONDS);

                        if (!finished) {
                            process.destroyForcibly();
                        }

                        // 刷新进程列表
                        SwingUtilities.invokeLater(() -> {
                            loadRealProcessDataAsync();
                            JOptionPane.showMessageDialog(this, "进程已结束。", "任务管理器", JOptionPane.INFORMATION_MESSAGE);
                        });

                    } catch (Exception e) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(this, "无法结束进程: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                        });
                    }
                });
            }
        }
    }

    private void showNewTaskDialog() {
        String program = JOptionPane.showInputDialog(this, "键入程序、文件夹、文档或 Internet 资源的名称，Javaows 将为您打开它。", "新建任务", JOptionPane.PLAIN_MESSAGE);
        if (program != null && !program.trim().isEmpty()) {
            executor.submit(() -> {
                try {
                    ProcessBuilder pb = new ProcessBuilder(program.split("\\s+"));
                    pb.start();

                    // 延迟刷新进程列表
                    Thread.sleep(2000);
                    SwingUtilities.invokeLater(() -> loadRealProcessDataAsync());

                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "无法启动程序: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                    });
                }
            });
        }
    }

    private void showUpdateSpeedDialog() {
        String[] options = {"高(1秒)", "正常(3秒)", "低(5秒)", "暂停"};
        int choice = JOptionPane.showOptionDialog(this, "选择更新速度:", "更新速度", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[1]);

        if (choice >= 0) {
            if (updateTimer != null) {
                updateTimer.stop();
            }

            switch (choice) {
                case 0:
                    updateTimer = new Timer(1000, e -> {
                        updatePerformanceData();
                        if (tabbedPane.getSelectedIndex() == 0 && !isLoading) {
                            loadRealProcessDataAsync();
                        }
                    });
                    updateTimer.start();
                    break;
                case 1:
                    updateTimer = new Timer(3000, e -> {
                        updatePerformanceData();
                        if (tabbedPane.getSelectedIndex() == 0 && !isLoading) {
                            loadRealProcessDataAsync();
                        }
                    });
                    updateTimer.start();
                    break;
                case 2:
                    updateTimer = new Timer(5000, e -> {
                        updatePerformanceData();
                        if (tabbedPane.getSelectedIndex() == 0 && !isLoading) {
                            loadRealProcessDataAsync();
                        }
                    });
                    updateTimer.start();
                    break;
                case 3:
                    // 暂停
                    break;
            }
        }
    }

    private void showAboutDialog() {
        JOptionPane.showMessageDialog(this,
            "<html><div style='font-family: SansSerif; font-size: 12px;'>" +
            "<b>真实数据任务管理器</b><br>" +
            "版本 2.0 - Metal主题版<br><br>" +
            "操作系统: " + System.getProperty("os.name") + "<br>" +
            "Java版本: " + System.getProperty("java.version") + "<br>" +
            "处理器数: " + processorCount + "<br>" +
            "外观主题: Metal Look and Feel<br>" +
            "</div></html>",
            "关于任务管理器",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void refreshProcesses() {
        if (!isLoading) {
            loadRealProcessDataAsync();
        }
    }

    // 清理资源
    private void cleanup() {
        if (updateTimer != null) {
            updateTimer.stop();
        }
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            RealWindowsNTTaskManager taskManager = new RealWindowsNTTaskManager();
            taskManager.setVisible(true);
        });
    }
}