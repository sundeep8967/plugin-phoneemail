package io.kestra.plugin.phoneemail;

import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.Task;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.Duration;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractPhoneEmailTask extends Task {
    @Schema(
        title = "Phone.email API Key",
        description = "Private API key obtained from the Phone.email admin dashboard."
    )
    @PluginProperty(group = "connection", secret = true)
    @ToString.Exclude
    protected Property<String> apiKey;

    @Schema(
        title = "Phone.email Client ID",
        description = "Unique client ID from the Phone.email admin profile settings."
    )
    @PluginProperty(group = "connection")
    protected Property<String> clientId;

    @Schema(
        title = "Base URL",
        description = "Phone.email API base endpoint. Defaults to `https://eapi.phone.email`."
    )
    @PluginProperty(group = "connection")
    @Builder.Default
    protected Property<String> baseUrl = Property.ofValue("https://eapi.phone.email");

    @Schema(
        title = "HTTP Request Timeout",
        description = "Maximum duration to wait for the verification request to complete."
    )
    @PluginProperty(group = "connection")
    @Builder.Default
    protected Property<Duration> timeout = Property.ofValue(Duration.ofSeconds(15));
}
