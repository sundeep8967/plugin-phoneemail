# Kestra Phone.email Plugin

The Phone.email plugin enables verifying user phone numbers, country dialing codes, and user details through the [Phone.email API](https://www.phone.email/docs).

Phone.email provides managed passwordless authentication widgets and OTP verification without requiring separate telecom gateway agreements, domestic DLT template registrations, or regional SMS providers.

## Why

- **Simplified Phone Verification**: Avoids managing multi-country SMS gateway providers, contracts, and telecom compliance.
- **Automated User Onboarding**: Validates verification tokens received from frontend redirect callbacks inside Kestra workflows.
- **Secure Token Exchange**: Verifies tokens server-to-server and extracts verified phone numbers and identity data directly into downstream database or CRM steps.

## What

- Provides tasks under `io.kestra.plugin.phoneemail`.
- Includes `io.kestra.plugin.phoneemail.GetUser` for token exchange and user profile verification.

## License

Apache 2.0 (c) Kestra Technologies.
