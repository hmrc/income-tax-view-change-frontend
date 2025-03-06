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

package views.manageBusinesses.manage

import auth.MtdItUser
import authV2.AuthActionsTestData.defaultMTDITUser
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.Call
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.twirl.api.HtmlFormat
import testConstants.BaseTestConstants.testSelfEmploymentId
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.ukPlusForeignPropertyWithSoleTraderIncomeSource
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import views.html.manageBusinesses.manage.ConfirmReportingMethod

class ConfirmReportingMethodSharedControllerViewSpec extends TestSupport {

  val confirmReportingMethodView: ConfirmReportingMethod = app.injector.instanceOf[ConfirmReportingMethod]

  val testUser: MtdItUser[_] = defaultMTDITUser(Some(Individual),
    ukPlusForeignPropertyWithSoleTraderIncomeSource, fakeRequestNoSession)

  val testTaxYear = "2021-2022"
  val testTaxYearStartYear = "2021"
  val testTaxYearEndYear = "2022"

  class Setup(isAgent: Boolean, incomeSourceType: IncomeSourceType, newReportingMethod: String) {

    private lazy val manageIncomeSourceDetailsController = controllers.manageBusinesses.manage.routes.ManageIncomeSourceDetailsController

    val selfEmploymentId = incomeSourceType match {
      case SelfEmployment => Some(testSelfEmploymentId)
      case _ => None
    }

    val backUrl = manageIncomeSourceDetailsController.show(isAgent, incomeSourceType, selfEmploymentId).url

    val pageHeading = s"Change to $newReportingMethod reporting for 2021 to 2022 tax year"
    val pageSubHeading = incomeSourceType match {
      case SelfEmployment => "Sole trader"
      case UkProperty => "UK property"
      case ForeignProperty => "Foreign property"
    }
    val pageDescription = if (newReportingMethod == "quarterly") "Changing to quarterly reporting will mean you need to submit your quarterly updates through compatible software."
    else "If you change to annual reporting, you can submit your tax return through your HMRC online account or compatible software."
    val pageInset = "If you have submitted any income and expenses for this tax year to HMRC, this will be deleted from our records. So make sure you keep hold of this information because you will need to include it in your quarterly updates."
    val pageConfirm = "Confirm and save"

    lazy val view: HtmlFormat.Appendable =
      confirmReportingMethodView(
        postAction = Call("POST", "/"),
        isAgent = isAgent,
        backUrl = backUrl,
        taxYearStartYear = testTaxYearStartYear,
        taxYearEndYear = testTaxYearEndYear,
        newReportingMethod = newReportingMethod,
        isCurrentTaxYear = true,
        incomeSourceType = incomeSourceType
      )

    lazy val document: Document = {
      Jsoup.parse(contentAsString(view))
    }
  }

  for {
    mtdRole <- List("Individual", "Agent")
    incomeSourceType <- List(SelfEmployment, ForeignProperty, UkProperty)
    reportingMethod <- List("annual", "quarterly")
  } yield {
    val isAgent = mtdRole == "Agent"

    s"ConfirmReportingMethodView - $incomeSourceType - $mtdRole - $reportingMethod" should {
      "render the heading" in new Setup(isAgent = isAgent, incomeSourceType = incomeSourceType, reportingMethod) {
        document.getElementsByClass("govuk-heading-l").first().text() shouldBe pageHeading
      }
      "render the back link with the correct URL" in new Setup(isAgent = isAgent, incomeSourceType = incomeSourceType, reportingMethod) {
        document.getElementById("back-fallback").text() shouldBe "Back"
        document.getElementById("back-fallback").attr("href") shouldBe controllers.manageBusinesses.manage.routes.ManageIncomeSourceDetailsController.show(isAgent = isAgent, incomeSourceType, selfEmploymentId).url
      }
      "render the sub-heading" in new Setup(isAgent = isAgent, incomeSourceType = incomeSourceType, reportingMethod) {
        document.getElementsByClass("govuk-caption-l").first().text().contains(pageSubHeading) shouldBe true
      }
      "render the main paragraph" in new Setup(isAgent = isAgent, incomeSourceType = incomeSourceType, reportingMethod) {
        document.getElementById("change-reporting-method-description").text() shouldBe pageDescription
      }
      "render the continue button" in new Setup(isAgent = isAgent, incomeSourceType = incomeSourceType, reportingMethod) {
        document.getElementById("confirm-button").text() shouldBe pageConfirm
      }
      "render the inset text if the user is quarterly" in new Setup(isAgent = isAgent, incomeSourceType = incomeSourceType, reportingMethod) {
        if (reportingMethod == "quarterly") {
          document.getElementById("change-reporting-method-inset").text() shouldBe pageInset
        } else {
          Option(document.getElementById("change-reporting-method-inset")).isEmpty shouldBe true
        }
      }
    }
  }
}
