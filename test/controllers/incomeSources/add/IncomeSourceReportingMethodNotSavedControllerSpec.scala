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
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
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
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
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

  "Individual - IncomeSourceReportingMethodNotSavedController.show" should {
    "return 200 OK" when {
      "business type is self employment" in {
        disableAllSwitches()
        enable(IncomeSources)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestIncomeSourceReportingMethodNotSavedController.show(SelfEmployment)(fakeRequestWithActiveSession)
        val document: Document = Jsoup.parse(contentAsString(result))
        status(result) mustBe OK
        document.title shouldBe TestConstants.titleIndividual
        document.getElementById("paragraph-1").text() shouldBe TestConstants.paragraphTextSelfEmployment
        document.getElementById("continue-button").attr("href") shouldBe TestConstants.selfEmploymentAddedUrl
      }

      "business type is UK property" in {
        disableAllSwitches()
        enable(IncomeSources)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestIncomeSourceReportingMethodNotSavedController.show(UkProperty)(fakeRequestWithActiveSession)
        val document: Document = Jsoup.parse(contentAsString(result))
        status(result) mustBe OK
        document.title shouldBe TestConstants.titleIndividual
        document.getElementById("paragraph-1").text() shouldBe TestConstants.paragraphTextUkProperty
        document.getElementById("continue-button").attr("href") shouldBe TestConstants.ukPropertyAddedUrl
      }

      "business type is foreign property" in {
        disableAllSwitches()
        enable(IncomeSources)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestIncomeSourceReportingMethodNotSavedController.show(ForeignProperty)(fakeRequestWithActiveSession)
        val document: Document = Jsoup.parse(contentAsString(result))
        status(result) mustBe OK
        document.title shouldBe TestConstants.titleIndividual
        document.getElementById("paragraph-1").text() shouldBe TestConstants.paragraphTextForeignProperty
        document.getElementById("continue-button").attr("href") shouldBe TestConstants.foreignPropertyAddedUrl
      }
    }

    "return 303 and redirect to the sign in" when {
      "the user is not authenticated" in {
        setupMockAuthorisationException()
        val result = TestIncomeSourceReportingMethodNotSavedController.show(ForeignProperty)(fakeRequestWithActiveSession)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
      }
    }
    "redirect to the session timeout page" when {
      "the user has timed out" in {
        setupMockAuthorisationException()
        val result = TestIncomeSourceReportingMethodNotSavedController.show(ForeignProperty)(fakeRequestWithTimeoutSession)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout.url)
      }
    }

    "return 303 and show home page" when {
      "when feature switch is disabled" in {
        disableAllSwitches()

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestIncomeSourceReportingMethodNotSavedController.show(ForeignProperty)(fakeRequestWithActiveSession)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.HomeController.show().url)
      }
    }
  }

  "Agent - IncomeSourceReportingMethodNotSavedController.showAgent" should {
    "return 200 OK" when {
      "business type is self employment" in {
        disableAllSwitches()
        enable(IncomeSources)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestIncomeSourceReportingMethodNotSavedController.showAgent(SelfEmployment)(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))
        status(result) mustBe OK
        document.title shouldBe TestConstants.titleAgent
        document.getElementById("paragraph-1").text() shouldBe TestConstants.paragraphTextSelfEmployment
        document.getElementById("continue-button").attr("href") shouldBe TestConstants.selfEmploymentAddedAgentUrl
      }

      "business type is UK property" in {
        disableAllSwitches()
        enable(IncomeSources)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestIncomeSourceReportingMethodNotSavedController.showAgent(UkProperty)(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))
        status(result) mustBe OK
        document.title shouldBe TestConstants.titleAgent
        document.getElementById("paragraph-1").text() shouldBe TestConstants.paragraphTextUkProperty
        document.getElementById("continue-button").attr("href") shouldBe TestConstants.ukPropertyAddedAgentUrl
      }

      "business type is foreign property" in {
        disableAllSwitches()
        enable(IncomeSources)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestIncomeSourceReportingMethodNotSavedController.showAgent(ForeignProperty)(fakeRequestConfirmedClient())
        val document: Document = Jsoup.parse(contentAsString(result))
        status(result) mustBe OK
        document.title shouldBe TestConstants.titleAgent
        document.getElementById("paragraph-1").text() shouldBe TestConstants.paragraphTextForeignProperty
        document.getElementById("continue-button").attr("href") shouldBe TestConstants.foreignPropertyAddedAgentUrl
      }
    }
    "return 303 and redirect to the sign in" when {
      "the user is not authenticated" in {
        setupMockAgentAuthorisationException()
        val result = TestIncomeSourceReportingMethodNotSavedController.showAgent(ForeignProperty)(fakeRequestConfirmedClient())
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
      }
    }
    "return 303 and show home page" when {
      "when feature switch is disabled" in {
        disableAllSwitches()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestIncomeSourceReportingMethodNotSavedController.showAgent(ForeignProperty)(fakeRequestConfirmedClient())
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.HomeController.showAgent.url)
      }
    }
  }
}
