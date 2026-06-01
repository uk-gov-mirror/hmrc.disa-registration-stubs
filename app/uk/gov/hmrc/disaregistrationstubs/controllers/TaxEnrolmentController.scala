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
import play.api.libs.json.*
import play.api.mvc.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.disaregistrationstubs.connectors.RegistrationConnector
import uk.gov.hmrc.disaregistrationstubs.controllers.TaxEnrolmentController.*
import uk.gov.hmrc.disaregistrationstubs.helpers.TaxEnrolmentControllerHelper
import uk.gov.hmrc.disaregistrationstubs.models.{TaxEnrollmentSubs, TaxEnrolmentCallbackRequest, TaxEnrolmentSubscriberRequest}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaxEnrolmentController @Inject() (
  cc: ControllerComponents,
  override val authConnector: AuthConnector,
  taxEnrolmentConnector: RegistrationConnector
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with AuthorisedFunctions
    with Logging
    with TaxEnrolmentControllerHelper {

  def subscribe(subscriptionId: String): Action[JsValue] =
    Action.async(parse.json) { implicit request =>
      authorised()
        .retrieve(Retrievals.credentials) {
          case Some(Credentials(credId, _)) =>
            request.body
              .validate[TaxEnrolmentSubscriberRequest]
              .fold(
                errors => {
                  logger.warn(
                    s"Parsing Tax Enrolments subscriber request failed for formBundleId: [$subscriptionId], errors: ${JsError
                        .toJson(errors)}"
                  )
                  Future.successful(BadRequest("Invalid request payload"))
                },
                payload => handleScenario(subscriptionId, credId, payload)
              )

          case None =>
            logger.warn(s"No credentials returned from authorise call for formBundleId: [$subscriptionId]")
            Future.successful(Unauthorized)
        }
        .recover { case e: AuthorisationException =>
          logger.warn(s"Authorise call failed for Tax Enrolments formBundleId: [$subscriptionId]", e)
          Unauthorized
        }
    }

  def checkGroupIdSubscription(groupId: String): Action[AnyContent] =
    Action.async { implicit request =>
      authorised() {
        groupId match {

          case "groupId-state-pending" => Future.successful(Ok(Json.toJson(makeResponse(groupIdPending, PendingState))))

          case "groupId-state-offline" => Future.successful(Ok(Json.toJson(makeResponse(groupIdOffline, OfflineState))))

          case "groupId-state-error" => Future.successful(Ok(Json.toJson(makeResponse(groupIdError, ErrorState))))

          case "groupId-notfound" => Future.successful(Ok(Json.toJson(Seq.empty[String])))

          case null | " " => Future.successful(InternalServerError)

          case aGroupId => Future.successful(Ok(Json.toJson(makeResponse(aGroupId, SucceededState))))

        }
      }
    }

  private def handleScenario(
    subscriptionId: String,
    credId: String,
    payload: TaxEnrolmentSubscriberRequest
  )(implicit hc: HeaderCarrier): Future[Result] =
    credId match {
      case BadRequestCredId =>
        logger.info(
          s"Tax Enrolments bad request response triggered for formBundleId: [$subscriptionId], etmpId: [${payload.etmpId}]"
        )
        Future.successful(BadRequest("Bad request from Tax Enrolments stub"))

      case InternalServerErrorCredId =>
        logger.info(
          s"Tax Enrolments internal server error response triggered for formBundleId: [$subscriptionId], etmpId: [${payload.etmpId}]"
        )
        Future.successful(InternalServerError("Internal server error from Tax Enrolments stub"))

      case SuccessCredId =>
        callCallback(subscriptionId, payload, callbackPayload(subscriptionId, SucceededState))
        Future.successful(NoContent)

      case IssuerFailureCredId =>
        callCallback(
          subscriptionId,
          payload,
          callbackPayload(
            subscriptionId = subscriptionId,
            state = ErrorState,
            errorResponse = Some("error message")
          )
        )
        Future.successful(NoContent)

      case EnrolmentErrorCredId =>
        callCallback(subscriptionId, payload, partialFailurePayload(subscriptionId, EnrolmentErrorState))
        Future.successful(NoContent)

      case EnrolledCredId =>
        callCallback(subscriptionId, payload, partialFailurePayload(subscriptionId, EnrolledState))
        Future.successful(NoContent)

      case AuthRefreshedCredId =>
        callCallback(subscriptionId, payload, partialFailurePayload(subscriptionId, AuthRefreshedState))
        Future.successful(NoContent)

      case _ =>
        logger.info(
          s"No specific Tax Enrolments credId scenario matched for credId: [$credId]. Defaulting to success for formBundleId: [$subscriptionId]"
        )
        callCallback(subscriptionId, payload, callbackPayload(subscriptionId, SucceededState))
        Future.successful(NoContent)
    }

  private def callCallback(
    subscriptionId: String,
    subscriberRequest: TaxEnrolmentSubscriberRequest,
    callbackPayload: TaxEnrolmentCallbackRequest
  )(implicit hc: HeaderCarrier): Unit = {
    logger.info(
      s"Calling Tax Enrolments callback URL [${subscriberRequest.callback}] for formBundleId: [$subscriptionId], " +
        s"etmpId: [${subscriberRequest.etmpId}], state: [${callbackPayload.state}]"
    )

    taxEnrolmentConnector
      .callCallback(
        callbackUrl = subscriberRequest.callback,
        payload = callbackPayload
      )
      .recover { case e =>
        logger.error(
          s"Tax Enrolments callback call failed for formBundleId: [$subscriptionId] and callbackUrl: [${subscriberRequest.callback}]",
          e
        )
      }
  }

  private def callbackPayload(
    subscriptionId: String,
    state: String,
    errorResponse: Option[String] = None
  ): TaxEnrolmentCallbackRequest =
    TaxEnrolmentCallbackRequest(
      url = subscriptionUrl(subscriptionId),
      state = state,
      errorResponse = errorResponse
    )

  private def partialFailurePayload(subscriptionId: String, state: String): TaxEnrolmentCallbackRequest =
    callbackPayload(
      subscriptionId = subscriptionId,
      state = state,
      errorResponse = Some(partialFailureMessage(subscriptionId, state))
    )

  private def partialFailureMessage(subscriptionId: String, state: String): String =
    s"Subscription with formBundleId $subscriptionId partially processed. " +
      s"This could indicate an error but will depend on your regime. " +
      s"Check the state [$state] Note This subscription can still be retried by making a call to " +
      s"PUT /tax-enrolments/subscription/$subscriptionId/issuer"

  private def subscriptionUrl(subscriptionId: String): String =
    s"http://tax-enrolments.service/tax-enrolments/subscriptions/$subscriptionId"
}

object TaxEnrolmentController {
  // Domain responses
  private val SuccessCredId             = "tax-enrolment-success"
  private val IssuerFailureCredId       = "tax-enrolment-issuer-failure"
  private val EnrolmentErrorCredId      = "tax-enrolment-enrolment-error"
  private val EnrolledCredId            = "tax-enrolment-enrolled"
  private val AuthRefreshedCredId       = "tax-enrolment-auth-refreshed"
  // Transport responses
  private val BadRequestCredId          = "tax-enrolment-bad-request"
  private val InternalServerErrorCredId = "tax-enrolment-internal-server-error"
  // Groud Ids
  private val groupIdSucceeded          = "groupId-state-succeeded"
  private val groupIdError              = "groupId-state-error"
  private val groupIdPending            = "groupId-state-pending"
  private val groupIdOffline            = "groupId-state-offline"
  // Enrolment states
  private val SucceededState            = "SUCCEEDED"
  private val ErrorState                = "ERROR"
  private val PendingState              = "PENDING"
  private val OfflineState              = "OFFLINE"
  private val EnrolmentErrorState       = "EnrolmentError"
  private val EnrolledState             = "Enrolled"
  private val AuthRefreshedState        = "AuthRefreshed"
}
