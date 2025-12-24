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

import connectors.{BusinessDetailsConnector, ITSAStatusConnector}
import enums.IncomeSourceJourney.SelfEmployment
import enums.JourneyType.{Add, IncomeSourceJourneyType, JourneyType}
import enums.{MTDIndividual, MTDSupportingAgent, MTDUserRole}
import forms.manageBusinesses.add.BusinessTradeForm
import mocks.auth.MockAuthActions
import mocks.services.MockSessionService
import models.core.{CheckMode, Mode, NormalMode}
import models.incomeSourceDetails.AddIncomeSourceData
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api
import play.api.Application
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.Helpers._
import services.{DateServiceInterface, SessionService}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants._

import scala.concurrent.Future


class AddBusinessTradeControllerSpec extends MockAuthActions with MockSessionService {

  val validBusinessTrade: String = "Test Business Trade"
  val validBusinessName: String = "Test Business Name"
  val journeyType: JourneyType = IncomeSourceJourneyType(Add, SelfEmployment)

  override lazy val app: Application =
    applicationBuilderWithAuthBindings
      .overrides(
        api.inject.bind[SessionService].toInstance(mockSessionService),
        api.inject.bind[ITSAStatusConnector].toInstance(mockItsaStatusConnector),
        api.inject.bind[BusinessDetailsConnector].toInstance(mockBusinessDetailsConnector),
        api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInterface)
      ).build()

  lazy val testAddBusinessTradeController = app.injector.instanceOf[AddBusinessTradeController]

  def getAction(mtdRole: MTDUserRole, mode: Mode, isPost: Boolean = false) = mtdRole match {
    case MTDIndividual if isPost => testAddBusinessTradeController.submit(mode)
    case MTDIndividual => testAddBusinessTradeController.show(mode)
    case _ if isPost => testAddBusinessTradeController.submitAgent(mode)
    case _ => testAddBusinessTradeController.showAgent(mode)
  }

  Seq(CheckMode, NormalMode).foreach { mode =>
    mtdAllRoles.foreach { mtdRole =>
      s"show${if (mtdRole != MTDIndividual) "Agent"}(mode = $mode)" when {
        val action = getAction(mtdRole, mode)
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
        s"the user is authenticated as a $mtdRole" should {
          "render the AddBusinessTrade page" when {
            "using the manage businesses journey" in {
              setupMockSuccess(mtdRole)
              mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Add, SelfEmployment))
                .copy(addIncomeSourceData = Some(AddIncomeSourceData(businessName = Some(validBusinessName)))))))

              val result: Future[Result] = action(fakeRequest)
              status(result) shouldBe OK
            }
          }

          s"return ${Status.SEE_OTHER}: redirect to the relevant You Cannot Go Back page" when {
            "user has already completed the journey" in {
              mockNoIncomeSources()
              setupMockSuccess(mtdRole)
              mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
              setupMockGetMongo(Right(Some(completedUIJourneySessionData(IncomeSourceJourneyType(Add, SelfEmployment)))))

              val result: Future[Result] = action(fakeRequest)
              status(result) shouldBe SEE_OTHER
              val redirectUrl = if (mtdRole != MTDIndividual) {
                controllers.manageBusinesses.add.routes.ReportingMethodSetBackErrorController.showAgent(SelfEmployment).url
              } else {
                controllers.manageBusinesses.add.routes.ReportingMethodSetBackErrorController.show(SelfEmployment).url
              }
              redirectLocation(result) shouldBe Some(redirectUrl)
            }

            "user has already added their income source" in {
              mockNoIncomeSources()
              setupMockSuccess(mtdRole)
              mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
              setupMockGetMongo(Right(Some(addedIncomeSourceUIJourneySessionData(SelfEmployment))))

              val result: Future[Result] = action(fakeRequest)
              status(result) shouldBe SEE_OTHER
              val redirectUrl = if (mtdRole != MTDIndividual) {
                controllers.manageBusinesses.add.routes.IncomeSourceAddedBackErrorController.showAgent(SelfEmployment).url
              } else {
                controllers.manageBusinesses.add.routes.IncomeSourceAddedBackErrorController.show(SelfEmployment).url
              }
              redirectLocation(result) shouldBe Some(redirectUrl)
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
        val action = getAction(mtdRole, mode, true)
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole).withMethod("POST")
        s"the user is authenticated as a $mtdRole" should {
          if (mode == CheckMode) {
            "redirect to the IncomeSourceCheckDetailsController page" when {
              "the business trade entered is valid" in {
                setupMockSuccess(mtdRole)
                mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
                setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
                setupMockCreateSession(true)
                setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Add, SelfEmployment))
                  .copy(addIncomeSourceData = Some(AddIncomeSourceData(businessName = Some(validBusinessName), businessTrade = Some(validBusinessTrade)))))))
                setupMockSetMongoData(true)

                val result: Future[Result] = action(fakeRequest.withFormUrlEncodedBody(
                  BusinessTradeForm.businessTrade -> validBusinessTrade))

                status(result) mustBe SEE_OTHER
                val expectedRedirectUrl = if (mtdRole == MTDIndividual) {
                  controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController.show(SelfEmployment).url
                } else {
                  controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment).url
                }
                redirectLocation(result) mustBe Some(expectedRedirectUrl)
              }
            }
          } else {
            "redirect to the add business address page" when {
              "the individual is authenticated and the business trade entered is valid" in {
                setupMockSuccess(mtdRole)
                mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
                setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
                setupMockCreateSession(true)
                setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Add, SelfEmployment))
                  .copy(addIncomeSourceData = Some(AddIncomeSourceData(businessName = Some(validBusinessName), businessTrade = Some(validBusinessTrade)))))))
                setupMockSetMongoData(true)

                val result: Future[Result] = action(fakeRequest.withFormUrlEncodedBody(
                  BusinessTradeForm.businessTrade -> validBusinessTrade))
                status(result) mustBe SEE_OTHER
                val expectedRedirectUrl = if (mtdRole == MTDIndividual) {
                  controllers.manageBusinesses.add.routes.AddBusinessAddressController.show(mode).url
                } else {
                  controllers.manageBusinesses.add.routes.AddBusinessAddressController.showAgent(mode).url
                }
                redirectLocation(result) mustBe Some(expectedRedirectUrl)
              }
            }
          }

          "return to add business trade page" when {
            "trade name is same as business name" in {
              setupMockSuccess(mtdRole)
              mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

              setupMockCreateSession(true)
              val businessNameAsTrade: String = "Test Name"
              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Add, SelfEmployment))
                .copy(addIncomeSourceData = Some(AddIncomeSourceData(businessName = Some(businessNameAsTrade),
                  businessTrade = Some(businessNameAsTrade)))))))

              val result: Future[Result] = action(fakeRequest.withFormUrlEncodedBody(
                BusinessTradeForm.businessTrade -> businessNameAsTrade))

              status(result) mustBe BAD_REQUEST
              contentAsString(result) must include("Trade and business name cannot be the same")
            }

            "trade name contains invalid characters" in {
              val invalidBusinessTradeChar: String = "££"
              setupMockSuccess(mtdRole)
              mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Add, SelfEmployment))
                .copy(addIncomeSourceData = Some(AddIncomeSourceData(businessName = Some(validBusinessName),
                  businessTrade = Some(invalidBusinessTradeChar)))))))

              val result: Future[Result] = action(fakeRequest.withFormUrlEncodedBody(
                BusinessTradeForm.businessTrade -> invalidBusinessTradeChar))

              status(result) mustBe BAD_REQUEST
              contentAsString(result) must include("Trade cannot include !, &quot;&quot;, * or ?")
            }

            "trade name is empty" in {
              val invalidBusinessTradeEmpty: String = ""
              setupMockSuccess(mtdRole)
              mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Add, SelfEmployment))
                .copy(addIncomeSourceData = Some(AddIncomeSourceData(businessName = Some(validBusinessName),
                  businessTrade = Some(invalidBusinessTradeEmpty)))))))

              val result: Future[Result] = action(fakeRequest.withFormUrlEncodedBody(
                BusinessTradeForm.businessTrade -> invalidBusinessTradeEmpty))

              status(result) mustBe BAD_REQUEST
              contentAsString(result) must include("Enter the trade of your business")
            }

            "trade name is too short" in {
              val invalidBusinessTradeShort: String = "A"
              setupMockSuccess(mtdRole)
              mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Add, SelfEmployment))
                .copy(addIncomeSourceData = Some(AddIncomeSourceData(businessName = Some(validBusinessName),
                  businessTrade = Some(invalidBusinessTradeShort)))))))

              val result: Future[Result] = action(fakeRequest.withFormUrlEncodedBody(
                BusinessTradeForm.businessTrade -> invalidBusinessTradeShort))

              status(result) mustBe BAD_REQUEST
              contentAsString(result) must include("Trade must be 2 characters or more")
            }

            "trade name is too long" in {
              val invalidBusinessTradeLong: String = "This trade name is far too long to be accepted"
              setupMockSuccess(mtdRole)
              mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Add, SelfEmployment))
                .copy(addIncomeSourceData = Some(AddIncomeSourceData(businessName = Some(validBusinessName),
                  businessTrade = Some(invalidBusinessTradeLong)))))))

              val result: Future[Result] = action(fakeRequest.withFormUrlEncodedBody(
                BusinessTradeForm.businessTrade -> invalidBusinessTradeLong))

              status(result) mustBe BAD_REQUEST
              contentAsString(result) must include("Trade must be 35 characters or fewer")
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