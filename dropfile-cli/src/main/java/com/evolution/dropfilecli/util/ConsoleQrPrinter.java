package com.evolution.dropfilecli.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.Map;

public class ConsoleQrPrinter {

    public static void printUrlAsQr(String url) {
        int size = 30;
        QRCodeWriter qrCodeWriter = new QRCodeWriter();

        try {
            BitMatrix bitMatrix = qrCodeWriter.encode(
                    url, BarcodeFormat.QR_CODE, size, size,
                    Map.of(
                            EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L,
                            EncodeHintType.MARGIN, 1,
                            EncodeHintType.CHARACTER_SET, "UTF-8"
                    )
            );

            String whiteBlock = "\033[47m  \033[0m";
            String blackBlock = "\033[40m  \033[0m";

            System.out.println("Scan this QR code to download: URL " + url);
            System.out.println();

            for (int y = 0; y < bitMatrix.getHeight(); y++) {
                System.out.print(whiteBlock + whiteBlock);

                for (int x = 0; x < bitMatrix.getWidth(); x++) {
                    if (bitMatrix.get(x, y)) {
                        System.out.print(blackBlock);
                    } else {
                        System.out.print(whiteBlock);
                    }
                }

                System.out.println(whiteBlock + whiteBlock);
            }

            System.out.println();

        } catch (WriterException e) {
            throw new RuntimeException(e);
        }
    }
}
