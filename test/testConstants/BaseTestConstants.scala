/*
 * Copyright 2022 HM Revenue & Customs
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

package testConstants

import auth.{MtdItUser, MtdItUserOptionNino, MtdItUserWithNino}
import config.FrontendAppConfig
import models.navBar.{ListLinks, NavContent, NavLinks}
import models.core.Nino
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.test.FakeRequest
import play.twirl.api.{Html, HtmlFormat}
import testConstants.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import testUtils.UnitSpec
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Name, ~}

object BaseTestConstants extends UnitSpec with GuiceOneAppPerSuite {

  val testMtditid = "XAIT0000123456"
  val testMtditidAgent = "XAIT00000000015"
  val testNinoAgent = "AA111111A"
  val testNino = "AB123456C"
  val testUserNino: Nino = Nino(testNino)
  val testSaUtrId = "1234567890"
  val testSaUtr = "testSaUtr"
  val taxYear: String = "2020-04-05"
  val taxYear2020: String = "2020"
  val idNumber = "1234567890"
  val idType: String = "utr"
  val ninoIdType: String = "NINO"
  val docNumber: String = "XM0026100122"
  val testArn = "XAIT0000123456"
  val testCredId = "testCredId"
  val testUserTypeIndividual = "Individual"
  val testUserTypeAgent = "Agent"
  val testUserType: String = testUserTypeIndividual
  val testTaxYear = 2018
  val testYearPlusOne = 2019
  val testYearPlusTwo = 2020
  val testYearPlusThree = 2021
  val testYearPlusFour = 2022
  val testUserName = "Albert Einstein"
  val testRetrievedUserName: Name = Name(Some(testUserName), None)
  val testPaymentRedirectUrl = "http://localhost:9081/report-quarterly/income-and-expenses/view"
  lazy val testMtdUserNoNino: MtdItUserOptionNino[_] = MtdItUserOptionNino(testMtditid, None, None, Some(testSaUtr), Some(testCredId), Some(testUserTypeIndividual))(FakeRequest())
  lazy implicit val testMtdUserNino: MtdItUserWithNino[_] = MtdItUserWithNino(testMtditid, testNino, Some(testRetrievedUserName), btaNavPartial = None, Some(testSaUtr), Some(testCredId), userType = Some(testUserTypeIndividual), arn = None)(FakeRequest())
  lazy val testMtdItUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testRetrievedUserName),
    businessesAndPropertyIncome, btaNavPartial = None, Some(testSaUtr), Some(testCredId), Some(testUserTypeIndividual), None)(FakeRequest())
  lazy val testMtdItAgentUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testRetrievedUserName),
    businessesAndPropertyIncome, btaNavPartial = None, Some(testSaUtr), Some(testCredId), Some(testUserTypeAgent), Some(testArn))(FakeRequest())
  lazy val testMtdItUserMinimal: MtdItUser[_] = MtdItUser(testMtditid, testNino, userName = None,
    incomeSources = businessesAndPropertyIncome, btaNavPartial = None, saUtr = None, credId = None, userType = None, arn = None)(FakeRequest())
  val testSelfEmploymentId = "XA00001234"
  val testSelfEmploymentId2 = "XA00001235"
  val testPropertyIncomeId = "1234"
  val testTaxCalculationId = "CALCID"
  val testTimeStampString = "2017-07-06T12:34:56.789Z"
  val testYear2017 = 2017
  val testMigrationYear2019 = "2019"
  val testFrom = "2016-04-06"
  val testTo = "2017-04-05"
  val testPaymentLot = "081203010024"
  val testPaymentLotItem = "000001"
  val testErrorStatus: Int = Status.INTERNAL_SERVER_ERROR
  val testErrorNotFoundStatus: Int = Status.NOT_FOUND
  val testErrorMessage = "Dummy Error Message"
  implicit val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val testListLink = Seq(
    ListLinks("testEnHome", appConfig.homePageUrl),
    ListLinks("testEnAccount", "testUrl"),
    ListLinks("testEnMessages", "testUrl", Some("0")),
    ListLinks("testEnForm", "testUrl", Some("1")),
    ListLinks("testEnHelp", "testUrl")
  )

  val testListLinkCy = Seq(
    ListLinks("testEnHome", appConfig.homePageUrl),
    ListLinks("testEnAccount", "testUrl"),
    ListLinks("testEnMessages", "testUrl", Some("0")),
    ListLinks("testEnForm", "testUrl", Some("1")),
    ListLinks("testEnHelp", "testUrl")
  )

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
  val testCredentials: Option[Credentials] = Some(Credentials(testCredId, ""))

  val testAgentAuthRetrievalSuccess = new ~(new ~(new ~(Enrolments(Set(arnEnrolment)), Some(AffinityGroup.Agent)), testConfidenceLevel), testCredentials)
  val testAgentAuthRetrievalSuccessNoEnrolment = new ~(new ~(new ~(Enrolments(Set()), Some(AffinityGroup.Agent)), testConfidenceLevel), testCredentials)

  val testReferrerUrl = "/test/url"

  val testNavHtml: Option[Html] = Some(HtmlFormat.raw(
    "<html><head></head><body>  <nav id='secondary-nav' class='hmrc-account-menu'> " +
      "<ul class='hmrc-account-menu__main govuk-grid-column-full' style='padding: 0;'>" +
      "<li> <a href='http://localhost:9081/report-quarterly/income-and-expenses/view' id='nav-bar-link-testEnHome' class='hmrc-account-menu__link'> testEnHome </a> </li>" +
      "<li> <a href='testUrl' id='nav-bar-link-testEnAccount' class='hmrc-account-menu__link'> testEnAccount </a> </li>" +
      "<li> <a href='testUrl' id='nav-bar-link-testEnMessages' class='hmrc-account-menu__link'> testEnMessages </a> </li>" +
      "<li> <a href='testUrl' id='nav-bar-link-testEnForm' class='hmrc-account-menu__link'> testEnForm <span class='hmrc-notification-badge'>1</span> </a> </li>" +
      "<li> <a href='testUrl' id='nav-bar-link-testEnHelp' class='hmrc-account-menu__link'> testEnHelp </a> </li>" +
      "</ul>  </nav> </body></html>"))
}
