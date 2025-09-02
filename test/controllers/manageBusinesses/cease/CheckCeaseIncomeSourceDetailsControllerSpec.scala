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

package controllers.manageBusinesses.cease

import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Cease, IncomeSourceJourneyType}
import enums.{MTDIndividual, MTDSupportingAgent}
import mocks.auth.MockAuthActions
import mocks.services.MockSessionService
import models.core.IncomeSourceId
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api
import play.api.http.Status
import play.api.http.Status.SEE_OTHER
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.{SessionService, UpdateIncomeSourceService, UpdateIncomeSourceSuccess}
import testConstants.BaseTestConstants.testMtditid
import testConstants.UpdateIncomeSourceTestConstants
import testConstants.incomeSources.IncomeSourceDetailsTestConstants._

import java.time.LocalDate
import scala.concurrent.Future

class CheckCeaseIncomeSourceDetailsControllerSpec extends MockAuthActions with MockSessionService {

 lazy val mockUpdateIncomeSourceService: UpdateIncomeSourceService = mock(classOf[UpdateIncomeSourceService])
  val validCeaseDate: String = LocalDate.of(2022, 10, 10).toString

  val checkDetailsHeading: String = messages("cease-check-answers.title")

  override lazy val app = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[SessionService].toInstance(mockSessionService),
      api.inject.bind[UpdateIncomeSourceService].toInstance(mockUpdateIncomeSourceService)
    ).build()

  lazy val testCeaseCheckIncomeSourceDetailsController =
    app.injector.instanceOf[CeaseCheckIncomeSourceDetailsController]

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


  val individual: Boolean = false
  val agent: Boolean = true

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

  Seq(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSourceType =>
    mtdAllRoles.foreach { mtdRole =>
      val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
      s"show${if (mtdRole != MTDIndividual) "Agent"}(incomeSourceType = $incomeSourceType)" when {
        val action = if (mtdRole == MTDIndividual) testCeaseCheckIncomeSourceDetailsController.show(incomeSourceType) else testCeaseCheckIncomeSourceDetailsController.showAgent(incomeSourceType)
        s"the user is authenticated as a $mtdRole" should {
          "return 200 OK" when {
            "using the manage businesses journey" in {
              setupMockSuccess(mtdRole)
              mockBothPropertyBothBusiness()
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Cease, incomeSourceType)))))
              mockGetCeaseIncomeSourceDetails(incomeSourceType)

              val result = action(fakeRequest)

              val document: Document = Jsoup.parse(contentAsString(result))
              status(result) shouldBe Status.OK
              document.getElementsByClass("hmrc-caption govuk-caption-l").text().contains(
                heading(incomeSourceType))
              document.select("h1").text shouldBe checkDetailsHeading
            }
          }

          "redirect to the Cannot Go Back page" when {
            "the journey is complete" in {
              setupMockSuccess(mtdRole)
              mockBothPropertyBothBusiness()
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(completedUIJourneySessionData(IncomeSourceJourneyType(Cease, incomeSourceType)))))

              val result = action(fakeRequest)

              status(result) shouldBe SEE_OTHER
              val expectedRedirect = if (mtdRole == MTDIndividual) {
                routes.IncomeSourceCeasedBackErrorController.show(incomeSourceType).url
              } else {
                routes.IncomeSourceCeasedBackErrorController.showAgent(incomeSourceType).url
              }
              redirectLocation(result) shouldBe Some(expectedRedirect)
            }
          }
        }
        if (mtdRole == MTDIndividual) {
          testMTDIndividualAuthFailures(action)
        } else {
          testMTDAgentAuthFailures(action, mtdRole == MTDSupportingAgent)
        }
      }

      s"submit${if (mtdRole != MTDIndividual) "Agent"}(incomeSourceType = $incomeSourceType)" when {
        val action = if (mtdRole == MTDIndividual) testCeaseCheckIncomeSourceDetailsController.submit(incomeSourceType) else testCeaseCheckIncomeSourceDetailsController.submitAgent(incomeSourceType)
        val fakePostRequest = fakeRequest.withMethod("POST")
        s"the user is authenticated as a $mtdRole" should {
          val expectedRedirect = if (mtdRole == MTDIndividual) {
            controllers.manageBusinesses.cease.routes.IncomeSourceCeasedObligationsController.show(incomeSourceType).url
          } else {
            controllers.manageBusinesses.cease.routes.IncomeSourceCeasedObligationsController.showAgent(incomeSourceType).url
          }
          s"return 303 SEE_OTHER and redirect to $expectedRedirect" when {
            "using the manage businesses journey" in {
              setupMockSuccess(mtdRole)
              mockBothPropertyBothBusiness()
              setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Cease, incomeSourceType)))))

              when(mockUpdateIncomeSourceService.updateCessationDate(any(), any(), any())(any(), any()))
                .thenReturn(Future.successful(Right(UpdateIncomeSourceSuccess(testMtditid))))

              val result = action(fakePostRequest)

              status(result) shouldBe Status.SEE_OTHER
              redirectLocation(result) shouldBe Some(expectedRedirect)
            }
          }

          s"return 303 SEE_OTHER and redirect to Income Source Not Ceased Controller" when {
            "updating Cessation date fails" in {
              setupMockSuccess(mtdRole)
              mockBothPropertyBothBusiness()
              setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Cease, incomeSourceType)))))

              when(mockUpdateIncomeSourceService.updateCessationDate(any(), any(), any())(any(), any()))
                .thenReturn(Future.successful(Left(UpdateIncomeSourceTestConstants.failureResponse)))

              val result = action(fakePostRequest)

              val expectedRedirect = controllers.manageBusinesses.cease.routes.IncomeSourceNotCeasedController.show(mtdRole != MTDIndividual, incomeSourceType).url

              status(result) shouldBe Status.SEE_OTHER
              redirectLocation(result) shouldBe Some(expectedRedirect)
            }
          }
          s"return 500 INTERNAL_SERVER_ERROR" when {
            "income source is missing" in {
              setupMockSuccess(mtdRole)
              mockNoIncomeSources()
              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Cease, incomeSourceType)))))

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
