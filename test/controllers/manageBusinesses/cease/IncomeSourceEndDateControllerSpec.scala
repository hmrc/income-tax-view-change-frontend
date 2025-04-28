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
import mocks.auth.MockAuthActions
import mocks.services.MockSessionService
import models.admin.IncomeSourcesFs
import models.core.IncomeSourceId.mkIncomeSourceId
import models.core.{CheckMode, Mode, NormalMode}
import models.incomeSourceDetails.CeaseIncomeSourceData
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api
import play.api.http.Status
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc._
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.{DateService, SessionService}
import testConstants.BaseTestConstants.testSelfEmploymentId
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{completedUIJourneySessionData, emptyUIJourneySessionData, notCompletedUIJourneySessionData}

import scala.concurrent.Future

class IncomeSourceEndDateControllerSpec extends MockAuthActions with MockSessionService {

  override lazy val app = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[SessionService].toInstance(mockSessionService),
      api.inject.bind[DateService].toInstance(dateService),
    ).build()

  lazy val testController = app.injector.instanceOf[IncomeSourceEndDateController]

  def heading(incomeSourceType: IncomeSourceType): String = {
    incomeSourceType match {
      case SelfEmployment => messages("incomeSources.cease.endDate.selfEmployment.heading")
      case UkProperty => messages("incomeSources.cease.endDate.ukProperty.heading")
      case ForeignProperty => messages("incomeSources.cease.endDate.foreignProperty.heading")
    }
  }

  def title(incomeSourceType: IncomeSourceType, isAgent: Boolean): String = {
    if (isAgent)
      s"${messages("htmlTitle.agent", heading(incomeSourceType))}"
    else
      s"${messages("htmlTitle", heading(incomeSourceType))}"
  }

  def getValidationErrorTabTitle(incomeSourceType: IncomeSourceType): String = {
    s"${messages("htmlTitle.invalidInput", heading(incomeSourceType))}"
  }

  def getActions(isAgent: Boolean, incomeSourceType: IncomeSourceType, id: Option[String], mode: Mode): (Call, Call, Call) = {
    val manageBusinessesCall = if (isAgent) {
      controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent()
    } else {
      controllers.manageBusinesses.routes.ManageYourBusinessesController.show()
    }
    (manageBusinessesCall,
      routes.IncomeSourceEndDateController.submit(id = id, incomeSourceType = incomeSourceType, isAgent = isAgent, mode = mode),
      manageBusinessesCall)
  }

  val incomeSourceTypes = List(SelfEmployment, UkProperty, ForeignProperty)

  mtdAllRoles.foreach { mtdRole =>
    incomeSourceTypes.foreach { incomeSourceType =>
      List(CheckMode, NormalMode).foreach { mode =>
        val isAgent = mtdRole != MTDIndividual
        val optIncomeSourceIdHash = if (incomeSourceType == SelfEmployment) {
          Some(mkIncomeSourceId(testSelfEmploymentId).toHash.hash)
        } else {
          None
        }
        s"show($incomeSourceType, $isAgent, $mode)" when {
          val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
          val action = testController.show(optIncomeSourceIdHash, incomeSourceType, isAgent, mode)
          s"the user is authenticated as a $mtdRole" should {
            "render the end date page" when {
              "incomeSource feature switch is enabled" in {
                setupMockSuccess(mtdRole)
                enable(IncomeSourcesFs)
                mockBothPropertyBothBusiness()
                if (mode == CheckMode) {
                  setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Cease, incomeSourceType)))))
                } else {
                  setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Cease, incomeSourceType)))))
                }

                val result: Future[Result] = action(fakeRequest)
                val document: Document = Jsoup.parse(contentAsString(result))
                val (backAction, postAction, _) = getActions(
                  isAgent = isAgent,
                  incomeSourceType = incomeSourceType,
                  id = optIncomeSourceIdHash,
                  mode = mode)

                status(result) shouldBe OK
                document.title shouldBe title(incomeSourceType, isAgent = isAgent)
                document.select("h1").text shouldBe heading(incomeSourceType)
                document.getElementById("back-fallback").attr("href") shouldBe backAction.url
                document.getElementById("income-source-end-date-form").attr("action") shouldBe postAction.url

                if (mode == CheckMode) {
                  document.getElementById("value.day").`val`() shouldBe "10"
                  document.getElementById("value.month").`val`() shouldBe "10"
                  document.getElementById("value.year").`val`() shouldBe "2022"
                }
              }
            }

            "return 303 SEE_OTHER and redirect to home page" when {
              "navigating to the page with FS Disabled" in {
                setupMockSuccess(mtdRole)
                disable(IncomeSourcesFs)
                mockBusinessIncomeSource()
                val result: Future[Result] = action(fakeRequest)
                status(result) shouldBe SEE_OTHER
              }
            }

            "redirect to the Cannot Go Back page" when {
              "journey is complete" in {
                setupMockSuccess(mtdRole)
                enable(IncomeSourcesFs)
                mockBothPropertyBothBusiness()
                setupMockCreateSession(true)
                setupMockGetMongo(Right(Some(completedUIJourneySessionData(IncomeSourceJourneyType(Cease, incomeSourceType)))))
                val result: Future[Result] = action(fakeRequest)
                val expectedRedirectUrl = if (isAgent) {
                  routes.IncomeSourceCeasedBackErrorController.showAgent(incomeSourceType).url
                } else {
                  routes.IncomeSourceCeasedBackErrorController.show(incomeSourceType).url
                }

                status(result) shouldBe SEE_OTHER
                redirectLocation(result) shouldBe Some(expectedRedirectUrl)
              }
            }

            if (incomeSourceType == SelfEmployment) {
              "return 500 INTERNAL SERVER ERROR to internal server page" when {
                "income source ID is missing" in {
                  setupMockSuccess(mtdRole)
                  enable(IncomeSourcesFs)
                  mockBothPropertyBothBusiness()

                  setupMockCreateSession(true)
                  setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Cease, incomeSourceType)))))
                  val actionNoId = testController.show(None, incomeSourceType, isAgent, mode)
                  val result: Future[Result] = actionNoId(fakeRequest)
                  status(result) shouldBe INTERNAL_SERVER_ERROR
                }
                "incomeSourceIdHash in URL does not match any incomeSourceIdHash in database" in {
                  setupMockSuccess(mtdRole)
                  enable(IncomeSourcesFs)
                  mockBothPropertyBothBusiness()

                  setupMockCreateSession(true)
                  setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Cease, incomeSourceType)))))
                  val actionIdNotRec = testController.show(Some("12345"), incomeSourceType, isAgent, mode)
                  val result: Future[Result] = actionIdNotRec(fakeRequest)
                  status(result) shouldBe INTERNAL_SERVER_ERROR
                }
              }
            }
          }
          testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
        }

        s"submit($incomeSourceType, $isAgent, $mode)" when {
          val fakeRequest = fakePostRequestBasedOnMTDUserType(mtdRole).withMethod("POST")
          val action = testController.submit(optIncomeSourceIdHash, incomeSourceType, isAgent, mode)
          s"the user is authenticated as a $mtdRole" should {
            "redirect to CheckIncomeSourceDetails" when {
              "form is completed successfully" in {
                setupMockSuccess(mtdRole)
                enable(IncomeSourcesFs)
                mockBothPropertyBothBusiness()
                setupMockCreateSession(true)
                if (incomeSourceType == SelfEmployment) {
                  setupMockSetMultipleMongoData(Right(true))
                } else {
                  setupMockSetSessionKeyMongo(Right(true))
                }
                if (mode == CheckMode) {
                  setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Cease, incomeSourceType)))))
                } else {
                  setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Cease, incomeSourceType)))))
                }
                val result: Future[Result] = action(fakeRequest.withFormUrlEncodedBody("value.day" -> "27", "value.month" -> "8",
                  "value.year" -> "2022"))

                val redirectLocationResult = if (isAgent) controllers.manageBusinesses.cease.routes.CeaseCheckIncomeSourceDetailsController.showAgent(incomeSourceType).url
                else controllers.manageBusinesses.cease.routes.CeaseCheckIncomeSourceDetailsController.show(incomeSourceType).url

                status(result) shouldBe Status.SEE_OTHER
                redirectLocation(result) shouldBe Some(redirectLocationResult)
              }
            }
            "display date errors" when {
              "form is errored out with before trading start date error" in {
                setupMockSuccess(mtdRole)
                enable(IncomeSourcesFs)
                mockBothPropertyBothBusiness()
                setupMockCreateSession(true)
                if (incomeSourceType == SelfEmployment) {
                  setupMockSetMultipleMongoData(Right(true))
                } else {
                  setupMockSetSessionKeyMongo(Right(true))
                }
                setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Cease, incomeSourceType)))))
                val result: Future[Result] = action(fakeRequest.withFormUrlEncodedBody("value.day" -> "27", "value.month" -> "8",
                  "value.year" -> "2018"))

                val expectedErrorMessage: (String, String) = incomeSourceType match {
                  case SelfEmployment => (
                    "There is a problem The date your sole trader business stopped must be on or after the date it started trading",
                    "Error:: The date your sole trader business stopped must be on or after the date it started trading"
                  )
                  case UkProperty => (
                    "There is a problem The date your UK property business stopped must be on or after the date it started trading",
                    "Error:: The date your UK property business stopped must be on or after the date it started trading"
                  )
                  case ForeignProperty => (
                    "There is a problem The date your foreign property business stopped must be on or after the date it started trading",
                    "Error:: The date your foreign property business stopped must be on or after the date it started trading"
                  )
                }

                status(result) shouldBe Status.BAD_REQUEST
                val document: Document = Jsoup.parse(contentAsString(result))
                document.title shouldBe getValidationErrorTabTitle(incomeSourceType)
                document.getElementById("error-summary").text() shouldBe expectedErrorMessage._1
                document.getElementById("value-error").text() shouldBe expectedErrorMessage._2
              }
              "form is errored out with future date error" in {
                setupMockSuccess(mtdRole)
                enable(IncomeSourcesFs)
                mockBothPropertyBothBusiness()
                setupMockCreateSession(true)
                if (incomeSourceType == SelfEmployment) {
                  setupMockSetMultipleMongoData(Right(true))
                } else {
                  setupMockSetSessionKeyMongo(Right(true))
                }
                setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Cease, incomeSourceType)))))
                val result: Future[Result] = action(fakeRequest.withFormUrlEncodedBody("value.day" -> "27", "value.month" -> "8",
                  "value.year" -> (dateService.getCurrentDate.getYear + 1).toString))

                val expectedErrorMessage: (String, String) = incomeSourceType match {
                  case SelfEmployment => (
                    "There is a problem The date your sole trader business stopped must be today or in the past",
                    "Error:: The date your sole trader business stopped must be today or in the past"
                  )
                  case UkProperty => (
                    "There is a problem The date your UK property business stopped must be today or in the past",
                    "Error:: The date your UK property business stopped must be today or in the past"
                  )
                  case ForeignProperty => (
                    "There is a problem The date your foreign property business stopped must be today or in the past",
                    "Error:: The date your foreign property business stopped must be today or in the past"
                  )
                }

                status(result) shouldBe Status.BAD_REQUEST
                val document: Document = Jsoup.parse(contentAsString(result))
                document.title shouldBe getValidationErrorTabTitle(incomeSourceType)
                document.getElementById("error-summary").text() shouldBe expectedErrorMessage._1
                document.getElementById("value-error").text() shouldBe expectedErrorMessage._2
              }
              "form is errored out with earliest date error for SelfEmployment" in {
                if (incomeSourceType == SelfEmployment) {
                  setupMockSuccess(mtdRole)
                  enable(IncomeSourcesFs)
                  mockSoleTraderWithStartDate2005()
                  setupMockCreateSession(true)
                  if (incomeSourceType == SelfEmployment) {
                    setupMockSetMultipleMongoData(Right(true))
                  } else {
                    setupMockSetSessionKeyMongo(Right(true))
                  }
                  setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Cease, incomeSourceType)))))
                  val result: Future[Result] = action(fakeRequest.withFormUrlEncodedBody("value.day" -> "27", "value.month" -> "8",
                    "value.year" -> "2014"))

                  status(result) shouldBe Status.BAD_REQUEST
                  val document: Document = Jsoup.parse(contentAsString(result))
                  document.title shouldBe getValidationErrorTabTitle(incomeSourceType)
                  document.getElementById("error-summary").text() shouldBe "There is a problem The date your sole trader business stopped cannot be earlier than the 6th of April 2015"
                  document.getElementById("value-error").text() shouldBe "Error:: The date your sole trader business stopped cannot be earlier than the 6th of April 2015"
                }
              }
            }
            "return 400 BAD_REQUEST" when {
              "the form is not completed successfully" in {
                setupMockSuccess(mtdRole)
                enable(IncomeSourcesFs)
                mockBusinessIncomeSource()
                val result: Future[Result] = action(fakeRequest
                  .withFormUrlEncodedBody("value.day" -> "", "value.month" -> "8",
                    "value.year" -> "2022"))

                status(result) shouldBe Status.BAD_REQUEST

                val document: Document = Jsoup.parse(contentAsString(result))
                document.title shouldBe getValidationErrorTabTitle(incomeSourceType)
                document.getElementsByClass("govuk-error-message").text() shouldBe "Error:: The date must include a day"
              }
            }
            if (incomeSourceType != ForeignProperty) {
              "return 500 INTERNAL SERVER ERROR to internal server page" when {
                if (incomeSourceType == SelfEmployment) {
                  "income source ID is missing" in {
                    setupMockSuccess(mtdRole)
                    enable(IncomeSourcesFs)
                    mockBothPropertyBothBusiness()

                    val actionMissingId = testController.submit(None, incomeSourceType, isAgent, mode)
                    val result: Future[Result] = actionMissingId(fakeRequest.withFormUrlEncodedBody("value.day" -> "27", "value.month" -> "8",
                      "value.year" -> "2022"))

                    status(result) shouldBe INTERNAL_SERVER_ERROR
                  }
                  "incomeSourceIdHash in URL does not match any incomeSourceIdHash in database" in {
                    setupMockSuccess(mtdRole)
                    enable(IncomeSourcesFs)
                    mockBothPropertyBothBusiness()

                    val actionInvalidId = testController.submit(Some("12345"), incomeSourceType, isAgent, mode)

                    val result: Future[Result] = actionInvalidId(fakeRequest.withFormUrlEncodedBody("value.day" -> "27", "value.month" -> "8",
                      "value.year" -> "2022"))

                    status(result) shouldBe INTERNAL_SERVER_ERROR
                  }

                  "unable to set incomeSourceIdField session data" in {
                    setupMockSuccess(mtdRole)
                    enable(IncomeSourcesFs)
                    mockBothPropertyBothBusiness()

                    setupMockCreateSession(true)
                    setupMockSetSessionKeyMongo(CeaseIncomeSourceData.dateCeasedField)(Right(true))
                    setupMockSetMultipleMongoData(Left(new Exception()))
                    val result: Future[Result] = action(fakeRequest.withFormUrlEncodedBody("value.day" -> "27", "value.month" -> "8",
                      "value.year" -> "2022"))
                    status(result) shouldBe INTERNAL_SERVER_ERROR
                  }
                } else {
                  "unable to set dateCeased session data" in {
                    setupMockSuccess(mtdRole)
                    enable(IncomeSourcesFs)
                    mockBothPropertyBothBusiness()

                    setupMockCreateSession(true)
                    setupMockSetSessionKeyMongo(CeaseIncomeSourceData.dateCeasedField)(Left(new Exception()))
                    val result = action(fakeRequest.withFormUrlEncodedBody("value.day" -> "27", "value.month" -> "8",
                      "value.year" -> "2022"))

                    status(result) shouldBe INTERNAL_SERVER_ERROR
                  }
                }
              }
            }
          }
          testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
        }
      }
    }
  }
}


