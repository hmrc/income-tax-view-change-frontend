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

import testConstants.BaseTestConstants.testSelfEmploymentId
import testConstants.BusinessDetailsTestConstants.business1
import testConstants.NextUpdatesTestConstants.twoObligationsSuccessModel
import config.FrontendAppConfig
import models.nextUpdates.{ObligationsModel, NextUpdateModel, NextUpdatesModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers._
import testUtils.TestSupport
import views.html.Obligations

import java.time.LocalDate

class ObligationsViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  val obligationsView: Obligations = app.injector.instanceOf[Obligations]

  class Setup(currentObligations: ObligationsModel, previousObligations: ObligationsModel) {
    val pageDocument: Document = Jsoup.parse(contentAsString(obligationsView(currentObligations, previousObligations,"testBackURL")))
  }

  object obligationsMessages {
    val updates: String = "Updates"
    val updatesDue: String = "Updates due"
    val previousUpdates: String = "Previously submitted updates"
  }

  "The Deadline Reports Page" should {

    lazy val businessIncomeSource = ObligationsModel(Seq(NextUpdatesModel(
      business1.incomeSourceId,
      twoObligationsSuccessModel.obligations
    )))

    val date: LocalDate = LocalDate.now.minusYears(1)
    val nextUpdate: NextUpdateModel = NextUpdateModel(date, date.plusMonths(1), date.plusMonths(2), "Quarterly", Some(date.plusMonths(1)), "#001")

    def basicDeadline(identification: String, obligationType: String): NextUpdatesModel = NextUpdatesModel(identification, List(nextUpdate.copy(obligationType = obligationType)))

    val basicBusinessDeadline: NextUpdatesModel = basicDeadline(testSelfEmploymentId, "Quarterly")

    val obligationModelWithSingleBusiness: ObligationsModel = ObligationsModel(Seq(basicBusinessDeadline))

    "have a h1" in new Setup(businessIncomeSource, obligationModelWithSingleBusiness) {
      pageDocument.select("h1").text shouldBe obligationsMessages.updates
    }

    "display all of the correct information for the main elements/sections on the page" when {

      s"showing the heading ${obligationsMessages.updates} on the page" in new Setup(businessIncomeSource, obligationModelWithSingleBusiness) {
        pageDocument.getElementById("page-heading").text shouldBe obligationsMessages.updates
      }

      s"showing the first tab ${obligationsMessages.updatesDue} on the page" in new Setup(businessIncomeSource, obligationModelWithSingleBusiness) {
        pageDocument.getElementById("tab_current").text shouldBe obligationsMessages.updatesDue
      }
    }
  }
}

