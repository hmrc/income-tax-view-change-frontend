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

import helpers.ComponentSpecBase
import helpers.servicemocks.AuthStub.mockImplicitDateFormatter.{longDate, toTaxYearEndDate}
import helpers.servicemocks.{IncomeTaxCalculationStub, IncomeTaxViewChangeStub}
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import models.core.AccountingPeriodModel
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel, PropertyDetailsModel}
import models.liabilitycalculation.LiabilityCalculationError
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.libs.ws.WSResponse
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid, testNino}
import testConstants.IncomeSourceIntegrationTestConstants.multipleBusinessesAndPropertyResponseWoMigration
import testConstants.NewCalcBreakdownItTestConstants.liabilityCalculationModelSuccessFull

import java.time.LocalDate
import java.util.Locale

class InYearTaxCalculationControllerISpec extends ComponentSpecBase {
  val implicitDateFormatter: ImplicitDateFormatter = app.injector.instanceOf[ImplicitDateFormatterImpl]

  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())
  lazy val mcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]

  def toMessages(language: String): Messages = {
    mcc.messagesApi.preferred(Seq(
      new Lang(new Locale(language))
    ))
  }

  val (taxYear, month, dayOfMonth) = (if (LocalDate.now().isAfter(toTaxYearEndDate(LocalDate.now().getYear.toString))){
    LocalDate.now().getYear+1
  }
  else LocalDate.now().getYear, LocalDate.now.getMonthValue, LocalDate.now.getDayOfMonth)
  val timeStampEN: String = longDate(LocalDate.now)(toMessages("EN")).toLongDate
  val timeStampCY: String = longDate(LocalDate.now)(toMessages("CY")).toLongDate


  val url: String = s"http://localhost:$port" + controllers.routes.InYearTaxCalculationController.showAgent().url

  def calculationStub(): Unit = {
    IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, taxYear.toString)(
      status = OK,
      body = liabilityCalculationModelSuccessFull
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

  lazy val playSessionCookie: String = bakeSessionCookie(clientDetailsWithConfirmation)

  lazy val incomeSourceDetailsSuccess: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    mtdbsa = testMtditid,
    yearOfMigration = None,
    businesses = List(BusinessDetailsModel(
      Some("testId"),
      Some(AccountingPeriodModel(LocalDate.now, LocalDate.now.plusYears(1))),
      Some("Test Trading Name"),
      Some(LocalDate.of(taxYear, month, dayOfMonth))
    )),
    property = Some(
      PropertyDetailsModel(
        Some("testId2"),
        Some(AccountingPeriodModel(LocalDate.now, LocalDate.now.plusYears(1))),
        Some(LocalDate.of(taxYear, month, dayOfMonth))
      )
    )
  )

  object Selectors {
    val caption = "#main-content > div > div > div > header > p"
    val title = "h1"

    val insetText = "#main-content > div > div > div > p"

    val incomeRowText = "#income-deductions-contributions-table > tbody > tr:nth-child(1) > td:nth-child(1) > a"
    val incomeRowAmount = "#income-deductions-contributions-table > tbody > tr:nth-child(1) > td.govuk-table__cell.govuk-table__cell--numeric"

    val allowanceRowText = "#income-deductions-contributions-table > tbody > tr:nth-child(2) > td:nth-child(1) > a"
    val allowanceRowAmount = "#income-deductions-contributions-table > tbody > tr:nth-child(2) > td.govuk-table__cell.govuk-table__cell--numeric"

    val taxIsDueRowText = "#income-deductions-contributions-table > tbody > tr:nth-child(3) > td:nth-child(1)"
    val taxIsDueRowAmount = "#income-deductions-contributions-table > tbody > tr:nth-child(3) > td.govuk-table__cell.govuk-table__cell--numeric"

    val continueButton = "#continue-button"
  }

  object ExpectedValues {
    val title = s"Your client’s tax overview 6 April ${taxYear-1} to $timeStampEN - Update and submit an Income Tax Return - GOV.UK"
    val caption = s"6 April ${taxYear-1} to 5 April $taxYear"

    val insetTextFull = s"This calculation is only based on your client’s completed updates for this tax year up to $timeStampEN. It is not their final tax bill for the year. It is a year to date calculation based on the information that has been entered so far."

    val tableTitle = "Calculation"

    val incomeText = "Income"
    val incomeAmount = "£12,500.00"
    val incomeLink = s"/report-quarterly/income-and-expenses/view/agents/calculation/$taxYear/income"

    val allowanceText = "Allowances and deductions"
    val allowanceAmount = "−£17,500.99"
    val allowanceLink = s"/report-quarterly/income-and-expenses/view/agents/calculation/$taxYear/deductions"

    val taxableIncome = "Total income on which tax is due"
    val taxableIncomeAmount = "£12,500.00"

    val taxIsDueText = "Total income on which tax is due"
    val taxIsDueAmount = "£12,500.00"

    val continueButtonText = "Go to Income Tax Account"
  }
  object ExpectedValuesWelsh {
    val title = s"Trosolwg o dreth eich cleient 6 Ebrill ${taxYear-1} i $timeStampCY - Diweddaru a chyflwyno Ffurflen Dreth Incwm - GOV.UK"
    val caption = s"6 Ebrill ${taxYear-1} i 5 Ebrill $taxYear"

    val insetTextFull = s"Mae’r cyfrifiad hwn yn seiliedig ar ddiweddariadau gorffenedig eich cleient ar gyfer y flwyddyn dreth hon hyd at $timeStampCY yn unig. Nid dyma ei fil treth terfynol ar gyfer y flwyddyn. Cyfrifiad o’r flwyddyn hyd yma yw hwn ar sail yr wybodaeth sydd wedi cael ei nodi hyd yma."

    val tableTitle = "Cyfrifiad"

    val incomeText = "Incwm"
    val incomeAmount = "£12,500.00"
    val incomeLink = s"/report-quarterly/income-and-expenses/view/agents/calculation/$taxYear/income"

    val allowanceText = "Lwfansau a didyniadau"
    val allowanceAmount = "−£17,500.99"
    val allowanceLink = s"/report-quarterly/income-and-expenses/view/agents/calculation/$taxYear/deductions"

    val taxIsDueText = "Cyfanswm yr incwm y mae treth yn ddyledus arno"
    val taxIsDueAmount = "£12,500.00"

    val continueButtonText = "mynd i gyfrifo treth"
  }


  s"calling GET ${controllers.routes.InYearTaxCalculationController.showAgent().url}" should {
    "display the new calc page in english" which {
      lazy val result = {
        stubAuthorisedAgentUser(authorised = true)
        calculationStub()
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponseWoMigration)

        ws.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookie)
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
        stubAuthorisedAgentUser(authorised = true)
        calculationStub()
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponseWoMigration)
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

        "have a submit button" that {
          lazy val submitButton = document.select(Selectors.continueButton)

          "has the correct text" in {
            submitButton.text() shouldBe ExpectedValuesWelsh.continueButtonText
          }
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
      "there is a different status to NOT_FOUND" which {
        lazy val result = {
          stubAuthorisedAgentUser(authorised = true)
          calculationStubFailCalculations()
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

}
