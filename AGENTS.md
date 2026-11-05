# Kestra Phone.email Plugin

## What

- Provides tasks under `io.kestra.plugin.phoneemail`.
- Includes `io.kestra.plugin.phoneemail.GetUser` for token exchange and user profile verification.

## Why

- What user problem does this solve? Simplifies international phone number and identity verification without SMS gateways or carrier contracts.
- Why would a team adopt this plugin in a workflow? Allows verifying user tokens from authentication widgets directly in Kestra onboarding and data pipelines.
- What operational outcome does it enable? Reliable, secure token validation with verified phone and country code extraction.

## How

### Architecture

Single-module plugin. Source package under `io.kestra.plugin`:
- `phoneemail`

### Key Plugin Classes

- `io.kestra.plugin.phoneemail.AbstractPhoneEmailTask`
- `io.kestra.plugin.phoneemail.GetUser`

### Project Structure

```
plugin-phoneemail/
├── src/main/java/io/kestra/plugin/phoneemail/
├── src/test/java/io/kestra/plugin/phoneemail/
├── build.gradle
└── README.md
```
