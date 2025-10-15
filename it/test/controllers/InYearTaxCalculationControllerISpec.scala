/*
 * Copyright 2021 HM Revenue & Customs
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

import enums.{MTDIndividual, MTDSupportingAgent, MTDUserRole}
import helpers.servicemocks.{AuditStub, IncomeTaxCalculationStub, IncomeTaxViewChangeStub}
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import models.liabilitycalculation.LiabilityCalculationError
import models.liabilitycalculation.viewmodels.CalculationSummary.toTaxYearEndDate
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.i18n.{Lang, Messages}
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino}
import testConstants.IncomeSourceIntegrationTestConstants.{multipleBusinessesAndPropertyResponse, multipleBusinessesAndPropertyResponseWoMigration}
import testConstants.NewCalcBreakdownItTestConstants.liabilityCalculationModelSuccessful

import java.time.LocalDate
import java.util.Locale

class InYearTaxCalculationControllerISpec extends ControllerISpecHelper {

  val currentDate = LocalDate.of(2023, 4, 5)
  val implicitDateFormatter: ImplicitDateFormatter = app.injector.instanceOf[ImplicitDateFormatterImpl]
  import implicitDateFormatter.longDate

  def toMessages(language: String): Messages = {
    messagesAPI.preferred(Seq(
      new Lang(new Locale(language))
    ))
  }

  val (taxYear, month, dayOfMonth) = (if (currentDate.isAfter(toTaxYearEndDate(currentDate.getYear))) {
    currentDate.getYear + 1
  }
  else currentDate.getYear, currentDate.getMonthValue, currentDate.getDayOfMonth)
  val taxYearString = taxYear.toString
  val previousTaxYearString = (taxYear - 1).toString
  val timeStampEN: String = longDate(currentDate)(toMessages("EN")).toLongDate
  val timeStampCY: String = longDate(currentDate)(toMessages("CY")).toLongDate

  def calculationStub(): Unit = {
    IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, taxYear.toString)(
      status = OK,
      body = liabilityCalculationModelSuccessful
    )
  }

  def calculationStubEmptyCalculations(): Unit = {
    IncomeTaxCalculationStub.stubGetCalculationErrorResponse(testNino, taxYear.toString)(
      status = NOT_FOUND,
      body = LiabilityCalculationError(NOT_FOUND, "not found")
    )
  }

  def calculationStubFailCalculations(): Unit = {
    IncomeTaxCalculationStub.stubGetCalculationErrorResponse(testNino, taxYear.toString)(
      status = BAD_REQUEST,
      body = LiabilityCalculationError(NOT_FOUND, "not found")
    )
  }

  object Selectors {
    val caption = "#main-content > div > div > div > header > p"
    val insetText = "#main-content > div > div > div > p"
    val incomeRowText = "#calculation-income-deductions-contributions-table > tbody > tr:nth-child(1) > th:nth-child(1) > div > a"
    val incomeRowAmount = "#calculation-income-deductions-contributions-table > tbody > tr:nth-child(1) > td.govuk-table__cell.govuk-table__cell--numeric"
    val allowanceRowText = "#calculation-income-deductions-contributions-table > tbody > tr:nth-child(2) > th:nth-child(1) > div > a"
    val allowanceRowAmount = "#calculation-income-deductions-contributions-table > tbody > tr:nth-child(2) > td.govuk-table__cell.govuk-table__cell--numeric"
    val taxIsDueRowText = "#calculation-income-deductions-contributions-table > tbody > tr:nth-child(3) > th:nth-child(1)"
    val taxIsDueRowAmount = "#calculation-income-deductions-contributions-table > tbody > tr:nth-child(3) > td.govuk-table__cell.govuk-table__cell--numeric"
    val continueButton = "#continue-button"
  }

  object ExpectedValues {
    val pathStart: Boolean => String = isAgent =>
      "/report-quarterly/income-and-expenses/view" + {if(isAgent) "/agents" else ""}
    val title: Boolean => String = isAgent =>
      if(isAgent) {
        s"Your client’s tax overview 6 April $previousTaxYearString to 5 April $taxYearString - Manage your Self Assessment - GOV.UK"
      } else {
        s"Your tax overview 6 April $previousTaxYearString to 5 April $taxYearString - Manage your Self Assessment - GOV.UK"
      }
    val caption = s"6 April $previousTaxYearString to 5 April $taxYearString"
    val insetTextFull: Boolean => String = isAgent =>
      if(isAgent) {
        s"This calculation is only based on your client’s completed updates for this tax year up to $timeStampEN. It is not their final tax bill for the year. It is a year to date calculation based on the information that has been entered so far."
      } else {
        s"This calculation is only based on your completed updates for this tax year up to $timeStampEN. It is not your final tax bill for the year. It is a year to date calculation based on the information that has been entered so far."
      }
    val tableTitle = "Calculation"
    val incomeText = "Income"
    val incomeAmount = "£12,500.00"
    val incomeLink: Boolean => String = isAgent => pathStart(isAgent) + s"/$taxYearString/income"
    val allowanceText = "Allowances and deductions"
    val allowanceAmount = "£12,500.00"
    val allowanceLink: Boolean => String = isAgent => pathStart(isAgent) + s"/$taxYearString/allowances-and-deductions"
    val taxableIncome = "Total income on which tax is due"
    val taxableIncomeAmount = "£12,500.00"
    val taxIsDueText = "Total income on which tax is due"
    val taxIsDueAmount = "£12,500.00"
    val continueButtonText = "Go to Income Tax Account"
  }

  object ExpectedValuesWelsh {
    val pathStart: Boolean => String = isAgent =>
      "/report-quarterly/income-and-expenses/view" + {if(isAgent) "/agents" else ""}
    val title: Boolean => String = isAgent =>
      if(isAgent) {
        s"Trosolwg o dreth eich cleient 6 Ebrill $previousTaxYearString i 5 Ebrill $taxYearString - Rheoli’ch Hunanasesiad - GOV.UK"
      } else {
        s"Trosolwg o’ch treth 6 Ebrill $previousTaxYearString i 5 Ebrill $taxYearString - Rheoli’ch Hunanasesiad - GOV.UK"
      }
    val caption = s"6 Ebrill $previousTaxYearString i 5 Ebrill $taxYearString"
    val insetTextFull: Boolean => String = isAgent =>
      if(isAgent) {
        s"Mae’r cyfrifiad hwn yn seiliedig ar ddiweddariadau gorffenedig eich cleient ar gyfer y flwyddyn dreth hon hyd at $timeStampCY yn unig. Nid dyma ei fil treth terfynol ar gyfer y flwyddyn. Cyfrifiad o’r flwyddyn hyd yma yw hwn ar sail yr wybodaeth sydd wedi cael ei nodi hyd yma."
      } else {
        s"Mae’r cyfrifiad hwn yn seiliedig ar eich diweddariadau gorffenedig ar gyfer y flwyddyn dreth hon hyd at $timeStampCY yn unig. Nid dyma’ch bil treth terfynol ar gyfer y flwyddyn. Cyfrifiad o’r flwyddyn hyd yma yw hwn ar sail yr wybodaeth sydd wedi cael ei nodi hyd yma."
      }
    val tableTitle = "Cyfrifiad"
    val incomeText = "Incwm"
    val incomeAmount = "£12,500.00"
    val incomeLink: Boolean => String = isAgent => pathStart(isAgent) + s"/$taxYearString/income"
    val allowanceText = "Lwfansau a didyniadau"
    val allowanceAmount = "£12,500.00"
    val allowanceLink: Boolean => String = isAgent => pathStart(isAgent) + s"/$taxYearString/allowances-and-deductions"
    val taxIsDueText = "Cyfanswm yr incwm y mae treth yn ddyledus arno"
    val taxIsDueAmount = "£12,500.00"
    val continueButtonText = "mynd i gyfrifo treth"
  }

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    pathStart + s"/tax-overview"
  }


  mtdAllRoles.foreach { case mtdUserRole =>
    val isAgent = mtdUserRole != MTDIndividual
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          if (mtdUserRole == MTDSupportingAgent) {
            testSupportingAgentAccessDenied(path, additionalCookies)
          } else {
            "render the new calc page" that {
              "is in english" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponseWoMigration)
                calculationStub()

                val res = buildGETMTDClient(path, additionalCookies).futureValue
                AuditStub.verifyAudit()

                res should have(
                  httpStatus(OK),
                  pageTitleCustom(ExpectedValues.title(isAgent)),
                  elementTextBySelector(Selectors.caption)(ExpectedValues.caption),
                  elementTextBySelector(Selectors.insetText)(ExpectedValues.insetTextFull(isAgent)),
                  elementTextBySelector(Selectors.incomeRowText)(ExpectedValues.incomeText),
                  elementTextBySelector(Selectors.incomeRowAmount)(ExpectedValues.incomeAmount),
                  elementAttributeBySelector(Selectors.incomeRowText, "href")(ExpectedValues.incomeLink(isAgent)),
                  elementTextBySelector(Selectors.allowanceRowText)(ExpectedValues.allowanceText),
                  elementTextBySelector(Selectors.allowanceRowAmount)(ExpectedValues.allowanceAmount),
                  elementAttributeBySelector(Selectors.allowanceRowText, "href")(ExpectedValues.allowanceLink(isAgent)),
                  elementTextBySelector(Selectors.taxIsDueRowText)(ExpectedValues.taxIsDueText),
                  elementTextBySelector(Selectors.taxIsDueRowAmount)(ExpectedValues.taxIsDueAmount),
                  elementTextBySelector(Selectors.continueButton)(ExpectedValues.continueButtonText)
                )
              }

              "is in welsh" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponseWoMigration)
                calculationStub()

                val res = buildGETMTDClient(path, additionalCookies, true).futureValue
                AuditStub.verifyAudit()

                res should have(
                  httpStatus(OK),
                  pageTitleCustom(ExpectedValuesWelsh.title(isAgent)),
                  elementTextBySelector(Selectors.caption)(ExpectedValuesWelsh.caption),
                  elementTextBySelector(Selectors.insetText)(ExpectedValuesWelsh.insetTextFull(isAgent)),
                  elementTextBySelector(Selectors.incomeRowText)(ExpectedValuesWelsh.incomeText),
                  elementTextBySelector(Selectors.incomeRowAmount)(ExpectedValuesWelsh.incomeAmount),
                  elementAttributeBySelector(Selectors.incomeRowText, "href")(ExpectedValuesWelsh.incomeLink(isAgent)),
                  elementTextBySelector(Selectors.allowanceRowText)(ExpectedValuesWelsh.allowanceText),
                  elementTextBySelector(Selectors.allowanceRowAmount)(ExpectedValuesWelsh.allowanceAmount),
                  elementAttributeBySelector(Selectors.allowanceRowText, "href")(ExpectedValuesWelsh.allowanceLink(isAgent)),
                  elementTextBySelector(Selectors.taxIsDueRowText)(ExpectedValuesWelsh.taxIsDueText),
                  elementTextBySelector(Selectors.taxIsDueRowAmount)(ExpectedValuesWelsh.taxIsDueAmount),
                  elementTextBySelector(Selectors.continueButton)(ExpectedValuesWelsh.continueButtonText)
                )
              }
            }


            "render the error page" when {
              "there is no calc data model" in {
                stubAuthorised(mtdUserRole)
                calculationStubEmptyCalculations()
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result.status shouldBe INTERNAL_SERVER_ERROR
              }

              "there is a different status to NOT_FOUND" in {
                stubAuthorised(mtdUserRole)
                calculationStubFailCalculations()
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result.status shouldBe INTERNAL_SERVER_ERROR
              }
            }
          }
        }
        testAuthFailures(path, mtdUserRole)
      }
    }
  }
}
