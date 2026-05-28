/*
 * Copyright 2025 HM Revenue & Customs
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
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.disaregistrationstubs.models.{EnrolmentSubmissionRequest, EnrolmentSubmissionResponse}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import scala.util.Random
import javax.inject.Inject

class EtmpController @Inject() (
  cc: ControllerComponents
) extends BackendController(cc)
    with Logging {

  def submitEnrolment(): Action[JsValue] = Action(parse.json) { implicit request =>
    request.body.validate[EnrolmentSubmissionRequest] match {
      case JsSuccess(req, _) =>
        req.p2pPlatform match {
          case Some("submit failure") =>
            logger.info(s"Submission failure response triggered for groupId: [${req.groupId}]")
            BadGateway(Json.obj("error" -> "Downstream error from ETMP stub"))

          case _ =>
            logger.info(s"Submission successful response triggered for groupId: [${req.groupId}]")
            val formBundleId = Random.between(100000000000L, 999999999999L).toString
            Ok(Json.toJson(EnrolmentSubmissionResponse(formBundleId)))
        }

      case JsError(errors) =>
        logger.warn(s"Parsing request for submission failed: ${JsError.toJson(errors)}")
        BadRequest(Json.obj("error" -> "Invalid request payload"))
    }
  }
}
