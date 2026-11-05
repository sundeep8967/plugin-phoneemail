# Phone.email

The Phone.email plugin enables verifying user phone numbers, country codes, and user details through the [Phone.email API](https://www.phone.email/docs).

Phone.email provides managed passwordless authentication widgets and OTP verification without requiring separate telecom gateway agreements, domestic DLT template registrations, or regional SMS providers.

## Getting Started

1. Sign up on the [Phone.email Admin Dashboard](https://admin.phone.email).
2. Retrieve your `CLIENT_ID` and private `API_KEY` from your dashboard settings.
3. Store your private API key in Kestra Secrets under `PHONE_EMAIL_API_KEY`.
4. Embed the Phone.email button on your frontend. Upon successful verification, the widget passes a temporary `access_token` (`phtoken`) to your redirect URL or webhook callback.

## Tasks

### `io.kestra.plugin.phoneemail.GetUser`

Verifies a temporary access token (`phtoken`) against the Phone.email API and retrieves verified profile details:
- `userPhoneNumber`: Verified mobile number without country code.
- `userCountryCode`: International dialing prefix (e.g. `+91`, `+1`).
- `firstName` and `lastName`: User name when captured by the widget.
- `email`: Verified email address when enabled.
- `rawResponse`: Full JSON map returned by the Phone.email API.
