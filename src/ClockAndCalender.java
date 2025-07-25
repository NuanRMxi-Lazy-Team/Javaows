import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.OceanTheme;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class ClockAndCalender extends JFrame {
    private JPanel clockPanel;
    private JPanel calendarPanel;
    private Timer timer;
    private boolean isAnalogClock = true;
    private AnalogClockPanel analogClock;
    private JLabel digitalClock;
    private JButton toggleButton;
    private JLabel[][] dayLabels;
    private Calendar calendar;

    // Metal主题颜色
    private static final Color METAL_BLUE = new Color(102, 153, 204);
    private static final Color METAL_GRAY = new Color(204, 204, 204);
    private static final Color METAL_DARK_GRAY = new Color(102, 102, 102);
    private static final Color METAL_LIGHT_GRAY = new Color(238, 238, 238);
    private static final Color METAL_SILVER = new Color(192, 192, 192);

    public ClockAndCalender() {
        setupMetalLookAndFeel();
        initializeComponents();
        setupLayout();
        startTimer();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("日期和时间");
        setSize(850, 520);
        setLocationRelativeTo(null);
        setResizable(false);

        // 应用Metal主题到所有组件
        SwingUtilities.updateComponentTreeUI(this);
    }

    private void setupMetalLookAndFeel() {
        try {
            // 设置Metal主题
            MetalLookAndFeel.setCurrentTheme(new OceanTheme());
            UIManager.setLookAndFeel(new MetalLookAndFeel());

            // 自定义Metal样式
            UIManager.put("Panel.background", METAL_LIGHT_GRAY);
            UIManager.put("Label.foreground", Color.BLACK);
            UIManager.put("Label.background", METAL_LIGHT_GRAY);

            // 按钮样式
            UIManager.put("Button.background", METAL_SILVER);
            UIManager.put("Button.foreground", Color.BLACK);
            UIManager.put("Button.focus", METAL_BLUE);
            UIManager.put("Button.select", METAL_BLUE);

            // 边框样式
            UIManager.put("TitledBorder.titleColor", Color.BLACK);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initializeComponents() {
        calendar = Calendar.getInstance();

        // 初始化时钟面板
        clockPanel = new JPanel(new CardLayout());
        clockPanel.setBackground(METAL_LIGHT_GRAY);

        analogClock = new AnalogClockPanel();
        digitalClock = new JLabel();
        digitalClock.setFont(new Font("SansSerif", Font.BOLD, 36));
        digitalClock.setHorizontalAlignment(JLabel.CENTER);
        digitalClock.setForeground(Color.BLACK);
        digitalClock.setBackground(METAL_LIGHT_GRAY);
        digitalClock.setOpaque(true);

        clockPanel.add(analogClock, "analog");
        clockPanel.add(digitalClock, "digital");

        // 切换按钮
        toggleButton = new JButton("切换到数字时钟");
        styleMetalButton(toggleButton);
        toggleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleClock();
            }
        });

        // 初始化日历面板
        calendarPanel = createCalendarPanel();
    }

    private void styleMetalButton(JButton button) {
        button.setBackground(METAL_SILVER);
        button.setForeground(Color.BLACK);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setBorder(BorderFactory.createRaisedBevelBorder());
        button.setPreferredSize(new Dimension(150, 32));
        button.setFocusPainted(false);

        // 添加鼠标悬停效果
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(METAL_BLUE);
                button.setForeground(Color.WHITE);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(METAL_SILVER);
                button.setForeground(Color.BLACK);
            }
        });
    }

    private void setupLayout() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(METAL_LIGHT_GRAY);

        // 左侧时钟区域
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBackground(METAL_LIGHT_GRAY);
        leftPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createRaisedBevelBorder(),
            "时钟",
            0, 0,
            new Font("SansSerif", Font.BOLD, 16),
            Color.BLACK));
        leftPanel.setPreferredSize(new Dimension(380, 480));

        // 时钟面板居中显示
        JPanel clockContainer = new JPanel(new BorderLayout());
        clockContainer.setBackground(METAL_LIGHT_GRAY);
        clockContainer.add(clockPanel, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(METAL_LIGHT_GRAY);
        buttonPanel.add(toggleButton);

        leftPanel.add(clockContainer, BorderLayout.CENTER);
        leftPanel.add(buttonPanel, BorderLayout.SOUTH);

        // 右侧日历区域
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(METAL_LIGHT_GRAY);
        rightPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createRaisedBevelBorder(),
            "日历",
            0, 0,
            new Font("SansSerif", Font.BOLD, 16),
            Color.BLACK));
        rightPanel.add(calendarPanel, BorderLayout.CENTER);

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);
    }

    private JPanel createCalendarPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(METAL_LIGHT_GRAY);

        // 月份年份显示
        JLabel monthYearLabel = new JLabel();
        monthYearLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        monthYearLabel.setHorizontalAlignment(JLabel.CENTER);
        monthYearLabel.setForeground(Color.BLACK);
        monthYearLabel.setBackground(METAL_LIGHT_GRAY);
        monthYearLabel.setOpaque(true);
        updateMonthYearLabel(monthYearLabel);

        // 月份切换按钮
        JButton prevButton = new JButton("◀");
        JButton nextButton = new JButton("▶");

        styleMetalButton(prevButton);
        styleMetalButton(nextButton);

        prevButton.setPreferredSize(new Dimension(50, 32));
        nextButton.setPreferredSize(new Dimension(50, 32));

        prevButton.addActionListener(e -> {
            calendar.add(Calendar.MONTH, -1);
            updateCalendar();
            updateMonthYearLabel(monthYearLabel);
        });

        nextButton.addActionListener(e -> {
            calendar.add(Calendar.MONTH, 1);
            updateCalendar();
            updateMonthYearLabel(monthYearLabel);
        });

        JPanel monthPanel = new JPanel(new BorderLayout());
        monthPanel.setBackground(METAL_LIGHT_GRAY);
        monthPanel.add(prevButton, BorderLayout.WEST);
        monthPanel.add(monthYearLabel, BorderLayout.CENTER);
        monthPanel.add(nextButton, BorderLayout.EAST);
        monthPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 日历网格
        JPanel gridPanel = new JPanel(new GridLayout(7, 7, 3, 3));
        gridPanel.setBackground(METAL_LIGHT_GRAY);
        gridPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 星期标题
        String[] weekdays = {"日", "一", "二", "三", "四", "五", "六"};
        for (String day : weekdays) {
            JLabel label = new JLabel(day, JLabel.CENTER);
            label.setFont(new Font("SansSerif", Font.BOLD, 16));
            label.setOpaque(true);
            label.setBackground(METAL_GRAY);
            label.setForeground(Color.BLACK);
            label.setBorder(BorderFactory.createRaisedBevelBorder());
            label.setPreferredSize(new Dimension(55, 45));
            gridPanel.add(label);
        }

        // 日期标签
        dayLabels = new JLabel[6][7];
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 7; j++) {
                dayLabels[i][j] = new JLabel("", JLabel.CENTER);
                dayLabels[i][j].setFont(new Font("SansSerif", Font.PLAIN, 14));
                dayLabels[i][j].setOpaque(true);
                dayLabels[i][j].setBackground(Color.WHITE);
                dayLabels[i][j].setForeground(Color.BLACK);
                dayLabels[i][j].setBorder(BorderFactory.createLoweredBevelBorder());
                dayLabels[i][j].setPreferredSize(new Dimension(55, 45));
                gridPanel.add(dayLabels[i][j]);
            }
        }

        updateCalendar();

        panel.add(monthPanel, BorderLayout.NORTH);
        panel.add(gridPanel, BorderLayout.CENTER);

        return panel;
    }

    private void updateMonthYearLabel(JLabel label) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy年 MM月");
        label.setText(format.format(calendar.getTime()));
    }

    private void updateCalendar() {
        Calendar tempCal = (Calendar) calendar.clone();
        tempCal.set(Calendar.DAY_OF_MONTH, 1);

        int firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 1;
        int daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH);

        // 清空所有标签
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 7; j++) {
                dayLabels[i][j].setText("");
                dayLabels[i][j].setBackground(Color.WHITE);
                dayLabels[i][j].setForeground(Color.BLACK);
                dayLabels[i][j].setFont(new Font("SansSerif", Font.PLAIN, 14));
                dayLabels[i][j].setBorder(BorderFactory.createLoweredBevelBorder());
            }
        }

        // 填充日期
        int day = 1;
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 7; j++) {
                if (i == 0 && j < firstDayOfWeek) {
                    continue;
                }
                if (day > daysInMonth) {
                    break;
                }

                dayLabels[i][j].setText(String.valueOf(day));

                // 高亮今天
                Calendar today = Calendar.getInstance();
                if (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                    day == today.get(Calendar.DAY_OF_MONTH)) {
                    dayLabels[i][j].setBackground(METAL_BLUE);
                    dayLabels[i][j].setForeground(Color.WHITE);
                    dayLabels[i][j].setFont(new Font("SansSerif", Font.BOLD, 16));
                    dayLabels[i][j].setBorder(BorderFactory.createRaisedBevelBorder());
                }

                day++;
            }
        }
    }

    private void toggleClock() {
        CardLayout cl = (CardLayout) clockPanel.getLayout();
        if (isAnalogClock) {
            cl.show(clockPanel, "digital");
            toggleButton.setText("切换到指针时钟");
        } else {
            cl.show(clockPanel, "analog");
            toggleButton.setText("切换到数字时钟");
        }
        isAnalogClock = !isAnalogClock;
    }

    private void startTimer() {
        timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateTime();
            }
        });
        timer.start();
    }

    private void updateTime() {
    Date now = new Date();

    // 更新数字时钟
    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    digitalClock.setText(timeFormat.format(now));

    // 更新指针时钟
    analogClock.setTime(now);

}

    // 指针时钟面板
    private class AnalogClockPanel extends JPanel {
        private Date time;

        public AnalogClockPanel() {
            setPreferredSize(new Dimension(300, 300));
            setBackground(METAL_LIGHT_GRAY);
            time = new Date();
        }

        public void setTime(Date time) {
            this.time = time;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int centerX = width / 2;
            int centerY = height / 2;
            int radius = Math.min(width, height) / 2 - 35;

            // 绘制表盘背景（Metal风格）
            g2d.setColor(Color.WHITE);
            g2d.fillOval(centerX - radius, centerY - radius, 2 * radius, 2 * radius);

            // 绘制Metal风格的外圈
            g2d.setColor(METAL_GRAY);
            g2d.setStroke(new BasicStroke(4));
            g2d.drawOval(centerX - radius, centerY - radius, 2 * radius, 2 * radius);

            // 绘制内圈
            g2d.setColor(METAL_DARK_GRAY);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(centerX - radius + 5, centerY - radius + 5, 2 * radius - 10, 2 * radius - 10);

            // 绘制时刻标记
            g2d.setStroke(new BasicStroke(3));
            g2d.setColor(Color.BLACK);
            for (int i = 0; i < 12; i++) {
                double angle = i * Math.PI / 6;
                int x1 = centerX + (int) ((radius - 20) * Math.sin(angle));
                int y1 = centerY - (int) ((radius - 20) * Math.cos(angle));
                int x2 = centerX + (int) ((radius - 8) * Math.sin(angle));
                int y2 = centerY - (int) ((radius - 8) * Math.cos(angle));
                g2d.drawLine(x1, y1, x2, y2);

                // 绘制数字
                g2d.setFont(new Font("SansSerif", Font.BOLD, 18));
                String number = String.valueOf(i == 0 ? 12 : i);
                FontMetrics fm = g2d.getFontMetrics();
                int numX = centerX + (int) ((radius - 35) * Math.sin(angle)) - fm.stringWidth(number) / 2;
                int numY = centerY - (int) ((radius - 35) * Math.cos(angle)) + fm.getHeight() / 4;
                g2d.drawString(number, numX, numY);
            }

            // 绘制分钟标记
            g2d.setStroke(new BasicStroke(1));
            g2d.setColor(METAL_DARK_GRAY);
            for (int i = 0; i < 60; i++) {
                if (i % 5 != 0) {
                    double angle = i * Math.PI / 30;
                    int x1 = centerX + (int) ((radius - 12) * Math.sin(angle));
                    int y1 = centerY - (int) ((radius - 12) * Math.cos(angle));
                    int x2 = centerX + (int) ((radius - 6) * Math.sin(angle));
                    int y2 = centerY - (int) ((radius - 6) * Math.cos(angle));
                    g2d.drawLine(x1, y1, x2, y2);
                }
            }

            // 获取当前时间
            Calendar cal = Calendar.getInstance();
            cal.setTime(time);
            int hour = cal.get(Calendar.HOUR);
            int minute = cal.get(Calendar.MINUTE);
            int second = cal.get(Calendar.SECOND);

            // 绘制时针（Metal风格）
            double hourAngle = (hour + minute / 60.0) * Math.PI / 6;
            int hourX = centerX + (int) ((radius * 0.5) * Math.sin(hourAngle));
            int hourY = centerY - (int) ((radius * 0.5) * Math.cos(hourAngle));
            g2d.setColor(METAL_DARK_GRAY);
            g2d.setStroke(new BasicStroke(8));
            g2d.drawLine(centerX, centerY, hourX, hourY);

            // 绘制分针
            double minuteAngle = minute * Math.PI / 30;
            int minuteX = centerX + (int) ((radius * 0.7) * Math.sin(minuteAngle));
            int minuteY = centerY - (int) ((radius * 0.7) * Math.cos(minuteAngle));
            g2d.setColor(METAL_DARK_GRAY);
            g2d.setStroke(new BasicStroke(6));
            g2d.drawLine(centerX, centerY, minuteX, minuteY);

            // 绘制秒针
            double secondAngle = second * Math.PI / 30;
            int secondX = centerX + (int) ((radius * 0.8) * Math.sin(secondAngle));
            int secondY = centerY - (int) ((radius * 0.8) * Math.cos(secondAngle));
            g2d.setColor(METAL_BLUE);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawLine(centerX, centerY, secondX, secondY);

            // 绘制中心点（Metal风格）
            g2d.setColor(METAL_DARK_GRAY);
            g2d.fillOval(centerX - 10, centerY - 10, 20, 20);
            g2d.setColor(METAL_BLUE);
            g2d.fillOval(centerX - 6, centerY - 6, 12, 12);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ClockAndCalender().setVisible(true);
        });
    }
}