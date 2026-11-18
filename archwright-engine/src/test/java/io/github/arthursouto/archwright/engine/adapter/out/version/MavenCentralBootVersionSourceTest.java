package io.github.arthursouto.archwright.engine.adapter.out.version;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MavenCentralBootVersionSourceTest {

    private static final String PATH = "/maven-metadata.xml";
    private static final String METADATA =
            "<metadata><versioning><versions><version>4.1.2</version><version>4.2.0-M1</version></versions></versioning></metadata>";

    private HttpServer server;
    private ExecutorService executor;

    @BeforeEach
    void startServer() throws IOException {
        executor = Executors.newCachedThreadPool();
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.setExecutor(executor);
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
        executor.shutdownNow();
    }

    @Test
    void fetchesTheVersionsListedInTheMetadata() throws IOException {
        answer(200, METADATA, Duration.ZERO);

        assertThat(source(Duration.ofSeconds(5)).fetch()).containsExactly("4.1.2", "4.2.0-M1");
    }

    @Test
    @DisplayName("BR-072 envia só um GET anônimo, sem dados do usuário")
    void sendsOnlyAnAnonymousGet() throws IOException {
        AtomicReference<String> seen = new AtomicReference<>();
        server.createContext(PATH, exchange -> {
            seen.set(exchange.getRequestMethod() + " " + exchange.getRequestURI()
                    + " agent=" + exchange.getRequestHeaders().getFirst("User-Agent")
                    + " headers=" + exchange.getRequestHeaders().keySet().stream().map(String::toLowerCase).sorted().toList()
                    + " body=" + exchange.getRequestBody().readAllBytes().length);
            reply(exchange, 200, METADATA);
        });
        server.start();

        source(Duration.ofSeconds(5)).fetch();

        assertThat(seen.get())
                .startsWith("GET /maven-metadata.xml agent=Archwright")
                .endsWith("body=0")
                .doesNotContain("cookie", "authorization");
    }

    @Test
    void failsWhenTheServerAnswersWithAnError() {
        answer(500, "boom", Duration.ZERO);

        assertThatThrownBy(() -> source(Duration.ofSeconds(5)).fetch())
                .isInstanceOf(IOException.class)
                .hasMessageContaining("500");
    }

    @Test
    void failsOnAnAnswerThatIsNotMavenMetadata() {
        answer(200, "<html>captive portal</html>", Duration.ZERO);

        assertThatThrownBy(() -> source(Duration.ofSeconds(5)).fetch()).isInstanceOf(IOException.class);
    }

    @Test
    @DisplayName("ENG-VER-01 #5 desiste quando o servidor demora mais que o limite")
    void givesUpWhenTheServerIsSlowerThanTheTimeout() {
        answer(200, METADATA, Duration.ofSeconds(5));
        long start = System.nanoTime();

        assertThatThrownBy(() -> source(Duration.ofMillis(300)).fetch())
                .isInstanceOf(IOException.class)
                .hasMessageContaining("did not answer within");

        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(3));
    }

    @Test
    void failsWhenNothingIsListening() throws IOException {
        server.start();
        URI unreachable = uri();
        server.stop(0);

        assertThatThrownBy(() -> new MavenCentralBootVersionSource(unreachable, Duration.ofSeconds(2)).fetch())
                .isInstanceOf(IOException.class);
    }

    private MavenCentralBootVersionSource source(Duration timeout) {
        return new MavenCentralBootVersionSource(uri(), timeout);
    }

    private URI uri() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + PATH);
    }

    private void answer(int status, String body, Duration delay) {
        server.createContext(PATH, exchange -> {
            pause(delay);
            reply(exchange, status, body);
        });
        server.start();
    }

    private static void pause(Duration delay) {
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static void reply(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
