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

import org.scalatest.matchers.must.Matchers.mustBe
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import utils.BaseUnitSpec

import scala.concurrent.Future

class EmailVerificationControllerSpec extends BaseUnitSpec {

  private val sendCodeUrl   = "/email-verification/v2/send-code"
  private val verifyCodeUrl = "/email-verification/v2/verify-code"

  private def sendCodeRequest(json: JsValue) =
    FakeRequest(POST, sendCodeUrl)
      .withHeaders(
        CONTENT_TYPE -> "application/json"
      )
      .withJsonBody(json)

  private def verifyCodeRequest(json: JsValue) =
    FakeRequest(POST, verifyCodeUrl)
      .withHeaders(
        CONTENT_TYPE -> "application/json"
      )
      .withJsonBody(json)

  private def responseJson(result: Future[Result]): JsValue =
    contentAsJson(result)

  private def sendCodeJson(email: String): JsObject =
    Json.obj(
      "email" -> email
    )

  private def verifyCodeJson(
    email: String,
    verificationCode: String = "123456"
  ): JsObject =
    Json.obj(
      "email"            -> email,
      "verificationCode" -> verificationCode
    )

  "EmailVerificationController.sendCode" should {

    "return 200 with CODE_SENT response when email maps to success scenario" in {
      running(fakeApplication()) {
        val result = route(app, sendCodeRequest(sendCodeJson("success@test.com"))).get

        status(result) mustBe OK

        val json = responseJson(result)

        (json \ "status").as[String] mustBe "CODE_SENT"
        (json \ "message").as[String] mustBe "Email containing verification code has been sent"
      }
    }

    "return 400 with CODE_NOT_SENT response when email maps to code not sent scenario" in {
      running(fakeApplication()) {
        val result = route(app, sendCodeRequest(sendCodeJson("code-not-sent@test.com"))).get

        status(result) mustBe BAD_REQUEST

        val json = responseJson(result)

        (json \ "status").as[String] mustBe "CODE_NOT_SENT"
      }
    }

    "return 500 when email maps to internal server error scenario" in {
      running(fakeApplication()) {
        val result = route(app, sendCodeRequest(sendCodeJson("server-error@test.com"))).get

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "return 400 when send code request json does not match expected model" in {
      running(fakeApplication()) {
        val result = route(
          app,
          sendCodeRequest(
            Json.obj("invalidField" -> "value")
          )
        ).get

        status(result) mustBe BAD_REQUEST
      }
    }
  }

  "EmailVerificationController.verifyCode" should {

    "return 200 with CODE_VERIFIED response when verification code is ABCDEF" in {
      running(fakeApplication()) {
        val result =
          route(app, verifyCodeRequest(verifyCodeJson("any@test.com", "ABCDEF"))).get

        status(result) mustBe OK

        val json = responseJson(result)

        (json \ "status").as[String] mustBe "CODE_VERIFIED"
        (json \ "message").as[String] mustBe
          "The verification code for the email verified successfully"
      }
    }

    "return 400 with CODE_NOT_VALIDATED response when verification code is NOTVAL" in {
      running(fakeApplication()) {
        val result =
          route(app, verifyCodeRequest(verifyCodeJson("any@test.com", "NOTVAL"))).get

        status(result) mustBe BAD_REQUEST

        val json = responseJson(result)

        (json \ "status").as[String] mustBe "CODE_NOT_VALIDATED"
        (json \ "message").isDefined mustBe false
      }
    }

    "return 404 with CODE_NOT_FOUND response when verification code is NOTFND" in {
      running(fakeApplication()) {
        val result =
          route(app, verifyCodeRequest(verifyCodeJson("any@test.com", "NOTFND"))).get

        status(result) mustBe NOT_FOUND

        val json = responseJson(result)

        (json \ "status").as[String] mustBe "CODE_NOT_FOUND"
        (json \ "message").as[String] mustBe "Verification code not found"
      }
    }

    "return 500 when verification code is SERERR" in {
      running(fakeApplication()) {
        val result =
          route(app, verifyCodeRequest(verifyCodeJson("any@test.com", "SERERR"))).get

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "return 400 with CODE_NOT_VALIDATED when verification code is unknown" in {
      running(fakeApplication()) {
        val result =
          route(app, verifyCodeRequest(verifyCodeJson("any@test.com", "RANDOM"))).get

        status(result) mustBe BAD_REQUEST

        val json = responseJson(result)

        (json \ "status").as[String] mustBe "CODE_NOT_VALIDATED"
        (json \ "message").as[String] mustBe "Invalid verification code"
      }
    }

    "return 400 when verify code request json does not match expected model" in {
      running(fakeApplication()) {
        val result =
          route(
            app,
            verifyCodeRequest(
              Json.obj(
                "email" -> "test@test.com"
              )
            )
          ).get

        status(result) mustBe BAD_REQUEST
      }
    }
  }
}
