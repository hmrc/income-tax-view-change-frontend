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

package controllers

import audit.mocks.MockAuditingService
import config.featureswitch._
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import connectors.IncomeTaxViewChangeConnector
import controllers.predicates.{NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import enums.ChargeType.{ITSA_ENGLAND_AND_NI, NIC4_WALES}
import implicits.ImplicitDateFormatter
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.MockIncomeSourceDetailsService
import models.chargeHistory.{ChargeHistoryResponseModel, ChargesHistoryErrorModel, ChargesHistoryModel}
import models.core.{AccountingPeriodModel, CessationModel}
import models.financialDetails.{FinancialDetail, FinancialDetailsResponseModel}
import models.incomeSourceDetails.viewmodels.IncomeSourcesViewModel
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel, PropertyDetailsModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import services.{DateService, FinancialDetailsService}
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testIndividualAuthSuccessWithSaUtrResponse, testTaxYear}
import testConstants.BusinessDetailsTestConstants.business1
import testConstants.FinancialDetailsTestConstants._
import testConstants.IncomeSourceDetailsTestConstants.singleBusinessIncomeWithCurrentYear
import testUtils.TestSupport

import java.time.{LocalDate, Month}
import scala.concurrent.Future

class AddIncomeSourceControllerSpec extends MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate
  with ImplicitDateFormatter
  with MockIncomeSourceDetailsService
  with MockNavBarEnumFsPredicate
  with MockFrontendAuthorisedFunctions
  with FeatureSwitching
  with TestSupport {

  val controller = new AddIncomeSourceController(
    app.injector.instanceOf[views.html.NewIncomeSources],
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate,
    mockAuthService,
    app.injector.instanceOf[NinoPredicate],
    MockIncomeSourceDetailsPredicate,
    app.injector.instanceOf[AgentItvcErrorHandler],
    mockIncomeSourceDetailsService,
    app.injector.instanceOf[NavBarPredicate]
  )(
    ec,
    app.injector.instanceOf[MessagesControllerComponents],
    app.injector.instanceOf[FrontendAppConfig]
  )


  "The NewIncomeSourcesController" should {

    "redirect a user back to the home page" when {

      "the IncomeSources FS is disabled" in {
        isDisabled(IncomeSources)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.AddIncomeSourceController.show().url)
      }
      "redirect an agent back to the home page" when {

        "the IncomeSources FS is disabled" in {
          isDisabled(IncomeSources)
          mockSingleBISWithCurrentYearAsMigrationYear()
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

          val result: Future[Result] = controller.showAgent()(fakeRequestConfirmedClient())
          status(result) shouldBe Status.SEE_OTHER
        }
      }
    }
    "redirect a user to the new income sources page" when {

      "the IncomeSources FS is enabled" in {
        isEnabled(IncomeSources)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

        when(mockIncomeSourceDetailsService.incomeSourcesAsViewModel(any()))
          .thenReturn(IncomeSourcesViewModel(
            soleTraderBusinesses = List(business1), None, None, Nil))

        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.OK
      }
    }
  }



  //  val business1 = BusinessDetailsModel(
  //    incomeSourceId = Some("XA00001234"),
  //    accountingPeriod = Some(AccountingPeriodModel(start = LocalDate.of(2017, Month.JUNE, 1), end = LocalDate.of(2018, Month.MAY, 30))),
  //    tradingName = Some("Big Company Ltd"),
  //    firstAccountingPeriodEndDate = Some(LocalDate.of(2018, Month.APRIL, 5)),
  //    tradingStartDate = Some(LocalDate.of(2018, 4, 5)),
  //    cessation = Some(CessationModel(Some(LocalDate.of(2022, 1, 2)), None))
  //  )
  //  val business2 = BusinessDetailsModel(
  //    incomeSourceId = Some("XA00001235"),
  //    accountingPeriod = Some(AccountingPeriodModel(start = LocalDate.of(2019, Month.MAY, 1), end = LocalDate.of(2018, Month.MAY, 30))),
  //    tradingName = Some("Small Company Ltd"),
  //    firstAccountingPeriodEndDate = None,
  //    tradingStartDate = Some(LocalDate.of(2020, 4, 5)),
  //    cessation = None
  //  )
  //  val propertyDetails = PropertyDetailsModel(
  //    incomeSourceId = Some("1234"),
  //    accountingPeriod = Some(AccountingPeriodModel(LocalDate.of(2017, 4, 6), LocalDate.of(2018, 4, 5))),
  //    firstAccountingPeriodEndDate = None,
  //    incomeSourceType = Some("uk-property"),
  //    tradingStartDate = Some(LocalDate.of(2020, 1, 5))
  //  )
  //  val dummyBusinessesAndPropertyIncome: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
  //    mtdbsa = "XIAT0000000000A",
  //    yearOfMigration = Some("2018"),
  //    businesses = List(business1, business2),
  //    property = Some(propertyDetails)
  //  )

}
