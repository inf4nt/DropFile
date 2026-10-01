package com.evolution.dropfilecli.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.Map;

public class ConsoleQrPrinter {

    public static void printAsQr(String value) {
        BitMatrix bitMatrix = createQr(value);

        System.out.println("Scan this QR code to continue: " + value);
        System.out.println();

        for (int y = 0; y < bitMatrix.getHeight(); y += 2) {
            StringBuilder line = new StringBuilder();

            for (int x = 0; x < bitMatrix.getWidth(); x++) {
                boolean top = bitMatrix.get(x, y);
                boolean bottom = y + 1 < bitMatrix.getHeight() && bitMatrix.get(x, y + 1);

                if (top && bottom) {
                    line.append('█');
                } else if (top) {
                    line.append('▀');
                } else if (bottom) {
                    line.append('▄');
                } else {
                    line.append(' ');
                }
            }

            System.out.println(line);
        }

        System.out.println();
    }

    private static BitMatrix createQr(String url) {
        try {
            return new QRCodeWriter().encode(
                    url,
                    BarcodeFormat.QR_CODE,
                    0,
                    0,
                    Map.of(
                            EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L,
                            EncodeHintType.MARGIN, 1
                    )
            );
        } catch (WriterException e) {
            throw new IllegalStateException("Failed to generate QR code", e);
        }
    }
}