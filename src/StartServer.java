import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

public class StartServer extends JFrame {
    public StartServer() {
        super("启动 Minecraft 服务器");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        JButton startServerButton = new JButton("选择并启动服务器");
        JTextArea logArea = new JTextArea();
        logArea.setEditable(false);

        JScrollPane logScrollPane = new JScrollPane(logArea);

        startServerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fileChooser.setDialogTitle("选择 Minecraft 服务器文件 (server.jar)");

                int result = fileChooser.showOpenDialog(StartServer.this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    String serverPath = selectedFile.getAbsolutePath();

                    try {
                        ProcessBuilder processBuilder = new ProcessBuilder("java", "-Xmx1024M", "-Xms1024M", "-jar", serverPath, "nogui");
                        processBuilder.redirectErrorStream(true); // 合并输出流和错误流
                        Process process = processBuilder.start();

                        // 读取并显示日志
                        new Thread(() -> {
                            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    logArea.append(line + "\n");
                                }
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }).start();
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(StartServer.this, "启动服务器失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                        ex.printStackTrace();
                    }
                }
            }
        });

        add(startServerButton, BorderLayout.NORTH);
        add(logScrollPane, BorderLayout.CENTER);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            StartServer startServer = new StartServer();
            startServer.setVisible(true);
        });
    }
}