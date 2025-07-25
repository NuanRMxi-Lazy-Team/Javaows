import uk.co.caprica.vlcj.factory.EqualizerApi;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.AudioApi;
import uk.co.caprica.vlcj.player.base.Equalizer;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class MusicPlayer extends JFrame {
    // UI组件
    private JTable playlistTable;
    private DefaultTableModel tableModel;
    private JButton playButton, pauseButton, stopButton;
    private JButton nextButton, prevButton, addButton, removeButton;
    private JSlider progressSlider, volumeSlider;
    private JLabel currentSongLabel, timeLabel, volumeLabel;

    // VLCJ组件
    private AudioPlayerComponent mediaPlayerComponent;
    private MediaPlayer mediaPlayer;
    private MediaPlayerFactory factory;

    // 播放列表和状态
    private List<File> playlist;
    private int currentSongIndex = -1;
    private boolean isPlaying = false;
    private boolean isPaused = false;
    private boolean userIsDraggingSlider = false;

    // 定时器
    private Timer progressTimer;

    public MusicPlayer() {
        super("Javaows VLC 音乐播放器");
        playlist = new ArrayList<>();

        initializeVLC();
        initializeUI();
        setupEventHandlers();
        setupKeyboardShortcuts();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 650);
        setLocationRelativeTo(null);
    }

    private void initializeVLC() {
        try {
            // 创建VLCJ音频播放组件
            mediaPlayerComponent = new AudioPlayerComponent();
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

                        if (progressTimer != null) {
                            progressTimer.start();
                        }

                        updateCurrentSongInfo();
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
                        // 自动播放下一首
                        nextSong();
                    });
                }

                @Override
                public void error(MediaPlayer mediaPlayer) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(MusicPlayer.this,
                            "播放出错，请检查文件格式或文件是否损坏。",
                            "播放错误", JOptionPane.ERROR_MESSAGE);
                        stopMusic();
                    });
                }
            });

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "初始化 VLC 音乐播放器失败！\n\n" +
                "请确保：\n" +
                "1. 已安装 VLC Media Player\n" +
                "2. VLCJ 库已正确添加到项目中\n\n" +
                "错误信息：" + e.getMessage(),
                "初始化失败", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        // 创建播放列表表格
        createPlaylistTable();

        // 创建控制面板
        createControlPanel();

        // 创建信息面板
        createInfoPanel();

        // 创建按钮面板
        createButtonPanel();

        // 初始化进度定时器
        progressTimer = new Timer(1000, e -> updateProgress());
    }

    private void createPlaylistTable() {
        tableModel = new DefaultTableModel(new String[]{"文件名", "路径", "格式", "时长"}, 0);
        playlistTable = new JTable(tableModel);
        playlistTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(playlistTable);
        scrollPane.setPreferredSize(new Dimension(900, 300));

        add(scrollPane, BorderLayout.CENTER);
    }

    private void createControlPanel() {
        JPanel controlPanel = new JPanel(new BorderLayout());

        // 播放控制按钮
        JPanel buttonPanel = new JPanel(new FlowLayout());
        prevButton = new JButton("上一首");
        playButton = new JButton("播放");
        pauseButton = new JButton("暂停");
        stopButton = new JButton("停止");
        nextButton = new JButton("下一首");

        pauseButton.setEnabled(false);

        buttonPanel.add(prevButton);
        buttonPanel.add(playButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(nextButton);

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
        currentSongLabel = new JLabel("当前播放: 无");
        timeLabel = new JLabel("00:00 / 00:00");

        infoPanel.add(currentSongLabel);
        infoPanel.add(Box.createHorizontalStrut(20));
        infoPanel.add(timeLabel);

        add(infoPanel, BorderLayout.NORTH);
    }

    private void createButtonPanel() {
        JPanel buttonPanel = new JPanel(new GridLayout(6, 1, 5, 5));

        addButton = new JButton("添加音乐");
        removeButton = new JButton("移除音乐");
        JButton clearButton = new JButton("清空列表");
        JButton infoButton = new JButton("音乐信息");
        JButton equalizer = new JButton("均衡器");
        JButton aboutButton = new JButton("关于");

        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(infoButton);
        buttonPanel.add(equalizer);
        buttonPanel.add(aboutButton);

        // 按钮事件
        clearButton.addActionListener(e -> clearPlaylist());
        infoButton.addActionListener(e -> showMusicInfo());
        equalizer.addActionListener(e -> showEqualizer());
        aboutButton.addActionListener(e -> showAbout());

        add(buttonPanel, BorderLayout.EAST);
    }

    private void setupEventHandlers() {
        // 播放按钮
        playButton.addActionListener(e -> {
            if (isPaused) {
                resumeMusic();
            } else if (currentSongIndex >= 0 && currentSongIndex < playlist.size()) {
                playMusic(currentSongIndex);
            } else if (!playlist.isEmpty()) {
                playMusic(0);
            }
        });

        // 暂停按钮
        pauseButton.addActionListener(e -> pauseMusic());

        // 停止按钮
        stopButton.addActionListener(e -> stopMusic());

        // 上一首/下一首按钮
        prevButton.addActionListener(e -> previousSong());
        nextButton.addActionListener(e -> nextSong());

        // 添加/移除音乐按钮
        addButton.addActionListener(e -> addMusic());
        removeButton.addActionListener(e -> removeMusic());

        // 播放列表双击事件
        playlistTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    int selectedRow = playlistTable.getSelectedRow();
                    if (selectedRow >= 0) {
                        playMusic(selectedRow);
                    }
                }
            }
        });

        // 进度条事件
        progressSlider.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                userIsDraggingSlider = true;
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
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
            if (mediaPlayer != null) {
                mediaPlayer.audio().setVolume(volume);
            }
        });

        // 窗口关闭事件
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
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
                    pauseMusic();
                } else if (isPaused) {
                    resumeMusic();
                }
            },
            KeyStroke.getKeyStroke("SPACE"),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        // Ctrl+O 添加文件
        getRootPane().registerKeyboardAction(
            e -> addMusic(),
            KeyStroke.getKeyStroke("ctrl O"),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        // Delete键删除选中项
        getRootPane().registerKeyboardAction(
            e -> removeMusic(),
            KeyStroke.getKeyStroke("DELETE"),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    private void addMusic() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("音频文件",
            "mp3", "ogg", "flac", "aac", "m4a", "wma", "wav", "au", "aiff", "opus"));
        fileChooser.setMultiSelectionEnabled(true);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] files = fileChooser.getSelectedFiles();
            for (File file : files) {
                playlist.add(file);
                String format = getAudioFormat(file);
                String duration = "加载中...";

                tableModel.addRow(new Object[]{file.getName(), file.getAbsolutePath(), format, duration});

                // 在后台获取音频时长
                int rowIndex = playlist.size() - 1;
                SwingUtilities.invokeLater(() -> {
                    String actualDuration = getAudioDuration(file);
                    tableModel.setValueAt(actualDuration, rowIndex, 3);
                });
            }
        }
    }

    private String getAudioFormat(File file) {
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".mp3")) return "MP3";
        if (fileName.endsWith(".ogg")) return "OGG";
        if (fileName.endsWith(".flac")) return "FLAC";
        if (fileName.endsWith(".aac")) return "AAC";
        if (fileName.endsWith(".m4a")) return "M4A";
        if (fileName.endsWith(".wma")) return "WMA";
        if (fileName.endsWith(".wav")) return "WAV";
        if (fileName.endsWith(".au")) return "AU";
        if (fileName.endsWith(".aiff")) return "AIFF";
        if (fileName.endsWith(".opus")) return "OPUS";
        return "未知";
    }

    private String getAudioDuration(File file) {
    try {
        // 使用临时播放器获取时长
        MediaPlayerFactory tempFactory = new MediaPlayerFactory();
        MediaPlayer tempPlayer = tempFactory.mediaPlayers().newMediaPlayer();

        tempPlayer.media().startPaused(file.getAbsolutePath());
        tempPlayer.media().parsing().parse(); // 显式解析

        long duration = tempPlayer.media().info().duration();
        tempPlayer.release();
        tempFactory.release();

        return formatTime(duration);
    } catch (Exception e) {
        return "未知";
    }
}

    private void removeMusic() {
        int selectedRow = playlistTable.getSelectedRow();
        if (selectedRow >= 0) {
            // 如果删除的是当前播放的歌曲，停止播放
            if (selectedRow == currentSongIndex) {
                stopMusic();
                currentSongIndex = -1;
            } else if (selectedRow < currentSongIndex) {
                currentSongIndex--;
            }

            playlist.remove(selectedRow);
            tableModel.removeRow(selectedRow);
        }
    }

    private void clearPlaylist() {
        stopMusic();
        playlist.clear();
        tableModel.setRowCount(0);
        currentSongIndex = -1;
        currentSongLabel.setText("当前播放: 无");
    }

    private void playMusic(int index) {
        if (index < 0 || index >= playlist.size()) return;

        File musicFile = playlist.get(index);
        String media = musicFile.getAbsolutePath();

        // 停止当前播放
        if (isPlaying) {
            mediaPlayer.controls().stop();
        }

        // 播放新音乐
        boolean success = mediaPlayer.media().play(media);

        if (success) {
            currentSongIndex = index;
            playlistTable.setRowSelectionInterval(index, index);

            // 设置音量
            mediaPlayer.audio().setVolume(volumeSlider.getValue());
        } else {
            JOptionPane.showMessageDialog(this,
                "无法播放音频文件：" + musicFile.getName(),
                "播放错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void pauseMusic() {
        if (isPlaying) {
            mediaPlayer.controls().pause();
        }
    }

    private void resumeMusic() {
        if (isPaused) {
            mediaPlayer.controls().play();
        }
    }

    private void stopMusic() {
        mediaPlayer.controls().stop();
    }

    private void nextSong() {
        if (!playlist.isEmpty()) {
            int nextIndex = (currentSongIndex + 1) % playlist.size();
            playMusic(nextIndex);
        }
    }

    private void previousSong() {
        if (!playlist.isEmpty()) {
            int prevIndex = (currentSongIndex - 1 + playlist.size()) % playlist.size();
            playMusic(prevIndex);
        }
    }

    private void updateCurrentSongInfo() {
        if (currentSongIndex >= 0 && currentSongIndex < playlist.size()) {
            File currentFile = playlist.get(currentSongIndex);
            String format = getAudioFormat(currentFile);
            currentSongLabel.setText("当前播放: " + currentFile.getName() + " (" + format + ")");
        }
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

    private void showMusicInfo() {
    int selectedRow = playlistTable.getSelectedRow();
    if (selectedRow < 0) return;

    File file = playlist.get(selectedRow);
    StringBuilder info = new StringBuilder();
    info.append("文件名: ").append(file.getName()).append("\n");
    info.append("路径: ").append(file.getAbsolutePath()).append("\n");
    info.append("大小: ").append(formatFileSize(file.length())).append("\n");
    info.append("格式: ").append(getAudioFormat(file)).append("\n");

    if (selectedRow == currentSongIndex && isPlaying) {
        info.append("\n当前播放信息:\n");
        info.append("播放时间: ").append(formatTime(mediaPlayer.status().time())).append("\n");
        info.append("总时长: ").append(formatTime(mediaPlayer.status().length())).append("\n");
        info.append("音量: ").append(mediaPlayer.audio().volume()).append("%\n");
    }

    JOptionPane.showMessageDialog(this, info.toString(),
        "音乐信息 - " + file.getName(), JOptionPane.INFORMATION_MESSAGE);
}
  private void showEqualizer() {
    // 1. 预设数据
    final String[] PRESET_NAMES = {"默认", "流行", "摇滚", "爵士"};
    final float[][] PRESET_VALUES = {
        {0, 1, 1.5f, 1, 0.5f, 0, 0.5f, 1, 1.5f, 1}, // 默认
        {1, 2, 1.5f, 0, -1, -1, 0, 1, 2, 3},        // 流行
        {4, 3, 2, 1, 0, 0, 1, 2, 3, 2},              // 摇滚
        {2, 1, 0, 1, 2, 2, 1, 0, 1, 2}               // 爵士
    };

    // 2. 创建均衡器
    var eqApi = mediaPlayer.audio();
    var eq = factory.equalizer().newEqualizer();   // ← 正确工厂方法
    int bands = eq.bandCount();                    // 通常为 10

    // 3. 界面
    JPanel main = new JPanel(new BorderLayout(5, 5));

    JComboBox<String> cbPreset = new JComboBox<>(PRESET_NAMES);
    JPanel sliderPanel = new JPanel(new GridLayout(bands, 1, 0, 3));
    JSlider[] sliders = new JSlider[bands];

    for (int i = 0; i < bands; i++) {
        sliders[i] = new JSlider(-20, 20, 0);
        sliders[i].setPaintLabels(true);
        sliders[i].setPaintTicks(true);
        sliders[i].setMajorTickSpacing(10);
        int idx = i;
        sliders[i].addChangeListener(e -> {
            eq.setAmp(idx, sliders[idx].getValue());
            eqApi.setEqualizer(eq);
        });
        sliderPanel.add(sliders[i]);
    }

    // 预设切换
    cbPreset.addActionListener(e -> {
        int p = cbPreset.getSelectedIndex();
        for (int i = 0; i < bands; i++) {
            sliders[i].setValue((int) PRESET_VALUES[p][i]);
        }
    });

    // 开关
    JCheckBox chkEnable = new JCheckBox("启用均衡器", eqApi.equalizer() != null);
    chkEnable.addActionListener(e ->
            eqApi.setEqualizer(chkEnable.isSelected() ? eq : null));

    JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
    top.add(new JLabel("预设："));
    top.add(cbPreset);
    top.add(chkEnable);

    main.add(top, BorderLayout.NORTH);
    main.add(sliderPanel, BorderLayout.CENTER);

    // 4. 首次加载默认预设
    cbPreset.setSelectedIndex(0);

    // 5. 对话框
    JDialog dlg = new JDialog(this, "均衡器", true);
    dlg.setContentPane(main);
    dlg.setSize(400, 500);
    dlg.setLocationRelativeTo(this);
    dlg.setVisible(true);
}
    private void showAbout() {
        String about = "Javaows VLC 音乐播放器 v1.0\n\n" +
                      "基于 VLCJ 库开发的音频播放器\n" +
                      "支持多种音频格式播放\n\n" +
                      "快捷键说明：\n" +
                      "空格键 - 播放/暂停\n" +
                      "Ctrl+O - 添加音乐文件\n" +
                      "Delete - 删除选中项目\n\n" +
                      "功能特性：\n" +
                      "• 播放列表管理\n" +
                      "• 音量控制\n" +
                      "• 进度控制\n" +
                      "• 均衡器支持\n" +
                      "• 多种音频格式支持";

        JOptionPane.showMessageDialog(this, about, "关于", JOptionPane.INFORMATION_MESSAGE);
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
    }

    public static void main(String[] args) {
        // 设置系统外观
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            new MusicPlayer().setVisible(true);
        });
    }
}