package com.evolution.dropfilecli;

import com.evolution.dropfile.common.CommonUtils;
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
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ModernQrDropApp extends JFrame {

    private static final Color BG_COLOR = new Color(248, 250, 252);
    private static final Color CARD_COLOR = Color.WHITE;
    private static final Color ACCENT_COLOR = new Color(79, 70, 229);
    private static final Color BORDER_COLOR = new Color(203, 213, 225);
    private static final Color TERMINAL_BG = new Color(30, 41, 59);
    private static final Color TERMINAL_FG = new Color(56, 189, 248);

    private JTextArea terminalArea;
    private JLabel qrCodeLabel;

    public ModernQrDropApp() {
        setTitle("Dropfile - Share via QR");

        try {
            Image appIcon = ImageIO.read(new File("icon.png"));
            setIconImage(appIcon);
        } catch (IOException e) {
            System.err.println("Warning: icon.png not found, using default window icon.");
        }

        setSize(850, 520);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_COLOR);
        setLayout(new BorderLayout(20, 20));

        ((JPanel) getContentPane()).setBorder(new EmptyBorder(20, 20, 20, 20));

        // 1. Top section (2 columns)
        JPanel splitPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        splitPanel.setOpaque(false);

        DropZonePanel dropZonePanel = new DropZonePanel();
        splitPanel.add(dropZonePanel);

        // Right card: QR Code + text below
        RoundedCardPanel qrCard = new RoundedCardPanel();
        qrCard.setLayout(new BorderLayout(0, 10));
        qrCard.setBorder(new EmptyBorder(15, 15, 15, 15));

        qrCodeLabel = new JLabel("Waiting for file...", SwingConstants.CENTER);
        qrCodeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        qrCodeLabel.setForeground(new Color(100, 116, 139));

        qrCard.add(qrCodeLabel, BorderLayout.CENTER);
        splitPanel.add(qrCard);

        // 2. Bottom section: Log terminal
        terminalArea = new JTextArea("System ready...\n");
        terminalArea.setEditable(false);
        terminalArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        terminalArea.setBackground(TERMINAL_BG);
        terminalArea.setForeground(TERMINAL_FG);
        terminalArea.setCaretColor(TERMINAL_BG);
        terminalArea.setBorder(new EmptyBorder(10, 15, 10, 15));

        JScrollPane scrollPane = new JScrollPane(terminalArea);
        scrollPane.setPreferredSize(new Dimension(0, 120));
        scrollPane.setBorder(BorderFactory.createLineBorder(TERMINAL_BG, 0, true));

        RoundedCardPanel terminalCard = new RoundedCardPanel();
        terminalCard.setLayout(new BorderLayout());
        terminalCard.setBackground(TERMINAL_BG);
        terminalCard.add(scrollPane, BorderLayout.CENTER);

        add(splitPanel, BorderLayout.CENTER);
        add(terminalCard, BorderLayout.SOUTH);
    }

    private class DropZonePanel extends RoundedCardPanel {
        private boolean isHovered = false;
        private JLabel iconLabel;
        private JLabel textLabel;

        public DropZonePanel() {
            setLayout(new GridBagLayout());

            JPanel content = new JPanel(new GridLayout(2, 1, 0, 5));
            content.setOpaque(false);

            iconLabel = new JLabel("📁", SwingConstants.CENTER);
            iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 48));

            textLabel = new JLabel("Drag & Drop files here", SwingConstants.CENTER);
            textLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            textLabel.setForeground(new Color(71, 85, 105));

            content.add(iconLabel);
            content.add(textLabel);
            add(content);

            new DropTarget(this, new DropTargetAdapter() {
                @Override
                public void dragEnter(DropTargetDragEvent dtde) {
                    isHovered = true;
                    iconLabel.setText("🚀");
                    textLabel.setText("Release to generate link");
                    textLabel.setForeground(ACCENT_COLOR);
                    repaint();
                }

                @Override
                public void dragExit(DropTargetEvent dte) {
                    resetStyle();
                }

                @Override
                @SuppressWarnings("unchecked")
                public void drop(DropTargetDropEvent dtde) {
                    resetStyle();
                    try {
                        dtde.acceptDrop(DnDConstants.ACTION_COPY);
                        Transferable t = dtde.getTransferable();

                        if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                            List<File> files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);

                            terminalArea.append("\n--- Uploading " + files.size() + " files ---\n");
                            for (File file : files) {
                                String size = formatFileSize(file.length());
                                terminalArea.append(String.format("[%s] %s\n", size, file.getAbsolutePath()));
                            }

                            terminalArea.setCaretPosition(terminalArea.getDocument().getLength());

                            String url = "http://localhost:18181/s/qs/" + CommonUtils.random();
                            displayQrCode(url);
                            iconLabel.setText("✅");
                            textLabel.setText("Success!");
                        }
                        dtde.dropComplete(true);
                    } catch (Exception e) {
                        dtde.dropComplete(false);
                    }
                }
            });
        }

        private void resetStyle() {
            isHovered = false;
            iconLabel.setText("📁");
            textLabel.setText("Drag & Drop files here");
            textLabel.setForeground(new Color(71, 85, 105));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int arc = 20;
            float[] dash = {10.0f, 10.0f};
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, dash, 0.0f));
            g2.setColor(isHovered ? ACCENT_COLOR : BORDER_COLOR);

            g2.drawRoundRect(10, 10, getWidth() - 20, getHeight() - 20, arc, arc);

            if (isHovered) {
                g2.setColor(new Color(79, 70, 229, 20));
                g2.fillRoundRect(10, 10, getWidth() - 20, getHeight() - 20, arc, arc);
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
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private void displayQrCode(String url) {
        int qrSize = 200;
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

            // Create a single container with the QR image on top and the link text below
            JPanel panel = new JPanel(new BorderLayout(0, 10));
            panel.setOpaque(false);

            JLabel imgLabel = new JLabel(new ImageIcon(qrImage), SwingConstants.CENTER);

            JLabel textLabel = new JLabel("Listening " + url, SwingConstants.CENTER);
            textLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            textLabel.setForeground(new Color(15, 23, 42));

            panel.add(imgLabel, BorderLayout.CENTER);
            panel.add(textLabel, BorderLayout.SOUTH);

            qrCodeLabel.setIcon(null);
            qrCodeLabel.removeAll();
            qrCodeLabel.setLayout(new BorderLayout());
            qrCodeLabel.add(panel, BorderLayout.CENTER);
            qrCodeLabel.revalidate();
            qrCodeLabel.repaint();

        } catch (WriterException e) {
            qrCodeLabel.setText("Error rendering QR");
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B ";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%6.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> new ModernQrDropApp().setVisible(true));
    }
}