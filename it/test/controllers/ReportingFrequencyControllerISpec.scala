/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers

import config.FrontendAppConfig
import enums.{MTDIndividual, MTDUserRole}
import helpers.WiremockHelper
import helpers.servicemocks.{ITSAStatusDetailsStub, IncomeTaxViewChangeStub}
import models.admin.{OptOutFs, ReportingFrequencyPage}
import models.itsaStatus.ITSAStatus.{Annual, Mandated, Voluntary}
import org.jsoup.Jsoup
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import repositories.UIJourneySessionDataRepository
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino}
import testConstants.IncomeSourceIntegrationTestConstants.businessAndPropertyResponseWoMigration

class ReportingFrequencyControllerISpec extends ControllerISpecHelper {

  override val appConfig: FrontendAppConfig = testAppConfig
  val repository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]

  val previousStartYear = dateService.getCurrentTaxYear.previousYear.startYear.toString
  val previousEndYear = dateService.getCurrentTaxYear.previousYear.endYear.toString
  val currentStartYear = dateService.getCurrentTaxYear.startYear.toString
  val currentEndYear = dateService.getCurrentTaxYear.endYear.toString
  val nextStartYear = dateService.getCurrentTaxYear.nextYear.startYear.toString
  val nextEndYear = dateService.getCurrentTaxYear.nextYear.endYear.toString



  def bullet(i: Int): String = s"#main-content > div > div > div > ul > li:nth-child($i) > a"

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if (mtdRole == MTDIndividual) "" else "/agents"
    pathStart + "/reporting-frequency"
  }

  def  stubCalculationListResponseBody(taxYearEnd: String) = {
    val responseBody = """
      |{
      |  "calculationId": "TEST_ID",
      |  "calculationTimestamp": "TEST_STAMP",
      |  "calculationType": "TEST_TYPE",
      |  "crystallised": false
      |}
      |""".stripMargin
    WiremockHelper.stubGet(s"/income-tax-view-change/list-of-calculation-results/$testNino/$taxYearEnd", OK, responseBody)
  }

  mtdAllRoles.foreach { case mtdUserRole =>
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          s"render reporting frequency page" that {
            "just has generic link for opt out" when {
              "CY is Quaterly and CY+1 is Quaterly" in {
                enable(ReportingFrequencyPage)
                enable(OptOutFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Mandated,
                  Voluntary,
                  Voluntary
                )
                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "reporting.frequency.title"),
                  elementTextBySelector(bullet(1))("Opt out of quarterly reporting and report annually")
                )
              }
            }

            "just has generic link for opt out" when {
              "CY is Annual and CY+1 is Annual" in {
                enable(ReportingFrequencyPage)
                enable(OptOutFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Mandated,
                  Annual,
                  Annual
                )
                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "reporting.frequency.title"),
                  elementTextBySelector(bullet(1))("Opt in to quarterly reporting")
                )
              }
            }

            "has tax year link for opt out and onwards link for opt in" when {
              "CY is Quaterly and CY+1 is Annual" in {
                enable(ReportingFrequencyPage)
                enable(OptOutFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Mandated,
                  Voluntary,
                  Annual
                )
                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "reporting.frequency.title"),
                  elementTextBySelector(bullet(1))(s"Opt out of quarterly reporting and report annually for the $currentStartYear to $currentEndYear tax year"),
                  elementTextBySelector(bullet(2))(s"Opt in to quarterly reporting from the $nextStartYear to $nextEndYear tax year onwards")
                )
              }
            }

            "has tax year link for opt in and onwards link for opt out" when {
              "CY is Annual and CY+1 is Quaterly" in {
                enable(ReportingFrequencyPage)
                enable(OptOutFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Mandated,
                  Annual,
                  Voluntary
                )

                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "reporting.frequency.title"),
                  elementTextBySelector(bullet(1))(s"Opt in to quarterly reporting for the $currentStartYear to $currentEndYear tax year"),
                  elementTextBySelector(bullet(2))(s"Opt out of quarterly reporting and report annually from the $nextStartYear to $nextEndYear tax year onwards")
                )
              }
            }

            "just has tax year link for opt in" when {
              "CY is Annual and CY+1 is Quaterly Mandated" in {
                enable(ReportingFrequencyPage)
                enable(OptOutFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Mandated,
                  Annual,
                  Mandated
                )

                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "reporting.frequency.title"),
                  elementTextBySelector(bullet(1))(s"Opt in to quarterly reporting for the $currentStartYear to $currentEndYear tax year")
                )
              }

              "CY is Quaterly Mandated and CY+1 is Annual" in {
                enable(ReportingFrequencyPage)
                enable(OptOutFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Mandated,
                  Mandated,
                  Annual
                )
                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "reporting.frequency.title"),
                  elementTextBySelector(bullet(1))(s"Opt in to quarterly reporting from the $nextStartYear to $nextEndYear tax year onwards")
                )
              }
            }

            "just has tax year onwards link for opt out" when {
              "CY is Quaterly Mandated and CY+1 is Quaterly" in {
                enable(ReportingFrequencyPage)
                enable(OptOutFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Mandated,
                  Mandated,
                  Voluntary
                )

                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "reporting.frequency.title"),
                  elementTextBySelector(bullet(1))(s"Opt out of quarterly reporting and report annually from the $nextStartYear to $nextEndYear tax year onwards")
                )
              }
            }

            "has generic link for opt out and tax year onward link for opt in" when {
              "CY-1 is Quaterly, CY is Quaterly and CY+1 is Annual" in {
                enable(ReportingFrequencyPage)
                enable(OptOutFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Voluntary,
                  Voluntary,
                  Annual
                )

                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "reporting.frequency.title"),
                  elementTextBySelector(bullet(1))("Opt out of quarterly reporting and report annually"),
                  elementTextBySelector(bullet(2))(s"Opt in to quarterly reporting from the $nextStartYear to $nextEndYear tax year onwards")
                )
              }
            }

            "has tax year link for opt out and generic link for opt in" when {
              "CY-1 is Quaterly, CY is Annual and CY+1 is Annual" in {
                enable(ReportingFrequencyPage)
                enable(OptOutFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Voluntary,
                  Annual,
                  Annual
                )

                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "reporting.frequency.title"),
                  elementTextBySelector(bullet(1))(s"Opt out of quarterly reporting and report annually for the $previousStartYear to $previousEndYear tax year"),
                  elementTextBySelector(bullet(2))(s"Opt in to quarterly reporting")
                )
              }
            }
          }

          "render the error page" when {
            "ReportingFrequencyPage feature switch is disabled" in {
              disable(ReportingFrequencyPage)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
              stubCalculationListResponseBody("2022")
              val result = buildGETMTDClient(path, additionalCookies).futureValue
              result should have(
                httpStatus(INTERNAL_SERVER_ERROR)
              )
              Jsoup.parse(result.body).title shouldBe "Sorry, there is a problem with the service - GOV.UK"

            }
          }
        }
        testAuthFailures(path, mtdUserRole)
      }
    }
  }
}
