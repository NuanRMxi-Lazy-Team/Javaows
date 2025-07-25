import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Color;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CSVExcel extends JFrame {
    private DefaultTableModel model;
    private JTable table;
    private JFileChooser chooser;
    private String currentFileName = "";

    public CSVExcel() {
        super("Javaows Office Excel");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 700);

        initializeComponents();
        setupMenus();
        setupTable();

        setLocationRelativeTo(null);

        // 默认创建一个新表格
        createNewSheet();
    }

    private void initializeComponents() {
        // 初始化文件选择器
        chooser = new JFileChooser();
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("CSV文件 (*.csv)", "csv"));
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("Excel文件 (*.xlsx)", "xlsx"));
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("Excel文件 (*.xls)", "xls"));
        chooser.setFileFilter(new FileNameExtensionFilter("所有支持的格式", "csv", "xlsx", "xls"));
    }

    private void setupTable() {
        model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };

        table = new JTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setGridColor(Color.LIGHT_GRAY);
        table.setShowGrid(true);

        // 添加右键菜单
        setupTableContextMenu();

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        add(scrollPane, BorderLayout.CENTER);
    }

    private void setupTableContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();

        JMenuItem insertRow = new JMenuItem("插入行");
        JMenuItem deleteRow = new JMenuItem("删除行");
        JMenuItem insertCol = new JMenuItem("插入列");
        JMenuItem deleteCol = new JMenuItem("删除列");

        insertRow.addActionListener(e -> insertRow());
        deleteRow.addActionListener(e -> deleteRow());
        insertCol.addActionListener(e -> insertColumn());
        deleteCol.addActionListener(e -> deleteColumn());

        contextMenu.add(insertRow);
        contextMenu.add(deleteRow);
        contextMenu.addSeparator();
        contextMenu.add(insertCol);
        contextMenu.add(deleteCol);

        table.setComponentPopupMenu(contextMenu);
    }

    private void setupMenus() {
    JMenuBar menuBar = new JMenuBar();

    /* ---------- 文件 ---------- */
    JMenu fileMenu = new JMenu("文件(F)");
    fileMenu.setMnemonic(KeyEvent.VK_F);

    JMenuItem newFile   = newItem("新建(N)", e -> createNewSheet());
    JMenuItem openFile  = newItem("打开(O)", e -> openFile());
    JMenuItem saveFile  = newItem("保存(S)", e -> saveFile());
    JMenuItem saveAsFile= newItem("另存为(A)", e -> saveAsFile());
    JMenuItem exitApp   = newItem("退出(X)", e -> System.exit(0));

    newFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
    openFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
    saveFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));

    fileMenu.add(newFile);
    fileMenu.addSeparator();
    fileMenu.add(openFile);
    fileMenu.addSeparator();
    fileMenu.add(saveFile);
    fileMenu.add(saveAsFile);
    fileMenu.addSeparator();
    fileMenu.add(exitApp);

    /* ---------- 编辑 ---------- */
    JMenu editMenu = new JMenu("编辑(E)");
    editMenu.setMnemonic(KeyEvent.VK_E);
    editMenu.add(newItem("添加行",  e -> addRow()));
    editMenu.add(newItem("添加列", e -> addColumn()));
    editMenu.addSeparator();
    editMenu.add(newItem("清空所有", e -> clearAll()));

    /* ---------- 帮助 ---------- */
    JMenu helpMenu = new JMenu("帮助(H)");
    helpMenu.setMnemonic(KeyEvent.VK_H);
    helpMenu.add(newItem("关于Javaows Office Excel", e ->
        JOptionPane.showMessageDialog(this,
            "Javaows Office Excel\nJavaows Office 套件之一\n版本 1.0\n作者: Celestia\n\n\n\n\n\n\n此软件无需激活",
            "关于", JOptionPane.INFORMATION_MESSAGE)
    ));

    menuBar.add(fileMenu);
    menuBar.add(editMenu);
    menuBar.add(helpMenu);

    setJMenuBar(menuBar);
}

/* 小工具：快速创建菜单项 */
private JMenuItem newItem(String text, ActionListener l) {
    JMenuItem mi = new JMenuItem(text);
    mi.addActionListener(l);
    return mi;
}

    // 新建工作表
    private void createNewSheet() {
        model.setRowCount(0);
        model.setColumnCount(0);

        // 创建默认的10列20行
        for (int i = 0; i < 10; i++) {
            model.addColumn(getColumnName(i));
        }

        for (int i = 0; i < 20; i++) {
            model.addRow(new Object[10]);
        }

        currentFileName = "";
        updateTitle();
    }

    // 获取Excel样式的列名 (A, B, C... AA, AB...)
    private String getColumnName(int columnIndex) {
        StringBuilder name = new StringBuilder();
        while (columnIndex >= 0) {
            name.insert(0, (char)('A' + (columnIndex % 26)));
            columnIndex = columnIndex / 26 - 1;
        }
        return name.toString();
    }

    // 打开文件
    private void openFile() {
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        String fileName = file.getName().toLowerCase();

        try {
            if (fileName.endsWith(".csv")) {
                openCsvFile(file);
            } else if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
                openExcelFile(file);
            } else {
                JOptionPane.showMessageDialog(this, "不支持的文件格式！", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            currentFileName = file.getAbsolutePath();
            updateTitle();
            JOptionPane.showMessageDialog(this, "文件打开成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "打开文件失败：" + ex.getMessage(),
                                        "错误", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    // 打开CSV文件
    private void openCsvFile(File file) throws IOException {
        model.setRowCount(0);
        model.setColumnCount(0);

        try (BufferedReader br = new BufferedReader(new FileReader(file, java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;

            while ((line = br.readLine()) != null) {
                String[] cells = parseCsvLine(line);

                if (firstLine) {
                    for (int i = 0; i < cells.length; i++) {
                        model.addColumn(getColumnName(i));
                    }
                    firstLine = false;
                }

                // 确保行数据长度与列数匹配
                Object[] rowData = new Object[model.getColumnCount()];
                for (int i = 0; i < rowData.length; i++) {
                    rowData[i] = i < cells.length ? cells[i] : "";
                }
                model.addRow(rowData);
            }
        }
    }

    // 解析CSV行，处理引号和逗号
    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++; // 跳过下一个引号
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());

        return result.toArray(new String[0]);
    }

    // 打开Excel文件
    private void openExcelFile(File file) throws IOException {
        model.setRowCount(0);
        model.setColumnCount(0);

        try (FileInputStream fis = new FileInputStream(file)) {
            Workbook workbook;
            if (file.getName().toLowerCase().endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(fis);
            } else {
                workbook = new HSSFWorkbook(fis);
            }

            Sheet sheet = workbook.getSheetAt(0); // 读取第一个工作表

            // 确定最大列数
            int maxColumns = 0;
            for (Row row : sheet) {
                if (row.getLastCellNum() > maxColumns) {
                    maxColumns = row.getLastCellNum();
                }
            }

            // 添加列
            for (int i = 0; i < maxColumns; i++) {
                model.addColumn(getColumnName(i));
            }

            // 读取数据
            for (Row row : sheet) {
                Object[] rowData = new Object[maxColumns];
                for (int i = 0; i < maxColumns; i++) {
                    Cell cell = row.getCell(i);
                    rowData[i] = getCellValue(cell);
                }
                model.addRow(rowData);
            }

            workbook.close();
        }
    }

    // 获取单元格值
    private String getCellValue(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double numValue = cell.getNumericCellValue();
                    if (numValue == Math.floor(numValue)) {
                        return String.valueOf((long) numValue);
                    } else {
                        return String.valueOf(numValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    // 保存文件
    private void saveFile() {
        if (currentFileName.isEmpty()) {
            saveAsFile();
        } else {
            try {
                File file = new File(currentFileName);
                if (currentFileName.toLowerCase().endsWith(".csv")) {
                    saveCsvFile(file);
                } else {
                    saveExcelFile(file);
                }
                JOptionPane.showMessageDialog(this, "保存成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "保存失败：" + ex.getMessage(),
                                            "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // 另存为
    private void saveAsFile() {
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        String fileName = file.getName().toLowerCase();

        try {
            if (fileName.endsWith(".csv")) {
                saveCsvFile(file);
            } else if (fileName.endsWith(".xlsx")) {
                if (!fileName.endsWith(".xlsx")) {
                    file = new File(file.getAbsolutePath() + ".xlsx");
                }
                saveExcelFile(file);
            } else if (fileName.endsWith(".xls")) {
                if (!fileName.endsWith(".xls")) {
                    file = new File(file.getAbsolutePath() + ".xls");
                }
                saveExcelFile(file);
            } else {
                // 默认保存为Excel格式
                file = new File(file.getAbsolutePath() + ".xlsx");
                saveExcelFile(file);
            }

            currentFileName = file.getAbsolutePath();
            updateTitle();
            JOptionPane.showMessageDialog(this, "保存成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "保存失败：" + ex.getMessage(),
                                        "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    // 保存CSV文件
    private void saveCsvFile(File file) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8))) {
            for (int r = 0; r < model.getRowCount(); r++) {
                List<String> line = new ArrayList<>();
                for (int c = 0; c < model.getColumnCount(); c++) {
                    Object val = model.getValueAt(r, c);
                    String cellValue = val == null ? "" : val.toString();

                    // 处理包含逗号或引号的字段
                    if (cellValue.contains(",") || cellValue.contains("\"") || cellValue.contains("\n")) {
                        cellValue = "\"" + cellValue.replace("\"", "\"\"") + "\"";
                    }
                    line.add(cellValue);
                }
                pw.println(String.join(",", line));
            }
        }
    }

    // 保存Excel文件
    private void saveExcelFile(File file) throws IOException {
        Workbook workbook;
        if (file.getName().toLowerCase().endsWith(".xlsx")) {
            workbook = new XSSFWorkbook();
        } else {
            workbook = new HSSFWorkbook();
        }

        Sheet sheet = workbook.createSheet("Sheet1");

        for (int r = 0; r < model.getRowCount(); r++) {
            Row row = sheet.createRow(r);
            for (int c = 0; c < model.getColumnCount(); c++) {
                Cell cell = row.createCell(c);
                Object value = model.getValueAt(r, c);
                if (value != null) {
                    String stringValue = value.toString();
                    // 尝试将字符串转换为数字
                    try {
                        double numValue = Double.parseDouble(stringValue);
                        cell.setCellValue(numValue);
                    } catch (NumberFormatException e) {
                        cell.setCellValue(stringValue);
                    }
                } else {
                    cell.setCellValue("");
                }
            }
        }

        // 自动调整列宽
        for (int c = 0; c < model.getColumnCount(); c++) {
            sheet.autoSizeColumn(c);
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            workbook.write(fos);
        }

        workbook.close();
    }

    // 更新窗口标题
    private void updateTitle() {
        String title = "Javaows Office Excel";
        if (!currentFileName.isEmpty()) {
            title += " - " + new File(currentFileName).getName();
        }
        setTitle(title);
    }

    // 表格操作方法
    private void addRow() {
        model.addRow(new Object[model.getColumnCount()]);
    }

    private void addColumn() {
        model.addColumn(getColumnName(model.getColumnCount()));
    }

    private void insertRow() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0) {
            model.insertRow(selectedRow, new Object[model.getColumnCount()]);
        } else {
            addRow();
        }
    }

    private void deleteRow() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0) {
            model.removeRow(selectedRow);
        }
    }

    private void insertColumn() {
        int selectedCol = table.getSelectedColumn();
        if (selectedCol >= 0) {
            model.addColumn(getColumnName(model.getColumnCount()));
            // 移动列数据
            for (int r = 0; r < model.getRowCount(); r++) {
                for (int c = model.getColumnCount() - 1; c > selectedCol; c--) {
                    model.setValueAt(model.getValueAt(r, c - 1), r, c);
                }
                model.setValueAt("", r, selectedCol);
            }
        } else {
            addColumn();
        }
    }

    private void deleteColumn() {
        int selectedCol = table.getSelectedColumn();
        if (selectedCol >= 0 && model.getColumnCount() > 1) {
            // 移动数据
            for (int r = 0; r < model.getRowCount(); r++) {
                for (int c = selectedCol; c < model.getColumnCount() - 1; c++) {
                    model.setValueAt(model.getValueAt(r, c + 1), r, c);
                }
            }
            // 删除最后一列
            TableColumnModel columnModel = table.getColumnModel();
            columnModel.removeColumn(columnModel.getColumn(model.getColumnCount() - 1));
            model.setColumnCount(model.getColumnCount() - 1);
        }
    }

    private void clearAll() {
        int option = JOptionPane.showConfirmDialog(this,
            "确定要清空所有数据吗？", "确认",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (option == JOptionPane.YES_OPTION) {
            createNewSheet();
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
            new CSVExcel().setVisible(true);
        });
    }
}