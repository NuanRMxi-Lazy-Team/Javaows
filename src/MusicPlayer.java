
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MusicPlayer extends JFrame {
    private JTable playlistTable;
    private DefaultTableModel tableModel;
    private JButton playButton;
    private JButton pauseButton;
    private JButton stopButton;
    private JButton nextButton;
    private JButton prevButton;
    private JButton addButton;
    private JButton removeButton;
    private JSlider progressSlider;
    private JSlider volumeSlider;
    private JLabel currentSongLabel;
    private JLabel timeLabel;
    private JLabel volumeLabel;

    private List<File> playlist;
    private int currentSongIndex = -1;
    private Process ffmpegProcess;
    private AtomicBoolean isPlaying = new AtomicBoolean(false);
    private AtomicBoolean isPaused = new AtomicBoolean(false);
    private Timer progressTimer;
    private long currentPosition = 0; // 当前播放位置（秒）
    private long totalDuration = 0; // 总时长（秒）
    private int currentVolume = 80; // 当前音量（0-100）

    public MusicPlayer() {
        super("Java音乐播放器");
        playlist = new ArrayList<>();
        initializeUI();
        setupEventHandlers();
        checkFFmpegAvailability();
    }

    private void checkFFmpegAvailability() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                showFFmpegError();
            }
        } catch (Exception e) {
            showFFmpegError();
        }
    }

    private void showFFmpegError() {
        JOptionPane.showMessageDialog(this,
            "未找到 FFmpeg！\n\n" +
            "请确保：\n" +
            "1. 已安装 FFmpeg\n" +
            "2. FFmpeg 已添加到系统 PATH 环境变量\n" +
            "3. 可以在命令行中运行 'ffmpeg' 命令\n\n" +
            "下载地址：https://ffmpeg.org/download.html",
            "FFmpeg 未找到", JOptionPane.WARNING_MESSAGE);
    }

    private void initializeUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 650);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // 创建播放列表表格
        tableModel = new DefaultTableModel(new String[]{"文件名", "路径", "格式", "时长"}, 0);
        playlistTable = new JTable(tableModel);
        playlistTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(playlistTable);
        scrollPane.setPreferredSize(new Dimension(900, 300));

        // 创建控制面板
        JPanel controlPanel = createControlPanel();

        // 创建信息面板
        JPanel infoPanel = createInfoPanel();

        // 创建按钮面板
        JPanel buttonPanel = createButtonPanel();

        // 布局
        add(scrollPane, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);
        add(infoPanel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.EAST);

        // 初始化进度定时器
        progressTimer = new Timer(1000, e -> updateProgress());
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // 播放控制按钮
        JPanel buttonPanel = new JPanel(new FlowLayout());
        playButton = new JButton("播放");
        pauseButton = new JButton("暂停");
        stopButton = new JButton("停止");
        nextButton = new JButton("下一首");
        prevButton = new JButton("上一首");

        buttonPanel.add(prevButton);
        buttonPanel.add(playButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(nextButton);

        // 初始化进度条
        progressSlider = new JSlider(0, 100, 0);
        progressSlider.setToolTipText("拖动调整播放进度");

        // 音量控制
        JPanel volumePanel = new JPanel(new FlowLayout());
        volumeLabel = new JLabel("音量:");
        volumeSlider = new JSlider(0, 100, 80);
        volumeSlider.setPreferredSize(new Dimension(100, 20));
        volumeSlider.setToolTipText("调整音量");
        volumePanel.add(volumeLabel);
        volumePanel.add(volumeSlider);

        panel.add(buttonPanel, BorderLayout.NORTH);
        panel.add(progressSlider, BorderLayout.CENTER);
        panel.add(volumePanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createInfoPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        currentSongLabel = new JLabel("当前播放: 无");
        timeLabel = new JLabel("00:00 / 00:00");

        panel.add(currentSongLabel);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(timeLabel);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 5, 5));

        addButton = new JButton("添加音乐");
        removeButton = new JButton("移除音乐");
        JButton clearButton = new JButton("清空列表");
        JButton infoButton = new JButton("文件信息");

        panel.add(addButton);
        panel.add(removeButton);
        panel.add(clearButton);
        panel.add(infoButton);

        clearButton.addActionListener(e -> clearPlaylist());
        infoButton.addActionListener(e -> showFileInfo());

        return panel;
    }

    private void setupEventHandlers() {
        // 播放按钮
        playButton.addActionListener(e -> {
            if (currentSongIndex >= 0 && currentSongIndex < playlist.size()) {
                if (isPaused.get()) {
                    resumeMusic();
                } else {
                    playMusic(currentSongIndex);
                }
            } else if (!playlist.isEmpty()) {
                playMusic(0);
            }
        });

        // 暂停按钮
        pauseButton.addActionListener(e -> {
            if (isPlaying.get()) {
                pauseMusic();
            }
        });

        // 停止按钮
        stopButton.addActionListener(e -> stopMusic());

        // 下一首按钮
        nextButton.addActionListener(e -> nextSong());

        // 上一首按钮
        prevButton.addActionListener(e -> previousSong());

        // 添加音乐按钮
        addButton.addActionListener(e -> addMusic());

        // 移除音乐按钮
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

        // 进度条拖动事件
        progressSlider.addChangeListener(e -> {
            if (progressSlider.getValueIsAdjusting() && isPlaying.get()) {
                seekToPosition(progressSlider.getValue());
            }
        });

        // 音量滑动条事件
        volumeSlider.addChangeListener(e -> {
            currentVolume = volumeSlider.getValue();
            volumeLabel.setText("音量: " + currentVolume + "%");
            // 注意：FFmpeg 播放时音量调整需要重新启动播放
        });
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
                String duration = getAudioDuration(file);
                tableModel.addRow(new Object[]{file.getName(), file.getAbsolutePath(), format, duration});
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
            ProcessBuilder pb = new ProcessBuilder("ffprobe", "-v", "quiet", "-show_entries",
                "format=duration", "-of", "csv=p=0", file.getAbsolutePath());
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            process.waitFor();

            if (line != null && !line.trim().isEmpty()) {
                double seconds = Double.parseDouble(line.trim());
                return formatTime((long)(seconds * 1000));
            }
        } catch (Exception e) {
            // 获取时长失败，返回未知
        }
        return "未知";
    }

    private void showFileInfo() {
        int selectedRow = playlistTable.getSelectedRow();
        if (selectedRow >= 0) {
            File file = playlist.get(selectedRow);
            getDetailedFileInfo(file);
        }
    }

    private void getDetailedFileInfo(File file) {
        try {
            ProcessBuilder pb = new ProcessBuilder("ffprobe", "-v", "quiet", "-print_format", "json",
                "-show_format", "-show_streams", file.getAbsolutePath());
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder info = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                info.append(line).append("\n");
            }
            process.waitFor();

            // 简化显示
            JTextArea textArea = new JTextArea(info.toString());
            textArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(600, 400));

            JOptionPane.showMessageDialog(this, scrollPane, "文件信息 - " + file.getName(),
                JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "获取文件信息失败: " + e.getMessage(),
                "错误", JOptionPane.ERROR_MESSAGE);
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

        stopMusic(); // 先停止当前播放

        File musicFile = playlist.get(index);
        String format = getAudioFormat(musicFile);

        try {
            // 获取音频时长
            totalDuration = getAudioDurationInSeconds(musicFile);
            currentPosition = 0;

            // 构建 FFmpeg 命令
            List<String> command = new ArrayList<>();
            command.add("ffplay");
            command.add("-nodisp"); // 不显示视频窗口
            command.add("-autoexit"); // 播放完自动退出
            command.add("-volume");
            command.add(String.valueOf(currentVolume)); // 设置音量
            command.add(musicFile.getAbsolutePath());

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            ffmpegProcess = pb.start();

            // 监控播放进程
            new Thread(() -> {
                try {
                    int exitCode = ffmpegProcess.waitFor();
                    if (exitCode == 0 && isPlaying.get()) {
                        // 播放完成，自动下一首
                        SwingUtilities.invokeLater(() -> nextSong());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();

            isPlaying.set(true);
            isPaused.set(false);
            currentSongIndex = index;

            currentSongLabel.setText("当前播放: " + musicFile.getName() + " (" + format + ")");
            playlistTable.setRowSelectionInterval(index, index);

            progressTimer.start();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "播放音乐失败: " + e.getMessage(),
                "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private long getAudioDurationInSeconds(File file) {
        try {
            ProcessBuilder pb = new ProcessBuilder("ffprobe", "-v", "quiet", "-show_entries",
                "format=duration", "-of", "csv=p=0", file.getAbsolutePath());
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            process.waitFor();

            if (line != null && !line.trim().isEmpty()) {
                return (long) Double.parseDouble(line.trim());
            }
        } catch (Exception e) {
            // 获取时长失败
        }
        return 0;
    }

    private void pauseMusic() {
        if (ffmpegProcess != null && isPlaying.get()) {
            // FFplay 不支持暂停，这里实现的是停止功能
            // 真正的暂停需要使用 FFmpeg 的其他方式或者记录当前位置
            stopMusic();
            isPaused.set(true);
            isPlaying.set(false);
            // 注意：这里暂停后恢复播放会从头开始，要实现真正的暂停需要更复杂的逻辑
        }
    }

    private void resumeMusic() {
        if (isPaused.get() && currentSongIndex >= 0) {
            // 由于 FFplay 限制，恢复播放会从头开始
            playMusic(currentSongIndex);
            isPaused.set(false);
        }
    }

    private void stopMusic() {
        if (ffmpegProcess != null) {
            ffmpegProcess.destroy();
            try {
                ffmpegProcess.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            ffmpegProcess = null;
        }
        isPlaying.set(false);
        isPaused.set(false);
        progressTimer.stop();
        currentPosition = 0;
        progressSlider.setValue(0);
        timeLabel.setText("00:00 / 00:00");
    }

    private void seekToPosition(int percentage) {
        if (totalDuration > 0) {
            long seekPosition = totalDuration * percentage / 100;
            // 由于 FFplay 限制，这里重新开始播放并跳转到指定位置
            if (isPlaying.get() && currentSongIndex >= 0) {
                stopMusic();
                playMusicFromPosition(currentSongIndex, seekPosition);
            }
        }
    }

    private void playMusicFromPosition(int index, long startSeconds) {
        if (index < 0 || index >= playlist.size()) return;

        File musicFile = playlist.get(index);

        try {
            // 构建 FFmpeg 命令，从指定位置开始播放
            List<String> command = new ArrayList<>();
            command.add("ffplay");
            command.add("-nodisp");
            command.add("-autoexit");
            command.add("-ss"); // 跳转到指定时间
            command.add(String.valueOf(startSeconds));
            command.add("-volume");
            command.add(String.valueOf(currentVolume));
            command.add(musicFile.getAbsolutePath());

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            ffmpegProcess = pb.start();

            currentPosition = startSeconds;
            isPlaying.set(true);
            isPaused.set(false);

            // 监控播放进程
            new Thread(() -> {
                try {
                    int exitCode = ffmpegProcess.waitFor();
                    if (exitCode == 0 && isPlaying.get()) {
                        SwingUtilities.invokeLater(() -> nextSong());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();

            if (!progressTimer.isRunning()) {
                progressTimer.start();
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "跳转播放失败: " + e.getMessage(),
                "错误", JOptionPane.ERROR_MESSAGE);
        }
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

    private void updateProgress() {
        if (isPlaying.get() && totalDuration > 0) {
            currentPosition++;

            int progress = (int) (currentPosition * 100 / totalDuration);
            progressSlider.setValue(progress);

            String current = formatTime(currentPosition * 1000);
            String total = formatTime(totalDuration * 1000);

            timeLabel.setText(current + " / " + total);

            // 如果播放完成
            if (currentPosition >= totalDuration) {
                nextSong();
            }
        }
    }
    
    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MusicPlayer player = new MusicPlayer();
            player.setVisible(true);
            player.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        });
    }
}