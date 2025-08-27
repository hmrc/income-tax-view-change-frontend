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
import enums.MTDIndividual
import forms.manageBusinesses.cease.DeclareIncomeSourceCeasedForm
import mocks.auth.MockAuthActions
import mocks.services.MockSessionService
import models.core.NormalMode
import models.incomeSourceDetails.CeaseIncomeSourceData
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.verify
import play.api
import play.api.http.Status
import play.api.http.Status.SEE_OTHER
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.SessionService
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{completedUIJourneySessionData, emptyUIJourneySessionData}

class DeclareIncomeSourceCeasedControllerSpec extends MockAuthActions with MockSessionService {

  override lazy val app = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[SessionService].toInstance(mockSessionService))
    .build()

  lazy val testController = app.injector.instanceOf[DeclareIncomeSourceCeasedController]

  def getHeader(incomeSourceType: IncomeSourceType): String = {
    incomeSourceType match {
      case SelfEmployment => messages("incomeSources.cease.SE.heading")
      case UkProperty => messages("incomeSources.cease.UK.heading")
      case _ => messages("incomeSources.cease.FP.heading")
    }
  }

  def getTitle(incomeSourceType: IncomeSourceType, isAgent: Boolean): String = {
    val title = getHeader(incomeSourceType)
    if (isAgent) {
      messages("htmlTitle.agent", s"$title")
    } else {
      messages("htmlTitle", s"$title")
    }
  }

  def verifySetMongoKey(key: String, value: String, journeyType: IncomeSourceJourneyType): Unit = {
    val argumentKey: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
    val argumentValue: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
    val argumentIncomeSourceJourneyType: ArgumentCaptor[IncomeSourceJourneyType] = ArgumentCaptor.forClass(classOf[IncomeSourceJourneyType])

    verify(mockSessionService).setMongoKey(argumentKey.capture(), argumentValue.capture(), argumentIncomeSourceJourneyType.capture())(any(), any())
    argumentKey.getValue shouldBe key
    argumentValue.getValue shouldBe value
    argumentIncomeSourceJourneyType.getValue.toString shouldBe journeyType.toString
  }

  val incomeSourceTypes = List(SelfEmployment, UkProperty, ForeignProperty)

  mtdAllRoles.foreach { mtdRole =>
    incomeSourceTypes.foreach { incomeSourceType =>
      val isAgent = mtdRole != MTDIndividual
      val optId = if (incomeSourceType == SelfEmployment) Some("test-id") else None
      s"show${if (isAgent) "Agent"}($incomeSourceType)" when {
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
        val action = if (!isAgent) testController.show(optId, incomeSourceType) else testController.showAgent(optId, incomeSourceType)
        s"the user is authenticated as a $mtdRole" should {
          "render the declare incomeSourceCeased page" in {
            setupMockSuccess(mtdRole)
            mockBothPropertyBothBusiness()

            setupMockCreateSession(true)
            setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Cease, incomeSourceType)))))
            val result = action(fakeRequest)
            val document: Document = Jsoup.parse(contentAsString(result))
            status(result) shouldBe Status.OK
            document.title shouldBe getTitle(incomeSourceType, isAgent)
            document.select("h1").text shouldBe getHeader(incomeSourceType)
          }

          "redirect to the Cannot Go Back page" when {
            "journey is complete" in {
              setupMockSuccess(mtdRole)
              disableAllSwitches()
              mockBothPropertyBothBusiness()
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(completedUIJourneySessionData(IncomeSourceJourneyType(Cease, incomeSourceType)))))

              val result = action(fakeRequest)

              val expectedRedirectUrl = if (isAgent) {
                routes.IncomeSourceCeasedBackErrorController.showAgent(incomeSourceType).url
              } else {
                routes.IncomeSourceCeasedBackErrorController.show(incomeSourceType).url
              }

              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some(expectedRedirectUrl)
            }
          }
        }
        testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
      }

      s"submit${if (isAgent) "Agent"}($incomeSourceType)" when {
        val action = if (mtdRole == MTDIndividual) testController.submit(optId, incomeSourceType) else testController.submitAgent(optId, incomeSourceType)
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole).withMethod("POST")
        val validFormData = Map(
          DeclareIncomeSourceCeasedForm.declaration -> "true",
          DeclareIncomeSourceCeasedForm.ceaseCsrfToken -> "12345"
        )
        s"the user is authenticated as a $mtdRole" should {
          "redirect to end date controller" when {
            "cease declaration is completed" in {
              setupMockSuccess(mtdRole)
              mockBothPropertyBothBusiness()
              setupMockSetSessionKeyMongo(Right(true))

              val journeyType = IncomeSourceJourneyType(Cease, incomeSourceType)

              val result = action(fakeRequest.withFormUrlEncodedBody(validFormData.toSeq: _*))

              status(result) shouldBe Status.SEE_OTHER
              val optId = if(incomeSourceType == SelfEmployment) Some("test-id") else None
              redirectLocation(result) shouldBe Some(controllers.manageBusinesses.cease.routes.IncomeSourceEndDateController.show(optId, incomeSourceType, isAgent, NormalMode).url)
              verifySetMongoKey(CeaseIncomeSourceData.ceaseIncomeSourceDeclare, "true", journeyType)
            }
          }

          "return 500 INTERNAL_SERVER_ERROR" when {
            "Exception received from Mongo" in {
              setupMockSuccess(mtdRole)
              mockBothPropertyBothBusiness()
              setupMockSetSessionKeyMongo(Left(new Exception))
              val result = action(fakeRequest.withFormUrlEncodedBody(validFormData.toSeq: _*))
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR

            }
          }
        }
        testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
      }
    }
  }
}
