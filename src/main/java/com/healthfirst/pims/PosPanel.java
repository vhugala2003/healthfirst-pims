package com.healthfirst.pims;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PosPanel extends JPanel {
    private final PimsUser cashier;
    private final DefaultTableModel catalogModel = Ui.model("ID", "Medicine", "Price", "Available", "Expiry");
    private final DefaultTableModel cartModel = Ui.model("ID", "Medicine", "Qty", "Unit price", "Line total");
    private final JTable catalog = Ui.table(catalogModel);
    private final JTable cartTable = Ui.table(cartModel);
    private final Map<Integer, CartLine> cart = new LinkedHashMap<>();
    private final JTextField search = new JTextField(18);
    private final JSpinner quantity = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
    private final JLabel totalLabel = new JLabel("R 0.00", SwingConstants.RIGHT);

    public PosPanel(PimsUser cashier) {
        this.cashier = cashier;
        setLayout(new BorderLayout(14, 14));
        setBackground(Ui.CANVAS);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 22, 24));
        add(header(), BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, cataloguePanel(), cartPanel());
        split.setResizeWeight(0.56);
        split.setBorder(null);
        split.setOpaque(false);
        split.setDividerSize(12);
        add(split, BorderLayout.CENTER);
        refreshCatalog();
    }

    private JPanel header() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 4));
        titles.setOpaque(false);
        titles.add(Ui.heading("Point of sale"));
        titles.add(Ui.muted("Select stock, build a basket and checkout in one step."));
        panel.add(titles, BorderLayout.WEST);
        JLabel mode = new JLabel("REGISTER  /  " + cashier.fullName().toUpperCase());
        mode.setForeground(Ui.FOREST);
        mode.setFont(mode.getFont().deriveFont(java.awt.Font.BOLD, 10f));
        panel.add(mode, BorderLayout.EAST);
        return panel;
    }

    private JPanel cataloguePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(Ui.CANVAS);
        JPanel toolbar = new JPanel(new BorderLayout(8, 0));
        toolbar.setOpaque(false);
        Ui.styleInput(search);
        search.putClientProperty("JTextField.placeholderText", "Find a medicine");
        JButton find = Ui.secondary("Search");
        JButton refresh = Ui.secondary("Refresh");
        JPanel searchActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        searchActions.setOpaque(false);
        searchActions.add(search);
        searchActions.add(find);
        searchActions.add(refresh);
        toolbar.add(searchActions, BorderLayout.WEST);
        JPanel addActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        addActions.setOpaque(false);
        addActions.add(new JLabel("Qty"));
        addActions.add(quantity);
        JButton add = Ui.primary("Add to cart");
        addActions.add(add);
        toolbar.add(addActions, BorderLayout.EAST);
        find.addActionListener(event -> refreshCatalog());
        search.addActionListener(event -> refreshCatalog());
        refresh.addActionListener(event -> {
            search.setText("");
            refreshCatalog();
        });
        add.addActionListener(event -> addSelected());
        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(Ui.scroll(catalog), BorderLayout.CENTER);
        return panel;
    }

    private JPanel cartPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Ui.CANVAS);
        JPanel cartHeader = new JPanel(new BorderLayout());
        cartHeader.setOpaque(false);
        cartHeader.add(Ui.heading("Current basket"), BorderLayout.WEST);
        JButton remove = Ui.secondary("Remove line");
        remove.addActionListener(event -> removeSelected());
        cartHeader.add(remove, BorderLayout.EAST);
        panel.add(cartHeader, BorderLayout.NORTH);
        panel.add(Ui.scroll(cartTable), BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(0, 10));
        footer.setBackground(Ui.WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Ui.LINE), BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        JPanel totalRow = new JPanel(new BorderLayout());
        totalRow.setOpaque(false);
        JLabel totalCaption = new JLabel("TOTAL DUE");
        totalCaption.setForeground(Ui.MUTED);
        totalCaption.setFont(totalCaption.getFont().deriveFont(java.awt.Font.BOLD, 10f));
        totalLabel.setForeground(Ui.FOREST);
        totalLabel.setFont(totalLabel.getFont().deriveFont(java.awt.Font.BOLD, 24f));
        totalRow.add(totalCaption, BorderLayout.WEST);
        totalRow.add(totalLabel, BorderLayout.EAST);
        footer.add(totalRow, BorderLayout.NORTH);
        JPanel actions = new JPanel(new GridLayout(1, 2, 8, 0));
        actions.setOpaque(false);
        JButton clear = Ui.secondary("Clear basket");
        JButton checkout = Ui.primary("Checkout  →");
        clear.addActionListener(event -> clearCart());
        checkout.addActionListener(event -> checkout());
        actions.add(clear);
        actions.add(checkout);
        footer.add(actions, BorderLayout.SOUTH);
        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    private void refreshCatalog() {
        catalogModel.setRowCount(0);
        String sql = "SELECT medicine_id, name, price, quantity_in_stock, expiry_date FROM medicines "
                + "WHERE name LIKE ? AND quantity_in_stock > 0 AND expiry_date >= CURRENT_DATE ORDER BY name";
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "%" + search.getText().trim() + "%");
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    catalogModel.addRow(new Object[]{result.getInt("medicine_id"), result.getString("name"),
                            Ui.money(result.getBigDecimal("price")), result.getInt("quantity_in_stock"),
                            result.getDate("expiry_date")});
                }
            }
        } catch (SQLException exception) {
            Ui.error(this, "Could not load available stock: " + exception.getMessage());
        }
    }

    private void addSelected() {
        int viewRow = catalog.getSelectedRow();
        if (viewRow < 0) {
            Ui.error(this, "Choose a medicine from the catalogue first.");
            return;
        }
        int row = catalog.convertRowIndexToModel(viewRow);
        int id = (Integer) catalogModel.getValueAt(row, 0);
        String name = String.valueOf(catalogModel.getValueAt(row, 1));
        int available = (Integer) catalogModel.getValueAt(row, 3);
        int requested = (Integer) quantity.getValue();
        CartLine existing = cart.get(id);
        int updatedQuantity = requested + (existing == null ? 0 : existing.quantity());
        if (updatedQuantity > available) {
            Ui.error(this, "Only " + available + " unit(s) are available.");
            return;
        }
        BigDecimal price = parseMoney(catalogModel.getValueAt(row, 2));
        cart.put(id, new CartLine(id, name, price, updatedQuantity));
        refreshCart();
    }

    private BigDecimal parseMoney(Object displayValue) {
        String value = String.valueOf(displayValue).replaceAll("[^0-9.,]", "").replace(",", "");
        return new BigDecimal(value);
    }

    private void removeSelected() {
        int viewRow = cartTable.getSelectedRow();
        if (viewRow < 0) {
            Ui.error(this, "Choose a basket line to remove.");
            return;
        }
        cart.remove((Integer) cartModel.getValueAt(cartTable.convertRowIndexToModel(viewRow), 0));
        refreshCart();
    }

    private void clearCart() {
        if (!cart.isEmpty() && JOptionPane.showConfirmDialog(this, "Clear all items from this basket?",
                "Clear basket", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        cart.clear();
        refreshCart();
    }

    private void refreshCart() {
        cartModel.setRowCount(0);
        BigDecimal total = BigDecimal.ZERO;
        for (CartLine line : cart.values()) {
            BigDecimal lineTotal = line.price().multiply(BigDecimal.valueOf(line.quantity()));
            total = total.add(lineTotal);
            cartModel.addRow(new Object[]{line.id(), line.name(), line.quantity(),
                    Ui.money(line.price()), Ui.money(lineTotal)});
        }
        totalLabel.setText(Ui.money(total));
    }

    private void checkout() {
        if (cart.isEmpty()) {
            Ui.error(this, "Add at least one medicine before checkout.");
            return;
        }
        try {
            Receipt receipt = saveSale();
            cart.clear();
            refreshCart();
            refreshCatalog();
            showReceipt(receipt);
        } catch (IllegalStateException exception) {
            Ui.error(this, exception.getMessage());
            refreshCatalog();
        } catch (SQLException exception) {
            Ui.error(this, "Checkout failed. No sale was saved. " + exception.getMessage());
        }
    }

    private Receipt saveSale() throws SQLException {
        List<LockedLine> lockedLines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        try (Connection connection = Database.connect()) {
            connection.setAutoCommit(false);
            try {
                for (CartLine line : cart.values()) {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "SELECT name, price, quantity_in_stock, expiry_date FROM medicines WHERE medicine_id = ? FOR UPDATE")) {
                        statement.setInt(1, line.id());
                        try (ResultSet result = statement.executeQuery()) {
                            if (!result.next()) {
                                throw new IllegalStateException(line.name() + " is no longer in the catalogue.");
                            }
                            int available = result.getInt("quantity_in_stock");
                            Date expiry = result.getDate("expiry_date");
                            if (available < line.quantity()) {
                                throw new IllegalStateException("Not enough stock for " + line.name()
                                        + ". Available: " + available + ".");
                            }
                            if (expiry.toLocalDate().isBefore(java.time.LocalDate.now())) {
                                throw new IllegalStateException(line.name() + " has expired and cannot be sold.");
                            }
                            BigDecimal price = result.getBigDecimal("price");
                            LockedLine locked = new LockedLine(line.id(), result.getString("name"), line.quantity(), price);
                            lockedLines.add(locked);
                            total = total.add(price.multiply(BigDecimal.valueOf(line.quantity())));
                        }
                    }
                }

                int saleId;
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO sales (total_amount, user_id) VALUES (?, ?)",
                        PreparedStatement.RETURN_GENERATED_KEYS)) {
                    statement.setBigDecimal(1, total);
                    statement.setInt(2, cashier.id());
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("MySQL did not return the new sale number.");
                        }
                        saleId = keys.getInt(1);
                    }
                }

                for (LockedLine line : lockedLines) {
                    try (PreparedStatement update = connection.prepareStatement(
                            "UPDATE medicines SET quantity_in_stock = quantity_in_stock - ? "
                                    + "WHERE medicine_id = ? AND quantity_in_stock >= ? AND expiry_date >= CURRENT_DATE")) {
                        update.setInt(1, line.quantity());
                        update.setInt(2, line.id());
                        update.setInt(3, line.quantity());
                        if (update.executeUpdate() != 1) {
                            throw new IllegalStateException("Stock changed during checkout for " + line.name() + ". Retry the sale.");
                        }
                    }
                    try (PreparedStatement insert = connection.prepareStatement(
                            "INSERT INTO sale_items (sale_id, medicine_id, quantity_sold, price_at_sale) VALUES (?, ?, ?, ?)")) {
                        insert.setInt(1, saleId);
                        insert.setInt(2, line.id());
                        insert.setInt(3, line.quantity());
                        insert.setBigDecimal(4, line.price());
                        insert.executeUpdate();
                    }
                }
                connection.commit();
                return new Receipt(saleId, LocalDateTime.now(), List.copyOf(lockedLines), total);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private void showReceipt(Receipt receipt) {
        JTextArea text = new JTextArea(receiptText(receipt), 18, 42);
        text.setEditable(false);
        text.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
        text.setBackground(Ui.WHITE);
        text.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        javax.swing.JDialog dialog = new javax.swing.JDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this), "Sale " + receipt.saleId() + " | Receipt",
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.add(new javax.swing.JScrollPane(text), BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton save = Ui.secondary("Save receipt");
        JButton print = Ui.primary("Print receipt");
        JButton close = Ui.secondary("Done");
        save.addActionListener(event -> saveReceipt(text.getText()));
        print.addActionListener(event -> {
            try {
                text.print();
            } catch (java.awt.print.PrinterException exception) {
                Ui.error(dialog, "Could not print receipt: " + exception.getMessage());
            }
        });
        close.addActionListener(event -> dialog.dispose());
        actions.add(save);
        actions.add(print);
        actions.add(close);
        dialog.add(actions, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private String receiptText(Receipt receipt) {
        StringBuilder text = new StringBuilder();
        text.append("             HEALTHFIRST PHARMACY\n");
        text.append("         18 Foundry Road, Johannesburg\n");
        text.append("------------------------------------------\n");
        text.append("Receipt: #").append(receipt.saleId()).append('\n');
        text.append("Date:    ").append(receipt.date().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append('\n');
        text.append("Cashier: ").append(cashier.fullName()).append('\n');
        text.append("------------------------------------------\n");
        for (LockedLine line : receipt.lines()) {
            text.append(line.name()).append('\n');
            text.append(String.format("  %2d x %s  %s%n", line.quantity(), Ui.money(line.price()),
                    Ui.money(line.price().multiply(BigDecimal.valueOf(line.quantity())))));
        }
        text.append("------------------------------------------\n");
        text.append("TOTAL:   ").append(Ui.money(receipt.total())).append('\n');
        text.append("\n          Thank you for choosing us.\n");
        return text.toString();
    }

    private void saveReceipt(String receipt) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save receipt");
        chooser.setSelectedFile(new java.io.File("healthfirst-receipt.txt"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                Files.writeString(chooser.getSelectedFile().toPath(), receipt);
            } catch (java.io.IOException exception) {
                Ui.error(this, "Could not save receipt: " + exception.getMessage());
            }
        }
    }

    private record CartLine(int id, String name, BigDecimal price, int quantity) {
    }

    private record LockedLine(int id, String name, int quantity, BigDecimal price) {
    }

    private record Receipt(int saleId, LocalDateTime date, List<LockedLine> lines, BigDecimal total) {
    }
}