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
import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents, RequestHeader, Result}
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.credentials
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisationException, AuthorisedFunctions}
import uk.gov.hmrc.disaregistrationstubs.config.AppConfig
import uk.gov.hmrc.disaregistrationstubs.models.GrsCreateJourneyRequest
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.bootstrap.binders.{AbsoluteWithHostnameFromAllowlist, OnlyRelative}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GrsController @Inject() (
  cc: ControllerComponents,
  val authConnector: AuthConnector,
  appConfig: AppConfig
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with AuthorisedFunctions
    with Logging {

  def createLimitedCompanyJourney(): Action[GrsCreateJourneyRequest] = createJourney()

  def createRegisteredSocietyJourney(): Action[GrsCreateJourneyRequest] = createJourney()

  def createGeneralPartnershipJourney(): Action[GrsCreateJourneyRequest] = createJourney()

  def createScottishPartnershipJourney(): Action[GrsCreateJourneyRequest] = createJourney()

  def createScottishLimitedPartnershipJourney(): Action[GrsCreateJourneyRequest] = createJourney()

  def createLimitedPartnershipJourney(): Action[GrsCreateJourneyRequest] = createJourney()

  def createLimitedLiabilityPartnershipJourney(): Action[GrsCreateJourneyRequest] = createJourney()

  private def createJourney(): Action[GrsCreateJourneyRequest] =
    Action(parse.json[GrsCreateJourneyRequest]).async { implicit request =>
      authorised()
        .retrieve(credentials) { creds =>
          val response = creds match {
            case None =>
              logger.warn("Credentials could not be retrieved from Auth when creating GRS journey")
              InternalServerError("Internal ID could not be retrieved from Auth")

            case Some(Credentials(credId, _)) =>
              credId match {
                case "grs-create-journey-unauthorised" =>
                  logger.info("Returning stubbed unauthorized response for create GRS journey")
                  Unauthorized

                case "grs-create-journey-upstream-error" =>
                  logger.info("Returning stubbed upstream error response for create GRS journey")
                  InternalServerError

                case "grs-create-journey-invalid-json" =>
                  logger.info("Returning stubbed invalid JSON response for create GRS journey")
                  BadRequest(
                    Json.obj(
                      "code"    -> "INVALID_JSON",
                      "message" -> "Request body is invalid"
                    )
                  )

                case "grs-create-journey-invalid-urls" =>
                  logger.info("Returning stubbed invalid URLs response for create GRS journey")
                  BadRequest(Json.toJson("JourneyConfig contained non-relative urls"))

                case _ if hasInvalidUrls(request.body) =>
                  logger.warn(s"Create GRS journey request contained invalid URLs for credId: $credId")
                  BadRequest(Json.toJson("JourneyConfig contained non-relative urls"))

                case "grs-create-journey-success" | _ =>
                  logger.info(s"Returning created GRS journey response for credId: $credId")
                  Created(
                    Json.obj(
                      "journeyStartUrl" ->
                        s"/obligations/enrolment/isa/incorporated-identity-callback?journeyId=$credId"
                    )
                  )
              }
          }
          Future.successful(response)
        }
        .recover { case _: AuthorisationException =>
          logger.warn("Authorisation failed when creating GRS journey")
          Unauthorized
        }
    }

  def retrieveIncorporatedEntityJourneyData(journeyId: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedRetrieval(s"retrieving GRS journey data for journeyId: $journeyId") {
      journeyId match {
        case "grs-retrieval-unauthorised" =>
          logger.info("Returning stubbed unauthorized response for GRS journey data retrieval")
          Unauthorized

        case "grs-retrieval-success" =>
          logger.info("Returning GRS retrieval success scenario")
          Ok(
            grsJson(
              identifiersMatch = true,
              bvStatus = Some("PASS"),
              registrationStatus = "REGISTERED",
              bpId = Some("111111")
            )
          )

        case "grs-retrieval-success-ct-enrolled" =>
          logger.info("Returning GRS retrieval CT enrolled success scenario")
          Ok(
            grsJson(
              identifiersMatch = true,
              bvStatus = Some("CT_ENROLLED"),
              registrationStatus = "REGISTERED",
              bpId = Some("111111")
            )
          )

        case "grs-retrieval-bv-fail" =>
          logger.info("Returning GRS retrieval business verification fail scenario")
          Ok(
            grsJson(
              identifiersMatch = true,
              bvStatus = Some("FAIL"),
              registrationStatus = "REGISTRATION_NOT_CALLED"
            )
          )

        case "grs-retrieval-registration-failed" =>
          logger.info("Returning GRS retrieval registration failed scenario")
          Ok(
            grsJson(
              identifiersMatch = true,
              bvStatus = Some("PASS"),
              registrationStatus = "REGISTRATION_FAILED"
            )
          )

        case "grs-retrieval-absent-utr" =>
          logger.info("Returning GRS retrieval absent UTR scenario")
          Ok(
            grsJson(
              identifiersMatch = false,
              bvStatus = Some("UNCHALLENGED"),
              registrationStatus = "REGISTRATION_NOT_CALLED"
            )
          )

        case "grs-retrieval-data-not-found" =>
          logger.info("Returning GRS retrieval data not found scenario")
          NotFound

        case _ =>
          logger.info(s"Returning default GRS retrieval success scenario for journeyId: $journeyId")
          Ok(
            grsJson(
              identifiersMatch = true,
              bvStatus = Some("PASS"),
              registrationStatus = "REGISTERED",
              bpId = Some("111111")
            )
          )
      }
    }
  }

  def retrievePartnershipJourneyData(journeyId: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedRetrieval(s"retrieving partnership journey data for journeyId: $journeyId") {
      journeyId match {
        case "grs-retrieval-unauthorised" =>
          logger.info("Returning stubbed unauthorized response for partnership journey data retrieval")
          Unauthorized

        case "grs-retrieval-success" =>
          logger.info("Returning partnership retrieval success scenario")
          Ok(
            partnershipJson(
              identifiersMatch = true,
              bvStatus = Some("PASS"),
              registrationStatus = "REGISTERED",
              bpId = Some("111111")
            )
          )

        case "grs-retrieval-success-incorporated-partnership" =>
          logger.info("Returning partnership retrieval success scenario including company profile")
          Ok(
            partnershipJson(
              identifiersMatch = true,
              bvStatus = Some("PASS"),
              registrationStatus = "REGISTERED",
              bpId = Some("111111"),
              includeCompanyProfile = true
            )
          )

        case "grs-retrieval-bv-fail" =>
          logger.info("Returning partnership retrieval business verification fail scenario")
          Ok(
            partnershipJson(
              identifiersMatch = true,
              bvStatus = Some("FAIL"),
              registrationStatus = "REGISTRATION_NOT_CALLED"
            )
          )

        case "grs-retrieval-registration-failed" =>
          logger.info("Returning partnership retrieval registration failed scenario")
          Ok(
            partnershipJson(
              identifiersMatch = true,
              bvStatus = Some("PASS"),
              registrationStatus = "REGISTRATION_FAILED",
              failures = Some(
                Json.arr(
                  Json.obj(
                    "code"   -> "INVALID_PAYLOAD",
                    "reason" -> "Request has not passed validation. Invalid Payload."
                  ),
                  Json.obj(
                    "code"   -> "INVALID_REGIME",
                    "reason" -> "Request has not passed validation. Invalid Regime."
                  )
                )
              )
            )
          )

        case "grs-retrieval-absent-utr" =>
          logger.info("Returning partnership retrieval absent UTR scenario")
          Ok(
            partnershipJson(
              identifiersMatch = false,
              bvStatus = Some("UNCHALLENGED"),
              registrationStatus = "REGISTRATION_NOT_CALLED"
            )
          )

        case "grs-retrieval-data-not-found" =>
          logger.info("Returning partnership retrieval data not found scenario")
          NotFound

        case _ =>
          logger.info(s"Returning default partnership retrieval success scenario for journeyId: $journeyId")
          Ok(
            partnershipJson(
              identifiersMatch = true,
              bvStatus = Some("PASS"),
              registrationStatus = "REGISTERED",
              bpId = Some("111111")
            )
          )
      }
    }
  }

  private def authorisedRetrieval(
    logContext: String
  )(result: => Result)(implicit request: RequestHeader): Future[Result] =
    authorised() {
      Future.successful(result)
    }.recover { case _: AuthorisationException =>
      logger.warn(s"Authorisation failed when $logContext")
      Unauthorized
    }

  private def grsJson(
    identifiersMatch: Boolean,
    bvStatus: Option[String],
    registrationStatus: String,
    bpId: Option[String] = None
  ): JsObject = {
    val base = Json.obj(
      "companyProfile"   -> companyProfileJson,
      "identifiersMatch" -> identifiersMatch,
      "registration"     -> registrationJson(registrationStatus, bpId),
      "ctutr"            -> "1234567890"
    )

    withBusinessVerification(base, bvStatus)
  }

  private def partnershipJson(
    identifiersMatch: Boolean,
    bvStatus: Option[String],
    registrationStatus: String,
    bpId: Option[String] = None,
    includeCompanyProfile: Boolean = false,
    failures: Option[JsArray] = None
  ): JsObject = {
    val base = Json.obj(
      "sautr"            -> "1234567890",
      "postcode"         -> "AA11AA",
      "identifiersMatch" -> identifiersMatch,
      "registration"     -> registrationJson(registrationStatus, bpId, failures)
    )

    val withBv = withBusinessVerification(base, bvStatus)

    if (includeCompanyProfile) withBv ++ Json.obj("companyProfile" -> companyProfileJson)
    else withBv
  }

  private def registrationJson(
    registrationStatus: String,
    bpId: Option[String] = None,
    failures: Option[JsArray] = None
  ): JsObject =
    Json.obj("registrationStatus" -> registrationStatus) ++
      bpId.map(id => Json.obj("registeredBusinessPartnerId" -> id)).getOrElse(Json.obj()) ++
      failures.map(f => Json.obj("failures" -> f)).getOrElse(Json.obj())

  private def withBusinessVerification(json: JsObject, bvStatus: Option[String]): JsObject =
    bvStatus
      .map(status => json ++ Json.obj("businessVerification" -> Json.obj("verificationStatus" -> status)))
      .getOrElse(json)

  private val companyProfileJson: JsObject = Json.obj(
    "companyName"            -> "Test Ltd",
    "companyNumber"          -> "12345678",
    "dateOfIncorporation"    -> "2020-01-01",
    "unsanitisedCHROAddress" -> Json.obj(
      "address_line_1" -> "1 Test Street",
      "address_line_2" -> "Town",
      "locality"       -> "County",
      "postal_code"    -> "AA11AA"
    )
  )

  private def hasInvalidUrls(request: GrsCreateJourneyRequest): Boolean =
    Seq(
      request.continueUrl,
      request.signOutUrl,
      request.accessibilityUrl
    ).exists(url => !isValidUrl(url))

  private def isValidUrl(url: String): Boolean =
    OnlyRelative.applies(url) || AbsoluteWithHostnameFromAllowlist.apply(appConfig.allowedHosts).applies(url)
}
