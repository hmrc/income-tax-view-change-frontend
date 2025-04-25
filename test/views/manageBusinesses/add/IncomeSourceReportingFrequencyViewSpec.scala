/*
 * Copyright 2025 HM Revenue & Customs
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

package views.manageBusinesses.add

import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import forms.manageBusinesses.add.IncomeSourceReportingFrequencyForm
import mocks.services.MockDateService
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Mockito.when
import play.api.mvc.Call
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import testUtils.TestSupport
import views.html.manageBusinesses.add.IncomeSourceReportingFrequency

import java.time.LocalDate

class IncomeSourceReportingFrequencyViewSpec extends TestSupport with MockDateService {
  val view: IncomeSourceReportingFrequency = app.injector.instanceOf[IncomeSourceReportingFrequency]

  class Setup(incomeSourceType: IncomeSourceType) {
    when(mockDateService.getCurrentTaxYearStart) thenReturn fixedDate
    when(mockDateService.getCurrentTaxYearEnd) thenReturn fixedDate.getYear + 1

    val subHeadingText: String = incomeSourceType match {
      case SelfEmployment => "Sole trader"
      case UkProperty => "UK property"
      case ForeignProperty => "Foreign property"
    }

    val title: String = "Your new business is set to report annually - Manage your Income Tax updates - GOV.UK"
    val titleError: String = "Error: Your new business is set to report annually - GOV.UK"
    val titleAgent: String = "Your new business is set to report annually - Manage your client’s Income Tax updates - GOV.UK"
    val heading: String = "Your new business is set to report annually"
    val paragraph1: String = "Because this is a new business, for up to 2 tax years you can submit its income and expenses once a year in your tax return, even if:"
    val reportingFrequencyUlLi1: String = "you are voluntarily opted in or required to report quarterly for your other businesses"
    val reportingFrequencyUlLi2: String = "your income from self-employment or property, or both, exceed the income threshold"
    val paragraph2: String = "You can choose to report quarterly, which means submitting an update every 3 months in addition to your tax return"
    val reportingFrequencyFormH1: String = "Do you want to change to report quarterly?"
    val reportingFrequencyFormNoSelectionError: String = "Select yes if you want to report quarterly or select no if you want to report annually"
    val continueButtonText: String = "Continue"

    val pageDocument: Document = Jsoup.parse(contentAsString(view(false, Call("", ""), IncomeSourceReportingFrequencyForm(), incomeSourceType, mockDateService)))
  }

  def getReportingFrequencyTableMessages(taxYear: Int): (String, String) = {
    (s"Reporting frequency ${taxYear} to ${taxYear+1}", "Annual")
  }

  def getWarningInsetTextMessage(currentTaxYearEnd: Int): String = {
    s"From April ${currentTaxYearEnd + 1} when this 2-year tax period ends, you could be required to report quarterly."
  }

  val incomeSourceTypes: Seq[IncomeSourceType] = List(SelfEmployment, UkProperty, ForeignProperty)

  incomeSourceTypes.foreach { incomeSourceType =>

    s"Income source reporting frequency page for incomeSourceType: $incomeSourceType" should {
      "have the correct title" in new Setup(incomeSourceType) {
        pageDocument.title() shouldBe title
      }

      "have the correct sub-heading" in new Setup(incomeSourceType) {
        pageDocument.getElementsByClass("govuk-caption-l").textNodes().get(0).text shouldBe subHeadingText
      }

      "have the correct heading" in new Setup(incomeSourceType) {
        pageDocument.getElementsByClass("govuk-heading-l margin-bottom-100").get(0).text shouldBe heading
      }

      "have the correct page contents" in new Setup(incomeSourceType) {
        val thisTaxYear: Int = LocalDate.of(2023, 4, 6).getYear
        val warningInsetTextMessages: String = getWarningInsetTextMessage(currentTaxYearEnd = thisTaxYear + 1)
        val documentTableC1 = pageDocument.getElementsByTag("th")
        val documentTableC2 = pageDocument.getElementsByTag("td")

        pageDocument.getElementById("paragraph-1").text shouldBe paragraph1
        pageDocument.getElementById("inset-text-bullet-1").text shouldBe reportingFrequencyUlLi1
        pageDocument.getElementById("inset-text-bullet-2").text shouldBe reportingFrequencyUlLi2

        documentTableC1.get(0).text shouldBe getReportingFrequencyTableMessages(thisTaxYear)._1
        documentTableC2.get(0).text shouldBe getReportingFrequencyTableMessages(thisTaxYear)._2
        documentTableC1.get(1).text shouldBe getReportingFrequencyTableMessages(thisTaxYear + 1)._1
        documentTableC2.get(1).text shouldBe getReportingFrequencyTableMessages(thisTaxYear + 1)._2

        pageDocument.getElementById("paragraph-2").text shouldBe paragraph2
        pageDocument.getElementById("warning-inset").text shouldBe warningInsetTextMessages
        pageDocument.getElementById("reporting-quarterly-form").getElementsByClass("govuk-fieldset__legend--m").get(0).text shouldBe reportingFrequencyFormH1
        pageDocument.getElementById("continue-button").text shouldBe continueButtonText
      }
    }
  }
}
