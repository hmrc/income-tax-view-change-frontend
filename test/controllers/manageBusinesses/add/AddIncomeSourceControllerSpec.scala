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

package controllers.manageBusinesses.add

import config.featureswitch._
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.SessionTimeoutPredicate
import implicits.ImplicitDateFormatter
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.{MockIncomeSourceDetailsService, MockSessionService}
import models.incomeSourceDetails.viewmodels.AddIncomeSourcesViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testIndividualAuthSuccessWithSaUtrResponse}
import testConstants.BusinessDetailsTestConstants.{businessDetailsViewModel, businessDetailsViewModel2, ceasedBusinessDetailsViewModel}
import testConstants.PropertyDetailsTestConstants.{foreignPropertyDetailsViewModel, ukPropertyDetailsViewModel}
import testUtils.TestSupport

import scala.concurrent.Future
import scala.util.{Failure, Success}

class AddIncomeSourceControllerSpec extends MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate
  with ImplicitDateFormatter
  with MockIncomeSourceDetailsService
  with MockNavBarEnumFsPredicate
  with MockItvcErrorHandler
  with MockFrontendAuthorisedFunctions
  with FeatureSwitching
  with MockSessionService
  with TestSupport {

  val controller = new AddIncomeSourceController(
    app.injector.instanceOf[views.html.manageBusinesses.add.AddIncomeSources],
    app.injector.instanceOf[SessionTimeoutPredicate],
    mockAuthService,
    mockIncomeSourceDetailsService,
    testAuthenticator
  )(app.injector.instanceOf[FrontendAppConfig],
    ec,
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler],
    sessionService = mockSessionService,
    app.injector.instanceOf[MessagesControllerComponents]
  )

  def authenticate(isAgent: Boolean): Unit = {
    if (isAgent) setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
    else setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
  }


  "The AddIncomeSourcesController" should {
    def homePageTest(isAgent: Boolean): Unit = {
      s"redirect an ${if (isAgent) "agent" else "individual"} back to the home page" when {
        "the IncomeSources FS is disabled" in {
          disableAllSwitches()
          isDisabled(IncomeSources)
          mockSingleBISWithCurrentYearAsMigrationYear()
          authenticate(isAgent)
          val result: Future[Result] = if (isAgent) controller.showAgent()(fakeRequestConfirmedClient()) else controller.show()(fakeRequestWithActiveSession)
          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe {
            if (isAgent) Some(controllers.routes.HomeController.showAgent.url) else Some(controllers.routes.HomeController.show().url)
          }
        }
      }
    }
    homePageTest(false)
    homePageTest(true)

    def successTest(isAgent: Boolean): Unit = {
      s"redirect an ${if (isAgent) "agent" else "individual"} to the add income source page" when {
        "user has a Sole Trader Business, a UK property and a Foreign Property" in {
          disableAllSwitches()
          enable(IncomeSources)
          ukPlusForeignPropertyWithSoleTraderIncomeSource()
          authenticate(isAgent)
          setupMockDeleteSession(true)
          when(mockIncomeSourceDetailsService.getAddIncomeSourceViewModel(any()))
            .thenReturn(Success(AddIncomeSourcesViewModel(
              soleTraderBusinesses = List(businessDetailsViewModel, businessDetailsViewModel2),
              ukProperty = Some(ukPropertyDetailsViewModel),
              foreignProperty = None,
              ceasedBusinesses = Nil)))

          val result = if(isAgent) controller.showAgent()(fakeRequestConfirmedClient("AB123456C")) else controller.show()(fakeRequestWithActiveSession)
          status(result) shouldBe Status.OK
        }
      }
    }
    successTest(false)
    successTest(true)

    "redirect a user to the add income source page with no tables or table paragraph text" when {
      "user has no businesses or properties" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockDeleteSession(true)
        when(mockIncomeSourceDetailsService.getAddIncomeSourceViewModel(any()))
          .thenReturn(Success(AddIncomeSourcesViewModel(Nil, None, None, Nil)))

        val result = controller.show()(fakeRequestWithActiveSession)
        val resultAgent = controller.showAgent()(fakeRequestConfirmedClient("AB123456C"))

        val doc: Document = Jsoup.parse(contentAsString(result))
        val docAgent: Document = Jsoup.parse(contentAsString(resultAgent))

        Option(doc.getElementById("sole-trader-businesses-table")).isDefined shouldBe false
        Option(docAgent.getElementById("sole-trader-businesses-table")).isDefined shouldBe false

        Option(doc.getElementById("uk-property-table")).isDefined shouldBe false
        Option(docAgent.getElementById("uk-property-table")).isDefined shouldBe false

        Option(doc.getElementById("foreign-property-table")).isDefined shouldBe false
        Option(docAgent.getElementById("foreign-property-table")).isDefined shouldBe false

        Option(doc.getElementById("ceased-businesses-table")).isDefined shouldBe false
        Option(docAgent.getElementById("ceased-businesses-table")).isDefined shouldBe false

        Option(doc.getElementById("uk-property-p1")).isDefined shouldBe false
        Option(doc.getElementById("foreign-property-p1")).isDefined shouldBe false
      }
    }
    "redirect a user to the add income source page with all tables showing" when {
      "user has a ceased business, sole trader business and uk/foreign property" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockBothPropertyBothBusiness()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockDeleteSession(true)
        when(mockIncomeSourceDetailsService.getAddIncomeSourceViewModel(any()))
          .thenReturn(Success(AddIncomeSourcesViewModel(
            soleTraderBusinesses = List(businessDetailsViewModel),
            ukProperty = Some(ukPropertyDetailsViewModel),
            foreignProperty = Some(foreignPropertyDetailsViewModel),
            ceasedBusinesses = List(ceasedBusinessDetailsViewModel))))

        val result = controller.show()(fakeRequestWithActiveSession)
        val resultAgent = controller.showAgent()(fakeRequestConfirmedClient("AB123456C"))

        val doc: Document = Jsoup.parse(contentAsString(result))
        val docAgent: Document = Jsoup.parse(contentAsString(resultAgent))

        Option(doc.getElementById("sole-trader-businesses-table")).isDefined shouldBe true
        Option(docAgent.getElementById("sole-trader-businesses-table")).isDefined shouldBe true

        Option(doc.getElementById("uk-property-table")).isDefined shouldBe true
        Option(docAgent.getElementById("uk-property-table")).isDefined shouldBe true

        Option(doc.getElementById("foreign-property-table")).isDefined shouldBe true
        Option(docAgent.getElementById("foreign-property-table")).isDefined shouldBe true

        Option(doc.getElementById("ceased-businesses-table")).isDefined shouldBe true
        Option(docAgent.getElementById("ceased-businesses-table")).isDefined shouldBe true

        Option(doc.getElementById("uk-property-p1")).isDefined shouldBe true
        Option(doc.getElementById("foreign-property-p1")).isDefined shouldBe true
      }
    }

  "show error page" when {
    def failReturnTest(isAgent: Boolean): Unit = {
      s"failed to return incomeSourceViewModel for ${if (isAgent) "agent" else "individual"}" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockUkPropertyWithSoleTraderBusiness()
        authenticate(isAgent)

        when(mockIncomeSourceDetailsService.getAddIncomeSourceViewModel(any()))
          .thenReturn(Failure(new Exception("UnknownError")))

        val result = if (isAgent) controller.showAgent()(fakeRequestConfirmedClient("AB123456C")) else controller.show()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    failReturnTest(false)
    failReturnTest(true)
  }
}
}