import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TranslatorAppSwing extends JFrame {
    private JTable table;
    private JButton loadButton;
    private JButton saveButton;
    private JButton insertButton;
    private DefaultTableModel tableModel;

    public TranslatorAppSwing() {
        super("沙雕翻译包实用程序（测试版）");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // 关闭时只关闭当前窗口
        setLayout(new BorderLayout());

        // Create table model
        tableModel = new DefaultTableModel(new String[]{"本地化键名", "键值"}, 0);
        table = new JTable(tableModel);

        // Create buttons
        loadButton = new JButton("载入翻译文件");
        saveButton = new JButton("写入翻译文件");
        insertButton = new JButton("插入键值与键名");

        // Button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(loadButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(insertButton);

        // Add components to frame
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Add action listeners
        loadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadJson();
            }
        });

        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveJson();
            }
        });

        insertButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertRow();
            }
        });
    }

    private void loadJson() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("JSON Files", "json"));
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, String> data = objectMapper.readValue(file, HashMap.class);

                tableModel.setRowCount(0); // Clear existing rows
                for (Map.Entry<String, String> entry : data.entrySet()) {
                    tableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "无法加载文件: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveJson() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("JSON Files", "json"));
        int result = fileChooser.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                Map<String, String> data = new HashMap<>();
                for (int row = 0; row < tableModel.getRowCount(); row++) {
                    String key = (String) tableModel.getValueAt(row, 0);
                    String value = (String) tableModel.getValueAt(row, 1);
                    data.put(key, value);
                }

                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, data);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "无法保存文件: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void insertRow() {
        tableModel.addRow(new Object[]{"", ""});
    }
}