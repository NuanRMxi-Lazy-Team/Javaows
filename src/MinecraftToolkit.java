import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class MinecraftToolkit extends JFrame {
    public MinecraftToolkit() {
        super("Minecraft Toolkit");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // 关闭主窗口时只关闭主窗口
        setLayout(new BorderLayout());

        // Create buttons
        JButton versionViewerButton = new JButton("启动 Minecraft 版本查看器");
        JButton translatorAppButton = new JButton("启动 翻译包实用程序");
        JButton minecraftLauncherButton = new JButton("启动 Minecraft 启动器");

        // Button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(3, 1, 10, 10));
        buttonPanel.add(versionViewerButton);
        buttonPanel.add(translatorAppButton);
        buttonPanel.add(minecraftLauncherButton);

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
                new MinecraftLauncher().launch();
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MinecraftToolkit toolkit = new MinecraftToolkit();
            toolkit.setVisible(true);
        });
    }
}