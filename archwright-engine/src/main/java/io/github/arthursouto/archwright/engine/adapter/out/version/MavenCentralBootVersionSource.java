package io.github.arthursouto.archwright.engine.adapter.out.version;

import io.github.arthursouto.archwright.engine.application.port.out.BootVersionSource;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class MavenCentralBootVersionSource implements BootVersionSource {

    private static final URI METADATA_URI =
            URI.create("https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot/maven-metadata.xml");
    private static final Duration TOTAL_TIMEOUT = Duration.ofSeconds(8);
    private static final String CLIENT_NAME = "Archwright";

    private final URI metadataUri;
    private final Duration timeout;
    private final HttpClient client;
    private final MavenMetadataParser parser = new MavenMetadataParser();

    public MavenCentralBootVersionSource() {
        this(METADATA_URI, TOTAL_TIMEOUT);
    }

    public MavenCentralBootVersionSource(URI metadataUri, Duration timeout) {
        this.metadataUri = metadataUri;
        this.timeout = timeout;
        this.client = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    @Override
    public List<String> fetch() throws IOException {
        HttpRequest request = HttpRequest.newBuilder(metadataUri)
                .header("User-Agent", CLIENT_NAME)
                .GET()
                .build();

        return parser.versions(requireOk(send(request)));
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException {
        CompletableFuture<HttpResponse<String>> pending =
                client.sendAsync(request, HttpResponse.BodyHandlers.ofString());

        try {
            return pending.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            pending.cancel(true);
            throw new IOException("maven central did not answer within " + timeout, exception);
        } catch (ExecutionException exception) {
            throw new IOException("maven central request failed: " + exception.getCause(), exception.getCause());
        } catch (InterruptedException exception) {
            pending.cancel(true);
            Thread.currentThread().interrupt();
            throw new IOException("maven central request interrupted", exception);
        }
    }

    private String requireOk(HttpResponse<String> response) throws IOException {
        if (response.statusCode() != 200) {
            throw new IOException("maven central answered HTTP " + response.statusCode());
        }

        return response.body();
    }
}
