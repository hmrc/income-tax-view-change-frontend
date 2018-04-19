/*
 * Copyright 2018 HM Revenue & Customs
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

import assets.BaseTestConstants._
import assets.BusinessDetailsTestConstants._
import assets.Messages.{Breadcrumbs => breadcrumbMessages, ReportDeadlines => messages}
import assets.PropertyDetailsTestConstants._
import assets.ReportDeadlinesTestConstants._
import config.FrontendAppConfig
import models.incomeSourcesWithDeadlines.{BusinessIncomeWithDeadlinesModel, IncomeSourcesWithDeadlinesModel, PropertyIncomeWithDeadlinesModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import utils.{ImplicitDateFormatter, TestSupport}

class ReportDeadlinesViewSpec extends TestSupport with ImplicitDateFormatter {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]


  private def pageSetup(model: IncomeSourcesWithDeadlinesModel) = new {
    lazy val page: HtmlFormat.Appendable = views.html.report_deadlines(model)(FakeRequest(), applicationMessages, mockAppConfig, testMtdItUser)
    lazy val document: Document = Jsoup.parse(contentAsString(page))
  }


  "The ReportDeadlines view" should {
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

    s"have the title '${messages.title}'" in {
      document.title() shouldBe messages.title
    }

    "have a breadcrumb trail" in {
      document.getElementById("breadcrumb-bta").text shouldBe breadcrumbMessages.bta
      document.getElementById("breadcrumb-it").text shouldBe breadcrumbMessages.it
      document.getElementById("breadcrumb-obligations").text shouldBe breadcrumbMessages.obligations
    }

    "have a table containing the obligations" should {

      "contain the heading for Report period" in {
        document.getElementById("bi-1-period-heading").text() shouldBe "Report period"
      }

      "contain the heading for Status" in {
        document.getElementById("bi-1-status-heading").text() shouldBe "Report due date"
      }

      "contain the first row and have the start date as '1 Apr 2017' and status 'Received'" in {
        document.getElementById("bi-1-ob-1-start").text() shouldBe "1 Jul 2017"
        document.getElementById("bi-1-ob-1-status").text() shouldBe "30 Oct 2017 Overdue"
      }

      s"contain a second row and have ${messages.eops} and a status '31 Oct 2017'" in {
        document.getElementById("bi-1-ob-2-eops").text() shouldBe messages.eops
        document.getElementById("bi-1-ob-2-status").text() shouldBe "31 Oct 2017"
      }

      "not contain a third row" in {
        Option(document.getElementById("bi-1-ob-3-status")) shouldBe None
      }
    }

    "when only business obligations are returned" should {

      val setup = pageSetup(businessIncomeSource)
      import setup._

      "contain a section for Business ReportDeadlines" in {
        document.getElementById("bi-1-section").text() shouldBe testTradeName
      }

      "not contain Property ReportDeadlines section" in {
        Option(document.getElementById("pi-section")) shouldBe None
      }
    }

    "when only property obligations are returned" should {

      lazy val propertyIncomeModel = IncomeSourcesWithDeadlinesModel(
        List.empty,
        Some(PropertyIncomeWithDeadlinesModel(
          propertyDetails,
          reportDeadlines  = obligationsEOPSDataSuccessModel
        ))
      )

      val setup = pageSetup(propertyIncomeModel)
      import setup._

      "contain a section for Property ReportDeadlines" in {
        document.getElementById("pi-section").text() shouldBe messages.propertyHeading
      }

      "contain a paragraph to explain that it is all properties in their portfolio" in {
        document.getElementById("portfolio").text() shouldBe messages.portfolio
      }

      "not contain Business ReportDeadlines section" in {
        Option(document.getElementById("bi-1-section")) shouldBe None
      }
    }

    "when a business has ceased trading" should {
      lazy val ceasedBusinessIncomeModel = IncomeSourcesWithDeadlinesModel(
        List(
          BusinessIncomeWithDeadlinesModel(
            ceasedBusiness,
            reportDeadlines = obligationsEOPSDataSuccessModel
          )
        ),
        None
      )

      val setup = pageSetup(ceasedBusinessIncomeModel)
      import setup._

      "contains text under the business name stating the business has ceased trading" in {
        document.getElementById("bi-1-ceased").text() shouldBe messages.ceased("1 January 2018")
      }
    }

    "when properties have ceased trading" should {
      lazy val ceasedPropertyIncomeModel = IncomeSourcesWithDeadlinesModel(
        List(),
        Some(
          PropertyIncomeWithDeadlinesModel(
            ceasedPropertyDetails,
            reportDeadlines = obligationsEOPSDataSuccessModel
          )
        )
      )

      val setup = pageSetup(ceasedPropertyIncomeModel)
      import setup._

      "have text saying that the properties have ceased trading" in {
        document.getElementById("portfolio").text() shouldBe messages.ceasedProperty("1 January 2018")
      }

    }

    "when both Business and Property obligations are errored" should {

      lazy val bothIncomeSourcesReportsErrored = IncomeSourcesWithDeadlinesModel(
        List(
          BusinessIncomeWithDeadlinesModel(
            business1,
            reportDeadlines = obligationsDataErrorModel
          )
        ),
        Some(PropertyIncomeWithDeadlinesModel(
          propertyDetails,
          reportDeadlines  = obligationsDataErrorModel
        ))
      )

      val setup = pageSetup(bothIncomeSourcesReportsErrored)
      import setup._

      "contains a no section Property ReportDeadlines" in {
        Option(document.getElementById("pi-section")) shouldBe None
      }

      "contains a no section Business ReportDeadlines" in {
        Option(document.getElementById("bi-1-section")) shouldBe None
      }

      "contains error content" which {

        s"has a paragraph with the message '${messages.Errors.p1}'" in {
          document.getElementById("p1").text() shouldBe messages.Errors.p1
        }

        s"has a second paragraph with the message '${messages.Errors.p2}'" in {
          document.getElementById("p2").text() shouldBe messages.Errors.p2
        }
      }
    }

    "when Business obligations are errored and there are no Property obligations" should {

      lazy val businessIncomeSourcesReportsErrored = IncomeSourcesWithDeadlinesModel(
        List(
          BusinessIncomeWithDeadlinesModel(
            business1,
            reportDeadlines = obligationsDataErrorModel
          )
        ),
        None
      )

      val setup = pageSetup(businessIncomeSourcesReportsErrored)
      import setup._

      "contain no section for Property ReportDeadlines" in {
        Option(document.getElementById("pi-section")) shouldBe None
      }

      "contains a section for Business ReportDeadlines" which {

        s"has the heading '$testTradeName'" in {
          document.getElementById("bi-1-section").text() shouldBe testTradeName
        }

        s"has a paragraph with the message '${messages.Errors.p1}'" in {
          document.getElementById("bi-1-p1").text() shouldBe messages.Errors.p1
        }

        s"has a second paragraph with the message '${messages.Errors.p2}'" in {
          document.getElementById("bi-1-p2").text() shouldBe messages.Errors.p2
        }
      }
    }

    "when Property obligations are errored and there are no Business obligations" should {

      lazy val propertyIncomeSourcesReportsErrored = IncomeSourcesWithDeadlinesModel(
        List(),
        Some(PropertyIncomeWithDeadlinesModel(
          propertyDetails,
          reportDeadlines  = obligationsDataErrorModel
        ))
      )

      val setup = pageSetup(propertyIncomeSourcesReportsErrored)
      import setup._

      "contain no section for Business ReportDeadlines" in {
        Option(document.getElementById("bi-1-section")) shouldBe None
      }

      "contains a section for Property ReportDeadlines" which {

        s"has the heading '${messages.propertyHeading}'" in {
          document.getElementById("pi-section").text() shouldBe messages.propertyHeading
        }

        s"has a paragraph with the message '${messages.Errors.p1}'" in {
          document.getElementById("pi-p1").text() shouldBe messages.Errors.p1
        }

        s"has a second paragraph with the message '${messages.Errors.p2}'" in {
          document.getElementById("pi-p2").text() shouldBe messages.Errors.p2
        }
      }
    }

    s"have a dropdown link '${messages.Dropdown.dropdownText}' containing text" in {
      document.getElementById("howToDoThis").text() shouldBe messages.Dropdown.dropdownText
      document.getElementById("why-may-change-1").text() shouldBe s"${messages.Dropdown.dropdownLink} ${messages.Dropdown.dropdown1}"
      document.getElementById("why-may-change-2").text() shouldBe messages.Dropdown.dropdown2
      document.getElementById("why-may-change-3").text() shouldBe messages.Dropdown.dropdown3
      document.getElementById("why-may-change-4").text() shouldBe messages.Dropdown.dropdown4
      document.getElementById("accounting-software-link").attr("href") shouldBe mockAppConfig.accountingSoftwareLinkUrl
    }

  }
}
