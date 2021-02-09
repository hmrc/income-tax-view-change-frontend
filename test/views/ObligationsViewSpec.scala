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

package views

import java.time.LocalDate

import assets.BaseTestConstants.{testMtdItUser, testSelfEmploymentId}
import assets.BusinessDetailsTestConstants.business1
import assets.MessagesLookUp.{Breadcrumbs => breadcrumbMessages}
import assets.ReportDeadlinesTestConstants.twoObligationsSuccessModel
import config.FrontendAppConfig
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import models.reportDeadlines.{ObligationsModel, ReportDeadlineModel, ReportDeadlinesModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testUtils.TestSupport

class ObligationsViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  class Setup(currentObligations: ObligationsModel, previousObligations: ObligationsModel) {
    val html: HtmlFormat.Appendable = views.html.obligations(currentObligations, previousObligations, mockImplicitDateFormatter)(FakeRequest(), implicitly, mockAppConfig, testMtdItUser)
    val pageDocument: Document = Jsoup.parse(contentAsString(views.html.obligations(currentObligations, previousObligations, mockImplicitDateFormatter)))
  }

  object obligationsMessages {
    val updates: String = "Updates"
    val updatesDue: String = "Updates due"
    val previousUpdates: String = "Previously submitted updates"
  }

  "The Deadline Reports Page" should {

    lazy val businessIncomeSource = ObligationsModel(Seq(ReportDeadlinesModel(
      business1.incomeSourceId,
      twoObligationsSuccessModel.obligations
    )))

    val date: LocalDate = LocalDate.now.minusYears(1)
    val reportDeadline: ReportDeadlineModel = ReportDeadlineModel(date, date.plusMonths(1), date.plusMonths(2), "Quarterly", Some(date.plusMonths(1)), "#001")

    def basicDeadline(identification: String, obligationType: String): ReportDeadlinesModel = ReportDeadlinesModel(identification, List(reportDeadline.copy(obligationType = obligationType)))

    val basicBusinessDeadline: ReportDeadlinesModel = basicDeadline(testSelfEmploymentId, "Quarterly")

    val obligationModelWithSingleBusiness: ObligationsModel = ObligationsModel(Seq(basicBusinessDeadline))

    "have a h1" in new Setup(businessIncomeSource, obligationModelWithSingleBusiness) {
      pageDocument.select("h1").text shouldBe obligationsMessages.updates
    }

    "display all of the correct information for the main elements/sections on the page" when {

      "showing the breadcrumb trail on the page" in new Setup(businessIncomeSource, obligationModelWithSingleBusiness) {
        pageDocument.getElementById("breadcrumb-bta").text shouldBe breadcrumbMessages.bta
        pageDocument.getElementById("breadcrumb-it").text shouldBe breadcrumbMessages.it
        pageDocument.getElementById("breadcrumb-updates").text shouldBe breadcrumbMessages.updates
      }

      s"showing the heading ${obligationsMessages.updates} on the page" in new Setup(businessIncomeSource, obligationModelWithSingleBusiness) {
        pageDocument.getElementById("page-heading").text shouldBe obligationsMessages.updates
      }

      s"showing the first tab ${obligationsMessages.updatesDue} on the page" in new Setup(businessIncomeSource, obligationModelWithSingleBusiness) {
        pageDocument.getElementById("tab_current").text shouldBe obligationsMessages.updatesDue
      }

      s"showing the second tab ${obligationsMessages.previousUpdates} on the page" in new Setup(businessIncomeSource, obligationModelWithSingleBusiness) {
        pageDocument.getElementById("tab_previous").text shouldBe obligationsMessages.previousUpdates
      }
    }
  }
}

