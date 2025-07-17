import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Calc extends JFrame {
    private JTextField display;
    private double current = 0, memory = 0;
    private char op = ' ';
    private boolean fresh = true;

    public Calc() {
        super("计算器");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(300, 450);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // 显示框
        display = new JTextField("0");
        display.setEditable(false);
        display.setHorizontalAlignment(SwingConstants.RIGHT);
        display.setFont(new Font("SansSerif", Font.BOLD, 28));
        add(display, BorderLayout.NORTH);

        // 按钮面板
        JPanel panel = new JPanel(new GridLayout(6, 4, 5, 5));
       String[] labels = {
        // 第 1 行
        "MC", "MR", "M+", "M-",
         // 第 2 行
        "%",  "CE", "C",  "÷",
        // 第 3 行
        "7",  "8",  "9",  "×",
        // 第 4 行
        "4",  "5",  "6",  "-",
        // 第 5 行
        "1",  "2",  "3",  "+",
               // 第 6 行
         "±",  "0",  ".",  "="
        };

        for (String s : labels) {
            JButton btn = new JButton(s);
            btn.setFont(new Font("SansSerif", Font.PLAIN, 20));
            btn.addActionListener(new ButtonListener());
            panel.add(btn);
        }
        add(panel, BorderLayout.CENTER);
    }

    private class ButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String cmd = e.getActionCommand();
            switch (cmd) {
                case "0": case "1": case "2": case "3": case "4":
                case "5": case "6": case "7": case "8": case "9":
                    if (fresh) { display.setText(cmd); fresh = false; }
                    else display.setText(display.getText() + cmd);
                    break;
                case ".":
                    if (fresh) { display.setText("0."); fresh = false; }
                    else if (!display.getText().contains("."))
                        display.setText(display.getText() + ".");
                    break;
                case "+": case "-": case "×": case "÷":
                    if (!fresh) calculate();
                    op = cmd.charAt(0);
                    fresh = true;
                    break;
                case "=":
                    calculate();
                    op = ' ';
                    fresh = true;
                    break;
                case "C":
                    display.setText("0");
                    current = 0; op = ' '; fresh = true;
                    break;
                case "CE":
                    display.setText("0");
                    fresh = true;
                    break;
                case "±":
                    double v = Double.parseDouble(display.getText());
                    display.setText(String.valueOf(-v));
                    break;
                case "%":
                    double pct = Double.parseDouble(display.getText());
                    display.setText(String.valueOf(pct / 100));
                    break;
                case "MC": memory = 0; break;
                case "MR": display.setText(String.valueOf(memory)); fresh = true; break;
                case "M+": memory += Double.parseDouble(display.getText()); break;
                case "M-": memory -= Double.parseDouble(display.getText()); break;
            }
        }
    }

    private void calculate() {
        double num = Double.parseDouble(display.getText());
        switch (op) {
            case '+': current += num; break;
            case '-': current -= num; break;
            case '×': current *= num; break;
            case '÷': current /= num; break;
            default: current = num;
        }
        display.setText(String.valueOf(current));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Calc().setVisible(true));
    }
}