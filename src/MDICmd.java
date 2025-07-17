import java.util.List;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MDICmd extends JFrame {
    private final JDesktopPane desktop = new JDesktopPane();
    private int winCount = 0;

    public MDICmd() {
        super("MDI 中文命令行终端");
        try { UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel"); }
        catch (Exception ignored) {}
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        /* ---------- 菜单 ---------- */
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("文件");
        file.add(newItem("新建 CMD",        e -> newTerm("cmd")));
        file.add(newItem("新建 PowerShell", e -> newTerm("powershell")));
        bar.add(file);
        setJMenuBar(bar);

        /* ---------- MDI 桌面 ---------- */
        add(desktop);
    }

    /* -------------------------------------------------
       创建新的内部窗口 + 启动终端
       ------------------------------------------------- */
    private void newTerm(String shell) {
        JInternalFrame frame = new JInternalFrame(shell + " #" + (++winCount),
                true, true, true, true);

        JTextArea output = new JTextArea(20, 80);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        output.setEditable(false);

        JTextField input = new JTextField();
        input.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        input.setBorder(BorderFactory.createTitledBorder("输入后回车"));

        frame.add(new JScrollPane(output), BorderLayout.CENTER);
        frame.add(input, BorderLayout.SOUTH);
        frame.setSize(650, 450);
        frame.setLocation(30 * (winCount % 8), 30 * (winCount % 8));
        desktop.add(frame);
        frame.setVisible(true);
        try { frame.setSelected(true); } catch (Exception ignored) {}

        startShell(frame, shell, output, input);
    }

    /* -------------------------------------------------
       启动子进程 + 双向 UTF-8 中文交互
       ------------------------------------------------- */
    private void startShell(JInternalFrame frame, String shell,
                            JTextArea output, JTextField input) {
        ArrayList<String> cmd = new ArrayList<>();

        if (shell.equalsIgnoreCase("powershell")) {
            cmd.addAll(Arrays.asList(
                    "powershell", "-NoLogo", "-NoExit",
                    "-Command", "chcp 65001 > nul; powershell"));
        } else {
            cmd.addAll(Arrays.asList("cmd", "/c", "chcp 65001 > nul && cmd"));
        }

        final Process[] proc = new Process[1];
        frame.addInternalFrameListener(new javax.swing.event.InternalFrameAdapter() {
            public void internalFrameClosing(javax.swing.event.InternalFrameEvent e) {
                if (proc[0] != null) proc[0].destroyForcibly();
            }
        });

        new Thread(() -> {
            try {
                proc[0] = new ProcessBuilder(cmd)
                        .redirectErrorStream(true)
                        .directory(new File("."))
                        .start();

                /* 读取输出（UTF-8） */
                new Thread(() -> {
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(proc[0].getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            String finalLine = line;
                            SwingUtilities.invokeLater(() -> {
                                output.append(finalLine + "\n");
                                output.setCaretPosition(output.getDocument().getLength());
                            });
                        }
                    } catch (IOException ignored) {}
                }).start();

                /* 发送输入（UTF-8） */
                OutputStream os = proc[0].getOutputStream();
                input.addActionListener(e -> {
                    try {
                        os.write((input.getText() + "\r\n").getBytes(StandardCharsets.UTF_8));
                        os.flush();
                        input.setText("");
                    } catch (IOException ex) {
                        output.append("无法发送命令\n");
                    }
                });
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() ->
                        output.append("启动失败：" + ex.getMessage()));
            }
        }).start();
    }

    /* ---------- 工具 ---------- */
    private static JMenuItem newItem(String txt, ActionListener al) {
        JMenuItem mi = new JMenuItem(txt);
        mi.addActionListener(al);
        return mi;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MDICmd().setVisible(true));
    }
}