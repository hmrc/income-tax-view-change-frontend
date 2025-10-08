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
import helpers.servicemocks.{IncomeTaxCalculationStub, IncomeTaxViewChangeStub, MTDIndividualAuthStub}
import models.liabilitycalculation.LiabilityCalculationError
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK, SEE_OTHER}
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino, testYear}
import testConstants.IncomeSourceIntegrationTestConstants.multipleBusinessesAndPropertyResponseWoMigration
import testConstants.NewCalcBreakdownItTestConstants.liabilityCalculationModelSuccessful

import java.util.Locale

class FinalTaxCalculationControllerISpec extends ControllerISpecHelper {

  val (taxYear, month, dayOfMonth) = (2018, 5, 6)
  val (hour, minute) = (12, 0)
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())
  lazy val mcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]

  def toMessages(language: String): Messages = {
    mcc.messagesApi.preferred(Seq(
      new Lang(new Locale(language))
    ))
  }

  def calculationStub(): Unit = {
    IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, "2018")(
      status = OK,
      body = liabilityCalculationModelSuccessful
    )
  }

  def calculationStubEmptyCalculations(): Unit = {
    IncomeTaxCalculationStub.stubGetCalculationErrorResponse(testNino, "2018")(
      status = NOT_FOUND,
      body = LiabilityCalculationError(NOT_FOUND, "not found")
    )
  }

  object Selectors {
    val caption = "#main-content > div > div > div > header > p"
    val title = "h1"

    val insetText = "#main-content > div > div > div > p.govuk-inset-text"
    val insetLinkText = "#main-content > div > div > div > p > a"

    val incomeRowText = "#calculation-income-deductions-contributions-table > tbody > tr:nth-child(1) > th > a"
    val incomeRowAmount = "#calculation-income-deductions-contributions-table > tbody > tr:nth-child(1) > td"

    val allowanceRowText = "#calculation-income-deductions-contributions-table > tbody > tr:nth-child(2) > th > a"
    val allowanceRowAmount = "#calculation-income-deductions-contributions-table > tbody > tr:nth-child(2) > td"

    val taxIsDueRowText = "#calculation-income-deductions-contributions-table > tbody > tr:nth-child(3) > th"
    val taxIsDueRowAmount = "#calculation-income-deductions-contributions-table > tbody > tr:nth-child(3) > td"

    val contributionDueRowText = "#calculation-income-deductions-contributions-table > tbody > tr:nth-child(4) > th > a"
    val contributionDueRowAmount = "#calculation-income-deductions-contributions-table > tbody > tr:nth-child(4) > td"

    val chargeInformationParagraph = "#main-content > div > div > div > p.govuk-body"

    val continueButton = "#continue-button"
  }

  object ExpectedValues {
    val caption = "6 April 2017 to 5 April 2018"

    val yourOrYourClients: Boolean => String = isAgent => if(isAgent) {
      "your client’s"
    } else "your"

    val pathStart: Boolean => String = isAgent =>
      "/report-quarterly/income-and-expenses/view" + {if(isAgent) "/agents" else ""}

    val insetTextFull: Boolean => String = isAgent =>
      s"If you think this information is incorrect, you can check ${yourOrYourClients(isAgent)} Income Tax Return."
    val insetTextLink: Boolean => String = isAgent => s"check ${yourOrYourClients(isAgent)} Income Tax Return."
    val insetLinkHref = "http://localhost:9302/update-and-submit-income-tax-return/2018/view"

    val incomeText = "Income"
    val incomeAmount = "£12,500.00"
    val incomeLink: Boolean => String = isAgent => pathStart(isAgent) + "/2018/income"

    val allowanceText = "Allowances and deductions"
    val allowanceAmount = "£12,500.00"
    val allowanceLink: Boolean => String = isAgent => pathStart(isAgent) + "/2018/allowances-and-deductions"

    val taxIsDueText = "Total taxable income"
    val taxIsDueAmount = "£12,500.00"

    val contributionText = "Income Tax and National Insurance contributions"
    val contributionAmount = "£90,500.99"
    val contributionLink: Boolean => String = isAgent => pathStart(isAgent) + "/2018/tax-calculation"

    val chargeInformationParagraph: Boolean => String = isAgent =>
      if(isAgent) {
        "The amount your client needs to pay might be different if there are other charges or payments on their account, for example, late payment interest."
      } else {
        "The amount you need to pay might be different if there are other charges or payments on your account, for example, late payment interest."
      }
    val continueButtonText = "Continue"
  }

  object ExpectedValuesWelsh {
    val caption = "6 Ebrill 2017 i 5 Ebrill 2018"

    val insetTextFull: Boolean => String = isAgent =>
      if(isAgent){
        "Os ydych o’r farn bod yr wybodaeth hon yn anghywir gallwch wirio Ffurflen Dreth Incwm eich cleient."
      } else {
        "Os ydych o’r farn bod yr wybodaeth hon yn anghywir gallwch wirio eich Ffurflen Dreth Incwm."
      }
    val insetTextLink: Boolean => String = isAgent =>
      if(isAgent){
        "wirio Ffurflen Dreth Incwm eich cleient."
      } else {
        "wirio eich Ffurflen Dreth Incwm."
      }

    val incomeText = "Incwm"

    val allowanceText = "Lwfansau a didyniadau"

    val taxIsDueText = "Cyfanswm eich incwm trethadwy"

    val contributionText = "Treth Incwm a chyfraniadau Yswiriant Gwladol"

    val chargeInformationParagraph: Boolean => String = isAgent =>
      if(isAgent){
        "Gall y swm y mae angen i’ch cleient ei dalu fod yn wahanol os oes taliadau neu ffioedd eraill ar ei gyfrif, er enghraifft, llog taliad hwyr."
      } else {
        "Gall y swm y mae angen i chi ei dalu fod yn wahanol os oes taliadau neu ffioedd eraill ar eich cyfrif, er enghraifft, llog taliad hwyr."
      }

    val continueButtonText = "Yn eich blaen"
  }

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    pathStart + s"/$testYear/final-tax-overview"
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
            "render the final tax calculation page" that {
              "is in English" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponseWoMigration)
                calculationStub()

                val res = buildGETMTDClient(path, additionalCookies).futureValue

                res should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, s"final-tax-overview${if(isAgent) ".agent" else ".individual"}.heading"),
                  elementTextBySelector(Selectors.caption)(ExpectedValues.caption),
                  elementTextBySelector(Selectors.insetText)(ExpectedValues.insetTextFull(isAgent)),
                  elementTextBySelector(Selectors.insetLinkText)(ExpectedValues.insetTextLink(isAgent)),
                  elementAttributeBySelector(Selectors.insetLinkText, "href")(ExpectedValues.insetLinkHref),
                  elementTextBySelector(Selectors.incomeRowText)(ExpectedValues.incomeText),
                  elementTextBySelector(Selectors.incomeRowAmount)(ExpectedValues.incomeAmount),
                  elementAttributeBySelector(Selectors.incomeRowText, "href")(ExpectedValues.incomeLink(isAgent)),
                  elementTextBySelector(Selectors.allowanceRowText)(ExpectedValues.allowanceText),
                  elementTextBySelector(Selectors.allowanceRowAmount)(ExpectedValues.allowanceAmount),
                  elementAttributeBySelector(Selectors.allowanceRowText, "href")(ExpectedValues.allowanceLink(isAgent)),
                  elementTextBySelector(Selectors.taxIsDueRowText)(ExpectedValues.taxIsDueText),
                  elementTextBySelector(Selectors.taxIsDueRowAmount)(ExpectedValues.taxIsDueAmount),
                  elementTextBySelector(Selectors.contributionDueRowText)(ExpectedValues.contributionText),
                  elementTextBySelector(Selectors.contributionDueRowAmount)(ExpectedValues.contributionAmount),
                  elementAttributeBySelector(Selectors.contributionDueRowText, "href")(ExpectedValues.contributionLink(isAgent)),
                  elementTextBySelector(Selectors.chargeInformationParagraph)(ExpectedValues.chargeInformationParagraph(isAgent)),
                  elementTextBySelector(Selectors.continueButton)(ExpectedValues.continueButtonText)
                )
              }

              "is in welsh" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponseWoMigration)
                calculationStub()

                val res = buildGETMTDClient(path, additionalCookies, true).futureValue

                res should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, s"final-tax-overview${if(isAgent) ".agent" else ".individual"}.heading")(implicitly, Lang("cy")),
                  elementTextBySelector(Selectors.caption)(ExpectedValuesWelsh.caption),
                  elementTextBySelector(Selectors.insetText)(ExpectedValuesWelsh.insetTextFull(isAgent)),
                  elementTextBySelector(Selectors.insetLinkText)(ExpectedValuesWelsh.insetTextLink(isAgent)),
                  elementAttributeBySelector(Selectors.insetLinkText, "href")(ExpectedValues.insetLinkHref),
                  elementTextBySelector(Selectors.incomeRowText)(ExpectedValuesWelsh.incomeText),
                  elementTextBySelector(Selectors.incomeRowAmount)(ExpectedValues.incomeAmount),
                  elementAttributeBySelector(Selectors.incomeRowText, "href")(ExpectedValues.incomeLink(isAgent)),
                  elementTextBySelector(Selectors.allowanceRowText)(ExpectedValuesWelsh.allowanceText),
                  elementTextBySelector(Selectors.allowanceRowAmount)(ExpectedValues.allowanceAmount),
                  elementAttributeBySelector(Selectors.allowanceRowText, "href")(ExpectedValues.allowanceLink(isAgent)),
                  elementTextBySelector(Selectors.taxIsDueRowText)(ExpectedValuesWelsh.taxIsDueText),
                  elementTextBySelector(Selectors.taxIsDueRowAmount)(ExpectedValues.taxIsDueAmount),
                  elementTextBySelector(Selectors.contributionDueRowText)(ExpectedValuesWelsh.contributionText),
                  elementTextBySelector(Selectors.contributionDueRowAmount)(ExpectedValues.contributionAmount),
                  elementAttributeBySelector(Selectors.contributionDueRowText, "href")(ExpectedValues.contributionLink(isAgent)),
                  elementTextBySelector(Selectors.chargeInformationParagraph)(ExpectedValuesWelsh.chargeInformationParagraph(isAgent)),
                  elementTextBySelector(Selectors.continueButton)(ExpectedValuesWelsh.continueButtonText)
                )
              }
            }

            "render the error page" when {
              "there is no calc data model" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponseWoMigration)
                calculationStubEmptyCalculations()

                val res = buildGETMTDClient(path, additionalCookies).futureValue
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

    s"POST $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          if (mtdUserRole == MTDSupportingAgent) {
            testSupportingAgentAccessDenied(path, additionalCookies)
          } else {
            "redirect to the confirmation page" in {
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponseWoMigration)
              calculationStub()

              val res = buildPOSTMTDPostClient(path, additionalCookies, Map.empty).futureValue

              res should have(
                httpStatus(SEE_OTHER),
                redirectURI("http://localhost:9302/update-and-submit-income-tax-return/2018/declaration")
              )
            }

            "render the error page" when {
              if (mtdUserRole == MTDIndividual) {
                "there is no name provided in the auth" in {
                  MTDIndividualAuthStub.stubAuthorisedWithNoName()
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponseWoMigration)
                  calculationStub()

                  val res = buildPOSTMTDPostClient(path, additionalCookies, Map.empty).futureValue
                  res.status shouldBe INTERNAL_SERVER_ERROR
                }
              }

              "there is no calc information" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponseWoMigration)
                calculationStubEmptyCalculations()

                val res = buildPOSTMTDPostClient(path, additionalCookies, Map.empty).futureValue
                res.status shouldBe INTERNAL_SERVER_ERROR
              }
            }
          }
        }
        testAuthFailures(path, mtdUserRole, Some(Map.empty))
      }
    }
  }
}
