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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers.mustBe
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.*
import utils.BaseUnitSpec

import scala.concurrent.Future

class GrsControllerSpec extends BaseUnitSpec {

  private def incorporatedEntityJourneyRetrievalRequest(journeyId: String) =
    FakeRequest(GET, s"/incorporated-entity-identification/api/journey/$journeyId")

  private def partnershipJourneyRetrievalRequest(journeyId: String) =
    FakeRequest(GET, s"/partnership-identification/api/journey/$journeyId")

  private def journeyRetrievalJson(result: Future[play.api.mvc.Result]): JsValue =
    contentAsJson(result)

  private val validCreateJourneyJson: JsObject = Json.obj(
    "continueUrl"               -> "/testUrl",
    "businessVerificationCheck" -> true,
    "deskProServiceId"          -> "DeskProServiceId",
    "signOutUrl"                -> "/testSignOutUrl",
    "regime"                    -> "DISA",
    "accessibilityUrl"          -> "/accessibility-statement/my-service"
  )

  private val invalidUrlsCreateJourneyJson: JsObject =
    validCreateJourneyJson ++ Json.obj(
      "continueUrl" -> "https://example.com/testUrl"
    )

  private def createJourneyRequest(url: String, json: JsValue = validCreateJourneyJson) =
    FakeRequest(POST, url)
      .withHeaders(CONTENT_TYPE -> "application/json")
      .withJsonBody(json)

  private val createLimitedCompanyJourneyUrl =
    "/incorporated-entity-identification/api/limited-company-journey"

  private val createRegisteredSocietyJourneyUrl =
    "/incorporated-entity-identification/api/registered-society-journey"

  private val createGeneralPartnershipJourneyUrl =
    "/partnership-identification/api/general-partnership-journey"

  private val createScottishPartnershipJourneyUrl =
    "/partnership-identification/api/scottish-partnership-journey"

  private val createScottishLimitedPartnershipJourneyUrl =
    "/partnership-identification/api/scottish-limited-partnership-journey"

  private val createLimitedPartnershipJourneyUrl =
    "/partnership-identification/api/limited-partnership-journey"

  private val createLimitedLiabilityPartnershipJourneyUrl =
    "/partnership-identification/api/limited-liability-partnership-journey"

  "GrsController.retrieveIncorporatedEntityJourneyData" should {

    "return 200 with success payload" in {
      running(fakeApplication()) {
        authorisedUser()

        val result = route(app, incorporatedEntityJourneyRetrievalRequest("grs-retrieval-success")).get

        status(result) mustBe OK
        (journeyRetrievalJson(result) \ "identifiersMatch").as[Boolean] mustBe true
        (journeyRetrievalJson(result) \ "registration" \ "registrationStatus").as[String] mustBe "REGISTERED"
        (journeyRetrievalJson(result) \ "businessVerification" \ "verificationStatus").as[String] mustBe "PASS"
        (journeyRetrievalJson(result) \ "ctutr").as[String] mustBe "1234567890"
      }
    }

    "return 200 with CT enrolled success payload" in {
      running(fakeApplication()) {
        authorisedUser()

        val result = route(app, incorporatedEntityJourneyRetrievalRequest("grs-retrieval-success-ct-enrolled")).get

        status(result) mustBe OK
        (journeyRetrievalJson(result) \ "identifiersMatch").as[Boolean] mustBe true
        (journeyRetrievalJson(result) \ "registration" \ "registrationStatus").as[String] mustBe "REGISTERED"
        (journeyRetrievalJson(result) \ "businessVerification" \ "verificationStatus").as[String] mustBe "CT_ENROLLED"
        (journeyRetrievalJson(result) \ "registration" \ "registeredBusinessPartnerId").as[String] mustBe "111111"
      }
    }

    "return BV fail scenario" in {
      running(fakeApplication()) {
        authorisedUser()

        val result = route(app, incorporatedEntityJourneyRetrievalRequest("grs-retrieval-bv-fail")).get

        status(result) mustBe OK
        (journeyRetrievalJson(result) \ "identifiersMatch").as[Boolean] mustBe true
        (journeyRetrievalJson(result) \ "businessVerification" \ "verificationStatus").as[String] mustBe "FAIL"
        (journeyRetrievalJson(result) \ "registration" \ "registrationStatus")
          .as[String] mustBe "REGISTRATION_NOT_CALLED"
        (journeyRetrievalJson(result) \ "registration" \ "registeredBusinessPartnerId").toOption mustBe None
      }
    }

    "return registration failed scenario" in {
      running(fakeApplication()) {
        authorisedUser()

        val result = route(app, incorporatedEntityJourneyRetrievalRequest("grs-retrieval-registration-failed")).get

        status(result) mustBe OK
        (journeyRetrievalJson(result) \ "identifiersMatch").as[Boolean] mustBe true
        (journeyRetrievalJson(result) \ "businessVerification" \ "verificationStatus").as[String] mustBe "PASS"
        (journeyRetrievalJson(result) \ "registration" \ "registrationStatus").as[String] mustBe "REGISTRATION_FAILED"
        (journeyRetrievalJson(result) \ "registration" \ "registeredBusinessPartnerId").toOption mustBe None
      }
    }

    "return absent UTR scenario" in {
      running(fakeApplication()) {
        authorisedUser()

        val result = route(app, incorporatedEntityJourneyRetrievalRequest("grs-retrieval-absent-utr")).get

        status(result) mustBe OK
        (journeyRetrievalJson(result) \ "identifiersMatch").as[Boolean] mustBe false
        (journeyRetrievalJson(result) \ "businessVerification" \ "verificationStatus").as[String] mustBe "UNCHALLENGED"
        (journeyRetrievalJson(result) \ "registration" \ "registrationStatus")
          .as[String] mustBe "REGISTRATION_NOT_CALLED"
      }
    }

    "return 404 when journey not found" in {
      running(fakeApplication()) {
        authorisedUser()

        val result = route(app, incorporatedEntityJourneyRetrievalRequest("grs-retrieval-data-not-found")).get

        status(result) mustBe NOT_FOUND
      }
    }

    "return 401 when journeyId indicates stubbed unauthorised scenario" in {
      running(fakeApplication()) {
        authorisedUser()

        val result = route(app, incorporatedEntityJourneyRetrievalRequest("grs-retrieval-unauthorised")).get

        status(result) mustBe UNAUTHORIZED
      }
    }

    "default to success-like response for unknown journeyId" in {
      running(fakeApplication()) {
        authorisedUser()

        val result = route(app, incorporatedEntityJourneyRetrievalRequest("something-random")).get

        status(result) mustBe OK
        (journeyRetrievalJson(result) \ "identifiersMatch").as[Boolean] mustBe true
        (journeyRetrievalJson(result) \ "registration" \ "registrationStatus").as[String] mustBe "REGISTERED"
        (journeyRetrievalJson(result) \ "businessVerification" \ "verificationStatus").as[String] mustBe "PASS"
      }
    }

    "return 401 when authorisation throws" in {
      running(fakeApplication()) {
        when(
          mockAuthConnector.authorise(
            any(),
            any()
          )(any(), any())
        ).thenReturn(Future.failed(InsufficientEnrolments()))

        val result = route(app, incorporatedEntityJourneyRetrievalRequest("grs-retrieval-success")).get

        status(result) mustBe UNAUTHORIZED
      }
    }
  }

  "GrsController.retrievePartnershipJourneyData" should {

    "return 200 with success payload without a company profile" in {
      running(fakeApplication()) {
        authorisedUser()

        val result = route(app, partnershipJourneyRetrievalRequest("grs-retrieval-success")).get

        status(result) mustBe OK
        (journeyRetrievalJson(result) \ "sautr").as[String] mustBe "1234567890"
        (journeyRetrievalJson(result) \ "postcode").as[String] mustBe "AA11AA"
        (journeyRetrievalJson(result) \ "identifiersMatch").as[Boolean] mustBe true
        (journeyRetrievalJson(result) \ "registration" \ "registrationStatus").as[String] mustBe "REGISTERED"
        (journeyRetrievalJson(result) \ "registration" \ "registeredBusinessPartnerId").as[String] mustBe "111111"
        (journeyRetrievalJson(result) \ "businessVerification" \ "verificationStatus").as[String] mustBe "PASS"
        (journeyRetrievalJson(result) \ "companyProfile").toOption mustBe None
      }
    }

    "return 200 with success payload including a company profile for incorporated partnership types" in {
      running(fakeApplication()) {
        authorisedUser()

        val result =
          route(app, partnershipJourneyRetrievalRequest("grs-retrieval-success-incorporated-partnership")).get

        status(result) mustBe OK
        (journeyRetrievalJson(result) \ "sautr").as[String] mustBe "1234567890"
        (journeyRetrievalJson(result) \ "postcode").as[String] mustBe "AA11AA"
        (journeyRetrievalJson(result) \ "identifiersMatch").as[Boolean] mustBe true
        (journeyRetrievalJson(result) \ "companyProfile" \ "companyNumber").as[String] mustBe "12345678"
        (journeyRetrievalJson(result) \ "registration" \ "registrationStatus").as[String] mustBe "REGISTERED"
      }
    }

    "return BV fail scenario" in {
      running(fakeApplication()) {
        authorisedUser()

        val result = route(app, partnershipJourneyRetrievalRequest("grs-retrieval-bv-fail")).get

        status(result) mustBe OK
        (journeyRetrievalJson(result) \ "identifiersMatch").as[Boolean] mustBe true
        (journeyRetrievalJson(result) \ "businessVerification" \ "verificationStatus").as[String] mustBe "FAIL"
        (journeyRetrievalJson(result) \ "registration" \ "registrationStatus")
          .as[String] mustBe "REGISTRATION_NOT_CALLED"
      }
    }

    "return registration failed scenario with a failures array" in {
      running(fakeApplication()) {
        authorisedUser()

        val result = route(app, partnershipJourneyRetrievalRequest("grs-retrieval-registration-failed")).get

        status(result) mustBe OK
        (journeyRetrievalJson(result) \ "registration" \ "registrationStatus").as[String] mustBe "REGISTRATION_FAILED"
        (journeyRetrievalJson(result) \ "registration" \ "failures").as[Seq[JsObject]].size mustBe 2
        (journeyRetrievalJson(result) \ "registration" \ "failures" \ 0 \ "code").as[String] mustBe "INVALID_PAYLOAD"
      }
    }

    "return absent UTR scenario" in {
      running(fakeApplication()) {
        authorisedUser()

        val result = route(app, partnershipJourneyRetrievalRequest("grs-retrieval-absent-utr")).get

        status(result) mustBe OK
        (journeyRetrievalJson(result) \ "identifiersMatch").as[Boolean] mustBe false
        (journeyRetrievalJson(result) \ "businessVerification" \ "verificationStatus").as[String] mustBe "UNCHALLENGED"
        (journeyRetrievalJson(result) \ "registration" \ "registrationStatus")
          .as[String] mustBe "REGISTRATION_NOT_CALLED"
      }
    }

    "return 404 when journey not found" in {
      running(fakeApplication()) {
        authorisedUser()

        val result = route(app, partnershipJourneyRetrievalRequest("grs-retrieval-data-not-found")).get

        status(result) mustBe NOT_FOUND
      }
    }

    "return 401 when journeyId indicates stubbed unauthorised scenario" in {
      running(fakeApplication()) {
        authorisedUser()

        val result = route(app, partnershipJourneyRetrievalRequest("grs-retrieval-unauthorised")).get

        status(result) mustBe UNAUTHORIZED
      }
    }

    "default to success-like response without a company profile for unknown journeyId" in {
      running(fakeApplication()) {
        authorisedUser()

        val result = route(app, partnershipJourneyRetrievalRequest("something-random")).get

        status(result) mustBe OK
        (journeyRetrievalJson(result) \ "identifiersMatch").as[Boolean] mustBe true
        (journeyRetrievalJson(result) \ "registration" \ "registrationStatus").as[String] mustBe "REGISTERED"
        (journeyRetrievalJson(result) \ "companyProfile").toOption mustBe None
      }
    }

    "return 401 when authorisation throws" in {
      running(fakeApplication()) {
        when(
          mockAuthConnector.authorise(
            any(),
            any()
          )(any(), any())
        ).thenReturn(Future.failed(InsufficientEnrolments()))

        val result = route(app, partnershipJourneyRetrievalRequest("grs-retrieval-success")).get

        status(result) mustBe UNAUTHORIZED
      }
    }
  }

  "GrsController create journey endpoints" should {

    "return 201 with journeyStartUrl when credId indicates create journey success (Limited Company)" in {
      running(fakeApplication()) {
        authorisedUser(Some("grs-create-journey-success"))

        val result = route(app, createJourneyRequest(createLimitedCompanyJourneyUrl)).get

        status(result) mustBe CREATED
        (journeyRetrievalJson(result) \ "journeyStartUrl").as[String] mustBe
          "/obligations/enrolment/isa/incorporated-identity-callback?journeyId=grs-create-journey-success"
      }
    }

    "return 201 with journeyStartUrl using GRS retrieval scenario credId (Limited Company)" in {
      running(fakeApplication()) {
        authorisedUser(Some("grs-retrieval-bv-fail"))

        val result = route(app, createJourneyRequest(createLimitedCompanyJourneyUrl)).get

        status(result) mustBe CREATED
        (journeyRetrievalJson(result) \ "journeyStartUrl").as[String] mustBe
          "/obligations/enrolment/isa/incorporated-identity-callback?journeyId=grs-retrieval-bv-fail"
      }
    }

    "return 401 when credId indicates create journey unauthorised (Limited Company)" in {
      running(fakeApplication()) {
        authorisedUser(Some("grs-create-journey-unauthorised"))

        val result = route(app, createJourneyRequest(createLimitedCompanyJourneyUrl)).get

        status(result) mustBe UNAUTHORIZED
      }
    }

    "return 500 when credId indicates create journey upstream error (Limited Company)" in {
      running(fakeApplication()) {
        authorisedUser(Some("grs-create-journey-upstream-error"))

        val result = route(app, createJourneyRequest(createLimitedCompanyJourneyUrl)).get

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "return 400 when credId indicates invalid json stub scenario (Limited Company)" in {
      running(fakeApplication()) {
        authorisedUser(Some("grs-create-journey-invalid-json"))

        val result = route(app, createJourneyRequest(createLimitedCompanyJourneyUrl)).get

        status(result) mustBe BAD_REQUEST
        (journeyRetrievalJson(result) \ "code").as[String] mustBe "INVALID_JSON"
        (journeyRetrievalJson(result) \ "message").as[String] mustBe "Request body is invalid"
      }
    }

    "return 400 when credId indicates invalid urls stub scenario (Limited Company)" in {
      running(fakeApplication()) {
        authorisedUser(Some("grs-create-journey-invalid-urls"))

        val result = route(app, createJourneyRequest(createLimitedCompanyJourneyUrl)).get

        status(result) mustBe BAD_REQUEST
        contentAsString(result) should include("JourneyConfig contained non-relative urls")
      }
    }

    "return 400 when request contains non-relative urls (Limited Company)" in {
      running(fakeApplication()) {
        authorisedUser(Some("grs-create-journey-success"))

        val result = route(app, createJourneyRequest(createLimitedCompanyJourneyUrl, invalidUrlsCreateJourneyJson)).get

        status(result) mustBe BAD_REQUEST
        contentAsString(result) should include("JourneyConfig contained non-relative urls")
      }
    }

    "return 400 when request json does not match the expected model (Limited Company)" in {
      running(fakeApplication()) {
        authorisedUser(Some("grs-create-journey-success"))

        val result = route(
          app,
          createJourneyRequest(createLimitedCompanyJourneyUrl, Json.obj("continueUrl" -> "/testUrl"))
        ).get

        status(result) mustBe BAD_REQUEST
      }
    }

    "return 401 when authorisation throws (Limited Company)" in {
      running(fakeApplication()) {
        when(
          mockAuthConnector.authorise(
            any(),
            any()
          )(any(), any())
        ).thenReturn(Future.failed(InsufficientEnrolments()))

        val result = route(app, createJourneyRequest(createLimitedCompanyJourneyUrl)).get

        status(result) mustBe UNAUTHORIZED
      }
    }

    "return 500 when credentials cannot be retrieved from auth (Limited Company)" in {
      running(fakeApplication()) {
        when(
          mockAuthConnector.authorise(
            any(),
            any()
          )(any(), any())
        ).thenReturn(Future.successful(None))

        val result = route(app, createJourneyRequest(createLimitedCompanyJourneyUrl)).get

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsString(result) mustBe "Internal ID could not be retrieved from Auth"
      }
    }

    // The shared createJourney() handler behaves identically for every entity type, so General
    // Partnership gets the same full scenario coverage as Limited Company above to prove the
    // Partnership Identification routes are wired up to the same stubbed behaviour.

    "return 201 with journeyStartUrl when credId indicates create journey success (General Partnership)" in {
      running(fakeApplication()) {
        authorisedUser(Some("grs-create-journey-success"))

        val result = route(app, createJourneyRequest(createGeneralPartnershipJourneyUrl)).get

        status(result) mustBe CREATED
        (journeyRetrievalJson(result) \ "journeyStartUrl").as[String] mustBe
          "/obligations/enrolment/isa/incorporated-identity-callback?journeyId=grs-create-journey-success"
      }
    }

    "return 401 when credId indicates create journey unauthorised (General Partnership)" in {
      running(fakeApplication()) {
        authorisedUser(Some("grs-create-journey-unauthorised"))

        val result = route(app, createJourneyRequest(createGeneralPartnershipJourneyUrl)).get

        status(result) mustBe UNAUTHORIZED
      }
    }

    "return 500 when credId indicates create journey upstream error (General Partnership)" in {
      running(fakeApplication()) {
        authorisedUser(Some("grs-create-journey-upstream-error"))

        val result = route(app, createJourneyRequest(createGeneralPartnershipJourneyUrl)).get

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "return 400 when credId indicates invalid json stub scenario (General Partnership)" in {
      running(fakeApplication()) {
        authorisedUser(Some("grs-create-journey-invalid-json"))

        val result = route(app, createJourneyRequest(createGeneralPartnershipJourneyUrl)).get

        status(result) mustBe BAD_REQUEST
        (journeyRetrievalJson(result) \ "code").as[String] mustBe "INVALID_JSON"
      }
    }

    "return 400 when request contains non-relative urls (General Partnership)" in {
      running(fakeApplication()) {
        authorisedUser(Some("grs-create-journey-success"))

        val result =
          route(app, createJourneyRequest(createGeneralPartnershipJourneyUrl, invalidUrlsCreateJourneyJson)).get

        status(result) mustBe BAD_REQUEST
        contentAsString(result) should include("JourneyConfig contained non-relative urls")
      }
    }

    // Remaining entity types just need a smoke test to prove the route wiring, since the create
    // journey behaviour itself is fully covered above.

    "return 201 for Registered Society journey creation" in {
      running(fakeApplication()) {
        authorisedUser(Some("grs-create-journey-success"))

        val result = route(app, createJourneyRequest(createRegisteredSocietyJourneyUrl)).get

        status(result) mustBe CREATED
      }
    }

    "return 201 for Scottish Partnership journey creation" in {
      running(fakeApplication()) {
        authorisedUser(Some("grs-create-journey-success"))

        val result = route(app, createJourneyRequest(createScottishPartnershipJourneyUrl)).get

        status(result) mustBe CREATED
      }
    }

    "return 201 for Scottish Limited Partnership journey creation" in {
      running(fakeApplication()) {
        authorisedUser(Some("grs-create-journey-success"))

        val result = route(app, createJourneyRequest(createScottishLimitedPartnershipJourneyUrl)).get

        status(result) mustBe CREATED
      }
    }

    "return 201 for Limited Partnership journey creation" in {
      running(fakeApplication()) {
        authorisedUser(Some("grs-create-journey-success"))

        val result = route(app, createJourneyRequest(createLimitedPartnershipJourneyUrl)).get

        status(result) mustBe CREATED
      }
    }

    "return 201 for Limited Liability Partnership journey creation" in {
      running(fakeApplication()) {
        authorisedUser(Some("grs-create-journey-success"))

        val result = route(app, createJourneyRequest(createLimitedLiabilityPartnershipJourneyUrl)).get

        status(result) mustBe CREATED
      }
    }
  }
}
