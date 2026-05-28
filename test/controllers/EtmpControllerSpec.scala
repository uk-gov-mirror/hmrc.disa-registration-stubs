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

import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.must.Matchers.mustBe
import play.api.Play.materializer
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.disaregistrationstubs.controllers.routes.EtmpController
import uk.gov.hmrc.disaregistrationstubs.models.EnrolmentSubmissionResponse
import utils.BaseUnitSpec

class EtmpControllerSpec extends BaseUnitSpec {

  "EtmpController.submitEnrolment" should {

    "must return 200 with EnrolmentSubmissionResponse when request is valid and p2pPlatform is not 'submit failure'" in {
      running(fakeApplication()) {
        val body = Json.parse(
          """
            |{
            |  "groupId": "group-123",
            |  "isaProducts": {
            |    "isaProducts": {
            |      "p2pPlatform": "test"
            |    }
            |  }
            |}
            |""".stripMargin
        )

        val request =
          FakeRequest(POST, EtmpController.submitEnrolment().url)
            .withHeaders("Content-Type" -> "application/json")
            .withBody(body)

        val result = route(app, request).get

        status(result) mustBe OK
        contentType(result) mustBe Some("application/json")

        val response = contentAsJson(result).as[EnrolmentSubmissionResponse]
        response.formBundleId.nonEmpty mustBe true
        response.formBundleId.matches("""\d{12}""") mustBe true
      }
    }

    "must return 200 with EnrolmentSubmissionResponse when request is valid and p2pPlatform is empty" in {
      running(fakeApplication()) {
        val body = Json.parse(
          """
            |{
            |  "groupId": "group-123"
            |}
            |""".stripMargin
        )

        val request =
          FakeRequest(POST, EtmpController.submitEnrolment().url)
            .withHeaders("Content-Type" -> "application/json")
            .withBody(body)

        val result = route(app, request).get

        status(result) mustBe OK
        contentType(result) mustBe Some("application/json")

        val response = contentAsJson(result).as[EnrolmentSubmissionResponse]
        response.formBundleId.nonEmpty mustBe true
        response.formBundleId.matches("""\d{12}""") mustBe true
      }
    }

    "must return 502 when request is valid and p2pPlatform is 'submit failure'" in {
      running(fakeApplication()) {
        val body = Json.parse(
          """
            |{
            |  "groupId": "group-123",
            |  "isaProducts": {
            |    "isaProducts": {
            |      "p2pPlatform": "submit failure"
            |    }
            |  }
            |}
            |""".stripMargin
        )

        val request =
          FakeRequest(POST, EtmpController.submitEnrolment().url)
            .withHeaders("Content-Type" -> "application/json")
            .withBody(body)

        val result = route(app, request).get

        status(result) mustBe BAD_GATEWAY
        contentType(result) mustBe Some("application/json")
        (contentAsJson(result) \ "error").as[String] mustBe "Downstream error from ETMP stub"
      }
    }

    "must return 400 when request payload is invalid" in {
      running(fakeApplication()) {
        val body = Json.parse("""{"json":"bad"}""")

        val request =
          FakeRequest(POST, EtmpController.submitEnrolment().url)
            .withHeaders("Content-Type" -> "application/json")
            .withBody(body)

        val result = route(app, request).get

        status(result) mustBe BAD_REQUEST
        contentType(result) mustBe Some("application/json")
        (contentAsJson(result) \ "error").as[String] mustBe "Invalid request payload"
      }
    }
  }
}
