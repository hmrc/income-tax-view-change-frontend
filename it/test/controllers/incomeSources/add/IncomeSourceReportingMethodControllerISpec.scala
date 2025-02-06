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
import controllers.ControllerISpecHelper
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.ITSAStatusDetailsStub.stubGetITSAStatusDetailsError
import helpers.servicemocks.{AuditStub, CalculationListStub, ITSAStatusDetailsStub, IncomeTaxViewChangeStub}
import models.admin.{IncomeSourcesFs, NavBarFs}
import models.incomeSourceDetails._
import models.updateIncomeSource.UpdateIncomeSourceResponseModel
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.{DateService, SessionService}
import testConstants.BaseIntegrationTestConstants._
import testConstants.BusinessDetailsIntegrationTestConstants.b1TradingName
import testConstants.CalculationListIntegrationTestConstants
import testConstants.IncomeSourceIntegrationTestConstants._

import java.time.LocalDate
import java.time.Month.APRIL

class IncomeSourceReportingMethodControllerISpec extends ControllerISpecHelper {
  override val dateService: DateService =
    app.injector.instanceOf[DateService] //overridden for TYS as implemented with 2023 elsewhere

  def redirectUrl(incomeSourceType: IncomeSourceType, mtdRole: MTDUserRole): String = {
    if (mtdRole == MTDIndividual) {
      routes.IncomeSourceAddedController.show(incomeSourceType).url
    } else {
      routes.IncomeSourceAddedController.showAgent(incomeSourceType).url
    }
  }
  def errorRedirectUrl(incomeSourceType: IncomeSourceType, mtdRole: MTDUserRole): String = {
    if (mtdRole == MTDIndividual) {
      routes.IncomeSourceReportingMethodNotSavedController.show(incomeSourceType).url
    } else {
      routes.IncomeSourceReportingMethodNotSavedController.showAgent(incomeSourceType).url
    }
  }

  def getPath(mtdRole: MTDUserRole, incomeSourceType: IncomeSourceType): String = {
    val pathStart = if (mtdRole == MTDIndividual) "/income-sources/add" else "/agents/income-sources/add"
    incomeSourceType match {
      case SelfEmployment  => s"$pathStart/business-reporting-method"
      case UkProperty      => s"$pathStart/uk-property-reporting-method"
      case ForeignProperty => s"$pathStart/foreign-property-reporting-method"
    }
  }

  val quarterlyIndicator: String = "Q"
  val annuallyIndicator:  String = "A"
  val currentTaxYear:     Int    = dateService.getCurrentTaxYearEnd
  val taxYear1:           Int    = currentTaxYear
  val taxYear2:           Int    = currentTaxYear + 1
  val taxYear1YYtoYY:     String = s"${(taxYear1 - 1).toString.takeRight(2)}-${taxYear1.toString.takeRight(2)}"
  val taxYear1YYtoYYForTimeMachineRemoval: String =
    s"${(taxYear1).toString.takeRight(2)}-${(taxYear1 + 1).toString.takeRight(2)}"
  val taxYear1YYYYtoYY:                      String = "20" + taxYear1YYtoYY
  val taxYear1YYYYtoYYForTimeMachineRemoval: String = "20" + taxYear1YYtoYYForTimeMachineRemoval
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
    case UkProperty      => "UK property"
    case ForeignProperty => "Foreign property"
    case SelfEmployment  => b1TradingName
  }

  val sessionService: SessionService = app.injector.instanceOf[SessionService]
  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Add))
  }
  def testUIJourneySessionData(incomeSourceType: IncomeSourceType): UIJourneySessionData =
    UIJourneySessionData(
      sessionId = testSessionId,
      journeyType = IncomeSourceJourneyType(Add, incomeSourceType).toString,
      addIncomeSourceData = Some(AddIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId)))
    )

  def getIncomeSourceDetailsResponse(
      incomeSourceType:      IncomeSourceType,
      isWithinLatencyPeriod: Boolean
    ): IncomeSourceDetailsModel = {
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

  val validFormData: Map[String, Seq[String]] = Map(
    "new_tax_year_1_reporting_method"          -> Seq("A"),
    "new_tax_year_2_reporting_method"          -> Seq("A"),
    "new_tax_year_1_reporting_method_tax_year" -> Seq(taxYear1.toString),
    "tax_year_1_reporting_method"              -> Seq("Q"),
    "new_tax_year_2_reporting_method_tax_year" -> Seq(taxYear2.toString),
    "tax_year_2_reporting_method"              -> Seq("Q")
  )

  List(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSourceType =>
    mtdAllRoles.foreach { mtdUserRole =>
      val path              = getPath(mtdUserRole, incomeSourceType)
      val additionalCookies = getAdditionalCookies(mtdUserRole)
      s"GET $path" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid enrolment" should {
            s"render the ${incomeSourceType.journeyType} Reporting Method page" when {
              val currentTaxYear = dateService.getCurrentTaxYearEnd
              val taxYear1:    Int    = currentTaxYear
              val taxYear2:    Int    = currentTaxYear + 1
              val taxYear1TYS: String = s"${taxYear1 - 1}-$taxYear1"
              val taxYear2TYS: String = s"${taxYear2 - 1}-$taxYear2"
              "user is within latency period (before 23/24) - tax year 1 not crystallised" in {
                enable(IncomeSourcesFs)
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                  OK,
                  getIncomeSourceDetailsResponse(incomeSourceType, true)
                )
                await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))
                ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", taxYear1YYYYtoYY)
                CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear1.toString)(
                  CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString()
                )

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "incomeSources.add.incomeSourceReportingMethod.heading"),
                  elementTextBySelectorList("#add-uk-property-reporting-method-form", "div:nth-of-type(3)", "legend")(
                    s"Tax year $taxYear1TYS"
                  ),
                  elementTextBySelectorList("#add-uk-property-reporting-method-form", "div:nth-of-type(7)", "legend")(
                    s"Tax year $taxYear2TYS"
                  )
                )

                sessionService
                  .getMongoKey(
                    AddIncomeSourceData.incomeSourceAddedField,
                    IncomeSourceJourneyType(Add, incomeSourceType)
                  )
                  .futureValue shouldBe Right(Some(true))
              }
              "user is within latency period (before 23/24) - tax year 1 crystallised" in {
                enable(IncomeSourcesFs)
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                  OK,
                  getIncomeSourceDetailsResponse(incomeSourceType, true)
                )
                await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))
                ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", taxYear1YYYYtoYY)
                CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear1.toString)(
                  CalculationListIntegrationTestConstants.successResponseCrystallised.toString()
                )

                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "incomeSources.add.incomeSourceReportingMethod.heading"),
                  elementCountBySelector("#add-uk-property-reporting-method-form > legend:nth-of-type(2)")(0),
                  elementTextBySelectorList("#add-uk-property-reporting-method-form", "legend:nth-of-type(1)")(
                    s"Tax year 2024-2025"
                  )
                )
                sessionService
                  .getMongoKey(
                    AddIncomeSourceData.incomeSourceAddedField,
                    IncomeSourceJourneyType(Add, incomeSourceType)
                  )
                  .futureValue shouldBe Right(Some(true))
              }
              "user is within latency period (after 23/24) - tax year 1 not crystallised" in {
                enable(IncomeSourcesFs)
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                  OK,
                  getIncomeSourceDetailsResponse(incomeSourceType, true)
                )
                await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))
                ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", taxYear1YYYYtoYY)
                CalculationListStub.stubGetCalculationList(testNino, taxYear1YYtoYY)(
                  CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString()
                )

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "incomeSources.add.incomeSourceReportingMethod.heading"),
                  elementTextBySelectorList("#add-uk-property-reporting-method-form", "div:nth-of-type(3)", "legend")(
                    s"Tax year $taxYear1TYS"
                  ),
                  elementTextBySelectorList("#add-uk-property-reporting-method-form", "div:nth-of-type(7)", "legend")(
                    s"Tax year $taxYear2TYS"
                  )
                )
                sessionService
                  .getMongoKey(
                    AddIncomeSourceData.incomeSourceAddedField,
                    IncomeSourceJourneyType(Add, incomeSourceType)
                  )
                  .futureValue shouldBe Right(Some(true))
              }
              "user is within latency period (after 23/24) - tax year 1 crystallised" in {
                enable(IncomeSourcesFs)
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                  OK,
                  getIncomeSourceDetailsResponse(incomeSourceType, true)
                )
                await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))
                ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", taxYear1YYYYtoYY)
                CalculationListStub.stubGetCalculationList(testNino, taxYear1YYtoYY)(
                  CalculationListIntegrationTestConstants.successResponseCrystallised.toString()
                )

                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "incomeSources.add.incomeSourceReportingMethod.heading"),
                  elementCountBySelector("#add-uk-property-reporting-method-form > legend:nth-of-type(2)")(0),
                  elementTextBySelectorList("#add-uk-property-reporting-method-form", "legend:nth-of-type(1)")(
                    s"Tax year $taxYear1TYS"
                  )
                )
                sessionService
                  .getMongoKey(
                    AddIncomeSourceData.incomeSourceAddedField,
                    IncomeSourceJourneyType(Add, incomeSourceType)
                  )
                  .futureValue shouldBe Right(Some(true))
              }
            }

            s"redirect to the ${incomeSourceType.journeyType} Obligations page" when {
              "user is out of latency period (before 23/24)" in {
                enable(IncomeSourcesFs)
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                  OK,
                  getIncomeSourceDetailsResponse(incomeSourceType, false)
                )
                await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))
                ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", taxYear1YYYYtoYY)
                CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear1.toString)(
                  CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString()
                )

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(redirectUrl(incomeSourceType, mtdUserRole))
                )
              }
              "user is out of latency period (after 23/24)" in {
                enable(IncomeSourcesFs)
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                  OK,
                  getIncomeSourceDetailsResponse(incomeSourceType, false)
                )
                await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))
                ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", taxYear1YYYYtoYY)
                CalculationListStub.stubGetCalculationList(testNino, taxYear1YYtoYY)(
                  CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString()
                )

                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(redirectUrl(incomeSourceType, mtdUserRole))
                )
              }
            }
            "500 INTERNAL_SERVER_ERROR" when {
              "API 1171 returns an error" in {
                enable(IncomeSourcesFs)
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                  INTERNAL_SERVER_ERROR,
                  IncomeSourceDetailsError(INTERNAL_SERVER_ERROR, "ISE")
                )
                await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))

                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(httpStatus(INTERNAL_SERVER_ERROR))
              }
              "API 1404 returns an error" in {
                enable(IncomeSourcesFs)
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                  OK,
                  allBusinessesAndPropertiesInLatencyPeriod(latencyDetails)
                )
                await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))
                ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", taxYear1YYYYtoYYForTimeMachineRemoval)
                CalculationListStub.stubGetLegacyCalculationListError(testNino, (taxYear1 + 1).toString)

                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(httpStatus(INTERNAL_SERVER_ERROR))
              }
              "API 1878 returns an error" in {
                enable(IncomeSourcesFs)
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                  OK,
                  allBusinessesAndPropertiesInLatencyPeriod(latencyDetails)
                )
                await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))
                stubGetITSAStatusDetailsError()

                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(httpStatus(INTERNAL_SERVER_ERROR))
              }
              "API 1896 returns an error" in {
                enable(IncomeSourcesFs)
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                  OK,
                  allBusinessesAndPropertiesInLatencyPeriod(latencyDetails)
                )
                await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))
                ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", taxYear1YYYYtoYYForTimeMachineRemoval)
                CalculationListStub.stubGetCalculationListError(testNino, taxYear1YYtoYYForTimeMachineRemoval)

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(httpStatus(INTERNAL_SERVER_ERROR))
              }
            }
          }
          testAuthFailures(path, mtdUserRole)
        }
      }

      s"POST $path" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid enrolment" should {
            s"303 SEE_OTHER - redirect to ${redirectUrl(incomeSourceType, mtdUserRole)}" when {
              "user completes the form and API 1776 updateIncomeSource returns a success response - UK Property" in {
                enable(IncomeSourcesFs)
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                  OK,
                  getIncomeSourceDetailsResponse(incomeSourceType, true)
                )
                await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))
                CalculationListStub.stubGetCalculationList(testNino, taxYear1YYtoYY)(
                  CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString()
                )
                IncomeTaxViewChangeStub.stubUpdateIncomeSource(
                  OK,
                  Json.toJson(UpdateIncomeSourceResponseModel("2024-01-31T09:26:17Z"))
                )

                val result = buildPOSTMTDPostClient(path, additionalCookies, validFormData).futureValue
                AuditStub.verifyAuditContainsDetail(
                  IncomeSourceReportingMethodAuditModel(
                    isSuccessful = true,
                    incomeSourceType.journeyType,
                    "ADD",
                    "Annually",
                    taxYearYYYYtoYYYY,
                    businessName(incomeSourceType)
                  )(getTestUser(mtdUserRole, multipleBusinessesAndPropertyResponse)).detail
                )

                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(redirectUrl(incomeSourceType, mtdUserRole))
                )
              }
            }
            s"303 SEE_OTHER - redirect to ${errorRedirectUrl(incomeSourceType, mtdUserRole)}" when {
              "API 1776 updateIncomeSource returns a failure response" in {
                enable(IncomeSourcesFs)
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                  OK,
                  allBusinessesAndPropertiesInLatencyPeriod(latencyDetails)
                )
                await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))
                ITSAStatusDetailsStub.stubGetITSAStatusDetails("MTD Mandated", taxYear1YYYYtoYYForTimeMachineRemoval)
                IncomeTaxViewChangeStub.stubUpdateIncomeSourceError()

                val result = buildPOSTMTDPostClient(path, additionalCookies, validFormData).futureValue

                AuditStub.verifyAuditContainsDetail(
                  IncomeSourceReportingMethodAuditModel(
                    isSuccessful = false,
                    incomeSourceType.journeyType,
                    "ADD",
                    "Annually",
                    taxYearYYYYtoYYYY,
                    businessName(incomeSourceType)
                  )(getTestUser(mtdUserRole, multipleBusinessesAndPropertyResponse)).detail
                )

                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(errorRedirectUrl(incomeSourceType, mtdUserRole))
                )
              }
            }
            "400 BAD_REQUEST" when {
              "user does not complete the form - UK Property" in {
                enable(IncomeSourcesFs)
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                  OK,
                  getIncomeSourceDetailsResponse(incomeSourceType, true)
                )
                await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))
                CalculationListStub.stubGetCalculationList(testNino, taxYear1YYtoYY)(
                  CalculationListIntegrationTestConstants.successResponseNonCrystallised.toString()
                )
                val invalidFormData: Map[String, Seq[String]] = Map(
                  "new_tax_year_1_reporting_method"          -> Seq(),
                  "new_tax_year_2_reporting_method"          -> Seq(),
                  "new_tax_year_1_reporting_method_tax_year" -> Seq(),
                  "tax_year_1_reporting_method"              -> Seq("Q"),
                  "new_tax_year_2_reporting_method_tax_year" -> Seq(),
                  "tax_year_2_reporting_method"              -> Seq("Q")
                )

                val result = buildPOSTMTDPostClient(path, additionalCookies, invalidFormData).futureValue
                result should have(
                  httpStatus(BAD_REQUEST)
                )
              }
            }
          }
          testAuthFailures(path, mtdUserRole, optBody = Some(validFormData))
        }
      }
    }
  }
}
