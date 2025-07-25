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

package services.manageBusinesses

import authV2.AuthActionsSpecHelper
import authV2.AuthActionsTestData.defaultMTDITUser
import enums.IncomeSourceJourney.{AfterSubmissionPage, SelfEmployment}
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import mocks.services.{MockCalculationListService, MockITSAStatusService, MockSessionService}
import models.admin.IncomeSourcesNewJourney
import models.incomeSourceDetails.{TaxYear, UIJourneySessionData}
import models.itsaStatus.ITSAStatus.{Annual, ITSAStatus, Mandated, NoStatus, Voluntary}
import models.itsaStatus.{StatusDetail, StatusReason}
import models.itsaStatus.StatusReason.{Rollover, SignupReturnAvailable, StatusReason}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status.SEE_OTHER
import play.api.mvc.{Result, Results}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.DateService
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{incomeSourceWithBothYearsInLatency, incomeSourceWithOneYearInLatency, notCompletedUIJourneySessionData, singleBusinessIncome2023, singleBusinessIncomeWithLatency2019}
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.Future

class IncomeSourceRFServiceSpec extends TestSupport
  with MockSessionService
  with MockITSAStatusService
  with MockCalculationListService
  with AuthActionsSpecHelper {

  object mockDateService extends DateService() {
    override def getCurrentDate: LocalDate = LocalDate.parse(s"2022-04-01")

    override def getCurrentTaxYearEnd: Int = 2023
  }

  val incomeSourceRFService = new IncomeSourceRFService(
    mockSessionService,
    mockITSAStatusService,
    mockItvcErrorHandler,
    mockAgentErrorHandler,
    mockDateService,
    mockAppConfig
  )

  implicit val hc: HeaderCarrier = mock(classOf[HeaderCarrier])

  class Setup(withBothYearsInLatency: Boolean, withOneYearsInLatency: Boolean, CYStatus: ITSAStatus, NYStatus: ITSAStatus) {
    enable(IncomeSourcesNewJourney)

    setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Add, SelfEmployment)))))
    setupMockSetMongoData(true)

    when(mockITSAStatusService.getStatusTillAvailableFutureYears(any())(any(), any(), any()))
      .thenReturn(Future.successful(
        Map(TaxYear.forYearEnd(2023) -> StatusDetail("", CYStatus, StatusReason.SignupNoReturnAvailable),
          TaxYear.forYearEnd(2024) -> StatusDetail("", NYStatus, StatusReason.Rollover),
          TaxYear.forYearEnd(2025) -> StatusDetail("", NYStatus, StatusReason.Rollover)
        )
      ))

    val testUser =
      if (withBothYearsInLatency) defaultMTDITUser(Some(Individual), incomeSourceWithBothYearsInLatency)
      else if(withOneYearsInLatency) defaultMTDITUser(Some(Individual), incomeSourceWithOneYearInLatency)
      else defaultMTDITUser(Some(Individual), singleBusinessIncomeWithLatency2019)
    def journeySessionCodeBlock: UIJourneySessionData => Future[Result] = (_) => Future.successful(Results.SeeOther("Successful"))

  }

  "redirectChecksForIncomeSourceRF" should {
    "redirect to the income source added page" when {
      "business is not in latency" in new Setup(false, false, NoStatus, Annual) {
        val result = incomeSourceRFService.redirectChecksForIncomeSourceRF(
          IncomeSourceJourneyType(Add, SelfEmployment),
          AfterSubmissionPage,
          SelfEmployment,
          mockDateService.getCurrentTaxYearEnd,
          false,
          false
        )(journeySessionCodeBlock)(testUser, hc)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/report-quarterly/income-and-expenses/view/manage-your-businesses/add-sole-trader/business-added")
      }

      "business is in latency for both years and CY+1 status is Annual at account level" in new Setup(true, false, Mandated, Annual) {
        val result = incomeSourceRFService.redirectChecksForIncomeSourceRF(
          IncomeSourceJourneyType(Add, SelfEmployment),
          AfterSubmissionPage,
          SelfEmployment,
          mockDateService.getCurrentTaxYearEnd,
          false,
          false
        )(journeySessionCodeBlock)(testUser, hc)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/report-quarterly/income-and-expenses/view/manage-your-businesses/add-sole-trader/business-added")
      }

      "business is in latency for both years and CY status is Annual at account level" in new Setup(true, false, Annual, Mandated) {
        val result = incomeSourceRFService.redirectChecksForIncomeSourceRF(
          IncomeSourceJourneyType(Add, SelfEmployment),
          AfterSubmissionPage,
          SelfEmployment,
          mockDateService.getCurrentTaxYearEnd,
          false,
          false
        )(journeySessionCodeBlock)(testUser, hc)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/report-quarterly/income-and-expenses/view/manage-your-businesses/add-sole-trader/business-added")
      }
    }
    "redirect to the url within the codeblock returned" when {
      "business is in latency for both years and mandated for both years" in new Setup(true, false, Mandated, Mandated) {
        val result = incomeSourceRFService.redirectChecksForIncomeSourceRF(
          IncomeSourceJourneyType(Add, SelfEmployment),
          AfterSubmissionPage,
          SelfEmployment,
          mockDateService.getCurrentTaxYearEnd,
          false,
          false
        )(journeySessionCodeBlock)(testUser, hc)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("Successful")
      }

      "business is in latency for one year" in new Setup(false, true, Mandated, Mandated) {
        val result = incomeSourceRFService.redirectChecksForIncomeSourceRF(
          IncomeSourceJourneyType(Add, SelfEmployment),
          AfterSubmissionPage,
          SelfEmployment,
          mockDateService.getCurrentTaxYearEnd,
          false,
          false
        )(journeySessionCodeBlock)(testUser, hc)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("Successful")
      }
    }
  }
}