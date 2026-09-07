package com.evolution.dropfilecli.command.quickshare;

import com.evolution.dropfile.common.dto.ApiQuickShareLsResponseDTO;
import com.evolution.dropfilecli.command.AbstractCommandHttpHandler;
import com.evolution.dropfilecli.util.ConsoleQrPrinter;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@CommandLine.Command(
        name = "show",
        description = "Retrieve quickshare file information",
        customSynopsis = "dropfile quickshare show <id> [options]",
        parameterListHeading = "%nRequired parameters:%n",
        optionListHeading = "%nOptional parameters:%n",
        sortOptions = false
)
public class QuickShareShowCommand extends AbstractCommandHttpHandler<ApiQuickShareLsResponseDTO> {

    @CommandLine.Parameters(index = "0", description = "Shared file id")
    private String id;

    @CommandLine.Option(
            names = {"--qrcode"},
            arity = "0..1",
            defaultValue = "true",
            fallbackValue = "true",
            description = "Receive QRCode"
    )
    private boolean qrCode;

    @CommandLine.Option(
            names = {"--qrcode-type"},
            description = "Generate QRCode: ${COMPLETION-CANDIDATES}",
            converter = QRCodeTypeEnumConverter.class
    )
    private QRCodeType qrCodeType;

    @Override
    public void run() {
        if (!qrCode && qrCodeType != null) {
            throw new IllegalArgumentException("Error: Cannot specify --qrcode-type when QR code generation is disabled (--qrcode false)");
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

    private void printQRCode(ApiQuickShareLsResponseDTO responseDTO) {
        if (responseDTO.expired()) {
            System.out.println("Unable to generate QRCode for expired object");
            return;
        }

        List<Map.Entry<QRCodeType, String>> links = extractLinks(qrCodeType, responseDTO);
        if (links.isEmpty()) {
            System.out.println("Unable to build QRCode. No links found");
            return;
        }

        for (Map.Entry<QRCodeType, String> linkEntry : links) {
            QRCodeType qrCodeType = linkEntry.getKey();
            String link = linkEntry.getValue();

            if (qrCodeType == QRCodeType.ETHERNET) {
                System.out.println();
                System.err.println(
                        """
                                Warning: Generating QR code for an Ethernet connection.
                                Wireless devices may not be able to connect using this network configuration."""
                );
            }

            System.out.println();
            System.out.println("Connection type " + qrCodeType);
            ConsoleQrPrinter.printUrlAsQr(link);
        }
    }

    private List<Map.Entry<QRCodeType, String>> extractLinks(QRCodeType qrCodeType, ApiQuickShareLsResponseDTO object) {
        if (qrCodeType != null) {
            return getUrlsByType(qrCodeType, object);
        }

        if (object.external() != null && !object.external().isBlank()) {
            return List.of(Map.entry(QRCodeType.EXTERNAL, object.external()));
        }
        if (object.wireless() != null && !object.wireless().isEmpty()) {
            return object.wireless().stream().map(it -> Map.entry(QRCodeType.WIRELESS, it)).toList();
        }
        if (object.ethernet() != null && !object.ethernet().isEmpty()) {
            return object.ethernet().stream().map(it -> Map.entry(QRCodeType.ETHERNET, it)).toList();
        }

        return List.of();
    }

    private List<Map.Entry<QRCodeType, String>> getUrlsByType(QRCodeType qrCodeType, ApiQuickShareLsResponseDTO object) {
        return switch (qrCodeType) {
            case EXTERNAL -> Optional.ofNullable(object.external())
                    .stream()
                    .map(it -> Map.entry(QRCodeType.EXTERNAL, it))
                    .toList();
            case WIRELESS -> object.wireless().stream().map(it -> Map.entry(QRCodeType.WIRELESS, it)).toList();
            case ETHERNET -> object.ethernet().stream().map(it -> Map.entry(QRCodeType.ETHERNET, it)).toList();
        };
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
