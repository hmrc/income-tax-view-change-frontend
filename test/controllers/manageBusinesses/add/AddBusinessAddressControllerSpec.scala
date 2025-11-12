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

import enums.{MTDIndividual, MTDSupportingAgent}
import mocks.auth.MockAuthActions
import mocks.services.MockSessionService
import models.UIJourneySessionData
import models.admin.AccountingMethodJourney
import models.core.{CheckMode, NormalMode}
import models.incomeSourceDetails.{AddIncomeSourceData, Address, BusinessAddressModel}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, verify, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, SEE_OTHER}
import play.api.mvc.{Call, Result}
import play.api.test.Helpers._
import services.{AddressLookupService, IncomeSourceDetailsService, SessionService}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome

import scala.concurrent.Future


class AddBusinessAddressControllerSpec extends MockAuthActions
  with MockSessionService {

  val incomeSourceDetailsService: IncomeSourceDetailsService = mock(classOf[IncomeSourceDetailsService])

  val postAction: Call = controllers.manageBusinesses.add.routes.AddBusinessAddressController.submit(None, mode = NormalMode)
  val postActionCheck: Call = controllers.manageBusinesses.add.routes.AddBusinessAddressController.submit(None, mode = CheckMode)
  lazy val mockAddressLookupService: AddressLookupService = mock(classOf[AddressLookupService])

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[SessionService].toInstance(mockSessionService),
      api.inject.bind[AddressLookupService].toInstance(mockAddressLookupService)
    ).build()

  lazy val testAddBusinessAddressController = app.injector.instanceOf[AddBusinessAddressController]

  val testBusinessAddressModel: BusinessAddressModel = BusinessAddressModel("auditRef", Address(Seq("Line 1", "Line 2"), Some("AA1 1AA")))
  val testAddIncomeSourceSessionData: Option[AddIncomeSourceData] = Some(AddIncomeSourceData(address = Some(testBusinessAddressModel.address), countryCode = Some("GB")))
  val testUIJourneySessionData: UIJourneySessionData = UIJourneySessionData("", "", testAddIncomeSourceSessionData)

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
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

              when(mockAddressLookupService.initialiseAddressJourney(any(), any())(any(), any()))
                .thenReturn(Future(Right(Some("Sample location"))))

              val result: Future[Result] = action(fakeRequest)
              status(result) shouldBe SEE_OTHER
              redirectLocation(result) mustBe Some("Sample location")
            }
          }

          "return the correct error" when {
            "no location returned" in {
              setupMockSuccess(mtdRole)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
              when(mockAddressLookupService.initialiseAddressJourney(any(), any())(any(), any()))
                .thenReturn(Future(Right(None)))

              val result: Future[Result] = action(fakeRequest)
              status(result) shouldBe INTERNAL_SERVER_ERROR
            }

            "failure returned" in {
              setupMockSuccess(mtdRole)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
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
          "redirect to add accounting method page" when {
            "valid data received" in {
              setupMockSuccess(mtdRole)
              enable(AccountingMethodJourney)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

              setupMockGetMongo(Right(Some(UIJourneySessionData("", ""))))
              setupMockSetMongoData(result = true)
              when(mockAddressLookupService.fetchAddress(any())(any()))
                .thenReturn(Future(Right(testBusinessAddressModel)))

              val result: Future[Result] = action(fakeRequest)
              status(result) shouldBe SEE_OTHER
              verifySetMongoData()
            }
          }

          "return the correct error" when {
            "no address returned" in {
              setupMockSuccess(mtdRole)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

              setupMockGetMongo(Right(Some(UIJourneySessionData("", ""))))
              setupMockSetMongoData(result = true)

              when(mockAddressLookupService.fetchAddress(any())(any()))
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
