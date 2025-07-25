import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.apache.poi.xwpf.usermodel.*;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class JavaWord extends JFrame {
    private JTextPane left;  // 改为JTextPane支持格式化
    private JEditorPane right;
    private File currentFile;
    private final Parser parser;
    private final HtmlRenderer renderer;

    // 撤销重做功能
    private UndoManager undoManager;
    private JMenuItem undoMenuItem, redoMenuItem;

    // 格式化工具栏
    private JToolBar formatToolBar;
    private JButton boldButton, italicButton, underlineButton;
    private JComboBox<String> fontSizeCombo;
    private JComboBox<String> fontFamilyCombo;
    private JColorChooser colorChooser;

    // 查找替换功能
    private JDialog findDialog;
    private JTextField findField, replaceField;

    public JavaWord() {
        super("Javaows Office Word");

        // Markdown 解析器
        MutableDataSet options = new MutableDataSet();
        parser = Parser.builder(options).build();
        renderer = HtmlRenderer.builder(options).build();

        // 初始化撤销管理器
        undoManager = new UndoManager();

        setupUI();
        setupMenu();
        setupToolBar();
        setupKeyboardShortcuts();

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);

        // 设置应用图标
        try {
            setIconImage(createAppIcon());
        } catch (Exception e) {
            // 如果创建图标失败，继续运行
        }
    }

    private void setupUI() {
        left = new JTextPane();
        left.setText("# Welcome to Javaows Office Word！\n\n- 所见即所得编辑器\n- 支持 DOCX 导出\n- 撤销重做功能\n- 文本格式化\n- 查找替换功能");

        right = new JEditorPane("text/html", "");
        right.setEditable(false);

        // 设置字体
        Font editorFont = new Font("Microsoft YaHei", Font.PLAIN, 14);
        left.setFont(editorFont);

        // 添加文档监听器
        left.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { render(); }
            public void removeUpdate(DocumentEvent e) { render(); }
            public void changedUpdate(DocumentEvent e) { render(); }
        });

        // 添加撤销监听器
        left.getDocument().addUndoableEditListener(new UndoableEditListener() {
            public void undoableEditHappened(UndoableEditEvent e) {
                undoManager.addEdit(e.getEdit());
                updateUndoRedoButtons();
            }
        });

        // 创建分割面板
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(left), new JScrollPane(right));
        split.setDividerLocation(700);
        split.setResizeWeight(0.5);

        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(split, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);
        render();
    }

    private void setupToolBar() {
        formatToolBar = new JToolBar("格式化工具栏");
        formatToolBar.setFloatable(false);

        // 撤销重做按钮
        JButton undoButton = createToolBarButton("撤销", "撤销上一步操作", e -> undo());
        JButton redoButton = createToolBarButton("重做", "重做下一步操作", e -> redo());

        formatToolBar.add(undoButton);
        formatToolBar.add(redoButton);
        formatToolBar.addSeparator();

        // 字体系列选择
        String[] fontFamilies = {"Microsoft YaHei", "SimSun", "Arial", "Times New Roman", "Courier New"};
        fontFamilyCombo = new JComboBox<>(fontFamilies);
        fontFamilyCombo.setSelectedItem("Microsoft YaHei");
        fontFamilyCombo.addActionListener(e -> setFontFamily());
        formatToolBar.add(new JLabel("字体: "));
        formatToolBar.add(fontFamilyCombo);

        formatToolBar.addSeparator();

        // 字体大小选择
        String[] fontSizes = {"10", "12", "14", "16", "18", "20", "24", "28", "32"};
        fontSizeCombo = new JComboBox<>(fontSizes);
        fontSizeCombo.setSelectedItem("14");
        fontSizeCombo.addActionListener(e -> setFontSize());
        formatToolBar.add(new JLabel("大小: "));
        formatToolBar.add(fontSizeCombo);

        formatToolBar.addSeparator();

        // 格式化按钮
        boldButton = createToggleButton("粗体", "设置文本为粗体", e -> toggleBold());
        italicButton = createToggleButton("斜体", "设置文本为斜体", e -> toggleItalic());
        underlineButton = createToggleButton("下划线", "设置文本下划线", e -> toggleUnderline());

        formatToolBar.add(boldButton);
        formatToolBar.add(italicButton);
        formatToolBar.add(underlineButton);

        formatToolBar.addSeparator();

        // 其他功能按钮
        JButton findButton = createToolBarButton("查找", "查找文本", e -> showFindDialog());
        JButton replaceButton = createToolBarButton("替换", "查找并替换文本", e -> showFindReplaceDialog());

        formatToolBar.add(findButton);
        formatToolBar.add(replaceButton);

        add(formatToolBar, BorderLayout.NORTH);
    }

    private JButton createToolBarButton(String text, String tooltip, ActionListener action) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.addActionListener(action);
        button.setFocusable(false);
        return button;
    }

    private JButton createToggleButton(String text, String tooltip, ActionListener action) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.addActionListener(action);
        button.setFocusable(false);
        return button;
    }

    private void setupKeyboardShortcuts() {
        // 撤销重做快捷键
        left.getInputMap().put(KeyStroke.getKeyStroke("ctrl Z"), "undo");
        left.getActionMap().put("undo", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { undo(); }
        });

        left.getInputMap().put(KeyStroke.getKeyStroke("ctrl Y"), "redo");
        left.getActionMap().put("redo", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { redo(); }
        });

        // 格式化快捷键
        left.getInputMap().put(KeyStroke.getKeyStroke("ctrl B"), "bold");
        left.getActionMap().put("bold", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { toggleBold(); }
        });

        left.getInputMap().put(KeyStroke.getKeyStroke("ctrl I"), "italic");
        left.getActionMap().put("italic", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { toggleItalic(); }
        });

        left.getInputMap().put(KeyStroke.getKeyStroke("ctrl U"), "underline");
        left.getActionMap().put("underline", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { toggleUnderline(); }
        });

        // 查找快捷键
        left.getInputMap().put(KeyStroke.getKeyStroke("ctrl F"), "find");
        left.getActionMap().put("find", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { showFindDialog(); }
        });

        // 替换快捷键
        left.getInputMap().put(KeyStroke.getKeyStroke("ctrl H"), "replace");
        left.getActionMap().put("replace", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { showFindReplaceDialog(); }
        });
    }

    private void render() {
        try {
            String html = renderer.render(parser.parse(left.getText()));
            right.setText("<html><head><style>body{font-family:'Microsoft YaHei',sans-serif; padding:20px;}</style></head><body>" + html + "</body></html>");
        } catch (Exception ex) {
            right.setText("<html><body style='color:red; font-family:Microsoft YaHei;'>" + ex.getMessage() + "</body></html>");
        }
    }

    private void setupMenu() {
        JMenuBar bar = new JMenuBar();

        // 文件菜单
        JMenu fileMenu = new JMenu("文件(F)");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        fileMenu.add(newItem("新建", KeyEvent.VK_N, "ctrl N", e -> newFile()));
        fileMenu.add(newItem("打开", KeyEvent.VK_O, "ctrl O", e -> openFile()));
        fileMenu.add(newItem("保存", KeyEvent.VK_S, "ctrl S", e -> saveFile()));
        fileMenu.add(newItem("另存为", KeyEvent.VK_A, "ctrl shift S", e -> saveAsFile()));
        fileMenu.addSeparator();
        fileMenu.add(newItem("导出 DOCX", KeyEvent.VK_E, null, e -> exportDocx()));
        fileMenu.add(newItem("导出 HTML", KeyEvent.VK_H, null, e -> exportHtml()));
        fileMenu.addSeparator();
        fileMenu.add(newItem("退出", KeyEvent.VK_X, "ctrl Q", e -> exitApplication()));

        // 编辑菜单
        JMenu editMenu = new JMenu("编辑(E)");
        editMenu.setMnemonic(KeyEvent.VK_E);
        undoMenuItem = newItem("撤销", KeyEvent.VK_Z, "ctrl Z", e -> undo());
        redoMenuItem = newItem("重做", KeyEvent.VK_Y, "ctrl Y", e -> redo());
        editMenu.add(undoMenuItem);
        editMenu.add(redoMenuItem);
        editMenu.addSeparator();
        editMenu.add(newItem("剪切", KeyEvent.VK_T, "ctrl X", e -> left.cut()));
        editMenu.add(newItem("复制", KeyEvent.VK_C, "ctrl C", e -> left.copy()));
        editMenu.add(newItem("粘贴", KeyEvent.VK_P, "ctrl V", e -> left.paste()));
        editMenu.addSeparator();
        editMenu.add(newItem("全选", KeyEvent.VK_A, "ctrl A", e -> left.selectAll()));
        editMenu.add(newItem("查找", KeyEvent.VK_F, "ctrl F", e -> showFindDialog()));
        editMenu.add(newItem("替换", KeyEvent.VK_R, "ctrl H", e -> showFindReplaceDialog()));

        // 格式菜单
        JMenu formatMenu = new JMenu("格式(M)");
        formatMenu.setMnemonic(KeyEvent.VK_M);
        formatMenu.add(newItem("粗体", KeyEvent.VK_B, "ctrl B", e -> toggleBold()));
        formatMenu.add(newItem("斜体", KeyEvent.VK_I, "ctrl I", e -> toggleItalic()));
        formatMenu.add(newItem("下划线", KeyEvent.VK_U, "ctrl U", e -> toggleUnderline()));
        formatMenu.addSeparator();
        formatMenu.add(newItem("字体设置", KeyEvent.VK_F, null, e -> showFontDialog()));

        // 视图菜单
        JMenu viewMenu = new JMenu("视图(V)");
        viewMenu.setMnemonic(KeyEvent.VK_V);
        JCheckBoxMenuItem toolbarItem = new JCheckBoxMenuItem("显示工具栏", true);
        toolbarItem.addActionListener(e -> formatToolBar.setVisible(toolbarItem.isSelected()));
        viewMenu.add(toolbarItem);

        // 帮助菜单
        JMenu helpMenu = new JMenu("帮助(H)");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        helpMenu.add(newItem("使用说明", KeyEvent.VK_H, "F1", e -> showHelp()));
        helpMenu.add(newItem("关于", KeyEvent.VK_A, null, e -> showAbout()));

        bar.add(fileMenu);
        bar.add(editMenu);
        bar.add(formatMenu);
        bar.add(viewMenu);
        bar.add(helpMenu);

        setJMenuBar(bar);
        updateUndoRedoButtons();
    }

    private JMenuItem newItem(String name, int mnemonic, String accelerator, ActionListener l) {
        JMenuItem mi = new JMenuItem(name);
        if (mnemonic != 0) mi.setMnemonic(mnemonic);
        if (accelerator != null) mi.setAccelerator(KeyStroke.getKeyStroke(accelerator));
        mi.addActionListener(l);
        return mi;
    }

    // 撤销重做功能
    private void undo() {
        if (undoManager.canUndo()) {
            undoManager.undo();
            updateUndoRedoButtons();
        }
    }

    private void redo() {
        if (undoManager.canRedo()) {
            undoManager.redo();
            updateUndoRedoButtons();
        }
    }

    private void updateUndoRedoButtons() {
        if (undoMenuItem != null) {
            undoMenuItem.setEnabled(undoManager.canUndo());
            redoMenuItem.setEnabled(undoManager.canRedo());
        }
    }

    // 格式化功能
    private void toggleBold() {
        StyledDocument doc = left.getStyledDocument();
        int start = left.getSelectionStart();
        int length = left.getSelectedText() != null ? left.getSelectedText().length() : 0;

        if (length > 0) {
            SimpleAttributeSet attrs = new SimpleAttributeSet();
            StyleConstants.setBold(attrs, !StyleConstants.isBold(doc.getCharacterElement(start).getAttributes()));
            doc.setCharacterAttributes(start, length, attrs, false);
        }
    }

    private void toggleItalic() {
        StyledDocument doc = left.getStyledDocument();
        int start = left.getSelectionStart();
        int length = left.getSelectedText() != null ? left.getSelectedText().length() : 0;

        if (length > 0) {
            SimpleAttributeSet attrs = new SimpleAttributeSet();
            StyleConstants.setItalic(attrs, !StyleConstants.isItalic(doc.getCharacterElement(start).getAttributes()));
            doc.setCharacterAttributes(start, length, attrs, false);
        }
    }

    private void toggleUnderline() {
        StyledDocument doc = left.getStyledDocument();
        int start = left.getSelectionStart();
        int length = left.getSelectedText() != null ? left.getSelectedText().length() : 0;

        if (length > 0) {
            SimpleAttributeSet attrs = new SimpleAttributeSet();
            StyleConstants.setUnderline(attrs, !StyleConstants.isUnderline(doc.getCharacterElement(start).getAttributes()));
            doc.setCharacterAttributes(start, length, attrs, false);
        }
    }

    private void setFontFamily() {
        String fontFamily = (String) fontFamilyCombo.getSelectedItem();
        StyledDocument doc = left.getStyledDocument();
        int start = left.getSelectionStart();
        int length = left.getSelectedText() != null ? left.getSelectedText().length() : 0;

        if (length > 0) {
            SimpleAttributeSet attrs = new SimpleAttributeSet();
            StyleConstants.setFontFamily(attrs, fontFamily);
            doc.setCharacterAttributes(start, length, attrs, false);
        } else {
            // 设置整体字体
            Font currentFont = left.getFont();
            Font newFont = new Font(fontFamily, currentFont.getStyle(), currentFont.getSize());
            left.setFont(newFont);
        }
    }

    private void setFontSize() {
        try {
            int fontSize = Integer.parseInt((String) fontSizeCombo.getSelectedItem());
            StyledDocument doc = left.getStyledDocument();
            int start = left.getSelectionStart();
            int length = left.getSelectedText() != null ? left.getSelectedText().length() : 0;

            if (length > 0) {
                SimpleAttributeSet attrs = new SimpleAttributeSet();
                StyleConstants.setFontSize(attrs, fontSize);
                doc.setCharacterAttributes(start, length, attrs, false);
            } else {
                // 设置整体字体大小
                Font currentFont = left.getFont();
                Font newFont = new Font(currentFont.getName(), currentFont.getStyle(), fontSize);
                left.setFont(newFont);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "请输入有效的字体大小！", "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    // 查找替换功能
    private void showFindDialog() {
        if (findDialog == null) {
            createFindDialog();
        }
        findField.setText("");
        findDialog.setVisible(true);
        findField.requestFocus();
    }

    private void showFindReplaceDialog() {
        if (findDialog == null) {
            createFindDialog();
        }
        replaceField.setVisible(true);
        findDialog.setVisible(true);
        findField.requestFocus();
    }

    private void createFindDialog() {
        findDialog = new JDialog(this, "查找和替换", false);
        findDialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // 查找字段
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.insets = new Insets(5, 5, 5, 5);
        findDialog.add(new JLabel("查找:"), gbc);

        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        findField = new JTextField(20);
        findDialog.add(findField, gbc);

        // 替换字段
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        findDialog.add(new JLabel("替换:"), gbc);

        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        replaceField = new JTextField(20);
        findDialog.add(replaceField, gbc);

        // 按钮
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel();

        JButton findButton = new JButton("查找下一个");
        findButton.addActionListener(e -> findNext());

        JButton replaceButton = new JButton("替换");
        replaceButton.addActionListener(e -> replace());

        JButton replaceAllButton = new JButton("全部替换");
        replaceAllButton.addActionListener(e -> replaceAll());

        buttonPanel.add(findButton);
        buttonPanel.add(replaceButton);
        buttonPanel.add(replaceAllButton);

        findDialog.add(buttonPanel, gbc);

        findDialog.setSize(400, 150);
        findDialog.setLocationRelativeTo(this);
    }

    private void findNext() {
        String searchText = findField.getText();
        if (searchText.isEmpty()) return;

        String content = left.getText();
        int startPos = left.getCaretPosition();
        int foundPos = content.indexOf(searchText, startPos);

        if (foundPos == -1 && startPos > 0) {
            // 从头开始搜索
            foundPos = content.indexOf(searchText, 0);
        }

        if (foundPos != -1) {
            left.setSelectionStart(foundPos);
            left.setSelectionEnd(foundPos + searchText.length());
            left.setCaretPosition(foundPos + searchText.length());
        } else {
            JOptionPane.showMessageDialog(this, "找不到 \"" + searchText + "\"", "查找", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void replace() {
        String selectedText = left.getSelectedText();
        String findText = findField.getText();
        String replaceText = replaceField.getText();

        if (selectedText != null && selectedText.equals(findText)) {
            left.replaceSelection(replaceText);
        }
        findNext();
    }

    private void replaceAll() {
        String findText = findField.getText();
        String replaceText = replaceField.getText();

        if (findText.isEmpty()) return;

        String content = left.getText();
        String newContent = content.replace(findText, replaceText);

        left.setText(newContent);

        int count = (content.length() - newContent.length()) / (findText.length() - replaceText.length());
        JOptionPane.showMessageDialog(this, "已替换 " + count + " 处", "替换完成", JOptionPane.INFORMATION_MESSAGE);
    }

    // 字体对话框
    private void showFontDialog() {
        JDialog fontDialog = new JDialog(this, "字体设置", true);
        fontDialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // 字体选择
        gbc.gridx = 0; gbc.gridy = 0;
        fontDialog.add(new JLabel("字体:"), gbc);

        gbc.gridx = 1;
        String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        JComboBox<String> fontCombo = new JComboBox<>(fonts);
        fontCombo.setSelectedItem(left.getFont().getName());
        fontDialog.add(fontCombo, gbc);

        // 字体大小
        gbc.gridx = 0; gbc.gridy = 1;
        fontDialog.add(new JLabel("大小:"), gbc);

        gbc.gridx = 1;
        JSpinner sizeSpinner = new JSpinner(new SpinnerNumberModel(left.getFont().getSize(), 8, 72, 1));
        fontDialog.add(sizeSpinner, gbc);

        // 按钮
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel();

        JButton okButton = new JButton("确定");
        okButton.addActionListener(e -> {
            String fontName = (String) fontCombo.getSelectedItem();
            int fontSize = (Integer) sizeSpinner.getValue();
            Font newFont = new Font(fontName, Font.PLAIN, fontSize);
            left.setFont(newFont);
            fontDialog.dispose();
        });

        JButton cancelButton = new JButton("取消");
        cancelButton.addActionListener(e -> fontDialog.dispose());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        fontDialog.add(buttonPanel, gbc);

        fontDialog.setSize(300, 150);
        fontDialog.setLocationRelativeTo(this);
        fontDialog.setVisible(true);
    }

    // 文件操作
    private void newFile() {
        if (hasUnsavedChanges()) {
            int option = JOptionPane.showConfirmDialog(this, "是否保存当前文档？", "新建文档", JOptionPane.YES_NO_CANCEL_OPTION);
            if (option == JOptionPane.YES_OPTION) {
                saveFile();
            } else if (option == JOptionPane.CANCEL_OPTION) {
                return;
            }
        }
        left.setText("");
        currentFile = null;
        undoManager.discardAllEdits();
        updateUndoRedoButtons();
        setTitle("Javaows Office Word - 新建文档");
    }

    private void openFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Markdown 文件", "md", "markdown", "txt"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        currentFile = chooser.getSelectedFile();
        try (BufferedReader br = new BufferedReader(new FileReader(currentFile, java.nio.charset.StandardCharsets.UTF_8))) {
            left.read(br, null);
            undoManager.discardAllEdits();
            updateUndoRedoButtons();
            setTitle("Javaows Office Word - " + currentFile.getName());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "打开文件失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveFile() {
        if (currentFile == null) {
            saveAsFile();
            return;
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(currentFile, java.nio.charset.StandardCharsets.UTF_8))) {
            left.write(bw);
            setTitle("Javaows Office Word - " + currentFile.getName());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "保存文件失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveAsFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Markdown 文件", "md"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        currentFile = chooser.getSelectedFile();
        if (!currentFile.getName().toLowerCase().endsWith(".md")) {
            currentFile = new File(currentFile.getAbsolutePath() + ".md");
        }
        saveFile();
    }

    private void exportHtml() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("HTML 文件", "html"));
        chooser.setSelectedFile(new File("document.html"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        try (FileWriter writer = new FileWriter(chooser.getSelectedFile(), java.nio.charset.StandardCharsets.UTF_8)) {
            String html = renderer.render(parser.parse(left.getText()));
            String fullHtml = "<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"UTF-8\">\n" +
                            "<title>导出的文档</title>\n" +
                            "<style>body{font-family:'Microsoft YaHei',sans-serif; padding:20px; max-width:800px; margin:0 auto;}</style>\n" +
                            "</head>\n<body>\n" + html + "\n</body>\n</html>";
            writer.write(fullHtml);
            JOptionPane.showMessageDialog(this, "HTML 导出成功！", "完成", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "导出失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportDocx() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Word 文档", "docx"));
        chooser.setSelectedFile(new File("document.docx"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        try (XWPFDocument doc = new XWPFDocument();
             FileOutputStream out = new FileOutputStream(chooser.getSelectedFile())) {

            for (String line : left.getText().split("\n")) {
                line = line.trim();
                if (line.startsWith("# ")) {
                    XWPFParagraph p = doc.createParagraph();
                    p.setStyle("Heading1");
                    p.createRun().setText(line.substring(2));
                } else if (line.startsWith("## ")) {
                    XWPFParagraph p = doc.createParagraph();
                    p.setStyle("Heading2");
                    p.createRun().setText(line.substring(3));
                } else if (line.startsWith("### ")) {
                    XWPFParagraph p = doc.createParagraph();
                    p.setStyle("Heading3");
                    p.createRun().setText(line.substring(4));
                } else if (line.startsWith("- ")) {
                    XWPFParagraph p = doc.createParagraph();
                    p.setStyle("ListBullet");
                    p.createRun().setText(line.substring(2));
                } else if (!line.isEmpty()) {
                    doc.createParagraph().createRun().setText(line);
                }
            }
            doc.write(out);
            JOptionPane.showMessageDialog(this, "DOCX 导出成功！", "完成", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "导出失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean hasUnsavedChanges() {
        // 简单的检查机制，可以根据需要完善
        return !left.getText().isEmpty();
    }

    private void exitApplication() {
        if (hasUnsavedChanges()) {
            int option = JOptionPane.showConfirmDialog(this, "是否保存当前文档？", "退出程序", JOptionPane.YES_NO_CANCEL_OPTION);
            if (option == JOptionPane.YES_OPTION) {
                saveFile();
            } else if (option == JOptionPane.CANCEL_OPTION) {
                return;
            }
        }
        System.exit(0);
    }

    // 帮助和关于
    private void showHelp() {
        String help = """
            Javaows Office Word - 使用说明
            
            功能特色：
            • 实时 Markdown 预览
            • 撤销/重做功能 (Ctrl+Z/Ctrl+Y)
            • 文本格式化 (粗体、斜体、下划线)
            • 查找替换功能 (Ctrl+F/Ctrl+H)
            • 导出为 DOCX 和 HTML 格式
            • 字体设置和工具栏
            
            快捷键：
            Ctrl+N - 新建文档
            Ctrl+O - 打开文档
            Ctrl+S - 保存文档
            Ctrl+Z - 撤销
            Ctrl+Y - 重做
            Ctrl+B - 粗体
            Ctrl+I - 斜体
            Ctrl+U - 下划线
            Ctrl+F - 查找
            Ctrl+H - 替换
            F1 - 显示帮助
            
            Markdown 语法：
            # 一级标题
            ## 二级标题
            **粗体文本**
            *斜体文本*
            - 无序列表
            1. 有序列表
            [链接](url)
            ![图片](url)
            """;

        JDialog helpDialog = new JDialog(this, "使用说明", true);
        JTextArea helpArea = new JTextArea(help);
        helpArea.setEditable(false);
        helpArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(helpArea);
        scrollPane.setPreferredSize(new Dimension(600, 500));

        helpDialog.add(scrollPane);
        helpDialog.setSize(650, 550);
        helpDialog.setLocationRelativeTo(this);
        helpDialog.setVisible(true);
    }

    private void showAbout() {
        String about = """
            Javaows Office Word
            Javaows Office 套件之一
            版本: 2.0
            
            一个功能强大的 Markdown 编辑器，支持：
            • 实时预览
            • 撤销重做
            • 文本格式化
            • 查找替换
            • 多格式导出
            
            开发技术：
            • Java Swing
            • Flexmark (Markdown 解析)
            • Apache POI (DOCX 导出)
            
            作者: Celestia
            此软件无需激活
            """;

        JOptionPane.showMessageDialog(this, about, "关于 Markdown Word", JOptionPane.INFORMATION_MESSAGE);
    }

    private Image createAppIcon() {
        // 创建一个简单的应用图标
        int size = 32;
        java.awt.image.BufferedImage icon = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = icon.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setColor(Color.BLUE);
        g2d.fillRoundRect(0, 0, size, size, 8, 8);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.drawString("M", 10, 20);

        g2d.dispose();
        return icon;
    }

    public static void main(String[] args) {
        // 设置系统外观
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        } catch (Exception e) {
            // 使用默认外观
        }

        SwingUtilities.invokeLater(() -> {
            new JavaWord().setVisible(true);
        });
    }
}