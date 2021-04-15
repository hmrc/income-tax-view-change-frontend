/*
 * Copyright 2021 HM Revenue & Customs
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

package assets

import IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import auth.{MtdItUser, MtdItUserOptionNino, MtdItUserWithNino}
import play.api.http.Status
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Name, ~}
import uk.gov.hmrc.auth.core._

object BaseTestConstants {

  val testMtditid = "XAIT0000123456"
  val testMtditidAgent = "XAIT00000000015"
  val testNinoAgent = "AA111111A"
  val testNino = "AB123456C"
  val testSaUtrId = 1234567890
  val testSaUtr = "saUtr"
  val taxYear: String = "2020-04-05"
  val taxYear2020: String = "2020"
  val idNumber: Int = 1234567890
  val idType: String = "utr"
  val testArn = "XAIT0000123456"
  val testCredId = "credId"
  val testUserType = "individual"
  val testTaxYear = 2018
  val testUserName = "Albert Einstein"
  val testRetrievedUserName: Name = Name(Some(testUserName), None)
  val testPaymentRedirectUrl = "http://localhost:9081/report-quarterly/income-and-expenses/view"
  lazy val testMtdUserNoNino: MtdItUserOptionNino[_] = MtdItUserOptionNino(testMtditid, None, None, Some("saUtr"), Some("credId"), Some("individual"))(FakeRequest())
  lazy implicit val testMtdUserNino: MtdItUserWithNino[_] = MtdItUserWithNino(testMtditid, testNino, Some(testRetrievedUserName), Some("saUtr"), Some("credId"), Some("individual"))(FakeRequest())
  lazy val testMtdItUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testRetrievedUserName),
    businessesAndPropertyIncome, Some("saUtr"), Some("credId"), Some("individual"))(FakeRequest())
  val testSelfEmploymentId = "XA00001234"
  val testSelfEmploymentId2 = "XA00001235"
  val testPropertyIncomeId = "1234"
  val testTaxCalculationId = "CALCID"
  val testTimeStampString = "2017-07-06T12:34:56.789Z"
  val testYear2017 = 2017
  val testFrom = "2016-04-06"
  val testTo = "2017-04-05"
  val testPaymentLot = "081203010024"
  val testPaymentLotItem = "000001"
  val testErrorStatus = Status.INTERNAL_SERVER_ERROR
  val testErrorNotFoundStatus = Status.NOT_FOUND
  val testErrorMessage = "Dummy Error Message"

  def testAuthSuccessResponse(confidenceLevel: ConfidenceLevel = ConfidenceLevel.L200,
                              affinityGroup: AffinityGroup = AffinityGroup.Individual) = new ~(new ~(new ~(new ~(Enrolments(Set(
    Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", testMtditid)), "activated"),
    Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", testNino)), "activated")
  )), Option(testRetrievedUserName)), Some(Credentials(testCredId, ""))), Some(affinityGroup)), confidenceLevel)

  def testAuthSuccessResponseOrgNoNino(confidenceLevel: ConfidenceLevel = ConfidenceLevel.L200) = new ~(new ~(new ~(new ~(Enrolments(Set(
    Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", testMtditid)), "activated")
  )), Option(testRetrievedUserName)), Some(Credentials(testCredId, ""))), Some(AffinityGroup.Organisation)), confidenceLevel)

  def testAuthSuccessWithSaUtrResponse(confidenceLevel: ConfidenceLevel = ConfidenceLevel.L200,
                                       affinityGroup: AffinityGroup = AffinityGroup.Individual) = new ~(new ~(new ~(new ~(Enrolments(Set(
    Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", testMtditid)), "activated"),
    Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", testNino)), "activated"),
    Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", "1234567890")), "activated")
  )), Option(testRetrievedUserName)), Some(Credentials(testCredId, ""))), Some(affinityGroup)), confidenceLevel)

  val arnEnrolment: Enrolment = Enrolment(
    "HMRC-AS-AGENT",
    Seq(EnrolmentIdentifier("ARN", testArn)),
    "Activated"
  )

  val testConfidenceLevel: ConfidenceLevel = ConfidenceLevel.L200

  val testAgentAuthRetrievalSuccess = new ~(new ~(Enrolments(Set(arnEnrolment)), Some(AffinityGroup.Agent)), testConfidenceLevel)
  val testAgentAuthRetrievalSuccessNoEnrolment = new ~(new ~(Enrolments(Set()), Some(AffinityGroup.Agent)), testConfidenceLevel)

  val testReferrerUrl = "/test/url"

}
