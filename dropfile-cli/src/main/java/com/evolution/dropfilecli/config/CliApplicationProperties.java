package com.evolution.dropfilecli.config;

import com.evolution.dropfilecli.command.PrintModeEnum;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class CliApplicationProperties {

    public final Path userDir;

    public final Path daemonSecretsDirectory;

    public final Path daemonInstallationSeedDirectory;

    public final String daemonHost;

    public final int daemonPort;

    @Nullable
    public final PrintModeEnum printModeDefault;

    public final long daemonClientHttpRequestTimeoutMillis;

    @Autowired
    public CliApplicationProperties(@Value("${user.dir}") Path userDir,
                                    @Value("${dropfile.daemon.daemon-secrets.directory}") Path daemonSecretsDirectory,
                                    @Value("${dropfile.daemon.installation-seed.directory}") Path daemonInstallationSeedDirectory,
                                    @Value("${dropfile.daemon.host}") String daemonHost,
                                    @Value("${dropfile.daemon.port}") int daemonPort,
                                    @Value("${dropfile.daemon.cli.print.mode.default:#{null}}") String printModeDefault,
                                    @Value("${dropfile.cli.daemon.client.http.request-timeout-millis}") long daemonClientHttpRequestTimeoutMillis) {
        this.userDir = userDir;
        this.daemonSecretsDirectory = daemonSecretsDirectory;
        this.daemonInstallationSeedDirectory = daemonInstallationSeedDirectory;
        this.daemonHost = daemonHost;
        this.daemonPort = daemonPort;
        this.daemonClientHttpRequestTimeoutMillis = daemonClientHttpRequestTimeoutMillis;
        this.printModeDefault = getPrintMode(printModeDefault);
    }

    private PrintModeEnum getPrintMode(String printMode) {
        if (printMode == null) {
            return null;
        }
        return PrintModeEnum.valueOf(printMode.toUpperCase());
    }
}
