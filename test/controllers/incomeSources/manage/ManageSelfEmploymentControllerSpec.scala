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

package controllers.incomeSources.manage

import config.featureswitch.FeatureSwitch.switches
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import mocks.connectors.MockIncomeTaxViewChangeConnector
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Mockito.{mock, when}
import org.mockito.ArgumentMatchers.any
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status}
import services.{CalculationListService, DateService, ITSAStatusService}
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testSelfEmploymentId, testTaxCalculationId}
import testUtils.TestSupport
import views.html.incomeSources.manage.BusinessManageDetails

import scala.concurrent.Future

class ManageSelfEmploymentControllerSpec extends TestSupport with MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with FeatureSwitching with MockIncomeTaxViewChangeConnector with MockNavBarEnumFsPredicate
{

  val mockDateService: DateService = mock(classOf[DateService])
  val mockITSAStatusService: ITSAStatusService = mock(classOf[ITSAStatusService])


  def disableAllSwitches(): Unit = {
    switches.foreach(switch => disable(switch))
  }

  object TestManageSelfEmploymentController extends ManageSelfEmploymentController (
    view = app.injector.instanceOf[BusinessManageDetails],
    checkSessionTimeout = app.injector.instanceOf[SessionTimeoutPredicate],
    authenticate = MockAuthenticationPredicate,
    authorisedFunctions = mockAuthService,
    retrieveNino = app.injector.instanceOf[NinoPredicate],
    retrieveIncomeSources = MockIncomeSourceDetailsPredicate,
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
    incomeSourceDetailsService = mockIncomeSourceDetailsService,
    mockITSAStatusService,
    mockDateService,
    retrieveBtaNavBar = MockNavBarPredicate,
    calculationListService = app.injector.instanceOf[CalculationListService]
  )(
    ec,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    appConfig = app.injector.instanceOf[FrontendAppConfig]
  ) {
    val heading: String = messages("incomeSources.manage.business-manage-details.heading")
    val title: String = s"${messages("htmlTitle", heading)}"
    val link: String = s"${messages("incomeSources.manage.business-manage-details.change")}"
    val incomeSourceId: String = "XAIS00000000008"
  }

  object Scenario extends Enumeration {
    type Scenario = Value
    val NO_LATENCY_INFORMATION, NON_ELIGIBLE_ITS_STATUS, FIRST_AND_SECOND_YEAR_CRYSTALLIZED,
    FIRST_AND_SECOND_YEAR_NOT_CRYSTALLIZED, FIRST_YEAR_CRYSTALLISED_SECOND_NOT, SECOND_YEAR_CRYSTALLISED_FIRST_NOT,
    CURRENT_TAX_YEAR_IN_LATENCY_YEARS, LATENCY_PERIOD_EXPIRED, CURRENT_TAX_2024_YEAR_IN_LATENCY_YEARS = Value
  }

  import Scenario._

  def mockAndBasicSetup(scenario: Scenario, isAgent: Boolean = false): Unit = {
    disableAllSwitches()
    if (isAgent) {
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
    } else {
      setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
    }

    scenario match {
      case NO_LATENCY_INFORMATION =>
        when(mockDateService.getCurrentTaxYearEnd(any)).thenReturn(2024)
        when(TestManageSelfEmploymentController.itsaStatusService.hasMandatedOrVoluntaryStatusCurrentYear(any, any, any))
          .thenReturn(Future.successful(true))
        mockSingleBusinessIncomeSource()

      case FIRST_AND_SECOND_YEAR_CRYSTALLIZED =>
        when(mockDateService.getCurrentTaxYearEnd(any)).thenReturn(2023)
        when(TestManageSelfEmploymentController.itsaStatusService.hasMandatedOrVoluntaryStatusCurrentYear(any, any, any))
          .thenReturn(Future.successful(true))
        when(TestManageSelfEmploymentController.calculationListService.isTaxYearCrystallised(2023)(any, any, any))
          .thenReturn(Future.successful(Some(true)))
        when(TestManageSelfEmploymentController.calculationListService.isTaxYearCrystallised(2024)(any, any, any))
          .thenReturn(Future.successful(Some(true)))
        mockBusinessIncomeSourceWithLatency2023()

      case FIRST_AND_SECOND_YEAR_NOT_CRYSTALLIZED =>
        when(mockDateService.getCurrentTaxYearEnd(any)).thenReturn(2023)
        when(TestManageSelfEmploymentController.itsaStatusService.hasMandatedOrVoluntaryStatusCurrentYear(any, any, any))
          .thenReturn(Future.successful(true))
        when(TestManageSelfEmploymentController.calculationListService.isTaxYearCrystallised(2023)(any, any, any))
          .thenReturn(Future.successful(Some(false)))
        when(TestManageSelfEmploymentController.calculationListService.isTaxYearCrystallised(2024)(any, any, any))
          .thenReturn(Future.successful(Some(false)))
        mockBusinessIncomeSourceWithLatency2023()

      case NON_ELIGIBLE_ITS_STATUS =>
        when(mockDateService.getCurrentTaxYearEnd(any)).thenReturn(2023)
        when(TestManageSelfEmploymentController.itsaStatusService.hasMandatedOrVoluntaryStatusCurrentYear(any, any, any))
          .thenReturn(Future.successful(false))
        mockBusinessIncomeSourceWithLatency2023()

    }

    enable(IncomeSources)

  }

  "Individual - ManageSelfEmploymentController" should {
    "return 200 OK" when {
      "FS is enabled and the .show(id) method is called with a valid id parameter and no latency information" in {
        mockAndBasicSetup(Scenario.NO_LATENCY_INFORMATION)

        val result: Future[Result] = TestManageSelfEmploymentController.show(testSelfEmploymentId)(fakeRequestWithNino)
        val document: Document = Jsoup.parse(contentAsString(result))
//        println("TTTTTTTTT")
//        println(document)

        status(result) shouldBe Status.OK
        document.title shouldBe TestManageSelfEmploymentController.title
        document.select("h1:nth-child(1)").text shouldBe TestManageSelfEmploymentController.heading
        Option(document.getElementById("change-link-1")) shouldBe None
        Option(document.getElementById("change-link-2")) shouldBe None

      }
    }
  }

}
