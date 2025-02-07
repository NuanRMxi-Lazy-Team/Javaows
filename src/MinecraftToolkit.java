import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Desktop;
import java.net.URI;

public class MinecraftToolkit extends JFrame {
    public MinecraftToolkit() {
        super("Minecraft Toolkit");
        setSize(600, 200); // 调整窗口大小
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create buttons
        JButton versionViewerButton = new JButton("版本查看器");
        JButton translatorAppButton = new JButton("翻译包实用程序");
        JButton minecraftLauncherButton = new JButton("启动器");
        JButton startServerButton = new JButton("服务器");
        JButton startDownloaderButton = new JButton("启动下载器（未完工）");

        // Button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10)); // 水平排列
        buttonPanel.add(versionViewerButton);
        buttonPanel.add(translatorAppButton);
        buttonPanel.add(minecraftLauncherButton);
        buttonPanel.add(startServerButton);
        buttonPanel.add(startDownloaderButton);

        // Add button panel to frame
        add(buttonPanel, BorderLayout.CENTER);

        // Add action listeners
        versionViewerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Launch Minecraft Version Viewer
                SwingUtilities.invokeLater(() -> {
                    MinecraftVersionViewerSwing viewer = new MinecraftVersionViewerSwing();
                    viewer.setVisible(true);
                });
            }
        });

        translatorAppButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Launch Translator App
                SwingUtilities.invokeLater(() -> {
                    TranslatorAppSwing app = new TranslatorAppSwing();
                    app.setVisible(true);
                });
            }
        });

        minecraftLauncherButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Launch Minecraft Launcher
                SwingUtilities.invokeLater(() -> {
                    MinecraftLauncher launcher = new MinecraftLauncher();
                    launcher.setVisible(true);
                });
            }
        });

        startServerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Launch Minecraft Server
                SwingUtilities.invokeLater(() -> {
                    StartServer startServer = new StartServer();
                    startServer.setVisible(true);
                });
            }
        });

        startDownloaderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Launch Downloader
                SwingUtilities.invokeLater(() -> {
                    Downloader downloader = new Downloader();
                    downloader.setVisible(true);
                });
            }
        });

        // Create menu bar
        JMenuBar menuBar = new JMenuBar();
        JMenu helpMenu = new JMenu("帮助");
        JMenuItem aboutItem = new JMenuItem("关于");
        JMenuItem viewSourceItem = new JMenuItem("查看源码");

        aboutItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showAboutDialog();
            }
        });

        viewSourceItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openSourceCodeLink();
            }
        });

        helpMenu.add(aboutItem);
        helpMenu.add(viewSourceItem);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);
    }

    private void showAboutDialog() {
        JOptionPane.showMessageDialog(this,
                "Minecraft Toolkit\n版本: 1.0\n作者: 辉夜星瞳\n暖日酱~ \n\n\n这是一个用于管理 Minecraft 与开发的的工具集。",
                "关于", JOptionPane.INFORMATION_MESSAGE);
    }

    private void openSourceCodeLink() {
        String url = "https://github.com/NuanRMxi-Lazy-Team/Minecraft-Toolkit";
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "无法打开链接，请检查链接的合法性和网络连接。\n链接: " + url,
                    "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MinecraftToolkit toolkit = new MinecraftToolkit();
            toolkit.setVisible(true);
        });
    }
}