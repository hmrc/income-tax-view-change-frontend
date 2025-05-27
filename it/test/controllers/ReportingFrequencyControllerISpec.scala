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

import enums.{MTDIndividual, MTDUserRole}
import helpers.WiremockHelper
import helpers.servicemocks.{ITSAStatusDetailsStub, IncomeTaxViewChangeStub}
import models.admin.{OptOutFs, ReportingFrequencyPage}
import models.itsaStatus.ITSAStatus.{Annual, Mandated, NoStatus, Voluntary}
import org.jsoup.Jsoup
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import repositories.UIJourneySessionDataRepository
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino}
import testConstants.IncomeSourceIntegrationTestConstants._

class ReportingFrequencyControllerISpec extends ControllerISpecHelper {

  val repository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]

  val previousStartYear = dateService.getCurrentTaxYear.previousYear.startYear.toString
  val previousEndYear = dateService.getCurrentTaxYear.previousYear.endYear.toString
  val currentStartYear = dateService.getCurrentTaxYear.startYear.toString
  val currentEndYear = dateService.getCurrentTaxYear.endYear.toString
  val nextStartYear = dateService.getCurrentTaxYear.nextYear.startYear.toString
  val nextEndYear = dateService.getCurrentTaxYear.nextYear.endYear.toString



  def optInOptOutLinks(i: Int): String = s"#main-content > div > div > div > ul > li:nth-child($i) > a"
  def latencyDetailsHeader: String = s"#main-content > div > div > div > details > summary > span"
  def latencyDetailsLink: String = "#rf-latency-para3-text > a"

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
                enable(ReportingFrequencyPage, OptOutFs)
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
                  elementTextByID("manage-reporting-frequency-heading")("Manage your reporting frequency for all your businesses"),
                  elementTextBySelector(optInOptOutLinks(1))("Opt out of quarterly reporting and report annually")
                )
                result shouldNot have(
                  elementTextBySelector(latencyDetailsHeader)("Your new businesses can have a different reporting frequency"),
                    elementTextByID("ceased-business-warning")("Warning There are currently no businesses on this account. You can add a sole trader or property business on the all businesses page.")
                )
              }
            }

            "just has generic link for opt out" when {
              "CY is Annual and CY+1 is Annual" in {
                enable(ReportingFrequencyPage, OptOutFs)
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
                  elementTextByID("manage-reporting-frequency-heading")("Manage your reporting frequency for all your businesses"),
                  elementTextBySelector(optInOptOutLinks(1))("Opt in to quarterly reporting")
                )
                result shouldNot have(
                  elementTextBySelector(latencyDetailsHeader)("Your new businesses can have a different reporting frequency"),
                  elementTextByID("ceased-business-warning")("Warning There are currently no businesses on this account. You can add a sole trader or property business on the all businesses page.")
                )
              }
            }

            "has tax year link for opt out and onwards link for opt in" when {
              "CY is Quaterly and CY+1 is Annual" in {
                enable(ReportingFrequencyPage, OptOutFs)
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
                  elementTextByID("manage-reporting-frequency-heading")("Manage your reporting frequency for all your businesses"),
                  elementTextBySelector(optInOptOutLinks(1))(s"Opt out of quarterly reporting and report annually for the $currentStartYear to $currentEndYear tax year"),
                  elementTextBySelector(optInOptOutLinks(2))(s"Opt in to quarterly reporting from the $nextStartYear to $nextEndYear tax year onwards")
                )
                result shouldNot have(
                  elementTextBySelector(latencyDetailsHeader)("Your new businesses can have a different reporting frequency"),
                  elementTextByID("ceased-business-warning")("Warning There are currently no businesses on this account. You can add a sole trader or property business on the all businesses page.")
                )
              }
            }

            "has tax year link for opt in and onwards link for opt out" when {
              "CY is Annual and CY+1 is Quaterly" in {
                enable(ReportingFrequencyPage, OptOutFs)
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
                  elementTextByID("manage-reporting-frequency-heading")("Manage your reporting frequency for all your businesses"),
                  elementTextBySelector(optInOptOutLinks(1))(s"Opt in to quarterly reporting for the $currentStartYear to $currentEndYear tax year"),
                  elementTextBySelector(optInOptOutLinks(2))(s"Opt out of quarterly reporting and report annually from the $nextStartYear to $nextEndYear tax year onwards")
                )
                result shouldNot have(
                  elementTextBySelector(latencyDetailsHeader)("Your new businesses can have a different reporting frequency"),
                  elementTextByID("ceased-business-warning")("Warning There are currently no businesses on this account. You can add a sole trader or property business on the all businesses page.")
                )
              }
            }

            "just has tax year link for opt in" when {
              "CY is Annual and CY+1 is Quaterly Mandated" in {
                enable(ReportingFrequencyPage, OptOutFs)
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
                  elementTextBySelector(optInOptOutLinks(1))(s"Opt in to quarterly reporting for the $currentStartYear to $currentEndYear tax year")
                )
                result shouldNot have(
                  elementTextBySelector(latencyDetailsHeader)("Your new businesses can have a different reporting frequency"),
                  elementTextByID("ceased-business-warning")("Warning There are currently no businesses on this account. You can add a sole trader or property business on the all businesses page.")
                )
              }

              "CY is Quaterly Mandated and CY+1 is Annual" in {
                enable(ReportingFrequencyPage, OptOutFs)
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
                  elementTextByID("manage-reporting-frequency-heading")("Manage your reporting frequency for all your businesses"),
                  elementTextBySelector(optInOptOutLinks(1))(s"Opt in to quarterly reporting from the $nextStartYear to $nextEndYear tax year onwards")
                )
                result shouldNot have(
                  elementTextBySelector(latencyDetailsHeader)("Your new businesses can have a different reporting frequency"),
                  elementTextByID("ceased-business-warning")("Warning There are currently no businesses on this account. You can add a sole trader or property business on the all businesses page.")
                )
              }
            }

            "just has tax year onwards link for opt out" when {
              "CY is Quaterly Mandated and CY+1 is Quaterly" in {
                enable(ReportingFrequencyPage, OptOutFs)
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
                  elementTextByID("manage-reporting-frequency-heading")("Manage your reporting frequency for all your businesses"),
                  elementTextBySelector(optInOptOutLinks(1))(s"Opt out of quarterly reporting and report annually from the $nextStartYear to $nextEndYear tax year onwards")
                )
                result shouldNot have(
                  elementTextBySelector(latencyDetailsHeader)("Your new businesses can have a different reporting frequency"),
                  elementTextByID("ceased-business-warning")("Warning There are currently no businesses on this account. You can add a sole trader or property business on the all businesses page.")
                )
              }
            }

            "has generic link for opt out and tax year onward link for opt in" when {
              "CY-1 is Quaterly, CY is Quaterly and CY+1 is Annual" in {
                enable(ReportingFrequencyPage, OptOutFs)
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
                  elementTextByID("manage-reporting-frequency-heading")("Manage your reporting frequency for all your businesses"),
                  elementTextBySelector(optInOptOutLinks(1))("Opt out of quarterly reporting and report annually"),
                  elementTextBySelector(optInOptOutLinks(2))(s"Opt in to quarterly reporting from the $nextStartYear to $nextEndYear tax year onwards")
                )
                result shouldNot have(
                  elementTextBySelector(latencyDetailsHeader)("Your new businesses can have a different reporting frequency"),
                  elementTextByID("ceased-business-warning")("Warning There are currently no businesses on this account. You can add a sole trader or property business on the all businesses page.")
                )
              }
            }

            "has tax year link for opt out and generic link for opt in" when {
              "CY-1 is Quaterly, CY is Annual and CY+1 is Annual" in {
                enable(ReportingFrequencyPage, OptOutFs)
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
                  elementTextByID("manage-reporting-frequency-heading")("Manage your reporting frequency for all your businesses"),
                  elementTextBySelector(optInOptOutLinks(1))(s"Opt out of quarterly reporting and report annually for the $previousStartYear to $previousEndYear tax year"),
                  elementTextBySelector(optInOptOutLinks(2))(s"Opt in to quarterly reporting")
                )
                result shouldNot have(
                  elementTextBySelector(latencyDetailsHeader)("Your new businesses can have a different reporting frequency"),
                  elementTextByID("ceased-business-warning")("Warning There are currently no businesses on this account. You can add a sole trader or property business on the all businesses page.")
                )
              }
            }

            "does not have Manage your reporting frequency section" when {
              "all business are ceased" in {
                enable(ReportingFrequencyPage, OptOutFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignAndSoleTraderCeasedBusiness)
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
                  pageTitle(mtdUserRole, "reporting.frequency.title")
                )
                result shouldNot have(
                  elementTextByID("manage-reporting-frequency-heading")("Manage your reporting frequency for all your businesses"),
                  elementTextBySelector(optInOptOutLinks(1))(s"Opt out of quarterly reporting and report annually for the $previousStartYear to $previousEndYear tax year"),
                  elementTextBySelector(optInOptOutLinks(2))(s"Opt in to quarterly reporting"),
                  elementTextBySelector(latencyDetailsHeader)("Your new businesses can have a different reporting frequency")
                )
              }
              "CY, CY-1 and CY+1 are mandated" in {
                enable(ReportingFrequencyPage, OptOutFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Mandated,
                  Mandated,
                  Mandated
                )
                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "reporting.frequency.title")
                )
                result shouldNot have(
                  elementTextByID("manage-reporting-frequency-heading")("Manage your reporting frequency for all your businesses"),
                  elementTextBySelector(optInOptOutLinks(1))(s"Opt out of quarterly reporting and report annually for the $previousStartYear to $previousEndYear tax year"),
                  elementTextBySelector(optInOptOutLinks(2))(s"Opt in to quarterly reporting")
                )
                result shouldNot have(
                  elementTextBySelector(latencyDetailsHeader)("Your new businesses can have a different reporting frequency")
                )
              }
              "CY is mandated and no opt in or opt out years to be displayed" in {
                enable(ReportingFrequencyPage, OptOutFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  NoStatus,
                  Mandated,
                  NoStatus
                )
                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "reporting.frequency.title")
                )
                result shouldNot have(
                  elementTextByID("manage-reporting-frequency-heading")("Manage your reporting frequency for all your businesses"),
                  elementTextBySelector(optInOptOutLinks(1))(s"Opt out of quarterly reporting and report annually for the $previousStartYear to $previousEndYear tax year"),
                  elementTextBySelector(optInOptOutLinks(2))(s"Opt in to quarterly reporting")
                )
                result shouldNot have(
                  elementTextBySelector(latencyDetailsHeader)("Your new businesses can have a different reporting frequency")
                )
              }
            }
            "has a ceased business warning" when {
              "all businesses have ceased" in {
                enable(ReportingFrequencyPage, OptOutFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignAndSoleTraderCeasedBusiness)
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
                  elementTextByID("ceased-business-warning")("Warning There are currently no businesses on this account. You can add a sole trader or property business on the all businesses page.")
                )
              }
            }

            "have latency related information section" when {
              Seq(("sole trader", businessWithLatency), ("property", propertyWithLatency), ("all", allBusinessesWithLatency)).foreach { response =>
                s"${response._1} business is latent" in {
                  enable(ReportingFrequencyPage, OptOutFs)
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, response._2)
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
                    elementTextBySelector(latencyDetailsHeader)("Your new businesses can have a different reporting frequency"),
                    elementTextByID("rf-latency-para1")("For tax years you report quarterly, you can separately choose to report annually for any new sole trader or property income source:"),
                    elementTextByID("rf-latency-para1-bullet1")("started less than 2 years ago"),
                    elementTextByID("rf-latency-para1-bullet2")("that you start in future"),
                    elementTextByID("rf-latency-para2")("This option is available to new businesses:"),
                    elementTextByID("rf-latency-para2-bullet1")("for up to 2 tax years"),
                    elementTextByID("rf-latency-para2-bullet2")("only when you report quarterly for your other businesses"),
                    elementTextByID("rf-latency-para2-bullet3")("even if your income from self-employment or property, or both, exceeds the income threshold"),
                    elementTextByID("rf-latency-small-heading")("How to change a new income source’s reporting frequency"),
                    elementTextByID("rf-latency-para3-text")("You can do this at any time in the all businesses section")
                  )

                  if (mtdUserRole == MTDIndividual) {
                    result should have(
                      elementAttributeBySelector(latencyDetailsLink, "href")("/report-quarterly/income-and-expenses/view/manage-your-businesses")
                    )
                  } else {
                    result should have(
                      elementAttributeBySelector(latencyDetailsLink, "href")("/report-quarterly/income-and-expenses/view/agents/manage-your-businesses")
                    )
                  }
                }
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
