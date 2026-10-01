package com.healthfirst.pims;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;

public final class LoginFrame extends JFrame {
    private final JTextField username = new JTextField(24);
    private final JPasswordField password = new JPasswordField(24);
    private final JButton signIn = Ui.primary("Sign in  →");
    private final JLabel status = Ui.muted("Secure access for your pharmacy team");

    public LoginFrame() {
        super("HealthFirst PIMS | Sign in");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(760, 520));
        setSize(900, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(brandPanel(), BorderLayout.WEST);
        add(formPanel(), BorderLayout.CENTER);
        getRootPane().setDefaultButton(signIn);
        signIn.addActionListener(event -> signIn());
        password.addActionListener(event -> signIn());
        Ui.installDefaults();
    }

    private JPanel brandPanel() {
        JPanel brand = new JPanel(new GridBagLayout());
        brand.setBackground(Ui.FOREST_DARK);
        brand.setPreferredSize(new Dimension(395, 0));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.insets = new Insets(8, 44, 8, 44);

        JLabel mark = new JLabel("H+");
        mark.setOpaque(true);
        mark.setBackground(Ui.LIME);
        mark.setForeground(Ui.FOREST_DARK);
        mark.setFont(mark.getFont().deriveFont(Font.BOLD, 24f));
        mark.setHorizontalAlignment(JLabel.CENTER);
        mark.setPreferredSize(new Dimension(54, 54));
        mark.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        constraints.gridy = 0;
        constraints.insets = new Insets(0, 44, 28, 44);
        brand.add(mark, constraints);

        JLabel name = new JLabel("HEALTHFIRST");
        name.setForeground(Color.WHITE);
        name.setFont(name.getFont().deriveFont(Font.BOLD, 13f));
        name.setText("HEALTHFIRST   /   PHARMACY");
        constraints.gridy++;
        constraints.insets = new Insets(8, 44, 8, 44);
        brand.add(name, constraints);

        JLabel title = new JLabel("<html><div style='width: 290px'>Care starts with<br>a well-run store.</div></html>");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 31f));
        constraints.gridy++;
        constraints.insets = new Insets(12, 44, 12, 44);
        brand.add(title, constraints);

        JLabel detail = new JLabel("<html><div style='width: 290px'>Stock, sales and daily insight,<br>in one calm workspace.</div></html>");
        detail.setForeground(new Color(202, 220, 211));
        detail.setFont(detail.getFont().deriveFont(14f));
        constraints.gridy++;
        constraints.insets = new Insets(4, 44, 4, 44);
        brand.add(detail, constraints);

        JLabel footer = new JLabel("OPERATIONS   •   INVENTORY   •   CARE");
        footer.setForeground(Ui.LIME);
        footer.setFont(footer.getFont().deriveFont(Font.BOLD, 10f));
        constraints.gridy++;
        constraints.insets = new Insets(48, 44, 0, 44);
        brand.add(footer, constraints);
        return brand;
    }

    private JPanel formPanel() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(Ui.CANVAS);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Ui.CANVAS);
        form.setBorder(BorderFactory.createEmptyBorder(30, 38, 30, 38));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.insets = new Insets(7, 0, 7, 0);

        JLabel eyebrow = new JLabel("TEAM SIGN IN");
        eyebrow.setForeground(Ui.CORAL);
        eyebrow.setFont(eyebrow.getFont().deriveFont(Font.BOLD, 11f));
        form.add(eyebrow, constraints);

        constraints.gridy++;
        JLabel title = Ui.heading("Welcome back");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 27f));
        form.add(title, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 22, 0);
        form.add(status, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(9, 0, 5, 0);
        form.add(label("Username"), constraints);
        constraints.gridy++;
        Ui.styleInput(username);
        form.add(username, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(14, 0, 5, 0);
        form.add(label("Password"), constraints);
        constraints.gridy++;
        Ui.styleInput(password);
        form.add(password, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(22, 0, 7, 0);
        signIn.setPreferredSize(new Dimension(0, 46));
        form.add(signIn, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(14, 0, 0, 0);
        JLabel hint = Ui.muted("Demo: admin / admin123   ·   cashier / cash123");
        hint.setFont(hint.getFont().deriveFont(11f));
        form.add(hint, constraints);

        GridBagConstraints outerConstraints = new GridBagConstraints();
        outerConstraints.fill = GridBagConstraints.HORIZONTAL;
        outerConstraints.weightx = 1;
        outer.add(form, outerConstraints);
        return outer;
    }

    private JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
        label.setForeground(Ui.INK);
        return label;
    }

    private void signIn() {
        String enteredUsername = username.getText().trim();
        char[] enteredPassword = password.getPassword();
        if (enteredUsername.isBlank() || enteredPassword.length == 0) {
            Ui.error(this, "Enter both your username and password.");
            return;
        }
        signIn.setEnabled(false);
        signIn.setText("Checking access…");
        status.setText("Connecting securely to HealthFirst…");
        new SwingWorker<PimsUser, Void>() {
            @Override
            protected PimsUser doInBackground() throws SQLException {
                return AuthService.authenticate(enteredUsername, enteredPassword);
            }

            @Override
            protected void done() {
                java.util.Arrays.fill(enteredPassword, '\0');
                signIn.setEnabled(true);
                signIn.setText("Sign in  →");
                try {
                    PimsUser user = get();
                    if (user == null) {
                        status.setText("We could not verify those credentials.");
                        Ui.error(LoginFrame.this, "Incorrect username or password, or this account is inactive.");
                        password.setText("");
                        password.requestFocusInWindow();
                        return;
                    }
                    dispose();
                    new DashboardFrame(user).setVisible(true);
                } catch (Exception exception) {
                    status.setText("Database connection unavailable");
                    Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                    Ui.error(LoginFrame.this,
                            "Could not sign in. Check MySQL is running and DB_URL, DB_USER, and DB_PASSWORD are set.\n\n"
                                    + cause.getMessage());
                }
            }
        }.execute();
    }
}