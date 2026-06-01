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
