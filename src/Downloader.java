import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
/*
    TODO:I Love NuanRMxi and SkyDynamic
    DO NOT REMOVE!
 */
public class Downloader extends JFrame {
    private JTextField urlField;
    private JButton destinationButton;
    private JComboBox<Integer> threadCountComboBox;
    private JButton downloadButton;
    private JProgressBar progressBar;
    private ExecutorService executorService;
    private String destinationPath;

    public Downloader() {
        super("文件下载器");
        setSize(600, 300);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create input fields
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(3, 2, 10, 10));
        inputPanel.add(new JLabel("文件URL:"));
        urlField = new JTextField();
        inputPanel.add(urlField);

        inputPanel.add(new JLabel("保存位置:"));
        destinationButton = new JButton("选择位置");
        destinationButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                chooseDestination();
            }
        });
        inputPanel.add(destinationButton);

        inputPanel.add(new JLabel("下载线程数:"));
        Integer[] threadCounts = {1, 2, 4, 8, 16, 32};
        threadCountComboBox = new JComboBox<>(threadCounts);
        inputPanel.add(threadCountComboBox);

        // Create download button
        downloadButton = new JButton("开始下载");
        downloadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startDownload();
            }
        });

        // Create progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);

        // Add components to frame
        add(inputPanel, BorderLayout.NORTH);
        add(downloadButton, BorderLayout.CENTER);
        add(progressBar, BorderLayout.SOUTH);
    }

    private void chooseDestination() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("选择保存位置");

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            destinationPath = fileChooser.getSelectedFile().getAbsolutePath();
        }
    }

    private void startDownload() {
        String url = urlField.getText();
        int threadCount = (int) threadCountComboBox.getSelectedItem();

        if (destinationPath == null || destinationPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请选择保存位置", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        executorService = Executors.newFixedThreadPool(threadCount);

        try {
            URL fileUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) fileUrl.openConnection();
            connection.setRequestMethod("HEAD");
            connection.connect();

            int fileSize = connection.getContentLength();
            if (fileSize == -1) {
                JOptionPane.showMessageDialog(this, "无法获取文件大小", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            progressBar.setMaximum(fileSize);
            progressBar.setValue(0);

            long partSize = fileSize / threadCount;
            AtomicLong downloaded = new AtomicLong(0);

            // 获取文件名
            String fileName = fileUrl.getPath().substring(fileUrl.getPath().lastIndexOf('/') + 1);
            File outputFile = new File(destinationPath, fileName);

            // 如果文件已存在，提示用户
            if (outputFile.exists()) {
                int result = JOptionPane.showConfirmDialog(this, "文件已存在，是否覆盖?", "提示", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.NO_OPTION) {
                    return;
                }
            }

            for (int i = 0; i < threadCount; i++) {
                long start = i * partSize;
                long end = (i == threadCount - 1) ? fileSize - 1 : start + partSize - 1;

                executorService.submit(() -> {
                    try {
                        HttpURLConnection partConnection = (HttpURLConnection) fileUrl.openConnection();
                        partConnection.setRequestMethod("GET");
                        partConnection.setRequestProperty("Range", "bytes=" + start + "-" + end);
                        partConnection.connect();

                        InputStream inputStream = partConnection.getInputStream();
                        RandomAccessFile randomAccessFile = new RandomAccessFile(outputFile, "rw");
                        randomAccessFile.seek(start);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        long totalBytesRead = 0;

                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            randomAccessFile.write(buffer, 0, bytesRead);
                            totalBytesRead += bytesRead;
                            downloaded.addAndGet(bytesRead);
                            SwingUtilities.invokeLater(() -> {
                                progressBar.setValue((int) downloaded.get());
                            });
                        }

                        randomAccessFile.close();
                        inputStream.close();
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(this, "下载失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                        e.printStackTrace();
                    }
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "下载失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Downloader downloader = new Downloader();
            downloader.setVisible(true);
        });
    }
}