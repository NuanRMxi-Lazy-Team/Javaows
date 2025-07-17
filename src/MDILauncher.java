import java.util.List;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class MDILauncher extends JFrame {
    private final JDesktopPane desktop = new JDesktopPane();
    private int runCount = 0;

    public MDILauncher() {
        super("MDI Java Logger");
        try { UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel"); }
        catch (Exception ignored) {}
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);

        /* ---------- 输入框 ---------- */
        JTextArea cmdArea = new JTextArea(4, 80);
        cmdArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        cmdArea.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && e.isControlDown()) {
                    runRaw(cmdArea.getText().trim());
                }
            }
        });

        /* ---------- 按钮 ---------- */
        JButton runRawBtn = new JButton("运行命令");
        runRawBtn.addActionListener(e -> runRaw(cmdArea.getText().trim()));

        JButton runJarBtn = new JButton("Launch JAR...");
        runJarBtn.addActionListener(e -> launchJar());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(runRawBtn);
        top.add(runJarBtn);

        /* ---------- 组装 ---------- */
        add(new JScrollPane(cmdArea), BorderLayout.NORTH);
        add(top, BorderLayout.CENTER);
        add(desktop, BorderLayout.CENTER);

        /* ---------- 菜单 ---------- */
        JMenuBar bar = new JMenuBar();
        JMenu runMenu = new JMenu("运行(R)");
        runMenu.setMnemonic('R');
        runMenu.add(newItem("Launch JAR...", e -> launchJar()));
        bar.add(runMenu);
        setJMenuBar(bar);
    }

    /* -------------------------------------------------
       1. 粘贴整行命令并运行
       ------------------------------------------------- */
    private void runRaw(String raw) {
        if (raw.isEmpty()) return;
        List<String> cmd = parseCommand(raw);
        runInFrame(cmd, new File("E:\\PCL2\\.minecraft"), "Raw");
    }

    /* -------------------------------------------------
       2. 选择 JAR 并运行
       ------------------------------------------------- */
    private void launchJar() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JAR", "jar"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File jar = fc.getSelectedFile();
        JTextField args = new JTextField(30);
        JTextField workDir = new JTextField(jar.getParent(), 30);

        JPanel p = new JPanel(new GridLayout(0, 1, 5, 5));
        p.add(new JLabel("参数（空格分隔）:"));
        p.add(args);
        p.add(new JLabel("工作目录:"));
        p.add(workDir);

        if (JOptionPane.showConfirmDialog(this, p, "Launch " + jar.getName(),
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        List<String> cmd = new ArrayList<>();
        cmd.add(System.getProperty("java.home") + "\\bin\\java");
        cmd.add("-jar");
        cmd.add(jar.getAbsolutePath());
        if (!args.getText().trim().isEmpty())
            cmd.addAll(Arrays.asList(args.getText().trim().split("\\s+")));

        runInFrame(cmd, new File(workDir.getText()), jar.getName());
    }

    /* -------------------------------------------------
       通用：运行并实时输出
       ------------------------------------------------- */
    private void runInFrame(List<String> cmd, File workDir, String title) {
        JInternalFrame frame = new JInternalFrame(title + " #" + (++runCount),
                true, true, true, true);
        JTextArea text = new JTextArea(20, 80);
        text.setEditable(false);
        text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        frame.add(new JScrollPane(text));
        frame.setSize(700, 500);
        frame.setLocation(30 * (runCount % 8), 30 * (runCount % 8));
        desktop.add(frame);
        frame.setVisible(true);
        try { frame.setSelected(true); } catch (Exception ignored) {}

        final Process[] proc = new Process[1];
        frame.addInternalFrameListener(new javax.swing.event.InternalFrameAdapter() {
            public void internalFrameClosing(javax.swing.event.InternalFrameEvent e) {
                if (proc[0] != null) proc[0].destroyForcibly();
            }
        });

        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.directory(workDir);
                pb.redirectErrorStream(true);
                proc[0] = pb.start();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(proc[0].getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        final String l = line;
                        SwingUtilities.invokeLater(() -> {
                            text.append(l + "\n");
                            text.setCaretPosition(text.getDocument().getLength());
                        });
                    }
                }
                int exit = proc[0].waitFor();
                SwingUtilities.invokeLater(() ->
                        text.append("\n=== 进程结束，退出码：" + exit + " ===\n"));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() ->
                        text.append("\n启动失败：" + ex.getMessage()));
            }
        });
    }

    /* ---------- 工具 ---------- */
    private static JMenuItem newItem(String txt, ActionListener al) {
        JMenuItem mi = new JMenuItem(txt);
        mi.addActionListener(al);
        return mi;
    }

    private static List<String> parseCommand(String raw) {
        List<String> list = new ArrayList<>();
        boolean inQuote = false;
        char quoteChar = 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if ((ch == '"' || ch == '\'') && !inQuote) {
                inQuote = true; quoteChar = ch;
            } else if (ch == quoteChar && inQuote) {
                inQuote = false;
            } else if (ch == ' ' && !inQuote) {
                if (sb.length() > 0) { list.add(sb.toString()); sb.setLength(0); }
            } else sb.append(ch);
        }
        if (sb.length() > 0) list.add(sb.toString());
        return list;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MDILauncher().setVisible(true));
    }
}