/*
 * Copyright 2024 HM Revenue & Customs
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

import connectors.{BusinessDetailsConnector, ITSAStatusConnector}
import enums.{MTDIndividual, MTDPrimaryAgent, MTDUserRole}
import mocks.auth.MockAuthActions
import mocks.services.*
import _root_.config.ItvcErrorHandler
import models.financialDetails.*
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.{ITSAStatus, StatusDetail, StatusReason}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api
import play.api.Application
import play.api.http.Status
import play.api.mvc.Results.InternalServerError
import play.api.mvc.{Action, AnyContent, AnyContentAsEmpty}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.twirl.api.Html
import services.*

import java.time.{LocalDate, Month}
import scala.concurrent.Future

trait HomeControllerHelperSpec extends MockAuthActions
  with MockNextUpdatesService
  with MockFinancialDetailsService
  with MockWhatYouOweService
  with MockClientDetailsService
  with MockDateService
  with MockITSAStatusService
  with MockPenaltyDetailsService {

  val agentTitle = s"${messages("htmlTitle.agent", messages("home.agent.heading"))}"
  
  lazy val mockDateServiceInjected: DateService = mock(classOfDateService)

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[NextUpdatesService].toInstance(mockNextUpdatesService),
      api.inject.bind[FinancialDetailsService].toInstance(mockFinancialDetailsService),
      api.inject.bind[WhatYouOweService].toInstance(mockWhatYouOweService),
      api.inject.bind[DateService].toInstance(mockDateServiceInjected),
      api.inject.bind[ITSAStatusService].toInstance(mockITSAStatusService),
      api.inject.bind[PenaltyDetailsService].toInstance(mockPenaltyDetailsService),
      api.inject.bind[ITSAStatusConnector].toInstance(mockItsaStatusConnector),
      api.inject.bind[BusinessDetailsConnector].toInstance(mockBusinessDetailsConnector),
      api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInterface)
    ).build()

  val updateYear: String = "2018"
  val nextPaymentYear: String = "2019"
  val nextPaymentYear2: String = "2018"
  val futureDueDates: Seq[LocalDate] = Seq(LocalDate.of(2100, 1, 1))
  val overdueDueDates: Seq[LocalDate] = Seq(LocalDate.of(2018, 1, 1))
  val updateDateAndOverdueObligations: (LocalDate, Seq[LocalDate]) = (LocalDate.of(updateYear.toInt, Month.JANUARY, 1), futureDueDates)
  val nextPaymentDate: LocalDate = LocalDate.of(nextPaymentYear.toInt, Month.JANUARY, 31)
  val nextPaymentDate2: LocalDate = LocalDate.of(nextPaymentYear2.toInt, Month.JANUARY, 31)
  val baseStatusDetail: StatusDetail = StatusDetail("2023-06-15T15:38:33.960Z", ITSAStatus.Annual, StatusReason.SignupReturnAvailable, Some(8000.25))
  val staticTaxYear: TaxYear = TaxYear(fixedDate.getYear - 1, fixedDate.getYear)

  def setupNextUpdatesTests(allDueDates: Seq[LocalDate],
                            nextQuarterlyUpdateDueDate: Option[LocalDate],
                            nextTaxReturnDueDate: Option[LocalDate],
                            mtdUserRole: MTDUserRole = MTDIndividual)(using NextUpdatesService): Unit = {
    mtdUserRole match {
      case MTDIndividual => setupMockUserAuth
      case MTDPrimaryAgent => setupMockAgentWithClientAuth(false)
      case _ => setupMockAgentWithClientAuth(true)
    }

    mockGetDueDates(Right(allDueDates))
    mockGetNextDueDates((nextQuarterlyUpdateDueDate, nextTaxReturnDueDate))
    mockSingleBusinessIncomeSource()

    when(mockFinancialDetailsService.getAllUnpaidFinancialDetails()(any(), any(), any()))
      .thenReturn(Future.successful(List(FinancialDetailsModel(
        balanceDetails = BalanceDetails(1.00, 2.00, 0.00, 3.00, None, None, None, None, None, None, None),
        documentDetails = List(DocumentDetail(nextPaymentYear.toInt, "testId", None, None, 1000.00, 0, LocalDate.of(2018, 3, 29))),
        financialDetails = List(FinancialDetail(nextPaymentYear, transactionId = Some("testId"),
          items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString))))))
      ))))

    setupMockGetWhatYouOweChargesListFromFinancialDetails(emptyWhatYouOweChargesList)
  }

  def testMTDObligationsDueFailures(action: Action[AnyContent], mtdUserRole: MTDUserRole = MTDIndividual)
                                   (fakeRequest: FakeRequest[AnyContentAsEmpty.type])
                                   (using nextUpdatesService: NextUpdatesService, itvcErrorHandler: ItvcErrorHandler): Unit = {
    s"the ${mtdUserRole.toString} is authenticated but the call to get obligations fails" should {
      "render the internal error page" in {
        mtdUserRole match {
          case MTDIndividual => setupMockUserAuth
          case MTDPrimaryAgent => setupMockGetSessionDataSuccess()
            setupMockAgentWithClientAuth(false)
          case _ => setupMockGetSessionDataSuccess()
            setupMockAgentWithClientAuth(true)
        }
        when(mockDateServiceInjected.getCurrentDate).thenReturn(fixedDate)
        mockSingleBusinessIncomeSource()
        mockGetDueDates(Left(new Exception("obligation test exception")))
        setupMockGetWhatYouOweChargesListFromFinancialDetails(emptyWhatYouOweChargesList)
        when(itvcErrorHandler.showInternalServerError()(any()))
          .thenReturn(InternalServerError(Html("<title>Sorry, there is a problem with the service - GOV.UK</title>")))

        val result = action(fakeRequest)

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        val errorPage = Jsoup.parse(contentAsString(result))
        errorPage.title shouldEqual "Sorry, there is a problem with the service - GOV.UK"
      }
    }
  }

}