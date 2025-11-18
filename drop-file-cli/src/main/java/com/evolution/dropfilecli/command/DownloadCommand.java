package com.evolution.dropfilecli.command;

import com.evolution.dropfilecli.client.DaemonHttpClient;
import com.evolution.dropfilecli.configuration.DropFileConfiguration;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import picocli.CommandLine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

@Component
@CommandLine.Command(
        name = "download",
        description = "Download file"
)
public class DownloadCommand implements Runnable {

    private final DaemonHttpClient daemonHttpClient;

    private final DropFileConfiguration dropFileConfiguration;

    @CommandLine.Parameters(index = "0", description = "File path")
    private String filePath;

    @Autowired
    public DownloadCommand(DaemonHttpClient daemonHttpClient, DropFileConfiguration dropFileConfiguration) {
        this.daemonHttpClient = daemonHttpClient;
        this.dropFileConfiguration = dropFileConfiguration;
    }

    @SneakyThrows
    @Override
    public void run() {
        HttpResponse<InputStream> downloadHttpResponse = daemonHttpClient.download(filePath);
        System.out.println(downloadHttpResponse.statusCode());

        String filename = getFilename(downloadHttpResponse.headers()).orElseThrow();
        System.out.println("Filename = " + filename);

        try (InputStream body = downloadHttpResponse.body()) {
            File homeDir = dropFileConfiguration.getDownloadDirectory();
            File file = new File(homeDir, filename);
            Files.createFile(file.toPath());

            try (OutputStream outStream = new FileOutputStream(file)) {
                byte[] buffer = new byte[8 * 1024];
                int bytesRead;
                while ((bytesRead = body.read(buffer)) != -1) {
                    outStream.write(buffer, 0, bytesRead);
                }
            }
        }
    }

    private Optional<String> getFilename(HttpHeaders headers) {
        List<String> contentDispositionList = headers.map().get("content-disposition");
        if (ObjectUtils.isEmpty(contentDispositionList)) {
            return Optional.empty();
        }
        String contentDisposition = contentDispositionList.stream().findAny().orElseThrow();
        String fileName = contentDisposition.replaceFirst("(?i)^.*filename=\"?([^\"]+)\"?.*$", "$1");
        return Optional.of(fileName);
    }
}
