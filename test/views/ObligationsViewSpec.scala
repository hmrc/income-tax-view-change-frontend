/*
 * Copyright 2019 HM Revenue & Customs
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

import assets.BaseTestConstants.testMtdItUser
import assets.Messages.{Breadcrumbs => breadcrumbMessages, Obligations => messages}
import assets.BusinessDetailsTestConstants.business1
import assets.ReportDeadlinesTestConstants.twoObligationsSuccessModel
import config.FrontendAppConfig
import implicits.ImplicitDateFormatter
import models.incomeSourcesWithDeadlines.{BusinessIncomeWithDeadlinesModel, IncomeSourcesWithDeadlinesModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testUtils.TestSupport

class ObligationsViewSpec extends TestSupport with ImplicitDateFormatter {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  private def pageSetup(model: IncomeSourcesWithDeadlinesModel) = new {
    lazy val page: HtmlFormat.Appendable = views.html.obligations(model)(FakeRequest(), applicationMessages, mockAppConfig, testMtdItUser)
    lazy val document: Document = Jsoup.parse(contentAsString(page))
  }

  "The Deadline Reports Page" should {
    lazy val businessIncomeSource = IncomeSourcesWithDeadlinesModel(
      List(
        BusinessIncomeWithDeadlinesModel(
          business1,
          reportDeadlines = twoObligationsSuccessModel
        )
      ),
      None
    )

    val setup = pageSetup(businessIncomeSource)
    import setup._

    //Main content section
    "show the breadcrumb trail on the page" in{
      document.getElementById("breadcrumb-bta").text shouldBe breadcrumbMessages.bta
      document.getElementById("breadcrumb-it").text shouldBe breadcrumbMessages.it
      document.getElementById("breadcrumb-obligations").text shouldBe breadcrumbMessages.obligations
    }

    s"show the title ${messages.title} on the page" in{
      document.getElementById("page-heading").text shouldBe messages.title
    }


    s"show the Sub heading ${messages.subTitle} on page" in{
      document.getElementById("page-sub-heading").text shouldBe messages.subTitle
    }





    //Declarations section

    //Heading and dropdown subsection
    "show the Declaration heading and drop down section on the page" in{
      document.getElementById("declaration-dropdown-title").text shouldBe messages.declerationDropDown
    }

    //Property income EOPS subsection

    //Income source EOPS subsection

    //Quarterly returns section

    //Heading and dropdown subsection
    "show the Quarterly heading and drop down section on the page" in{
      document.getElementById("quarterly-dropdown-title").text shouldBe messages.quarterlyDropDown
    }

    //Property income quarterly subsection

    //Income source quarterly subsection
  }
}
