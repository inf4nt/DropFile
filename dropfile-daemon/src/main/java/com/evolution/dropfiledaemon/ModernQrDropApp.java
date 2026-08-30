package com.evolution.dropfiledaemon;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ModernQrDropApp extends JFrame {

    private static final Color BG_COLOR = new Color(248, 250, 252);
    private static final Color CARD_COLOR = Color.WHITE;
    private static final Color BORDER_COLOR = new Color(203, 213, 225);
    private static final Color TEXT_PRIMARY = new Color(15, 23, 42);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);

    private static final Color ACCENT_COLOR = new Color(79, 70, 229);
    private static final Color ACCENT_HOVER = new Color(67, 56, 202);
    private static final Color RESET_COLOR = new Color(239, 68, 68);
    private static final Color RESET_HOVER = new Color(220, 38, 38);
    private static final Color TERMINAL_BG = new Color(30, 41, 59);
    private static final Color TERMINAL_FG = new Color(56, 189, 248);

    private final DropZonePanel dropZonePanel;
    private final JCheckBox secureCheckBox;
    private final JTextField secretField;
    private final JButton copySecretButton;
    private final JCheckBox singleUseCheckBox;
    private final JLabel expiredStatusLabel;
    private final JButton confirmButton;
    private final JButton resetButton;

    private final JLabel qrCodeLabel;
    private final JButton copyLinkButton;
    private final JTextArea terminalArea;

    private final JPanel glassOverlayPanel;
    private final SpinnerPanel spinnerPanel;

    private final List<File> selectedFiles = new ArrayList<>();
    private boolean isExpired = false;
    private String generatedUrl = null;

    public ModernQrDropApp() {
        setTitle("Dropfile - Share via QR");

        try {
            Image appIcon = ImageIO.read(new File("icon.png"));
            setIconImage(appIcon);
        } catch (IOException e) {
            System.err.println("Warning: icon.png not found, using default window icon.");
        }

        setSize(870, 620);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JLayeredPane layeredPane = new JLayeredPane();
        setContentPane(layeredPane);

        JPanel mainContentPanel = new JPanel(new BorderLayout(20, 15));
        mainContentPanel.setBackground(BG_COLOR);
        mainContentPanel.setBorder(new EmptyBorder(15, 20, 20, 20));
        mainContentPanel.setBounds(0, 0, 855, 580);

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int w = getContentPane().getWidth();
                int h = getContentPane().getHeight();
                mainContentPanel.setBounds(0, 0, w, h);
                glassOverlayPanel.setBounds(0, 0, w, h);
                layeredPane.revalidate();
            }
        });

        secretField = new JTextField(UUID.randomUUID().toString());
        secretField.setFont(new Font("Consolas", Font.PLAIN, 12));

        copySecretButton = new JButton("Copy");
        copySecretButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        copySecretButton.setToolTipText("Copy Secret");
        copySecretButton.setFocusPainted(false);
        copySecretButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        copySecretButton.setPreferredSize(new Dimension(85, 28));

        copySecretButton.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(secretField.getText()), null);
            copySecretButton.setText("Copied");

            Timer timer = new Timer(1500, t -> copySecretButton.setText("Copy"));
            timer.setRepeats(false);
            timer.start();
        });

        secureCheckBox = new JCheckBox("Secure (Password protected)");
        secureCheckBox.setFont(new Font("Segoe UI", Font.BOLD, 13));
        secureCheckBox.setForeground(TEXT_PRIMARY);
        secureCheckBox.setOpaque(false);
        secureCheckBox.setSelected(true);
        secureCheckBox.addActionListener(e -> {
            boolean selected = secureCheckBox.isSelected();
            if (secretField != null) secretField.setEnabled(selected);
            if (copySecretButton != null) copySecretButton.setEnabled(selected);
        });

        singleUseCheckBox = new JCheckBox("SingleUse (One-time download)");
        singleUseCheckBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        singleUseCheckBox.setForeground(TEXT_PRIMARY);
        singleUseCheckBox.setOpaque(false);
        singleUseCheckBox.setSelected(true);

        expiredStatusLabel = new JLabel("Expired: false");
        expiredStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        expiredStatusLabel.setForeground(new Color(16, 185, 129));

        confirmButton = new JButton("OK (Generate QR)");
        confirmButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        confirmButton.setForeground(Color.WHITE);
        confirmButton.setBackground(ACCENT_COLOR);
        confirmButton.setFocusPainted(false);
        confirmButton.setBorderPainted(false);
        confirmButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        confirmButton.setEnabled(false);
        confirmButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        confirmButton.addActionListener(e -> onConfirmClicked());

        resetButton = new JButton("Reset All");
        resetButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        resetButton.setForeground(Color.WHITE);
        resetButton.setBackground(RESET_COLOR);
        resetButton.setFocusPainted(false);
        resetButton.setBorderPainted(false);
        resetButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        resetButton.setVisible(false);
        resetButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        resetButton.addActionListener(e -> resetAll());

        resetButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                resetButton.setBackground(RESET_HOVER);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                resetButton.setBackground(RESET_COLOR);
            }
        });

        dropZonePanel = new DropZonePanel();

        JPanel configPanel = new JPanel();
        configPanel.setLayout(new BoxLayout(configPanel, BoxLayout.Y_AXIS));
        configPanel.setOpaque(false);

        JLabel secretLabel = new JLabel("Secret Key:");
        secretLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        secretLabel.setForeground(TEXT_MUTED);

        JPanel secretContainer = new JPanel(new BorderLayout(5, 0));
        secretContainer.setOpaque(false);
        secretContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        secretContainer.add(secretField, BorderLayout.CENTER);
        secretContainer.add(copySecretButton, BorderLayout.EAST);

        secureCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        secretLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        secretContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        singleUseCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        expiredStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        confirmButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        resetButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        configPanel.add(secureCheckBox);
        configPanel.add(Box.createVerticalStrut(4));
        configPanel.add(secretLabel);
        configPanel.add(Box.createVerticalStrut(2));
        configPanel.add(secretContainer);
        configPanel.add(Box.createVerticalStrut(8));
        configPanel.add(singleUseCheckBox);
        configPanel.add(Box.createVerticalStrut(6));
        configPanel.add(expiredStatusLabel);
        configPanel.add(Box.createVerticalStrut(12));
        configPanel.add(confirmButton);
        configPanel.add(resetButton);

        RoundedCardPanel leftCard = new RoundedCardPanel();
        leftCard.setLayout(new BorderLayout(0, 15));
        leftCard.setBorder(new EmptyBorder(15, 15, 15, 15));
        leftCard.add(dropZonePanel, BorderLayout.CENTER);
        leftCard.add(configPanel, BorderLayout.SOUTH);

        RoundedCardPanel qrCard = new RoundedCardPanel();
        qrCard.setLayout(new BorderLayout(0, 10));
        qrCard.setBorder(new EmptyBorder(15, 15, 15, 15));

        qrCodeLabel = new JLabel("Waiting for files & confirmation...", SwingConstants.CENTER);
        qrCodeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        qrCodeLabel.setForeground(TEXT_MUTED);

        copyLinkButton = new JButton("Copy Link");
        copyLinkButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        copyLinkButton.setForeground(Color.WHITE);
        copyLinkButton.setBackground(ACCENT_COLOR);
        copyLinkButton.setFocusPainted(false);
        copyLinkButton.setBorderPainted(false);
        copyLinkButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        copyLinkButton.setVisible(false);

        copyLinkButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (copyLinkButton.isEnabled()) copyLinkButton.setBackground(ACCENT_HOVER);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (copyLinkButton.isEnabled()) copyLinkButton.setBackground(ACCENT_COLOR);
            }
        });

        copyLinkButton.addActionListener(e -> {
            if (generatedUrl != null) {
                Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new StringSelection(generatedUrl), null);
                copyLinkButton.setText("Copied!");

                Timer timer = new Timer(2000, t -> copyLinkButton.setText("Copy Link"));
                timer.setRepeats(false);
                timer.start();
            }
        });

        qrCard.add(qrCodeLabel, BorderLayout.CENTER);
        qrCard.add(copyLinkButton, BorderLayout.SOUTH);

        JPanel splitPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        splitPanel.setOpaque(false);
        splitPanel.add(leftCard);
        splitPanel.add(qrCard);

        terminalArea = new JTextArea("Starting Spring Boot application...\n");
        terminalArea.setEditable(false);
        terminalArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        terminalArea.setBackground(TERMINAL_BG);
        terminalArea.setForeground(TERMINAL_FG);
        terminalArea.setCaretColor(TERMINAL_BG);
        terminalArea.setBorder(new EmptyBorder(10, 15, 10, 15));

        JScrollPane scrollPane = new JScrollPane(terminalArea);
        scrollPane.setPreferredSize(new Dimension(0, 110));
        scrollPane.setBorder(BorderFactory.createLineBorder(TERMINAL_BG, 0, true));

        RoundedCardPanel terminalCard = new RoundedCardPanel();
        terminalCard.setLayout(new BorderLayout());
        terminalCard.setBackground(TERMINAL_BG);
        terminalCard.add(scrollPane, BorderLayout.CENTER);

        mainContentPanel.add(splitPanel, BorderLayout.CENTER);
        mainContentPanel.add(terminalCard, BorderLayout.SOUTH);

        glassOverlayPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(248, 250, 252, 200));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        glassOverlayPanel.setOpaque(false);
        glassOverlayPanel.addMouseListener(new java.awt.event.MouseAdapter() {});
        glassOverlayPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {});

        JPanel spinnerCard = new JPanel(new BorderLayout(0, 15));
        spinnerCard.setOpaque(false);

        spinnerPanel = new SpinnerPanel();
        JLabel loadingText = new JLabel("Starting Application Service...", SwingConstants.CENTER);
        loadingText.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loadingText.setForeground(TEXT_PRIMARY);

        spinnerCard.add(spinnerPanel, BorderLayout.CENTER);
        spinnerCard.add(loadingText, BorderLayout.SOUTH);

        glassOverlayPanel.add(spinnerCard);

        layeredPane.add(mainContentPanel, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(glassOverlayPanel, JLayeredPane.MODAL_LAYER);

        setLoading(true);
    }

    private void resetAll() {
        selectedFiles.clear();
        generatedUrl = null;

        secretField.setText(UUID.randomUUID().toString());

        secureCheckBox.setSelected(true);
        singleUseCheckBox.setSelected(true);
        setExpired(false);

        confirmButton.setVisible(true);
        confirmButton.setEnabled(false);
        resetButton.setVisible(false);

        setFieldsReadOnly(false);

        qrCodeLabel.setIcon(null);
        qrCodeLabel.removeAll();
        qrCodeLabel.setText("Waiting for files & confirmation...");
        copyLinkButton.setVisible(false);

        dropZonePanel.updateFileListUI();

        terminalArea.setText("System reset. Ready for new files...\n");

        revalidate();
        repaint();
    }

    public void setLoading(boolean loading) {
        SwingUtilities.invokeLater(() -> {
            glassOverlayPanel.setVisible(loading);
            if (loading) {
                spinnerPanel.startAnimation();
            } else {
                spinnerPanel.stopAnimation();
                terminalArea.append("[SYSTEM] Spring Boot context initialized successfully.\n");
                terminalArea.append("System ready...\n");
            }
            revalidate();
            repaint();
        });
    }

    private static class SpinnerPanel extends JPanel {
        private int angle = 0;
        private final Timer timer;

        public SpinnerPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(50, 50));
            timer = new Timer(30, e -> {
                angle = (angle + 8) % 360;
                repaint();
            });
        }

        public void startAnimation() {
            if (!timer.isRunning()) timer.start();
        }

        public void stopAnimation() {
            if (timer.isRunning()) timer.stop();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int size = Math.min(width, height) - 8;
            int x = (width - size) / 2;
            int y = (height - size) / 2;

            g2.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(203, 213, 225));
            g2.drawOval(x, y, size, size);

            g2.setColor(ACCENT_COLOR);
            g2.drawArc(x, y, size, size, angle, 100);

            g2.dispose();
        }
    }

    private void addFiles(List<File> newFiles) {
        for (File f : newFiles) {
            if (!selectedFiles.contains(f)) {
                selectedFiles.add(f);
                String size = formatFileSize(f.length());
                terminalArea.append(String.format("Added: [%s] %s\n", size, f.getAbsolutePath()));
            }
        }

        boolean hasFiles = !selectedFiles.isEmpty();
        confirmButton.setEnabled(hasFiles);
        dropZonePanel.updateFileListUI();

        if (hasFiles && generatedUrl == null) {
            qrCodeLabel.setText("Click 'OK' to freeze options & generate QR");
        } else if (!hasFiles) {
            qrCodeLabel.setText("Waiting for files & confirmation...");
        }
    }

    private void removeFile(File file) {
        selectedFiles.remove(file);
        terminalArea.append("Removed: " + file.getName() + "\n");

        boolean hasFiles = !selectedFiles.isEmpty();
        confirmButton.setEnabled(hasFiles);
        dropZonePanel.updateFileListUI();

        if (!hasFiles) {
            qrCodeLabel.setText("Waiting for files & confirmation...");
        }
    }

    private void onConfirmClicked() {
        if (selectedFiles.isEmpty()) return;

        setFieldsReadOnly(true);

        confirmButton.setVisible(false);
        resetButton.setVisible(true);

        generatedUrl = "http://localhost:18181/s/qs/" + (int) (Math.random() * 89999 + 10000);
        displayQrCode(generatedUrl);

        terminalArea.append(String.format("[CONFIRMED] Locked %d file(s). QR Code generated.\n", selectedFiles.size()));
        if (secureCheckBox.isSelected()) {
            terminalArea.append("[SECURITY] Secure Mode: Protected with secret key.\n");
        } else {
            terminalArea.append("[SECURITY] Insecure Mode: Direct download.\n");
        }
        if (singleUseCheckBox.isSelected()) {
            terminalArea.append("[POLICY] SingleUse: Link expires after first download.\n");
        }
    }

    private void setFieldsReadOnly(boolean readOnly) {
        boolean editable = !readOnly;
        dropZonePanel.setAcceptsDrop(editable);
        secureCheckBox.setEnabled(editable);
        if (secretField != null) {
            secretField.setEditable(editable && secureCheckBox.isSelected());
            secretField.setEnabled(editable && secureCheckBox.isSelected());
        }
        if (copySecretButton != null) {
            copySecretButton.setEnabled(secureCheckBox.isSelected());
        }
        singleUseCheckBox.setEnabled(editable);
        confirmButton.setEnabled(editable && !selectedFiles.isEmpty());
    }

    public void setExpired(boolean expired) {
        this.isExpired = expired;
        expiredStatusLabel.setText("Expired: " + isExpired);
        expiredStatusLabel.setForeground(isExpired ? new Color(239, 68, 68) : new Color(16, 185, 129));
    }

    private class DropZonePanel extends RoundedCardPanel {
        private boolean isHovered = false;
        private boolean acceptsDrop = true;

        private final JPanel filesListPanel;
        private final JScrollPane listScrollPane;
        private final JLabel placeholderLabel;

        public DropZonePanel() {
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(10, 10, 10, 10));

            placeholderLabel = new JLabel("Drag & Drop files here", SwingConstants.CENTER);
            placeholderLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            placeholderLabel.setForeground(TEXT_PRIMARY);

            filesListPanel = new JPanel();
            filesListPanel.setLayout(new BoxLayout(filesListPanel, BoxLayout.Y_AXIS));
            filesListPanel.setOpaque(false);

            listScrollPane = new JScrollPane(filesListPanel);
            listScrollPane.setOpaque(false);
            listScrollPane.getViewport().setOpaque(false);
            listScrollPane.setBorder(null);

            add(placeholderLabel, BorderLayout.CENTER);

            new DropTarget(this, new DropTargetAdapter() {
                @Override
                public void dragEnter(DropTargetDragEvent dtde) {
                    if (!acceptsDrop) return;
                    isHovered = true;
                    if (selectedFiles.isEmpty()) {
                        placeholderLabel.setText("Release to choose files");
                        placeholderLabel.setForeground(ACCENT_COLOR);
                    }
                    repaint();
                }

                @Override
                public void dragExit(DropTargetEvent dte) {
                    if (!acceptsDrop) return;
                    isHovered = false;
                    if (selectedFiles.isEmpty()) {
                        placeholderLabel.setText("Drag & Drop files here");
                        placeholderLabel.setForeground(TEXT_PRIMARY);
                    }
                    repaint();
                }

                @Override
                @SuppressWarnings("unchecked")
                public void drop(DropTargetDropEvent dtde) {
                    if (!acceptsDrop) {
                        dtde.rejectDrop();
                        return;
                    }
                    isHovered = false;
                    repaint();
                    try {
                        dtde.acceptDrop(DnDConstants.ACTION_COPY);
                        Transferable t = dtde.getTransferable();

                        if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                            List<File> files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                            if (!files.isEmpty()) {
                                addFiles(files);
                            }
                        }
                        dtde.dropComplete(true);
                    } catch (Exception e) {
                        dtde.dropComplete(false);
                    }
                }
            });
        }

        public void updateFileListUI() {
            filesListPanel.removeAll();

            if (selectedFiles.isEmpty()) {
                remove(listScrollPane);
                placeholderLabel.setText("Drag & Drop files here");
                placeholderLabel.setForeground(TEXT_PRIMARY);
                add(placeholderLabel, BorderLayout.CENTER);
            } else {
                remove(placeholderLabel);

                for (File file : selectedFiles) {
                    filesListPanel.add(createFileRowPanel(file));
                    filesListPanel.add(Box.createVerticalStrut(6));
                }

                add(listScrollPane, BorderLayout.CENTER);
            }

            revalidate();
            repaint();
        }

        private JPanel createFileRowPanel(File file) {
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setOpaque(true);
            row.setBackground(new Color(241, 245, 249));
            row.setBorder(new EmptyBorder(6, 10, 6, 8));

            JLabel nameLabel = new JLabel(file.getName());
            nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            nameLabel.setForeground(TEXT_PRIMARY);

            JLabel sizeLabel = new JLabel(formatFileSize(file.length()));
            sizeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            sizeLabel.setForeground(TEXT_MUTED);

            JPanel textContainer = new JPanel(new BorderLayout(5, 0));
            textContainer.setOpaque(false);
            textContainer.add(nameLabel, BorderLayout.WEST);
            textContainer.add(sizeLabel, BorderLayout.EAST);

            JButton removeBtn = new JButton("x");
            removeBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
            removeBtn.setForeground(TEXT_MUTED);
            removeBtn.setBorderPainted(false);
            removeBtn.setContentAreaFilled(false);
            removeBtn.setFocusPainted(false);
            removeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            removeBtn.setEnabled(acceptsDrop);

            removeBtn.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    if (acceptsDrop) removeBtn.setForeground(RESET_COLOR);
                }
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    removeBtn.setForeground(TEXT_MUTED);
                }
            });

            removeBtn.addActionListener(e -> removeFile(file));

            row.add(textContainer, BorderLayout.CENTER);
            row.add(removeBtn, BorderLayout.EAST);

            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            return row;
        }

        public void setAcceptsDrop(boolean acceptsDrop) {
            this.acceptsDrop = acceptsDrop;
            updateFileListUI();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int arc = 20;
            float[] dash = {10.0f, 10.0f};
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, dash, 0.0f));
            g2.setColor(isHovered ? ACCENT_COLOR : BORDER_COLOR);

            g2.drawRoundRect(4, 4, getWidth() - 8, getHeight() - 8, arc, arc);

            if (isHovered) {
                g2.setColor(new Color(79, 70, 229, 20));
                g2.fillRoundRect(4, 4, getWidth() - 8, getHeight() - 8, arc, arc);
            }
            g2.dispose();
        }
    }

    private class RoundedCardPanel extends JPanel {
        public RoundedCardPanel() {
            setOpaque(false);
            setBackground(CARD_COLOR);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private void displayQrCode(String url) {
        int qrSize = 180;
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = qrCodeWriter.encode(
                    url, BarcodeFormat.QR_CODE, qrSize, qrSize,
                    Map.of(
                            EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L,
                            EncodeHintType.MARGIN, 0
                    )
            );

            BufferedImage qrImage = new BufferedImage(qrSize, qrSize, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = qrImage.createGraphics();

            g.setColor(Color.WHITE);
            g.fillRect(0, 0, qrSize, qrSize);
            g.setColor(new Color(15, 23, 42));

            for (int i = 0; i < qrSize; i++) {
                for (int j = 0; j < qrSize; j++) {
                    if (bitMatrix.get(i, j)) {
                        g.fillRect(i, j, 1, 1);
                    }
                }
            }
            g.dispose();

            JPanel panel = new JPanel(new BorderLayout(0, 10));
            panel.setOpaque(false);

            JLabel imgLabel = new JLabel(new ImageIcon(qrImage), SwingConstants.CENTER);

            JLabel textLabel = new JLabel("Listening: " + url, SwingConstants.CENTER);
            textLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            textLabel.setForeground(TEXT_PRIMARY);

            panel.add(imgLabel, BorderLayout.CENTER);
            panel.add(textLabel, BorderLayout.SOUTH);

            qrCodeLabel.setText("");
            qrCodeLabel.setIcon(null);
            qrCodeLabel.removeAll();

            qrCodeLabel.setLayout(new BorderLayout());
            qrCodeLabel.add(panel, BorderLayout.CENTER);
            qrCodeLabel.revalidate();
            qrCodeLabel.repaint();

            copyLinkButton.setVisible(true);

        } catch (WriterException e) {
            qrCodeLabel.setText("Error rendering QR");
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> new ModernQrDropApp().setVisible(true));
    }
}