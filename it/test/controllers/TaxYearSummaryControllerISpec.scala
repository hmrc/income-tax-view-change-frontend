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

package controllers

import audit.models.{NextUpdatesResponseAuditModel, TaxYearSummaryResponseAuditModel}
import enums.{MTDIndividual, MTDSupportingAgent, MTDUserRole}
import helpers.servicemocks.AuditStub.{verifyAuditContainsDetail, verifyAuditEvent}
import helpers.servicemocks._
import models.admin._
import models.financialDetails._
import models.liabilitycalculation.LiabilityCalculationError
import models.liabilitycalculation.viewmodels.{CalculationSummary, TaxYearSummaryViewModel}
import models.obligations.{GroupedObligationsModel, ObligationsModel, SingleObligationModel, StatusFulfilled}
import models.taxyearsummary.TaxYearSummaryChargeItem
import org.jsoup.Jsoup
import play.api.http.Status._
import play.api.libs.json.Json
import testConstants.BaseIntegrationTestConstants._
import testConstants.CalculationListIntegrationTestConstants.successResponseNonCrystallised
import testConstants.IncomeSourceIntegrationTestConstants._
import testConstants.NewCalcBreakdownItTestConstants._
import testConstants.messages.TaxYearSummaryMessages._

import java.time.LocalDate

class TaxYearSummaryControllerISpec extends TaxSummaryISpecHelper {

  def getPath(mtdRole: MTDUserRole, year: String = getCurrentTaxYearEnd.getYear.toString): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    pathStart + s"/tax-year-summary/$year"
  }

  mtdAllRoles.foreach { case mtdUserRole =>
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          if (mtdUserRole == MTDSupportingAgent) {
            testSupportingAgentAccessDenied(path, additionalCookies)
          } else {
            "render the tax summary page" that {
              "includes the latest and previous calculations tab" in {
                enable(PostFinalisationAmendmentsR18)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)
                IncomeTaxCalculationStub.stubGetCalculationResponseWithFlagResponse(testNino, getCurrentTaxYearEnd.getYear.toString, "LATEST")(
                  status = OK,
                  body = liabilityCalculationModelSuccessfulWithAmendment
                )
                IncomeTaxCalculationStub.stubGetCalculationResponseWithFlagResponse(testNino, getCurrentTaxYearEnd.getYear.toString, "PREVIOUS")(
                  status = OK,
                  body = liabilityCalculationModelSuccessfulNotCrystallised
                )
                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
                  nino = testNino,
                  from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
                  to = getCurrentTaxYearEnd.toString
                )(
                  status = OK,
                  response = Json.toJson(financialDetailsSuccess)
                )
                IncomeTaxViewChangeStub.stubGetAllObligations(
                  nino = testNino,
                  fromDate = getCurrentTaxYearEnd.minusYears(1).plusDays(1),
                  toDate = getCurrentTaxYearEnd,
                  deadlines = allObligations
                )

                val res = buildGETMTDClient(path, additionalCookies).futureValue

                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

                res should have(
                  httpStatus(OK),
                  elementTextByID("latest-calculation-overview-description")("Your tax return was amended on 15 February 2019 and as a result this is your most up-to-date calculation."),
                  elementTextByID("previous-calculation-overview-description")("When your tax return is amended it changes your tax calculation. If this happens, this page shows any previous tax calculations you may have."),
                  elementTextByID("previous-calculation-note")("The tax return was filed then."),
                  elementTextByID("previous-calculation-bullet-start")("You can change your tax return after you have filed it. To do this online you must:"),
                  elementTextByID("previous-calculation-bullet-1")("use the software or HMRC online service used to submit the return"),
                  elementTextByID("previous-calculation-bullet-2")("do it within 12 months of the Self Assessment deadline (opens in new tab)"),
                  elementTextByID("previous-calculation-contact-hmrc")("If that date has passed, or you cannot amend your return for another reason, you’ll need to contact HMRC (opens in new tab)."),
                  elementTextByID("previous-calculation-example")("For example, for the 2025 to 2026 tax year, you’ll usually need to make the change online by 31 January 2028."),
                  elementTextByID("previous-calculation-bill")("Your calculation as well as your bill will then be updated based on what you report. This may mean you have to pay more tax or that you can claim a refund.")
                )
              }

              "includes the forecast calculation tab" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)
                IncomeTaxCalculationStub.stubGetCalculationResponseWithFlagResponse(testNino, getCurrentTaxYearEnd.getYear.toString, "LATEST")(
                  status = OK,
                  body = liabilityCalculationModelSuccessfulNotCrystallised
                )
                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
                  nino = testNino,
                  from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
                  to = getCurrentTaxYearEnd.toString
                )(
                  status = OK,
                  response = Json.toJson(financialDetailsSuccess)
                )
                IncomeTaxViewChangeStub.stubGetAllObligations(
                  nino = testNino,
                  fromDate = getCurrentTaxYearEnd.minusYears(1).plusDays(1),
                  toDate = getCurrentTaxYearEnd,
                  deadlines = allObligations
                )
                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
                  nino = testNino,
                  from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
                  to = getCurrentTaxYearEnd.toString
                )(
                  status = OK,
                  response = Json.toJson(financialDetailsSuccess)
                )

                val res = buildGETMTDClient(path, additionalCookies).futureValue

                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                IncomeTaxCalculationStub.verifyGetCalculationWithFlagResponse(testNino, getCurrentTaxYearEnd.getYear.toString, "LATEST")
                IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino,
                  from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
                  to = getCurrentTaxYearEnd.toString,
                  noOffcalls = 2
                )

                val tableText = "Forecast Section Amount Income £12,500.00 Allowances and deductions £4,200.00 Total income on which tax is due £12,500.00 " +
                  "Forecast Self Assessment tax amount £5,000.99"
                val forecastTabHeader = messagesAPI("tax-year-summary.forecast")
                val forecastTotal = s"${
                  messagesAPI("tax-year-summary.forecast_total_title", (getCurrentTaxYearEnd.getYear - 1).toString,
                    getCurrentTaxYearEnd.getYear.toString)
                } £5,000.99"
                res should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "tax-year-summary.heading"),
                  elementTextBySelector("""a[href$="#forecast"]""")(forecastTabHeader),
                  elementTextBySelector(".forecast_table")(tableText)
                )
              }

              "that includes updates" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)
                IncomeTaxCalculationStub.stubGetCalculationResponseWithFlagResponse(testNino, getCurrentTaxYearEnd.getYear.toString, "LATEST")(
                  status = OK,
                  body = liabilityCalculationModelSuccessful
                )
                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
                  nino = testNino,
                  from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
                  to = getCurrentTaxYearEnd.toString
                )(
                  status = OK,
                  response = Json.toJson(financialDetailsSuccess)
                )
                IncomeTaxViewChangeStub.stubGetAllObligations(
                  nino = testNino,
                  fromDate = getCurrentTaxYearEnd.minusYears(1).plusDays(1),
                  toDate = getCurrentTaxYearEnd,
                  deadlines = allObligations
                )
                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
                  nino = testNino,
                  from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
                  to = getCurrentTaxYearEnd.toString
                )(
                  status = OK,
                  response = Json.toJson(financialDetailsSuccess)
                )

                val res = buildGETMTDClient(path, additionalCookies).futureValue

                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                IncomeTaxCalculationStub.verifyGetCalculationWithFlagResponse(testNino, getCurrentTaxYearEnd.getYear.toString, "LATEST")
                IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino,
                  from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
                  to = getCurrentTaxYearEnd.toString,
                  noOffcalls = 2
                )

                allObligations.obligations.foreach {
                  obligation => verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser(mtdUserRole), obligation.identification, obligation.obligations).detail)
                }

                res should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "tax-year-summary.heading"),
                  elementTextBySelector("#income-deductions-contributions-table tr:nth-child(1) td[class=govuk-table__cell govuk-table__cell--numeric]")("£12,500.00"),
                  elementTextBySelector("#income-deductions-contributions-table tr:nth-child(2) td[class=govuk-table__cell govuk-table__cell--numeric]")("£12,500.00"),
                  elementTextBySelectorList("#income-deductions-contributions-table", "tbody", "tr:nth-child(4)", "td:nth-of-type(1)")("£90,500.99"),
                  elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "th")(s"$overdue $poa1"),
                  elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")("23 Apr 2021"),
                  elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")("£1,000.00"),
                  elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "a")(poa1Lpi),
                  elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(1)")("24 Jun 2021"),
                  elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(2)")("£100.00"),
                  elementTextBySelectorList("#updates", "div:nth-of-type(1)", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")(quarterlyUpdate),
                  elementTextBySelectorList("#updates", "div:nth-of-type(1)", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")("business"),
                  elementTextBySelectorList("#updates", "div:nth-of-type(1)", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(3)")("5 Apr " + getCurrentTaxYearEnd.getYear.toString),
                  elementTextBySelectorList("#updates", "div:nth-of-type(1)", "table:eq(1) tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")(quarterlyUpdate),
                  elementTextBySelectorList("#updates", "div:nth-of-type(1)", "table:eq(1) tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")("business"),
                  elementTextBySelectorList("#updates", "div:nth-of-type(1)", "table:eq(1) tbody", "tr:nth-of-type(1)", "td:nth-of-type(3)")("5 Apr " + getCurrentTaxYearEnd.getYear.toString)
                )
              }


              "has payments with and without dunning locks in the payments tab" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)
                IncomeTaxCalculationStub.stubGetCalculationResponseWithFlagResponse(testNino, getCurrentTaxYearEnd.getYear.toString, "LATEST")(
                  status = OK,
                  body = liabilityCalculationModelSuccessful
                )
                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
                  nino = testNino,
                  from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
                  to = getCurrentTaxYearEnd.toString
                )(
                  status = OK,
                  response = Json.toJson(financialDetailsDunningLockSuccess)
                )
                IncomeTaxViewChangeStub.stubGetAllObligations(
                  nino = testNino,
                  fromDate = getCurrentTaxYearEnd.minusYears(1).plusDays(1),
                  toDate = getCurrentTaxYearEnd,
                  deadlines = allObligations
                )
                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
                  nino = testNino,
                  from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
                  to = getCurrentTaxYearEnd.toString
                )(
                  status = OK,
                  response = Json.toJson(financialDetailsDunningLockSuccess)
                )

                val res = buildGETMTDClient(path, additionalCookies).futureValue

                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                IncomeTaxCalculationStub.verifyGetCalculationWithFlagResponse(testNino, getCurrentTaxYearEnd.getYear.toString, "LATEST")
                IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino,
                  from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
                  to = getCurrentTaxYearEnd.toString,
                  noOffcalls = 2
                )

                allObligations.obligations.foreach {
                  obligation => verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser(mtdUserRole), obligation.identification, obligation.obligations).detail)
                }

                res should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "tax-year-summary.heading"),
                  elementTextBySelector("#income-deductions-contributions-table tr:nth-child(1) td[class=govuk-table__cell govuk-table__cell--numeric]")("£12,500.00"),
                  elementTextBySelector("#income-deductions-contributions-table tr:nth-child(2) td[class=govuk-table__cell govuk-table__cell--numeric]")("£12,500.00"),
                  elementTextBySelectorList("#income-deductions-contributions-table", "tbody", "tr:nth-child(4)", "td:nth-of-type(1)")("£90,500.99"),
                  elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "th")(s"$overdue $poa1 $underReview"),
                  elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")("23 Apr 2021"),
                  elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")("£1,000.00"),

                  elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "th")(s"$overdue $poa2"),
                  elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(1)")("23 Apr 2021"),
                  elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(2)")("£2,000.00"),

                  elementTextBySelectorList("#payments", "table", "tr:nth-of-type(3)", "th")(s"$poa1Lpi $underReview"),
                  elementTextBySelectorList("#payments", "table", "tr:nth-of-type(3)", "td:nth-of-type(1)")("24 Jun 2021"),
                  elementTextBySelectorList("#payments", "table", "tr:nth-of-type(3)", "td:nth-of-type(2)")("£100.00"),

                  elementTextBySelectorList("#updates", "div:nth-of-type(1)", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")(quarterlyUpdate),
                  elementTextBySelectorList("#updates", "div:nth-of-type(1)", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")("business"),
                  elementTextBySelectorList("#updates", "div:nth-of-type(1)", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(3)")("5 Apr " + getCurrentTaxYearEnd.getYear.toString),
                  elementTextBySelectorList("#updates", "div:nth-of-type(1)", "table:eq(1) tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")(quarterlyUpdate),
                  elementTextBySelectorList("#updates", "div:nth-of-type(1)", "table:eq(1) tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")("business"),
                  elementTextBySelectorList("#updates", "div:nth-of-type(1)", "table:eq(1) tbody", "tr:nth-of-type(1)", "td:nth-of-type(3)")("5 Apr " + getCurrentTaxYearEnd.getYear.toString),
                )

                AuditStub.verifyAuditEvent(TaxYearSummaryResponseAuditModel(testUser(mtdUserRole, singleBusinessResponse),
                  messagesAPI, TaxYearSummaryViewModel(Some(CalculationSummary(liabilityCalculationModelSuccessfulExpected)),
                    None, financialDetailsDunningLockSuccess.toChargeItem.map(TaxYearSummaryChargeItem.fromChargeItem),
                    allObligations, showForecastData = true, ctaViewModel = emptyCTAModel, LPP2Url = "", pfaEnabled = false, hasAmendments = false)))
              }


              "has Coding out that is requested and immediately rejected by NPS" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)
                IncomeTaxCalculationStub.stubGetCalculationResponseWithFlagResponse(testNino, getCurrentTaxYearEnd.getYear.toString, "LATEST")(
                  status = OK,
                  body = liabilityCalculationModelSuccessful
                )
                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
                  nino = testNino,
                  from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
                  to = getCurrentTaxYearEnd.toString
                )(
                  status = OK,
                  response = Json.toJson(immediatelyRejectedByNps)
                )
                IncomeTaxViewChangeStub.stubGetAllObligations(
                  nino = testNino,
                  fromDate = getCurrentTaxYearEnd.minusYears(1).plusDays(1),
                  toDate = getCurrentTaxYearEnd,
                  deadlines = allObligations
                )
                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
                  nino = testNino,
                  from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
                  to = getCurrentTaxYearEnd.toString
                )(
                  status = OK,
                  response = Json.toJson(immediatelyRejectedByNps)
                )

                val res = buildGETMTDClient(path, additionalCookies).futureValue

                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                IncomeTaxCalculationStub.verifyGetCalculationWithFlagResponse(testNino, getCurrentTaxYearEnd.getYear.toString, "LATEST")
                IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino,
                  from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
                  to = getCurrentTaxYearEnd.toString,
                  noOffcalls = 2
                )

                res should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "tax-year-summary.heading"),
                  elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "a")(balancingPayment),
                  elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(2)", "th")(s"$overdue $class2Nic")

                )
              }

              "has Coding out that has been accepted and rejected by NPS part way through the year" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)
                IncomeTaxCalculationStub.stubGetCalculationResponseWithFlagResponse(testNino, getCurrentTaxYearEnd.getYear.toString, "LATEST")(
                  status = OK,
                  body = liabilityCalculationModelSuccessful
                )
                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
                  nino = testNino,
                  from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
                  to = getCurrentTaxYearEnd.toString
                )(
                  status = OK,
                  response = Json.toJson(rejectedByNpsPartWay)
                )
                IncomeTaxViewChangeStub.stubGetAllObligations(
                  nino = testNino,
                  fromDate = getCurrentTaxYearEnd.minusYears(1).plusDays(1),
                  toDate = getCurrentTaxYearEnd,
                  deadlines = allObligations
                )
                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
                  nino = testNino,
                  from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
                  to = getCurrentTaxYearEnd.toString
                )(
                  status = OK,
                  response = Json.toJson(rejectedByNpsPartWay)
                )

                val res = buildGETMTDClient(path, additionalCookies).futureValue

                Then("I check all calls expected were made")
                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                IncomeTaxCalculationStub.verifyGetCalculationWithFlagResponse(testNino, getCurrentTaxYearEnd.getYear.toString, "LATEST")
                IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino,
                  from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
                  to = getCurrentTaxYearEnd.toString,
                  noOffcalls = 2
                )

                res should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "tax-year-summary.heading"),
                  elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "th")(s"$overdue $class2Nic"),
                  elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(2)", "th")(s"$overdue $cancelledPayeSA")
                )
              }

              "has expected content" when {

                "the user has the coding out requested amount has not been fully collected (partially collected)" in {
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)
                  IncomeTaxCalculationStub.stubGetCalculationResponseWithFlagResponse(testNino, getCurrentTaxYearEnd.getYear.toString, "LATEST")(
                    status = OK,
                    body = liabilityCalculationModelSuccessful
                  )
                  IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
                    nino = testNino,
                    from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
                    to = getCurrentTaxYearEnd.toString
                  )(
                    status = OK,
                    response = Json.toJson(codingOutPartiallyCollected)
                  )
                  IncomeTaxViewChangeStub.stubGetAllObligations(
                    nino = testNino,
                    fromDate = getCurrentTaxYearEnd.minusYears(1).plusDays(1),
                    toDate = getCurrentTaxYearEnd,
                    deadlines = allObligations
                  )
                  IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
                    nino = testNino,
                    from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
                    to = getCurrentTaxYearEnd.toString
                  )(
                    status = OK,
                    response = Json.toJson(codingOutPartiallyCollected)
                  )

                  val res = buildGETMTDClient(path, additionalCookies).futureValue

                  IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                  IncomeTaxCalculationStub.verifyGetCalculationWithFlagResponse(testNino, getCurrentTaxYearEnd.getYear.toString, "LATEST")
                  IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino,
                    from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
                    to = getCurrentTaxYearEnd.toString,
                    noOffcalls = 2
                  )

                  res should have(
                    httpStatus(OK),
                    pageTitle(mtdUserRole, "tax-year-summary.heading"),
                    elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "th")(s"$overdue $balancingPayment"),
                    elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(2)", "a")(class2Nic),
                    elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(3)", "a")(cancelledPayeSA)
                  )
                }

                "financial details service returns a not found" in {
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponseWoMigration)
                  IncomeTaxCalculationStub.stubGetCalculationResponseWithFlagResponse(testNino, getCurrentTaxYearEnd.getYear.toString, "LATEST")(
                    status = OK,
                    body = liabilityCalculationModelSuccessful
                  )
                  IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
                    nino = testNino,
                    from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
                    to = getCurrentTaxYearEnd.toString
                  )(
                    status = NOT_FOUND,
                    response = Json.obj()
                  )
                  IncomeTaxViewChangeStub.stubGetAllObligations(
                    nino = testNino,
                    fromDate = getCurrentTaxYearEnd.minusYears(1).plusDays(1),
                    toDate = getCurrentTaxYearEnd,
                    deadlines = allObligations
                  )
                  IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
                    nino = testNino,
                    from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
                    to = getCurrentTaxYearEnd.toString
                  )(
                    status = NOT_FOUND,
                    response = Json.obj()
                  )

                  val res = buildGETMTDClient(path, additionalCookies).futureValue

                  IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                  IncomeTaxCalculationStub.verifyGetCalculationWithFlagResponse(testNino, getCurrentTaxYearEnd.getYear.toString, "LATEST")
                  IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(
                    nino = testNino,
                    from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
                    to = getCurrentTaxYearEnd.toString,
                    noOffcalls = 2)

                  allObligations.obligations.foreach {
                    obligation => verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser(mtdUserRole), obligation.identification, obligation.obligations).detail)
                  }

                  res should have(
                    httpStatus(OK),
                    pageTitle(mtdUserRole, "tax-year-summary.heading"),
                    elementTextBySelector("#payments p")(noPaymentsDue)
                  )

                  AuditStub.verifyAuditEvent(TaxYearSummaryResponseAuditModel(
                    testUser(mtdUserRole),
                    messagesAPI,
                    TaxYearSummaryViewModel(
                      Some(CalculationSummary(liabilityCalculationModelSuccessful)),
                      None, emptyPaymentsList,
                      allObligations, showForecastData = true, ctaViewModel = emptyCTAModel, LPP2Url = "", pfaEnabled = false, hasAmendments = false
                    )))
                }
                "retrieving a calculation failed" in {
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)
                  IncomeTaxCalculationStub.stubGetCalculationErrorResponseWithFlag(testNino,
                    "2018", "LATEST")(NO_CONTENT, LiabilityCalculationError(NO_CONTENT, "error"))
                  IncomeTaxViewChangeStub.stubGetAllObligations(testNino,
                    LocalDate.of(2017, 4, 6),
                    LocalDate.of(2018, 4, 5),
                    ObligationsModel(Seq(
                      GroupedObligationsModel(
                        "ABC123456789",
                        List(SingleObligationModel(
                          LocalDate.of(2017, 12, 28),
                          LocalDate.of(2018, 2, 3),
                          LocalDate.of(2018, 2, 4),
                          "Quarterly",
                          Some(LocalDate.of(2018, 2, 2)),
                          "#001",
                          StatusFulfilled
                        ))
                      )
                    ))
                  )

                  val res = buildGETMTDClient(getPath(mtdUserRole, "2018"), additionalCookies).futureValue

                  IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                  IncomeTaxCalculationStub.verifyGetCalculationWithFlagResponse(testNino, "2018", "LATEST")

                  res should have(
                    httpStatus(OK),
                    pageTitle(mtdUserRole, "tax-year-summary.heading"),
                    elementTextByID("no-calc-data-header")(noCalcHeading),
                    elementTextByID("no-calc-data-note")(noCalcNote)
                  )
                }

                "calculation response contain error messages" in {
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)
                  IncomeTaxCalculationStub.stubGetCalculationResponseWithFlagResponse(testNino, getCurrentTaxYearEnd.getYear.toString, "LATEST")(
                    status = OK,
                    body = liabilityCalculationModelErrorMessages
                  )
                  IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
                    nino = testNino,
                    from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
                    to = getCurrentTaxYearEnd.toString
                  )(
                    status = OK,
                    response = Json.toJson(financialDetailsSuccess)
                  )
                  IncomeTaxViewChangeStub.stubGetAllObligations(
                    nino = testNino,
                    fromDate = getCurrentTaxYearEnd.minusYears(1).plusDays(1),
                    toDate = getCurrentTaxYearEnd,
                    deadlines = allObligations
                  )
                  IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
                    nino = testNino,
                    from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
                    to = getCurrentTaxYearEnd.toString
                  )(
                    status = OK,
                    response = Json.toJson(financialDetailsMFADebits)
                  )

                  val res = buildGETMTDClient(path, additionalCookies).futureValue

                  IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                  IncomeTaxCalculationStub.verifyGetCalculationWithFlagResponse(testNino, getCurrentTaxYearEnd.getYear.toString, "LATEST")
                  IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino,
                    from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
                    to = getCurrentTaxYearEnd.toString, noOffcalls = 2
                  )

                  allObligations.obligations.foreach {
                    obligation => verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser(mtdUserRole), obligation.identification, obligation.obligations).detail)
                  }

                  val errMessages = liabilityCalculationModelErrorMessagesFormatted(mtdUserRole).messages.get.errorMessages

                  res should have(
                    httpStatus(OK),
                    pageTitle(mtdUserRole, "tax-year-summary.heading"),
                    elementTextBySelector("dl")(""),
                    elementTextBySelector("#forecast_total")(""),
                    elementTextBySelector("#calculation-date")(""),
                    elementTextBySelector("""a[href$="#forecast"]""")(""),
                    elementTextBySelector(".forecast_table")(""),
                    elementTextBySelectorList("#taxCalculation", "div h2")(messagesAPI("tax-year-summary.message.header")),
                    elementTextBySelectorList("#taxCalculation", "div strong")("Warning " + messagesAPI(s"tax-year-summary${if(mtdUserRole == MTDIndividual) "" else ".agent"}.message.action")),
                    elementTextBySelectorList("#taxCalculation", "ul > li:nth-child(1)")(errMessages.head.text),
                    elementTextBySelectorList("#taxCalculation", "ul > li:nth-child(2)")(errMessages(1).text),
                    elementTextBySelectorList("#taxCalculation", "ul > li:nth-child(3)")(errMessages(2).text),
                    elementTextBySelectorList("#taxCalculation", "ul > li:nth-child(4)")(errMessages(3).text),
                  )
                }
              }

              "MFA Debits on the Payment Tab with FS ENABLED" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)
                IncomeTaxCalculationStub.stubGetCalculationResponseWithFlagResponse(testNino, getCurrentTaxYearEnd.getYear.toString, "LATEST")(
                  status = OK,
                  body = liabilityCalculationModelSuccessful
                )
                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
                  nino = testNino,
                  from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
                  to = getCurrentTaxYearEnd.toString
                )(
                  status = OK,
                  response = Json.toJson(financialDetailsMFADebits)
                )
                IncomeTaxViewChangeStub.stubGetAllObligations(
                  nino = testNino,
                  fromDate = getCurrentTaxYearEnd.minusYears(1).plusDays(1),
                  toDate = getCurrentTaxYearEnd,
                  deadlines = allObligations
                )
                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
                  nino = testNino,
                  from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
                  to = getCurrentTaxYearEnd.toString
                )(
                  status = OK,
                  response = Json.toJson(financialDetailsMFADebits)
                )

                val result = buildGETMTDClient(path, additionalCookies).futureValue

                val auditDD = financialDetailsMFADebits.getAllDocumentDetailsWithDueDates()
                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                IncomeTaxCalculationStub.verifyGetCalculationWithFlagResponse(testNino, getCurrentTaxYearEnd.getYear.toString, "LATEST")
                IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino,
                  from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
                  to = getCurrentTaxYearEnd.toString, noOffcalls = 2
                )
                verifyAuditEvent(TaxYearSummaryResponseAuditModel(
                  testUser(mtdUserRole, singleBusinessResponse),
                  messagesAPI, TaxYearSummaryViewModel(
                    Some(CalculationSummary(liabilityCalculationModelSuccessful)),
                    None, auditDD.map(dd => ChargeItem.fromDocumentPair(dd.documentDetail, financialDetailsMFADebits.financialDetails)).map(TaxYearSummaryChargeItem.fromChargeItem), allObligations,
                    showForecastData = true, ctaViewModel = emptyCTAModel, LPP2Url = "", pfaEnabled = false, hasAmendments = false)))

                allObligations.obligations.foreach {
                  obligation => verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser(mtdUserRole), obligation.identification, obligation.obligations).detail)
                }
                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "tax-year-summary.heading"),
                  elementTextBySelectorList("#payments-table", "tbody", "tr:nth-of-type(1)", "th")(s"$hmrcAdjustment"),
                  elementTextBySelectorList("#payments-table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")("22 Apr 2021"),
                  elementTextBySelectorList("#payments-table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")("£2,234.00"),
                  elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "th")(s"$hmrcAdjustment"),
                  elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(1)")("23 Apr 2021"),
                  elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(2)")("£1,234.00"),
                  elementCountBySelector("#payments-table", "tbody", "tr")(2)
                )
              }

              "that has the charges table" when {
                "the user has Review and Reconcile debit charges" in {
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)
                  IncomeTaxCalculationStub.stubGetCalculationResponseWithFlagResponse(testNino, getCurrentTaxYearEnd.getYear.toString, "LATEST")(status = OK, body = liabilityCalculationModelDeductionsMinimal)
                  CalculationListStub.stubGetCalculationList(testNino, "22-23")(successResponseNonCrystallised.toString)
                  IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testYear2023 - 1}-04-06", s"$testYear2023-04-05")(OK,
                    testValidFinancialDetailsModelReviewAndReconcileDebitsJson(2000, 2000, testYear2023.toString, futureDate.toString))

                  val res = buildGETMTDClient(path, additionalCookies).futureValue

                  val document = Jsoup.parse(res.body)
                  def getChargeSummaryUrl(id: String) = if(mtdUserRole == MTDIndividual) {
                    controllers.routes.ChargeSummaryController.show(testYear2023, id).url
                  } else {
                    controllers.routes.ChargeSummaryController.showAgent(testYear2023, id).url
                  }
                  document.getElementById("paymentTypeLink-0").attr("href") shouldBe getChargeSummaryUrl("1040000123")
                  document.getElementById("paymentTypeLink-1").attr("href") shouldBe getChargeSummaryUrl("1040000124")

                  res should have(
                    httpStatus(OK),
                    pageTitle(mtdUserRole, "tax-year-summary.heading"),
                    elementTextByID("paymentTypeLink-0")("First payment on account: extra amount from your tax return"),
                    elementTextByID("paymentTypeLink-1")("Second payment on account: extra amount from your tax return"),
                    isElementVisibleById("accrues-interest-tag")(expectedValue = true))
                }
              }

              "adjust POA link visible" when {
                "The user has amendable POAs for the given tax year" in {
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)
                  IncomeTaxCalculationStub.stubGetCalculationResponseWithFlagResponse(testNino, getCurrentTaxYearEnd.getYear.toString, "LATEST")(status = OK, body = liabilityCalculationModelDeductionsMinimal)
                  CalculationListStub.stubGetCalculationList(testNino, "22-23")(successResponseNonCrystallised.toString)
                  IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testYear2023 - 1}-04-06", s"$testYear2023-04-05")(OK,
                    testValidFinancialDetailsModelJson(2000, 2000, testYear2023.toString, testDate.toString))

                  val res = buildGETMTDClient(path, additionalCookies).futureValue

                  res should have(
                    httpStatus(OK),
                    pageTitle(mtdUserRole, "tax-year-summary.heading"),
                    isElementVisibleById("adjust-poa-link")(expectedValue = true))
                }
              }
              "does not have adjustable POA link visible" when {
                "The user has no amendable POAs" in {
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)
                  IncomeTaxCalculationStub.stubGetCalculationResponseWithFlagResponse(testNino, getCurrentTaxYearEnd.getYear.toString, "LATEST")(status = OK, body = liabilityCalculationModelDeductionsMinimal)
                  CalculationListStub.stubGetCalculationList(testNino, "22-23")(successResponseNonCrystallised.toString)
                  IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testYear2023 - 1}-04-06", s"$testYear2023-04-05")(OK, testEmptyFinancialDetailsModelJson)

                  val res = buildGETMTDClient(path, additionalCookies).futureValue

                  res should have(
                    httpStatus(OK),
                    pageTitle(mtdUserRole, "tax-year-summary.heading"),
                    isElementVisibleById("adjust-poa-link")(expectedValue = false))
                }
              }
            }

            "render the error page" when {
              "financial details service returns an error" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)
                IncomeTaxCalculationStub.stubGetCalculationResponseWithFlagResponse(testNino, testYear, "LATEST")(
                  status = OK,
                  body = liabilityCalculationModelSuccessful
                )
                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(INTERNAL_SERVER_ERROR, testFinancialDetailsErrorModelJson())

                val res = buildGETMTDClient(getPath(mtdUserRole, testYear), additionalCookies).futureValue

                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino)

                res should have(
                  httpStatus(INTERNAL_SERVER_ERROR)
                )
              }

              "retrieving a calculation failed with INTERNAL_SERVER_ERROR" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)
                IncomeTaxCalculationStub.stubGetCalculationErrorResponseWithFlag(testNino,
                  "2018", "LATEST")(INTERNAL_SERVER_ERROR, LiabilityCalculationError(INTERNAL_SERVER_ERROR, "error"))
                IncomeTaxViewChangeStub.stubGetAllObligations(testNino,
                  LocalDate.of(2017, 4, 6),
                  LocalDate.of(2018, 4, 5),
                  ObligationsModel(Seq(
                    GroupedObligationsModel(
                      "ABC123456789",
                      List(SingleObligationModel(
                        LocalDate.of(2017, 12, 28),
                        LocalDate.of(2018, 2, 3),
                        LocalDate.of(2018, 2, 4),
                        "Quarterly",
                        Some(LocalDate.of(2018, 2, 2)),
                        "#001",
                        StatusFulfilled
                      ))
                    )
                  ))
                )

                val res = buildGETMTDClient(getPath(mtdUserRole, year = testYear), additionalCookies).futureValue

                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                IncomeTaxCalculationStub.verifyGetCalculationWithFlagResponse(testNino, "2018", "LATEST")

                res should have(
                  httpStatus(INTERNAL_SERVER_ERROR)
                )
              }

              "retrieving a getAllObligations error" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)
                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
                  nino = testNino,
                  from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
                  to = getCurrentTaxYearEnd.toString
                )(
                  status = OK,
                  response = Json.toJson(codingOutPartiallyCollected)
                )
                IncomeTaxViewChangeStub.stubGetAllObligationsError(testNino,
                  LocalDate.of(2017, 4, 6),
                  LocalDate.of(2018, 4, 5))

                val res = buildGETMTDClient(getPath(mtdUserRole, year = testYear), additionalCookies).futureValue

                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, LocalDate.of(2017, 4, 6).toString,
                  LocalDate.of(2018, 4, 5).toString)

                res should have(
                  httpStatus(INTERNAL_SERVER_ERROR)
                )
              }

              "retrieving a calculation failed" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)
                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
                  nino = testNino,
                  from = LocalDate.of(2017, 4, 6).toString,
                  to = LocalDate.of(2018, 4, 5).toString
                )(
                  status = OK,
                  response = Json.toJson(codingOutPartiallyCollected)
                )
                IncomeTaxViewChangeStub.stubGetAllObligations(
                  nino = testNino,
                  fromDate = LocalDate.of(2017, 4, 6),
                  toDate = LocalDate.of(2018, 4, 5),
                  deadlines = allObligations
                )
                IncomeTaxCalculationStub.stubGetCalculationErrorResponseWithFlag(testNino, "2018", "LATEST")(INTERNAL_SERVER_ERROR, LiabilityCalculationError(INTERNAL_SERVER_ERROR, "Error"))

                val res = buildGETMTDClient(getPath(mtdUserRole, testYear), additionalCookies).futureValue

                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                IncomeTaxViewChangeStub.verifyGetAllObligations(testNino,
                  LocalDate.of(2017, 4, 6), LocalDate.of(2018, 4, 5))
                IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino)

                res should have(
                  httpStatus(INTERNAL_SERVER_ERROR)
                )
              }

              "retrieving a financial transaction failed" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)
                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(INTERNAL_SERVER_ERROR, testFinancialDetailsErrorModelJson())

                val res = buildGETMTDClient(getPath(mtdUserRole, testYear), additionalCookies).futureValue

                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino)

                res should have(
                  httpStatus(INTERNAL_SERVER_ERROR)
                )
              }
            }
          }
        }
        testAuthFailures(path, mtdUserRole)
      }
    }
  }
}
