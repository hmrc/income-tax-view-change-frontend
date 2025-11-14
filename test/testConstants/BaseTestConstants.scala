/*
 * Copyright 2023 HM Revenue & Customs
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

import auth.MtdItUser
import auth.authV2.models.AuthorisedAndEnrolledRequest
import authV2.AuthActionsTestData._
import config.FrontendAppConfig
import enums.MTDIndividual
import models.btaNavBar.ListLinks
import models.core.Nino
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear, TaxYearRange}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.test.FakeRequest
import play.twirl.api.{Html, HtmlFormat}
import testConstants.BusinessDetailsTestConstants.business1
import testConstants.PropertyDetailsTestConstants.propertyDetails
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import testUtils.UnitSpec
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Name, ~}

object BaseTestConstants extends UnitSpec with GuiceOneAppPerSuite {

  val testMtditid = "XAIT00001234567"
  val testMtditid2 = "XAIT00001234578"
  val testMtditidBusinessEndDateForm1 = "XAIT00000270899"
  val testMtditidBusinessEndDateForm2 = "XAIT00000902100"
  val testMtditidAgent = "XAIT00000000015"
  val testNinoAgent = "AA111111A"
  val testNino = "AB123456C"
  val testNinoNino: Nino = Nino(testNino)
  val testSessionId = "xsession-12345"
  val repaymentId = "123456789"
  val dateFrom = "12/02/2021"
  val dateTo = "12/02/2022"
  val testUserNino: Nino = Nino(testNino)
  val testSaUtr = "1234567890"
  val taxYear: Int = 2020
  val taxYearTyped: TaxYear = TaxYear(2019, 2020)
  val taxYear2020: String = "2020"
  val idNumber = "1234567890"
  val idType: String = "utr"
  val ninoIdType: String = "NINO"
  val docNumber: String = "XM0026100122"
  val chargeReference: String = "XD000024425799"
  val testArn = "XAIT0000123456"
  val testCredId = "testCredId"
  val expectedJourneyId: String = "testJourneyId"
  val testUserTypeIndividual = AffinityGroup.Individual
  val testUserTypeAgent = AffinityGroup.Agent
  val testUserType: AffinityGroup = testUserTypeIndividual
  val testTaxYear = 2018
  val testTaxYearRange = "23-24"
  val testTaxYearTo = "2017 to 2018 tax year"
  val calendarYear2018 = 2018
  val testYearPlusOne = 2019
  val testYearPlusTwo = 2020
  val testYearPlusThree = 2021
  val testYearPlusFour = 2022
  val testYearPlusFive = 2023
  val testYearPlusSix = 2024
  val testUserName = "Albert Einstein"
  val testFirstName = "Jon"
  val testSecondName = "Jones"
  val testClientNameString = "Jon Jones"
  val testRetrievedUserName: Name = Name(Some(testUserName), None)
  val testClientName: Name = Name(Some(testFirstName), Some(testSecondName))
  val testPaymentRedirectUrl = "http://localhost:9081/report-quarterly/income-and-expenses/view"
  val testMandationStatusOn = "on"
  val testMandationStatusOff = "off"
  val testSetUpPaymentPlanUrl = "http://localhost:9215/set-up-a-payment-plan/sa-payment-plan"

  lazy val testAuthorisedAndEnrolled: AuthorisedAndEnrolledRequest[_] = defaultAuthorisedAndEnrolledRequest(MTDIndividual, FakeRequest())

  lazy val testMtdItUser: MtdItUser[_] = defaultMTDITUser(Some(testUserTypeIndividual), businessesAndPropertyIncome)

  lazy val testMtdItAgentUser: MtdItUser[_] = defaultMTDITUser(Some(testUserTypeAgent), businessesAndPropertyIncome)

  lazy val testMtdItUserMinimal: MtdItUser[_] = getMinimalMTDITUser(None, businessesAndPropertyIncome)

  lazy val testMtdItUserNoIncomeSource: MtdItUser[_] = defaultMTDITUser(Some(testUserTypeIndividual),
    IncomeSourceDetailsModel(testNino, "", Some("2018"), List(business1.copy("", None, None, None)), List(propertyDetails.copy("", None, None))))
  val testSelfEmploymentId = "XA00001234"
  val testSelfEmploymentId2 = "XA00001235"
  val testSelfEmploymentIdValidation = "XAIS00000000002"
  val testPropertyIncomeId = "1234"
  val testPropertyIncomeId2 = "1235"
  val testHashedSelfEmploymentId: String = "4154473711487316523"
  val testTaxCalculationId = "CALCID"
  val testTimeStampString = "2017-07-06T12:34:56.789Z"
  val testYear2017 = 2017
  val testTaxYear2017: TaxYear = TaxYear(2017, 2018)
  val testTaxYear2016: TaxYear = TaxYear(2016, 2017)
  val testTaxYearRange2017: TaxYearRange = TaxYearRange(testTaxYear2017,testTaxYear2017)
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

  val mtdItEnrolment = Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", testMtditid)), "activated")
  val ninoEnrolment = Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", testNino)), "activated")
  val saEnrolment = Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", "1234567890")), "activated")

  def testAuthSuccessResponse(confidenceLevel: ConfidenceLevel = testConfidenceLevel,
                              affinityGroup: AffinityGroup = AffinityGroup.Individual) = new ~(new ~(new ~(new ~(Enrolments(Set(
    mtdItEnrolment,
    ninoEnrolment
  )), Option(testRetrievedUserName)), Some(Credentials(testCredId, ""))), Some(affinityGroup)), confidenceLevel)

  def testAuthSuccessResponseOrgNoNino(confidenceLevel: ConfidenceLevel = testConfidenceLevel) = new ~(new ~(new ~(new ~(Enrolments(Set(
    mtdItEnrolment
  )), Option(testRetrievedUserName)), Some(Credentials(testCredId, ""))), Some(AffinityGroup.Organisation)), confidenceLevel)

  def testIndividualAuthSuccessWithSaUtrResponse(confidenceLevel: ConfidenceLevel = testConfidenceLevel,
                                                 affinityGroup: AffinityGroup = AffinityGroup.Individual) = new ~(new ~(new ~(new ~(Enrolments(Set(
    mtdItEnrolment,
    ninoEnrolment,
    saEnrolment
  )), Option(testRetrievedUserName)), Some(Credentials(testCredId, ""))), Some(affinityGroup)), confidenceLevel)

  def testAuthAgentSuccessWithSaUtrResponse(confidenceLevel: ConfidenceLevel = testConfidenceLevel,
                                       affinityGroup: AffinityGroup = AffinityGroup.Agent) = new ~(new ~(new ~(new ~(Enrolments(Set(
    arnEnrolment,
    Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", testMtditid)), "activated"),
    Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", testNino)), "activated"),
    Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", "1234567890")), "activated")
  )), Option(testRetrievedUserName)), Some(Credentials(testCredId, ""))), Some(affinityGroup)), confidenceLevel)


  val arnEnrolment: Enrolment = Enrolment(
    "HMRC-AS-AGENT",
    Seq(EnrolmentIdentifier("AgentReferenceNumber", testArn)),
    "Activated"
  )

  val testConfidenceLevel: ConfidenceLevel = appConfig.requiredConfidenceLevel match {
    case 200 => ConfidenceLevel.L200
    case 250 => ConfidenceLevel.L250
    case ex => throw new NoSuchElementException(s"Invalid confidence level: $ex")
  }

  val testCredentials: Option[Credentials] = Some(Credentials(testCredId, ""))

  val testAgentAuthRetrievalSuccess = new ~(new ~(new ~(Enrolments(Set(arnEnrolment)), Some(AffinityGroup.Agent)), testConfidenceLevel), testCredentials)
  val testAgentAuthRetrievalSuccessNoEnrolment = new ~(new ~(new ~(Enrolments(Set()), Some(AffinityGroup.Agent)), testConfidenceLevel), testCredentials)

  val agentAuthRetrievalSuccess = new ~(new ~(new ~(new ~(Enrolments(Set(arnEnrolment)), None),  testCredentials), Some(AffinityGroup.Agent)), testConfidenceLevel)

  val testReferrerUrl = "/test/url"

  val testNavHtml: Html = HtmlFormat.raw(
    "<html><head></head><body>  <nav id='secondary-nav' class='hmrc-account-menu'> " +
      "<ul class='hmrc-account-menu__main govuk-grid-column-full' style='padding: 0;'>" +
      "<li> <a href='http://localhost:9081/report-quarterly/income-and-expenses/view' id='nav-bar-link-testEnHome' class='hmrc-account-menu__link'> testEnHome </a> </li>" +
      "<li> <a href='testUrl' id='nav-bar-link-testEnAccount' class='hmrc-account-menu__link'> testEnAccount </a> </li>" +
      "<li> <a href='testUrl' id='nav-bar-link-testEnMessages' class='hmrc-account-menu__link'> testEnMessages </a> </li>" +
      "<li> <a href='testUrl' id='nav-bar-link-testEnForm' class='hmrc-account-menu__link'> testEnForm <span class='hmrc-notification-badge'>1</span> </a> </li>" +
      "<li> <a href='testUrl' id='nav-bar-link-testEnHelp' class='hmrc-account-menu__link'> testEnHelp </a> </li>" +
      "</ul>  </nav> </body></html>")
}
