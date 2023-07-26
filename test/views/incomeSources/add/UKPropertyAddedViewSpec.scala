/*
 * Copyright 2023 HM Revenue & Customs
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

package views.incomeSources.add

import models.incomeSourceDetails.viewmodels.{DatesModel, ObligationsViewModel}
import org.jsoup.Jsoup
import play.test.Helpers.contentAsString
import testConstants.BaseTestConstants.testPropertyIncomeId
import testUtils.TestSupport
import views.html.incomeSources.add.UKPropertyAdded

import java.time.LocalDate

class UKPropertyAddedViewSpec extends TestSupport {

  val ukPropertyAddedView: UKPropertyAdded = app.injector.instanceOf[UKPropertyAdded]

  class Setup(isAgent: Boolean) {
    val date = LocalDate.of(2022, 1, 1)
    val viewModel: ObligationsViewModel = ObligationsViewModel(
      quarterlyObligationsDatesYearOne = Seq(DatesModel(date, date.plusDays(1), date.plusDays(2), "#001", isFinalDec = false)),
      quarterlyObligationsDatesYearTwo = Seq(DatesModel(date.plusYears(1), date.plusYears(1).plusDays(1), date.plusYears(1).plusDays(2), "#001", isFinalDec = false)),
      eopsObligationsDates = Seq(DatesModel(date, date.plusDays(1), date.plusDays(2), "EOPS", isFinalDec = false)),
      finalDeclarationDates = Seq(DatesModel(date, date.plusDays(1), date.plusDays(2), "C", isFinalDec = true)),
      currentTaxYear = 2023,
      showPrevTaxYears = true
    ) //TODO: move to utils file
    val backUrl = if (isAgent) controllers.incomeSources.add.routes.UKPropertyReportingMethodController.showAgent(testPropertyIncomeId).url else
      controllers.incomeSources.add.routes.UKPropertyReportingMethodController.show(testPropertyIncomeId).url
    val view = ukPropertyAddedView(viewModel, isAgent, backUrl)
    val document = Jsoup.parse(contentAsString(view))
  }

  "UKPropertyReportingMethodView - Individual" should {
    "display the confirmation banner" in new Setup(false) {
      println(document)
      document.getElementById("") shouldBe ""
    }


  }

}
