import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class ImgViewer extends JFrame {
    private JLabel imageLabel;
    private JScrollPane scrollPane;
    private BufferedImage currentImage;
    private File[] imageFiles;
    private int currentImageIndex = -1;
    private double scaleFactor = 1.0;
    
    public ImgViewer() {
        setTitle("Java照片查看器");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        
        // 设置Metal外观
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        initComponents();
        createMenuBar();
        setupKeyBindings();
    }
    
    private void initComponents() {
        // 创建图像显示标签
        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        imageLabel.setVerticalAlignment(JLabel.CENTER);
        imageLabel.setBackground(Color.WHITE);
        imageLabel.setOpaque(true);
        imageLabel.setText("请选择图片文件");
        imageLabel.setForeground(Color.WHITE);
        imageLabel.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        
        // 创建滚动面板
        scrollPane = new JScrollPane(imageLabel);
        scrollPane.setPreferredSize(new Dimension(800, 600));
        scrollPane.getViewport().setBackground(Color.WHITE);
        
        add(scrollPane, BorderLayout.CENTER);
        
        // 创建工具栏
        JToolBar toolBar = new JToolBar();
        toolBar.setBackground(new Color(102, 102, 102));
        
        JButton openButton = new JButton("打开");
        openButton.addActionListener(e -> openFile());
        toolBar.add(openButton);
        
        toolBar.addSeparator();
        
        JButton prevButton = new JButton("上一张");
        prevButton.addActionListener(e -> showPreviousImage());
        toolBar.add(prevButton);
        
        JButton nextButton = new JButton("下一张");
        nextButton.addActionListener(e -> showNextImage());
        toolBar.add(nextButton);
        
        toolBar.addSeparator();
        
        JButton zoomInButton = new JButton("放大");
        zoomInButton.addActionListener(e -> zoomIn());
        toolBar.add(zoomInButton);
        
        JButton zoomOutButton = new JButton("缩小");
        zoomOutButton.addActionListener(e -> zoomOut());
        toolBar.add(zoomOutButton);
        
        JButton fitButton = new JButton("适应窗口");
        fitButton.addActionListener(e -> fitToWindow());
        toolBar.add(fitButton);
        
        JButton actualSizeButton = new JButton("实际大小");
        actualSizeButton.addActionListener(e -> actualSize());
        toolBar.add(actualSizeButton);
        
        add(toolBar, BorderLayout.NORTH);
        
        // 状态栏
        JLabel statusBar = new JLabel("就绪");
        statusBar.setBorder(BorderFactory.createLoweredBevelBorder());
        statusBar.setBackground(new Color(102, 102, 102));
        statusBar.setOpaque(true);
        add(statusBar, BorderLayout.SOUTH);
    }
    
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // 文件菜单
        JMenu fileMenu = new JMenu("文件");
        
        JMenuItem openItem = new JMenuItem("打开");
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
        openItem.addActionListener(e -> openFile());
        fileMenu.add(openItem);
        
        fileMenu.addSeparator();
        
        JMenuItem exitItem = new JMenuItem("退出");
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, ActionEvent.ALT_MASK));
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);
        
        // 查看菜单
        JMenu viewMenu = new JMenu("查看");
        
        JMenuItem zoomInItem = new JMenuItem("放大");
        zoomInItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, ActionEvent.CTRL_MASK));
        zoomInItem.addActionListener(e -> zoomIn());
        viewMenu.add(zoomInItem);
        
        JMenuItem zoomOutItem = new JMenuItem("缩小");
        zoomOutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, ActionEvent.CTRL_MASK));
        zoomOutItem.addActionListener(e -> zoomOut());
        viewMenu.add(zoomOutItem);
        
        JMenuItem fitItem = new JMenuItem("适应窗口");
        fitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, ActionEvent.CTRL_MASK));
        fitItem.addActionListener(e -> fitToWindow());
        viewMenu.add(fitItem);
        
        JMenuItem actualSizeItem = new JMenuItem("实际大小");
        actualSizeItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, ActionEvent.CTRL_MASK));
        actualSizeItem.addActionListener(e -> actualSize());
        viewMenu.add(actualSizeItem);
        
        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        
        setJMenuBar(menuBar);
    }
    
    private void setupKeyBindings() {
        // 键盘快捷键
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "previous");
        getRootPane().getActionMap().put("previous", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showPreviousImage();
            }
        });
        
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "next");
        getRootPane().getActionMap().put("next", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showNextImage();
            }
        });
    }
    
    private void openFile() {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
            "图片文件 (*.jpg, *.jpeg, *.png, *.bmp)", 
            "jpg", "jpeg", "png", "bmp");
        fileChooser.setFileFilter(filter);
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            loadImageFromFile(selectedFile);
            loadImagesFromDirectory(selectedFile.getParentFile());
        }
    }
    
    private void loadImageFromFile(File file) {
        try {
            currentImage = ImageIO.read(file);
            if (currentImage != null) {
                scaleFactor = 1.0;
                updateImageDisplay();
                setTitle("Metal主题照片查看器 - " + file.getName());
            } else {
                JOptionPane.showMessageDialog(this, "无法加载图片文件", "错误", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "读取图片时发生错误: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadImagesFromDirectory(File directory) {
        if (directory != null && directory.isDirectory()) {
            imageFiles = directory.listFiles(file -> {
                String name = file.getName().toLowerCase();
                return name.endsWith(".jpg") || name.endsWith(".jpeg") || 
                       name.endsWith(".png") || name.endsWith(".bmp");
            });
            
            if (imageFiles != null) {
                // 找到当前图片的索引
                String currentFileName = getTitle().substring(getTitle().lastIndexOf(" - ") + 3);
                for (int i = 0; i < imageFiles.length; i++) {
                    if (imageFiles[i].getName().equals(currentFileName)) {
                        currentImageIndex = i;
                        break;
                    }
                }
            }
        }
    }
    
    private void showPreviousImage() {
        if (imageFiles != null && currentImageIndex > 0) {
            currentImageIndex--;
            loadImageFromFile(imageFiles[currentImageIndex]);
        }
    }
    
    private void showNextImage() {
        if (imageFiles != null && currentImageIndex < imageFiles.length - 1) {
            currentImageIndex++;
            loadImageFromFile(imageFiles[currentImageIndex]);
        }
    }
    
    private void zoomIn() {
        scaleFactor *= 1.2;
        updateImageDisplay();
    }
    
    private void zoomOut() {
        scaleFactor /= 1.2;
        updateImageDisplay();
    }
    
    private void fitToWindow() {
        if (currentImage != null) {
            Dimension viewSize = scrollPane.getViewport().getSize();
            double scaleX = (double) viewSize.width / currentImage.getWidth();
            double scaleY = (double) viewSize.height / currentImage.getHeight();
            scaleFactor = Math.min(scaleX, scaleY);
            updateImageDisplay();
        }
    }
    
    private void actualSize() {
        scaleFactor = 1.0;
        updateImageDisplay();
    }
    
    private void updateImageDisplay() {
        if (currentImage != null) {
            int width = (int) (currentImage.getWidth() * scaleFactor);
            int height = (int) (currentImage.getHeight() * scaleFactor);
            
            Image scaledImage = currentImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            imageLabel.setIcon(new ImageIcon(scaledImage));
            imageLabel.setText("");
            
            imageLabel.setPreferredSize(new Dimension(width, height));
            scrollPane.revalidate();
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ImgViewer().setVisible(true);
        });
    }
}