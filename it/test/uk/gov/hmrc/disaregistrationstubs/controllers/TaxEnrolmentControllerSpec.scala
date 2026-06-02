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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.libs.ws.WSClient
import play.api.{Application, inject}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.disaregistrationstubs.helpers.TaxEnrolmentControllerSpecHelper
import uk.gov.hmrc.disaregistrationstubs.utils.Utils

import scala.concurrent.Future

class TaxEnrolmentControllerSpec
  extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with GuiceOneServerPerSuite
    with TaxEnrolmentControllerSpecHelper
    with Utils {


  val mockAuthConnector: AuthConnector = mock[AuthConnector]


  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .overrides(inject.bind[AuthConnector].toInstance(mockAuthConnector))
      .build()

  when(mockAuthConnector.authorise(any(), any())(any(), any()))
    .thenReturn(Future.successful(()))

  private val wsClient = app.injector.instanceOf[WSClient]
  private val baseUrl = s"http://localhost:$port"

  "/tax-enrolments/groups/:groupId/subscriptions endpoint" should {
    "respond with 200 status and an ERROR state payload" in {

      val response = {
        wsClient
          .url(s"$baseUrl/tax-enrolments/groups/groupId-state-error/subscriptions")
          .get()
          .futureValue

      }

      val json = stripFields(Json.parse(response.body).as[JsArray], "created", "lastModified")
      val expected = stripFields(Json.toJson(expectedErrorState).as[JsArray], "created", "lastModified")

      response.status shouldBe 200
      json shouldBe expected
    }

    "respond with 200 status and a not found state payload with a random groupId" in {

      val response = {
        wsClient
          .url(s"$baseUrl/tax-enrolments/groups/testGroupId/subscriptions")
          .get()
          .futureValue
      }

      val json = stripFields(Json.parse(response.body).as[JsArray], "created", "lastModified")
      val expected = stripFields(Json.toJson(expectedNotFoundState).as[JsArray], "created", "lastModified")

      response.status shouldBe 200
      json shouldBe expected
    }

    "respond with 200 status and a not found state payload" in {

      val response = {
        wsClient
          .url(s"$baseUrl/tax-enrolments/groups/groupId-notfound/subscriptions")
          .get()
          .futureValue

      }

      val json = stripFields(Json.parse(response.body).as[JsArray], "created", "lastModified")
      val expected = stripFields(Json.toJson(expectedNotFoundState).as[JsArray], "created", "lastModified")

      response.status shouldBe 200
      json shouldBe expected
    }
    "respond with 200 status and a groupId-state-pending state payload" in {

      val response = {
        wsClient
          .url(s"$baseUrl/tax-enrolments/groups/groupId-state-pending/subscriptions")
          .get()
          .futureValue

      }

      val json = stripFields(Json.parse(response.body).as[JsArray], "created", "lastModified")
      val expected = stripFields(Json.toJson(expectedPendingState).as[JsArray], "created", "lastModified")

      response.status shouldBe 200
      json shouldBe expected
    }
    "respond with 200 status and a groupId-state-offline state payload" in {

      val response = {
        wsClient
          .url(s"$baseUrl/tax-enrolments/groups/groupId-state-offline/subscriptions")
          .get()
          .futureValue

      }

      val json = stripFields(Json.parse(response.body).as[JsArray], "created", "lastModified")
      val expected = stripFields(Json.toJson(expectedOfflineState).as[JsArray], "created", "lastModified")

      response.status shouldBe 200
      json shouldBe expected
    }

    "respond with 500 status if groupId isn't specified" in {

      val response = {
        wsClient
          .url(s"$baseUrl/tax-enrolments/groups/ /subscriptions")
          .get()
          .futureValue

      }

      response.status shouldBe 500
    }
  }

}
