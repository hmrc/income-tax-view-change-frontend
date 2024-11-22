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

package controllers.incomeSources.cease

import connectors.UpdateIncomeSourceConnector
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.{MTDIndividual, MTDSupportingAgent}
import mocks.auth.MockAuthActions
import enums.JourneyType.{Cease, IncomeSources, JourneyType}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.MockSessionService
import models.admin.IncomeSourcesFs
import models.core.IncomeSourceId
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api
import play.api.http.Status
import play.api.http.Status.SEE_OTHER
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.{SessionService, UpdateIncomeSourceService, UpdateIncomeSourceSuccess}
import testConstants.BaseTestConstants.testMtditid
import testConstants.UpdateIncomeSourceTestConstants
import testConstants.incomeSources.IncomeSourceDetailsTestConstants._
import uk.gov.hmrc.http.HttpClient

import java.time.LocalDate
import scala.concurrent.Future

class CheckCeaseIncomeSourceDetailsControllerSpec extends MockAuthActions with MockSessionService {

 lazy val mockUpdateIncomeSourceService: UpdateIncomeSourceService = mock(classOf[UpdateIncomeSourceService])
  val validCeaseDate: String = LocalDate.of(2022, 10, 10).toString

  val checkDetailsHeading: String = messages("incomeSources.ceaseBusiness.checkDetails.heading")

  override def fakeApplication() = applicationBuilderWithAuthBindings()
    .overrides(
      api.inject.bind[SessionService].toInstance(mockSessionService),
      api.inject.bind[UpdateIncomeSourceService].toInstance(mockUpdateIncomeSourceService)
    ).build()

  val testCeaseCheckIncomeSourceDetailsController =
    fakeApplication().injector.instanceOf[CeaseCheckIncomeSourceDetailsController]

  def heading(incomeSourceType: IncomeSourceType): String = {
    incomeSourceType match {
      case SelfEmployment => messages("incomeSources.ceaseBusiness.checkDetails.caption")
      case UkProperty => messages("incomeSources.ceaseUKProperty.checkDetails.caption")
      case ForeignProperty => messages("incomeSources.ceaseForeignProperty.checkDetails.caption")
    }
  }

  def title(incomeSourceType: IncomeSourceType, isAgent: Boolean): String = {
    if (isAgent)
      s"${messages("htmlTitle.agent", heading(incomeSourceType))}"
    else
      s"${messages("htmlTitle", heading(incomeSourceType))}"
  }

  def mockGetCeaseIncomeSourceDetails(incomeSourceType: IncomeSourceType) : Unit = {
    incomeSourceType match {
      case SelfEmployment =>
        when(mockIncomeSourceDetailsService.getCheckCeaseSelfEmploymentDetailsViewModel(any(), IncomeSourceId(any()), any()))
          .thenReturn(Right(checkCeaseBusinessDetailsModel))
      case UkProperty =>
        when(mockIncomeSourceDetailsService.getCheckCeasePropertyIncomeSourceDetailsViewModel(any(), any(), any()))
          .thenReturn(Right(checkCeaseUkPropertyDetailsModel))
      case ForeignProperty =>
        when(mockIncomeSourceDetailsService.getCheckCeasePropertyIncomeSourceDetailsViewModel(any(), any(), any()))
          .thenReturn(Right(checkCeaseForeignPropertyDetailsModel))
    }
  }

  val individual: Boolean = false
  val agent: Boolean = true

  Seq(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSourceType =>
    mtdAllRoles.foreach { mtdRole =>
      val fakeRequest = getFakeRequestBasedOnMTDUserType(mtdRole)
      s"show${if (mtdRole != MTDIndividual) "Agent"}($incomeSourceType)" when {
        val action = if (mtdRole == MTDIndividual) testCeaseCheckIncomeSourceDetailsController.show(incomeSourceType) else testCeaseCheckIncomeSourceDetailsController.showAgent(incomeSourceType)
        s"the user is authenticated as a $mtdRole" should {
          "return 200 OK" when {
            "income source is enabled" in {
              setupMockSuccess(mtdRole)
              enable(IncomeSourcesFs)
              mockBothPropertyBothBusiness()
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(JourneyType(Cease, incomeSourceType)))))
              mockGetCeaseIncomeSourceDetails(incomeSourceType)

              val result: Future[Result] = action(fakeRequest)

              val document: Document = Jsoup.parse(contentAsString(result))
              status(result) shouldBe Status.OK
              document.getElementsByClass("hmrc-caption govuk-caption-l").text().contains(
                heading(incomeSourceType))
              document.select("h1").text shouldBe checkDetailsHeading
            }
          }
          "Redirect to homepage" when {

            "income source is disabled" in {
              setupMockSuccess(mtdRole)
              disable(IncomeSourcesFs)
              mockPropertyIncomeSource()

              val result: Future[Result] = action(fakeRequest)

              status(result) shouldBe Status.SEE_OTHER
              val homeURL = if (mtdRole == MTDIndividual) {
                controllers.routes.HomeController.show().url
              }
              else {
                controllers.routes.HomeController.showAgent.url
              }
              redirectLocation(result) shouldBe Some(homeURL)

            }
          }

          "redirect to the Cannot Go Back page" when {
            "the journey is complete" in {
              setupMockSuccess(mtdRole)
              enable(IncomeSourcesFs)
              mockBothPropertyBothBusiness()
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(completedUIJourneySessionData(JourneyType(Cease, incomeSourceType)))))
              mockGetCeaseIncomeSourceDetails(incomeSourceType)

              val result = action(fakeRequest)

              status(result) shouldBe SEE_OTHER

              val expectedRedirectUrl = if (mtdRole == MTDIndividual) {
                routes.IncomeSourceCeasedBackErrorController.show(incomeSourceType).url
              } else {
                routes.IncomeSourceCeasedBackErrorController.showAgent(incomeSourceType).url

              }
              redirectLocation(result) shouldBe Some(expectedRedirectUrl)
            }
          }
        }
        if (mtdRole == MTDIndividual) {
          testMTDIndividualAuthFailures(action)
        } else {
          testMTDAgentAuthFailures(action, mtdRole == MTDSupportingAgent)
        }
      }

      s"submit${if (mtdRole != MTDIndividual) "Agent"}($incomeSourceType)" when {
        val action = if (mtdRole == MTDIndividual) testCeaseCheckIncomeSourceDetailsController.submit(incomeSourceType) else testCeaseCheckIncomeSourceDetailsController.submitAgent(incomeSourceType)
        val fakePostRequest = fakeRequest.withMethod("POST")
        s"the user is authenticated as a $mtdRole" should {
          val expectedRedirectUrl = if (mtdRole == MTDIndividual) {
            controllers.incomeSources.cease.routes.IncomeSourceCeasedObligationsController.show(incomeSourceType).url
          } else {
            controllers.incomeSources.cease.routes.IncomeSourceCeasedObligationsController.showAgent(incomeSourceType).url
          }
          s"return 303 SEE_OTHER and redirect to $expectedRedirectUrl" when {
            s"submitted and Income Source Type = $incomeSourceType" in {
              setupMockSuccess(mtdRole)
              enable(IncomeSources)
              mockBothPropertyBothBusiness()
              setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(JourneyType(Cease, incomeSourceType)))))
              when(mockUpdateIncomeSourceService.updateCessationDate(any(), any(), any())(any(), any()))
                .thenReturn(Future.successful(Right(UpdateIncomeSourceSuccess(testMtditid))))

              val result = action(fakePostRequest)

              status(result) shouldBe Status.SEE_OTHER
              redirectLocation(result) shouldBe Some(expectedRedirectUrl)
            }
          }
          s"return 303 SEE_OTHER and redirect to Income Source Not Ceased Controller" when {
            "updating Cessation date fails" in {
              setupMockSuccess(mtdRole)
              enable(IncomeSourcesFs)
              mockBothPropertyBothBusiness()
              setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(JourneyType(Cease, incomeSourceType)))))

              when(mockUpdateIncomeSourceService.updateCessationDate(any(), any(), any())(any(), any()))
                .thenReturn(Future.successful(Left(UpdateIncomeSourceTestConstants.failureResponse)))

              val result = action(fakePostRequest)

              val expectedRedirectUrl = controllers.incomeSources.cease.routes.IncomeSourceNotCeasedController.show(mtdRole != MTDIndividual, incomeSourceType).url

              status(result) shouldBe Status.SEE_OTHER
              redirectLocation(result) shouldBe Some(expectedRedirectUrl)
            }
          }
          s"return 500 INTERNAL_SERVER_ERROR" when {
            "income sources is missing" in {
              setupMockSuccess(mtdRole)
              enable(IncomeSourcesFs)
              mockNoIncomeSources()
              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Cease, incomeSourceType)))))

              val result = action(fakePostRequest)

              status(result) shouldBe Status.INTERNAL_SERVER_ERROR

            }
          }
        }
        if (mtdRole == MTDIndividual) {
          testMTDIndividualAuthFailures(action)
        } else {
          testMTDAgentAuthFailures(action, mtdRole == MTDSupportingAgent)
        }
      }
    }
  }
}






