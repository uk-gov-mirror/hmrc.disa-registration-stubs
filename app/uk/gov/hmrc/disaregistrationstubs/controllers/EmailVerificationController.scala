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

package uk.gov.hmrc.disaregistrationstubs.controllers

import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.disaregistrationstubs.models.emailverification.*
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}

@Singleton
class EmailVerificationController @Inject() (
  cc: ControllerComponents
) extends BackendController(cc)
    with Logging {

  def sendCode(): Action[SendCodeV2Request] =
    Action(parse.json[SendCodeV2Request]) { implicit request =>
      request.body.email match {

        case "code-not-sent@test.com" =>
          logger.info("Returning CODE_NOT_SENT response")

          BadRequest(
            Json.toJson(
              SendCodeResult(
                status = "CODE_NOT_SENT",
                message = None
              )
            )
          )

        case "server-error@test.com" =>
          logger.info("Returning internal server error response")

          InternalServerError

        case _ =>
          logger.info("Returning CODE_SENT response")

          Ok(
            Json.toJson(
              SendCodeResult(
                status = "CODE_SENT",
                message = Some("Email containing verification code has been sent")
              )
            )
          )
      }
    }

  def verifyCode(): Action[VerifyCodeV2Request] =
    Action(parse.json[VerifyCodeV2Request]) { implicit request =>
      val code = request.body.verificationCode

      code match {

        case "NOTVAL" =>
          logger.info("Returning CODE_NOT_VALIDATED response")
          BadRequest(
            Json.toJson(
              VerifyCodeResult(
                status = "CODE_NOT_VALIDATED",
                message = None
              )
            )
          )

        case "NOTFND" =>
          logger.info("Returning CODE_NOT_FOUND response")
          NotFound(
            Json.toJson(
              VerifyCodeResult(
                status = "CODE_NOT_FOUND",
                message = Some("Verification code not found")
              )
            )
          )

        case "SERERR" =>
          logger.info("Returning internal server error response")
          InternalServerError

        case "ABCDEF" =>
          logger.info("Returning CODE_VERIFIED response")
          Ok(
            Json.toJson(
              VerifyCodeResult(
                status = "CODE_VERIFIED",
                message = Some("The verification code for the email verified successfully")
              )
            )
          )

        case _ =>
          logger.info(s"Unknown verification code received: $code")
          BadRequest(
            Json.toJson(
              VerifyCodeResult(
                status = "CODE_NOT_VALIDATED",
                message = Some("Invalid verification code")
              )
            )
          )
      }
    }
}
