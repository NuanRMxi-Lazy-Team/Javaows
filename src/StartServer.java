import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StartServer extends JFrame {
    private JButton startServerButton;
    private JTextField memoryField;
    private JTextField commandField;
    private JButton sendCommandButton;
    private JTextArea logArea;
    private JTextArea playerListArea;
    private JFreeChart chart;
    private TimeSeries cpuSeries;
    private TimeSeries memorySeries;
    private Process serverProcess;
    private Set<String> playerList;

    public StartServer() {
        super("启动 Minecraft 服务器");
        setSize(600, 200); // 调整窗口大小
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create input fields
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(2, 2, 10, 10));
        inputPanel.add(new JLabel("服务器文件:"));
        JButton selectFileButton = new JButton("选择文件");
        inputPanel.add(selectFileButton);

        inputPanel.add(new JLabel("分配内存 (GB):"));
        memoryField = new JTextField();
        inputPanel.add(memoryField);

        startServerButton = new JButton("启动服务器");
        startServerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startServer();
            }
        });



        selectFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fileChooser.setDialogTitle("选择 Minecraft 服务器文件 (server.jar)");

                int result = fileChooser.showOpenDialog(StartServer.this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    String serverPath = selectedFile.getAbsolutePath();
                    File serverDir = selectedFile.getParentFile(); // 获取 server.jar 所在目录

                    // 创建独立的日志窗口
                    JFrame logFrame = new JFrame("Minecraft 服务器日志");
                    logFrame.setSize(800, 600);
                    logFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    logArea = new JTextArea();
                    logArea.setEditable(false);
                    logFrame.add(new JScrollPane(logArea));
                    logFrame.setVisible(true);

                    // 创建独立的命令和性能窗口
                    JFrame commandFrame = new JFrame("服务器命令与性能");
                    commandFrame.setSize(800, 600);
                    commandFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

                    // 创建命令输入框
                    JPanel commandPanel = new JPanel();
                    commandPanel.setLayout(new BorderLayout());
                    commandField = new JTextField();
                    sendCommandButton = new JButton("发送命令");
                    sendCommandButton.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            sendCommand();
                        }
                    });
                    commandPanel.add(commandField, BorderLayout.CENTER);
                    commandPanel.add(sendCommandButton, BorderLayout.EAST);

                    // 创建性能图表
                    cpuSeries = new TimeSeries("CPU占用");
                    memorySeries = new TimeSeries("内存占用");
                    TimeSeriesCollection dataset = new TimeSeriesCollection();
                    dataset.addSeries(cpuSeries);
                    dataset.addSeries(memorySeries);
                    chart = ChartFactory.createTimeSeriesChart(
                            "服务器性能", "时间", "使用率 (%)", dataset, true, true, false
                    );
                    XYPlot plot = chart.getXYPlot();
                    XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
                    renderer.setSeriesPaint(0, Color.RED);
                    renderer.setSeriesPaint(1, Color.BLUE);
                    plot.setRenderer(renderer);

                    // 设置字体为微软雅黑
                    Font font = new Font("微软雅黑", Font.PLAIN, 12);
                    chart.getTitle().setFont(font);
                    plot.getDomainAxis().setLabelFont(font);
                    plot.getRangeAxis().setLabelFont(font);

                    ChartPanel chartPanel = new ChartPanel(chart);
                    chartPanel.setPreferredSize(new Dimension(800, 400));

                    // 创建玩家列表显示区域
                    playerListArea = new JTextArea();
                    playerListArea.setEditable(false);
                    playerListArea.setFont(font);

                    // 添加到命令窗口
                    commandFrame.add(commandPanel, BorderLayout.NORTH);
                    commandFrame.add(chartPanel, BorderLayout.CENTER);
                    commandFrame.add(new JScrollPane(playerListArea), BorderLayout.SOUTH);
                    commandFrame.setVisible(true);

                    try {
                        int memoryGB = Integer.parseInt(memoryField.getText());
                        String memoryOption = "-Xmx" + memoryGB + "G -Xms" + memoryGB + "G";

                        ProcessBuilder processBuilder = new ProcessBuilder(
                                "java", memoryOption, "-jar", serverPath, "nogui"
                        );
                        processBuilder.directory(serverDir); // 设置工作目录为 server.jar 所在目录
                        processBuilder.redirectErrorStream(true); // 合并输出流和错误流
                        serverProcess = processBuilder.start();

                        // 读取并显示日志
                        new Thread(() -> {
                            try (BufferedReader reader = new BufferedReader(new InputStreamReader(serverProcess.getInputStream()))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    logArea.append(line + "\n");
                                    updatePlayerList(line);
                                }
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }).start();

                        // 定期更新性能图表
                        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
                        executor.scheduleAtFixedRate(() -> {
                            // 模拟 CPU 和内存使用率数据
                            double cpuUsage = Math.random() * 100;
                            double memoryUsage = Math.random() * memoryGB * 100;
                            cpuSeries.add(new Millisecond(), cpuUsage);
                            memorySeries.add(new Millisecond(), memoryUsage);
                        }, 0, 1, TimeUnit.SECONDS);
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(StartServer.this, "无效的内存大小，请输入一个整数", "错误", JOptionPane.ERROR_MESSAGE);
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(StartServer.this, "启动服务器失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                        ex.printStackTrace();
                    }
                }
            }
        });

        add(inputPanel, BorderLayout.NORTH);
        add(startServerButton, BorderLayout.CENTER);
    }

     private void startServer() {
        // 启动服务器的逻辑可以放在这里
        JOptionPane.showMessageDialog(this, "请先选择服务器文件", "提示", JOptionPane.INFORMATION_MESSAGE);
    }

    private void sendCommand() {
        if (serverProcess != null && serverProcess.getOutputStream() != null) {
            try {
                String command = commandField.getText() + "\n";
                serverProcess.getOutputStream().write(command.getBytes());
                serverProcess.getOutputStream().flush();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "发送命令失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    private void updatePlayerList(String logLine) {
        // 使用正则表达式匹配玩家加入和离开的记录
        Pattern joinPattern = Pattern.compile("\\[(\\d{2}:\\d{2}:\\d{2})\\] \\[Server thread/INFO\\]: (\\w+) joined the game");
        Pattern leavePattern = Pattern.compile("\\[(\\d{2}:\\d{2}:\\d{2})\\] \\[Server thread/INFO\\]: (\\w+) left the game");
        Matcher joinMatcher = joinPattern.matcher(logLine);
        Matcher leaveMatcher = leavePattern.matcher(logLine);

        if (joinMatcher.find()) {
            String playerName = joinMatcher.group(2);
            if (playerList == null) {
                playerList = new HashSet<>();
            }
            playerList.add(playerName);
            updatePlayerListDisplay();
        } else if (leaveMatcher.find()) {
            String playerName = leaveMatcher.group(2);
            if (playerList != null && playerList.contains(playerName)) {
                playerList.remove(playerName);
                updatePlayerListDisplay();
            }
        }
    }

    private void updatePlayerListDisplay() {
        // 更新玩家列表显示
        if (playerList != null) {
            StringBuilder playerListText = new StringBuilder("在线玩家:\n");
            for (String player : playerList) {
                playerListText.append(player).append("\n");
            }
            playerListArea.setText(playerListText.toString());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            StartServer startServer = new StartServer();
            startServer.setVisible(true);
        });
    }
}