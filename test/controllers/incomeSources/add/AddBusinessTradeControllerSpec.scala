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

package controllers.incomeSources.add

import enums.IncomeSourceJourney.SelfEmployment
import enums.JourneyType.{Add, IncomeSourceJourneyType, JourneyType}
import enums.{MTDIndividual, MTDSupportingAgent, MTDUserRole}
import forms.incomeSources.add.BusinessTradeForm
import mocks.auth.MockAuthActions
import mocks.services.MockSessionService
import models.admin.IncomeSourcesFs
import models.incomeSourceDetails.AddIncomeSourceData
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api
import play.api.Application
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.Helpers._
import services.SessionService
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{addedIncomeSourceUIJourneySessionData, businessesAndPropertyIncome, completedUIJourneySessionData, emptyUIJourneySessionData}

import scala.concurrent.Future


class AddBusinessTradeControllerSpec extends MockAuthActions with MockSessionService {

  val validBusinessTrade: String = "Test Business Trade"
  val validBusinessName: String = "Test Business Name"
  val journeyType: JourneyType = IncomeSourceJourneyType(Add, SelfEmployment)

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[SessionService].toInstance(mockSessionService)
    ).build()

  lazy val testAddBusinessTradeController = app.injector.instanceOf[AddBusinessTradeController]

  def getAction(mtdRole: MTDUserRole, isChange: Boolean, isPost: Boolean = false) = mtdRole match {
    case MTDIndividual if isPost => testAddBusinessTradeController.submit(isChange)
    case MTDIndividual => testAddBusinessTradeController.show(isChange)
    case _ if isPost => testAddBusinessTradeController.submitAgent(isChange)
    case _ => testAddBusinessTradeController.showAgent(isChange)
  }

  Seq(true, false).foreach { isChange =>
    mtdAllRoles.foreach { mtdRole =>
      s"show${if (mtdRole != MTDIndividual) "Agent"}(isChange = $isChange)" when {
        val action = getAction(mtdRole, isChange)
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
        s"the user is authenticated as a $mtdRole" should {
          "render the AddBusinessTrade page" when {
            "incomeSources feature is enabled" in {
              enable(IncomeSourcesFs)
              setupMockSuccess(mtdRole)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Add, SelfEmployment))
                .copy(addIncomeSourceData = Some(AddIncomeSourceData(businessName = Some(validBusinessName)))))))

              val result: Future[Result] = action(fakeRequest)
              status(result) shouldBe OK
            }
          }

          "redirect to the home page" when {
            "when feature switch is disabled" in {
              setupMockSuccess(mtdRole)

              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
              setupMockCreateSession(true)

              val result: Future[Result] = action(fakeRequest)
              status(result) shouldBe SEE_OTHER
              val homeUrl = if (mtdRole == MTDIndividual){
                controllers.routes.HomeController.show().url
              } else {
                controllers.routes.HomeController.showAgent().url
              }
              redirectLocation(result) shouldBe Some(homeUrl)
            }
          }

          s"return ${Status.SEE_OTHER}: redirect to the relevant You Cannot Go Back page" when {
            "user has already completed the journey" in {
              enable(IncomeSourcesFs)
              mockNoIncomeSources()
              setupMockSuccess(mtdRole)
              setupMockGetMongo(Right(Some(completedUIJourneySessionData(IncomeSourceJourneyType(Add, SelfEmployment)))))

              val result: Future[Result] = action(fakeRequest)
              status(result) shouldBe SEE_OTHER
              val redirectUrl = if (mtdRole != MTDIndividual) {
                controllers.incomeSources.add.routes.ReportingMethodSetBackErrorController.showAgent(SelfEmployment).url
              } else {
                controllers.incomeSources.add.routes.ReportingMethodSetBackErrorController.show(SelfEmployment).url
              }
              redirectLocation(result) shouldBe Some(redirectUrl)
            }

            "user has already added their income source" in {
              enable(IncomeSourcesFs)
              mockNoIncomeSources()
              setupMockSuccess(mtdRole)
              setupMockGetMongo(Right(Some(addedIncomeSourceUIJourneySessionData(SelfEmployment))))

              val result: Future[Result] = action(fakeRequest)
              status(result) shouldBe SEE_OTHER
              val redirectUrl = if (mtdRole != MTDIndividual) {
                controllers.incomeSources.add.routes.IncomeSourceAddedBackErrorController.showAgent(SelfEmployment).url
              } else {
                controllers.incomeSources.add.routes.IncomeSourceAddedBackErrorController.show(SelfEmployment).url
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

      s"submit${if (mtdRole != MTDIndividual) "Agent"}(isChange = $isChange)" when {
        val action = getAction(mtdRole, isChange, true)
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole).withMethod("POST")
        s"the user is authenticated as a $mtdRole" should {
          if(isChange) {
            "redirect to the IncomeSourceCheckDetailsController page" when {
              "the business trade entered is valid" in {
                enable(IncomeSourcesFs)
                setupMockSuccess(mtdRole)
                setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
                setupMockCreateSession(true)
                setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Add, SelfEmployment))
                  .copy(addIncomeSourceData = Some(AddIncomeSourceData(businessName = Some(validBusinessName), businessTrade = Some(validBusinessTrade)))))))
                setupMockSetMongoData(true)

                val result: Future[Result] = action(fakeRequest.withFormUrlEncodedBody(
                  BusinessTradeForm.businessTrade -> validBusinessTrade))

                status(result) mustBe SEE_OTHER
                val expectedRedirectUrl = if(mtdRole == MTDIndividual) {
                  controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController.show(SelfEmployment).url
                } else {
                  controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment).url
                }
                redirectLocation(result) mustBe Some(expectedRedirectUrl)
              }
            }
          } else {
            "redirect to the add business address page" when {
              "the individual is authenticated and the business trade entered is valid" in {
                enable(IncomeSourcesFs)
                setupMockSuccess(mtdRole)
                setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
                setupMockCreateSession(true)
                setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Add, SelfEmployment))
                  .copy(addIncomeSourceData = Some(AddIncomeSourceData(businessName = Some(validBusinessName), businessTrade = Some(validBusinessTrade)))))))
                setupMockSetMongoData(true)

                val result: Future[Result] = action(fakeRequest.withFormUrlEncodedBody(
                  BusinessTradeForm.businessTrade -> validBusinessTrade))
                status(result) mustBe SEE_OTHER
                val expectedRedirectUrl = if(mtdRole == MTDIndividual) {
                  controllers.incomeSources.add.routes.AddBusinessAddressController.show(isChange).url
                } else {
                  controllers.incomeSources.add.routes.AddBusinessAddressController.showAgent(isChange).url
                }
                redirectLocation(result) mustBe Some(expectedRedirectUrl)
              }
            }
          }

          "return to add business trade page" when {
            "trade name is same as business name" in {
              enable(IncomeSourcesFs)
              setupMockSuccess(mtdRole)
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
              enable(IncomeSourcesFs)
              val invalidBusinessTradeChar: String = "££"
              setupMockSuccess(mtdRole)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Add, SelfEmployment))
                .copy(addIncomeSourceData = Some(AddIncomeSourceData(businessName = Some(validBusinessName),
                  businessTrade = Some(invalidBusinessTradeChar)))))))

              val result: Future[Result] = action(fakeRequest.withFormUrlEncodedBody(
                BusinessTradeForm.businessTrade -> invalidBusinessTradeChar))

              status(result) mustBe BAD_REQUEST
              contentAsString(result) must include("Business trade cannot include !, &quot;&quot;, * or ?")
            }

            "trade name is empty" in {
              enable(IncomeSourcesFs)
              val invalidBusinessTradeEmpty: String = ""
              setupMockSuccess(mtdRole)
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
              enable(IncomeSourcesFs)
              val invalidBusinessTradeShort: String = "A"
              setupMockSuccess(mtdRole)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Add, SelfEmployment))
                .copy(addIncomeSourceData = Some(AddIncomeSourceData(businessName = Some(validBusinessName),
                  businessTrade = Some(invalidBusinessTradeShort)))))))

              val result: Future[Result] = action(fakeRequest.withFormUrlEncodedBody(
                BusinessTradeForm.businessTrade -> invalidBusinessTradeShort))

              status(result) mustBe BAD_REQUEST
              contentAsString(result) must include("Business trade must have at least two letters")
            }

            "trade name is too long" in {
              enable(IncomeSourcesFs)
              val invalidBusinessTradeLong: String = "This trade name is far too long to be accepted"
              setupMockSuccess(mtdRole)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Add, SelfEmployment))
                .copy(addIncomeSourceData = Some(AddIncomeSourceData(businessName = Some(validBusinessName),
                  businessTrade = Some(invalidBusinessTradeLong)))))))

              val result: Future[Result] = action(fakeRequest.withFormUrlEncodedBody(
                BusinessTradeForm.businessTrade -> invalidBusinessTradeLong))

              status(result) mustBe BAD_REQUEST
              contentAsString(result) must include("Business trade must be 35 characters or fewer")
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
