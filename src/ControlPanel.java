
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class ControlPanel extends JFrame {
    // 定义壁纸显示模式枚举
    public enum WallpaperMode {
        STRETCH("拉伸"),
        FIT("适应"),
        CENTER("居中"),
        TILE("平铺");

        private final String displayName;

        WallpaperMode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private ToolLauncher mainWindow;
    private String currentWallpaperPath;
    private WallpaperMode currentMode = WallpaperMode.STRETCH;
    private BufferedImage currentWallpaper;

    public ControlPanel() {
        super("控制面板");
        // 移除错误的赋值，让 mainWindow 保持为 null
        // this.mainWindow = mainWindow;
        initializeUI();
    }

    // 添加设置主窗口的方法
    public void setMainWindow(ToolLauncher mainWindow) {
        this.mainWindow = mainWindow;
    }

    private void initializeUI() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(450, 350);
        setLocationRelativeTo(null);

        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout());

        // 创建标题面板
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel titleLabel = new JLabel("🎨 桌面个性化设置");
        titleLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        titlePanel.add(titleLabel);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        // 创建壁纸选项面板
        JPanel wallpaperPanel = createWallpaperPanel();

        // 创建按钮面板
        JPanel buttonPanel = createButtonPanel();

        mainPanel.add(titlePanel, BorderLayout.NORTH);
        mainPanel.add(wallpaperPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // 检查当前是否有壁纸
        checkCurrentWallpaper();
    }

    private JPanel createWallpaperPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("壁纸设置"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);

        // 壁纸选择按钮
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        JButton selectWallpaperBtn = new JButton("🖼️ 选择壁纸");
        selectWallpaperBtn.setPreferredSize(new Dimension(120, 30));
        selectWallpaperBtn.addActionListener(this::selectWallpaper);
        panel.add(selectWallpaperBtn, gbc);

        // 当前壁纸路径显示
        gbc.gridx = 1; gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        JLabel currentWallpaperLabel = new JLabel("未选择壁纸");
        currentWallpaperLabel.setName("currentWallpaperLabel");
        currentWallpaperLabel.setForeground(Color.GRAY);
        panel.add(currentWallpaperLabel, gbc);

        // 显示模式选择
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel modeLabel = new JLabel("显示模式:");
        modeLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        panel.add(modeLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        JComboBox<WallpaperMode> modeCombo = new JComboBox<>(WallpaperMode.values());
        modeCombo.setSelectedItem(currentMode);
        modeCombo.setName("modeCombo");
        modeCombo.addActionListener(e -> {
            currentMode = (WallpaperMode) modeCombo.getSelectedItem();
            if (currentWallpaperPath != null) {
                applyWallpaper();
            }
        });
        panel.add(modeCombo, gbc);

        // 预览面板
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        JPanel previewPanel = new JPanel();
        previewPanel.setPreferredSize(new Dimension(350, 120));
        previewPanel.setBorder(BorderFactory.createTitledBorder("预览"));
        previewPanel.setBackground(Color.WHITE);

        JLabel previewLabel = new JLabel("选择图片后显示预览", SwingConstants.CENTER);
        previewLabel.setForeground(Color.GRAY);
        previewPanel.add(previewLabel);
        previewPanel.setName("previewPanel");
        panel.add(previewPanel, gbc);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 应用按钮
        JButton applyBtn = new JButton("✅ 应用");
        applyBtn.setPreferredSize(new Dimension(80, 30));
        applyBtn.addActionListener(e -> {
            if (currentWallpaperPath != null) {
                applyWallpaper();
                JOptionPane.showMessageDialog(this, "壁纸已成功应用！", "提示", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "请先选择壁纸图片！", "提示", JOptionPane.WARNING_MESSAGE);
            }
        });

        // 恢复默认按钮
        JButton defaultBtn = new JButton("🔄 恢复默认");
        defaultBtn.setPreferredSize(new Dimension(100, 30));
        defaultBtn.addActionListener(e -> {
            restoreDefaultWallpaper();
            JOptionPane.showMessageDialog(this, "已恢复默认桌面！", "提示", JOptionPane.INFORMATION_MESSAGE);
        });

        // 关闭按钮
        JButton closeBtn = new JButton("❌ 关闭");
        closeBtn.setPreferredSize(new Dimension(80, 30));
        closeBtn.addActionListener(e -> dispose());

        panel.add(applyBtn);
        panel.add(defaultBtn);
        panel.add(closeBtn);

        return panel;
    }

    private void selectWallpaper(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择壁纸文件");
        fileChooser.setFileFilter(new FileNameExtensionFilter(
            "图片文件 (*.bmp, *.png, *.jpg, *.jpeg)",
            "bmp", "png", "jpg", "jpeg"));

        // 设置默认目录为用户图片目录
        String userHome = System.getProperty("user.home");
        File picturesDir = new File(userHome, "Pictures");
        if (picturesDir.exists()) {
            fileChooser.setCurrentDirectory(picturesDir);
        }

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            currentWallpaperPath = selectedFile.getAbsolutePath();

            // 更新当前壁纸路径显示
            JLabel currentWallpaperLabel = findComponentByName("currentWallpaperLabel");
            if (currentWallpaperLabel != null) {
                currentWallpaperLabel.setText(selectedFile.getName());
                currentWallpaperLabel.setToolTipText(currentWallpaperPath);
                currentWallpaperLabel.setForeground(Color.BLACK);
            }

            // 更新预览
            updatePreview(selectedFile);
        }
    }

    private void updatePreview(File imageFile) {
        try {
            BufferedImage image = ImageIO.read(imageFile);
            if (image != null) {
                JPanel previewPanel = findComponentByName("previewPanel");
                if (previewPanel != null) {
                    previewPanel.removeAll();

                    // 创建预览图片
                    int maxWidth = 320;
                    int maxHeight = 90;

                    int width = image.getWidth();
                    int height = image.getHeight();

                    double scale = Math.min((double) maxWidth / width, (double) maxHeight / height);
                    int newWidth = (int) (width * scale);
                    int newHeight = (int) (height * scale);

                    Image scaledImage = image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
                    JLabel imageLabel = new JLabel(new ImageIcon(scaledImage));

                    previewPanel.add(imageLabel);
                    previewPanel.revalidate();
                    previewPanel.repaint();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "预览图片失败: " + ex.getMessage(),
                "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void applyWallpaper() {
        if (currentWallpaperPath == null) {
            return;
        }

        try {
            BufferedImage image = ImageIO.read(new File(currentWallpaperPath));
            if (image == null) {
                throw new Exception("无法读取图片文件");
            }

            // 保存当前壁纸
            currentWallpaper = image;

            // 应用壁纸到桌面 - 这里使用简单的背景设置
            if (mainWindow != null) {
                applyWallpaperToDesktop(image, currentMode);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "应用壁纸失败: " + ex.getMessage(),
                "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void applyWallpaperToDesktop(BufferedImage image, WallpaperMode mode) {
        // 为桌面设置壁纸
        JDesktopPane desktop = mainWindow.getDesktopPane();
        if (desktop != null) {
            // 创建一个自定义的JDesktopPane来绘制壁纸
            desktop.setOpaque(false);

            // 设置背景绘制
            desktop.setBackground(Color.BLACK);

            // 这里可以进一步实现壁纸的绘制逻辑
            // 为了简化，我们只是显示一个消息
            System.out.println("正在应用壁纸: " + currentWallpaperPath);
            System.out.println("壁纸模式: " + mode);
        }
    }

    private void restoreDefaultWallpaper() {
        try {
            // 清除当前壁纸设置
            currentWallpaper = null;
            currentWallpaperPath = null;

            // 恢复桌面默认背景
            if (mainWindow != null) {
                JDesktopPane desktop = mainWindow.getDesktopPane();
                if (desktop != null) {
                    desktop.setOpaque(true);
                    desktop.setBackground(UIManager.getColor("desktop"));
                }
            }

            JLabel currentWallpaperLabel = findComponentByName("currentWallpaperLabel");
            if (currentWallpaperLabel != null) {
                currentWallpaperLabel.setText("未选择壁纸");
                currentWallpaperLabel.setToolTipText(null);
                currentWallpaperLabel.setForeground(Color.GRAY);
            }

            // 清除预览
            JPanel previewPanel = findComponentByName("previewPanel");
            if (previewPanel != null) {
                previewPanel.removeAll();
                JLabel previewLabel = new JLabel("选择图片后显示预览", SwingConstants.CENTER);
                previewLabel.setForeground(Color.GRAY);
                previewPanel.add(previewLabel);
                previewPanel.revalidate();
                previewPanel.repaint();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "恢复默认壁纸失败: " + ex.getMessage(),
                "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void checkCurrentWallpaper() {
        // 检查是否有当前壁纸
        if (currentWallpaper != null) {
            JLabel currentWallpaperLabel = findComponentByName("currentWallpaperLabel");
            if (currentWallpaperLabel != null) {
                currentWallpaperLabel.setText("当前已设置壁纸");
                currentWallpaperLabel.setForeground(Color.BLACK);
            }

            JComboBox<WallpaperMode> modeCombo = findComponentByName("modeCombo");
            if (modeCombo != null) {
                modeCombo.setSelectedItem(currentMode);
            }
        }
    }

    // 获取桌面面板的方法
    public JDesktopPane getDesktopPane() {
        return mainWindow != null ? mainWindow.getDesktopPane() : null;
    }

    // 获取当前壁纸的方法
    public BufferedImage getCurrentWallpaper() {
        return currentWallpaper;
    }

    // 获取当前壁纸模式的方法
    public WallpaperMode getCurrentWallpaperMode() {
        return currentMode;
    }

    @SuppressWarnings("unchecked")
    private <T extends Component> T findComponentByName(String name) {
        return (T) findComponentByName(this, name);
    }

    private Component findComponentByName(Container container, String name) {
        for (Component component : container.getComponents()) {
            if (name.equals(component.getName())) {
                return component;
            }
            if (component instanceof Container) {
                Component found = findComponentByName((Container) component, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}