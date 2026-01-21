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

package controllers.manageBusinesses.add

import config.FrontendAppConfig
import connectors.{BusinessDetailsConnector, ITSAStatusConnector}
import enums.{MTDIndividual, MTDSupportingAgent}
import mocks.auth.MockAuthActions
import mocks.services.{MockDateService, MockSessionService}
import models.UIJourneySessionData
import models.core.{CheckMode, NormalMode}
import models.incomeSourceDetails.{AddIncomeSourceData, Address, BusinessAddressModel}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{clearInvocations, mock, never, verify, when, mock as mMock}
import org.scalatest.matchers.must.Matchers
import services.{DateService, DateServiceInterface}
import play.api
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, SEE_OTHER}
import play.api.mvc.{Call, Result}
import play.api.test.Helpers.*
import services.{AddressLookupService, DateService, IncomeSourceDetailsService, SessionService}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome

import scala.concurrent.Future


class AddBusinessAddressControllerSpec extends MockAuthActions
  with MockSessionService with MockDateService with Matchers{

  val incomeSourceDetailsService: IncomeSourceDetailsService = mock(classOf[IncomeSourceDetailsService])

  val postAction: Call = controllers.manageBusinesses.add.routes.AddBusinessAddressController.submit(None, mode = NormalMode)
  val postActionCheck: Call = controllers.manageBusinesses.add.routes.AddBusinessAddressController.submit(None, mode = CheckMode)
  lazy val mockAddressLookupService: AddressLookupService = mock(classOf[AddressLookupService])
  lazy val mockDateServiceInjected: DateService = mMock(classOfDateService)

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[SessionService].toInstance(mockSessionService),
      api.inject.bind[AddressLookupService].toInstance(mockAddressLookupService),
      api.inject.bind[ITSAStatusConnector].toInstance(mockItsaStatusConnector),
      api.inject.bind[BusinessDetailsConnector].toInstance(mockBusinessDetailsConnector),
      api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInjected)
    ).build()

  lazy val testAddBusinessAddressController = app.injector.instanceOf[AddBusinessAddressController]
  lazy val frontendAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val testBusinessAddressModel: BusinessAddressModel = BusinessAddressModel("auditRef", Address(Seq("Line 1", "Line 2"), Some("AA1 1AA")))
  val testAddIncomeSourceSessionData: Option[AddIncomeSourceData] = Some(AddIncomeSourceData(address = Some(testBusinessAddressModel.address), countryCode = Some("GB"), addressLookupId = Some("123")))
  val testUIJourneySessionData: UIJourneySessionData = UIJourneySessionData("", "", testAddIncomeSourceSessionData)

  val testUIJourneySessionDataNoLookupId: UIJourneySessionData = testUIJourneySessionData.copy(
    addIncomeSourceData = testAddIncomeSourceSessionData.map(_.copy(addressLookupId = None))
  )

  def verifySetMongoData(): Unit = {
    val argument: ArgumentCaptor[UIJourneySessionData] = ArgumentCaptor.forClass(classOf[UIJourneySessionData])
    verify(mockSessionService).setMongoData(argument.capture())
    argument.getValue.addIncomeSourceData shouldBe testAddIncomeSourceSessionData
  }

  case class AddressError(status: String) extends RuntimeException

  Seq(CheckMode, NormalMode).foreach { mode =>

    mtdAllRoles.foreach { mtdRole =>

      val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)

      s"show${if (mtdRole != MTDIndividual) "Agent"}(mode = $mode)" when {
        val action = if (mtdRole == MTDIndividual) testAddBusinessAddressController.show(mode) else testAddBusinessAddressController.showAgent(mode)
        s"the user is authenticated as a $mtdRole" should {
          "redirect to the address lookup service" when {
            "location redirect is returned by the lookup service" in {
              setupMockSuccess(mtdRole)
              mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

              setupMockGetMongo(Right(None))

              when(mockAddressLookupService.initialiseAddressJourney(any(), any())(any(), any()))
                .thenReturn(Future(Right(Some("Sample location"))))

              val result: Future[Result] = action(fakeRequest)
              status(result) shouldBe SEE_OTHER
              redirectLocation(result) mustBe Some("Sample location")
            }

            "session data exists but addressLookupId is missing" in {
              setupMockSuccess(mtdRole)
              mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

              setupMockGetMongo(Right(Some(testUIJourneySessionDataNoLookupId)))

              when(mockAddressLookupService.initialiseAddressJourney(any(), any())(any(), any()))
                .thenReturn(Future(Right(Some("Sample location"))))

              val result: Future[Result] = action(fakeRequest)
              status(result) shouldBe SEE_OTHER
              redirectLocation(result) mustBe Some("Sample location")
            }

            "mongo call fails" in {
              setupMockSuccess(mtdRole)
              mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

              setupMockGetMongo(Left(AddressError("mongo failure")))

              when(mockAddressLookupService.initialiseAddressJourney(any(), any())(any(), any()))
                .thenReturn(Future(Right(Some("Sample location"))))

              val result: Future[Result] = action(fakeRequest)
              status(result) shouldBe SEE_OTHER
              redirectLocation(result) mustBe Some("Sample location")
            }
          }

          "redirect to the address lookup confirmation page" when {
            "addressLookupId exists in session data and is still valid" in {
              setupMockSuccess(mtdRole)
              mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

              setupMockGetMongo(Right(Some(testUIJourneySessionData)))

              when(mockAddressLookupService.fetchAddress(any())(any()))
                .thenReturn(Future(Right(testBusinessAddressModel)))

              val expectedUrl = s"${frontendAppConfig.addressLookupExternalHost}/lookup-address/123/confirm"

              clearInvocations(mockAddressLookupService)

              val result: Future[Result] = action(fakeRequest)
              status(result) shouldBe SEE_OTHER
              redirectLocation(result) mustBe Some(expectedUrl)

              verify(mockAddressLookupService, never).initialiseAddressJourney(any(), any())(any(), any())
            }
          }

          "redirect to the address lookup service" when {
            "addressLookupId exists in session data but journey is expired/invalid" in {
              setupMockSuccess(mtdRole)
              mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

              setupMockGetMongo(Right(Some(testUIJourneySessionData)))

              when(mockAddressLookupService.fetchAddress(any())(any()))
                .thenReturn(Future(Left(AddressError("Not found"))))

              when(mockAddressLookupService.initialiseAddressJourney(any(), any())(any(), any()))
                .thenReturn(Future(Right(Some("Sample location"))))

              val result: Future[Result] = action(fakeRequest)
              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some("Sample location")
            }
          }

          "return the correct error" when {
            "no location returned" in {
              setupMockSuccess(mtdRole)
              mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

              setupMockGetMongo(Right(None))

              when(mockAddressLookupService.initialiseAddressJourney(any(), any())(any(), any()))
                .thenReturn(Future(Right(None)))

              val result: Future[Result] = action(fakeRequest)
              status(result) shouldBe INTERNAL_SERVER_ERROR
            }

            "failure returned" in {
              setupMockSuccess(mtdRole)
              mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

              setupMockGetMongo(Right(None))

              when(mockAddressLookupService.initialiseAddressJourney(any(), any())(any(), any()))
                .thenReturn(Future(Left(AddressError("Test status"))))

              val result: Future[Result] = action(fakeRequest)
              status(result) shouldBe INTERNAL_SERVER_ERROR
            }
          }
        }

        if (mtdRole == MTDIndividual) {
          testMTDIndividualAuthFailures(action)
        } else {
          testMTDAgentAuthFailures(action, mtdRole == MTDSupportingAgent)
        }
      }

      s"submit${if (mtdRole != MTDIndividual) "Agent"}(mode = $mode)" when {
        val action = if (mtdRole == MTDIndividual) testAddBusinessAddressController.submit(Some("123"), mode) else testAddBusinessAddressController.agentSubmit(Some("123"), mode)
        s"the user is authenticated as a $mtdRole" should {
          "redirect to the business check answers page" when {
            "valid data received" in {
              val checkAnswersUrl = if(mtdRole == MTDIndividual) {
                "/report-quarterly/income-and-expenses/view/manage-your-businesses/add-sole-trader/business-check-answers"
              } else {
                "/report-quarterly/income-and-expenses/view/agents/manage-your-businesses/add-sole-trader/business-check-answers"
              }
              setupMockSuccess(mtdRole)
              mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

              setupMockGetMongo(Right(Some(UIJourneySessionData("", ""))))
              setupMockSetMongoData(result = true)
              when(mockAddressLookupService.fetchAddress(any())(any()))
                .thenReturn(Future(Right(testBusinessAddressModel)))

              val result: Future[Result] = action(fakeRequest)
              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some(checkAnswersUrl)
              verifySetMongoData()
            }
          }

          "return the correct error" when {
            "no address returned" in {
              setupMockSuccess(mtdRole)
              mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

              setupMockGetMongo(Right(Some(UIJourneySessionData("", ""))))
              setupMockSetMongoData(result = true)

              when(mockAddressLookupService.fetchAddress(any())(any()))
                .thenReturn(Future(Left(AddressError("Test status"))))

              val result: Future[Result] = action(fakeRequest)
              status(result) shouldBe INTERNAL_SERVER_ERROR
            }

            "session data not found" in {
              setupMockSuccess(mtdRole)
              mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

              setupMockGetMongo(Right(None))

              when(mockAddressLookupService.fetchAddress(any())(any()))
                .thenReturn(Future(Right(testBusinessAddressModel)))

              val result: Future[Result] = action(fakeRequest)
              status(result) shouldBe INTERNAL_SERVER_ERROR
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
