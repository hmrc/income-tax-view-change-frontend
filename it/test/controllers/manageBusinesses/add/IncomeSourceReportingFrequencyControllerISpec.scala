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

package controllers.manageBusinesses.add

import controllers.ControllerISpecHelper
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.ITSAStatusDetailsStub.ITSAYearStatus
import helpers.servicemocks.{CalculationListStub, ITSAStatusDetailsStub, IncomeTaxViewChangeStub}
import models.UIJourneySessionData
import models.admin.NavBarFs
import models.incomeSourceDetails._
import models.itsaStatus.ITSAStatus.Voluntary
import play.api.http.Status.OK
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.{DateService, SessionService}
import testConstants.BaseIntegrationTestConstants._
import testConstants.BusinessDetailsIntegrationTestConstants.b1TradingName
import testConstants.CalculationListIntegrationTestConstants
import testConstants.IncomeSourceIntegrationTestConstants._

import java.time.LocalDate
import java.time.Month.APRIL

class IncomeSourceReportingFrequencyControllerISpec extends ControllerISpecHelper {
  override val dateService: DateService = app.injector.instanceOf[DateService] //overridden for TYS as implemented with 2023 elsewhere

  def redirectUrl(incomeSourceType: IncomeSourceType, mtdRole: MTDUserRole): String = {
    if(mtdRole == MTDIndividual){
      routes.IncomeSourceAddedController.show(incomeSourceType).url
    } else {
      routes.IncomeSourceAddedController.showAgent(incomeSourceType).url
    }
  }
  def errorRedirectUrl(incomeSourceType: IncomeSourceType, mtdRole: MTDUserRole): String = {
    if(mtdRole == MTDIndividual){
      routes.IncomeSourceReportingMethodNotSavedController.show(incomeSourceType).url
    } else {
      routes.IncomeSourceReportingMethodNotSavedController.showAgent(incomeSourceType).url
    }
  }

  def getPath(mtdRole: MTDUserRole, incomeSourceType: IncomeSourceType): String = {
    val pathStart = if(mtdRole == MTDIndividual) "/manage-your-businesses" else "/agents/manage-your-businesses"
    incomeSourceType match {
      case SelfEmployment => s"$pathStart/add-sole-trader/reporting-frequency"
      case UkProperty => s"$pathStart/add-uk-property/reporting-frequency"
      case ForeignProperty => s"$pathStart/add-foreign-property/reporting-frequency"
    }
  }
  val quarterlyIndicator: String = "Q"
  val annuallyIndicator: String = "A"
  val currentTaxYear: Int = dateService.getCurrentTaxYearEnd
  val taxYear1: Int = currentTaxYear
  val taxYear2: Int = currentTaxYear + 1
  val taxYear1YYtoYY: String = s"${(taxYear1 - 1).toString.takeRight(2)}-${taxYear1.toString.takeRight(2)}"
  val taxYear1YYtoYYForTimeMachineRemoval: String = s"${(taxYear1).toString.takeRight(2)}-${(taxYear1 + 1).toString.takeRight(2)}"
  val currentTaxYear1YY: String = s"${(taxYear1 - 1).toString.takeRight(2)}-${(taxYear1).toString.takeRight(2)}"
  val taxYear1YYYYtoYY: String = "20" + taxYear1YYtoYY
  val taxYear1YYYYtoYYForTimeMachineRemoval: String = "20" + taxYear1YYtoYYForTimeMachineRemoval
  val currentTaxYearRange: String = "20" + currentTaxYear1YY
  val legacyTaxYearRange: String = s"${(taxYear1 - 2).toString.takeRight(2)}-${(taxYear1 - 1).toString.takeRight(2)}"
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
  val latencyDetails2: LatencyDetails =
    LatencyDetails(
      latencyEndDate = lastDayOfCurrentTaxYear,
      taxYear1 = (taxYear1 - 1).toString,
      latencyIndicator1 = quarterlyIndicator,
      taxYear2 = (taxYear2 - 1).toString,
      latencyIndicator2 = annuallyIndicator
    )
  val legacyLatencyDetails: LatencyDetails =
    LatencyDetails(
      latencyEndDate = lastDayOfCurrentTaxYear,
      taxYear1 = (taxYear1 - 2).toString,
      latencyIndicator1 = quarterlyIndicator,
      taxYear2 = (taxYear2 -1 ).toString,
      latencyIndicator2 = annuallyIndicator
    )

  val businessName: IncomeSourceType => String = {
    case UkProperty => "UK property"
    case ForeignProperty => "Foreign property"
    case SelfEmployment => b1TradingName
  }

  val sessionService: SessionService = app.injector.instanceOf[SessionService]
  override def beforeEach(): Unit = {
    super.beforeEach()
    ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetailsWithGivenThreeStatus(dateService.getCurrentTaxYearEnd + 1, ITSAYearStatus(Voluntary, Voluntary, Voluntary))

    await(sessionService.deleteSession(Add))
  }
  def testUIJourneySessionData(incomeSourceType: IncomeSourceType): UIJourneySessionData = UIJourneySessionData(
    sessionId = testSessionId,
    journeyType = IncomeSourceJourneyType(Add, incomeSourceType).toString,
    addIncomeSourceData = Some(AddIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId))))

  def getIncomeSourceDetailsResponse(incomeSourceType: IncomeSourceType, isWithinLatencyPeriod: Boolean): IncomeSourceDetailsModel = {
    (incomeSourceType, isWithinLatencyPeriod) match {
      case (UkProperty, true) =>
        singleUKPropertyResponseInLatencyPeriod(latencyDetails)
      case (ForeignProperty, true) =>
        singleForeignPropertyResponseInLatencyPeriod(latencyDetails)
      case (SelfEmployment, true) =>
        singleBusinessResponseInLatencyPeriod(latencyDetails)
      case (_, false) =>
        multipleBusinessesWithBothPropertiesAndCeasedBusiness
    }
  }

  List(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSourceType =>
    mtdAllRoles.foreach { mtdUserRole =>
      val path = getPath(mtdUserRole, incomeSourceType)
      val additionalCookies = getAdditionalCookies(mtdUserRole)
      s"GET $path" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid enrolment" should {
            s"render the ${incomeSourceType.journeyType} Reporting Method page" when {
              val currentTaxYear = dateService.getCurrentTaxYearStart
              val taxYear1: Int = currentTaxYear.getYear
              val taxYear2: Int = taxYear1 + 1
              val taxYear1TYS: String = s"Reporting frequency $taxYear1 to $taxYear2"
              val taxYear2TYS: String = s"Reporting frequency $taxYear2 to ${taxYear2+1}"
              "user is within latency period (before 23/24) - tax year 1 not crystallised" in {
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, getIncomeSourceDetailsResponse(incomeSourceType, true))
                await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))

                CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear1.toString)(CalculationListIntegrationTestConstants
                  .successResponseNonCrystallised.toString())

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "incomeSources.add.reportingFrequency.h1"),
                  elementTextByID("reporting-frequency-table-row-1")(taxYear1TYS),
                  elementTextByID("reporting-frequency-table-row-3")(taxYear2TYS)
                )

                sessionService.getMongoKey(AddIncomeSourceData.incomeSourceAddedField, IncomeSourceJourneyType(Add, incomeSourceType)).futureValue shouldBe Right(Some(true))
              }
              "user is within latency period (before 23/24) - tax year 1 crystallised" in {
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, getIncomeSourceDetailsResponse(incomeSourceType, true))
                await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))
                ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", taxYear1YYYYtoYY)
                CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear1.toString)(CalculationListIntegrationTestConstants
                  .successResponseCrystallised.toString())

                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "incomeSources.add.reportingFrequency.h1"),
                  elementTextByID("reporting-frequency-table-row-1")(taxYear1TYS),
                  elementTextByID("reporting-frequency-table-row-3")(taxYear2TYS)
                )
                sessionService.getMongoKey(AddIncomeSourceData.incomeSourceAddedField, IncomeSourceJourneyType(Add, incomeSourceType)).futureValue shouldBe Right(Some(true))
              }
              "user is within latency period (after 23/24) - tax year 1 not crystallised" in {
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, getIncomeSourceDetailsResponse(incomeSourceType, true))
                await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))
                ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", taxYear1YYYYtoYY)
                CalculationListStub.stubGetCalculationList(testNino, taxYear1YYtoYY)(CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString())

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "incomeSources.add.reportingFrequency.h1"),
                  elementTextByID("reporting-frequency-table-row-1")(taxYear1TYS),
                  elementTextByID("reporting-frequency-table-row-3")(taxYear2TYS)
                )
                sessionService.getMongoKey(AddIncomeSourceData.incomeSourceAddedField, IncomeSourceJourneyType(Add, incomeSourceType)).futureValue shouldBe Right(Some(true))
              }
              "user is within latency period (after 23/24) - tax year 1 crystallised" in {
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, getIncomeSourceDetailsResponse(incomeSourceType, true))
                await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))
                ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", taxYear1YYYYtoYY)
                CalculationListStub.stubGetCalculationList(testNino, taxYear1YYtoYY)(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())

                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "incomeSources.add.reportingFrequency.h1"),
                  elementTextByID("reporting-frequency-table-row-1")(taxYear1TYS),
                  elementTextByID("reporting-frequency-table-row-3")(taxYear2TYS)
                )
                sessionService.getMongoKey(AddIncomeSourceData.incomeSourceAddedField, IncomeSourceJourneyType(Add, incomeSourceType)).futureValue shouldBe Right(Some(true))
              }
            }}
          testAuthFailures(path, mtdUserRole)
        }
      }
    }
  }
}
