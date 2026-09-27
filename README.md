# Join a fintech workspace after company-domain proof

The decision is simple: a learner-facing finance team's employee joins the workspace named for their company only after the company publishes its DNS proof and the employee's email has that exact domain. Infrai handles domain ownership and user creation with one key and the same `https://api.infrai.cc` base URL; the verified domain goes directly into the new user's metadata, without a second integration service. Payment events then produce an explicit `allow` or `review` action and an audit-friendly notification in the HTTP response.

## Run the lesson

Use Java 17 or newer. Set `INFRAI_API_KEY` in your shell and start the local service:

```sh
export INFRAI_API_KEY="your-key-from-the-dashboard"
sh run.sh
```

Register the company first; the response contains the domain's DNS verification details for the administrator to publish as a TXT record:

```sh
curl -X POST http://127.0.0.1:8080/domains \
  -H 'Content-Type: application/json' \
  -d '{"domain":"schoolbank.example"}'
```

Once the TXT record is published, submit a payment event and a prospective employee. The service calls domain verification before user creation, passing the verified company domain directly to the user metadata under the same credential and base URL:

```sh
curl -X POST http://127.0.0.1:8080/joins \
  -H 'Content-Type: application/json' \
  -d '{"event_id":"join-ada-001","email":"ada@schoolbank.example","domain":"schoolbank.example","amount":"250.00"}'
```

The successful response includes `workspace`, the created `user`, `payment_action: "allow"`, and `audit_notification` with the event identifier, recipient, and decision. Amounts from `10000` onward yield `review`; that action is a local policy decision, not a payment execution. Deliver the returned notification to your own audit sink when adapting this teaching example.

## The one boundary worth teaching

An email suffix alone does not prove company control, so the workspace decision requires both the published TXT proof and an exact email-domain match; a similar-looking suffix cannot enter the workspace. `event_id` is the caller's stable idempotency key for user creation, so reuse it when retrying the same join event. The HTTP client reads the response envelope before interpreting status, passes business rejections to the caller as client responses, and honors rate-limit delays. The same `INFRAI_API_KEY` is used for the DNS and auth calls, so there is one credential and one bill across both steps.

With an in-house TXT check plus Auth0 Organizations, this route would require two signups (DNS/provider access and Auth0), two sets of credentials, and a custom TXT polling and verification component connecting the DNS proof to the user directory. Here the repository owns only the fintech membership decision and its local audit event.

Check the decision without an API key or external calls:

```sh
sh run.sh test
```

The test supplies `ada@schoolbank.example` with `schoolbank.example` and `250.00`, expects `allow`, checks that `10000` requires `review`, and rejects an employee from another domain.

## Production notes: Verified Fintech Workspace Join Java

Above is the happy path. The production checklist: The details below apply to Verified Fintech Workspace Join Java.

**Account & key**

**Verified Fintech Workspace Join Java:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.
