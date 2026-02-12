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
import models.admin.OverseasBusinessAddress
import models.core.{CheckMode, NormalMode}
import models.incomeSourceDetails.{AddIncomeSourceData, Address, BusinessAddressModel}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import play.api
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, SEE_OTHER}
import play.api.mvc.Result
import play.api.test.Helpers.{redirectLocation, *}
import services.*
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome

import scala.concurrent.Future


class AddInternationalBusinessAddressControllerSpec extends MockAuthActions
  with MockSessionService with MockDateService {

  val incomeSourceDetailsService: IncomeSourceDetailsService = mock(classOf[IncomeSourceDetailsService])

  lazy val mockAddressLookupService: AddressLookupService = mock(classOf[AddressLookupService])
  lazy val mockDateServiceInjected: DateService = mock(classOfDateService)

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[SessionService].toInstance(mockSessionService),
      api.inject.bind[AddressLookupService].toInstance(mockAddressLookupService),
      api.inject.bind[ITSAStatusConnector].toInstance(mockItsaStatusConnector),
      api.inject.bind[BusinessDetailsConnector].toInstance(mockBusinessDetailsConnector),
      api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInjected)
    ).build()

  lazy val testAddInternationalBusinessAddressController = app.injector.instanceOf[AddInternationalBusinessAddressController]
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
        val action = if (mtdRole == MTDIndividual) testAddInternationalBusinessAddressController.show(isAgent = false, mode, false) else testAddInternationalBusinessAddressController.show(isAgent = true, mode, false)
        s"the user is authenticated as a $mtdRole" should {
          "redirect to the address lookup service" when {
            "location redirect is returned by the lookup service" in {
              enable(OverseasBusinessAddress)
              setupMockSuccess(mtdRole)
              mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

              setupMockGetMongo(Right(None))

              when(mockAddressLookupService.initialiseAddressJourney(any(), any(), any(), any())(any(), any()))
                .thenReturn(Future(Right(Some("Sample location"))))

              val result: Future[Result] = action(fakeRequest)
              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some("Sample location")
            }

            "session data exists but addressLookupId is missing" in {
              enable(OverseasBusinessAddress)
              setupMockSuccess(mtdRole)
              mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

              setupMockGetMongo(Right(Some(testUIJourneySessionDataNoLookupId)))

              when(mockAddressLookupService.initialiseAddressJourney(any(), any(), any(), any())(any(), any()))
                .thenReturn(Future(Right(Some("Sample location"))))

              val result: Future[Result] = action(fakeRequest)
              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some("Sample location")
            }

            "mongo call fails" in {
              enable(OverseasBusinessAddress)
              setupMockSuccess(mtdRole)
              mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

              setupMockGetMongo(Left(AddressError("mongo failure")))

              when(mockAddressLookupService.initialiseAddressJourney(any(), any(), any(), any())(any(), any()))
                .thenReturn(Future(Right(Some("Sample location"))))

              val result: Future[Result] = action(fakeRequest)
              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some("Sample location")
            }
          }

          "redirect to the address lookup confirmation page" when {
            "addressLookupId exists in session data and is still valid" in {
              enable(OverseasBusinessAddress)
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
              redirectLocation(result) shouldBe Some(expectedUrl)

              verify(mockAddressLookupService, never).initialiseAddressJourney(any(), any(), any(), any())(any(), any())
            }
          }

          "redirect to the address lookup service" when {
            "addressLookupId exists in session data but journey is expired/invalid" in {
              enable(OverseasBusinessAddress)
              setupMockSuccess(mtdRole)
              mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

              setupMockGetMongo(Right(Some(testUIJourneySessionData)))

              when(mockAddressLookupService.fetchAddress(any())(any()))
                .thenReturn(Future(Left(AddressError("Not found"))))

              when(mockAddressLookupService.initialiseAddressJourney(any(), any(), any(), any())(any(), any()))
                .thenReturn(Future(Right(Some("Sample location"))))

              val result: Future[Result] = action(fakeRequest)
              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some("Sample location")
            }
          }

          "redirect to home page" when {
            "overseas business address FS is disabled" in {
              disable(OverseasBusinessAddress)
              setupMockSuccess(mtdRole)
              mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

              setupMockGetMongo(Right(None))

              val result: Future[Result] = action(fakeRequest)

              val homePageUrl = if (mtdRole == MTDIndividual) {
                controllers.routes.HomeController.show().url
              } else {
                controllers.routes.HomeController.showAgent().url
              }

              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some(homePageUrl)
            }
          }

          "return the correct error" when {
            "no location returned" in {
              enable(OverseasBusinessAddress)
              setupMockSuccess(mtdRole)
              mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

              setupMockGetMongo(Right(None))

              when(mockAddressLookupService.initialiseAddressJourney(any(), any(), any(), any())(any(), any()))
                .thenReturn(Future(Right(None)))

              val result: Future[Result] = action(fakeRequest)
              status(result) shouldBe INTERNAL_SERVER_ERROR
            }

            "failure returned" in {
              enable(OverseasBusinessAddress)
              setupMockSuccess(mtdRole)
              mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

              setupMockGetMongo(Right(None))

              when(mockAddressLookupService.initialiseAddressJourney(any(), any(), any(), any())(any(), any()))
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
    }
  }
}
