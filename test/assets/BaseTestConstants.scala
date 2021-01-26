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

import assets.IncomeSourceDetailsTestConstants._
import auth.{MtdItUser, MtdItUserOptionNino, MtdItUserWithNino}
import play.api.http.Status
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Name, ~}
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, Enrolment, EnrolmentIdentifier, Enrolments}

object BaseTestConstants {

  val testMtditid = "XAIT0000123456"
  val testArn = "XAIT0000123456"
  val testNino = "AB123456C"
  val testSaUtr = "saUtr"
  val testCredId  = "credId"
  val testUserType = "individual"
  val testTaxYear = 2018
  val testUserName = "Albert Einstein"
  val testRetrievedUserName: Name = Name(Some(testUserName), None)
  val testPaymentRedirectUrl = "http://localhost:9081/report-quarterly/income-and-expenses/view"
  lazy val testMtdUserNoNino: MtdItUserOptionNino[_] = MtdItUserOptionNino(testMtditid, None, None, Some("saUtr"), Some("credId"), Some("individual"))(FakeRequest())
  lazy implicit val testMtdUserNino: MtdItUserWithNino[_] = MtdItUserWithNino(testMtditid, testNino, None, Some("saUtr"), Some("credId"), Some("individual"))(FakeRequest())
  lazy val testMtdItUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testRetrievedUserName),
    businessesAndPropertyIncome, Some("saUtr"), Some("credId"), Some("individual"))(FakeRequest())
  val testSelfEmploymentId  = "XA00001234"
  val testSelfEmploymentId2 = "XA00001235"
  val testPropertyIncomeId = "1234"
  val testTaxCalculationId = "CALCID"
  val testTimeStampString = "2017-07-06T12:34:56.789Z"
  val testFrom = "2017-01-01"
  val testTo = "2017-01-31"
  val testPaymentLot = "081203010024"
  val testPaymentLotItem = "000001"
  val testErrorStatus = Status.INTERNAL_SERVER_ERROR
  val testErrorMessage = "Dummy Error Message"

  val testAuthSuccessResponse = new ~(new ~(new ~(Enrolments(Set(
    Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", testMtditid)), "activated"),
    Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", testNino)), "activated")
  )), Option(testRetrievedUserName)), Some(Credentials(testCredId,""))), Some(AffinityGroup.Individual))

  val testAuthSuccessWithSaUtrResponse = new ~(new ~(new ~(Enrolments(Set(
    Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", testMtditid)), "activated"),
    Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", testNino)), "activated"),
    Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", "saUtr")), "activated")
  )),Option(testRetrievedUserName)), Some(Credentials(testCredId,""))), Some(AffinityGroup.Individual))

  val testReferrerUrl = "/test/url"

  val arnEnrolment = Enrolment(
    "HMRC-AS-AGENT",
    Seq(EnrolmentIdentifier("ARN", testArn)),
    "Activated"
  )

  val testConfidenceLevel = ConfidenceLevel.L200

}
