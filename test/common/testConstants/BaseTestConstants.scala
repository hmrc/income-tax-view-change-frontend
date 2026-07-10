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

package common.testConstants

import common.auth.actions.AuthActionsTestData.*
import common.auth.{AuthorisedAndEnrolledRequest, MtdItUser}
import common.config.FrontendAppConfig
import common.enums.MTDIndividual
import common.models.core.{AccountingPeriodModel, AddressModel, Nino}
import common.models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel, LatencyDetails, PropertyDetailsModel, QuarterTypeElection, TaxYear, TaxYearRange}
import common.testUtils.UnitSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Name, ~}
import uk.gov.hmrc.govukfrontend.views.Aliases.{ServiceNavigationItem, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.servicenavigation.ServiceNavigation

import java.time.{LocalDate, Month}

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
  val year2017: Int = 2017
  val year2018: Int = 2018
  val year2019: Int = 2019
  val testUserName = "Albert Einstein"
  val testFirstName = "Jon"
  val testSecondName = "Jones"
  val testClientNameString = "Jon Jones"
  val testRetrievedUserName: Name = Name(Some(testUserName), None)
  val testClientName: Name = Name(Some(testFirstName), Some(testSecondName))
  val testMandationStatusOn = "on"
  val testMandationStatusOff = "off"
  val testSetUpPaymentPlanUrl = "http://localhost:9215/set-up-a-payment-plan/sa-payment-plan"
  val testIncomeType = "property-unspecified"
  val testIncomeSource = "Fruit Ltd"
  val testStartDate = LocalDate.parse("2022-01-01")
  val testStartDate2 = LocalDate.parse("2021-01-01")
  val testTradeName = "nextUpdates.business"
  val testTradeName2 = "nextUpdates.business2"

  val quarterTypeElectionStandard = QuarterTypeElection("STANDARD", "2021")
  val quarterTypeElectionCalendar = QuarterTypeElection("CALENDAR", "2021")

  val testBusinessAccountingPeriod = AccountingPeriodModel(start = LocalDate.of(year2017, Month.JUNE, 1), end = LocalDate.of(year2018, Month.MAY, 30))

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
  val testBadRequestStatus: Int = Status.BAD_REQUEST
  val testErrorMessage = "Dummy Error Message"
  implicit val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val mtdItEnrolment = Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", testMtditid)), "activated")
  val ninoEnrolment = Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", testNino)), "activated")
  val saEnrolment = Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", "1234567890")), "activated")
  val foreignIncomeType = "foreign-property"


  val testLatencyDetails = LatencyDetails(
    latencyEndDate = LocalDate.of(year2019, 1, 1),
    taxYear1 = year2018.toString,
    latencyIndicator1 = "A",
    taxYear2 = year2019.toString,
    latencyIndicator2 = "Q")

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

  val address = AddressModel(
    Some("8 Test"),
    Some("New Court"),
    Some("New Town"),
    Some("New City"),
    Some("NE12 6CI"),
    Some("GB")
  )

  val testServiceNavigation = ServiceNavigation(
      navigation = Seq(ServiceNavigationItem(
      content = Text("testHome"),
      href = "testLink"
    )), 
    navigationId = "bta-service-navigation"
  )

  val business1 = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    incomeSource = Some(testIncomeSource),
    accountingPeriod = Some(testBusinessAccountingPeriod),
    tradingName = Some(testTradeName),
    firstAccountingPeriodEndDate = Some(LocalDate.of(year2018, Month.APRIL, 5)),
    tradingStartDate = Some(testStartDate),
    contextualTaxYear = None,
    cessation = None,
    latencyDetails = Some(testLatencyDetails),
    address = Some(address),
  )

  val testPropertyAccountingPeriod = AccountingPeriodModel(LocalDate.of(2017, 4, 6), LocalDate.of(2018, 4, 5))

  val propertyDetails = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId,
    accountingPeriod = Some(testPropertyAccountingPeriod),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(testIncomeType),
    tradingStartDate = Some(testStartDate),
    contextualTaxYear = None,
    cessation = None
  )

  val business2 = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId2,
    incomeSource = Some(testIncomeSource),
    accountingPeriod = Some(testBusinessAccountingPeriod),
    tradingName = Some(testTradeName2),
    contextualTaxYear = None,
    firstAccountingPeriodEndDate = None,
    cessation = None,
    tradingStartDate = Some(testStartDate2),
    address = Some(address),
  )

  val businessesAndPropertyIncome = IncomeSourceDetailsModel(testNino, testMtditid, Some("2018"), List(business1, business2), List(propertyDetails))

  val foreignPropertyDetails = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId,
    accountingPeriod = None,
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(foreignIncomeType),
    tradingStartDate = Some(testStartDate),
    contextualTaxYear = None,
    cessation = None,
    quarterTypeElection = Some(quarterTypeElectionCalendar),
  )

  val foreignPropertyDetails2 = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId2,
    accountingPeriod = None,
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some(foreignIncomeType),
    tradingStartDate = Some(testStartDate),
    contextualTaxYear = None,
    cessation = None,
  )

  val twoActiveForeignPropertyIncomes = IncomeSourceDetailsModel(testNino, testMtditid, Some("2018"), Nil, List(foreignPropertyDetails, foreignPropertyDetails2))

}
