/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers.newHomePage

import common.auth.AuthActions
import enums.MTDIndividual
import common.mocks.auth.MockAuthActions
import mocks.services.*
import models.admin.{MortgageEvidence, NewHomePage}
import models.itsaStatus.ITSAStatusResponseModel
import models.liabilitycalculation.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api
import play.api.Application
import play.api.http.Status
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status}
import services.*
import views.html.partials.newHome.overview.ProofOfYourIncomeView

import java.time.LocalDate
import scala.concurrent.Future


class ProofOfYourIncomeControllerSpec extends MockAuthActions
with MockITSAStatusService
with MockCalculationService
with MockDateService {

  lazy val mockDateServiceInjected: DateService = mock(classOfDateService)

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[ITSAStatusService].toInstance(mockITSAStatusService),
      api.inject.bind[CalculationService].toInstance(mockCalculationService),
      api.inject.bind[DateService].toInstance(mockDateServiceInjected),
      api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInterface)
    ).build()

  def getTestCalcResponse(calcType: String) = {
      LiabilityCalculationResponse(
        inputs = Inputs(personalInformation = PersonalInformation(taxRegime = "UK", class2VoluntaryContributions = None)),
        messages = None,
        calculation = None,
        metadata = Metadata(
          calculationTimestamp = Some("2019-02-15T09:35:15.094Z"),
          calculationType = calcType,
          calculationReason = Some("customerRequest"),
          periodFrom = Some(LocalDate.of(2022, 1, 1)),
          periodTo = Some(LocalDate.of(2023, 1, 1))
        ),
        submissionChannel = None
      )
    }
  val expectedDescriptionProof = "Proof of your income"
  val expectedDescriptionIncomplete = "Incomplete - cannot be used as proof of your income"
  val view: ProofOfYourIncomeView = app.injector.instanceOf(classOf[ProofOfYourIncomeView])
  val authActions: AuthActions = app.injector.instanceOf(classOf[AuthActions])

  val testController = app.injector.instanceOf[ProofOfYourIncomeController]

  override def beforeEach(): Unit = {
    super.beforeEach()
    disableAllSwitches()

  }

  mtdAllRoles.foreach { mtdRole =>
    val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
    val isAgent = mtdRole != MTDIndividual
    s"show(isAgent = $isAgent)" when {
      val action = if (isAgent) testController.showAgent() else testController.show()
      s"the user is authenticated as $mtdRole" should {
        "render the mortgage evidence page with proof of the income" when {
          "the mortgage evidence feature switch is enabled" in {
            val itsaResponse = List(ITSAStatusResponseModel("2021-22", None))
            val taxYearStartDate = LocalDate.of(2023, 4, 6)
            enable(NewHomePage, MortgageEvidence)
            mockSingleBusinessIncomeSource()
            when(mockDateServiceInterface.getCurrentTaxYearStart)
              .thenReturn(taxYearStartDate)
            when(mockITSAStatusService.getITSAStatusDetail(any(), any(), any())(any(), any(), any())).thenReturn(Future(itsaResponse))
            when(mockCalculationService.getLiabilityCalculationDetail(any(), any(), any())(any()))
              .thenReturn(Future.successful(getTestCalcResponse("DF")))
            setupMockSuccess(mtdRole)

            val result = action(fakeRequest)

            status(result) shouldBe Status.OK

            val document: Document = Jsoup.parse(contentAsString(result))
            document.select(".card-row-description-class").first().text shouldBe expectedDescriptionProof
          }
        }

        "render the mortgage evidence page with incomplete status" when {
          "the mortgage evidence feature switch is enabled" in {
            val itsaResponse = List(ITSAStatusResponseModel("2021-22", None))
            val taxYearStartDate = LocalDate.of(2023, 4, 6)
            enable(NewHomePage, MortgageEvidence)
            mockSingleBusinessIncomeSource()
            when(mockDateServiceInterface.getCurrentTaxYearStart)
              .thenReturn(taxYearStartDate)
            when(mockITSAStatusService.getITSAStatusDetail(any(), any(), any())(any(), any(), any())).thenReturn(Future(itsaResponse))
            when(mockCalculationService.getLiabilityCalculationDetail(any(), any(), any())(any()))
              .thenReturn(Future.successful(getTestCalcResponse("IF")))
            setupMockSuccess(mtdRole)

            val result = action(fakeRequest)

            status(result) shouldBe Status.OK

            val document: Document = Jsoup.parse(contentAsString(result))
            document.select(".card-row-description-class").first().text shouldBe expectedDescriptionIncomplete
          }
        }
      }
    }
  }
}