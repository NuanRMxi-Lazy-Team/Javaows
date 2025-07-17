import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class WebBrowser extends JFrame {

    // 主要组件
    private JTabbedPane tabbedPane;
    private List<BrowserTab> tabs;
    private JToolBar toolBar;
    private JTextField addressBar;
    private JButton backButton;
    private JButton forwardButton;
    private JButton refreshButton;
    private JButton homeButton;
    private JButton newTabButton;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private HistoryManager historyManager;
    private DownloadManager downloadManager;

    // 默认主页
    private static final String DEFAULT_HOME = "https://www.google.com";

    public WebBrowser() {
        super("Web Browser");

        this.tabs = new ArrayList<>();
        this.historyManager = new HistoryManager();
        this.downloadManager = new DownloadManager();

        initializeComponents();
        setupEventHandlers();
        setupMenuBar();
        createNewTab(DEFAULT_HOME);

        // 窗口设置
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initializeComponents() {
        setLayout(new BorderLayout());

        // 创建工具栏
        createToolBar();

        // 创建标签页容器
        tabbedPane = new JTabbedPane();
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        // 创建状态栏
        JPanel statusBar = createStatusBar();

        // 布局
        add(toolBar, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
    }

    private void createToolBar() {
        toolBar = new JToolBar();
        toolBar.setFloatable(false);

        // 导航按钮
        backButton = new JButton("←");
        backButton.setToolTipText("后退");
        backButton.setEnabled(false);

        forwardButton = new JButton("→");
        forwardButton.setToolTipText("前进");
        forwardButton.setEnabled(false);

        refreshButton = new JButton("⟳");
        refreshButton.setToolTipText("刷新");

        homeButton = new JButton("🏠");
        homeButton.setToolTipText("主页");

        // 地址栏
        addressBar = new JTextField();
        addressBar.setToolTipText("输入网址或搜索内容...");

        // 新建标签页按钮
        newTabButton = new JButton("+");
        newTabButton.setToolTipText("新建标签页");

        // 添加组件到工具栏
        toolBar.add(backButton);
        toolBar.add(forwardButton);
        toolBar.add(refreshButton);
        toolBar.add(homeButton);
        toolBar.addSeparator();
        toolBar.add(new JLabel("地址: "));
        toolBar.add(addressBar);
        toolBar.addSeparator();
        toolBar.add(newTabButton);
    }

    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEtchedBorder());

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);

        statusLabel = new JLabel("准备就绪");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        statusBar.add(progressBar, BorderLayout.WEST);
        statusBar.add(statusLabel, BorderLayout.CENTER);

        return statusBar;
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // 文件菜单
        JMenu fileMenu = new JMenu("文件");

        JMenuItem newTabItem = new JMenuItem("新建标签页");
        newTabItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
        newTabItem.addActionListener(e -> createNewTab(DEFAULT_HOME));

        JMenuItem closeTabItem = new JMenuItem("关闭标签页");
        closeTabItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK));
        closeTabItem.addActionListener(e -> closeCurrentTab());

        JMenuItem exitItem = new JMenuItem("退出");
        exitItem.addActionListener(e -> System.exit(0));

        fileMenu.add(newTabItem);
        fileMenu.add(closeTabItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // 编辑菜单
        JMenu editMenu = new JMenu("编辑");

        JMenuItem findItem = new JMenuItem("查找");
        findItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK));
        findItem.addActionListener(e -> {
            if (getCurrentTab() != null) {
                getCurrentTab().showFindDialog();
            }
        });

        editMenu.add(findItem);

        // 视图菜单
        JMenu viewMenu = new JMenu("视图");

        JMenuItem historyItem = new JMenuItem("历史记录");
        historyItem.addActionListener(e -> showHistory());

        JMenuItem downloadItem = new JMenuItem("下载管理");
        downloadItem.addActionListener(e -> showDownloads());

        viewMenu.add(historyItem);
        viewMenu.add(downloadItem);

        // 帮助菜单
        JMenu helpMenu = new JMenu("帮助");

        JMenuItem aboutItem = new JMenuItem("关于");
        aboutItem.addActionListener(e -> showAbout());

        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    private void setupEventHandlers() {
        // 地址栏事件
        addressBar.addActionListener(e -> navigateToUrl());

        // 导航按钮事件
        backButton.addActionListener(e -> {
            if (getCurrentTab() != null) getCurrentTab().goBack();
        });

        forwardButton.addActionListener(e -> {
            if (getCurrentTab() != null) getCurrentTab().goForward();
        });

        refreshButton.addActionListener(e -> {
            if (getCurrentTab() != null) getCurrentTab().refresh();
        });

        homeButton.addActionListener(e -> {
            if (getCurrentTab() != null) getCurrentTab().navigateTo(DEFAULT_HOME);
        });

        // 新建标签页事件
        newTabButton.addActionListener(e -> createNewTab(DEFAULT_HOME));

        // 标签页切换事件
        tabbedPane.addChangeListener(e -> updateToolBarState());

        // 快捷键
        setupKeyboardShortcuts();
    }

    private void setupKeyboardShortcuts() {
        // 地址栏快捷键
        KeyStroke ctrlL = KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ctrlL, "focusAddressBar");
        getRootPane().getActionMap().put("focusAddressBar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addressBar.requestFocus();
                addressBar.selectAll();
            }
        });

        // 刷新快捷键
        KeyStroke ctrlR = KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ctrlR, "refresh");
        getRootPane().getActionMap().put("refresh", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (getCurrentTab() != null) getCurrentTab().refresh();
            }
        });
    }

    public void createNewTab(String url) {
        BrowserTab browserTab = new BrowserTab(url, this);
        tabs.add(browserTab);

        tabbedPane.addTab(browserTab.getTitle(), browserTab.getComponent());
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);

        // 添加右键菜单到标签页
        int tabIndex = tabbedPane.getTabCount() - 1;
        JPopupMenu tabPopup = new JPopupMenu();
        JMenuItem closeTab = new JMenuItem("关闭标签页");
        closeTab.addActionListener(e -> closeTab(tabIndex));
        tabPopup.add(closeTab);

        updateToolBarState();
    }

    private void closeCurrentTab() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        if (selectedIndex >= 0 && tabs.size() > 1) {
            closeTab(selectedIndex);
        }
    }

    private void closeTab(int index) {
        if (index >= 0 && index < tabs.size()) {
            tabs.get(index).dispose();
            tabs.remove(index);
            tabbedPane.removeTabAt(index);

            if (tabs.isEmpty()) {
                System.exit(0);
            }
        }
    }

    private BrowserTab getCurrentTab() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        return selectedIndex >= 0 && selectedIndex < tabs.size() ? tabs.get(selectedIndex) : null;
    }

    private void navigateToUrl() {
        BrowserTab currentTab = getCurrentTab();
        if (currentTab != null) {
            String url = addressBar.getText().trim();
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                if (url.contains(".")) {
                    url = "https://" + url;
                } else {
                    url = "https://www.google.com/search?q=" + url;
                }
            }
            currentTab.navigateTo(url);
        }
    }

    public void updateToolBarState() {
        BrowserTab currentTab = getCurrentTab();
        if (currentTab != null) {
            backButton.setEnabled(currentTab.canGoBack());
            forwardButton.setEnabled(currentTab.canGoForward());
            addressBar.setText(currentTab.getCurrentUrl());
        }
    }

    public void updateProgress(int progress) {
        SwingUtilities.invokeLater(() -> {
            if (progress < 0) {
                progressBar.setVisible(false);
            } else {
                progressBar.setVisible(true);
                progressBar.setValue(progress);
            }
        });
    }

    public void updateStatus(String status) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(status));
    }

    public void updateTabTitle(BrowserTab tab, String title) {
        int index = tabs.indexOf(tab);
        if (index >= 0) {
            tabbedPane.setTitleAt(index, title);
        }
    }

    // 菜单功能实现
    private void showHistory() {
        JDialog dialog = new JDialog(this, "历史记录", true);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);

        JList<HistoryManager.HistoryItem> historyList = new JList<>(historyManager.getHistory().toArray(new HistoryManager.HistoryItem[0]));
        historyList.setCellRenderer(new HistoryListCellRenderer());

        historyList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    HistoryManager.HistoryItem selected = historyList.getSelectedValue();
                    if (selected != null) {
                        createNewTab(selected.getUrl());
                        dialog.dispose();
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(historyList);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton clearButton = new JButton("清空历史");
        clearButton.addActionListener(e -> {
            historyManager.clearHistory();
            historyList.setListData(new HistoryManager.HistoryItem[0]);
        });
        buttonPanel.add(clearButton);

        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void showDownloads() {
        JDialog dialog = new JDialog(this, "下载管理", true);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);

        JList<DownloadManager.DownloadItem> downloadList = new JList<>(downloadManager.getDownloads().toArray(new DownloadManager.DownloadItem[0]));
        downloadList.setCellRenderer(new DownloadListCellRenderer());

        JScrollPane scrollPane = new JScrollPane(downloadList);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton clearButton = new JButton("清空下载");
        clearButton.addActionListener(e -> {
            downloadManager.clearDownloads();
            downloadList.setListData(new DownloadManager.DownloadItem[0]);
        });
        buttonPanel.add(clearButton);

        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(this,
            "Web Browser\n" +
            "版本: 1.0\n" +
            "基于Java Swing的简单网页浏览器\n\n" +
            "功能特性:\n" +
            "• 多标签页浏览\n" +
            "• 地址栏导航\n" +
            "• 历史记录管理\n" +
            "• 下载管理\n" +
            "• 页面内搜索\n" +
            "• 键盘快捷键支持",
            "关于", JOptionPane.INFORMATION_MESSAGE);
    }

    public HistoryManager getHistoryManager() { return historyManager; }
    public DownloadManager getDownloadManager() { return downloadManager; }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new WebBrowser();
        });
    }

    // 内部类：浏览器标签页
    private class BrowserTab {
        private JPanel component;
        private JEditorPane editorPane;
        private String currentUrl;
        private String title;
        private WebBrowser browserWindow;
        private List<String> history;
        private int historyIndex;
        private FindDialog findDialog;

        public BrowserTab(String initialUrl, WebBrowser browserWindow) {
            this.browserWindow = browserWindow;
            this.title = "新标签页";
            this.history = new ArrayList<>();
            this.historyIndex = -1;

            initializeComponents();
            setupEventHandlers();
            navigateTo(initialUrl);
        }

        private void initializeComponents() {
            component = new JPanel(new BorderLayout());

            editorPane = new JEditorPane();
            editorPane.setEditable(false);
            editorPane.setContentType("text/html");

            JScrollPane scrollPane = new JScrollPane(editorPane);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

            component.add(scrollPane, BorderLayout.CENTER);
        }

        private void setupEventHandlers() {
            editorPane.addHyperlinkListener(new HyperlinkListener() {
                @Override
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        if (e instanceof HTMLFrameHyperlinkEvent) {
                            HTMLFrameHyperlinkEvent frameEvent = (HTMLFrameHyperlinkEvent) e;
                            HTMLDocument doc = (HTMLDocument) editorPane.getDocument();
                            doc.processHTMLFrameHyperlinkEvent(frameEvent);
                        } else {
                            navigateTo(e.getURL().toString());
                        }
                    }
                }
            });
        }

        public void navigateTo(String url) {
            if (url == null || url.trim().isEmpty()) return;

            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    browserWindow.updateStatus("正在加载: " + url);
                    browserWindow.updateProgress(0);

                    try {
                        URL pageUrl = new URL(url);
                        URLConnection connection = pageUrl.openConnection();
                        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

                        // 模拟加载进度
                        for (int i = 0; i <= 100; i += 10) {
                            browserWindow.updateProgress(i);
                            Thread.sleep(50);
                        }

                        SwingUtilities.invokeLater(() -> {
                            try {
                                editorPane.setPage(pageUrl);
                                currentUrl = url;

                                // 更新历史记录
                                if (historyIndex < history.size() - 1) {
                                    history.subList(historyIndex + 1, history.size()).clear();
                                }
                                history.add(url);
                                historyIndex = history.size() - 1;

                                // 更新标题
                                String pageTitle = getPageTitle(url);
                                setTitle(pageTitle);

                                // 添加到全局历史记录
                                browserWindow.getHistoryManager().addToHistory(url, pageTitle);

                                browserWindow.updateToolBarState();
                                browserWindow.updateStatus("完成");
                            } catch (Exception e) {
                                browserWindow.updateStatus("加载失败: " + e.getMessage());
                                editorPane.setText("<html><body><h2>加载失败</h2><p>无法加载页面: " + url + "</p><p>错误: " + e.getMessage() + "</p></body></html>");
                            }
                        });
                    } catch (Exception e) {
                        SwingUtilities.invokeLater(() -> {
                            browserWindow.updateStatus("加载失败: " + e.getMessage());
                            editorPane.setText("<html><body><h2>加载失败</h2><p>无法加载页面: " + url + "</p><p>错误: " + e.getMessage() + "</p></body></html>");
                        });
                    } finally {
                        browserWindow.updateProgress(-1);
                    }

                    return null;
                }
            };

            worker.execute();
        }

        private String getPageTitle(String url) {
            try {
                // 从URL中提取简单的标题
                String title = url;
                if (title.startsWith("http://")) {
                    title = title.substring(7);
                } else if (title.startsWith("https://")) {
                    title = title.substring(8);
                }

                if (title.startsWith("www.")) {
                    title = title.substring(4);
                }

                int slashIndex = title.indexOf('/');
                if (slashIndex > 0) {
                    title = title.substring(0, slashIndex);
                }

                return title;
            } catch (Exception e) {
                return "网页";
            }
        }

        public void goBack() {
            if (canGoBack()) {
                historyIndex--;
                currentUrl = history.get(historyIndex);
                navigateTo(currentUrl);
            }
        }

        public void goForward() {
            if (canGoForward()) {
                historyIndex++;
                currentUrl = history.get(historyIndex);
                navigateTo(currentUrl);
            }
        }

        public void refresh() {
            if (currentUrl != null) {
                navigateTo(currentUrl);
            }
        }

        public boolean canGoBack() {
            return historyIndex > 0;
        }

        public boolean canGoForward() {
            return historyIndex < history.size() - 1;
        }

        public String getCurrentUrl() {
            return currentUrl != null ? currentUrl : "";
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title != null ? title : "新标签页";
            browserWindow.updateTabTitle(this, this.title);
        }

        public JPanel getComponent() {
            return component;
        }

        public void showFindDialog() {
            if (findDialog == null) {
                findDialog = new FindDialog(browserWindow, this);
            }
            findDialog.setVisible(true);
        }

        public void findText(String text) {
            // 简单的文本搜索实现
            String content = editorPane.getText();
            if (content.toLowerCase().contains(text.toLowerCase())) {
                browserWindow.updateStatus("找到匹配项");
                // 这里可以添加更复杂的搜索和高亮功能
            } else {
                browserWindow.updateStatus("未找到匹配项");
            }
        }

        public void dispose() {
            if (findDialog != null) {
                findDialog.dispose();
            }
        }
    }

    // 内部类：查找对话框
    private class FindDialog extends JDialog {
        private JTextField searchField;
        private BrowserTab browserTab;

        public FindDialog(WebBrowser parent, BrowserTab browserTab) {
            super(parent, "查找", true);
            this.browserTab = browserTab;

            setSize(350, 120);
            setLocationRelativeTo(parent);
            setResizable(false);

            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            searchField = new JTextField();
            searchField.addActionListener(e -> findText());

            JPanel buttonPanel = new JPanel(new FlowLayout());
            JButton findButton = new JButton("查找");
            JButton closeButton = new JButton("关闭");

            findButton.addActionListener(e -> findText());
            closeButton.addActionListener(e -> setVisible(false));

            buttonPanel.add(findButton);
            buttonPanel.add(closeButton);

            panel.add(new JLabel("查找:"), BorderLayout.WEST);
            panel.add(searchField, BorderLayout.CENTER);
            panel.add(buttonPanel, BorderLayout.SOUTH);

            add(panel);
        }

        private void findText() {
            String text = searchField.getText().trim();
            if (!text.isEmpty()) {
                browserTab.findText(text);
            }
        }

        @Override
        public void setVisible(boolean visible) {
            super.setVisible(visible);
            if (visible) {
                searchField.requestFocus();
                searchField.selectAll();
            }
        }
    }

    // 内部类：历史记录管理器
    private class HistoryManager {
        private List<HistoryItem> history;

        public HistoryManager() {
            this.history = new ArrayList<>();
        }

        public void addToHistory(String url, String title) {
            HistoryItem item = new HistoryItem(url, title, LocalDateTime.now());
            history.add(0, item);

            if (history.size() > 1000) {
                history.remove(history.size() - 1);
            }
        }

        public List<HistoryItem> getHistory() {
            return new ArrayList<>(history);
        }

        public void clearHistory() {
            history.clear();
        }

        public class HistoryItem {
            private String url;
            private String title;
            private LocalDateTime visitTime;

            public HistoryItem(String url, String title, LocalDateTime visitTime) {
                this.url = url;
                this.title = title != null ? title : "无标题";
                this.visitTime = visitTime;
            }

            public String getUrl() { return url; }
            public String getTitle() { return title; }
            public LocalDateTime getVisitTime() { return visitTime; }

            @Override
            public String toString() {
                return title + " - " + url + " (" + visitTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + ")";
            }
        }
    }

    // 内部类：下载管理器
    private class DownloadManager {
        private List<DownloadItem> downloads;

        public DownloadManager() {
            this.downloads = new ArrayList<>();
        }

        public void addDownload(String url, String filePath) {
            DownloadItem item = new DownloadItem(url, filePath, LocalDateTime.now());
            downloads.add(0, item);
        }

        public List<DownloadItem> getDownloads() {
            return new ArrayList<>(downloads);
        }

        public void clearDownloads() {
            downloads.clear();
        }

        public class DownloadItem {
            private String url;
            private String filePath;
            private LocalDateTime downloadTime;
            private String status;

            public DownloadItem(String url, String filePath, LocalDateTime downloadTime) {
                this.url = url;
                this.filePath = filePath;
                this.downloadTime = downloadTime;
                this.status = "已完成";
            }

            public String getUrl() { return url; }
            public String getFilePath() { return filePath; }
            public LocalDateTime getDownloadTime() { return downloadTime; }
            public String getStatus() { return status; }
            public void setStatus(String status) { this.status = status; }

            @Override
            public String toString() {
                return filePath + " - " + status + " (" + downloadTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + ")";
            }
        }
    }

    // 历史记录列表渲染器
    private class HistoryListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof HistoryManager.HistoryItem) {
                HistoryManager.HistoryItem item = (HistoryManager.HistoryItem) value;
                setText(item.toString());
                setIcon(new ImageIcon()); // 可以添加网页图标
            }

            return this;
        }
    }

    // 下载列表渲染器
    private class DownloadListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof DownloadManager.DownloadItem) {
                DownloadManager.DownloadItem item = (DownloadManager.DownloadItem) value;
                setText(item.toString());
                setIcon(new ImageIcon()); // 可以添加文件图标
            }

            return this;
        }
    }
}