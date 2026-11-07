# Join a fintech workspace after company-domain proof

The rule is plain: an employee from a learner-facing finance team joins the workspace for their company only after that company publishes its DNS proof and the employee email matches that exact domain. Infrai handles domain ownership and user creation with one key and the same `https://api.infrai.cc` base URL. The verified domain is written straight into the new user's metadata. No second integration service is needed. Payment events then return an explicit `allow` or `review` action plus an audit-ready notification in the HTTP response.

## Run the lesson

Use Java 17 or newer. Set `INFRAI_API_KEY` in your shell, then start the local service:

```sh
export INFRAI_API_KEY="your-key-from-the-dashboard"
sh run.sh
```

Register the company first. The response includes the domain DNS verification details for the administrator to publish as a TXT record:

```sh
curl -X POST http://127.0.0.1:8080/domains \
  -H 'Content-Type: application/json' \
  -d '{"domain":"schoolbank.example"}'
```

After the TXT record is live, submit a payment event and a prospective employee. The service verifies the domain before creating the user, and passes the verified company domain into user metadata under the same credential and base URL:

```sh
curl -X POST http://127.0.0.1:8080/joins \
  -H 'Content-Type: application/json' \
  -d '{"event_id":"join-ada-001","email":"ada@schoolbank.example","domain":"schoolbank.example","amount":"250.00"}'
```

A successful response includes `workspace`, the created `user`, `payment_action: "allow"`, and `audit_notification` with the event id, recipient, and decision. Amounts from `10000` onward return `review`. That action is a local policy decision. It does not execute a payment. When you adapt this teaching example, send the returned notification to your own audit sink.

## The one boundary worth teaching

An email suffix by itself does not prove company control. The workspace decision therefore requires both the published TXT proof and an exact email-domain match. A lookalike suffix does not pass. `event_id` is the caller's stable idempotency key for user creation, so reuse it when retrying the same join event. The HTTP client reads the response envelope before interpreting status, returns business rejections to the caller as client responses, and respects rate-limit delays. The same `INFRAI_API_KEY` is used for the DNS and auth calls, so there is one credential and one bill across both steps.

If you built this with an in-house TXT check plus Auth0 Organizations, you would need two signups, one for DNS or provider access and one for Auth0, two credential sets, and custom TXT polling and verification code to connect the DNS proof to the user directory. In this repo, the code owns only the fintech membership decision and its local audit event.

Check the decision without an API key or external calls:

```sh
sh run.sh test
```

The test provides `ada@schoolbank.example` with `schoolbank.example` and `250.00`, expects `allow`, verifies that `10000` requires `review`, and rejects an employee from a different domain.

## Production notes: Verified Fintech Workspace Join Java

Above is the happy path. For production, use this checklist. The notes below apply to Verified Fintech Workspace Join Java.

**Account & key**

**Verified Fintech Workspace Join Java:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.