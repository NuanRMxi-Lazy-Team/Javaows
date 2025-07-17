import org.json.JSONObject;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class APIDataFetcher extends JFrame {
    private JLabel aLabel;
    private JProgressBar aProgressBar;
    private JLabel cLabel;
    private JProgressBar cProgressBar;
    private JLabel statusLabel;
    private JLabel winnerLabel;
    private JLabel rewardNameLabel;
    private JLabel rewardImg0Label;
    private JLabel rewardVariant0Label;
    private JLabel rewardImg1Label;
    private JLabel rewardVariant1Label;
    private JButton fetchButton;
    private Timer timer;

    public APIDataFetcher() {
        // 设置窗口标题和布局
        setTitle("比赛数据获取器");
        setPreferredSize(new Dimension(1280, 720)); // 设置窗口大小以适应16:9比例
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 创建顶部面板用于显示进度条
        JPanel topPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        aLabel = new JLabel("Arcaea的进度: 0%");
        aProgressBar = new JProgressBar(0, 100);
        aProgressBar.setValue(0);
        aProgressBar.setStringPainted(true);
        cLabel = new JLabel("CHUNITHM的进度: 0%");
        cProgressBar = new JProgressBar(0, 100);
        cProgressBar.setValue(0);
        cProgressBar.setStringPainted(true);
        topPanel.add(aLabel);
        topPanel.add(aProgressBar);
        topPanel.add(cLabel);
        topPanel.add(cProgressBar);

        // 创建中间面板用于显示奖励信息
        JPanel middlePanel = new JPanel(new GridLayout(6, 2, 10, 10));
        winnerLabel = new JLabel("Winner: Unknown");
        rewardNameLabel = new JLabel("Reward Name: Unknown");
        rewardImg0Label = new JLabel();
        rewardVariant0Label = new JLabel("Reward Variant 0: Unknown");
        rewardImg1Label = new JLabel();
        rewardVariant1Label = new JLabel("Reward Variant 1: Unknown");
        middlePanel.add(new JLabel("Winner:"));
        middlePanel.add(winnerLabel);
        middlePanel.add(new JLabel("Reward Name:"));
        middlePanel.add(rewardNameLabel);
        middlePanel.add(new JLabel("Reward Image 0:"));
        middlePanel.add(rewardImg0Label);
        middlePanel.add(new JLabel("Reward Variant 0:"));
        middlePanel.add(rewardVariant0Label);
        middlePanel.add(new JLabel("Reward Image 1:"));
        middlePanel.add(rewardImg1Label);
        middlePanel.add(new JLabel("Reward Variant 1:"));
        middlePanel.add(rewardVariant1Label);

        // 创建底部面板用于显示状态和按钮
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        statusLabel = new JLabel("状态: 仍在进行");
        fetchButton = new JButton("手动刷新");
        fetchButton.addActionListener(this::fetchData);
        bottomPanel.add(statusLabel);
        bottomPanel.add(fetchButton);

        // 添加面板到窗口
        add(topPanel, BorderLayout.NORTH);
        add(middlePanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);


        // 显示窗口
        pack(); // 自动调整窗口大小以适应组件
        setLocationRelativeTo(null); // 窗口居中
        setVisible(true);
    }



    private void fetchData(ActionEvent actionEvent) {
        try {
            // 发起HTTP请求
            URL url = new URL("https://webapi.lowiro.com/collabo/kop/progress");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            // 读取响应
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            connection.disconnect();

            // 解析JSON数据
            JSONObject jsonResponseObj = new JSONObject(response.toString());
            JSONObject valueObj = jsonResponseObj.getJSONObject("value");
            double a = Double.parseDouble(valueObj.getString("a"));
            double c = Double.parseDouble(valueObj.getString("c"));
            boolean isFinished = valueObj.getBoolean("is_finished");
            String winner = valueObj.getString("winner");
            String rewardName = valueObj.getJSONObject("assets").getString("reward_name");
            String rewardImg0 = valueObj.getJSONObject("assets").getString("reward_img_0");
            String rewardVariant0 = valueObj.getJSONObject("assets").getString("reward_variant_0");
            String rewardImg1 = valueObj.getJSONObject("assets").getString("reward_img_1");
            String rewardVariant1 = valueObj.getJSONObject("assets").getString("reward_variant_1");

            // 更新UI
            SwingUtilities.invokeLater(() -> {
                aProgressBar.setValue((int) (a * 100));
                cProgressBar.setValue((int) (c * 100));
                aLabel.setText("Arcaea的进度: " + String.format("%.2f%%", a ));
                cLabel.setText("CHUNITHM的进度: " + String.format("%.2f%%", c ));
                winnerLabel.setText("Winner: " + winner);
                rewardNameLabel.setText("Reward Name: " + rewardName);
                rewardVariant0Label.setText("Reward Variant 0: " + rewardVariant0);
                rewardVariant1Label.setText("Reward Variant 1: " + rewardVariant1);
                statusLabel.setText("状态: " + (isFinished ? "已完成" : "仍在进行"));

                // 加载并显示图片
                loadImageAndDisplay(rewardImg0, rewardImg0Label);
                loadImageAndDisplay(rewardImg1, rewardImg1Label);
            });

        } catch (Exception e) {
            e.printStackTrace();
            SwingUtilities.invokeLater(() -> {
                aProgressBar.setValue(0);
                cProgressBar.setValue(0);
                statusLabel.setText("状态: 错误");
            });
        }
    }

    private void loadImageAndDisplay(String imageUrl, JLabel label) {
        try {
            URL url = new URL(imageUrl);
            ImageIcon icon = new ImageIcon(url);
            label.setIcon(icon);
        } catch (Exception e) {
            e.printStackTrace();
            label.setText("无法加载图片");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new APIDataFetcher());
    }
}