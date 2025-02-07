import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class MinecraftVersionViewerSwing extends JFrame {
    private JButton refreshButton;
    private JTable tableView;
    private JTextArea detailView;
    private JLabel statusLabel;

    public MinecraftVersionViewerSwing() {
        super("Minecraft 版本查看器");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // 关闭时只关闭当前窗口
        setLayout(new BorderLayout());

        // Create UI components
        refreshButton = new JButton("刷新当前版本");
        tableView = new JTable();
        tableView.setModel(new DefaultTableModel(new String[]{"版本号", "发布类型", "发布日期"}, 0));
        detailView = new JTextArea();
        detailView.setEditable(false);
        statusLabel = new JLabel("状态：就绪");

        // Layout
        JPanel northPanel = new JPanel();
        northPanel.add(refreshButton);

        JScrollPane tableScrollPane = new JScrollPane(tableView);
        JScrollPane detailScrollPane = new JScrollPane(detailView);

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                tableScrollPane,
                detailScrollPane
        );
        splitPane.setDividerLocation(200);

        add(northPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        // Event handlers
        refreshButton.addActionListener(e -> loadVersions());
        tableView.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showVersionDetails();
            }
        });
    }

    private void loadVersions() {
        statusLabel.setText("状态：正在下载元文件");
        new Thread(() -> {
            try {
                String url = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode rootNode = objectMapper.readTree(connection.getInputStream());
                JsonNode versionsNode = rootNode.path("versions");

                DefaultTableModel model = (DefaultTableModel) tableView.getModel();
                model.setRowCount(0);

                for (JsonNode versionNode : versionsNode) {
                    String id = versionNode.path("id").asText();
                    String type = versionNode.path("type").asText();
                    String releaseTime = versionNode.path("releaseTime").asText();

                    // Convert releaseTime to UTC+8
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
                    SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy年MM月dd日HH点mm分");
                    outputFormat.setTimeZone(TimeZone.getTimeZone("UTC+8"));
                    Date date = inputFormat.parse(releaseTime);
                    String formattedReleaseTime = outputFormat.format(date);

                    model.addRow(new Object[]{id, getTypeDescription(type), formattedReleaseTime});
                }

                statusLabel.setText("状态：版本载入完成！");
            } catch (Exception e) {
                statusLabel.setText("状态: 错误 - " + e.getMessage());
                JOptionPane.showMessageDialog(this, "无法加载版本信息，请检查链接的合法性或稍后重试。\n链接: " , "错误", JOptionPane.ERROR_MESSAGE);
            }
        }).start();
    }

    private String getTypeDescription(String type) {
        switch (type) {
            case "snapshot":
                return "快照";
            case "release":
                return "正式版";
            case "old_alpha":
                return "旧内测版";
            case "old_beta":
                return "旧测试版";
            default:
                return type;
        }
    }

    private void showVersionDetails() {
        int selectedRow = tableView.getSelectedRow();
        if (selectedRow >= 0) {
            String versionId = tableView.getValueAt(selectedRow, 0).toString();
            String versionType = tableView.getValueAt(selectedRow, 1).toString();
            String releaseTime = tableView.getValueAt(selectedRow, 2).toString();

            String details = String.format("版本号: %s\n发布类型: %s\n发布日期: %s", versionId, versionType, releaseTime);
            detailView.setText(details);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MinecraftVersionViewerSwing viewer = new MinecraftVersionViewerSwing();
            viewer.setVisible(true);
        });
    }
}