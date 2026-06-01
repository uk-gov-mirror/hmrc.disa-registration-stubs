
# disa-registration-stubs
App Description to be confirmed

### Before running the app

This repository relies on having mongodb running locally. You can start it with:

```bash
# first check to see if mongo is already running
docker ps | grep mongodb

# if not, start it
docker run --restart unless-stopped --name mongodb -p 27017:27017 -d percona/percona-server-mongodb:7.0 --replSet rs0
```

Reference instructions for [setting up docker](https://docs.tax.service.gov.uk/mdtp-handbook/documentation/developer-set-up/install-docker.html) and [running mongodb](https://docs.tax.service.gov.uk/mdtp-handbook/documentation/developer-set-up/set-up-mongodb.html#install-mongodb-applesilicon-mac).

### Running the app

```bash
sbt run
```

You can then query the app to ensure it is working with the following command:

```bash
# other useful commands
sbt clean

sbt reload

sbt compile
```

### Running the test suite

To run the unit tests:

```bash
sbt test
```

To run the integration tests:

```bash
sbt it/test
```

### Before you commit

This service leverages scalaFmt to ensure that the code is formatted correctly.

Before you commit, please run the following commands to check that the code is formatted correctly:

```bash
# checks all source and sbt files are correctly formatted
sbt prePrChecks

# if checks fail, you can format with the following commands

# formats all source files
sbt scalafmtAll

# formats all sbt files
sbt scalafmtSbt

# formats just the main source files (excludes test and configuration files)
sbt scalafmt
```

### Endpoints

### PUT /tax-enrolments/subscriptions/:subscriptionId/subscriber

This endpoint allows for simulation of tax enrolment subscription and an associated asynchronous callback.
The `credId` used in the generation of the bearer token is used to select the scenario.

| Scenario | `credId` | Subscriber response | Callback triggered | Callback state |
| --- | --- | --- | --- | --- |
| Success | `tax-enrolment-success` or anything except specific test values | `204 No Content` | Yes | `SUCCEEDED` |
| Issuer failure | `tax-enrolment-issuer-failure` | `204 No Content` | Yes | `ERROR` |
| Enrolment error | `tax-enrolment-enrolment-error` | `204 No Content` | Yes | `EnrolmentError` |
| Enrolled | `tax-enrolment-enrolled` | `204 No Content` | Yes | `Enrolled` |
| Auth refreshed | `tax-enrolment-auth-refreshed` | `204 No Content` | Yes | `AuthRefreshed` |
| Bad request | `tax-enrolment-bad-request` | `400 Bad Request` | No | N/A |
| Internal server error | `tax-enrolment-internal-server-error` | `500 Internal Server Error` | No | N/A |
| Unauthorized | Auth fails or no credentials | `401 Unauthorized` | No | N/A |

For callback scenarios, the subscriber endpoint returns `204 No Content` independently of the callback result. The callback is triggered separately using the `callback` URL from the request payload.

### POST /incorporated-entity-identification/api/limited-company-journey
Simulates the GRS create limited company journey endpoint.

The response is driven by the `credId` returned from Auth.

| Scenario                | `credId`                            | Response                      |
|-------------------------|-------------------------------------|-------------------------------|
| Unauthorized            | `grs-create-journey-unauthorised`   | `401 Unauthorized`            |
| Upstream Error          | `grs-create-journey-upstream-error` | `500 Internal Server Error`   |
| Invalid JSON (stubbed)  | `grs-create-journey-invalid-json`   | `400 Bad Request`             |
| Invalid URLs (stubbed)  | `grs-create-journey-invalid-urls`   | `400 Bad Request`             |
| Success                 | `grs-create-journey-success`        | `201 Created`                 |
| Success (default)       | any other value                     | `201 Created`                 |

For successful responses, the body will be:

```json
{
  "journeyStartUrl": "/obligations/enrolment/isa/incorporated-identity-callback?journeyId=<credId>"
}
```
Where `<credId>` is reused as the journeyId for subsequent calls to the journey data retrieval endpoint (see below).

### GET /journey/:journeyId
Simulates the GRS/BV journey data retrieval endpoint.

This can be triggered directly with calls, or by using the create journey endpoint with one of the following retrieval journey IDs as the Auth `credId`.

| Scenario                   | `journeyId` or `credId`             | Response           | Description                                                          |
|----------------------------|-------------------------------------|--------------------|----------------------------------------------------------------------|
| Success                    | `grs-retrieval-success`             | `200 OK`           | Typical success case with user going through GRS and BV              |
| Success (CT Enrolled)      | `grs-retrieval-success-ct-enrolled` | `200 OK`           | Success case for users with IR-CT enrolment, fast-tracked through BV |
| Business Verification Fail | `grs-retrieval-bv-fail`             | `200 OK`           | Failure in BV journey resulting in lockout                           |
| Registration Failed        | `grs-retrieval-registration-failed` | `200 OK`           | Successful verification but failure to register user with ETMP       |
| Absent UTR                 | `grs-retrieval-absent-utr`          | `200 OK`           | Edge case that can occur with Registered Societies                   |
| Not Found                  | `grs-retrieval-data-not-found`      | `404 Not Found`    | No journey data found for the given ID                               |
| Unauthorized (stubbed)     | `grs-retrieval-unauthorised`        | `401 Unauthorized` | Explicit stubbed unauthorized response                               |
| Unauthorized (real)        | auth fails                          | `401 Unauthorized` | Real authorization failure (e.g. missing or invalid credentials)     |
| Success (default)          | any other value                     | `200 OK`           | Defaults to typical success response                                 |

### POST /address-lookup/lookup

Simulates the Address Lookup lookup-by-postcode endpoint.

The response is driven by the `postcode` submitted in the request. The optional `filter` field performs a substring match against address lines.

| Scenario                 | `postcode`  | `filter`      | Response           | Description                                      |
|--------------------------|-------------|---------------|--------------------|--------------------------------------------------|
| No results               | `ZZ00 1ZZ`  | not supplied  | `200 OK`           | Returns an empty JSON array                      |
| Single result            | `ZZ11 1ZZ`  | not supplied  | `200 OK`           | Returns one address record                       |
| Multiple results         | `ZZ22 2ZZ`  | not supplied  | `200 OK`           | Returns multiple address records                 |
| Filtered single result   | `ZZ22 2ZZ`  | `10`          | `200 OK`           | Returns only addresses containing `10`           |
| Filtered no results      | `ZZ22 2ZZ`  | no match      | `200 OK`           | Returns an empty JSON array                      |
| No results default       | any other value | any value | `200 OK`           | Defaults to an empty JSON array                  |

#### Request body

```json
{
  "postcode": "ZZ11 1ZZ",
  "filter": "10"
}
```
### GET     /tax-enrolments/groups/:groupId/subscriptions

Simulates the tax-enrolment, retrieve subscription by groupId, endpoint.

The response is driven by the `groupId` returned from Auth.

| Scenario                  | `groupId`              | Response         | Callback state |
|---------------------------|------------------------|------------------|----------------|
| Success                   | any other group Id     | `200 with JSON`  | `SUCCEEDED`    |
| Success (status: pending) | `groupId-state-pending` | `200 with JSON`  | `PENDING`      |
| Success (status: offline) | `groupId-state-offline` | `200 with JSON`  | `OFFLINE`      |
| Success (status: error)   | `groupId-state-error`  | `200 with JSON`  | `ERROR`        |
| Success (not found)       | `groupId-notfound`     | `200 Empty JSON` | No state       |
| Internal Server Error     | null                   | `500`            | No state       |

Available identifier keys and values:

| keys   | `values` |
|--------|----------|
| ZREF   | `Z0001`  |

### POST /email-verification/v2/send-code

Simulates sending an email verification code.

The response is driven by the email field in the request body.

| Scenario          | `email`                  | Response                                                                                   | HTTP Status                 | Description                       |
| ----------------- | ------------------------ | ------------------------------------------------------------------------------------------ | --------------------------- | --------------------------------- |
| Code not sent     | `code-not-sent@test.com` | `{ "status": "CODE_NOT_SENT" }`                                                            | `400 Bad Request`           | Simulates failure to send code    |
| Internal error    | `server-error@test.com`  | No body                                                                                    | `500 Internal Server Error` | Simulates upstream/system failure |
| Success (default) | any other value          | `{ "status": "CODE_SENT", "message": "Email containing verification code has been sent" }` | `200 OK`                    | Successful send                   |

### POST /email-verification/v2/verify-code

Simulates verification of an email verification code.

The response is driven by the verificationCode field in the request body.

| Scenario        | `verificationCode` | Response                                                                                                | HTTP Status                 | Description                       |
| --------------- | ------------------ | ------------------------------------------------------------------------------------------------------- | --------------------------- | --------------------------------- |
| Verified        | `ABCDEF`           | `{ "status": "CODE_VERIFIED", "message": "The verification code for the email verified successfully" }` | `200 OK`                    | Successful verification           |
| Not validated   | `NOTVAL`           | `{ "status": "CODE_NOT_VALIDATED" }`                                                                    | `400 Bad Request`           | Code failed validation            |
| Not found       | `NOTFND`           | `{ "status": "CODE_NOT_FOUND", "message": "Verification code not found" }`                              | `404 Not Found`             | Code not found or expired         |
| Internal error  | `SERERR`           | No body                                                                                                 | `500 Internal Server Error` | Simulates upstream/system failure |
| Default failure | any other value    | `{ "status": "CODE_NOT_VALIDATED", "message": "Invalid verification code" }`                            | `400 Bad Request`           | Unknown/invalid code              |


### Further documentation

You can view further information regarding this service via our [service guide](#).

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
