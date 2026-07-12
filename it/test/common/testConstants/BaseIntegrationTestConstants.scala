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

package common.testConstants

import common.auth.{AgentClientDetails, AuthUserDetails}
import common.enums.{MTDIndividual, MTDUserRole}
import common.models.core.{AccountingPeriodModel, AddressModel}
import common.utils.sessionUtils.SessionKeys
import common.models.incomeSourceDetails.*
import play.api.http.Status
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}

import java.time.LocalDate

object BaseIntegrationTestConstants {

  val getCurrentTaxYearEnd: LocalDate = {
    val currentDate: LocalDate = LocalDate.of(2023, 4, 5)
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, 4, 6))) LocalDate.of(currentDate.getYear, 4, 5)
    else LocalDate.of(currentDate.getYear + 1, 4, 5)
  }
  val testDate: LocalDate = LocalDate.of(2018, 5, 5)
  val futureDate: LocalDate = LocalDate.of(2100, 1, 1)
  val startYear = getCurrentTaxYearEnd.getYear - 5
  val endYear = getCurrentTaxYearEnd.getYear - 4

  val propertyTradingStartDate = Some(LocalDate.parse((startYear - 1).toString + "-01-01"))


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
  val otherTestSelfEmploymentId = "ABC123456780"
  val testPropertyIncomeId = "1234"
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

  val b1AccountingStart = LocalDate.of(startYear, 1, 1)
  val b2AccountingStart = LocalDate.of(endYear, 1, 1)
  val b2AccountingEnd = LocalDate.of(endYear, 12, 31)

  val address = AddressModel(
    Some("8 Test"),
    Some("New Court"),
    Some("New Town"),
    Some("New City"),
    Some("NE12 6CI"),
    Some("GB")
  )

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

  val b1TradingName = "business"
  val b1AccountingEnd = LocalDate.of(startYear, 12, 31)
  val b1TradingStart = LocalDate.parse("2017-01-01")
  
  val business1 = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    incomeSource = Some(testIncomeSource),
    accountingPeriod = Some(AccountingPeriodModel(
      start = b1AccountingStart,
      end = b1AccountingEnd
    )),
    tradingName = Some(b1TradingName),
    firstAccountingPeriodEndDate = Some(b1AccountingEnd),
    tradingStartDate = Some(b1TradingStart),
    contextualTaxYear = None,
    cessation = None,
    address = Some(address)
  )

  val b2TradingStart = LocalDate.parse("2018-01-01")
  val b2TradingName = "secondBusiness"

  val business2 = BusinessDetailsModel(
    incomeSourceId = otherTestSelfEmploymentId,
    incomeSource = Some(testIncomeSource),
    accountingPeriod = Some(AccountingPeriodModel(
      start = b2AccountingStart,
      end = b2AccountingEnd
    )),
    tradingName = Some(b2TradingName),
    firstAccountingPeriodEndDate = Some(b2AccountingEnd),
    tradingStartDate = Some(b2TradingStart),
    contextualTaxYear = None,
    cessation = None,
    address = Some(address)
  )

  val propertyAccountingStartLocalDate = LocalDate.of(startYear, 1, 1)
  val propertyAccounringEndLocalDate = LocalDate.of(startYear, 12, 31)
  val propertyIncomeType = Some("property-unspecified")

  val property: PropertyDetailsModel = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId,
    accountingPeriod = Some(AccountingPeriodModel(
      start = propertyAccountingStartLocalDate,
      end = propertyAccounringEndLocalDate
    )),
    firstAccountingPeriodEndDate = Some(propertyAccounringEndLocalDate),
    propertyIncomeType,
    propertyTradingStartDate,
    None,
    None,
  )

  val businessAndPropertyResponse: IncomeSourceDetailsModel =
    IncomeSourceDetailsModel(
      testNino,
      testMtditid,
      businesses = List(business1),
      properties = List(property),
      yearOfMigration = Some("2018")
    )
}