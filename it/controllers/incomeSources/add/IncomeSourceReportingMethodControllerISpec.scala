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

import audit.models.IncomeSourceReportingMethodAuditModel
import auth.MtdItUser
import config.featureswitch.{IncomeSources, TimeMachineAddYear}
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.Add
import helpers.ComponentSpecBase
import helpers.servicemocks.ITSAStatusDetailsStub.stubGetITSAStatusDetailsError
import helpers.servicemocks.{AuditStub, CalculationListStub, ITSAStatusDetailsStub, IncomeTaxViewChangeStub}
import models.incomeSourceDetails.{AddIncomeSourceData, IncomeSourceDetailsError, LatencyDetails, UIJourneySessionData}
import models.updateIncomeSource.UpdateIncomeSourceResponseModel
import org.scalatest.Assertion
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.UIJourneySessionDataRepository
import services.{DateService, SessionService}
import testConstants.BaseIntegrationTestConstants._
import testConstants.BusinessDetailsIntegrationTestConstants.b1TradingName
import testConstants.CalculationListIntegrationTestConstants
import testConstants.IncomeSourceIntegrationTestConstants._
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

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

case object API1776 extends APIErrorScenario

case object API1878 extends APIErrorScenario

case object API1896 extends APIErrorScenario


class IncomeSourceReportingMethodControllerISpec extends ComponentSpecBase {
  override val dateService: DateService = app.injector.instanceOf[DateService] //overridden for TYS as implemented with 2023 elsewhere

  lazy val showUrl: (Boolean, IncomeSourceType) => String = (isAgent: Boolean, incomeSourceType: IncomeSourceType) =>
    routes.IncomeSourceReportingMethodController.show(isAgent, incomeSourceType).url
  lazy val submitUrl: (Boolean, IncomeSourceType) => String = (isAgent: Boolean, incomeSourceType: IncomeSourceType) =>
    routes.IncomeSourceReportingMethodController.show(isAgent, incomeSourceType).url
  lazy val obligationsUrl: (Boolean, IncomeSourceType) => String = (isAgent: Boolean, incomeSourceType: IncomeSourceType) =>
    if (isAgent) routes.IncomeSourceAddedController.showAgent(incomeSourceType).url else routes.IncomeSourceAddedController.show(incomeSourceType).url
  lazy val redirectUrl: IncomeSourceType => String = {
    case UkProperty => routes.IncomeSourceAddedController.show(UkProperty).url
    case ForeignProperty => routes.IncomeSourceAddedController.show(ForeignProperty).url
    case SelfEmployment => routes.IncomeSourceAddedController.show(SelfEmployment).url
  }
  lazy val errorRedirectUrl: IncomeSourceType => String = {
    case UkProperty => routes.IncomeSourceReportingMethodNotSavedController.show(UkProperty).url
    case ForeignProperty => routes.IncomeSourceReportingMethodNotSavedController.show(ForeignProperty).url
    case SelfEmployment => routes.IncomeSourceReportingMethodNotSavedController.show(SelfEmployment).url
  }
  lazy val uri: IncomeSourceType => String = {
    case UkProperty => s"/income-sources/add/uk-property-reporting-method"
    case ForeignProperty => s"/income-sources/add/foreign-property-reporting-method"
    case SelfEmployment => s"/income-sources/add/business-reporting-method"
  }
  val quarterlyIndicator: String = "Q"
  val annuallyIndicator: String = "A"
  val currentTaxYear: Int = dateService.getCurrentTaxYearEnd()
  val taxYear1: Int = currentTaxYear
  val taxYear2: Int = currentTaxYear + 1
  val taxYear1YYtoYY: String = s"${(taxYear1 - 1).toString.takeRight(2)}-${taxYear1.toString.takeRight(2)}"
  val taxYear1YYYYtoYY: String = "20" + taxYear1YYtoYY
  val taxYearYYYYtoYYYY = s"${taxYear1 - 1}-$taxYear1"
  val lastDayOfCurrentTaxYear: LocalDate = LocalDate.of(currentTaxYear, APRIL, 5)
  val latencyDetails: LatencyDetails =
    LatencyDetails(
      latencyEndDate = lastDayOfCurrentTaxYear.plusYears(2),
      taxYear1 = taxYear1.toString,
      latencyIndicator1 = quarterlyIndicator,
      taxYear2 = taxYear2.toString,
      latencyIndicator2 = annuallyIndicator
    )
  val businessName: IncomeSourceType => String = {
    case UkProperty => "UK property"
    case ForeignProperty => "Foreign property"
    case SelfEmployment => b1TradingName
  }
  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, None, multipleBusinessesAndPropertyResponse,
    None, Some("1234567890"), Some("12345-credId"), Some(Individual), None
  )(FakeRequest())

  val sessionService: SessionService = app.injector.instanceOf[SessionService]
  val repository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Add))
  }

  def setupStubCalls(incomeSourceType: IncomeSourceType, scenario: ReportingMethodScenario): Unit = {
    Given("Income Sources FS is enabled")
    disable(TimeMachineAddYear)
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

    incomeSourceType match {
      case SelfEmployment => await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-SE",
        addIncomeSourceData = Some(AddIncomeSourceData(createdIncomeSourceId = Some(testSelfEmploymentId))))))
      case UkProperty => await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-UK",
        addIncomeSourceData = Some(AddIncomeSourceData(createdIncomeSourceId = Some(testSelfEmploymentId))))))
      case ForeignProperty => await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-FP",
        addIncomeSourceData = Some(AddIncomeSourceData(createdIncomeSourceId = Some(testSelfEmploymentId))))))
    }

    And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
    ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", taxYear1YYYYtoYY)

    scenario match {
      case LegacyScenario(true, _) =>
        And("API 1404 getListOfCalculationResults returns a success response")
        CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear1.toString)(CalculationListIntegrationTestConstants
          .successResponseCrystallised.toString())
      case LegacyScenario(false, _) =>
        And("API 1404 getListOfCalculationResults returns a success response")
        CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear1.toString)(CalculationListIntegrationTestConstants
          .successResponseNonCrystallised.toString())
      case TaxYearSpecificScenario(true, _) =>
        And("API 1896 getCalculationList returns a success response")
        CalculationListStub.stubGetCalculationList(testNino, taxYear1YYtoYY)(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())
      case TaxYearSpecificScenario(false, _) =>
        And("API 1896 getCalculationList returns a success response")
        CalculationListStub.stubGetCalculationList(testNino, taxYear1YYtoYY)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())
    }
  }

  def setupStubErrorCall(scenario: APIErrorScenario): Unit = {
    Given("Income Sources FS is enabled")
    enable(IncomeSources)
    enable(TimeMachineAddYear)

    if (scenario.equals(API1171)) {
      And("API 1171 getIncomeSourceDetails returns an error response")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(INTERNAL_SERVER_ERROR, IncomeSourceDetailsError(INTERNAL_SERVER_ERROR, "ISE"))
    } else {
      And("API 1171 getIncomeSourceDetails returns a success response")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, allBusinessesAndPropertiesInLatencyPeriod(latencyDetails))
    }

    if (scenario.equals(API1878)) {
      And("API 1878 getITSAStatus returns an error response")
      stubGetITSAStatusDetailsError
    } else {
      And("API 1878 getITSAStatus returns a success response with a valid status (MTD Mandated or MTD Voluntary)")
      ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", taxYear1YYYYtoYY)
    }

    if (scenario.equals(API1404)) {
      And("API 1404 getListOfCalculationResults returns an error response")
      CalculationListStub.stubGetLegacyCalculationListError(testNino, taxYear1.toString)
    }

    if (scenario.equals(API1896)) {
      And("API 1896 getCalculationList returns an error response")
      CalculationListStub.stubGetCalculationListError(testNino, taxYear1YYtoYY)
    }

    if (scenario.equals(API1776)) {
      And("API 1776 updateIncomeSource returns an error response")
      IncomeTaxViewChangeStub.stubUpdateIncomeSourceError()
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

  def checkBothTaxYears(incomeSourceType: IncomeSourceType): Assertion = {
    Then("User is asked to select reporting method for Tax Year 1 and Tax Year 2")
    val result = IncomeTaxViewChangeFrontend.get(uri(incomeSourceType))

    result should have(
      httpStatus(OK),
      pageTitleIndividual("incomeSources.add.incomeSourceReportingMethod.heading"))

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

  s"calling GET ${showUrl(false, UkProperty)}" should {
    "200 OK - render the UK Property Reporting Method page" when {
      "user is within latency period (before 23/24) - tax year 1 not crystallised - UK Property" in {
        setupStubCalls(UkProperty, LegacyScenario(isFirstTaxYearCrystallised = false, isWithinLatencyPeriod = true))
        checkBothTaxYears(UkProperty)
      }
      "user is within latency period (before 23/24) - tax year 1 crystallised - UK Property" in {
        setupStubCalls(UkProperty, LegacyScenario(isFirstTaxYearCrystallised = true, isWithinLatencyPeriod = true))
        checkOneTaxYear(UkProperty, LegacyScenario(isFirstTaxYearCrystallised = true, isWithinLatencyPeriod = true))
      }
      "user is within latency period (after 23/24) - tax year 1 not crystallised - UK Property" in {
        setupStubCalls(UkProperty, TaxYearSpecificScenario(isFirstTaxYearCrystallised = false, isWithinLatencyPeriod = true))
        checkBothTaxYears(UkProperty)
      }
      "user is within latency period (after 23/24) - tax year 1 crystallised - UK Property" in {
        setupStubCalls(UkProperty, TaxYearSpecificScenario(isFirstTaxYearCrystallised = true, isWithinLatencyPeriod = true))
        checkOneTaxYear(UkProperty, TaxYearSpecificScenario(isFirstTaxYearCrystallised = true, isWithinLatencyPeriod = true))
      }
    }
    "303 SEE_OTHER - redirect to the UK Property Obligations page" when {
      "user is out of latency period (before 23/24) - UK Property" in {
        setupStubCalls(UkProperty, LegacyScenario(isFirstTaxYearCrystallised = false, isWithinLatencyPeriod = false))
        checkRedirect(UkProperty, redirectUrl(UkProperty))
      }
      "user is out of latency period (after 23/24) - UK Property" in {
        setupStubCalls(UkProperty, TaxYearSpecificScenario(isFirstTaxYearCrystallised = false, isWithinLatencyPeriod = false))
        checkRedirect(UkProperty, redirectUrl(UkProperty))
      }
    }
    "500 INTERNAL_SERVER_ERROR" when {
      "API 1171 returns an error" in {
        setupStubErrorCall(API1171)
        checkError(UkProperty)
      }
      "API 1404 returns an error" in {
        setupStubErrorCall(API1404)
        checkError(UkProperty)
      }
      "API 1878 returns an error" in {
        setupStubErrorCall(API1878)
        checkError(UkProperty)
      }
      "API 1896 returns an error" in {
        setupStubErrorCall(API1896)
        checkError(UkProperty)
      }
    }
  }

  s"calling GET ${showUrl(false, ForeignProperty)}" should {
    "200 OK - render the Foreign Property Reporting Method page" when {
      "user is within latency period (before 23/24) - tax year 1 not crystallised - Foreign Property" in {
        setupStubCalls(ForeignProperty, LegacyScenario(isFirstTaxYearCrystallised = false, isWithinLatencyPeriod = true))
        checkBothTaxYears(ForeignProperty)
      }
      "user is within latency period (before 23/24) - tax year 1 crystallised - Foreign Property" in {
        setupStubCalls(ForeignProperty, LegacyScenario(isFirstTaxYearCrystallised = true, isWithinLatencyPeriod = true))
        checkOneTaxYear(ForeignProperty, LegacyScenario(isFirstTaxYearCrystallised = true, isWithinLatencyPeriod = true))
      }
      "user is within latency period (after 23/24) - tax year 1 not crystallised - Foreign Property" in {
        setupStubCalls(ForeignProperty, TaxYearSpecificScenario(isFirstTaxYearCrystallised = false, isWithinLatencyPeriod = true))
        checkBothTaxYears(ForeignProperty)
      }
      "user is within latency period (after 23/24) - tax year 1 crystallised - Foreign Property" in {
        setupStubCalls(ForeignProperty, TaxYearSpecificScenario(isFirstTaxYearCrystallised = true, isWithinLatencyPeriod = true))
        checkOneTaxYear(ForeignProperty, TaxYearSpecificScenario(isFirstTaxYearCrystallised = true, isWithinLatencyPeriod = true))
      }
    }
    "303 SEE_OTHER - redirect to the Foreign Property Obligations page" when {
      "user is out of latency period (before 23/24) - Foreign Property" in {
        setupStubCalls(ForeignProperty, LegacyScenario(isFirstTaxYearCrystallised = false, isWithinLatencyPeriod = false))
        checkRedirect(ForeignProperty, redirectUrl(ForeignProperty))
      }
      "user is out of latency period (after 23/24) - Foreign Property" in {
        setupStubCalls(ForeignProperty, TaxYearSpecificScenario(isFirstTaxYearCrystallised = false, isWithinLatencyPeriod = false))
        checkRedirect(ForeignProperty, redirectUrl(ForeignProperty))
      }
    }
    "500 INTERNAL_SERVER_ERROR" when {
      "API 1171 returns an error" in {
        setupStubErrorCall(API1171)
        checkError(ForeignProperty)
      }
      "API 1404 returns an error" in {
        setupStubErrorCall(API1404)
        checkError(ForeignProperty)
      }
      "API 1878 returns an error" in {
        setupStubErrorCall(API1878)
        checkError(ForeignProperty)
      }
      "API 1896 returns an error" in {
        setupStubErrorCall(API1896)
        checkError(ForeignProperty)
      }
    }
  }

  s"calling GET ${showUrl(false, SelfEmployment)}" should {
    "200 OK - render the Self Employment Reporting Method page" when {
      "user is within latency period (before 23/24) - tax year 1 not crystallised - Self Employment" in {
        setupStubCalls(SelfEmployment, LegacyScenario(isFirstTaxYearCrystallised = false, isWithinLatencyPeriod = true))
        checkBothTaxYears(SelfEmployment)
      }
      "user is within latency period (before 23/24) - tax year 1 crystallised - Self Employment" in {
        setupStubCalls(SelfEmployment, LegacyScenario(isFirstTaxYearCrystallised = true, isWithinLatencyPeriod = true))
        checkOneTaxYear(SelfEmployment, LegacyScenario(isFirstTaxYearCrystallised = true, isWithinLatencyPeriod = true))
      }
      "user is within latency period (after 23/24) - tax year 1 not crystallised - Self Employment" in {
        setupStubCalls(SelfEmployment, TaxYearSpecificScenario(isFirstTaxYearCrystallised = false, isWithinLatencyPeriod = true))
        checkBothTaxYears(SelfEmployment)
      }
      "user is within latency period (after 23/24) - tax year 1 crystallised - Self Employment" in {
        setupStubCalls(SelfEmployment, TaxYearSpecificScenario(isFirstTaxYearCrystallised = true, isWithinLatencyPeriod = true))
        checkOneTaxYear(SelfEmployment, TaxYearSpecificScenario(isFirstTaxYearCrystallised = true, isWithinLatencyPeriod = true))
      }
    }
    "303 SEE_OTHER - redirect to the Self Employment Obligations page" when {
      "user is out of latency period (before 23/24) - Self Employment" in {
        setupStubCalls(SelfEmployment, LegacyScenario(isFirstTaxYearCrystallised = false, isWithinLatencyPeriod = false))
        checkRedirect(SelfEmployment, redirectUrl(SelfEmployment))
      }
      "user is out of latency period (after 23/24) - Self Employment" in {
        setupStubCalls(SelfEmployment, TaxYearSpecificScenario(isFirstTaxYearCrystallised = false, isWithinLatencyPeriod = false))
        checkRedirect(SelfEmployment, redirectUrl(SelfEmployment))
      }
    }
    "500 INTERNAL_SERVER_ERROR" when {
      "API 1171 returns an error" in {
        setupStubErrorCall(API1171)
        checkError(SelfEmployment)
      }
      "API 1404 returns an error" in {
        setupStubErrorCall(API1404)
        checkError(SelfEmployment)
      }
      "API 1878 returns an error" in {
        setupStubErrorCall(API1878)
        checkError(SelfEmployment)
      }
      "API 1896 returns an error" in {
        setupStubErrorCall(API1896)
        checkError(SelfEmployment)
      }
    }
  }

  def checkSubmitRedirect(incomeSourceType: IncomeSourceType): Assertion = {
    val formData: Map[String, Seq[String]] = Map(
      "new_tax_year_1_reporting_method" -> Seq("A"),
      "new_tax_year_2_reporting_method" -> Seq("A"),
      "new_tax_year_1_reporting_method_tax_year" -> Seq(taxYear1.toString),
      "tax_year_1_reporting_method" -> Seq("Q"),
      "new_tax_year_2_reporting_method_tax_year" -> Seq(taxYear2.toString),
      "tax_year_2_reporting_method" -> Seq("Q")
    )
    And("API 1776 updateTaxYearSpecific returns a success response")
    IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel("2024-01-31T09:26:17Z")))

    val result: WSResponse = IncomeTaxViewChangeFrontend.post(uri(incomeSourceType))(formData)

    AuditStub.verifyAuditContainsDetail(
      IncomeSourceReportingMethodAuditModel(isSuccessful = true, incomeSourceType.journeyType, "ADD",
        "Annually", taxYearYYYYtoYYYY, businessName(incomeSourceType))(testUser).detail
    )

    result should have(
      httpStatus(SEE_OTHER),
      redirectURI(redirectUrl(incomeSourceType))
    )
  }

  def checkSubmitBadRequest(incomeSourceType: IncomeSourceType): Assertion = {
    val invalidFormData: Map[String, Seq[String]] = Map(
      "new_tax_year_1_reporting_method" -> Seq(),
      "new_tax_year_2_reporting_method" -> Seq(),
      "new_tax_year_1_reporting_method_tax_year" -> Seq(),
      "tax_year_1_reporting_method" -> Seq("Q"),
      "new_tax_year_2_reporting_method_tax_year" -> Seq(),
      "tax_year_2_reporting_method" -> Seq("Q")
    )

    val result: WSResponse = IncomeTaxViewChangeFrontend.post(uri(incomeSourceType))(invalidFormData)
    result should have(
      httpStatus(BAD_REQUEST)
    )
  }

  def checkSubmitErrorRedirect(incomeSourceType: IncomeSourceType): Assertion = {
    val formData: Map[String, Seq[String]] = Map(
      "new_tax_year_1_reporting_method" -> Seq("A"),
      "new_tax_year_2_reporting_method" -> Seq("A"),
      "new_tax_year_1_reporting_method_tax_year" -> Seq(taxYear1.toString),
      "tax_year_1_reporting_method" -> Seq("Q"),
      "new_tax_year_2_reporting_method_tax_year" -> Seq(taxYear2.toString),
      "tax_year_2_reporting_method" -> Seq("Q")
    )

    incomeSourceType match {
      case SelfEmployment => await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-SE",
        addIncomeSourceData = Some(AddIncomeSourceData(createdIncomeSourceId = Some(testSelfEmploymentId))))))
      case UkProperty => await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-UK",
        addIncomeSourceData = Some(AddIncomeSourceData(createdIncomeSourceId = Some(testSelfEmploymentId))))))
      case ForeignProperty => await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-FP",
        addIncomeSourceData = Some(AddIncomeSourceData(createdIncomeSourceId = Some(testSelfEmploymentId))))))
    }

    val result: WSResponse = IncomeTaxViewChangeFrontend.post(uri(incomeSourceType))(formData)

    AuditStub.verifyAuditContainsDetail(
      IncomeSourceReportingMethodAuditModel(isSuccessful = false, incomeSourceType.journeyType, "ADD",
        "Annually", taxYearYYYYtoYYYY, businessName(incomeSourceType))(testUser).detail
    )

    result should have(
      httpStatus(SEE_OTHER),
      redirectURI(errorRedirectUrl(incomeSourceType))
    )
  }

  s"calling POST ${submitUrl(false, UkProperty)}" should {
    s"303 SEE_OTHER - redirect to ${redirectUrl(UkProperty)}" when {
      "user completes the form and API 1776 updateIncomeSource returns a success response - UK Property" in {
        setupStubCalls(UkProperty, TaxYearSpecificScenario(isFirstTaxYearCrystallised = false, isWithinLatencyPeriod = true))
        checkSubmitRedirect(UkProperty)
      }
    }
    s"303 SEE_OTHER - redirect to ${errorRedirectUrl(UkProperty)}" when {
      "API 1776 updateIncomeSource returns a failure response" in {
        setupStubErrorCall(API1776)
        checkSubmitErrorRedirect(UkProperty)
      }
      "400 BAD_REQUEST" when {
        "user does not complete the form - UK Property" in {
          setupStubCalls(UkProperty, TaxYearSpecificScenario(isFirstTaxYearCrystallised = false, isWithinLatencyPeriod = true))
          checkSubmitBadRequest(UkProperty)
        }
      }
    }

    s"calling POST ${submitUrl(false, ForeignProperty)}" should {
      s"303 SEE_OTHER - redirect to ${redirectUrl(ForeignProperty)}" when {
        "user completes the form and API 1776 updateIncomeSource returns a success response - Foreign Property" in {
          setupStubCalls(ForeignProperty, TaxYearSpecificScenario(isFirstTaxYearCrystallised = false, isWithinLatencyPeriod = true))
          checkSubmitRedirect(ForeignProperty)
        }
      }
      s"303 SEE_OTHER - redirect to ${errorRedirectUrl(ForeignProperty)}" when {
        "API 1776 updateIncomeSource returns a failure response" in {
          setupStubErrorCall(API1776)
          checkSubmitErrorRedirect(ForeignProperty)
        }
      }
      "400 BAD_REQUEST" when {
        "user does not complete the form - Foreign Property" in {
          setupStubCalls(ForeignProperty, TaxYearSpecificScenario(isFirstTaxYearCrystallised = false, isWithinLatencyPeriod = true))
          checkSubmitBadRequest(ForeignProperty)
        }
      }
    }

    s"calling POST ${submitUrl(false, SelfEmployment)}" should {
      s"303 SEE_OTHER - redirect to ${redirectUrl(SelfEmployment)}" when {
        "user completes the form and API 1776 updateIncomeSource returns a success response - Self Employment" in {
          setupStubCalls(SelfEmployment, TaxYearSpecificScenario(isFirstTaxYearCrystallised = false, isWithinLatencyPeriod = true))
          checkSubmitRedirect(SelfEmployment)
        }
      }
      s"303 SEE_OTHER - redirect to ${errorRedirectUrl(SelfEmployment)}" when {
        "API 1776 updateIncomeSource returns a failure response" in {
          setupStubErrorCall(API1776)
          checkSubmitErrorRedirect(SelfEmployment)
        }
        "400 BAD_REQUEST" when {
          "user does not complete the form - Self Employment" in {
            setupStubCalls(SelfEmployment, TaxYearSpecificScenario(isFirstTaxYearCrystallised = false, isWithinLatencyPeriod = true))
            checkSubmitBadRequest(SelfEmployment)
          }
        }
      }
    }
  }
}
