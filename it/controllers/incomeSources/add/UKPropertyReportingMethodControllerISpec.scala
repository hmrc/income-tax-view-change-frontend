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

package controllers.incomeSources.add

import config.featureswitch.IncomeSources
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import helpers.ComponentSpecBase
import helpers.servicemocks.ITSAStatusDetailsStub.stubGetITSAStatusDetailsError
import helpers.servicemocks.{CalculationListStub, ITSAStatusDetailsStub, IncomeTaxViewChangeStub}
import models.incomeSourceDetails.{IncomeSourceDetailsError, LatencyDetails}
import org.scalatest.Assertion
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import services.DateService
import testConstants.BaseIntegrationTestConstants._
import testConstants.CalculationListIntegrationTestConstants
import testConstants.IncomeSourceIntegrationTestConstants.{multipleBusinessesWithBothPropertiesAndCeasedBusiness, singleBusinessResponseInLatencyPeriod, singleForeignPropertyResponseInLatencyPeriod, singleUKPropertyResponseInLatencyPeriod}

import java.time.LocalDate
import java.time.Month.APRIL

sealed trait ReportingMethodScenario {
  def isLegacy: Boolean

  def isFirstTaxYearCrystallised: Boolean

  def isWithinLatencyPeriod: Boolean
}

case class LegacyScenario(isFirstTaxYearCrystallised: Boolean,
                          isWithinLatencyPeriod: Boolean) extends ReportingMethodScenario {
  override def isLegacy: Boolean = true
}

case class TaxYearSpecificScenario(isFirstTaxYearCrystallised: Boolean,
                                   isWithinLatencyPeriod: Boolean) extends ReportingMethodScenario {
  override def isLegacy: Boolean = false
}

sealed trait APIErrorScenario

case object API1171 extends APIErrorScenario

case object API1404 extends APIErrorScenario

case object API1878 extends APIErrorScenario

case object API1896 extends APIErrorScenario


class IncomeSourceReportingMethodControllerISpec extends ComponentSpecBase {
  override val dateService: DateService = app.injector.instanceOf[DateService] //overridden for TYS as implemented with 2023 elsewhere

  lazy val showUrl: (Boolean, IncomeSourceType, String) => String = (isAgent: Boolean, incomeSourceType: IncomeSourceType, id: String) =>
    routes.IncomeSourceReportingMethodController.show(isAgent, incomeSourceType, id).url
  lazy val submitUrl: (Boolean, IncomeSourceType, String) => String = (isAgent: Boolean, incomeSourceType: IncomeSourceType, id: String) =>
    routes.IncomeSourceReportingMethodController.show(isAgent, incomeSourceType, id).url
  lazy val obligationsUrl: (Boolean, IncomeSourceType, String) => String = (isAgent: Boolean, incomeSourceType: IncomeSourceType, id: String) =>
    if (isAgent) routes.IncomeSourceAddedController.showAgent(id, incomeSourceType).url else routes.IncomeSourceAddedController.show(id, incomeSourceType).url
  lazy val uri: (IncomeSourceType) => String = {
    case UkProperty => s"/income-sources/add/uk-property-reporting-method?id=$testPropertyIncomeId"
    case ForeignProperty => s"/income-sources/add/foreign-property-reporting-method?id=$testPropertyIncomeId"
    case SelfEmployment => s"/income-sources/add/business-reporting-method?id=$testSelfEmploymentId"
  }
  lazy val redirectUri: (IncomeSourceType) => String = {
    case UkProperty => routes.IncomeSourceAddedController.show(id = testPropertyIncomeId, UkProperty).url
    case ForeignProperty => routes.IncomeSourceAddedController.show(id = testPropertyIncomeId, ForeignProperty).url
    case SelfEmployment => routes.IncomeSourceAddedController.show(id = testSelfEmploymentId, SelfEmployment).url
  }


  def setupStubCalls(incomeSourceType: IncomeSourceType, scenario: ReportingMethodScenario): Unit = {
    val currentTaxYear: Int = if (scenario.isLegacy) 2023 else dateService.getCurrentTaxYearEnd()
    val lastDayOfCurrentTaxYear = LocalDate.of(currentTaxYear, APRIL, 5)

    val taxYear1: Int = currentTaxYear
    val taxYear2: Int = currentTaxYear + 1
    val taxYear1TYS: String = s"${(taxYear1 - 1).toString.takeRight(2)}-${taxYear1.toString.takeRight(2)}"

    val quarterlyIndicator: String = "Q"
    val annuallyIndicator: String = "A"
    val latencyDetails: LatencyDetails =
      LatencyDetails(
        latencyEndDate = lastDayOfCurrentTaxYear,
        taxYear1 = taxYear1.toString,
        latencyIndicator1 = quarterlyIndicator,
        taxYear2 = taxYear2.toString,
        latencyIndicator2 = annuallyIndicator
      )

    Given("Income Sources FS is enabled")
    enable(IncomeSources)

    And("API 1171 getIncomeSourceDetails returns a success response")
    (incomeSourceType, scenario.isWithinLatencyPeriod) match {
      case (UkProperty, true) =>
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleUKPropertyResponseInLatencyPeriod(latencyDetails))
      case (ForeignProperty, true) =>
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleForeignPropertyResponseInLatencyPeriod(latencyDetails))
      case (SelfEmployment, true) =>
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseInLatencyPeriod(latencyDetails))
      case (_, false) =>
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesWithBothPropertiesAndCeasedBusiness)
    }

    And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
    ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", "2023-24")

    scenario match {
      case LegacyScenario(true, _) =>
        And("API 1404 getListOfCalculationResults returns a success response")
        CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear1.toString)(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())
      case LegacyScenario(false, _) =>
        And("API 1404 getListOfCalculationResults returns a success response")
        CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear1.toString)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())
      case TaxYearSpecificScenario(true, _) =>
        And("API 1896 getCalculationList returns a success response")
        CalculationListStub.stubGetCalculationList(testNino, taxYear1TYS)(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())
      case TaxYearSpecificScenario(false, _) =>
        And("API 1896 getCalculationList returns a success response")
        CalculationListStub.stubGetCalculationList(testNino, taxYear1TYS)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())
    }
  }

  def setupStubErrorCall(incomeSourceType: IncomeSourceType, scenario: APIErrorScenario): Unit = {
    val currentTaxYear: Int = 2023
    val taxYear1: Int = currentTaxYear
//    val taxYear1TYS: String = s"${(taxYear1 - 1).toString.takeRight(2)}-${taxYear1.toString.takeRight(2)}"
    Given("Income Sources FS is enabled")
    enable(IncomeSources)

    if (scenario.equals(API1171)) {
      And("API 1171 getIncomeSourceDetails returns an error response")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(INTERNAL_SERVER_ERROR, IncomeSourceDetailsError(INTERNAL_SERVER_ERROR, "ISE"))
    } else {
      And("API 1171 getIncomeSourceDetails returns a success response")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesWithBothPropertiesAndCeasedBusiness)
    }

    if (scenario.equals(API1878)) {
      And("API 1878 getITSAStatus returns an error response")
      stubGetITSAStatusDetailsError
    } else {
      And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
      ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", "2023-24")
    }

    if (scenario.equals(API1404)) {
      And("API 1404 getListOfCalculationResults returns an error response")
      CalculationListStub.stubGetLegacyCalculationListError(testNino, taxYear1.toString)
    }

    if (scenario.equals(API1896)) {
      And("API 1896 getCalculationList returns an error response")
      CalculationListStub.stubGetCalculationListError(testNino, taxYear1.toString)
    }
  }


  def checkOneTaxYear(incomeSourceType: IncomeSourceType, scenario: ReportingMethodScenario): Assertion = {
    Then("User is asked to select reporting method for Tax Year 2")
    val result = IncomeTaxViewChangeFrontend.get(uri(incomeSourceType))

    result should have(
      httpStatus(OK),
      pageTitleIndividual("incomeSources.add.incomeSourceReportingMethod.heading"),
      elementCountBySelector("#add-uk-property-reporting-method-form > legend:nth-of-type(2)")(0))

    if (scenario.isLegacy) {
      result should have(
        elementTextBySelectorList("#add-uk-property-reporting-method-form", "legend:nth-of-type(1)")(s"Tax year 2023-2024")
      )
    } else {
      val currentTaxYear = dateService.getCurrentTaxYearEnd()
      val taxYear1: Int = currentTaxYear
      val taxYear1TYS: String = s"${taxYear1 - 1}-$taxYear1"
      result should have(
        elementTextBySelectorList("#add-uk-property-reporting-method-form", "legend:nth-of-type(1)")(s"Tax year $taxYear1TYS")
      )
    }
  }

  def checkBothTaxYears(incomeSourceType: IncomeSourceType, scenario: ReportingMethodScenario): Assertion = {
    Then("user is asked to select reporting method for Tax Year 1 and Tax Year 2")
    val result = IncomeTaxViewChangeFrontend.get(uri(incomeSourceType))

    result should have(
      httpStatus(OK),
      pageTitleIndividual("incomeSources.add.incomeSourceReportingMethod.heading"))

    if (scenario.isLegacy) {
      result should have(
        elementTextBySelectorList("#add-uk-property-reporting-method-form", "div:nth-of-type(3)", "legend")(s"Tax year 2022-2023"),
        elementTextBySelectorList("#add-uk-property-reporting-method-form", "div:nth-of-type(7)", "legend")(s"Tax year 2023-2024")
      )
    } else {
      val currentTaxYear = dateService.getCurrentTaxYearEnd()
      val taxYear1: Int = currentTaxYear
      val taxYear2: Int = currentTaxYear + 1
      val taxYear1TYS: String = s"${taxYear1 - 1}-$taxYear1"
      val taxYear2TYS: String = s"${taxYear2 - 1}-$taxYear2"
      result should have(
        elementTextBySelectorList("#add-uk-property-reporting-method-form", "div:nth-of-type(3)", "legend")(s"Tax year $taxYear1TYS"),
        elementTextBySelectorList("#add-uk-property-reporting-method-form", "div:nth-of-type(7)", "legend")(s"Tax year $taxYear2TYS")
      )
    }
  }

  def checkRedirect(incomeSourceType: IncomeSourceType, expectedRedirectUri: String): Assertion = {
    val result = IncomeTaxViewChangeFrontend.get(uri(incomeSourceType))

    result should have(
      httpStatus(SEE_OTHER),
      redirectURI(expectedRedirectUri))
  }

  def checkError(incomeSourceType: IncomeSourceType): Assertion = {
    val result = IncomeTaxViewChangeFrontend.get(uri(incomeSourceType))

    result should have(httpStatus(INTERNAL_SERVER_ERROR))
  }

  s"calling GET ${showUrl(false, UkProperty, testPropertyIncomeId)}" should {
    "200 OK - render the UK Property Reporting Method page" when {
      "user is within latency period (before 23/24) - tax year 1 not crystallised - UK Property" in {
        setupStubCalls(UkProperty, LegacyScenario(isFirstTaxYearCrystallised = false, isWithinLatencyPeriod = true))
        checkBothTaxYears(UkProperty, LegacyScenario(isFirstTaxYearCrystallised = false, isWithinLatencyPeriod = true))
      }
      "user is within latency period (before 23/24) - tax year 1 crystallised - UK Property" in {
        setupStubCalls(UkProperty, LegacyScenario(isFirstTaxYearCrystallised = true, isWithinLatencyPeriod = true))
        checkOneTaxYear(UkProperty, LegacyScenario(isFirstTaxYearCrystallised = true, isWithinLatencyPeriod = true))
      }
      "user is within latency period (after 23/24) - tax year 1 not crystallised - UK Property" in {
        setupStubCalls(UkProperty, TaxYearSpecificScenario(isFirstTaxYearCrystallised = false, isWithinLatencyPeriod = true))
        checkBothTaxYears(UkProperty, TaxYearSpecificScenario(isFirstTaxYearCrystallised = false, isWithinLatencyPeriod = true))
      }
      "user is within latency period (after 23/24) - tax year 1 crystallised - UK Property" in {
        setupStubCalls(UkProperty, TaxYearSpecificScenario(isFirstTaxYearCrystallised = true, isWithinLatencyPeriod = true))
        checkOneTaxYear(UkProperty, TaxYearSpecificScenario(isFirstTaxYearCrystallised = true, isWithinLatencyPeriod = true))
      }
    }
    "303 SEE_OTHER - redirect to the UK Property Obligations page" when {
      "user is out of latency period (before 23/24) - UK Property" in {
        setupStubCalls(UkProperty, LegacyScenario(isFirstTaxYearCrystallised = false, isWithinLatencyPeriod = false))
        checkRedirect(UkProperty, redirectUri(UkProperty))
      }
      "user is out of latency period (after 23/24) - UK Property" in {
        setupStubCalls(UkProperty, TaxYearSpecificScenario(isFirstTaxYearCrystallised = false, isWithinLatencyPeriod = false))
        checkRedirect(UkProperty, redirectUri(UkProperty))
      }
    }
    "500 INTERNAL_SERVER_ERROR" when {
      "API 1171 returns an error" in {
        setupStubErrorCall(UkProperty, API1171)
        checkError(UkProperty)
      }
      "API 1404 returns an error" in {
        setupStubErrorCall(UkProperty, API1404)
        checkError(UkProperty)
      }
      "API 1878 returns an error" in {
        setupStubErrorCall(UkProperty, API1878)
        checkError(UkProperty)
      }
      "API 1896 returns an error" in {
        setupStubErrorCall(UkProperty, API1896)
        checkError(UkProperty)
      }
    }
  }

  s"calling GET ${showUrl(false, ForeignProperty, testPropertyIncomeId)}" should {
    "200 OK - render the Foreign Property Reporting Method page" when {
      "user is within latency period (before 23/24) - tax year 1 not crystallised - Foreign Property" in {
        setupStubCalls(ForeignProperty, LegacyScenario(isFirstTaxYearCrystallised = false, isWithinLatencyPeriod = true))
        checkBothTaxYears(ForeignProperty, LegacyScenario(isFirstTaxYearCrystallised = false, isWithinLatencyPeriod = true))
      }
      "user is within latency period (before 23/24) - tax year 1 crystallised - Foreign Property" in {
        setupStubCalls(ForeignProperty, LegacyScenario(isFirstTaxYearCrystallised = true, isWithinLatencyPeriod = true))
        checkOneTaxYear(ForeignProperty, LegacyScenario(isFirstTaxYearCrystallised = true, isWithinLatencyPeriod = true))
      }
      "user is within latency period (after 23/24) - tax year 1 not crystallised - Foreign Property" in {
        setupStubCalls(ForeignProperty, TaxYearSpecificScenario(isFirstTaxYearCrystallised = false, isWithinLatencyPeriod = true))
        checkBothTaxYears(ForeignProperty, TaxYearSpecificScenario(isFirstTaxYearCrystallised = false, isWithinLatencyPeriod = true))
      }
      "user is within latency period (after 23/24) - tax year 1 crystallised - Foreign Property" in {
        setupStubCalls(ForeignProperty, TaxYearSpecificScenario(isFirstTaxYearCrystallised = true, isWithinLatencyPeriod = true))
        checkOneTaxYear(ForeignProperty, TaxYearSpecificScenario(isFirstTaxYearCrystallised = true, isWithinLatencyPeriod = true))
      }
    }
    "303 SEE_OTHER - redirect to the Foreign Property Obligations page" when {
      "user is out of latency period (before 23/24) - Foreign Property" in {
        setupStubCalls(ForeignProperty, LegacyScenario(isFirstTaxYearCrystallised = false, isWithinLatencyPeriod = false))
        checkRedirect(ForeignProperty, redirectUri(ForeignProperty))
      }
      "user is out of latency period (after 23/24) - Foreign Property" in {
        setupStubCalls(ForeignProperty, TaxYearSpecificScenario(isFirstTaxYearCrystallised = false, isWithinLatencyPeriod = false))
        checkRedirect(ForeignProperty, redirectUri(ForeignProperty))
      }
    }
    "500 INTERNAL_SERVER_ERROR" when {
      "API 1171 returns an error" in {
        setupStubErrorCall(ForeignProperty, API1171)
        checkError(ForeignProperty)
      }
      "API 1404 returns an error" in {
        setupStubErrorCall(ForeignProperty, API1404)
        checkError(ForeignProperty)
      }
      "API 1878 returns an error" in {
        setupStubErrorCall(ForeignProperty, API1878)
        checkError(ForeignProperty)
      }
      "API 1896 returns an error" in {
        setupStubErrorCall(ForeignProperty, API1896)
        checkError(ForeignProperty)
      }
    }
  }

  s"calling GET ${showUrl(false, SelfEmployment, testSelfEmploymentId)}" should {
    "200 OK - render the Self Employment Reporting Method page" when {
      "user is within latency period (before 23/24) - tax year 1 not crystallised - Self Employment" in {
        setupStubCalls(SelfEmployment, LegacyScenario(isFirstTaxYearCrystallised = false, isWithinLatencyPeriod = true))
        checkBothTaxYears(SelfEmployment, LegacyScenario(isFirstTaxYearCrystallised = false, isWithinLatencyPeriod = true))
      }
      "user is within latency period (before 23/24) - tax year 1 crystallised - Self Employment" in {
        setupStubCalls(SelfEmployment, LegacyScenario(isFirstTaxYearCrystallised = true, isWithinLatencyPeriod = true))
        checkOneTaxYear(SelfEmployment, LegacyScenario(isFirstTaxYearCrystallised = true, isWithinLatencyPeriod = true))
      }
      "user is within latency period (after 23/24) - tax year 1 not crystallised - Self Employment" in {
        setupStubCalls(SelfEmployment, TaxYearSpecificScenario(isFirstTaxYearCrystallised = false, isWithinLatencyPeriod = true))
        checkBothTaxYears(SelfEmployment, TaxYearSpecificScenario(isFirstTaxYearCrystallised = false, isWithinLatencyPeriod = true))
      }
      "user is within latency period (after 23/24) - tax year 1 crystallised - Self Employment" in {
        setupStubCalls(SelfEmployment, TaxYearSpecificScenario(isFirstTaxYearCrystallised = true, isWithinLatencyPeriod = true))
        checkOneTaxYear(SelfEmployment, TaxYearSpecificScenario(isFirstTaxYearCrystallised = true, isWithinLatencyPeriod = true))
      }
    }
    "303 SEE_OTHER - redirect to the Self Employment Obligations page" when {
      "user is out of latency period (before 23/24) - Self Employment" in {
        setupStubCalls(SelfEmployment, LegacyScenario(isFirstTaxYearCrystallised = false, isWithinLatencyPeriod = false))
        checkRedirect(SelfEmployment, redirectUri(SelfEmployment))
      }
      "user is out of latency period (after 23/24) - Self Employment" in {
        setupStubCalls(SelfEmployment, TaxYearSpecificScenario(isFirstTaxYearCrystallised = false, isWithinLatencyPeriod = false))
        checkRedirect(SelfEmployment, redirectUri(SelfEmployment))
      }
    }
    "500 INTERNAL_SERVER_ERROR" when {
      "API 1171 returns an error" in {
        setupStubErrorCall(SelfEmployment, API1171)
        checkError(SelfEmployment)
      }
      "API 1404 returns an error" in {
        setupStubErrorCall(SelfEmployment, API1404)
        checkError(SelfEmployment)
      }
      "API 1878 returns an error" in {
        setupStubErrorCall(SelfEmployment, API1878)
        checkError(SelfEmployment)
      }
      "API 1896 returns an error" in {
        setupStubErrorCall(SelfEmployment, API1896)
        checkError(SelfEmployment)
      }
    }
  }

  //  "return 500 INTERNAL SERVER ERROR" when {
  //    "API 1896 getCalculationList returns an error" in {
  //      Given("Income Sources FS is enabled")
  //      enable(IncomeSources)
  //      enable(TimeMachineAddYear)
  //
  //      And("API 1171 getIncomeSourceDetails returns a success response")
  //      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleSelfEmploymentResponseInLatencyPeriod(latencyDetails))
  //
  //      And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
  //      ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated")
  //
  //      And("API 1896 getCalculationList returns an error")
  //      CalculationListStub.stubGetCalculationListError(testNino, testTaxYearRange)
  //
  //      val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/add/uk-property-reporting-method?id=$testPropertyIncomeId")
  //
  //      result should have(
  //        httpStatus(INTERNAL_SERVER_ERROR)
  //      )
  //    }
  //    "API 1404 getListOfCalculationResults returns an error" in {
  //      val taxYear2023: Int = 2023
  //      val taxYear2024: Int = 2024
  //      val latencyPeriodEndDate: LocalDate = LocalDate.of(2025, APRIL, 5)
  //
  //      val latencyDetailsPreviousTaxYear: LatencyDetails = LatencyDetails(
  //        latencyEndDate = latencyPeriodEndDate,
  //        taxYear1 = taxYear2023.toString,
  //        latencyIndicator1 = quarterlyIndicator,
  //        taxYear2 = taxYear2024.toString,
  //        latencyIndicator2 = quarterlyIndicator
  //      )
  //
  //      Given("Income Sources FS is enabled")
  //      enable(IncomeSources)
  //
  //      And("API 1171 getIncomeSourceDetails returns a success response")
  //      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleUKPropertyResponseInLatencyPeriod(latencyDetailsPreviousTaxYear))
  //
  //      And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
  //      ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated")
  //
  //      And("API 1404 getListOfCalculationResults returns an error response")
  //      CalculationListStub.stubGetLegacyCalculationListError(testNino, taxYear2023.toString)
  //
  //      val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/add/uk-property-reporting-method?id=$testPropertyIncomeId")
  //
  //      result should have(
  //        httpStatus(INTERNAL_SERVER_ERROR)
  //      )
  //    }
  //    "API 1878 getITSAStatusDetails returns an error" in {
  //      Given("Income Sources FS is enabled")
  //      enable(IncomeSources)
  //
  //      And("API 1171 getIncomeSourceDetails returns a success response")
  //      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleUKPropertyResponseInLatencyPeriod(latencyDetails))
  //
  //      And("API 1878 getITSAStatus returns a success response with one of these statuses: Annual, No Status, Non Digital, Dormant, MTD Exempt")
  //      ITSAStatusDetailsStub.stubGetITSAStatusDetailsError
  //
  //      And("API 1896 getCalculationList returns a success response")
  //      CalculationListStub.stubGetCalculationList(testNino, testTaxYearRange)(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())
  //
  //      val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/add/uk-property-reporting-method?id=$testPropertyIncomeId")
  //
  //      result should have(
  //        httpStatus(INTERNAL_SERVER_ERROR)
  //      )
  //    }
  //    "API 1171 getIncomeSourceDetails returns an error" in {
  //      Given("Income Sources FS is enabled")
  //      enable(IncomeSources)
  //
  //      And("API 1171 getIncomeSourceDetails returns an error response")
  //      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(INTERNAL_SERVER_ERROR,
  //        IncomeSourceDetailsError(INTERNAL_SERVER_ERROR, "IF is currently experiencing problems that require live service intervention."))
  //
  //      And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
  //      ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated")
  //
  //      And("API 1896 getCalculationList returns a success response")
  //      CalculationListStub.stubGetCalculationList(testNino, testTaxYearRange)(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())
  //
  //      val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/add/uk-property-reporting-method?id=$testPropertyIncomeId")
  //
  //      result should have(
  //        httpStatus(INTERNAL_SERVER_ERROR)
  //      )
  //    }
  //  }
  //}
  //
  //s"calling POST $ukPropertyReportingMethodSubmitUrl" should {
  //  s"redirect to $ukPropertyAddedShowUrl" when {
  //    "user completes the form and API 1776 updateIncomeSource returns a success response" in {
  //      val formData: Map[String, Seq[String]] = Map(
  //        "new_tax_year_1_reporting_method" -> Seq("A"),
  //        "new_tax_year_2_reporting_method" -> Seq("A"),
  //        "new_tax_year_1_reporting_method_tax_year" -> Seq(taxYear1.toString),
  //        "tax_year_1_reporting_method" -> Seq("Q"),
  //        "new_tax_year_2_reporting_method_tax_year" -> Seq(taxYear2.toString),
  //        "tax_year_2_reporting_method" -> Seq("Q")
  //      )
  //
  //      Given("Income Sources FS is enabled")
  //      enable(IncomeSources)
  //
  //      And("API 1171 getIncomeSourceDetails returns a success response")
  //      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleUKPropertyResponseInLatencyPeriod(latencyDetails))
  //
  //      And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
  //      ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated")
  //
  //      And("API 1896 getCalculationList returns a success response")
  //      CalculationListStub.stubGetCalculationList(testNino, testTaxYearRange)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())
  //
  //      And("API 1776 updateTaxYearSpecific returns a success response")
  //      IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel("")))
  //
  //      val result = IncomeTaxViewChangeFrontend.post(s"/income-sources/add/uk-property-reporting-method?id=$testPropertyIncomeId")(formData)
  //      result should have(
  //        httpStatus(SEE_OTHER),
  //        redirectURI(s"/report-quarterly/income-and-expenses/view/income-sources/add/uk-property-added?id=$testPropertyIncomeId")
  //      )
  //    }
  //  }
  //  "return 400 BAD_REQUEST" when {
  //    "user submits an invalid form entry" in {
  //      val formData = IncomeSourceReportingMethodForm(
  //        newTaxYear1ReportingMethod = None,
  //        newTaxYear2ReportingMethod = None,
  //        taxYear1 = Some(taxYear1.toString),
  //        taxYear1ReportingMethod = None,
  //        taxYear2 = Some(taxYear2.toString),
  //        taxYear2ReportingMethod = None
  //      )
  //      val formWithErrors = IncomeSourceReportingMethodForm.form
  //        .fillAndValidate(formData)
  //
  //      Given("Income Sources FS is enabled")
  //      enable(IncomeSources)
  //
  //      And("API 1171 getIncomeSourceDetails returns a success response")
  //      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleUKPropertyResponseInLatencyPeriod(latencyDetails))
  //
  //      And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
  //      ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated")
  //
  //      And("API 1896 getCalculationList returns a success response")
  //      CalculationListStub.stubGetCalculationList(testNino, testTaxYearRange)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())
  //
  //      val result = IncomeTaxViewChangeFrontend.postAddUKPropertyReportingMethod(formWithErrors.value.get)()
  //
  //      result should have(
  //        httpStatus(BAD_REQUEST)
  //      )
  //    }
  //  }
}
