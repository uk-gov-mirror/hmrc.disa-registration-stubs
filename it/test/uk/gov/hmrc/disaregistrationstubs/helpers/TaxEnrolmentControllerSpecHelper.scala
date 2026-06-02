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

package uk.gov.hmrc.disaregistrationstubs.helpers

import uk.gov.hmrc.disaregistrationstubs.models.{TaxEnrollmentSubs, groupIdIdentifier}

import java.time.Instant

trait TaxEnrolmentControllerSpecHelper {
  private val mockIdentifiers = groupIdIdentifier("ZREF", "Z0001")

  val expectedErrorState: Seq[TaxEnrollmentSubs] = Seq(TaxEnrollmentSubs(
    Instant.now,
    Instant.now,
    identifiers = Some(Seq(mockIdentifiers)),
    Some("url passed in by the subscriber service"),
    "ERROR",
    Some("da4053bf-2ea3-4cb8-bb9c-65b70252b656"),
    Some("error message"),
    groupIdentifier = Some("groupId-state-error")
  ))

  val expectedSucceededState: Seq[TaxEnrollmentSubs] = Seq(TaxEnrollmentSubs(
    Instant.now,
    Instant.now,
    identifiers = Some(Seq(mockIdentifiers)),
    Some("url passed in by the subscriber service"),
    "SUCCEEDED",
    Some("da4053bf-2ea3-4cb8-bb9c-65b70252b656"),
    groupIdentifier = Some("testGroudId")
  ))

  val expectedPendingState: Seq[TaxEnrollmentSubs] = Seq(expectedSucceededState.head.copy(state = "PENDING",groupIdentifier =Some("groupId-state-pending")))

  val expectedOfflineState: Seq[TaxEnrollmentSubs] = Seq(expectedSucceededState.head.copy(state = "OFFLINE",groupIdentifier =Some("groupId-state-offline")))
  
  val expectedNotFoundState: Seq[TaxEnrollmentSubs] = Seq()
}
