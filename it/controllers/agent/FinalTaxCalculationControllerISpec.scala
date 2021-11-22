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

package controllers.agent

import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino}
import testConstants.CalcDataIntegrationTestConstants.estimatedCalculationFullJson
import controllers.agent.utils.SessionKeys
import helpers.agent.{ComponentSpecBase, SessionCookieBaker}
import helpers.servicemocks.{IncomeTaxViewChangeStub, IndividualCalculationStub}
import models.calculation.{CalculationItem, ListCalculationItems}
import models.core.AccountingPeriodModel
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel, PropertyDetailsModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK, SEE_OTHER}

import java.time.{LocalDate, LocalDateTime}

class FinalTaxCalculationControllerISpec extends ComponentSpecBase with SessionCookieBaker {

  val (taxYear, month, dayOfMonth) = (2018, 5, 6)
  val (hour, minute) = (12, 0)
  val url: String = s"http://localhost:$port" + controllers.agent.routes.FinalTaxCalculationController.show(taxYear).url

  def calculationStub(taxYearString: String = "2017-18"): Unit = {
    IndividualCalculationStub.stubGetCalculationList(testNino, taxYearString)(
      status = OK,
      body = ListCalculationItems(Seq(CalculationItem("idOne", LocalDateTime.of(LocalDate.now().getYear, month, dayOfMonth, hour, minute))))
    )
    IndividualCalculationStub.stubGetCalculation(testNino, "idOne")(
      status = OK,
      body = estimatedCalculationFullJson
    )
  }

  def calculationStubEmptyCalculations(): Unit = {
    IndividualCalculationStub.stubGetCalculationList(testNino, "2017-18")(
      status = NOT_FOUND,
      body = ListCalculationItems(Seq())
    )
  }

  object Selectors {
    val caption = "h1 > span"
    val title = "h1"

    val insetText = "#main-content > div > div > div > p.govuk-inset-text"
    val insetLinkText = "#main-content > div > div > div > p.govuk-inset-text > a"

    val incomeRowText = "#income-deductions-table > tbody > tr:nth-child(1) > th > a"
    val incomeRowAmount = "#income-deductions-table > tbody > tr:nth-child(1) > td"

    val allowanceRowText = "#income-deductions-table > tbody > tr:nth-child(2) > th > a"
    val allowanceRowAmount = "#income-deductions-table > tbody > tr:nth-child(2) > td"

    val taxIsDueRowText = "#income-deductions-table > tbody > tr:nth-child(3) > th"
    val taxIsDueRowAmount = "#income-deductions-table > tbody > tr:nth-child(3) > td"

    val contributionDueRowText = "#taxdue-payments-table > tbody > tr > th > a"
    val contributionDueRowAmount = "#taxdue-payments-table > tbody > tr > td"

    val chargeInformationParagraph = "#main-content > div > div > div > p.govuk-body"

    val continueButton = "#continue-button"
  }

  object ExpectedValues {
    val title = "Your client’s final tax overview - Business Tax account - GOV.UK"
    val caption = "6 April 2017 to 5 April 2018"

    val insetTextFull = "If you think this information is incorrect, you can check your client’s Income Tax Return."
    val insetTextLink = "check your client’s Income Tax Return."
    val insetLinkHref = "http://localhost:9302/update-and-submit-income-tax-return/2018/view"

    val incomeText = "Income"
    val incomeAmount = "£199,505.00"
    val incomeLink = "/report-quarterly/income-and-expenses/view/agents/calculation/2018/income"

    val allowanceText = "Allowances and deductions"
    val allowanceAmount = "−£500.00"
    val allowanceLink = "/report-quarterly/income-and-expenses/view/agents/calculation/2018/deductions"

    val taxIsDueText = "Total taxable income"
    val taxIsDueAmount = "£198,500.00"

    val contributionText = "Income Tax and National Insurance contributions"
    val contributionAmount = "£90,500.00"
    val contributionLink = "/report-quarterly/income-and-expenses/view/agents/calculation/2018/tax-due"

    val chargeInformationParagraph: String = "The amount your client needs to pay might be different if there are other charges or payments on their account, for example, late payment interest."

    val continueButtonText = "Continue"
  }

  object ExpectedValuesWelsh {
    val title = "Trosolwg treth terfynol eich cleient - Cyfrif Treth Busnes - GOV.UK"
    val caption = "6 Ebrill 2017 i 5 Ebrill 2018"

    val insetTextFull = "Os ydych o’r farn bod yr wybodaeth hon yn anghywir gallwch gwirio Ffurflen Dreth Incwm eich cleient."
    val insetTextLink = "gwirio Ffurflen Dreth Incwm eich cleient."

    val incomeText = "Incwm"

    val allowanceText = "Lwfansau a didyniadau"

    val taxIsDueText = "Cyfanswm eich incwm trethadwy"

    val contributionText = "Treth Incwm a chyfraniadau Yswiriant Gwladol"

    val chargeInformationParagraph: String = "Gall y swm sydd angen i’ch cleient ei dalu fod yn wahanol os oes taliadau neu ffioedd eraill ar ei gyfrif, er enghraifft, llog taliad hwyr."

    val continueButtonText = "Yn eich blaen"
  }

  val testArn: String = "1"

  val clientDetailsWithConfirmation: Map[String, String] = Map(
    SessionKeys.clientFirstName -> "Test",
    SessionKeys.clientLastName -> "User",
    SessionKeys.clientUTR -> "1234567890",
    SessionKeys.clientNino -> testNino,
    SessionKeys.clientMTDID -> testMtditid,
    SessionKeys.confirmedClient -> "true"
  )

  lazy val playSessionCookie: String = bakeSessionCookie(clientDetailsWithConfirmation)

  lazy val getCurrentTaxYearEnd: LocalDate = {
    val currentDate: LocalDate = LocalDate.now
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, 4, 6))) LocalDate.of(currentDate.getYear, 4, 5)
    else LocalDate.of(currentDate.getYear + 1, 4, 5)
  }

  lazy val incomeSourceDetailsSuccess: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    mtdbsa = testMtditid,
    yearOfMigration = None,
    businesses = List(BusinessDetailsModel(
      "testId",
      AccountingPeriodModel(LocalDate.now, LocalDate.now.plusYears(1)),
      Some("Test Trading Name"), None, None, None, None, None, None, None,
      Some(LocalDate.of(2018, 1, 1))
    )),
    property = Some(
      PropertyDetailsModel(
        "testId2",
        AccountingPeriodModel(LocalDate.now, LocalDate.now.plusYears(1)),
        None, None, None, None,
        Some(LocalDate.of(2018, 1, 1))
      )
    )
  )

  s"calling GET ${controllers.agent.routes.FinalTaxCalculationController.show(taxYear)}" should {

    "display the page" which {
      lazy val result = {
        stubAuthorisedAgentUser(authorised = true)
        calculationStub()
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsSuccess
        )

        ws.url(url)
          .withHttpHeaders(HeaderNames.COOKIE -> playSessionCookie)
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
      lazy val result = {
        stubAuthorisedAgentUser(authorised = true)
        calculationStub()
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsSuccess
        )

        ws.url(url)
          .withHttpHeaders(HeaderNames.COOKIE -> playSessionCookie, HeaderNames.ACCEPT_LANGUAGE -> "cy")
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
          stubAuthorisedAgentUser(authorised = true)
          calculationStubEmptyCalculations()
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
            status = OK,
            response = incomeSourceDetailsSuccess
          )

            ws.url(url)
              .withHttpHeaders(HeaderNames.COOKIE -> playSessionCookie)
              .get()

        }.futureValue

        "has a status of INTERNAL_SERVER_ERROR (500)" in {
          result.status shouldBe INTERNAL_SERVER_ERROR
        }
      }
    }
  }

  s"calling POST ${controllers.agent.routes.FinalTaxCalculationController.submit(taxYear)}" should {

    "redirect to the confirmation page on income-tax-submission-frontend" which {
      lazy val result = {
        stubAuthorisedAgentUser(authorised = true, clientMtdId = testMtditid)
        calculationStub()
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsSuccess
        )

        ws.url(url)
          .withFollowRedirects(false)
          .withHttpHeaders(HeaderNames.COOKIE -> playSessionCookie, "Csrf-Token" -> "nocheck")
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

      "there is no calc information" in {
        lazy val result = {
          stubAuthorisedAgentUser(authorised = true)
          calculationStubEmptyCalculations()
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
            status = OK,
            response = incomeSourceDetailsSuccess
          )

            ws.url(url)
              .withFollowRedirects(false)
              .withHttpHeaders(HeaderNames.COOKIE -> playSessionCookie, "Csrf-Token" -> "nocheck")
              .post("{}")
        }.futureValue

        result.status shouldBe INTERNAL_SERVER_ERROR
      }

    }

  }
}
