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
import enums.IncomeSourceJourney.ForeignProperty.{reportingMethodChangeErrorPrefix => foreignFormError}
import enums.IncomeSourceJourney.SelfEmployment.{reportingMethodChangeErrorPrefix => seFormError}
import enums.IncomeSourceJourney.UkProperty.{reportingMethodChangeErrorPrefix => ukFormError}
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import forms.manageBusinesses.manage.ChangeReportingMethodForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.{Form, FormError}
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


  class Setup(isAgent: Boolean, incomeSourceType: IncomeSourceType, newReportingMethod: String, isCYPlus: Boolean, contentFeatureSwitchEnabled: Boolean) {

    private lazy val manageIncomeSourceDetailsController = controllers.manageBusinesses.manage.routes.ManageIncomeSourceDetailsController

    val testChangeToAnnual = "annual"

    val testChangeToQuarterly = "quarterly"

    val formFieldName = "incomeSources.manage.propertyReportingMethod"

    val taxYearSet = if(isCYPlus)("2026","2027") else ("2025", "2026")

    def getFormErrorMessage(incomeSourceType: IncomeSourceType): String = {
      incomeSourceType match {
        case SelfEmployment => seFormError
        case ForeignProperty => foreignFormError
        case UkProperty => ukFormError
      }
    }

    val selfEmploymentId = incomeSourceType match {
      case SelfEmployment => Some(testSelfEmploymentId)
      case _ => None
    }

    val backUrl = manageIncomeSourceDetailsController.show(isAgent, incomeSourceType, selfEmploymentId).url
    val pageSubHeading = incomeSourceType match {
      case SelfEmployment => "Sole trader"
      case UkProperty => "UK property"
      case ForeignProperty => "Foreign property"
    }

    //new messages (content R17 FS enabled)
    def getPageHeadingFor(reportingMethod: String, CYPlus: Boolean): String = {
      (reportingMethod, CYPlus) match {
        case ("annual", false) => "Opt out of Making Tax Digital for Income Tax for the 2025 to 2026 tax year"
        case ("annual", true) => "Opt out of Making Tax Digital for Income Tax for the 2026 to 2027 tax year"
        case ("quarterly", false) => "Signing up to Making Tax Digital for Income Tax for the 2025 to 2026 tax year"
        case ("quarterly", true) => "Signing up to Making Tax Digital for Income Tax for the 2026 to 2027 tax year"
      }
    }
    val pageDescriptionAnnual = "This will mean you no longer need to submit quarterly updates through compatible software for this income source."
    val pageDescriptionQuarterly = "This will mean you need to submit quarterly updates through compatible software for this income source."
    val pageInsetAnnual = "If for this tax year you have already submitted to HMRC any quarterly updates for this new business, you will need to resubmit this information in your tax return."
    val pageUlDescription = "This will mean you:"
    val pageUl1 = "need to submit quarterly updates for this income source"
    val pageUl2 = "could have at least one quarterly update overdue"
    val pageInsetQuarterly = "If for this tax year you have already submitted to HMRC a quarterly update for this new business, you will need to resubmit this information in your next quarterly update."
    val pageFormHeadingSignUp = s"Do you want to sign up for the ${taxYearSet._1} to ${taxYearSet._2} tax year?"
    val pageFormHeadingOptOut = s"Do you want to opt out of the ${taxYearSet._1} to ${taxYearSet._2} tax year?"
    val pageContinue = "Continue"


    //old messages (content R17 FS disabled)
    val oldPageHeading = s"Change to $newReportingMethod reporting for ${taxYearSet._1} to ${taxYearSet._2} tax year"
    val oldPageDescription = if (newReportingMethod == "quarterly") "Changing to quarterly reporting will mean you need to submit your quarterly updates through compatible software."
    else "If you change to annual reporting, you can submit your tax return through your HMRC online account or compatible software."
    val oldPageInset = "If you have submitted any income and expenses for this tax year to HMRC, this will be deleted from our records. So make sure you keep hold of this information because you will need to include it in your quarterly updates."
    val oldPageConfirm = "Confirm and save"


    def form(changeTo: String): Form[ChangeReportingMethodForm] = ChangeReportingMethodForm(changeTo)

    lazy val view: HtmlFormat.Appendable =
      confirmReportingMethodView(
        postAction = Call("POST", "/"),
        isAgent = isAgent,
        backUrl = backUrl,
        taxYearStartYear = taxYearSet._1,
        taxYearEndYear = taxYearSet._2,
        newReportingMethod = newReportingMethod,
        isCurrentTaxYear = !isCYPlus,
        incomeSourceType = incomeSourceType,
        form = form(testChangeToQuarterly).withError(FormError(formFieldName, getFormErrorMessage(incomeSourceType))),
        optInOutContentFSEnabled = contentFeatureSwitchEnabled
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

    s"ConfirmReportingMethodView (new content - R17 content feature switch enabled) - $incomeSourceType - $mtdRole - $reportingMethod" should {
      "render the heading" in new Setup(
        isAgent,
        incomeSourceType,
        reportingMethod,
        isCYPlus = false,
        contentFeatureSwitchEnabled = true
      ) {
        document.getElementsByClass("govuk-heading-xl").first().text() shouldBe getPageHeadingFor(reportingMethod = reportingMethod, CYPlus = false)
      }

      "render the back link with the correct URL" in new Setup(
        isAgent,
        incomeSourceType,
        reportingMethod,
        isCYPlus = false,
        contentFeatureSwitchEnabled = true
      ) {
        document.getElementById("back-fallback").text() shouldBe "Back"
        document.getElementById("back-fallback").attr("href") shouldBe controllers.manageBusinesses.manage.routes.ManageIncomeSourceDetailsController.show(isAgent = isAgent, incomeSourceType, selfEmploymentId).url
      }

      "render the sub-heading" in new Setup(
        isAgent,
        incomeSourceType,
        reportingMethod,
        isCYPlus = false,
        contentFeatureSwitchEnabled = true
      ) {
        document.getElementsByClass("govuk-caption-l").first().text().contains(pageSubHeading) shouldBe true
      }

      "render the continue button" in new Setup(
        isAgent,
        incomeSourceType,
        reportingMethod,
        isCYPlus = false,
        contentFeatureSwitchEnabled = true
      ) {
        document.getElementById("continue-button").text() shouldBe pageContinue
      }

      "render the CY quarterly content if user is switching to quarterly else render the annual content" in
        new Setup(
          isAgent,
          incomeSourceType,
          reportingMethod,
          isCYPlus = false,
          contentFeatureSwitchEnabled = true
        ) {
        if (reportingMethod == "quarterly") {
          document.getElementById("change-reporting-method-ul-description").text() shouldBe pageUlDescription
          document.getElementById("change-reporting-method-ul-li1").text() shouldBe pageUl1
          document.getElementById("change-reporting-method-ul-li2").text() shouldBe pageUl2
          document.getElementById("change-reporting-method-inset").text() shouldBe pageInsetQuarterly


          Option(document.getElementById("change-reporting-method-description-quarterly-CYplus")).isEmpty shouldBe true
        } else {
          document.getElementById("change-reporting-method-description-annual").text() shouldBe pageDescriptionAnnual
          document.getElementById("change-reporting-method-inset").text() shouldBe pageInsetAnnual

          Option(document.getElementById("change-reporting-method-description-quarterly-CYplus")).isEmpty shouldBe true
          Option(document.getElementById("change-reporting-method-ul-description")).isEmpty shouldBe true
          Option(document.getElementById("change-reporting-method-ul-li1")).isEmpty shouldBe true
          Option(document.getElementById("change-reporting-method-ul-li2")).isEmpty shouldBe true
        }
      }

      "render the CY+1 quarterly content if user is switching to quarterly else render the annual content" in
        new Setup(
          isAgent,
          incomeSourceType,
          reportingMethod,
          isCYPlus = true,
          contentFeatureSwitchEnabled = true
        ) {
          if (reportingMethod == "quarterly") {
            document.getElementById("change-reporting-method-description-quarterly-CYplus").text() shouldBe pageDescriptionQuarterly

            Option(document.getElementById("change-reporting-method-ul-description")).isEmpty shouldBe true
            Option(document.getElementById("change-reporting-method-ul-li1")).isEmpty shouldBe true
            Option(document.getElementById("change-reporting-method-ul-li2")).isEmpty shouldBe true
          } else {
            document.getElementById("change-reporting-method-description-annual").text() shouldBe pageDescriptionAnnual

            Option(document.getElementById("change-reporting-method-ul-description")).isEmpty shouldBe true
            Option(document.getElementById("change-reporting-method-ul-li1")).isEmpty shouldBe true
            Option(document.getElementById("change-reporting-method-ul-li2")).isEmpty shouldBe true
          }
        }
      "render the correct form heading" in
        new Setup(
          isAgent = isAgent,
          incomeSourceType,
          reportingMethod,
          isCYPlus = true,
          true
        ) {
          if(reportingMethod == "quarterly") {
            document.getElementsByClass("govuk-fieldset__heading").select("h1").text() shouldBe pageFormHeadingSignUp
          }else{
            document.getElementsByClass("govuk-fieldset__heading").select("h1").text() shouldBe pageFormHeadingOptOut
          }
        }
    }
  }

  //old content tests
  for {
    mtdRole <- List("Individual", "Agent")
    incomeSourceType <- List(SelfEmployment, ForeignProperty, UkProperty)
    reportingMethod <- List("annual", "quarterly")
  } yield {
    val isAgent = mtdRole == "Agent"

    s"ConfirmReportingMethodView (old content - R17 content feature switch disabled) - $incomeSourceType - $mtdRole - $reportingMethod" should {
      "render the heading" in new Setup(
        isAgent = isAgent,
        incomeSourceType = incomeSourceType,
        reportingMethod,
        isCYPlus = false,
        contentFeatureSwitchEnabled = false
      ) {
        document.getElementsByClass("govuk-heading-xl").first().text() shouldBe oldPageHeading
      }

      "render the back link with the correct URL" in new Setup(
        isAgent = isAgent,
        incomeSourceType = incomeSourceType,
        reportingMethod,
        isCYPlus = false,
        contentFeatureSwitchEnabled = false
      ) {
        document.getElementById("back-fallback").text() shouldBe "Back"
        document.getElementById("back-fallback").attr("href") shouldBe controllers.manageBusinesses.manage.routes.ManageIncomeSourceDetailsController.show(isAgent = isAgent, incomeSourceType, selfEmploymentId).url
      }

      "render the sub-heading" in new Setup(
        isAgent = isAgent,
        incomeSourceType = incomeSourceType,
        reportingMethod,
        isCYPlus = false,
        contentFeatureSwitchEnabled = false
      ) {
        document.getElementsByClass("govuk-caption-l").first().text().contains(pageSubHeading) shouldBe true
      }

      "render the main paragraph" in new Setup(
        isAgent = isAgent,
        incomeSourceType = incomeSourceType,
        reportingMethod,
        isCYPlus = false,
        contentFeatureSwitchEnabled = false
      ) {
        document.getElementById("change-reporting-method-description").text() shouldBe oldPageDescription
      }

      "render the continue button" in new Setup(
        isAgent = isAgent,
        incomeSourceType = incomeSourceType,
        reportingMethod,
        isCYPlus = false,
        contentFeatureSwitchEnabled = false
      ) {
        document.getElementById("confirm-button").text() shouldBe oldPageConfirm
      }

      "render the inset text if the user is quarterly" in new Setup(
        isAgent = isAgent,
        incomeSourceType = incomeSourceType,
        reportingMethod,
        isCYPlus = false,
        contentFeatureSwitchEnabled = false
      ) {
        if (reportingMethod == "quarterly") {
          document.getElementById("change-reporting-method-inset").text() shouldBe oldPageInset
        } else {
          Option(document.getElementById("change-reporting-method-inset")).isEmpty shouldBe true
        }
      }
    }
  }
}
