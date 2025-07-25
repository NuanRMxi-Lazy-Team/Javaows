import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class VideoPlayer extends JFrame {
    // UI组件
    private JTable playlistTable;
    private DefaultTableModel tableModel;
    private JButton playButton, pauseButton, stopButton;
    private JButton nextButton, prevButton, addButton, removeButton;
    private JButton fullscreenButton, snapshotButton;
    private JSlider progressSlider, volumeSlider;
    private JLabel currentVideoLabel, timeLabel, volumeLabel;
    private JPanel controlPanel;

    // VLCJ组件
    private EmbeddedMediaPlayerComponent mediaPlayerComponent;
    private MediaPlayer mediaPlayer;
    private MediaPlayerFactory factory;

    // 播放列表和状态
    private List<File> playlist;
    private int currentVideoIndex = -1;
    private boolean isPlaying = false;
    private boolean isPaused = false;
    private boolean isFullscreen = false;
    private boolean userIsDraggingSlider = false;

    // 全屏相关
    private JFrame fullscreenFrame;
    private Point normalLocation;
    private Dimension normalSize;

    // 定时器
    private Timer progressTimer;

    public VideoPlayer() {
        super("Javaows VLC 视频播放器");
        playlist = new ArrayList<>();

        initializeVLC();
        setupUI();
        setupEventHandlers();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
    }

    private void initializeVLC() {
        try {
            // 创建 VLCJ 组件
            mediaPlayerComponent = new EmbeddedMediaPlayerComponent();
            mediaPlayer = mediaPlayerComponent.mediaPlayer();
            factory = new MediaPlayerFactory();

            // 添加事件监听器
            mediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
                @Override
                public void playing(MediaPlayer mediaPlayer) {
                    SwingUtilities.invokeLater(() -> {
                        isPlaying = true;
                        isPaused = false;
                        playButton.setText("播放中");
                        playButton.setEnabled(false);
                        pauseButton.setEnabled(true);

                        // 开始进度更新
                        if (progressTimer != null) {
                            progressTimer.start();
                        }
                    });
                }

                @Override
                public void paused(MediaPlayer mediaPlayer) {
                    SwingUtilities.invokeLater(() -> {
                        isPaused = true;
                        isPlaying = false;
                        playButton.setText("继续");
                        playButton.setEnabled(true);
                        pauseButton.setEnabled(false);

                        if (progressTimer != null) {
                            progressTimer.stop();
                        }
                    });
                }

                @Override
                public void stopped(MediaPlayer mediaPlayer) {
                    SwingUtilities.invokeLater(() -> {
                        isPlaying = false;
                        isPaused = false;
                        playButton.setText("播放");
                        playButton.setEnabled(true);
                        pauseButton.setEnabled(false);
                        progressSlider.setValue(0);
                        timeLabel.setText("00:00 / 00:00");

                        if (progressTimer != null) {
                            progressTimer.stop();
                        }
                    });
                }

                @Override
                public void finished(MediaPlayer mediaPlayer) {
                    SwingUtilities.invokeLater(() -> {
                        // 自动播放下一个
                        nextVideo();
                    });
                }

                @Override
                public void error(MediaPlayer mediaPlayer) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(VideoPlayer.this,
                            "播放出错，请检查文件格式或文件是否损坏。",
                            "播放错误", JOptionPane.ERROR_MESSAGE);
                        stopVideo();
                    });
                }
            });

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "初始化 VLC 播放器失败！\n\n" +
                "请确保：\n" +
                "1. 已安装 VLC Media Player\n" +
                "2. VLCJ 库已正确添加到项目中\n\n" +
                "错误信息：" + e.getMessage(),
                "初始化失败", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void setupUI() {
        setLayout(new BorderLayout());

        // 创建媒体播放面板
        createMediaPanel();

        // 创建播放列表
        createPlaylistPanel();

        // 创建控制面板
        createControlPanel();

        // 创建信息面板
        createInfoPanel();

        // 创建按钮面板
        createButtonPanel();

        // 初始化进度定时器
        progressTimer = new Timer(1000, e -> updateProgress());
    }

    private void createMediaPanel() {
        // 视频显示区域
        mediaPlayerComponent.videoSurfaceComponent().setPreferredSize(new Dimension(800, 450));

        // 添加双击全屏功能
        mediaPlayerComponent.videoSurfaceComponent().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    toggleFullscreen();
                }
            }
        });

        // 创建分割面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent(mediaPlayerComponent);
        splitPane.setDividerLocation(500);
        splitPane.setResizeWeight(0.7);

        add(splitPane, BorderLayout.CENTER);
    }

    private void createPlaylistPanel() {
        // 播放列表表格
        String[] columnNames = {"文件名", "路径", "格式", "分辨率", "时长"};
        tableModel = new DefaultTableModel(columnNames, 0);
        playlistTable = new JTable(tableModel);
        playlistTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(playlistTable);
        scrollPane.setPreferredSize(new Dimension(1200, 250));

        // 获取分割面板并设置底部组件
        JSplitPane splitPane = (JSplitPane) getContentPane().getComponent(0);
        splitPane.setBottomComponent(scrollPane);
    }

    private void createControlPanel() {
        controlPanel = new JPanel(new BorderLayout());

        // 播放控制按钮
        JPanel buttonPanel = new JPanel(new FlowLayout());

        prevButton = new JButton("上一个");
        playButton = new JButton("播放");
        pauseButton = new JButton("暂停");
        stopButton = new JButton("停止");
        nextButton = new JButton("下一个");
        fullscreenButton = new JButton("全屏");

        pauseButton.setEnabled(false);

        buttonPanel.add(prevButton);
        buttonPanel.add(playButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(nextButton);
        buttonPanel.add(fullscreenButton);

        // 进度条
        progressSlider = new JSlider(0, 100, 0);
        progressSlider.setToolTipText("拖动调整播放进度");

        // 音量控制
        JPanel volumePanel = new JPanel(new FlowLayout());
        volumeLabel = new JLabel("音量: 80%");
        volumeSlider = new JSlider(0, 100, 80);
        volumeSlider.setPreferredSize(new Dimension(120, 20));
        volumeSlider.setToolTipText("调整音量");

        volumePanel.add(volumeLabel);
        volumePanel.add(volumeSlider);

        controlPanel.add(buttonPanel, BorderLayout.NORTH);
        controlPanel.add(progressSlider, BorderLayout.CENTER);
        controlPanel.add(volumePanel, BorderLayout.SOUTH);

        add(controlPanel, BorderLayout.SOUTH);
    }

    private void createInfoPanel() {
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        currentVideoLabel = new JLabel("当前播放: 无");
        timeLabel = new JLabel("00:00 / 00:00");

        infoPanel.add(currentVideoLabel);
        infoPanel.add(Box.createHorizontalStrut(20));
        infoPanel.add(timeLabel);

        add(infoPanel, BorderLayout.NORTH);
    }

    private void createButtonPanel() {
        JPanel buttonPanel = new JPanel(new GridLayout(6, 1, 5, 5));

        addButton = new JButton("添加视频");
        removeButton = new JButton("移除视频");
        snapshotButton = new JButton("截图");
        JButton clearButton = new JButton("清空列表");
        JButton infoButton = new JButton("视频信息");
        JButton aboutButton = new JButton("关于");

        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(snapshotButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(infoButton);
        buttonPanel.add(aboutButton);

        // 按钮事件
        clearButton.addActionListener(e -> clearPlaylist());
        infoButton.addActionListener(e -> showVideoInfo());
        aboutButton.addActionListener(e -> showAbout());

        add(buttonPanel, BorderLayout.EAST);
    }

    private void setupEventHandlers() {
        // 播放按钮
        playButton.addActionListener(e -> {
            if (isPaused) {
                resumeVideo();
            } else if (currentVideoIndex >= 0 && currentVideoIndex < playlist.size()) {
                playVideo(currentVideoIndex);
            } else if (!playlist.isEmpty()) {
                playVideo(0);
            }
        });

        // 暂停按钮
        pauseButton.addActionListener(e -> pauseVideo());

        // 停止按钮
        stopButton.addActionListener(e -> stopVideo());

        // 上一个/下一个按钮
        prevButton.addActionListener(e -> previousVideo());
        nextButton.addActionListener(e -> nextVideo());

        // 全屏按钮
        fullscreenButton.addActionListener(e -> toggleFullscreen());

        // 截图按钮
        snapshotButton.addActionListener(e -> takeSnapshot());

        // 添加/移除视频按钮
        addButton.addActionListener(e -> addVideos());
        removeButton.addActionListener(e -> removeVideo());

        // 播放列表双击事件
        playlistTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int selectedRow = playlistTable.getSelectedRow();
                    if (selectedRow >= 0) {
                        playVideo(selectedRow);
                    }
                }
            }
        });

        // 进度条事件
        progressSlider.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                userIsDraggingSlider = true;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                userIsDraggingSlider = false;
                if (isPlaying && mediaPlayer.status().length() > 0) {
                    float position = progressSlider.getValue() / 100.0f;
                    mediaPlayer.controls().setPosition(position);
                }
            }
        });

        // 音量滑动条事件
        volumeSlider.addChangeListener(e -> {
            int volume = volumeSlider.getValue();
            volumeLabel.setText("音量: " + volume + "%");
            mediaPlayer.audio().setVolume(volume);
        });

        // 键盘快捷键
        setupKeyboardShortcuts();

        // 窗口关闭事件
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanup();
                System.exit(0);
            }
        });
    }

    private void setupKeyboardShortcuts() {
        // 空格键暂停/播放
        getRootPane().registerKeyboardAction(
            e -> {
                if (isPlaying) {
                    pauseVideo();
                } else if (isPaused) {
                    resumeVideo();
                }
            },
            KeyStroke.getKeyStroke("SPACE"),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        // ESC退出全屏
        getRootPane().registerKeyboardAction(
            e -> {
                if (isFullscreen) {
                    exitFullscreen();
                }
            },
            KeyStroke.getKeyStroke("ESCAPE"),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        // F键切换全屏
        getRootPane().registerKeyboardAction(
            e -> toggleFullscreen(),
            KeyStroke.getKeyStroke("F"),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        // 左右箭头键快进快退
        getRootPane().registerKeyboardAction(
            e -> {
                if (isPlaying) {
                    long currentTime = mediaPlayer.status().time();
                    mediaPlayer.controls().setTime(Math.max(0, currentTime - 10000)); // 快退10秒
                }
            },
            KeyStroke.getKeyStroke("LEFT"),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        getRootPane().registerKeyboardAction(
            e -> {
                if (isPlaying) {
                    long currentTime = mediaPlayer.status().time();
                    long totalTime = mediaPlayer.status().length();
                    mediaPlayer.controls().setTime(Math.min(totalTime, currentTime + 10000)); // 快进10秒
                }
            },
            KeyStroke.getKeyStroke("RIGHT"),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    private void addVideos() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("视频文件",
            "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "3gp",
            "ts", "mpg", "mpeg", "asf", "rm", "rmvb"));
        fileChooser.setMultiSelectionEnabled(true);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] files = fileChooser.getSelectedFiles();
            for (File file : files) {
                playlist.add(file);

                // 获取视频信息（这里简化处理，实际可以通过VLCJ获取更详细信息）
                String format = getFileFormat(file);
                String fileName = file.getName();
                String path = file.getAbsolutePath();

                tableModel.addRow(new Object[]{fileName, path, format, "未知", "未知"});
            }
        }
    }

    private String getFileFormat(File file) {
        String name = file.getName().toLowerCase();
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0) {
            return name.substring(lastDot + 1).toUpperCase();
        }
        return "未知";
    }

    private void removeVideo() {
        int selectedRow = playlistTable.getSelectedRow();
        if (selectedRow >= 0) {
            if (selectedRow == currentVideoIndex) {
                stopVideo();
                currentVideoIndex = -1;
            } else if (selectedRow < currentVideoIndex) {
                currentVideoIndex--;
            }

            playlist.remove(selectedRow);
            tableModel.removeRow(selectedRow);
        }
    }

    private void clearPlaylist() {
        stopVideo();
        playlist.clear();
        tableModel.setRowCount(0);
        currentVideoIndex = -1;
        currentVideoLabel.setText("当前播放: 无");
    }

    private void playVideo(int index) {
        if (index < 0 || index >= playlist.size()) return;

        File videoFile = playlist.get(index);
        String media = videoFile.getAbsolutePath();

        // 停止当前播放
        if (isPlaying) {
            mediaPlayer.controls().stop();
        }

        // 播放新视频
        boolean success = mediaPlayer.media().play(media);

        if (success) {
            currentVideoIndex = index;
            currentVideoLabel.setText("当前播放: " + videoFile.getName());
            playlistTable.setRowSelectionInterval(index, index);

            // 设置音量
            mediaPlayer.audio().setVolume(volumeSlider.getValue());
        } else {
            JOptionPane.showMessageDialog(this,
                "无法播放视频文件：" + videoFile.getName(),
                "播放错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void pauseVideo() {
        if (isPlaying) {
            mediaPlayer.controls().pause();
        }
    }

    private void resumeVideo() {
        if (isPaused) {
            mediaPlayer.controls().play();
        }
    }

    private void stopVideo() {
        mediaPlayer.controls().stop();
    }

    private void nextVideo() {
        if (!playlist.isEmpty()) {
            int nextIndex = (currentVideoIndex + 1) % playlist.size();
            playVideo(nextIndex);
        }
    }

    private void previousVideo() {
        if (!playlist.isEmpty()) {
            int prevIndex = (currentVideoIndex - 1 + playlist.size()) % playlist.size();
            playVideo(prevIndex);
        }
    }

    private void toggleFullscreen() {
        if (!isFullscreen) {
            enterFullscreen();
        } else {
            exitFullscreen();
        }
    }

    private void enterFullscreen() {
        // 保存当前窗口状态
        normalLocation = getLocation();
        normalSize = getSize();

        // 创建全屏窗口
        fullscreenFrame = new JFrame();
        fullscreenFrame.setUndecorated(true);
        fullscreenFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        fullscreenFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        // 移动媒体播放器组件到全屏窗口
        remove(((JSplitPane) getContentPane().getComponent(0)));
        fullscreenFrame.add(mediaPlayerComponent, BorderLayout.CENTER);

        // 全屏窗口事件
        fullscreenFrame.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    exitFullscreen();
                }
            }
        });

        fullscreenFrame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    exitFullscreen();
                }
            }
        });

        fullscreenFrame.setVisible(true);
        fullscreenFrame.requestFocus();

        setVisible(false);
        isFullscreen = true;
        fullscreenButton.setText("退出全屏");
    }

    private void exitFullscreen() {
        if (fullscreenFrame != null) {
            // 移动媒体播放器组件回主窗口
            fullscreenFrame.remove(mediaPlayerComponent);

            JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
            splitPane.setTopComponent(mediaPlayerComponent);
            splitPane.setBottomComponent(new JScrollPane(playlistTable));
            splitPane.setDividerLocation(500);
            splitPane.setResizeWeight(0.7);

            add(splitPane, BorderLayout.CENTER);

            fullscreenFrame.dispose();
            fullscreenFrame = null;
        }

        // 恢复主窗口
        setSize(normalSize);
        setLocation(normalLocation);
        setVisible(true);
        requestFocus();

        isFullscreen = false;
        fullscreenButton.setText("全屏");
    }

    private void takeSnapshot() {
        if (isPlaying && currentVideoIndex >= 0) {
            try {
                File videoFile = playlist.get(currentVideoIndex);
                String baseName = videoFile.getName().replaceFirst("[.][^.]+$", "");
                String snapshotPath = videoFile.getParent() + File.separator +
                    baseName + "_snapshot_" + System.currentTimeMillis() + ".png";

                boolean success = mediaPlayer.snapshots().save(new File(snapshotPath));

                if (success) {
                    JOptionPane.showMessageDialog(this,
                        "截图已保存到：\n" + snapshotPath,
                        "截图成功", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this,
                        "截图保存失败！",
                        "错误", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                    "截图失败：" + e.getMessage(),
                    "错误", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this,
                "请先播放一个视频文件！",
                "提示", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void showVideoInfo() {
        int selectedRow = playlistTable.getSelectedRow();
        if (selectedRow >= 0) {
            File file = playlist.get(selectedRow);

            StringBuilder info = new StringBuilder();
            info.append("文件名: ").append(file.getName()).append("\n");
            info.append("路径: ").append(file.getAbsolutePath()).append("\n");
            info.append("大小: ").append(formatFileSize(file.length())).append("\n");
            info.append("格式: ").append(getFileFormat(file)).append("\n");

            if (selectedRow == currentVideoIndex && isPlaying) {
                info.append("\n当前播放信息:\n");
                info.append("播放时间: ").append(formatTime(mediaPlayer.status().time())).append("\n");
                info.append("总时长: ").append(formatTime(mediaPlayer.status().length())).append("\n");
                info.append("音量: ").append(mediaPlayer.audio().volume()).append("%\n");
            }

            JOptionPane.showMessageDialog(this, info.toString(),
                "视频信息 - " + file.getName(), JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void showAbout() {
        String about = "Javaows VLC 视频播放器 v1.0\n\n" +
                      "基于 VLCJ 库开发的多媒体播放器\n" +
                      "支持大部分视频格式播放\n\n" +
                      "快捷键说明：\n" +
                      "空格键 - 播放/暂停\n" +
                      "F键 - 切换全屏\n" +
                      "ESC键 - 退出全屏\n" +
                      "左箭头 - 快退10秒\n" +
                      "右箭头 - 快进10秒\n\n" +
                      "双击视频画面可切换全屏模式";

        JOptionPane.showMessageDialog(this, about, "关于", JOptionPane.INFORMATION_MESSAGE);
    }

    private void updateProgress() {
        if (isPlaying && !userIsDraggingSlider) {
            long length = mediaPlayer.status().length();
            long time = mediaPlayer.status().time();

            if (length > 0) {
                int progress = (int) ((time * 100) / length);
                progressSlider.setValue(progress);

                String currentTime = formatTime(time);
                String totalTime = formatTime(length);
                timeLabel.setText(currentTime + " / " + totalTime);
            }
        }
    }

    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        minutes = minutes % 60;
        seconds = seconds % 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private void cleanup() {
        if (progressTimer != null) {
            progressTimer.stop();
        }

        if (mediaPlayer != null) {
            mediaPlayer.controls().stop();
            mediaPlayer.release();
        }

        if (factory != null) {
            factory.release();
        }

        if (fullscreenFrame != null) {
            fullscreenFrame.dispose();
        }
    }

    public static void main(String[] args) {
        // 设置系统外观
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            new VideoPlayer().setVisible(true);
        });
    }
}