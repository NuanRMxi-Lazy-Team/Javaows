import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Explorer extends JFrame {
    private JTree directoryTree;
    private JTable fileTable;
    private JList<FileItem> fileList;
    private DefaultTableModel tableModel;
    private DefaultListModel<FileItem> listModel;
    private DefaultTreeModel treeModel;
    private JLabel statusLabel;
    private JTextField pathField;
    private JButton backButton;
    private JButton forwardButton;
    private JButton upButton;
    private JButton refreshButton;
    private JButton viewModeButton;
    private JScrollPane rightScrollPane;
    private JPanel rightPanel;
    private CardLayout cardLayout;

    private Stack<String> backHistory = new Stack<>();
    private Stack<String> forwardHistory = new Stack<>();
    private String currentPath;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    // 视图模式：true=列表视图，false=图标视图
    private boolean isListView = true;

    // 文件类型图标
    private Map<String, Icon> fileIcons = new HashMap<>();
    private Map<String, Icon> largeFileIcons = new HashMap<>();

    public Explorer() {
        // 设置Metal外观
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
        }

        initializeFileIcons();
        setupUI();
        initializeDefaultPath();
    }

    private void initializeFileIcons() {
        // 初始化小图标（16x16）
        fileIcons.put("folder", createColoredIcon(Color.YELLOW, 16, 16));
        fileIcons.put("file", createColoredIcon(Color.WHITE, 16, 16));
        fileIcons.put("txt", createColoredIcon(Color.LIGHT_GRAY, 16, 16));
        fileIcons.put("java", createColoredIcon(Color.ORANGE, 16, 16));
        fileIcons.put("exe", createColoredIcon(Color.RED, 16, 16));
        fileIcons.put("pdf", createColoredIcon(Color.RED, 16, 16));
        fileIcons.put("image", createColoredIcon(Color.GREEN, 16, 16));
        fileIcons.put("doc", createColoredIcon(Color.BLUE, 16, 16));
        fileIcons.put("zip", createColoredIcon(Color.MAGENTA, 16, 16));

        // 初始化大图标（48x48）
        largeFileIcons.put("folder", createFolderIcon(48, 48));
        largeFileIcons.put("file", createFileIcon(48, 48, Color.WHITE));
        largeFileIcons.put("txt", createFileIcon(48, 48, Color.LIGHT_GRAY));
        largeFileIcons.put("java", createFileIcon(48, 48, Color.ORANGE));
        largeFileIcons.put("exe", createFileIcon(48, 48, Color.RED));
        largeFileIcons.put("pdf", createFileIcon(48, 48, Color.RED));
        largeFileIcons.put("image", createImageIcon(48, 48));
        largeFileIcons.put("doc", createFileIcon(48, 48, Color.BLUE));
        largeFileIcons.put("zip", createFileIcon(48, 48, Color.MAGENTA));
    }

    private Icon createColoredIcon(Color color, int width, int height) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                g.setColor(color);
                g.fillRect(x, y, width, height);
                g.setColor(Color.BLACK);
                g.drawRect(x, y, width, height);
            }

            @Override
            public int getIconWidth() { return width; }

            @Override
            public int getIconHeight() { return height; }
        };
    }

    private Icon createFolderIcon(int width, int height) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 文件夹背景
                g2d.setColor(new Color(255, 215, 0));
                g2d.fillRoundRect(x + 2, y + 8, width - 4, height - 10, 4, 4);

                // 文件夹标签
                g2d.setColor(new Color(255, 193, 7));
                g2d.fillRoundRect(x + 2, y + 4, width / 3, 8, 4, 4);

                // 边框
                g2d.setColor(Color.BLACK);
                g2d.drawRoundRect(x + 2, y + 8, width - 4, height - 10, 4, 4);
                g2d.drawRoundRect(x + 2, y + 4, width / 3, 8, 4, 4);

                g2d.dispose();
            }

            @Override
            public int getIconWidth() { return width; }

            @Override
            public int getIconHeight() { return height; }
        };
    }

    private Icon createFileIcon(int width, int height, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 文件背景
                g2d.setColor(color);
                g2d.fillRoundRect(x + 4, y + 4, width - 8, height - 8, 4, 4);

                // 文件边框
                g2d.setColor(Color.BLACK);
                g2d.drawRoundRect(x + 4, y + 4, width - 8, height - 8, 4, 4);

                // 文件内容线条
                g2d.setColor(Color.GRAY);
                for (int i = 0; i < 3; i++) {
                    int lineY = y + 12 + i * 8;
                    g2d.drawLine(x + 8, lineY, x + width - 8, lineY);
                }

                g2d.dispose();
            }

            @Override
            public int getIconWidth() { return width; }

            @Override
            public int getIconHeight() { return height; }
        };
    }

    private Icon createImageIcon(int width, int height) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 图片背景
                g2d.setColor(Color.WHITE);
                g2d.fillRoundRect(x + 4, y + 4, width - 8, height - 8, 4, 4);

                // 图片边框
                g2d.setColor(Color.BLACK);
                g2d.drawRoundRect(x + 4, y + 4, width - 8, height - 8, 4, 4);

                // 山和太阳图案
                g2d.setColor(Color.YELLOW);
                g2d.fillOval(x + width - 18, y + 8, 10, 10);

                g2d.setColor(Color.GREEN);
                int[] xPoints = {x + 8, x + 16, x + 24};
                int[] yPoints = {y + height - 8, y + height - 20, y + height - 8};
                g2d.fillPolygon(xPoints, yPoints, 3);

                g2d.dispose();
            }

            @Override
            public int getIconWidth() { return width; }

            @Override
            public int getIconHeight() { return height; }
        };
    }

    private void setupUI() {
        setTitle("文件资源管理器");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 创建菜单栏
        createMenuBar();

        // 创建工具栏
        createToolBar();

        // 创建主面板
        createMainPanel();

        // 创建状态栏
        createStatusBar();

        // 更新组件树UI以应用Metal外观
        SwingUtilities.updateComponentTreeUI(this);
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // 文件菜单
        JMenu fileMenu = new JMenu("文件(F)");
        fileMenu.setMnemonic('F');

        JMenuItem newFolderItem = new JMenuItem("新建文件夹");
        newFolderItem.setAccelerator(KeyStroke.getKeyStroke("ctrl shift N"));
        newFolderItem.addActionListener(e -> createNewFolder());

        JMenuItem newFileItem = new JMenuItem("新建文件");
        newFileItem.setAccelerator(KeyStroke.getKeyStroke("ctrl N"));
        newFileItem.addActionListener(e -> createNewFile());

        JMenuItem deleteItem = new JMenuItem("删除");
        deleteItem.setAccelerator(KeyStroke.getKeyStroke("DELETE"));
        deleteItem.addActionListener(e -> deleteSelectedItems());

        JMenuItem exitItem = new JMenuItem("退出");
        exitItem.addActionListener(e -> System.exit(0));

        fileMenu.add(newFolderItem);
        fileMenu.add(newFileItem);
        fileMenu.addSeparator();
        fileMenu.add(deleteItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // 编辑菜单
        JMenu editMenu = new JMenu("编辑(E)");
        editMenu.setMnemonic('E');

        JMenuItem copyItem = new JMenuItem("复制");
        copyItem.setAccelerator(KeyStroke.getKeyStroke("ctrl C"));
        copyItem.addActionListener(e -> copySelectedItems());

        JMenuItem pasteItem = new JMenuItem("粘贴");
        pasteItem.setAccelerator(KeyStroke.getKeyStroke("ctrl V"));
        pasteItem.addActionListener(e -> pasteItems());

        JMenuItem renameItem = new JMenuItem("重命名");
        renameItem.setAccelerator(KeyStroke.getKeyStroke("F2"));
        renameItem.addActionListener(e -> renameSelectedItem());

        editMenu.add(copyItem);
        editMenu.add(pasteItem);
        editMenu.addSeparator();
        editMenu.add(renameItem);

        // 查看菜单
        JMenu viewMenu = new JMenu("查看(V)");
        viewMenu.setMnemonic('V');

        JMenuItem listViewItem = new JMenuItem("列表视图");
        listViewItem.addActionListener(e -> switchToListView());

        JMenuItem iconViewItem = new JMenuItem("图标视图");
        iconViewItem.addActionListener(e -> switchToIconView());

        JMenuItem refreshItem = new JMenuItem("刷新");
        refreshItem.setAccelerator(KeyStroke.getKeyStroke("F5"));
        refreshItem.addActionListener(e -> refreshCurrentDirectory());

        JMenuItem propertiesItem = new JMenuItem("属性");
        propertiesItem.addActionListener(e -> showProperties());

        viewMenu.add(listViewItem);
        viewMenu.add(iconViewItem);
        viewMenu.addSeparator();
        viewMenu.add(refreshItem);
        viewMenu.addSeparator();
        viewMenu.add(propertiesItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);

        setJMenuBar(menuBar);
    }

    private void createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        // 导航按钮
        backButton = new JButton("←");
        backButton.setToolTipText("后退");
        backButton.addActionListener(e -> goBack());
        backButton.setEnabled(false);

        forwardButton = new JButton("→");
        forwardButton.setToolTipText("前进");
        forwardButton.addActionListener(e -> goForward());
        forwardButton.setEnabled(false);

        upButton = new JButton("↑");
        upButton.setToolTipText("向上");
        upButton.addActionListener(e -> goUp());

        refreshButton = new JButton("刷新");
        refreshButton.setToolTipText("刷新");
        refreshButton.addActionListener(e -> refreshCurrentDirectory());

        // 视图切换按钮
        viewModeButton = new JButton("图标");
        viewModeButton.setToolTipText("切换视图模式");
        viewModeButton.addActionListener(e -> toggleViewMode());

        // 地址栏
        pathField = new JTextField();
        pathField.addActionListener(e -> navigateToPath(pathField.getText()));

        toolBar.add(backButton);
        toolBar.add(forwardButton);
        toolBar.add(upButton);
        toolBar.add(refreshButton);
        toolBar.addSeparator();
        toolBar.add(viewModeButton);
        toolBar.addSeparator();
        toolBar.add(new JLabel("地址: "));
        toolBar.add(pathField);

        add(toolBar, BorderLayout.NORTH);
    }

    private void createMainPanel() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(250);

        // 创建目录树
        createDirectoryTree();
        JScrollPane treeScrollPane = new JScrollPane(directoryTree);
        treeScrollPane.setPreferredSize(new Dimension(250, 0));

        // 创建右侧面板（使用CardLayout支持两种视图）
        createRightPanel();

        splitPane.setLeftComponent(treeScrollPane);
        splitPane.setRightComponent(rightPanel);

        add(splitPane, BorderLayout.CENTER);
    }

    private void createRightPanel() {
        rightPanel = new JPanel();
        cardLayout = new CardLayout();
        rightPanel.setLayout(cardLayout);

        // 创建列表视图
        createFileTable();
        JScrollPane tableScrollPane = new JScrollPane(fileTable);
        rightPanel.add(tableScrollPane, "LIST");

        // 创建图标视图
        createFileList();
        JScrollPane listScrollPane = new JScrollPane(fileList);
        rightPanel.add(listScrollPane, "ICON");

        // 默认显示列表视图
        cardLayout.show(rightPanel, "LIST");
    }

    private void createDirectoryTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("计算机");
        treeModel = new DefaultTreeModel(root);
        directoryTree = new JTree(treeModel);
        directoryTree.setRootVisible(true);
        directoryTree.setShowsRootHandles(true);

        // 添加系统根目录
        File[] roots = File.listRoots();
        for (File rootFile : roots) {
            DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(rootFile.getAbsolutePath());
            root.add(rootNode);
            loadDirectoryIntoTree(rootNode, rootFile, 1);
        }

        // 展开根节点
        directoryTree.expandRow(0);

        // 添加选择监听器
        directoryTree.addTreeSelectionListener(e -> {
            TreePath path = e.getPath();
            if (path != null) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (!node.isRoot()) {
                    String dirPath = buildPathFromNode(node);
                    navigateToDirectory(dirPath);
                }
            }
        });

        // 添加展开监听器
        directoryTree.addTreeExpansionListener(new javax.swing.event.TreeExpansionListener() {
            @Override
            public void treeExpanded(javax.swing.event.TreeExpansionEvent event) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) event.getPath().getLastPathComponent();
                if (!node.isRoot()) {
                    String dirPath = buildPathFromNode(node);
                    File dir = new File(dirPath);
                    if (dir.exists() && dir.isDirectory()) {
                        loadDirectoryIntoTree(node, dir, 1);
                    }
                }
            }

            @Override
            public void treeCollapsed(javax.swing.event.TreeExpansionEvent event) {
                // 可以在这里添加折叠时的处理逻辑
            }
        });
    }

    private void loadDirectoryIntoTree(DefaultMutableTreeNode parentNode, File directory, int depth) {
        if (depth <= 0) return;

        // 清除现有子节点
        parentNode.removeAllChildren();

        executor.submit(() -> {
            try {
                File[] files = directory.listFiles();
                if (files != null) {
                    List<DefaultMutableTreeNode> childNodes = new ArrayList<>();

                    for (File file : files) {
                        if (file.isDirectory() && !file.isHidden()) {
                            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(file.getName());
                            childNodes.add(childNode);

                            // 递归加载子目录
                            if (depth > 1) {
                                loadDirectoryIntoTree(childNode, file, depth - 1);
                            }
                        }
                    }

                    // 在EDT中更新UI
                    SwingUtilities.invokeLater(() -> {
                        for (DefaultMutableTreeNode child : childNodes) {
                            parentNode.add(child);
                        }
                        treeModel.nodeStructureChanged(parentNode);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private String buildPathFromNode(DefaultMutableTreeNode node) {
        StringBuilder path = new StringBuilder();
        TreePath treePath = new TreePath(node.getPath());

        for (int i = 1; i < treePath.getPathCount(); i++) {
            DefaultMutableTreeNode pathNode = (DefaultMutableTreeNode) treePath.getPathComponent(i);
            if (i == 1) {
                path.append(pathNode.getUserObject().toString());
            } else {
                path.append(File.separator).append(pathNode.getUserObject().toString());
            }
        }

        return path.toString();
    }

    private void createFileTable() {
        String[] columnNames = {"名称", "类型", "大小", "修改时间"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 0) return Object.class;
                return String.class;
            }
        };

        fileTable = new JTable(tableModel);
        fileTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        fileTable.setRowHeight(20);
        fileTable.getTableHeader().setReorderingAllowed(false);

        // 设置列宽
        fileTable.getColumnModel().getColumn(0).setPreferredWidth(300);
        fileTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        fileTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        fileTable.getColumnModel().getColumn(3).setPreferredWidth(150);

        // 自定义渲染器显示图标
        fileTable.getColumnModel().getColumn(0).setCellRenderer(new FileNameCellRenderer());

        // 添加双击监听器
        fileTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openSelectedTableItem();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showTableContextMenu(e.getX(), e.getY());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showTableContextMenu(e.getX(), e.getY());
                }
            }
        });
    }

    private void createFileList() {
        listModel = new DefaultListModel<>();
        fileList = new JList<>(listModel);
        fileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        fileList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        fileList.setVisibleRowCount(-1);
        fileList.setCellRenderer(new FileIconCellRenderer());
        fileList.setFixedCellWidth(80);
        fileList.setFixedCellHeight(80);

        // 添加双击监听器
        fileList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openSelectedListItem();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showListContextMenu(e.getX(), e.getY());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showListContextMenu(e.getX(), e.getY());
                }
            }
        });
    }

    private void createStatusBar() {
        statusLabel = new JLabel("就绪");
        statusLabel.setBorder(BorderFactory.createLoweredBevelBorder());
        add(statusLabel, BorderLayout.SOUTH);
    }

    private void initializeDefaultPath() {
        String userHome = System.getProperty("user.home");
        navigateToDirectory(userHome);
    }

    private void navigateToDirectory(String path) {
        if (currentPath != null && !currentPath.equals(path)) {
            backHistory.push(currentPath);
            forwardHistory.clear();
            updateNavigationButtons();
        }

        currentPath = path;
        pathField.setText(path);
        loadDirectoryContents(path);
    }

    private void loadDirectoryContents(String path) {
        statusLabel.setText("正在加载...");
        tableModel.setRowCount(0);
        listModel.clear();

        executor.submit(() -> {
            try {
                File directory = new File(path);
                if (!directory.exists() || !directory.isDirectory()) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("无效的目录: " + path);
                    });
                    return;
                }

                File[] files = directory.listFiles();
                if (files == null) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("无法访问目录: " + path);
                    });
                    return;
                }

                // 排序：文件夹在前，然后按名称排序
                Arrays.sort(files, (f1, f2) -> {
                    if (f1.isDirectory() && !f2.isDirectory()) return -1;
                    if (!f1.isDirectory() && f2.isDirectory()) return 1;
                    return f1.getName().compareToIgnoreCase(f2.getName());
                });

                List<Object[]> tableRows = new ArrayList<>();
                List<FileItem> listItems = new ArrayList<>();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                for (File file : files) {
                    if (file.isHidden()) continue;

                    String name = file.getName();
                    String type = file.isDirectory() ? "文件夹" : getFileType(file);
                    String size = file.isDirectory() ? "" : formatFileSize(file.length());
                    String modifiedTime = dateFormat.format(new Date(file.lastModified()));

                    // 为表格视图创建数据
                    Icon smallIcon = getFileIcon(file, false);
                    FileNameWithIcon nameWithIcon = new FileNameWithIcon(name, smallIcon);
                    tableRows.add(new Object[]{nameWithIcon, type, size, modifiedTime});

                    // 为图标视图创建数据
                    Icon largeIcon = getFileIcon(file, true);
                    FileItem fileItem = new FileItem(file, name, largeIcon);
                    listItems.add(fileItem);
                }

                SwingUtilities.invokeLater(() -> {
                    // 更新表格视图
                    for (Object[] row : tableRows) {
                        tableModel.addRow(row);
                    }

                    // 更新图标视图
                    for (FileItem item : listItems) {
                        listModel.addElement(item);
                    }

                    statusLabel.setText(String.format("就绪 - %d 个项目", tableRows.size()));
                });

            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("加载失败: " + e.getMessage());
                });
            }
        });
    }

    private Icon getFileIcon(File file, boolean large) {
        Map<String, Icon> icons = large ? largeFileIcons : fileIcons;

        if (file.isDirectory()) {
            return icons.get("folder");
        }

        String extension = getFileExtension(file.getName()).toLowerCase();
        if (icons.containsKey(extension)) {
            return icons.get(extension);
        }

        if (isImageFile(extension)) {
            return icons.get("image");
        }

        if (isDocumentFile(extension)) {
            return icons.get("doc");
        }

        if (isArchiveFile(extension)) {
            return icons.get("zip");
        }

        return icons.get("file");
    }

    private String getFileType(File file) {
        if (file.isDirectory()) {
            return "文件夹";
        }

        String extension = getFileExtension(file.getName());
        if (extension.isEmpty()) {
            return "文件";
        }

        return extension.toUpperCase() + " 文件";
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex + 1) : "";
    }

    private boolean isImageFile(String extension) {
        return Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "ico", "svg", "webp").contains(extension);
    }

    private boolean isDocumentFile(String extension) {
        return Arrays.asList("doc", "docx", "xls", "xlsx", "ppt", "pptx").contains(extension);
    }

    private boolean isArchiveFile(String extension) {
        return Arrays.asList("zip", "rar", "7z", "tar", "gz", "bz2").contains(extension);
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024.0));
        return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
    }

    // 视图切换方法
    private void toggleViewMode() {
        if (isListView) {
            switchToIconView();
        } else {
            switchToListView();
        }
    }

    private void switchToListView() {
        isListView = true;
        viewModeButton.setText("图标");
        viewModeButton.setToolTipText("切换到图标视图");
        cardLayout.show(rightPanel, "LIST");
    }

    private void switchToIconView() {
        isListView = false;
        viewModeButton.setText("列表");
        viewModeButton.setToolTipText("切换到列表视图");
        cardLayout.show(rightPanel, "ICON");
    }

    // 导航方法
    private void goBack() {
        if (!backHistory.isEmpty()) {
            forwardHistory.push(currentPath);
            String previousPath = backHistory.pop();
            currentPath = previousPath;
            pathField.setText(currentPath);
            loadDirectoryContents(currentPath);
            updateNavigationButtons();
        }
    }

    private void goForward() {
        if (!forwardHistory.isEmpty()) {
            backHistory.push(currentPath);
            String nextPath = forwardHistory.pop();
            currentPath = nextPath;
            pathField.setText(currentPath);
            loadDirectoryContents(currentPath);
            updateNavigationButtons();
        }
    }

    private void goUp() {
        File current = new File(currentPath);
        File parent = current.getParentFile();
        if (parent != null) {
            navigateToDirectory(parent.getAbsolutePath());
        }
    }

    private void navigateToPath(String path) {
        File file = new File(path);
        if (file.exists()) {
            if (file.isDirectory()) {
                navigateToDirectory(path);
            } else {
                navigateToDirectory(file.getParent());
            }
        } else {
            JOptionPane.showMessageDialog(this, "路径不存在: " + path, "错误", JOptionPane.ERROR_MESSAGE);
            pathField.setText(currentPath);
        }
    }

    private void updateNavigationButtons() {
        backButton.setEnabled(!backHistory.isEmpty());
        forwardButton.setEnabled(!forwardHistory.isEmpty());
    }

    // 文件操作方法
    private void openSelectedTableItem() {
        int selectedRow = fileTable.getSelectedRow();
        if (selectedRow >= 0) {
            FileNameWithIcon nameWithIcon = (FileNameWithIcon) tableModel.getValueAt(selectedRow, 0);
            String fileName = nameWithIcon.getName();
            openFile(fileName);
        }
    }

    private void openSelectedListItem() {
        FileItem selectedItem = fileList.getSelectedValue();
        if (selectedItem != null) {
            openFile(selectedItem.getName());
        }
    }

    private void openFile(String fileName) {
        File file = new File(currentPath, fileName);

        if (file.isDirectory()) {
            navigateToDirectory(file.getAbsolutePath());
        } else {
            try {
                Desktop.getDesktop().open(file);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "无法打开文件: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void createNewFolder() {
        String folderName = JOptionPane.showInputDialog(this, "请输入文件夹名称:", "新建文件夹", JOptionPane.PLAIN_MESSAGE);
        if (folderName != null && !folderName.trim().isEmpty()) {
            File newFolder = new File(currentPath, folderName.trim());
            if (newFolder.mkdir()) {
                refreshCurrentDirectory();
                statusLabel.setText("文件夹创建成功: " + folderName);
            } else {
                JOptionPane.showMessageDialog(this, "无法创建文件夹", "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void createNewFile() {
        String fileName = JOptionPane.showInputDialog(this, "请输入文件名称:", "新建文件", JOptionPane.PLAIN_MESSAGE);
        if (fileName != null && !fileName.trim().isEmpty()) {
            File newFile = new File(currentPath, fileName.trim());
            try {
                if (newFile.createNewFile()) {
                    refreshCurrentDirectory();
                    statusLabel.setText("文件创建成功: " + fileName);
                } else {
                    JOptionPane.showMessageDialog(this, "文件已存在", "错误", JOptionPane.ERROR_MESSAGE);
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "无法创建文件: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteSelectedItems() {
        List<String> selectedFiles = new ArrayList<>();

        if (isListView) {
            int[] selectedRows = fileTable.getSelectedRows();
            for (int row : selectedRows) {
                FileNameWithIcon nameWithIcon = (FileNameWithIcon) tableModel.getValueAt(row, 0);
                selectedFiles.add(nameWithIcon.getName());
            }
        } else {
            List<FileItem> selectedItems = fileList.getSelectedValuesList();
            for (FileItem item : selectedItems) {
                selectedFiles.add(item.getName());
            }
        }

        if (selectedFiles.isEmpty()) return;

        String message = selectedFiles.size() == 1 ? "确定要删除选定的项目吗？" : "确定要删除选定的 " + selectedFiles.size() + " 个项目吗？";
        int result = JOptionPane.showConfirmDialog(this, message, "确认删除", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            for (String fileName : selectedFiles) {
                File file = new File(currentPath, fileName);

                try {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "删除失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
            refreshCurrentDirectory();
            statusLabel.setText("删除操作完成");
        }
    }

    private void deleteDirectory(File directory) throws IOException {
        Files.walkFileTree(directory.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void copySelectedItems() {
        statusLabel.setText("复制功能尚未实现");
    }

    private void pasteItems() {
        statusLabel.setText("粘贴功能尚未实现");
    }

    private void renameSelectedItem() {
        String fileName = null;

        if (isListView) {
            int selectedRow = fileTable.getSelectedRow();
            if (selectedRow >= 0) {
                FileNameWithIcon nameWithIcon = (FileNameWithIcon) tableModel.getValueAt(selectedRow, 0);
                fileName = nameWithIcon.getName();
            }
        } else {
            FileItem selectedItem = fileList.getSelectedValue();
            if (selectedItem != null) {
                fileName = selectedItem.getName();
            }
        }

        if (fileName != null) {
            String newName = JOptionPane.showInputDialog(this, "请输入新名称:", fileName);

            if (newName != null && !newName.trim().isEmpty() && !newName.equals(fileName)) {
                File oldFile = new File(currentPath, fileName);
                File newFile = new File(currentPath, newName.trim());

                if (oldFile.renameTo(newFile)) {
                    refreshCurrentDirectory();
                    statusLabel.setText("重命名成功");
                } else {
                    JOptionPane.showMessageDialog(this, "重命名失败", "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void refreshCurrentDirectory() {
        if (currentPath != null) {
            loadDirectoryContents(currentPath);
        }
    }

    private void showTableContextMenu(int x, int y) {
        showContextMenu(x, y);
    }

    private void showListContextMenu(int x, int y) {
        showContextMenu(x, y);
    }

    private void showContextMenu(int x, int y) {
        JPopupMenu contextMenu = new JPopupMenu();

        JMenuItem openItem = new JMenuItem("打开");
        openItem.addActionListener(e -> {
            if (isListView) {
                openSelectedTableItem();
            } else {
                openSelectedListItem();
            }
        });

        JMenuItem copyItem = new JMenuItem("复制");
        copyItem.addActionListener(e -> copySelectedItems());

        JMenuItem deleteItem = new JMenuItem("删除");
        deleteItem.addActionListener(e -> deleteSelectedItems());

        JMenuItem renameItem = new JMenuItem("重命名");
        renameItem.addActionListener(e -> renameSelectedItem());

        JMenuItem propertiesItem = new JMenuItem("属性");
        propertiesItem.addActionListener(e -> showProperties());

        contextMenu.add(openItem);
        contextMenu.addSeparator();
        contextMenu.add(copyItem);
        contextMenu.add(deleteItem);
        contextMenu.add(renameItem);
        contextMenu.addSeparator();
        contextMenu.add(propertiesItem);

        Component targetComponent = isListView ? fileTable : fileList;
        contextMenu.show(targetComponent, x, y);
    }

    private void showProperties() {
        String fileName = null;

        if (isListView) {
            int selectedRow = fileTable.getSelectedRow();
            if (selectedRow >= 0) {
                FileNameWithIcon nameWithIcon = (FileNameWithIcon) tableModel.getValueAt(selectedRow, 0);
                fileName = nameWithIcon.getName();
            }
        } else {
            FileItem selectedItem = fileList.getSelectedValue();
            if (selectedItem != null) {
                fileName = selectedItem.getName();
            }
        }

        if (fileName != null) {
            File file = new File(currentPath, fileName);

            StringBuilder info = new StringBuilder();
            info.append("名称: ").append(file.getName()).append("\n");
            info.append("类型: ").append(file.isDirectory() ? "文件夹" : "文件").append("\n");
            info.append("位置: ").append(file.getParent()).append("\n");
            info.append("大小: ").append(formatFileSize(file.length())).append("\n");
            info.append("修改时间: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(file.lastModified()))).append("\n");
            info.append("只读: ").append(file.canWrite() ? "否" : "是").append("\n");
            info.append("隐藏: ").append(file.isHidden() ? "是" : "否");

            JOptionPane.showMessageDialog(this, info.toString(), "属性", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // 内部类：文件项
    private static class FileItem {
        private File file;
        private String name;
        private Icon icon;

        public FileItem(File file, String name, Icon icon) {
            this.file = file;
            this.name = name;
            this.icon = icon;
        }

        public File getFile() { return file; }
        public String getName() { return name; }
        public Icon getIcon() { return icon; }

        @Override
        public String toString() { return name; }
    }

    // 内部类：文件名与图标的包装类
    private static class FileNameWithIcon {
        private String name;
        private Icon icon;

        public FileNameWithIcon(String name, Icon icon) {
            this.name = name;
            this.icon = icon;
        }

        public String getName() { return name; }
        public Icon getIcon() { return icon; }

        @Override
        public String toString() { return name; }
    }

    // 内部类：表格文件名渲染器
    private class FileNameCellRenderer extends JLabel implements TableCellRenderer {
        public FileNameCellRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof FileNameWithIcon) {
                FileNameWithIcon nameWithIcon = (FileNameWithIcon) value;
                setText(nameWithIcon.getName());
                setIcon(nameWithIcon.getIcon());
            } else {
                setText(value != null ? value.toString() : "");
                setIcon(null);
            }

            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }

            return this;
        }
    }

    // 内部类：图标视图渲染器
    private class FileIconCellRenderer extends JLabel implements ListCellRenderer<FileItem> {
        public FileIconCellRenderer() {
            setOpaque(true);
            setHorizontalAlignment(JLabel.CENTER);
            setVerticalAlignment(JLabel.CENTER);
            setHorizontalTextPosition(JLabel.CENTER);
            setVerticalTextPosition(JLabel.BOTTOM);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends FileItem> list, FileItem value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value != null) {
                setText(value.getName());
                setIcon(value.getIcon());
            } else {
                setText("");
                setIcon(null);
            }

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            return this;
        }
    }

    // 清理资源
    private void cleanup() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    @Override
    public void dispose() {
        cleanup();
        super.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Explorer().setVisible(true);
        });
    }
}