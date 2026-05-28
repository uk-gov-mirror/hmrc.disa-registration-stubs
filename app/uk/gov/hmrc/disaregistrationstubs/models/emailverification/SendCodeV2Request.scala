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

package uk.gov.hmrc.disaregistrationstubs.models.emailverification

/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import play.api.libs.json.*

case class SendCodeV2Request(email: String)

object SendCodeV2Request {
  implicit val format: OFormat[SendCodeV2Request] = Json.format[SendCodeV2Request]
}

case class SendCodeResult(status: String, message: Option[String])

object SendCodeResult {
  implicit val format: OFormat[SendCodeResult] = Json.format[SendCodeResult]
}

case class VerifyCodeV2Request(
  email: String,
  verificationCode: String
)

object VerifyCodeV2Request {
  implicit val format: OFormat[VerifyCodeV2Request] = Json.format[VerifyCodeV2Request]
}

case class VerifyCodeResult(
  status: String,
  message: Option[String]
)

object VerifyCodeResult {
  implicit val format: OFormat[VerifyCodeResult] = Json.format[VerifyCodeResult]
}
