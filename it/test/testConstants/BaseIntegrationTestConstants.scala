/*
 * Copyright 2017 HM Revenue & Customs
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

import auth.authV2.models.{AgentClientDetails, AuthUserDetails}
import controllers.agent.sessionUtils.SessionKeys
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import enums.{MTDIndividual, MTDUserRole}
import models.btaNavBar.{NavContent, NavLinks}
import models.core.IncomeSourceId.mkIncomeSourceId
import models.core.{AddressModel, IncomeSourceId}
import models.incomeSourceDetails._
import models.incomeSourceDetails.viewmodels.ManageIncomeSourceDetailsViewModel
import play.api.http.Status
import testConstants.PropertyDetailsIntegrationTestConstants.propertyTradingStartDate
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}

import java.time.LocalDate

object BaseIntegrationTestConstants {

  val testDate: LocalDate = LocalDate.of(2018, 5, 5)
  val futureDate: LocalDate = LocalDate.of(2100, 1, 1)

  val testUserTypeIndividual = Individual
  val testUserTypeAgent = Agent

  val testMtditidEnrolmentKey = "HMRC-MTD-IT"
  val testMtditidEnrolmentIdentifier = "MTDITID"
  val testMtditid = "XAIT00001234567"
  val testUserName = "Albert Einstein"
  val testClientFirstName = "Issac"
  val testClientSurname = "Newton"

  val testSaUtrEnrolmentKey = "IR-SA"
  val testSaUtrEnrolmentIdentifier = "UTR"
  val testSaUtr = "1234567890"
  val credId = "12345-credId"
  val testSessionId = "xsession-12345"
  val testArn = "XAIT0000123456"

  val testNinoEnrolmentKey = "HMRC-NI"
  val testNinoEnrolmentIdentifier = "NINO"
  val testNino = "AA123456A"
  val testCalcId = "01234567"
  val testCalcId2 = "01234568"

  val testTaxYear = 2018
  val testTaxYearTyped = TaxYear.makeTaxYearWithEndYear(testTaxYear)
  val taxYear: String = "2020-04-05"
  val testTaxYearRange = "23-24"
  val testTaxYearTo = "2021 to 2022 tax year"
  val testYear = "2018"
  val testYearPlusOne = "2019"
  val testYearInt = 2018
  val testYearPlusOneInt = 2019

  val testYear2023 = 2023
  val testCalcType = "it"

  val testYear2024 = 2024

  val testSelfEmploymentId = "ABC123456789"
  val testIncomeSource = "Fruit Ltd"
  val testSelfEmploymentIdHashed: String = mkIncomeSourceId(testSelfEmploymentId).toHash.hash
  val otherTestSelfEmploymentId = "ABC123456780"
  val testPropertyIncomeId = "1234"
  val testPropertyIncomeIdHashed: String = mkIncomeSourceId(testPropertyIncomeId).toHash.hash
  val otherTestPropertyIncomeId = "ABC123456789"
  val testEndDate2022: String = "2022-10-10"

  val testTradeName = "business"
  val testErrorStatus: Int = Status.INTERNAL_SERVER_ERROR
  val testErrorNotFoundStatus: Int = Status.NOT_FOUND
  val testErrorMessage = "Dummy Error Message"

  val testTaxCalculationId = "CALCID"
  val testTimeStampString = "2017-07-06T12:34:56.789Z"

  val stringTrue = "true"
  val testCredentials = Credentials(credId, "bar")

  val expectedAddress: Option[AddressModel] = Some(AddressModel(Some("Line 1"), Some("Line 2"), Some("Line 3"), Some("Line 4"), Some("LN12 2NL"), Some("NI")))

  val mtdEnrolment              = Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", testMtditid)), "Activated", None)
  val agentEnrolment            = Enrolment("HMRC-AS-AGENT", Seq(EnrolmentIdentifier("AgentReferenceNumber", testArn)), "Activated", None)
  val ninoEnrolment             = Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", testNino)), "Activated", None)
  val saEnrolment               = Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", testSaUtr)), "Activated", None)

  lazy val defaultEnrolments: MTDUserRole => Enrolments = mtdUserRole => {
    mtdUserRole match {
      case MTDIndividual => Enrolments(Set(mtdEnrolment, ninoEnrolment, saEnrolment))
      case _ => Enrolments(Set(agentEnrolment, ninoEnrolment, saEnrolment))
    }
  }

  lazy val defaultAuthUserDetails: MTDUserRole => AuthUserDetails = mtdUserRole => {
    AuthUserDetails(
      defaultEnrolments(mtdUserRole),
      Some(if(mtdUserRole == MTDIndividual) Individual else Agent),
      Some(testCredentials),
      None
    )
  }

  lazy val defaultClientDetails = AgentClientDetails(
    testMtditid,
    Some(testClientFirstName),
    Some(testClientSurname),
    testNino,
    testSaUtr,
    true
  )
  val testLatencyDetails3: LatencyDetails = LatencyDetails(
    latencyEndDate = LocalDate.of(testYear2023, 1, 1),
    taxYear1 = testYear2023.toString,
    latencyIndicator1 = "A",
    taxYear2 = testYear2024.toString,
    latencyIndicator2 = "Q")

  val quarterTypeElection: QuarterTypeElection = QuarterTypeElection("STANDARD", "2021")

  val testNavLinks: NavContent = NavContent(
    NavLinks("testEnHome", "testCyHome", "testUrl"),
    NavLinks("testEnAccount", "testCyAccount", "testUrl"),
    NavLinks("testEnMessages", "testCyMessages", "testUrl"),
    NavLinks("testEnHelp", "testCyHelp", "testUrl"),
    NavLinks("testEnForm", "testCyForm", "testUrl", Some(1)),
  )

  val clientDetailsWithoutConfirmation: Map[String, String] = Map(
    SessionKeys.clientFirstName -> "Test",
    SessionKeys.clientLastName -> "User",
    SessionKeys.clientUTR -> "1234567890",
    SessionKeys.clientNino -> testNino,
    SessionKeys.clientMTDID -> testMtditid
  )


  val clientDetailsWithConfirmation: Map[String, String] = Map(
    SessionKeys.clientFirstName -> "Test",
    SessionKeys.clientLastName -> "User",
    SessionKeys.clientUTR -> "1234567890",
    SessionKeys.clientNino -> testNino,
    SessionKeys.clientMTDID -> testMtditid,
    SessionKeys.confirmedClient -> "true"
  )

  val clientDetailsWithStartDate: Map[String, String] = Map(
    SessionKeys.clientFirstName -> "Test",
    SessionKeys.clientLastName -> "User",
    SessionKeys.clientUTR -> "1234567890",
    SessionKeys.clientNino -> testNino,
    SessionKeys.clientMTDID -> testMtditid,
    SessionKeys.confirmedClient -> "true"
  )

  def getAgentClientDetailsForCookie(isSupportingAgent: Boolean, requiresConfirmation: Boolean): Map[String, String] = {
    if(requiresConfirmation) {
      clientDetailsWithConfirmation ++ Map(SessionKeys.isSupportingAgent -> isSupportingAgent.toString)
    } else {
      clientDetailsWithoutConfirmation ++ Map(SessionKeys.isSupportingAgent -> isSupportingAgent.toString)
    }
  }

  val getCurrentTaxYearEnd: LocalDate = {
    val currentDate: LocalDate = LocalDate.of(2023, 4, 5)
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, 4, 6))) LocalDate.of(currentDate.getYear, 4, 5)
    else LocalDate.of(currentDate.getYear + 1, 4, 5)
  }

  val manageIncomeSourceDetailsViewModelSelfEmploymentBusiness: ManageIncomeSourceDetailsViewModel = ManageIncomeSourceDetailsViewModel(
    incomeSourceId = IncomeSourceId(testSelfEmploymentId),
    incomeSource = Some(testTradeName),
    tradingName = Some(testTradeName),
    tradingStartDate = Some(testDate),
    address = expectedAddress,
    latencyYearsQuarterly = LatencyYearsQuarterly(
      firstYear = Some(true),
      secondYear = Some(true)
    ),
    latencyYearsAnnual = LatencyYearsAnnual(
      firstYear = Some(true),
      secondYear = Some(true)
    ),
    latencyYearsCrystallised = LatencyYearsCrystallised(
      firstYear = Some(false),
      secondYear = Some(false)
    ),
    latencyDetails = Some(testLatencyDetails3),
    incomeSourceType = SelfEmployment,
    quarterReportingType = Some(QuarterTypeStandard),
    currentTaxYearEnd = getCurrentTaxYearEnd.getYear
  )

  val manageIncomeSourceDetailsViewModelUkPropertyBusiness: ManageIncomeSourceDetailsViewModel = ManageIncomeSourceDetailsViewModel(
    incomeSourceId = IncomeSourceId(testPropertyIncomeId),
    incomeSource = None,
    tradingName = None,
    tradingStartDate = propertyTradingStartDate,
    address = None,
    latencyYearsQuarterly = LatencyYearsQuarterly(
      firstYear = Some(true),
      secondYear = Some(true)
    ),
    latencyYearsAnnual = LatencyYearsAnnual(
      firstYear = Some(true),
      secondYear = Some(true)
    ),
    latencyYearsCrystallised = LatencyYearsCrystallised(
      firstYear = Some(false),
      secondYear = Some(false)
    ),
    latencyDetails = Some(testLatencyDetails3),
    incomeSourceType = UkProperty,
    quarterReportingType = Some(QuarterTypeStandard),
    currentTaxYearEnd = getCurrentTaxYearEnd.getYear
  )

  val manageIncomeSourceDetailsViewModelForeignPropertyBusiness: ManageIncomeSourceDetailsViewModel = ManageIncomeSourceDetailsViewModel(
    incomeSourceId = IncomeSourceId(testPropertyIncomeId),
    incomeSource = None,
    tradingName = None,
    tradingStartDate = propertyTradingStartDate,
    address = None,
    latencyYearsQuarterly = LatencyYearsQuarterly(
      firstYear = Some(true),
      secondYear = Some(true)
    ),
    latencyYearsAnnual = LatencyYearsAnnual(
      firstYear = Some(true),
      secondYear = Some(true)
    ),
    latencyYearsCrystallised = LatencyYearsCrystallised(
      firstYear = Some(false),
      secondYear = Some(false)
    ),
    latencyDetails = Some(testLatencyDetails3),
    incomeSourceType = ForeignProperty,
    quarterReportingType = None,
    currentTaxYearEnd = getCurrentTaxYearEnd.getYear
  )
}