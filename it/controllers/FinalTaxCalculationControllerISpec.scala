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

import helpers.ComponentSpecBase
import helpers.servicemocks.{AuthStub, IncomeTaxCalculationStub, IncomeTaxViewChangeStub}
import models.liabilitycalculation.LiabilityCalculationError
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino}
import testConstants.IncomeSourceIntegrationTestConstants.{multipleBusinessesAndPropertyResponse, multipleBusinessesAndPropertyResponseWoMigration}
import testConstants.NewCalcBreakdownItTestConstants.liabilityCalculationModelSuccessFull

class FinalTaxCalculationControllerISpec extends ComponentSpecBase {

  val (taxYear, month, dayOfMonth) = (2018, 5, 6)
  val (hour, minute) = (12, 0)
  val url: String = s"http://localhost:$port" + controllers.routes.FinalTaxCalculationController.show(taxYear).url

  def calculationStub(): Unit = {
    IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, "2018")(
      status = OK,
      body = liabilityCalculationModelSuccessFull
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

    val incomeRowText = "#income-deductions-contributions-table > tbody > tr:nth-child(1) > th > a"
    val incomeRowAmount = "#income-deductions-contributions-table > tbody > tr:nth-child(1) > td"

    val allowanceRowText = "#income-deductions-contributions-table > tbody > tr:nth-child(2) > th > a"
    val allowanceRowAmount = "#income-deductions-contributions-table > tbody > tr:nth-child(2) > td"

    val taxIsDueRowText = "#income-deductions-contributions-table > tbody > tr:nth-child(3) > th"
    val taxIsDueRowAmount = "#income-deductions-contributions-table > tbody > tr:nth-child(3) > td"

    val contributionDueRowText = "#income-deductions-contributions-table > tbody > tr:nth-child(4) > th > a"
    val contributionDueRowAmount = "#income-deductions-contributions-table > tbody > tr:nth-child(4) > td"

    val chargeInformationParagraph = "#main-content > div > div > div > p.govuk-body"

    val continueButton = "#continue-button"
  }

  object ExpectedValues {
    val title = "Your final tax overview - Update and submit an Income Tax Return - GOV.UK"
    val caption = "6 April 2017 to 5 April 2018"

    val insetTextFull = "If you think this information is incorrect, you can check your Income Tax Return."
    val insetTextLink = "check your Income Tax Return."
    val insetLinkHref = "http://localhost:9302/update-and-submit-income-tax-return/2018/view"

    val incomeText = "Income"
    val incomeAmount = "£12,500.00"
    val incomeLink = "/report-quarterly/income-and-expenses/view/2018/income"

    val allowanceText = "Allowances and deductions"
    val allowanceAmount = "−£17,500.99"
    val allowanceLink = "/report-quarterly/income-and-expenses/view/2018/allowances-and-deductions"

    val taxIsDueText = "Total taxable income"
    val taxIsDueAmount = "£12,500.00"

    val contributionText = "Income Tax and National Insurance contributions"
    val contributionAmount = "£90,500.99"
    val contributionLink = "/report-quarterly/income-and-expenses/view/2018/tax-calculation"

    val chargeInformationParagraph: String = "The amount you need to pay might be different if there are other charges or payments on your account, for example, late payment interest."

    val continueButtonText = "Continue"
  }

  object ExpectedValuesWelsh {
    val title = "Eich trosolwg treth terfynol - Diweddaru a chyflwyno Ffurflen Dreth Incwm - GOV.UK"
    val caption = "6 Ebrill 2017 i 5 Ebrill 2018"

    val insetTextFull = "Os ydych o’r farn bod yr wybodaeth hon yn anghywir gallwch wirio eich Ffurflen Dreth Incwm."
    val insetTextLink = "wirio eich Ffurflen Dreth Incwm."

    val incomeText = "Incwm"

    val allowanceText = "Lwfansau a didyniadau"

    val taxIsDueText = "Cyfanswm eich incwm trethadwy"

    val contributionText = "Treth Incwm a chyfraniadau Yswiriant Gwladol"

    val chargeInformationParagraph: String = "Gall y swm y mae angen i chi ei dalu fod yn wahanol os oes taliadau neu ffioedd eraill ar eich cyfrif, er enghraifft, llog taliad hwyr."

    val continueButtonText = "Yn eich blaen"
  }

  s"calling GET ${controllers.routes.FinalTaxCalculationController.show(taxYear)}" should {
    "display the new calc page in english" which {
      lazy val result = {
        isAuthorisedUser(authorised = true)
        calculationStub()
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponseWoMigration)

        ws.url(url)
          .get()
      }.futureValue

      lazy val document: Document = Jsoup.parse(result.body)

      "have a status of OK (200)" in {
        result.status shouldBe OK
      }

      "have the correct title" in {
        document.title() shouldBe ExpectedValues.title
      }

      "have the correct caption" in {
        document.select(Selectors.caption).text() shouldBe ExpectedValues.caption
      }

      "the inset text" should {

        "have the correct full text" in {
          document.select(Selectors.insetText).text() shouldBe ExpectedValues.insetTextFull
        }

        "have the correct link text" which {
          lazy val insetElement = document.select(Selectors.insetLinkText)

          "has the correct text" in {
            insetElement.text() shouldBe ExpectedValues.insetTextLink
          }

          "has the correct href" in {
            insetElement.attr("href") shouldBe ExpectedValues.insetLinkHref
          }

        }

      }

      "have a table that" should {

        "have the correct income row content" which {
          lazy val key = document.select(Selectors.incomeRowText)

          "has the correct key text" in {
            key.text() shouldBe ExpectedValues.incomeText
          }

          "has the correct amount" in {
            document.select(Selectors.incomeRowAmount).text() shouldBe ExpectedValues.incomeAmount
          }

          "has the correct URL" in {
            key.attr("href") shouldBe ExpectedValues.incomeLink
          }

        }

        "have the correct allowance row content" which {
          lazy val key = document.select(Selectors.allowanceRowText)

          "has the correct key text" in {
            key.text() shouldBe ExpectedValues.allowanceText
          }

          "has the correct amount" in {
            document.select(Selectors.allowanceRowAmount).text() shouldBe ExpectedValues.allowanceAmount
          }

          "has the correct URL" in {
            key.attr("href") shouldBe ExpectedValues.allowanceLink
          }

        }

        "have the correct income on which tax is due row content" which {

          "has the correct key text" in {
            document.select(Selectors.taxIsDueRowText).text() shouldBe ExpectedValues.taxIsDueText
          }

          "has the correct amount" in {
            document.select(Selectors.taxIsDueRowAmount).text() shouldBe ExpectedValues.taxIsDueAmount
          }
        }

        "have the correct total contributions row content" which {
          lazy val key = document.select(Selectors.contributionDueRowText)

          "has the correct key text" in {
            key.text() shouldBe ExpectedValues.contributionText
          }

          "has the correct amount" in {
            document.select(Selectors.contributionDueRowAmount).text() shouldBe ExpectedValues.contributionAmount
          }

          "has the correct URL" in {
            key.attr("href") shouldBe ExpectedValues.contributionLink
          }

        }

      }

      "have a charge or payment information section" that {

        "has the correct paragraph text" in {
          document.select(Selectors.chargeInformationParagraph).text() shouldBe ExpectedValues.chargeInformationParagraph
        }

      }

      "have a submit button" that {
        lazy val submitButton = document.select(Selectors.continueButton)

        "has the correct text" in {
          submitButton.text() shouldBe ExpectedValues.continueButtonText
        }
      }
    }

    "display the page in welsh" which {
      lazy val result: WSResponse = {
        isAuthorisedUser(authorised = true)
        calculationStub()
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponseWoMigration)
        ws.url(url)
          .withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy")
          .get()
      }.futureValue


      lazy val document: Document = Jsoup.parse(result.body)

      "have a status of OK (200)" in {
        result.status shouldBe OK
      }

      "have the correct title" in {
        document.title() shouldBe ExpectedValuesWelsh.title
      }

      "have the correct caption" in {
        document.select(Selectors.caption).text() shouldBe ExpectedValuesWelsh.caption
      }

      "the inset text" should {

        "have the correct full text" in {
          document.select(Selectors.insetText).text() shouldBe ExpectedValuesWelsh.insetTextFull
        }

        "have the correct link text" which {
          lazy val insetElement = document.select(Selectors.insetLinkText)

          "has the correct text" in {
            insetElement.text() shouldBe ExpectedValuesWelsh.insetTextLink
          }

          "has the correct href" in {
            insetElement.attr("href") shouldBe ExpectedValues.insetLinkHref
          }

        }

      }

      "have a table that" should {

        "have the correct income row content" which {
          lazy val key = document.select(Selectors.incomeRowText)

          "has the correct key text" in {
            key.text() shouldBe ExpectedValuesWelsh.incomeText
          }

          "has the correct amount" in {
            document.select(Selectors.incomeRowAmount).text() shouldBe ExpectedValues.incomeAmount
          }

          "has the correct URL" in {
            key.attr("href") shouldBe ExpectedValues.incomeLink
          }

        }

        "have the correct allowance row content" which {
          lazy val key = document.select(Selectors.allowanceRowText)

          "has the correct key text" in {
            key.text() shouldBe ExpectedValuesWelsh.allowanceText
          }

          "has the correct amount" in {
            document.select(Selectors.allowanceRowAmount).text() shouldBe ExpectedValues.allowanceAmount
          }

          "has the correct URL" in {
            key.attr("href") shouldBe ExpectedValues.allowanceLink
          }

        }

        "have the correct income on which tax is due row content" which {

          "has the correct key text" in {
            document.select(Selectors.taxIsDueRowText).text() shouldBe ExpectedValuesWelsh.taxIsDueText
          }

          "has the correct amount" in {
            document.select(Selectors.taxIsDueRowAmount).text() shouldBe ExpectedValues.taxIsDueAmount
          }
        }

        "have the correct total contributions row content" which {
          lazy val key = document.select(Selectors.contributionDueRowText)

          "has the correct key text" in {
            key.text() shouldBe ExpectedValuesWelsh.contributionText
          }

          "has the correct amount" in {
            document.select(Selectors.contributionDueRowAmount).text() shouldBe ExpectedValues.contributionAmount
          }

          "has the correct URL" in {
            key.attr("href") shouldBe ExpectedValues.contributionLink
          }

        }

      }

      "have a charge or payment information section" that {

        "has the correct paragraph text" in {
          document.select(Selectors.chargeInformationParagraph).text() shouldBe ExpectedValuesWelsh.chargeInformationParagraph
        }

      }

      "have a submit button" that {
        lazy val submitButton = document.select(Selectors.continueButton)

        "has the correct text" in {
          submitButton.text() shouldBe ExpectedValuesWelsh.continueButtonText
        }
      }
    }

    "show an error page" when {
      "there is no calc data model" which {
        lazy val result = {
          isAuthorisedUser(authorised = true)
          calculationStubEmptyCalculations()
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

          ws.url(url)
            .get()
        }.futureValue

        "has a status of INTERNAL_SERVER_ERROR (500)" in {
          result.status shouldBe INTERNAL_SERVER_ERROR
        }
      }
    }
  }

  s"calling POST ${controllers.routes.FinalTaxCalculationController.submit(taxYear)}" should {
    "redirect to the confirmation page on income-tax-submission-frontend" which {
      lazy val result = {
        AuthStub.stubAuthorisedWithName()
        calculationStub()
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

        ws.url(url)
          .withFollowRedirects(false)
          .post("{}")
      }.futureValue

      "has a status of SEE_OTHER (303)" in {
        result.status shouldBe SEE_OTHER
      }

      "has the correct redirect url" in {
        result.headers("Location").head shouldBe "http://localhost:9302/update-and-submit-income-tax-return/2018/declaration"
      }

    }

    "show an error page" when {
      "there is no name provided in the auth" in {
        lazy val result = {
          AuthStub.stubAuthorised()
          calculationStub()
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

          ws.url(url)
            .withFollowRedirects(false)
            .post("{}")
        }.futureValue

        result.status shouldBe INTERNAL_SERVER_ERROR
      }

      "there is no calc information" in {
        lazy val result = {
          AuthStub.stubAuthorisedWithName()
          calculationStubEmptyCalculations()
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

          ws.url(url)
            .withFollowRedirects(false)
            .post("{}")
        }.futureValue

        result.status shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
