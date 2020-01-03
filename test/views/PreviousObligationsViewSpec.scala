/*
 * Copyright 2020 HM Revenue & Customs
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

package views

import java.time.LocalDate

import assets.BaseTestConstants.testMtdItUser
import assets.Messages.{PreviousObligations => previousObligations}
import config.FrontendAppConfig
import implicits.ImplicitDateFormatter
import models.reportDeadlines.{ReportDeadlineModel, ReportDeadlineModelWithIncomeType}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testUtils.TestSupport

class PreviousObligationsViewSpec extends TestSupport with ImplicitDateFormatter {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val noPreviousObligations: List[ReportDeadlineModelWithIncomeType] = Nil
  val date: LocalDate = LocalDate.now

  val reportDeadline: ReportDeadlineModel = ReportDeadlineModel(date, date.plusMonths(1), date.plusMonths(2), "Quarterly", Some(date.plusMonths(1)), "#001")
  val basicDeadline: ReportDeadlineModelWithIncomeType = ReportDeadlineModelWithIncomeType("Property", reportDeadline)

  class Setup(previousObligations: List[ReportDeadlineModelWithIncomeType] = Nil) {
    val html: HtmlFormat.Appendable = views.html.previousObligations(previousObligations)(FakeRequest(), implicitly, mockAppConfig, testMtdItUser)
    val pageDocument: Document = Jsoup.parse(contentAsString(html))

    def getElementById(id: String): Option[Element] = Option(pageDocument.getElementById(id))
    def getTextOfElementById(id: String): Option[String] = getElementById(id).map(_.text)
  }

  "previousObligations" should {

    "have a title" in new Setup {
      pageDocument.title shouldBe previousObligations.title
    }

    "have a heading" in new Setup {
      getTextOfElementById("heading") shouldBe Some(previousObligations.heading)
    }

    "have a sub heading" in new Setup {
      getTextOfElementById("sub-heading") shouldBe Some(previousObligations.subHeading)
    }

    "display the no previous updates message when there are none" in new Setup {
      getTextOfElementById("no-previous-obligations") shouldBe Some(previousObligations.noPreviousObligations)
      getElementById("income-source-0") shouldBe None
      getElementById("obligation-type-0") shouldBe None
      getElementById("date-from-to-0") shouldBe None
      getElementById("was-due-on-0") shouldBe None
      getElementById("submitted-on-label-0") shouldBe None
      getElementById("submitted-on-date-0") shouldBe None
    }

    "not display the no previous updates message when there are previous updates to display" in new Setup(List(basicDeadline)) {
      getElementById("no-previous-obligations") shouldBe None
    }

    "display a list of previously submitted updates" that {

      val propertyObligation = basicDeadline.copy(incomeType = "Property")
      val businessObligation = basicDeadline.copy(incomeType = "Obligations Ltd.")
      val crystallisationObligation = basicDeadline.copy(incomeType = "Crystallised")

      "displays the correct income source" when {
        "it is from property" in new Setup(List(propertyObligation)) {
          getTextOfElementById("income-source-0") shouldBe Some(previousObligations.propertyIncomeSource)
        }
        "it is from business" in new Setup(List(businessObligation)) {
          getTextOfElementById("income-source-0") shouldBe Some("Obligations Ltd.")
        }
        "it is from a crystallisation" in new Setup(List(crystallisationObligation)) {
          getTextOfElementById("income-source-0") shouldBe Some(previousObligations.crystallisationIncomeSource)
        }
      }

      "displays the correct obligation type" when {
        "it is quarterly" in new Setup(List(basicDeadline.copy(obligation = reportDeadline.copy(obligationType = "Quarterly")))) {
          getTextOfElementById("obligation-type-0") shouldBe Some(previousObligations.quarterly)
        }
        "it is eops" in new Setup(List(basicDeadline.copy(obligation = reportDeadline.copy(obligationType = "EOPS")))) {
          getTextOfElementById("obligation-type-0") shouldBe Some(previousObligations.eops)
        }
        "it is crystallisation" in new Setup(List(basicDeadline.copy(obligation = reportDeadline.copy(obligationType = "Crystallised")))) {
          getTextOfElementById("obligation-type-0") shouldBe Some(previousObligations.crystallised)
        }
      }

      "displays the from and to dates of the obligation" in new Setup(List(basicDeadline)) {
        getTextOfElementById("date-from-to-0") shouldBe Some(previousObligations.dateToDate(date.toLongDate, date.plusMonths(1).toLongDate))
      }

      "displays the date the obligation was due on" in new Setup(List(basicDeadline)) {
        getTextOfElementById("was-due-on-0") shouldBe Some(previousObligations.wasDueOn(date.plusMonths(2).toLongDate))
      }

      "displays the submitted on label" in new Setup(List(basicDeadline)) {
        getTextOfElementById("submitted-on-label-0") shouldBe Some(previousObligations.submittedOn)
      }

      "displays the date the obligation was submitted" in new Setup(List(basicDeadline)) {
        getTextOfElementById("submitted-on-date-0") shouldBe Some(date.plusMonths(1).toLongDate)
      }

    }

  }

}

