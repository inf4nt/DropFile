package com.evolution.dropfilecli.command.quickshare;

import com.evolution.dropfile.common.dto.ApiQuickShareLsResponseDTO;
import com.evolution.dropfilecli.AbstractCommandHttpHandler;
import com.evolution.dropfilecli.ConsoleQrPrinter;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import picocli.CommandLine;

import java.net.http.HttpResponse;
import java.util.Optional;

@Component
@CommandLine.Command(
        name = "show"
)
public class QuickShareShowCommand extends AbstractCommandHttpHandler<ApiQuickShareLsResponseDTO> {

    @CommandLine.Option(names = {"-id", "--id"}, description = "Id", required = true)
    private String id;

    @CommandLine.Option(
            names = {"-qrcode", "--qrcode", "-qr", "--qr"},
            arity = "0..1",
            defaultValue = "true",
            fallbackValue = "true",
            description = "Receive QRCode"
    )
    private boolean qrCode;

    @CommandLine.Option(
            names = {"-type", "--type", "-t", "--t"},
            description = "QRCode type",
            converter = QRCodeTypeEnumConverter.class
    )
    private QRCodeType qrCodeType;

    @Override
    public void run() {
        if (!qrCode && qrCodeType != null) {
            throw new RuntimeException("Error: Cannot specify --type when QR code generation is disabled (-qr false)");
        }
        super.run();
    }

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.quickShareShow(id);
    }

    @Override
    protected TypeReference<ApiQuickShareLsResponseDTO> getTypeReference() {
        return new TypeReference<ApiQuickShareLsResponseDTO>() {
        };
    }

    @Override
    protected void print(ApiQuickShareLsResponseDTO object) {
        super.print(object);

        if (qrCode) {
            printQRCode(object);
        }
    }

    private void printQRCode(ApiQuickShareLsResponseDTO object) {
        String url = extractURL(qrCodeType, object).orElse(null);
        if (ObjectUtils.isEmpty(url)) {
            throw new IllegalStateException("Unable to build QRCode by given type " + qrCodeType);
        }

        ConsoleQrPrinter.printUrlAsQr(url);
    }

    private Optional<String> extractURL(QRCodeType qrCodeType, ApiQuickShareLsResponseDTO object) {
        if (qrCodeType == null) {
            return object.external().stream().findFirst()
                    .or(() -> object.wireless().stream().findFirst())
                    .or(() -> object.ethernet().stream().findFirst());
        }

        if (qrCodeType == QRCodeType.EXTERNAL) {
            return object.external().stream().findFirst();
        }
        if (qrCodeType == QRCodeType.WIRELESS) {
            return object.wireless().stream().findFirst();
        }
        if (qrCodeType == QRCodeType.ETHERNET) {
            return object.ethernet().stream().findFirst();
        }
        throw new RuntimeException("No source found by " + qrCodeType);
    }

    private static class QRCodeTypeEnumConverter implements CommandLine.ITypeConverter<QRCodeType> {
        @Override
        public QRCodeType convert(String value) {
            return QRCodeType.valueOf(value.toUpperCase());
        }
    }

    private enum QRCodeType {
        EXTERNAL,
        WIRELESS,
        ETHERNET
    }
}
