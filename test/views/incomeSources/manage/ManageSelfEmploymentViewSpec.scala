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

package views.incomeSources.manage

import testUtils.TestSupport
import forms.incomeSources.add.AddBusinessReportingMethodForm
import models.incomeSourceDetails.viewmodels.{BusinessReportingMethodViewModel, ViewBusinessDetailsViewModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.mvc.Call
import play.test.Helpers.contentAsString
import play.twirl.api.HtmlFormat
import testUtils.TestSupport
import views.html.incomeSources.manage.ManageSelfEmployment

class ManageSelfEmploymentViewSpec extends TestSupport {

  val manageSelfEmploymentView: ManageSelfEmployment = app.injector.instanceOf[ManageSelfEmployment]

  val unknown = messages("incomeSources.generic.unknown")
  val heading = messages("incomeSources.manage.business-manage-details.heading")
  val soleTrader = messages("incomeSources.manage.business-manage-details.sole-trader-section")
  val businessName = messages("incomeSources.manage.business-manage-details.business-name")
  val businessAddress = messages("incomeSources.manage.business-manage-details.business-address")
  val dateStarted = messages("incomeSources.manage.business-manage-details.date-started")
  val accountingMethod = messages("incomeSources.manage.business-manage-details.accounting-method")
  val reportingMethod = messages("incomeSources.manage.business-manage-details.reporting-method")
  val change = messages("incomeSources.manage.business-manage-details.change")
  val quarterly = messages("incomeSources.manage.business-manage-details.quarterly")
  val annually = messages("incomeSources.manage.business-manage-details.annually")
  val cash = messages("incomeSources.manage.business-manage-details.cash-accounting")
  val traditional = messages("incomeSources.manage.business-manage-details.traditional-accounting")

  val viewModel: ViewBusinessDetailsViewModel = ???


  class Setup(isAgent: Boolean, error: Boolean = false) {
    val backUrl: Call = if (isAgent) {
      controllers.incomeSources.manage.routes.ManageIncomeSourceController.showAgent()
    } else {
      controllers.incomeSources.manage.routes.ManageIncomeSourceController.show()
    }

    def changeReportingMethodUrl(id: String, taxYear: String, changeTo: String): String = {
      if (isAgent) {
        controllers.incomeSources.manage.routes.ChangeBusinessReportingMethodController.showAgent(id, taxYear, changeTo: String).url
      } else {
        controllers.incomeSources.manage.routes.ChangeBusinessReportingMethodController.show(id, taxYear, changeTo: String).url
      }
    }

//    lazy val view: HtmlFormat.Appendable = {
//      manageSelfEmploymentView(
//
//      )
//    }
  }
}
