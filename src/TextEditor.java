import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class TextEditor extends JFrame {

    private final JDesktopPane desktop = new JDesktopPane();
    private int windowCount = 0;

    public TextEditor() {
        super("MDI 文本编辑器");

        // 1. 强制 Metal（经典）外观
        setClassicLookAndFeel();

        // 2. 主框架设置
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        // 3. 菜单栏
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("文件(F)");
        fileMenu.setMnemonic('F');

        fileMenu.add(createMenuItem("新建(N)", 'N', KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), this::createNewDocument));
        fileMenu.add(createMenuItem("打开(O)...", 'O', KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK), this::openFile));
        fileMenu.addSeparator();
        fileMenu.add(createMenuItem("保存(S)...", 'S', KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), this::saveFile));
        fileMenu.addSeparator();
        fileMenu.add(createMenuItem("退出(X)", 'X', null, e -> System.exit(0)));

        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        // 4. 工具栏（简单示例）
        JToolBar toolBar = new JToolBar();
        toolBar.add(createToolbarButton("新建(Ctrl+N)", this::createNewDocument));

        toolBar.add(createToolbarButton("打开(Ctrl+O)", this::openFile));
        toolBar.add(createToolbarButton("保存(Ctrl+S)", this::saveFile));
        add(toolBar, BorderLayout.NORTH);

        // 5. 把 desktop 放到窗口中心
        add(desktop);
    }

    /* ---------------- 内部窗口管理 ---------------- */

    private void createNewDocument(ActionEvent e) {
        createInternalFrame(null);
    }

    private void openFile(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        createInternalFrame(file);
    }

    // 修改文件读取方法
    private void createInternalFrame(File file) {
        JInternalFrame frame = new JInternalFrame(
                file == null ? "无标题 " + (++windowCount) : file.getName(),
                true, true, true, true);
        JTextArea textArea = new JTextArea(20, 60);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));

        if (file != null) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                textArea.read(br, null);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "无法读取文件:\n" + ex.getMessage(),
                        "错误", JOptionPane.ERROR_MESSAGE);
            }
        }

        frame.add(new JScrollPane(textArea));
        frame.setSize(400, 300);
        frame.setLocation(20 * (windowCount % 10), 20 * (windowCount % 10));
        frame.putClientProperty("textArea", textArea);
        frame.putClientProperty("file", file);
        frame.setVisible(true);
        desktop.add(frame);
        try {
            frame.setSelected(true);
        } catch (java.beans.PropertyVetoException ignored) {
        }
    }

    /* ---------------- 保存 ---------------- */

    // 修改文件保存方法
    private void saveFile(ActionEvent e) {
        JInternalFrame frame = desktop.getSelectedFrame();
        if (frame == null) return;

        JTextArea textArea = (JTextArea) frame.getClientProperty("textArea");
        File file = (File) frame.getClientProperty("file");

        if (file == null) {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            file = chooser.getSelectedFile();
            frame.putClientProperty("file", file);
            frame.setTitle(file.getName());
        }

        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            textArea.write(bw);
            JOptionPane.showMessageDialog(this, "文件保存成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "无法保存文件:\n" + ex.getMessage(),
                    "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    /* ---------------- 工具方法 ---------------- */

    private static JMenuItem createMenuItem(String text, char mnemonic, KeyStroke accelerator, java.util.function.Consumer<ActionEvent> action) {
        JMenuItem item = new JMenuItem(text);
        item.setMnemonic(mnemonic);
        if (accelerator != null) {
            item.setAccelerator(accelerator);
        }
        item.addActionListener(action::accept);
        return item;
    }

    private static JButton createToolbarButton(String text, java.util.function.Consumer<ActionEvent> action) {
        JButton btn = new JButton(text);
        btn.addActionListener(action::accept);
        return btn;
    }

    private static void setClassicLookAndFeel() {
        try {
            // 强制 Metal，关闭“Ocean”主题，保持经典灰色
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
            UIManager.put("swing.boldMetal", Boolean.FALSE);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /* ---------------- 启动 ---------------- */

    // 在每个主类的 main 方法中添加
    public static void main(String[] args) {
        // 设置全局字体以支持中文显示
        setGlobalFont();
        
        SwingUtilities.invokeLater(() -> new TextEditor().setVisible(true));
    }

    private static void setGlobalFont() {
        try {
            // 设置默认字体为支持中文的字体
            Font font = new Font("Microsoft YaHei", Font.PLAIN, 12);
            
            java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                Object value = UIManager.get(key);
                if (value instanceof javax.swing.plaf.FontUIResource) {
                    UIManager.put(key, font);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}