/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, verifyNoInteractions, when}
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.InsufficientEnrolments
import uk.gov.hmrc.disaregistrationstubs.models.TaxEnrolmentCallbackRequest
import uk.gov.hmrc.http.HeaderCarrier
import utils.BaseUnitSpec

import scala.concurrent.Future

class TaxEnrolmentControllerSpec extends BaseUnitSpec {

  private val subscriptionId = "subscription-id-123"

  private val callbackUrl =
    "http://localhost:1201/disa-registration/callback/subscriptions"

  private val subscriptionUrl =
    s"http://tax-enrolments.service/tax-enrolments/subscriptions/$subscriptionId"

  private val validBody = Json.parse(
    s"""
       |{
       |  "serviceName": "HMRC-DISA-ORG",
       |  "callback": "$callbackUrl",
       |  "etmpId": "62724841-c368-4645-b755-839e19d2af39"
       |}
       |""".stripMargin
  )

  private def request(body: JsValue) =
    FakeRequest(PUT, s"/tax-enrolments/subscriptions/$subscriptionId/subscriber")
      .withHeaders("Content-Type" -> "application/json")
      .withBody(body)

  private def mockCallbackSuccess(): Unit =
    when(
      mockRegistrationConnector.callCallback(
        callbackUrl = eqTo(callbackUrl),
        payload = any[TaxEnrolmentCallbackRequest]
      )(any[HeaderCarrier])
    ).thenReturn(Future.unit)

  private def mockCallbackFailure(): Unit =
    when(
      mockRegistrationConnector.callCallback(
        callbackUrl = eqTo(callbackUrl),
        payload = any[TaxEnrolmentCallbackRequest]
      )(any[HeaderCarrier])
    ).thenReturn(Future.failed(new RuntimeException("callback failed")))

  private def partialFailureMessage(state: String): String =
    s"Subscription with formBundleId $subscriptionId partially processed. " +
      s"This could indicate an error but will depend on your regime. " +
      s"Check the state [$state] Note This subscription can still be retried by making a call to " +
      s"PUT /tax-enrolments/subscription/$subscriptionId/issuer"

  "TaxEnrolmentController.subscribe" should {

    "call the callback with SUCCEEDED and return 204 when authorise returns the success credId" in {
      running(fakeApplication()) {
        authorisedUser(Some("tax-enrolment-success"))
        mockCallbackSuccess()

        val result = route(app, request(validBody)).get

        status(result)          shouldBe NO_CONTENT
        contentAsString(result) shouldBe empty

        verify(mockRegistrationConnector).callCallback(
          callbackUrl = eqTo(callbackUrl),
          payload = eqTo(
            TaxEnrolmentCallbackRequest(
              url = subscriptionUrl,
              state = "SUCCEEDED",
              errorResponse = None
            )
          )
        )(any[HeaderCarrier])
      }
    }

    "call the callback with SUCCEEDED and return 204 when no specific credId scenario matches" in {
      running(fakeApplication()) {
        authorisedUser(Some("some-other-cred-id"))
        mockCallbackSuccess()

        val result = route(app, request(validBody)).get

        status(result)          shouldBe NO_CONTENT
        contentAsString(result) shouldBe empty

        verify(mockRegistrationConnector).callCallback(
          callbackUrl = eqTo(callbackUrl),
          payload = eqTo(
            TaxEnrolmentCallbackRequest(
              url = subscriptionUrl,
              state = "SUCCEEDED",
              errorResponse = None
            )
          )
        )(any[HeaderCarrier])
      }
    }

    "call the callback with ERROR and return 204 when authorise returns the issuer failure credId" in {
      running(fakeApplication()) {
        authorisedUser(Some("tax-enrolment-issuer-failure"))
        mockCallbackSuccess()

        val result = route(app, request(validBody)).get

        status(result)          shouldBe NO_CONTENT
        contentAsString(result) shouldBe empty

        verify(mockRegistrationConnector).callCallback(
          callbackUrl = eqTo(callbackUrl),
          payload = eqTo(
            TaxEnrolmentCallbackRequest(
              url = subscriptionUrl,
              state = "ERROR",
              errorResponse = Some("error message")
            )
          )
        )(any[HeaderCarrier])
      }
    }

    "call the callback with EnrolmentError and return 204 when authorise returns the enrolment error credId" in {
      running(fakeApplication()) {
        authorisedUser(Some("tax-enrolment-enrolment-error"))
        mockCallbackSuccess()

        val result = route(app, request(validBody)).get

        status(result)          shouldBe NO_CONTENT
        contentAsString(result) shouldBe empty

        verify(mockRegistrationConnector).callCallback(
          callbackUrl = eqTo(callbackUrl),
          payload = eqTo(
            TaxEnrolmentCallbackRequest(
              url = subscriptionUrl,
              state = "EnrolmentError",
              errorResponse = Some(partialFailureMessage("EnrolmentError"))
            )
          )
        )(any[HeaderCarrier])
      }
    }

    "call the callback with Enrolled and return 204 when authorise returns the enrolled credId" in {
      running(fakeApplication()) {
        authorisedUser(Some("tax-enrolment-enrolled"))
        mockCallbackSuccess()

        val result = route(app, request(validBody)).get

        status(result)          shouldBe NO_CONTENT
        contentAsString(result) shouldBe empty

        verify(mockRegistrationConnector).callCallback(
          callbackUrl = eqTo(callbackUrl),
          payload = eqTo(
            TaxEnrolmentCallbackRequest(
              url = subscriptionUrl,
              state = "Enrolled",
              errorResponse = Some(partialFailureMessage("Enrolled"))
            )
          )
        )(any[HeaderCarrier])
      }
    }

    "call the callback with AuthRefreshed and return 204 when authorise returns the auth refreshed credId" in {
      running(fakeApplication()) {
        authorisedUser(Some("tax-enrolment-auth-refreshed"))
        mockCallbackSuccess()

        val result = route(app, request(validBody)).get

        status(result)          shouldBe NO_CONTENT
        contentAsString(result) shouldBe empty

        verify(mockRegistrationConnector).callCallback(
          callbackUrl = eqTo(callbackUrl),
          payload = eqTo(
            TaxEnrolmentCallbackRequest(
              url = subscriptionUrl,
              state = "AuthRefreshed",
              errorResponse = Some(partialFailureMessage("AuthRefreshed"))
            )
          )
        )(any[HeaderCarrier])
      }
    }

    "return 400 and not call the callback when authorise returns the bad request credId" in {
      running(fakeApplication()) {
        authorisedUser(Some("tax-enrolment-bad-request"))

        val result = route(app, request(validBody)).get

        status(result)          shouldBe BAD_REQUEST
        contentType(result)     shouldBe Some("text/plain")
        contentAsString(result) shouldBe "Bad request from Tax Enrolments stub"

        verifyNoInteractions(mockRegistrationConnector)
      }
    }

    "return 500 and not call the callback when authorise returns the internal server error credId" in {
      running(fakeApplication()) {
        authorisedUser(Some("tax-enrolment-internal-server-error"))

        val result = route(app, request(validBody)).get

        status(result)          shouldBe INTERNAL_SERVER_ERROR
        contentType(result)     shouldBe Some("text/plain")
        contentAsString(result) shouldBe "Internal server error from Tax Enrolments stub"

        verifyNoInteractions(mockRegistrationConnector)
      }
    }

    "return 204 when the callback call fails after the subscription request is accepted" in {
      running(fakeApplication()) {
        authorisedUser(Some("tax-enrolment-success"))
        mockCallbackFailure()

        val result = route(app, request(validBody)).get

        status(result) shouldBe NO_CONTENT
      }
    }

    "return 401 when authorise returns no credentials" in {
      running(fakeApplication()) {
        when(
          mockAuthConnector.authorise(
            any(),
            any()
          )(any(), any())
        ).thenReturn(
          Future.successful(None)
        )

        val result = route(app, request(validBody)).get

        status(result) shouldBe UNAUTHORIZED

        verifyNoInteractions(mockRegistrationConnector)
      }
    }

    "return 401 when authorise fails" in {
      running(fakeApplication()) {
        when(
          mockAuthConnector.authorise(
            any(),
            any()
          )(any(), any())
        ).thenReturn(Future.failed(InsufficientEnrolments()))

        val result = route(app, request(validBody)).get

        status(result) shouldBe UNAUTHORIZED

        verifyNoInteractions(mockRegistrationConnector)
      }
    }

    "return 400 when request payload is invalid" in {
      running(fakeApplication()) {
        authorisedUser(Some("tax-enrolment-success"))

        val invalidBody = Json.parse("""{"json":"bad"}""")

        val result = route(app, request(invalidBody)).get

        status(result)          shouldBe BAD_REQUEST
        contentType(result)     shouldBe Some("text/plain")
        contentAsString(result) shouldBe "Invalid request payload"

        verifyNoInteractions(mockRegistrationConnector)
      }
    }
  }
}
