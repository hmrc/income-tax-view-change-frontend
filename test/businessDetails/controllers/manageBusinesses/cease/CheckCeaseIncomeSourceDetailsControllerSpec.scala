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

package businessDetails.controllers.manageBusinesses.cease

import businessDetails.controllers.manageBusinesses.cease.routes as ceaseBusinessRoutes
import businessDetails.controllers.triggeredMigration.routes as triggeredMigrationRoutes
import businessDetails.mocks.services.MockIncomeSourceDetailsService
import businessDetails.services.{IncomeSourceDetailsService, SessionService, UpdateIncomeSourceService, UpdateIncomeSourceSuccess}
import businessDetails.testConstants.UpdateIncomeSourceTestConstants.*
import common.connectors.ITSAStatusConnector
import common.enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import common.enums.JourneyType.{Cease, IncomeSourceJourneyType}
import common.enums.TriggeredMigration.TriggeredMigrationCeased
import common.enums.{MTDIndividual, MTDSupportingAgent}
import common.mocks.auth.MockAuthActions
import common.mocks.services.MockSessionService
import common.models.core.IncomeSourceId
import common.services.DateServiceInterface
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api
import play.api.http.Status
import play.api.http.Status.SEE_OTHER
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import common.testConstants.BaseTestConstants.testMtditid
import common.testConstants.IncomeSourceDetailsTestConstants.*
import businessDetails.testConstants.UpdateIncomeSourceTestConstants

import java.time.LocalDate
import scala.concurrent.Future

class CheckCeaseIncomeSourceDetailsControllerSpec extends MockAuthActions with MockSessionService with MockIncomeSourceDetailsService {

 lazy val mockUpdateIncomeSourceService: UpdateIncomeSourceService = mock(classOf[UpdateIncomeSourceService])
  val validCeaseDate: String = LocalDate.of(2022, 10, 10).toString

  val checkDetailsHeading: String = messages("cease-check-answers.title")

  override lazy val app = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[SessionService].toInstance(mockSessionService),
      api.inject.bind[UpdateIncomeSourceService].toInstance(mockUpdateIncomeSourceService),
      api.inject.bind[ITSAStatusConnector].toInstance(mockItsaStatusConnector),
      api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInterface),
      api.inject.bind[IncomeSourceDetailsService].toInstance(mockIncomeSourceDetailsService)

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
      s"show${if (mtdRole != MTDIndividual) "Agent" else ""}(incomeSourceType = $incomeSourceType)" when {
        val action = if (mtdRole == MTDIndividual) testCeaseCheckIncomeSourceDetailsController.show(incomeSourceType, false) else testCeaseCheckIncomeSourceDetailsController.showAgent(incomeSourceType, false)
        s"the user is authenticated as a $mtdRole" should {
          "return 200 OK" when {
            "using the manage businesses journey" in {
              setupMockSuccess(mtdRole)
              mockItsaStatusRetrievalAction(ukPlusForeignPropertyAndSoleTraderPlusCeasedBusinessIncome)
              mockBothPropertyBothBusiness()
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Cease, incomeSourceType)))))
              mockGetCeaseIncomeSourceDetails(incomeSourceType)

              val result = action(fakeRequest)

              val document: Document = Jsoup.parse(contentAsString(result))
              status(result) shouldBe Status.OK
              document.getElementsByClass("hmrc-caption govuk-caption-xl").text().contains(
                heading(incomeSourceType))
              document.select("h1").text shouldBe checkDetailsHeading
            }
          }

          "redirect to the Cannot Go Back page" when {
            "the journey is complete" in {
              setupMockSuccess(mtdRole)
              mockItsaStatusRetrievalAction(ukPlusForeignPropertyAndSoleTraderPlusCeasedBusinessIncome)
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

      s"submit${if (mtdRole != MTDIndividual) "Agent" else ""}(incomeSourceType = $incomeSourceType)" when {
        def action(isTriggeredMigration: Boolean) = if (mtdRole == MTDIndividual) testCeaseCheckIncomeSourceDetailsController.submit(incomeSourceType, isTriggeredMigration) else testCeaseCheckIncomeSourceDetailsController.submitAgent(incomeSourceType, isTriggeredMigration)
        val fakePostRequest = fakeRequest.withMethod("POST")
        s"the user is authenticated as a $mtdRole" should {
          val expectedRedirect = if (mtdRole == MTDIndividual) {
            ceaseBusinessRoutes.IncomeSourceCeasedObligationsController.show(incomeSourceType).url
          } else {
            ceaseBusinessRoutes.IncomeSourceCeasedObligationsController.showAgent(incomeSourceType).url
          }
          s"return 303 SEE_OTHER and redirect to $expectedRedirect" when {
            "using the manage businesses journey" in {
              setupMockSuccess(mtdRole)
              mockItsaStatusRetrievalAction(ukPlusForeignPropertyAndSoleTraderPlusCeasedBusinessIncome)
              mockBothPropertyBothBusiness()
              setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Cease, incomeSourceType)))))

              when(mockUpdateIncomeSourceService.updateCessationDate(any(), any(), any())(any(), any()))
                .thenReturn(Future.successful(Right(UpdateIncomeSourceSuccess(testMtditid))))

              val result = action(false)(fakePostRequest)

              status(result) shouldBe Status.SEE_OTHER
              redirectLocation(result) shouldBe Some(expectedRedirect)
            }
          }

          s"return 303 SEE_OTHER and redirect to Income Source Not Ceased Controller" when {
            "updating Cessation date fails" in {
              setupMockSuccess(mtdRole)
              mockItsaStatusRetrievalAction(ukPlusForeignPropertyAndSoleTraderPlusCeasedBusinessIncome)
              mockBothPropertyBothBusiness()
              setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Cease, incomeSourceType)))))

              when(mockUpdateIncomeSourceService.updateCessationDate(any(), any(), any())(any(), any()))
                .thenReturn(Future.successful(Left(UpdateIncomeSourceTestConstants.failureResponse)))

              val result = action(false)(fakePostRequest)

              val expectedRedirect = ceaseBusinessRoutes.IncomeSourceNotCeasedController.show(mtdRole != MTDIndividual, incomeSourceType).url

              status(result) shouldBe Status.SEE_OTHER
              redirectLocation(result) shouldBe Some(expectedRedirect)
            }
          }

          s"return 303 SEE_OTHER and redirect to the triggered migration check hmrc records page" when {
            "using the triggered migration journey" in {
              setupMockSuccess(mtdRole)
              mockItsaStatusRetrievalAction(ukPlusForeignPropertyAndSoleTraderPlusCeasedBusinessIncome)
              mockBothPropertyBothBusiness()
              setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Cease, incomeSourceType)))))

              when(mockUpdateIncomeSourceService.updateCessationDate(any(), any(), any())(any(), any()))
                .thenReturn(Future.successful(Right(UpdateIncomeSourceSuccess(testMtditid))))

              val result = action(true)(fakePostRequest)

              val expectedRedirect = triggeredMigrationRoutes.CheckHmrcRecordsController.show(mtdRole != MTDIndividual, Some(TriggeredMigrationCeased.toString)).url

              status(result) shouldBe Status.SEE_OTHER
              redirectLocation(result) shouldBe Some(expectedRedirect)
            }
          }

          s"return 500 INTERNAL_SERVER_ERROR" when {
            "income source is missing" in {
              setupMockSuccess(mtdRole)
              mockItsaStatusRetrievalAction(noIncomeDetails)
              mockNoIncomeSources()
              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Cease, incomeSourceType)))))

              val result = action(false)(fakePostRequest)

              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            }
          }
        }
          if (mtdRole == MTDIndividual) {
            testMTDIndividualAuthFailures(action(false))
          } else {
            testMTDAgentAuthFailures(action(false), mtdRole == MTDSupportingAgent)
          }
        }
    }
  }
}
