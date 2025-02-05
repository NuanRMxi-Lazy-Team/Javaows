import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

public class MinecraftLauncher extends JFrame {
    private JButton findLauncherButton;
    private JButton downloadHMCLButton;
    private JComboBox<String> launcherComboBox;
    private Properties config;
    private String configFilePath;

    public MinecraftLauncher() {
        super("Minecraft 启动器");
        setSize(600, 200); // 调整窗口大小
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // Load configuration
        configFilePath = System.getProperty("user.home") + "/.minecraftlauncher/config.properties";
        config = loadConfig();

        // Create buttons
        findLauncherButton = new JButton("寻找启动器");
        downloadHMCLButton = new JButton("下载 HMCL");

        // Create combo box
        launcherComboBox = new JComboBox<>();
        for (String path : config.stringPropertyNames()) {
            launcherComboBox.addItem(path);
        }

        // Button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10)); // 水平排列
        buttonPanel.add(findLauncherButton);
        buttonPanel.add(downloadHMCLButton);
        buttonPanel.add(launcherComboBox);

        // Add button panel to frame
        add(buttonPanel, BorderLayout.CENTER);

        // Add action listeners
        findLauncherButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                findExistingLauncher();
            }
        });

        downloadHMCLButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                downloadAndLaunchHMCL();
            }
        });

        launcherComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                launchSelectedLauncher();
            }
        });
    }

    private void findExistingLauncher() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setDialogTitle("选择现有的 Minecraft 启动器");

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String path = selectedFile.getAbsolutePath();
            config.setProperty(path, path);
            saveConfig();
            launcherComboBox.addItem(path);
            JOptionPane.showMessageDialog(this, "已添加启动器: " + path, "成功", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void downloadAndLaunchHMCL() {
        String downloadUrl = "https://files.moerain.cn/f/3Nc9/HMCL-3.6.11.jar";
        String destinationPath = System.getProperty("user.home") + "/Downloads/HMCL-3.6.11.jar";

        try {
            URL url = new URL(downloadUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                try (InputStream in = connection.getInputStream();
                     OutputStream out = new FileOutputStream(destinationPath)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }

                    // Launch HMCL with log window
                    launchHMCLWithLog(destinationPath);
                }
            } else {
                JOptionPane.showMessageDialog(this, "下载失败，响应码: " + responseCode, "错误", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "下载失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void launchHMCLWithLog(String filePath) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", filePath);
            processBuilder.redirectErrorStream(true); // 合并输出流和错误流
            Process process = processBuilder.start();

            // 创建日志窗口
            JFrame logFrame = new JFrame("HMCL 日志");
            logFrame.setSize(800, 600);
            logFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            JTextArea logArea = new JTextArea();
            logArea.setEditable(false);
            logFrame.add(new JScrollPane(logArea));
            logFrame.setVisible(true);

            // 读取并显示日志
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        logArea.append(line + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "启动失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void launchSelectedLauncher() {
        String selectedPath = (String) launcherComboBox.getSelectedItem();
        if (selectedPath != null) {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder(selectedPath);
                processBuilder.inheritIO();
                processBuilder.start();
                JOptionPane.showMessageDialog(this, "启动器已启动", "成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "启动失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    private Properties loadConfig() {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(configFilePath)) {
            props.load(in);
        } catch (IOException e) {
            // Ignore if file does not exist
        }
        return props;
    }

    private void saveConfig() {
        try (FileOutputStream out = new FileOutputStream(configFilePath)) {
            config.store(out, null);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "保存配置失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MinecraftLauncher launcher = new MinecraftLauncher();
            launcher.setVisible(true);
        });
    }
}