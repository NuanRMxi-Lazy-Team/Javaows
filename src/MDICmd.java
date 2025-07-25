import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;

public class MDICmd extends JFrame {
    private final JDesktopPane desktop = new JDesktopPane();
    private int winCount = 0;

    /* ---------------- 支持的 Shell 定义 ---------------- */
    private enum Shell {
        CMD("cmd", "cmd.exe", "cmd.exe /c echo test"),
        POWERSHELL("powershell", "powershell.exe", "powershell.exe -Command echo test"),
        BASH("bash", "bash", "bash -c 'echo test'"),
        ZSH("zsh", "zsh", "zsh -c 'echo test'"),
        SH("sh", "sh", "sh -c 'echo test'");

        final String label;
        final String exe;
        final String detectCmd;

        Shell(String label, String exe, String detectCmd) {
            this.label = label;
            this.exe = exe;
            this.detectCmd = detectCmd;
        }

        /* 判断当前系统是否可用 */
        boolean available() {
            try {
                // 根据操作系统选择合适的检测方式
                String os = System.getProperty("os.name").toLowerCase();
                Process p;

                if (os.contains("windows")) {
                    // Windows系统
                    if (this == CMD || this == POWERSHELL) {
                        p = new ProcessBuilder(detectCmd.split(" "))
                                .redirectErrorStream(true)
                                .start();
                    } else {
                        return false; // Windows下不支持bash/zsh
                    }
                } else {
                    // Unix-like系统 (Linux, macOS等)
                    if (this == CMD || this == POWERSHELL) {
                        return false; // Linux下不支持cmd/powershell
                    } else {
                        // 使用which命令检测
                        p = new ProcessBuilder("which", exe)
                                .redirectErrorStream(true)
                                .start();
                    }
                }

                p.waitFor();
                return p.exitValue() == 0;
            } catch (Exception e) {
                return false;
            }
        }

        /* 构造启动命令 */
        List<String> buildCmd() {
            List<String> cmd = new ArrayList<>();
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("windows")) {
                // Windows系统
                if (this == CMD) {
                    cmd.addAll(Arrays.asList("cmd", "/c", "chcp 65001 > nul && cmd"));
                } else if (this == POWERSHELL) {
                    cmd.addAll(Arrays.asList("powershell", "-NoLogo", "-NoExit",
                            "-Command", "chcp 65001 > nul; powershell"));
                }
            } else {
                // Unix-like系统 (Linux, macOS等)
                if (this == BASH) {
                    cmd.addAll(Arrays.asList("bash", "-i")); // 交互式bash
                } else if (this == ZSH) {
                    cmd.addAll(Arrays.asList("zsh", "-i")); // 交互式zsh
                } else if (this == SH) {
                    cmd.addAll(Arrays.asList("sh", "-i")); // 交互式sh
                }
            }
            return cmd;
        }
    }

    public MDICmd() {
        super("Javaonsole");
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        } catch (Exception ignored) {}
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        /* ---------- 菜单：只列出当前系统可用的 shell ---------- */
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("文件");

        // 添加一个系统信息菜单项
        String os = System.getProperty("os.name");
        file.add(newItem("系统信息: " + os, e ->
            JOptionPane.showMessageDialog(this,
                "操作系统: " + System.getProperty("os.name") + "\n" +
                "版本: " + System.getProperty("os.version") + "\n" +
                "架构: " + System.getProperty("os.arch"),
                "系统信息", JOptionPane.INFORMATION_MESSAGE)));
        file.addSeparator();

        // 添加可用的shell
        boolean hasShell = false;
        for (Shell s : Shell.values()) {
            if (s.available()) {
                file.add(newItem("新建 " + s.label, e -> newTerm(s)));
                hasShell = true;
            }
        }

        if (!hasShell) {
            JMenuItem noShell = new JMenuItem("没有可用的Shell");
            noShell.setEnabled(false);
            file.add(noShell);
        }

        bar.add(file);
        setJMenuBar(bar);

        add(desktop);
    }

    /* ----------------------------------------------------- */
    private void newTerm(Shell shell) {
        JInternalFrame frame = new JInternalFrame(shell.label + " #" + (++winCount),
                true, true, true, true);

        JTextArea output = new JTextArea(20, 80);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        output.setEditable(false);
        output.setBackground(Color.BLACK);
        output.setForeground(Color.WHITE);

        JTextField input = new JTextField();
        input.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        input.setBorder(BorderFactory.createTitledBorder("输入后回车"));
        input.setBackground(Color.WHITE);

        frame.add(new JScrollPane(output), BorderLayout.CENTER);
        frame.add(input, BorderLayout.SOUTH);
        frame.setSize(650, 450);
        frame.setLocation(30 * (winCount % 8), 30 * (winCount % 8));
        desktop.add(frame);
        frame.setVisible(true);
        try { frame.setSelected(true); } catch (Exception ignored) {}

        startShell(frame, shell, output, input);
    }

    /* ----------------------------------------------------- */
    private void startShell(JInternalFrame frame, Shell shell,
                            JTextArea output, JTextField input) {
        final Process[] proc = new Process[1];
        frame.addInternalFrameListener(new javax.swing.event.InternalFrameAdapter() {
            public void internalFrameClosing(javax.swing.event.InternalFrameEvent e) {
                if (proc[0] != null) {
                    proc[0].destroyForcibly();
                }
            }
        });

        new Thread(() -> {
            try {
                // 构建启动命令
                List<String> cmd = shell.buildCmd();
                if (cmd.isEmpty()) {
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(
                                    frame,
                                    "不支持的Shell: " + shell.label,
                                    "错误",
                                    JOptionPane.ERROR_MESSAGE));
                    return;
                }

                // 启动进程
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.redirectErrorStream(true);
                pb.directory(new File(System.getProperty("user.home"))); // 使用用户主目录

                // 设置环境变量
                Map<String, String> env = pb.environment();
                env.put("TERM", "xterm"); // 设置终端类型
                if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
                    env.put("LANG", "zh_CN.UTF-8"); // Linux下设置中文UTF-8
                    env.put("LC_ALL", "zh_CN.UTF-8");
                }

                proc[0] = pb.start();

                SwingUtilities.invokeLater(() ->
                    output.append("启动 " + shell.label + " 终端...\n"));

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
                    } catch (IOException e) {
                        SwingUtilities.invokeLater(() ->
                            output.append("读取输出流错误: " + e.getMessage() + "\n"));
                    }

                    SwingUtilities.invokeLater(() ->
                        output.append("\n[进程已结束]\n"));
                }).start();

                /* 发送输入（UTF-8） */
                OutputStream os = proc[0].getOutputStream();
                input.addActionListener(e -> {
                    try {
                        String command = input.getText();
                        if (!command.trim().isEmpty()) {
                            os.write((command + "\n").getBytes(StandardCharsets.UTF_8));
                            os.flush();
                            input.setText("");

                            // 在输出区显示用户输入的命令
                            SwingUtilities.invokeLater(() ->
                                output.append("$ " + command + "\n"));
                        }
                    } catch (IOException ex) {
                        SwingUtilities.invokeLater(() ->
                            output.append("发送命令失败: " + ex.getMessage() + "\n"));
                    }
                });

                // 设置输入焦点
                SwingUtilities.invokeLater(() -> input.requestFocus());

            } catch (IOException ex) {
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(
                                frame,
                                "启动失败: " + ex.getMessage() + "\n" +
                                "请确保系统已安装 " + shell.label,
                                "错误",
                                JOptionPane.ERROR_MESSAGE));
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