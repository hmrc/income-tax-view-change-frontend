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

package controllers.incomeSources.add

import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.SessionTimeoutPredicate
import controllers.routes
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.{MockClientDetailsService, MockSessionService}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers._
import play.api.mvc.{Call, MessagesControllerComponents, Result}
import play.api.test.Helpers._
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testIndividualAuthSuccessWithSaUtrResponse}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{businessesAndPropertyIncome, ukPlusForeignPropertyAndSoleTraderNoLatency}
import testUtils.TestSupport
import views.html.incomeSources.add.IncomeSourceReportingMethodNotSaved

import scala.concurrent.Future

class IncomeSourceReportingMethodNotSavedControllerSpec extends TestSupport
  with MockFrontendAuthorisedFunctions
  with MockIncomeSourceDetailsPredicate
  with MockAuthenticationPredicate
  with MockItvcErrorHandler
  with MockNavBarEnumFsPredicate
  with MockClientDetailsService
  with FeatureSwitching
  with MockSessionService {

  val view: IncomeSourceReportingMethodNotSaved = app.injector.instanceOf[IncomeSourceReportingMethodNotSaved]
  val postAction: Call = controllers.incomeSources.add.routes.AddBusinessNameController.submit()

  def authenticate(isAgent: Boolean): Unit = {
    if (isAgent) setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
    else setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
  }

  val incomeSourceTypes: Seq[IncomeSourceType with Serializable] = List(SelfEmployment, UkProperty, ForeignProperty)

  def getParagraphText(incomeSourceType: IncomeSourceType): String = {
    incomeSourceType match {
      case SelfEmployment => TestConstants.paragraphTextSelfEmployment
      case UkProperty => TestConstants.paragraphTextUkProperty
      case ForeignProperty => TestConstants.paragraphTextForeignProperty
    }
  }

  def getContinueButtonLink(incomeSourceType: IncomeSourceType, isAgent: Boolean): String = {
    (incomeSourceType, isAgent) match {
      case (SelfEmployment, false) => TestConstants.selfEmploymentAddedUrl
      case (UkProperty, false) => TestConstants.ukPropertyAddedUrl
      case (ForeignProperty, false) => TestConstants.foreignPropertyAddedUrl
      case (SelfEmployment, true) => TestConstants.selfEmploymentAddedAgentUrl
      case (UkProperty, true) => TestConstants.ukPropertyAddedAgentUrl
      case (ForeignProperty, true) => TestConstants.foreignPropertyAddedAgentUrl
    }
  }

  def getPageTitle(isAgent: Boolean): String = {
    if (isAgent) {
      TestConstants.titleAgent
    } else {
      TestConstants.titleIndividual
    }
  }

  object TestConstants {
    val title: String = messages("incomeSources.add.error.standardError")
    val titleAgent: String = s"${messages("htmlTitle.agent", title)}"
    val titleIndividual: String = s"${messages("htmlTitle", title)}"

    val selfEmployment: String = messages("incomeSources.add.error.reportingMethodNotSaved.se")
    val ukProperty: String = messages("incomeSources.add.error.reportingMethodNotSaved.uk")
    val foreignProperty: String = messages("incomeSources.add.error.reportingMethodNotSaved.fp")
    val paragraphTextSelfEmployment: String = messages("incomeSources.add.error.reportingMethodNotSaved.p1", selfEmployment)
    val paragraphTextUkProperty: String = messages("incomeSources.add.error.reportingMethodNotSaved.p1", ukProperty)
    val paragraphTextForeignProperty: String = messages("incomeSources.add.error.reportingMethodNotSaved.p1", foreignProperty)

    val selfEmploymentAddedUrl: String = controllers.incomeSources.add.routes.IncomeSourceAddedController.show(SelfEmployment).url
    val ukPropertyAddedUrl: String = controllers.incomeSources.add.routes.IncomeSourceAddedController.show(UkProperty).url
    val foreignPropertyAddedUrl: String = controllers.incomeSources.add.routes.IncomeSourceAddedController.show(ForeignProperty).url

    val selfEmploymentAddedAgentUrl: String = controllers.incomeSources.add.routes.IncomeSourceAddedController.showAgent(SelfEmployment).url
    val ukPropertyAddedAgentUrl: String = controllers.incomeSources.add.routes.IncomeSourceAddedController.showAgent(UkProperty).url
    val foreignPropertyAddedAgentUrl: String = controllers.incomeSources.add.routes.IncomeSourceAddedController.showAgent(ForeignProperty).url
  }

  object TestIncomeSourceReportingMethodNotSavedController
    extends IncomeSourceReportingMethodNotSavedController(
      authorisedFunctions = mockAuthService,
      view = view,
      testAuthenticator
    )(
      mcc = app.injector.instanceOf[MessagesControllerComponents],
      appConfig = app.injector.instanceOf[FrontendAppConfig],
      itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
      itvcAgentErrorHandler = app.injector.instanceOf[AgentItvcErrorHandler],
      ec = ec
    )

  for (isAgent <- Seq(true, false)) yield {
    for (incomeSourceType <- incomeSourceTypes) yield {
      s"${if (isAgent) "Agent" else "individual"} - IncomeSourceReportingMethodNotSavedController.${if (isAgent) "showAgent" else "show"}" should {
        "return 200 OK" when {
          s"business type is $incomeSourceType" in {
            disableAllSwitches()
            enable(IncomeSources)
            authenticate(isAgent = isAgent)
            setupMockGetIncomeSourceDetails()(ukPlusForeignPropertyAndSoleTraderNoLatency)

            val result: Future[Result] = if (isAgent)
              TestIncomeSourceReportingMethodNotSavedController.showAgent(incomeSourceType)(fakeRequestConfirmedClient())
            else TestIncomeSourceReportingMethodNotSavedController.show(incomeSourceType)(fakeRequestWithActiveSession)

            val document: Document = Jsoup.parse(contentAsString(result))
            status(result) mustBe OK
            document.title shouldBe getPageTitle(isAgent)
            document.getElementById("paragraph-1").text() shouldBe getParagraphText(incomeSourceType)
            document.getElementById("continue-button").attr("href") shouldBe getContinueButtonLink(incomeSourceType, isAgent)
          }
        }

        s"return 303 and redirect to the sign in for <incomeSourceType: $incomeSourceType>" when {
          "the user is not authenticated" in {
            if(isAgent) setupMockAgentAuthorisationException() else setupMockAuthorisationException()
            val result = if (isAgent) TestIncomeSourceReportingMethodNotSavedController.showAgent(incomeSourceType)(fakeRequestConfirmedClient())
            else TestIncomeSourceReportingMethodNotSavedController.show(incomeSourceType)(fakeRequestWithActiveSession)

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
          }
        }

        s"redirect to the session timeout page for <incomeSourceType: $incomeSourceType>" when {
          "the user has timed out" in {
            if(isAgent) setupMockAgentAuthorisationException() else setupMockAuthorisationException()
            val result = if(isAgent) TestIncomeSourceReportingMethodNotSavedController.showAgent(incomeSourceType)(fakeRequestConfirmedClientTimeout())
            else TestIncomeSourceReportingMethodNotSavedController.show(incomeSourceType)(fakeRequestWithTimeoutSession)
            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout.url)
          }
        }

        s"return 303 and show home page for <incomeSourceType: $incomeSourceType>" when {
          "when feature switch is disabled" in {
            disableAllSwitches()

            authenticate(isAgent)
            setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

            val result: Future[Result] = if (isAgent)
              TestIncomeSourceReportingMethodNotSavedController.showAgent(incomeSourceType)(fakeRequestConfirmedClient())
            else TestIncomeSourceReportingMethodNotSavedController.show(incomeSourceType)(fakeRequestWithActiveSession)

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) mustBe (if(isAgent) Some(routes.HomeController.showAgent.url) else Some(routes.HomeController.show().url))
          }
        }
      }
    }
  }

}
