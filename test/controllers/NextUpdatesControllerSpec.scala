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

import mocks.auth.MockAuthActions
import mocks.services.{MockNextUpdatesService, MockOptOutService}
import models.admin.OptOutFs
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.{Mandated, Voluntary}
import models.obligations._
import models.optout.{NextUpdatesQuarterlyReportingContentChecks, OptOutMultiYearViewModel}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import play.api
import play.api.Application
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.Helpers._
import services.NextUpdatesService
import services.optout.{OptOutProposition, OptOutService}
import testConstants.{BaseTestConstants, NextUpdatesTestConstants}

import java.time.LocalDate
import scala.concurrent.Future

class NextUpdatesControllerSpec extends MockAuthActions
  with MockNextUpdatesService with MockOptOutService {

  val nextTitle: String = NextUpdatesTestConstants.title

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[NextUpdatesService].toInstance(mockNextUpdatesService),
      api.inject.bind[OptOutService].toInstance(mockOptOutService)
    ).build()

  lazy val testNextUpdatesController: NextUpdatesController = app.injector.instanceOf[NextUpdatesController]

  val obligationsModel: ObligationsModel = ObligationsModel(Seq(
    GroupedObligationsModel(BaseTestConstants.testSelfEmploymentId, List(SingleObligationModel(fixedDate, fixedDate, fixedDate, "Quarterly", Some(fixedDate), "#001", StatusFulfilled))),
    GroupedObligationsModel(BaseTestConstants.testPropertyIncomeId, List(SingleObligationModel(fixedDate, fixedDate, fixedDate, "EOPS", Some(fixedDate), "EOPS", StatusFulfilled)))
  ))

  val nextUpdatesViewModel: NextUpdatesViewModel = NextUpdatesViewModel(ObligationsModel(Seq(
    GroupedObligationsModel(BaseTestConstants.testSelfEmploymentId, List(SingleObligationModel(fixedDate, fixedDate, fixedDate, "Quarterly", Some(fixedDate), "#001", StatusFulfilled))),
    GroupedObligationsModel(BaseTestConstants.testPropertyIncomeId, List(SingleObligationModel(fixedDate, fixedDate, fixedDate, "EOPS", Some(fixedDate), "EOPS", StatusFulfilled)))
  )).obligationsByDate(isR17ContentEnabled = true).map { case (date: LocalDate, obligations: Seq[ObligationWithIncomeType]) =>
    DeadlineViewModel(getQuarterType(obligations.head.incomeType), standardAndCalendar = false, date, obligations, Seq.empty)
  })

  val contentChecks = NextUpdatesQuarterlyReportingContentChecks(
    currentYearItsaStatus = true,
    previousYearItsaStatus = false,
    previousYearCrystallisedStatus = false
  )

  val optOutViewModel = OptOutMultiYearViewModel()

  val optOutProposition: OptOutProposition =
    OptOutProposition.createOptOutProposition(
      currentYear = TaxYear(2024, 2025),
      previousYearCrystallised = false,
      previousYearItsaStatus = Mandated,
      currentYearItsaStatus = Voluntary,
      nextYearItsaStatus = Mandated
    )

  private def getQuarterType(string: String) = {
    if (string == "Quarterly") QuarterlyObligation else EopsObligation
  }

  def mockObligations: OngoingStubbing[Future[ObligationsResponseModel]] = {
    when(mockNextUpdatesService.getOpenObligations()(any(), any()))
      .thenReturn(Future.successful(obligationsModel))
  }

  def mockNoObligations: OngoingStubbing[Future[ObligationsResponseModel]] = {
    when(mockNextUpdatesService.getOpenObligations()(any(), any()))
      .thenReturn(Future.successful(ObligationsModel(Seq())))
  }

  def mockViewModel: OngoingStubbing[NextUpdatesViewModel] = {
    when(mockNextUpdatesService.getNextUpdatesViewModel(any(), any())(any()))
      .thenReturn(nextUpdatesViewModel)
  }

  /* INDIVIDUAL **/
  "The NextUpdatesController.show function" when {

    disableAllSwitches()

    "the Next Updates feature switch is disabled" should {

      "called with an Authenticated HMRC-MTD-IT user with NINO" which {

        "successfully retrieves a set of Business NextUpdates and Previous Obligations from the NextUpdates service" should {

          setupMockUserAuth
          mockSingleBusinessIncomeSource()
          mockSingleBusinessIncomeSourceWithDeadlines()
          mockGetNextUpdatesPageChecksAndProposition(Future.successful((contentChecks, Some(optOutViewModel), optOutProposition)))
          mockObligations
          mockViewModel
          val result = testNextUpdatesController.show()(fakeRequestWithActiveSession)
          lazy val document = Jsoup.parse(contentAsString(result))

          "return Status OK (200) with right html content" in {
            status(result) shouldBe Status.OK
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
            document.title shouldBe nextTitle
          }
        }

        "successfully retrieves a set of Property obligations from the NextUpdates service" should {

          setupMockUserAuth
          mockPropertyIncomeSource()
          mockPropertyIncomeSourceWithDeadlines()
          mockObligations
          val result = testNextUpdatesController.show()(fakeRequestWithActiveSession)
          val document = Jsoup.parse(contentAsString(result))

          "return Status OK (200)return Status OK (200) with right html content" in {
            status(result) shouldBe Status.OK
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
            document.title shouldBe nextTitle
          }
        }

        "successfully retrieves a set of both Business & Property NextUpdates and Previous Obligations from the NextUpdates service" should {

          setupMockUserAuth
          mockBothIncomeSourcesBusinessAligned()
          mockBothIncomeSourcesBusinessAlignedWithDeadlines()
          mockObligations
          val result = testNextUpdatesController.show(origin = Some("PTA"))(fakeRequestWithActiveSession)
          val document = Jsoup.parse(contentAsString(result))

          "return Status OK (200) with right html content" in {
            status(result) shouldBe Status.OK
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
            document.title shouldBe nextTitle
          }
        }

        "successfully retrieves a set of only Business NextUpdates and no Previous Obligations from the NextUpdates service" should {

          setupMockUserAuth
          mockSingleBusinessIncomeSource()
          mockSingleBusinessIncomeSourceWithDeadlines()
          val result = testNextUpdatesController.show()(fakeRequestWithActiveSession)
          val document = Jsoup.parse(contentAsString(result))

          "return Status OK (200) with right html content" in {
            status(result) shouldBe Status.OK
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
            document.title shouldBe nextTitle
          }
        }

        "successfully retrieves a set of only Property NextUpdates and no Previous from the NextUpdates service" should {

          setupMockUserAuth
          mockPropertyIncomeSource()
          mockPropertyIncomeSourceWithDeadlines()
          val result = testNextUpdatesController.show()(fakeRequestWithActiveSession)
          val document = Jsoup.parse(contentAsString(result))

          "return Status OK (200) with right html content" in {
            status(result) shouldBe Status.OK
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
            document.title shouldBe nextTitle
          }
        }

        "successfully retrieves a set of only both Business & Property NextUpdates and no Previous Obligations from the NextUpdates service" should {

          setupMockUserAuth
          mockViewModel
          mockBothIncomeSourcesBusinessAligned()
          mockBothIncomeSourcesBusinessAlignedWithDeadlines()
          val result = testNextUpdatesController.show()(fakeRequestWithActiveSession)
          val document = Jsoup.parse(contentAsString(result))

          "return Status OK (200) with right html content" in {
            status(result) shouldBe Status.OK
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
            document.title shouldBe nextTitle
          }
        }

        "receives an Error from the NextUpdates Service" should {

          "return Status ISE (500) with error page" in {
            setupMockUserAuth
            mockSingleBusinessIncomeSource()
            mockSingleBusinessIncomeSourceWithDeadlines()
            val result = testNextUpdatesController.show()(fakeRequestWithActiveSession)

            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }
        }

        "doesn't have any Income Source" should {
          setupMockUserAuth
          mockNoIncomeSources()
          mockSingleBusinessIncomeSourceWithDeadlines()
          val result = testNextUpdatesController.show()(fakeRequestWithActiveSession)
          val document = Jsoup.parse(contentAsString(result))

          "return Status OK (200) with right html content" in {
            status(result) shouldBe Status.OK
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
            document.title shouldBe NextUpdatesTestConstants.noNextUpdatesTitle
            document.select("h1").text() shouldBe NextUpdatesTestConstants.noNextUpdatesHeading
            document.select("p.govuk-body").text shouldBe NextUpdatesTestConstants.noNextUpdatesText
          }
        }

      }

      "Called with an Unauthenticated User" should {

        "return redirect SEE_OTHER (303)" in {
          setupMockUserAuthorisationException()
          val result = testNextUpdatesController.show()(fakeRequestWithActiveSession)
          status(result) shouldBe Status.SEE_OTHER
        }
      }
    }

    "the Next Updates feature switch disabled: other cases" should {

      "called with an Authenticated HMRC-MTD-IT user with NINO" which {

        "failed to retrieve a set of Business NextUpdates" should {
          setupMockUserAuth
          mockSingleBusinessIncomeSourceError()
          mockSingleBusinessIncomeSourceWithDeadlines()
          mockObligations
          val result = testNextUpdatesController.show()(fakeRequestWithActiveSession)

          "return Status ERROR (500)" in {
            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }
        }
      }
    }

    "the opt out feature switch is enabled and optOutService returns failed future" should {
      s"show an INTERNAL SERVER ERROR page with HTTP ${Status.INTERNAL_SERVER_ERROR} Status " in {
        enable(OptOutFs)
        setupMockUserAuth
        mockSingleBusinessIncomeSourceError()
        mockSingleBusinessIncomeSourceWithDeadlines()
        mockGetNextUpdatesPageChecksAndProposition(Future.failed(new Exception("api failure")))
        mockObligations
        val result = testNextUpdatesController.show()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    testMTDIndividualAuthFailures(testNextUpdatesController.show())
  }

  /* AGENT **/
  Map("primary agent" -> false, "supporting agent" -> true).foreach { case (agentType, isSupportingAgent) =>
    "The NextUpdatesController.showAgent function" when {

      s"the $agentType has all correct details" should {
        "return Status OK (200) when we have obligations" in {
          disableAllSwitches()
          mockSingleBusinessIncomeSource()
          mockSingleBusinessIncomeSourceWithDeadlines()
          setupMockAgentWithClientAuth(isSupportingAgent)
          mockGetNextUpdatesPageChecksAndProposition(Future.successful((contentChecks, Some(optOutViewModel), optOutProposition)))
          mockViewModel
          mockObligations

          val result: Future[Result] = testNextUpdatesController.showAgent()(
            fakeRequestConfirmedClient(isSupportingAgent = isSupportingAgent)
          )

          status(result) shouldBe OK
          contentType(result) shouldBe Some(HTML)
        }

        "return Status INTERNAL_SERVER_ERROR (500) when we have no obligations" in {
          setupMockAgentWithClientAuthAndIncomeSources(isSupportingAgent)
          mockSingleBusinessIncomeSource()
          mockNoObligations
          mockGetNextUpdatesPageChecksAndProposition(Future.successful((contentChecks, Some(optOutViewModel), optOutProposition)))
          mockNoIncomeSourcesWithDeadlines()

          val result: Future[Result] = testNextUpdatesController.showAgent()(
            fakeRequestConfirmedClient(isSupportingAgent= isSupportingAgent)
          )
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }

      testMTDAgentAuthFailures(testNextUpdatesController.showAgent(), isSupportingAgent)
    }
  }
}
