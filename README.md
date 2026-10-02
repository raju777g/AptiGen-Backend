# AptiGen Backend

This is the **Spring Boot backend** for AptiGen, an AI-powered aptitude mock test platform. It handles authentication, the AG coin wallet, AI test generation, payments, the marketplace, and weekly contests.

**Frontend repository:** _https://github.com/raju777g/AptiGen-Frontend_

**Live demo:** _https://aptigenai.netlify.app_

## What it does

- **Authentication:** email and password signup with email verification, plus Google and GitHub OAuth2 login. Sessions use secure cookies, not JWT.
- **AI test generation:** an uploaded image goes through OCR.space for text extraction, then the Groq API (`openai/gpt-oss-20b`) structures it into MCQs.
- **AG coin wallet:** signup bonus, a ledger of every transaction, and coin deductions for generating or taking tests.
- **Payments:** Razorpay order creation and webhook verification for coin top-ups.
- **Marketplace:** public tests that other users can browse and take.
- **Creator royalties:** coins credited to creators when others take their public tests.
- **Accuracy cashback:** a coin refund for a perfect score.
- **Daily check-in and streaks:** reward coins for consecutive daily logins.
- **Skill radar analytics:** accuracy breakdown by topic (Quant, Logical Reasoning, Verbal) from past attempts.
- **Weekly live contests:** scheduled tests with a coin entry fee, with coins and badges for top scorers.
- **Notifications**

## Tech Stack

| Area | Tools |
| --- | --- |
| Framework | Spring Boot |
| AI integration | Spring AI, Groq API |
| OCR | OCR.space |
| Database | MySQL |
| Authentication | Spring Security, session cookies, OAuth2 (Google, GitHub) |
| Payments | Razorpay |
| Email | Mailtrap (development) |
| Hosting | _add host here_ |

## Project Structure

```
src/main/java/.../aptigen/
  config/       Security, CORS and app configuration
  controller/   REST API endpoints
  service/      Business logic
  repository/   Spring Data JPA repositories
  entity/       Database entities
  dto/          Request and response objects
  scheduler/    Scheduled jobs (contests, streaks)
src/main/resources/
  application.properties
```


### Prerequisites

- Java 17 or later
- MySQL 8
- API keys for Groq and OCR.space
- Razorpay test keys
- Google and GitHub OAuth app credentials
- A Mailtrap account (or any SMTP provider)


### Environment variables

| Variable | Purpose |
| --- | --- |
| `SQL_URL`, `SQL_USERNAME`, `SQL_PASSWORD` | MySQL connection |
| `GROQ_API_KEY` | AI question generation |
| `OCR_SPACE_API_KEY` | Image text extraction |
| `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET` | Payments |
| `RAZORPAY_WEBHOOK_SECRET` | Webhook signature verification |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | Google login |
| `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET` | GitHub login |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | Email sending |
| `FRONTEND_URL` | Allowed CORS origin and OAuth redirect target |

Never commit real keys. Keep them in environment variables or an untracked local config file.

## Webhook testing

Razorpay webhooks need a public URL. For local testing, expose the backend with ngrok and set that URL as the webhook endpoint in the Razorpay dashboard.

## Deployment

The backend is deployed on **Aiven**, with the MySQL database also hosted there. Environment variables are configured in the hosting dashboard, not committed to the repository.
The frontend proxies `/api`, `/oauth2` and `/login/oauth2` to this backend through Netlify, so the browser treats the session cookie as first-party. The backend's CORS and OAuth redirect settings must allow the deployed frontend URL.

## Author

Built by _Raju Garain_ · [GitHub](https://github.com/raju777g) · [LinkedIn](https://linkedin.com/in/raju-garain-581873290)
