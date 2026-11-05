package io.kestra.plugin.phoneemail;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kestra.core.exceptions.KestraRuntimeException;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Verify a user access token with Phone.email",
    description = "Exchanges a temporary Phone.email access token for verified user profile details including mobile phone number and country code."
)
@Plugin(
    examples = {
        @Example(
            title = "Verify a user phone number from a webhook access token",
            full = true,
            code = """
                id: verify_phone_email_user
                namespace: company.team

                tasks:
                  - id: verify_token
                    type: io.kestra.plugin.phoneemail.GetUser
                    apiKey: "{{ secret('PHONE_EMAIL_API_KEY') }}"
                    clientId: "1234567890"
                    accessToken: "{{ trigger.body.phtoken }}"
                """
        )
    }
)
public class GetUser extends AbstractPhoneEmailTask implements RunnableTask<GetUser.Output> {
    private static final ObjectMapper MAPPER = JacksonMapper.ofJson();

    @NotNull
    @Schema(
        title = "Access Token",
        description = "The temporary verification access token (phtoken) received from the Phone.email widget."
    )
    @PluginProperty(group = "main", secret = true)
    private Property<String> accessToken;

    @Schema(
        title = "Fail On Error",
        description = "Whether to throw an exception if the verification request or status is not successful."
    )
    @PluginProperty(group = "advanced")
    @Builder.Default
    private Property<Boolean> failOnError = Property.ofValue(true);

    @Override
    public Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        String base = runContext.render(this.baseUrl).as(String.class).orElse("https://eapi.phone.email");
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        String token = runContext.render(this.accessToken).as(String.class).orElseThrow();
        String key = runContext.render(this.apiKey).as(String.class).orElse(null);
        String client = runContext.render(this.clientId).as(String.class).orElse(null);
        Duration reqTimeout = runContext.render(this.timeout).as(Duration.class).orElse(Duration.ofSeconds(15));
        boolean failOnErr = runContext.render(this.failOnError).as(Boolean.class).orElse(true);

        Map<String, Object> payload = new HashMap<>();
        payload.put("access_token", token);
        payload.put("phtoken", token);
        if (client != null && !client.isBlank()) {
            payload.put("client_id", client);
        }
        if (key != null && !key.isBlank()) {
            payload.put("apiKey", key);
        }

        String jsonPayload = MAPPER.writeValueAsString(payload);
        String targetUrl = base + "/getuser";

        logger.debug("Calling Phone.email verification endpoint: {}", targetUrl);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(targetUrl))
            .timeout(reqTimeout)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8));

        if (key != null && !key.isBlank()) {
            requestBuilder.header("X-API-KEY", key);
        }

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        int httpStatus = response.statusCode();
        String responseBody = response.body();

        Map<String, Object> responseMap = null;
        try {
            if (responseBody != null && !responseBody.isBlank()) {
                responseMap = MAPPER.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            logger.warn("Could not parse Phone.email response as JSON: {}", responseBody);
        }

        Integer status = httpStatus;
        if (responseMap != null && responseMap.get("status") instanceof Number n) {
            status = n.intValue();
        }

        if (httpStatus >= 400 || (status != null && status != 200)) {
            String errorMsg = "Phone.email verification failed with HTTP status %d: %s".formatted(httpStatus, responseBody);
            if (failOnErr) {
                throw new KestraRuntimeException(errorMsg);
            }
            logger.warn(errorMsg);
        }

        String countryCode = null;
        String phoneNumber = null;
        String firstName = null;
        String lastName = null;
        String email = null;

        if (responseMap != null) {
            countryCode = stringOrNull(responseMap.get("user_country_code"));
            phoneNumber = stringOrNull(responseMap.get("user_phone_number"));
            firstName = stringOrNull(responseMap.get("user_first_name"));
            lastName = stringOrNull(responseMap.get("user_last_name"));
            email = stringOrNull(responseMap.get("user_email_id"));
        }

        logger.info("Phone.email verification succeeded for country code {} and phone {}", countryCode, phoneNumber);

        return Output.builder()
            .status(status)
            .countryCode(countryCode)
            .phoneNumber(phoneNumber)
            .firstName(firstName)
            .lastName(lastName)
            .email(email)
            .rawResponse(responseMap)
            .build();
    }

    private static String stringOrNull(Object value) {
        return value == null ? null : value.toString();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "Status Code",
            description = "Response status code returned by the Phone.email API."
        )
        private Integer status;

        @Schema(
            title = "User Country Code",
            description = "Verified international country dialing code prefix."
        )
        private String countryCode;

        @Schema(
            title = "User Phone Number",
            description = "Verified mobile phone number without the country prefix."
        )
        private String phoneNumber;

        @Schema(
            title = "User First Name",
            description = "User first name when provided during verification."
        )
        private String firstName;

        @Schema(
            title = "User Last Name",
            description = "User last name when provided during verification."
        )
        private String lastName;

        @Schema(
            title = "User Email",
            description = "Verified email address if captured during verification."
        )
        private String email;

        @Schema(
            title = "Raw Response",
            description = "Full JSON response returned by the Phone.email endpoint."
        )
        private Map<String, Object> rawResponse;
    }
}
