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
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import enums.{MTDIndividual, MTDSupportingAgent, MTDUserRole}
import forms.incomeSources.add.BusinessNameForm
import mocks.auth.MockAuthActions
import mocks.services.MockSessionService
import models.admin.IncomeSourcesFs
import models.incomeSourceDetails.AddIncomeSourceData
import models.incomeSourceDetails.AddIncomeSourceData.{businessNameField, businessTradeField}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers._
import play.api
import play.api.Application
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.Helpers._
import services.SessionService
import testConstants.incomeSources.IncomeSourceDetailsTestConstants._

import scala.concurrent.Future


class AddBusinessNameControllerSpec extends MockAuthActions
  with MockSessionService {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[SessionService].toInstance(mockSessionService)
    ).build()

  lazy val testAddBusinessNameController = app.injector.instanceOf[AddBusinessNameController]

  val validBusinessName: String = "Test Business Name"
  val journeyType: IncomeSourceJourneyType = IncomeSourceJourneyType(Add, SelfEmployment)

  def getValidationErrorTabTitle(): String = {
    s"${messages("htmlTitle.invalidInput", messages("add-business-name.heading"))}"
  }

  def getAction(mtdRole: MTDUserRole, isChange: Boolean, isPost: Boolean = false) = mtdRole match {
    case MTDIndividual if isPost => testAddBusinessNameController.submit(isChange)
    case MTDIndividual => testAddBusinessNameController.show(isChange)
    case _ if isPost => testAddBusinessNameController.submitAgent(isChange)
    case _ => testAddBusinessNameController.showAgent(isChange)
  }

  Seq(true, false).foreach { isChange =>
    mtdAllRoles.foreach { mtdRole =>
      s"show${if(mtdRole != MTDIndividual)"Agent"}(isChange = $isChange)" when {
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
        val action = getAction(mtdRole, isChange)
        s"the user is authenticated as a $mtdRole" should {
          "return 200 OK" when {
            "feature switch is enabled" in {
              setupMockSuccess(mtdRole)
              enable(IncomeSourcesFs)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
              if (isChange) {
                setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Add, SelfEmployment)))))
                setupMockGetSessionKeyMongoTyped[String](businessNameField, journeyType, Right(Some(validBusinessName)))
                setupMockGetSessionKeyMongoTyped[String](businessTradeField, journeyType, Right(Some("Test Business Trade")))
              }
              else {
                setupMockCreateSession(true)
                setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Add, SelfEmployment)))))
              }

              val result: Future[Result] = action(fakeRequest)

              status(result) mustBe OK
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
              setupMockSuccess(mtdRole)
              mockNoIncomeSources()
              setupMockGetMongo(Right(Some(completedUIJourneySessionData(IncomeSourceJourneyType(Add, SelfEmployment)))))

              val result: Future[Result] = action(fakeRequest)
              status(result) shouldBe SEE_OTHER
              val redirectUrl = if (mtdRole == MTDIndividual) {
                controllers.incomeSources.add.routes.ReportingMethodSetBackErrorController.show(SelfEmployment).url
              } else {
                controllers.incomeSources.add.routes.ReportingMethodSetBackErrorController.showAgent(SelfEmployment).url
              }
              redirectLocation(result) shouldBe Some(redirectUrl)
            }

            "user has already added their income source" in {
              enable(IncomeSourcesFs)
              setupMockSuccess(mtdRole)
              mockNoIncomeSources()
              setupMockGetMongo(Right(Some(addedIncomeSourceUIJourneySessionData(SelfEmployment))))

              val result: Future[Result] = action(fakeRequest)
              status(result) shouldBe SEE_OTHER
              val redirectUrl = if (mtdRole == MTDIndividual) {
                controllers.incomeSources.add.routes.IncomeSourceAddedBackErrorController.show(SelfEmployment).url
              } else {
                controllers.incomeSources.add.routes.IncomeSourceAddedBackErrorController.showAgent(SelfEmployment).url
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
      s"submit${if(mtdRole != MTDIndividual)"Agent"}(isChange = $isChange)" when {
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole).withMethod("POST")
        val action = getAction(mtdRole, isChange, true)
        s"the user is authenticated as a $mtdRole" should {
          if(isChange) {
            "return 303 and redirect to check details page" when {
              "the business name entered is valid" in {
                enable(IncomeSourcesFs)
                setupMockSuccess(mtdRole)
                setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
                setupMockCreateSession(true)
                setupMockSetMongoData(true)
                setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Add, SelfEmployment)))))

                val result = action(fakeRequest.withFormUrlEncodedBody(
                  BusinessNameForm.businessName -> validBusinessName))

                val redirectUrl = if(mtdRole == MTDIndividual) {
                  controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController.show(SelfEmployment).url
                } else {
                  controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment).url
                }

                status(result) mustBe SEE_OTHER
                redirectLocation(result) mustBe Some(redirectUrl)
              }
            }
          } else {
            "return 303 and redirect to add business start date" when {
              "the business name entered is valid" in {
                enable(IncomeSourcesFs)
                setupMockSuccess(mtdRole)
                setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
                setupMockCreateSession(true)
                setupMockSetMongoData(true)
                setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Add, SelfEmployment)))))

                val result = action(fakeRequest.withFormUrlEncodedBody(
                  BusinessNameForm.businessName -> validBusinessName))

                status(result) mustBe SEE_OTHER
                redirectLocation(result) mustBe Some(
                  controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(mtdRole != MTDIndividual, false, SelfEmployment).url
                )
              }
            }
          }

          "show AddBusinessName with error" when {
            "Business name is empty" in {
              enable(IncomeSourcesFs)
              setupMockSuccess(mtdRole)
              setupMockCreateSession(true)
              setupMockSetMongoData(true)
              val invalidBusinessNameEmpty: String = ""
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Add, SelfEmployment)))))

              val result: Future[Result] = action(fakeRequest.withFormUrlEncodedBody(
                BusinessNameForm.businessName -> invalidBusinessNameEmpty))

              status(result) mustBe BAD_REQUEST
              contentAsString(result) must include("Enter your name or the name of your business")

              val document: Document = Jsoup.parse(contentAsString(result))
              document.title shouldBe getValidationErrorTabTitle()
            }

            "Business name is too long" in {
              enable(IncomeSourcesFs)
              val invalidBusinessNameLength: String = "ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ"
              setupMockSuccess(mtdRole)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Add, SelfEmployment)))))

              val result: Future[Result] = action(fakeRequest.withFormUrlEncodedBody(
                BusinessNameForm.businessName -> invalidBusinessNameLength))

              status(result) mustBe BAD_REQUEST
              contentAsString(result) must include("Business name must be 105 characters or fewer")
            }

            "Business name has invalid characters" in {
              enable(IncomeSourcesFs)
              val invalidBusinessNameEmpty: String = "££"
              setupMockSuccess(mtdRole)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Add, SelfEmployment)))))

              val result: Future[Result] =action(fakeRequest.withFormUrlEncodedBody(
                BusinessNameForm.businessName -> invalidBusinessNameEmpty))

              status(result) mustBe BAD_REQUEST
              contentAsString(result) must include("Business name cannot include !, &quot;&quot;, * or ?")
            }

            if (isChange) {
              "show invalid error when business name is same as business trade name" in {
                enable(IncomeSourcesFs)
                setupMockSuccess(mtdRole)
                setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
                setupMockCreateSession(true)
                val businessName: String = "Plumbing"
                setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Add, SelfEmployment))
                  .copy(addIncomeSourceData = Some(AddIncomeSourceData(businessName = Some(businessName),
                    businessTrade = Some(businessName)))))))

                val result: Future[Result] =action(fakeRequest.withFormUrlEncodedBody(
                  BusinessNameForm.businessName -> businessName))

                status(result) mustBe BAD_REQUEST
                contentAsString(result) must include("Trade and business name cannot be the same")
              }
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
