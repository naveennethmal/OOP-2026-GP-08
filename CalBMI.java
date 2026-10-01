import javax.swing.*;
import java.awt.*;

public class CalBMI extends JFrame {

    JLabel titleLabel, weightLabel, heightLabel;
    JTextField weightField, heightField;
    JRadioButton metricRadio, englishRadio;
    JButton calculateButton, clearButton;
    JLabel bmiValueLabel, resultValueLabel, bmiTypeLabel;
    JLabel heightResultLabel, weightResultLabel;
    JButton backButton;
    CardLayout cardLayout;
    JPanel cardPanel;

    public CalBMI() {
        setTitle("Body Mass Calculator APP");
        setSize(700, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        JPanel calculatorPanel = createCalculatorPanel();
        JPanel resultPanel = createResultPanel();

        cardPanel.add(calculatorPanel, "calculator");
        cardPanel.add(resultPanel, "result");
        add(cardPanel);

        cardLayout.show(cardPanel, "calculator");
        setVisible(true);
    }

    private JPanel createCalculatorPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);

        titleLabel = new JLabel("Body Mass Calculator APP");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(25, 10, 25, 10));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 0, 0), 1),
                BorderFactory.createEmptyBorder(25, 35, 25, 35)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        metricRadio = new JRadioButton("Metric (kg, cm)");
        englishRadio = new JRadioButton("English (lb, inch)");

        metricRadio.setFont(new Font("Arial", Font.PLAIN, 16));
        englishRadio.setFont(new Font("Arial", Font.PLAIN, 16));
        metricRadio.setBackground(Color.WHITE);
        englishRadio.setBackground(Color.WHITE);
        metricRadio.setSelected(true);

        ButtonGroup unitGroup = new ButtonGroup();
        unitGroup.add(metricRadio);
        unitGroup.add(englishRadio);

        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(metricRadio, gbc);

        gbc.gridx = 1;
        inputPanel.add(englishRadio, gbc);

        weightLabel = new JLabel("Your weight:");
        weightLabel.setFont(new Font("Arial", Font.BOLD, 17));

        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(weightLabel, gbc);

        weightField = new JTextField();
        weightField.setFont(new Font("Arial", Font.PLAIN, 17));

        gbc.gridx = 1;
        inputPanel.add(weightField, gbc);

        heightLabel = new JLabel("Your height:");
        heightLabel.setFont(new Font("Arial", Font.BOLD, 17));

        gbc.gridx = 0;
        gbc.gridy = 2;
        inputPanel.add(heightLabel, gbc);

        heightField = new JTextField();
        heightField.setFont(new Font("Arial", Font.PLAIN, 17));

        gbc.gridx = 1;
        inputPanel.add(heightField, gbc);

        clearButton = new JButton("Clear");
        clearButton.setFont(new Font("Arial", Font.BOLD, 16));
        clearButton.setFocusPainted(false);

        gbc.gridx = 0;
        gbc.gridy = 3;
        inputPanel.add(clearButton, gbc);

        calculateButton = new JButton("Calculate BMI");
        calculateButton.setFont(new Font("Arial", Font.BOLD, 16));
        calculateButton.setFocusPainted(false);

        gbc.gridx = 1;
        inputPanel.add(calculateButton, gbc);

        mainPanel.add(inputPanel, BorderLayout.CENTER);

        clearButton.addActionListener(e -> {
            weightField.setText("");
            heightField.setText("");
            weightField.requestFocus();
        });

        calculateButton.addActionListener(e -> calculateBMI());

        return mainPanel;
    }

    private void calculateBMI() {
        try {
            double weight = Double.parseDouble(weightField.getText().trim());
            double height = Double.parseDouble(heightField.getText().trim());

            if (weight <= 0 || height <= 0) {
                JOptionPane.showMessageDialog(
                        this,
                        "Weight and Height must be greater than 0.",
                        "Invalid Input",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            double bmi;

            if (metricRadio.isSelected()) {
                double heightMeter = height / 100.0;
                bmi = weight / (heightMeter * heightMeter);
            } else {
                bmi = (703.0 * weight) / (height * height);
            }

            String category;

            if (bmi < 18.5) {
                category = "Underweight";
                resultValueLabel.setForeground(new Color(188, 161, 9));

            } else if (bmi < 25.0) {
                category = "Normal";
                resultValueLabel.setForeground(new Color(10, 117, 10));

            } else if (bmi < 30.0) {
                category = "Overweight";
                resultValueLabel.setForeground(new Color(200, 118, 36));

            } else {
                category = "Obese";
                resultValueLabel.setForeground(new Color(139, 0, 0));
            }

            resultValueLabel.setText(category);


            bmiValueLabel.setText(String.format("%.2f", bmi));
            resultValueLabel.setText(category);

            if (metricRadio.isSelected()) {
                bmiTypeLabel.setText("Metric (kg, cm)");
                heightResultLabel.setText("Height = " + height + " cm");
                weightResultLabel.setText("Weight = " + weight + " kg");
            } else {
                bmiTypeLabel.setText("English (lb, inch)");
                heightResultLabel.setText("Height = " + height + " inch");
                weightResultLabel.setText("Weight = " + weight + " lb");
            }

            cardLayout.show(cardPanel, "result");

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please enter valid numbers.\n\nExample:\nWeight: 60\nHeight: 170",
                    "Invalid Input",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createResultPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);

        JLabel resultTitle = new JLabel("Body Mass Index Result");
        resultTitle.setFont(new Font("Arial", Font.PLAIN, 28));
        resultTitle.setHorizontalAlignment(SwingConstants.CENTER);
        resultTitle.setBorder(BorderFactory.createEmptyBorder(25, 10, 20, 10));
        mainPanel.add(resultTitle, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(Color.WHITE);

        JPanel resultBox = new JPanel(new GridLayout(3, 2, 10, 10));
        resultBox.setBackground(Color.WHITE);
        resultBox.setPreferredSize(new Dimension(350, 170));
        resultBox.setMaximumSize(new Dimension(350, 170));
        resultBox.setMinimumSize(new Dimension(350, 170));

        resultBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK, 1),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)));

        JLabel yourBMI = new JLabel("Your BMI");
        yourBMI.setFont(new Font("Arial", Font.PLAIN, 18));

        bmiValueLabel = new JLabel("0.00");
        bmiValueLabel.setFont(new Font("Arial", Font.BOLD, 18));
        bmiValueLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        resultBox.add(yourBMI);
        resultBox.add(bmiValueLabel);

        JLabel resultLabel = new JLabel("Result");
        resultLabel.setFont(new Font("Arial", Font.PLAIN, 18));

        resultValueLabel = new JLabel("-");
        resultValueLabel.setFont(new Font("Arial", Font.BOLD, 18));
        resultValueLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        resultBox.add(resultLabel);
        resultBox.add(resultValueLabel);

        JLabel typeLabel = new JLabel("BMI type");
        typeLabel.setFont(new Font("Arial", Font.PLAIN, 18));

        bmiTypeLabel = new JLabel("-");
        bmiTypeLabel.setFont(new Font("Arial", Font.BOLD, 16));
        bmiTypeLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        resultBox.add(typeLabel);
        resultBox.add(bmiTypeLabel);

        centerPanel.add(resultBox);
        centerPanel.add(Box.createVerticalStrut(20));

        JPanel inputResultPanel = new JPanel(new GridLayout(1, 2));
        inputResultPanel.setBackground(Color.WHITE);

        heightResultLabel = new JLabel("Height = Input value");
        heightResultLabel.setFont(new Font("Arial", Font.PLAIN, 16));

        weightResultLabel = new JLabel("Weight = Input value");
        weightResultLabel.setFont(new Font("Arial", Font.PLAIN, 16));

        inputResultPanel.add(heightResultLabel);
        inputResultPanel.add(weightResultLabel);
        centerPanel.add(inputResultPanel);

        centerPanel.add(Box.createVerticalStrut(20));

        JLabel bmiValuesTitle = new JLabel("BMI VALUES");
        bmiValuesTitle.setFont(new Font("Arial", Font.BOLD, 18));
        bmiValuesTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(bmiValuesTitle);

        JPanel valuesPanel = new JPanel(new GridLayout(4, 1, 0, 5));
        valuesPanel.setBackground(Color.WHITE);

        JLabel underweightLabel = new JLabel(
                "Underweight : less than 18.5");
        JLabel normalLabel = new JLabel(
                "Normal : between 18.5 and 24.9");
        JLabel overweightLabel = new JLabel(
                "Overweight : between 25 and 29.9");
        JLabel obeseLabel = new JLabel(
                "Obese : 30 or greater");


        Font bmiFont = new Font("Arial", Font.PLAIN, 15);

        underweightLabel.setFont(bmiFont);
        normalLabel.setFont(bmiFont);
        overweightLabel.setFont(bmiFont);
        obeseLabel.setFont(bmiFont);

        valuesPanel.add(underweightLabel);
        valuesPanel.add(normalLabel);
        valuesPanel.add(overweightLabel);
        valuesPanel.add(obeseLabel);

        centerPanel.add(valuesPanel);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        backButton = new JButton("Back");
        backButton.setFont(new Font("Arial", Font.BOLD, 16));
        backButton.setFocusPainted(false);

        JPanel backPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        backPanel.setBackground(Color.WHITE);
        backPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 25, 30));
        backPanel.add(backButton);

        mainPanel.add(backPanel, BorderLayout.SOUTH);

        backButton.addActionListener(e ->
                cardLayout.show(cardPanel, "calculator"));

        return mainPanel;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CalBMI());
    }
}
