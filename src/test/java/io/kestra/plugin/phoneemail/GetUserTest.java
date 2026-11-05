package io.kestra.plugin.phoneemail;

import com.sun.net.httpserver.HttpServer;
import io.kestra.core.exceptions.KestraRuntimeException;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@KestraTest
class GetUserTest {
    @Inject
    private RunContextFactory runContextFactory;

    private HttpServer server;
    private String serverUrl;
    private final AtomicReference<String> lastAuthHeader = new AtomicReference<>();
    private final AtomicReference<String> lastRequestBody = new AtomicReference<>();
    private int responseStatus = 200;
    private String responseBody = """
        {
          "status": 200,
          "user_country_code": "+91",
          "user_phone_number": "9876543210",
          "user_first_name": "John",
          "user_last_name": "Doe",
          "user_email_id": "john.doe@example.com"
        }
        """;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        serverUrl = "http://localhost:" + server.getAddress().getPort();

        server.createContext("/getuser", exchange -> {
            lastAuthHeader.set(exchange.getRequestHeaders().getFirst("X-API-KEY"));
            lastRequestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseStatus, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldVerifyTokenSuccessfullyWhenParametersValid() throws Exception {
        RunContext runContext = runContextFactory.of(Map.of(
            "apiKey", "phone_secret_key",
            "token", "phtoken_test_abc"
        ));

        GetUser task = GetUser.builder()
            .apiKey(Property.ofExpression("{{ apiKey }}"))
            .clientId(Property.ofValue("client_12345"))
            .baseUrl(Property.ofValue(serverUrl))
            .accessToken(Property.ofExpression("{{ token }}"))
            .build();

        GetUser.Output output = task.run(runContext);

        assertThat(output).isNotNull();
        assertThat(output.getStatus()).isEqualTo(200);
        assertThat(output.getCountryCode()).isEqualTo("+91");
        assertThat(output.getPhoneNumber()).isEqualTo("9876543210");
        assertThat(output.getFirstName()).isEqualTo("John");
        assertThat(output.getLastName()).isEqualTo("Doe");
        assertThat(output.getEmail()).isEqualTo("john.doe@example.com");

        assertThat(lastAuthHeader.get()).isEqualTo("phone_secret_key");
        assertThat(lastRequestBody.get()).contains("\"access_token\":\"phtoken_test_abc\"");
        assertThat(lastRequestBody.get()).contains("\"client_id\":\"client_12345\"");
    }

    @Test
    void shouldThrowExceptionWhenApiReturnsErrorStatus() {
        responseStatus = 400;
        responseBody = "{\"status\": 400, \"message\": \"Invalid access token\"}";

        RunContext runContext = runContextFactory.of();

        GetUser task = GetUser.builder()
            .apiKey(Property.ofValue("phone_secret_key"))
            .baseUrl(Property.ofValue(serverUrl))
            .accessToken(Property.ofValue("invalid_token"))
            .build();

        assertThatThrownBy(() -> task.run(runContext))
            .isInstanceOf(KestraRuntimeException.class)
            .hasMessageContaining("Phone.email verification failed with HTTP status 400");
    }

    @Test
    void shouldReturnOutputWithoutThrowingWhenFailOnErrorIsFalse() throws Exception {
        responseStatus = 401;
        responseBody = "{\"status\": 401, \"message\": \"Unauthorized\"}";

        RunContext runContext = runContextFactory.of();

        GetUser task = GetUser.builder()
            .apiKey(Property.ofValue("phone_secret_key"))
            .baseUrl(Property.ofValue(serverUrl))
            .accessToken(Property.ofValue("expired_token"))
            .failOnError(Property.ofValue(false))
            .build();

        GetUser.Output output = task.run(runContext);

        assertThat(output).isNotNull();
        assertThat(output.getStatus()).isEqualTo(401);
        assertThat(output.getPhoneNumber()).isNull();
    }
}
