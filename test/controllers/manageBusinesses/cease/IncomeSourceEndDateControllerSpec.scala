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

import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Cease, JourneyType}
import forms.incomeSources.cease.CeaseIncomeSourceEndDateFormProvider
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.MockSessionService
import models.admin.{IncomeSources, IncomeSourcesNewJourney}
import models.core.IncomeSourceId.mkIncomeSourceId
import models.incomeSourceDetails.CeaseIncomeSourceData
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.isA
import org.mockito.Mockito.mock
import org.scalatest.Assertion
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testIndividualAuthSuccessWithSaUtrResponse, testSelfEmploymentId}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{completedUIJourneySessionData, emptyUIJourneySessionData, notCompletedUIJourneySessionData}
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpClient
import views.html.manageBusinesses.cease.IncomeSourceEndDate

import scala.concurrent.Future

class IncomeSourceEndDateControllerSpec extends TestSupport with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate
  with FeatureSwitching with MockSessionService {

  val mockHttpClient: HttpClient = mock(classOf[HttpClient])
  val validCeaseDate: String = "2022-08-27"

  object TestIncomeSourceEndDateController extends IncomeSourceEndDateController(
    mockAuthService,
    app.injector.instanceOf[IncomeSourceEndDate],
    sessionService = mockSessionService,
    app.injector.instanceOf[CeaseIncomeSourceEndDateFormProvider],
    testAuthenticator)(appConfig,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec,
    dateService,
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler])

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

    def getActions(isAgent: Boolean, incomeSourceType: IncomeSourceType, id: Option[String], isChange: Boolean): (Call, Call, Call) = {
      val manageBusinessesCall = if(isAgent) {
        controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent()
      } else {
        controllers.manageBusinesses.routes.ManageYourBusinessesController.show()
      }
      (manageBusinessesCall,
        routes.IncomeSourceEndDateController.submit(id = id, incomeSourceType = incomeSourceType, isAgent = isAgent, isChange = isChange),
        manageBusinessesCall)

    }

    "IncomeSourceEndDateController show/showChange/showAgent/showChangeAgent" should {
      "return 200 OK" when {
        def testShowResponse(id: Option[String], incomeSourceType: IncomeSourceType, isAgent: Boolean, isChange: Boolean): Unit = {

          setupMockAuthorisationSuccess(isAgent)
          disableAllSwitches()
          enable(IncomeSources)
          mockBothPropertyBothBusiness()
          if (isChange) {
            setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(JourneyType(Cease, incomeSourceType)))))
          } else {
            setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Cease, incomeSourceType)))))
          }


          val result: Future[Result] = (isAgent, isChange) match {
            case (true, true) =>
              TestIncomeSourceEndDateController.show(id, incomeSourceType, isAgent, isChange)(fakeRequestConfirmedClient())
            case (true, false) =>
              TestIncomeSourceEndDateController.show(id, incomeSourceType, isAgent, isChange)(fakeRequestConfirmedClient())
            case (false, true) =>
              TestIncomeSourceEndDateController.show(id, incomeSourceType, isAgent, isChange)(fakeRequestWithActiveSession)
            case (false, false) =>
              TestIncomeSourceEndDateController.show(id, incomeSourceType, isAgent, isChange)(fakeRequestWithActiveSession)
          }

          val document: Document = Jsoup.parse(contentAsString(result))
          val (backAction, postAction, _) = getActions(
            isAgent = isAgent,
            incomeSourceType = incomeSourceType,
            id = id,
            isChange = isChange)

          status(result) shouldBe OK
          document.title shouldBe title(incomeSourceType, isAgent = isAgent)
          document.select("h1").text shouldBe heading(incomeSourceType)
          document.getElementById("back-fallback").attr("href") shouldBe backAction.url
          document.getElementById("income-source-end-date-form").attr("action") shouldBe postAction.url

          if (isChange) {
            document.getElementById("value.day").`val`() shouldBe "10"
            document.getElementById("value.month").`val`() shouldBe "10"
            document.getElementById("value.year").`val`() shouldBe "2022"

          }
        }

        "navigating to the page with FS Enabled with income source type as Self Employment" when {
          val incomeSourceType = SelfEmployment
          val id = Some(mkIncomeSourceId(testSelfEmploymentId).toHash.hash)

          "called .show" when {
            "user is an Individual" in {
              testShowResponse(id = id, incomeSourceType = incomeSourceType, isAgent = false, isChange = false)
            }
            "user is an Agent" in {
              testShowResponse(id = id, incomeSourceType = incomeSourceType, isAgent = true, isChange = false)
            }
          }
          "called .showChange" when {
            "user is an Individual" in {
              testShowResponse(id = id, incomeSourceType = incomeSourceType, isAgent = false, isChange = true)
            }
            "user is an Agent" in {
              testShowResponse(id = id, incomeSourceType = incomeSourceType, isAgent = true, isChange = true)
            }
          }
        }

        "navigating to the page with FS Enabled with income source type as Foreign Property" when {
          val incomeSourceType = ForeignProperty
          val id = None

          "called .show" when {
            "user is an Individual" in {
              testShowResponse(id = id, incomeSourceType = incomeSourceType, isAgent = false, isChange = false)
            }
            "user is an Agent" in {
              testShowResponse(id = id, incomeSourceType = incomeSourceType, isAgent = true, isChange = false)
            }
          }
          "called .showChange" when {
            "user is an Individual" in {
              testShowResponse(id = id, incomeSourceType = incomeSourceType, isAgent = false, isChange = true)
            }
            "user is an Agent" in {
              testShowResponse(id = id, incomeSourceType = incomeSourceType, isAgent = true, isChange = true)
            }
          }
        }

        "navigating to the page with FS Enabled with income source type as UK Property" when {
          val incomeSourceType = UkProperty
          val id = None

          "called .show" when {
            "user is an Individual" in {
              testShowResponse(id = id, incomeSourceType = incomeSourceType, isAgent = false, isChange = false)
            }
            "user is an Agent" in {
              testShowResponse(id = id, incomeSourceType = incomeSourceType, isAgent = true, isChange = false)
            }
          }
          "called .showChange" when {
            "user is an Individual" in {
              testShowResponse(id = id, incomeSourceType = incomeSourceType, isAgent = false, isChange = true)
            }
            "user is an Agent" in {
              testShowResponse(id = id, incomeSourceType = incomeSourceType, isAgent = true, isChange = true)
            }
          }
        }
      }
      "return 303 SEE_OTHER and redirect to home page" when {
        "navigating to the page with FS Disabled" when {
          def testFSDisabled(isAgent: Boolean, isChange: Boolean): Unit = {
            if (isAgent) setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
            else setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
            disable(IncomeSources)
            mockBusinessIncomeSource()

            val incomeSourceType = SelfEmployment
            val id = Some(testSelfEmploymentId)

            val result: Future[Result] = (isAgent, isChange) match {
              case (true, true) =>
                TestIncomeSourceEndDateController.show(id = id, incomeSourceType, isAgent, isChange)(fakeRequestConfirmedClient())
              case (true, false) =>
                TestIncomeSourceEndDateController.show(id = id, incomeSourceType, isAgent, isChange)(fakeRequestConfirmedClient())
              case (false, true) =>
                TestIncomeSourceEndDateController.show(id = id, incomeSourceType, isAgent, isChange)(fakeRequestWithActiveSession)
              case (false, false) =>
                TestIncomeSourceEndDateController.show(id = id, incomeSourceType, isAgent, isChange)(fakeRequestWithActiveSession)
            }

            status(result) shouldBe SEE_OTHER
          }

          "called .show" when {
            "user is an Individual" in {
              testFSDisabled(isAgent = false, isChange = false)
            }
            "user is an Agent" in {
              testFSDisabled(isAgent = true, isChange = false)
            }
          }
          "called .showChange" when {
            "user is an Individual" in {
              testFSDisabled(isAgent = false, isChange = true)
            }
            "user is an Agent" in {
              testFSDisabled(isAgent = true, isChange = true)
            }
          }
        }
        "called with an unauthenticated user" when {
          def testUnauthenticatedUser(isAgent: Boolean, isChange: Boolean): Unit = {

            val incomeSourceType = SelfEmployment
            val id = Some(testSelfEmploymentId)
            val result: Future[Result] = (isAgent, isChange) match {
              case (true, true) =>
                setupMockAgentAuthorisationException()
                TestIncomeSourceEndDateController.show(id = id, incomeSourceType, isAgent, isChange)(fakeRequestConfirmedClient())
              case (true, false) =>
                setupMockAgentAuthorisationException()
                TestIncomeSourceEndDateController.show(id = id, incomeSourceType, isAgent, isChange)(fakeRequestConfirmedClient())
              case (false, true) =>
                setupMockAuthorisationException()
                TestIncomeSourceEndDateController.show(id = id, incomeSourceType, isAgent, isChange)(fakeRequestWithActiveSession)
              case (false, false) =>
                setupMockAuthorisationException()
                TestIncomeSourceEndDateController.show(id = id, incomeSourceType, isAgent, isChange)(fakeRequestWithActiveSession)
            }

            status(result) shouldBe SEE_OTHER
          }

          "called .show" when {
            "user is an Individual" in {
              testUnauthenticatedUser(isAgent = false, isChange = false)
            }
            "user is an Agent" in {
              testUnauthenticatedUser(isAgent = true, isChange = false)
            }
          }
          "called .showChange" when {
            "user is an Individual" in {
              testUnauthenticatedUser(isAgent = false, isChange = true)
            }
            "user is an Agent" in {
              testUnauthenticatedUser(isAgent = true, isChange = true)
            }
          }
        }
      }
      "return 500 INTERNAL SERVER ERROR to internal server page" when {
        def testInternalServerErrors(isAgent: Boolean, incomeSourceType: IncomeSourceType, isChange: Boolean = false, id: Option[String] = None): Unit = {
          if (isAgent) setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          else setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
          disableAllSwitches()
          enable(IncomeSources)
          mockBothPropertyBothBusiness()

          setupMockCreateSession(true)
          setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Cease, incomeSourceType)))))


          val result: Future[Result] = if (isChange && !isAgent) {
            TestIncomeSourceEndDateController.show(id, incomeSourceType, isAgent, isChange)(fakeRequestWithActiveSession)
          } else if (isAgent) {
            TestIncomeSourceEndDateController.show(id, incomeSourceType, isAgent, isChange)(fakeRequestConfirmedClient())
          } else {
            TestIncomeSourceEndDateController.show(id, incomeSourceType, isAgent, isChange)(fakeRequestWithActiveSession)
          }
          status(result) shouldBe INTERNAL_SERVER_ERROR
        }

        "income source ID is missing and income source type is Self Employment " when {
          val incomeSourceType = SelfEmployment
          "user is an Individual" in {
            testInternalServerErrors(isAgent = false, incomeSourceType)
          }
          "user is an Agent" in {
            testInternalServerErrors(isAgent = true, incomeSourceType)
          }
        }
        "incomeSourceIdHash in URL does not match any incomeSourceIdHash in database" when {
          "called .show" in {
            testInternalServerErrors(isAgent = false, SelfEmployment, id = Some("12345"))
          }
          "called .showAgent" in {
            testInternalServerErrors(isAgent = true, SelfEmployment, id = Some("12345"))
          }
        }
      }
      "redirect to the Cannot Go Back page" when {
        def setupCompletedCeaseJourney(id: Option[String], isAgent: Boolean, isChange: Boolean, incomeSourceType: IncomeSourceType): Assertion = {
          setupMockAuthorisationSuccess(isAgent)
          mockBothPropertyBothBusiness()
          setupMockCreateSession(true)
          setupMockGetMongo(Right(Some(completedUIJourneySessionData(JourneyType(Cease, incomeSourceType)))))

          val result = if (isAgent) {
            TestIncomeSourceEndDateController.show(id, incomeSourceType, isAgent, isChange)(fakeRequestConfirmedClient())
          } else {
            TestIncomeSourceEndDateController.show(id, incomeSourceType, isAgent, isChange)(fakeRequestWithActiveSession)
          }

          val expectedRedirectUrl = if (isAgent) {
            routes.IncomeSourceCeasedBackErrorController.showAgent(incomeSourceType).url
          } else {
            routes.IncomeSourceCeasedBackErrorController.show(incomeSourceType).url
          }

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(expectedRedirectUrl)
        }

        "UK Property journey is complete - Individual" in {
          setupCompletedCeaseJourney(None, isAgent = false, isChange = false, UkProperty)
        }
        "UK Property journey is complete - Agent" in {
          setupCompletedCeaseJourney(None, isAgent = true, isChange = false, UkProperty)
        }
        "Foreign Property journey is complete - Individual" in {
          setupCompletedCeaseJourney(None, isAgent = false, isChange = false, ForeignProperty)
        }
        "Foreign Property journey is complete - Agent" in {
          setupCompletedCeaseJourney(None, isAgent = true, isChange = false, ForeignProperty)
        }
        "Self Employment journey is complete - Individual" in {
          setupCompletedCeaseJourney(Some(testSelfEmploymentId), isAgent = false, isChange = false, SelfEmployment)
        }
        "Self Employment journey is complete - Agent" in {
          setupCompletedCeaseJourney(Some(testSelfEmploymentId), isAgent = true, isChange = false, SelfEmployment)
        }
      }
    }
    "IncomeSourceEndDateController submit/submitChange/submitAgent/submitChangeAgent" should {
      "return 303 SEE_OTHER" when {
        def testSubmitResponse(id: Option[String], incomeSourceType: IncomeSourceType, isAgent: Boolean, isChange: Boolean): Unit = {
          implicit class FormEncoding(request: FakeRequest[AnyContentAsEmpty.type]) {
            def withDateInFormEncoding: FakeRequest[AnyContentAsFormUrlEncoded] = request.withMethod("POST")
              .withFormUrlEncodedBody("value.day" -> "27", "value.month" -> "8",
                "value.year" -> "2022")
          }

          setupMockAuthorisationSuccess(isAgent)
          disableAllSwitches()
          enable(IncomeSources)
          mockBothPropertyBothBusiness()
          setupMockCreateSession(true)
          println("INCOME SOURCE TYPE YAYA "+ incomeSourceType)
          if (incomeSourceType == SelfEmployment) {setupMockSetMultipleMongoData(Right(true))}
          else {setupMockSetSessionKeyMongo(Right(true))}
          if (isChange) {
            setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(JourneyType(Cease, incomeSourceType)))))
          } else {
            setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Cease, incomeSourceType)))))
          }

          val result: Future[Result] = (isAgent, isChange) match {
            case (true, true) =>
              TestIncomeSourceEndDateController.submit(id, incomeSourceType, isAgent, isChange)(fakeRequestConfirmedClient().withDateInFormEncoding)
            case (true, false) =>
              TestIncomeSourceEndDateController.submit(id, incomeSourceType, isAgent, isChange)(fakeRequestConfirmedClient().withDateInFormEncoding)
            case (false, true) =>
              TestIncomeSourceEndDateController.submit(id, incomeSourceType, isAgent, isChange)(fakeRequestWithActiveSession.withDateInFormEncoding)
            case (false, false) =>
              TestIncomeSourceEndDateController.submit(id, incomeSourceType, isAgent, isChange)(fakeRequestWithActiveSession.withDateInFormEncoding)
          }

          val redirectLocationResult = if (isAgent) controllers.manageBusinesses.cease.routes.CeaseCheckIncomeSourceDetailsController.showAgent(incomeSourceType).url
          else controllers.manageBusinesses.cease.routes.CeaseCheckIncomeSourceDetailsController.show(incomeSourceType).url

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(redirectLocationResult)

          if (incomeSourceType == SelfEmployment) verifyMockSetMultipleMongoDataResponse(1)
          else verifyMockSetMongoKeyResponse(1)
        }

        "Self Employment - form is completed successfully" when {
          val incomeSourceType = SelfEmployment
          val id = Some(mkIncomeSourceId(testSelfEmploymentId).toHash.hash)
          "called .submit" when {
            "user is an Individual" in {
              testSubmitResponse(id = id, incomeSourceType, isAgent = false, isChange = false)
            }
            "user is an Agent" in {
              testSubmitResponse(id = id, incomeSourceType, isAgent = true, isChange = false)
            }
            "user is an Individual change" in {
              testSubmitResponse(id = id, incomeSourceType, isAgent = false, isChange = true)
            }
            "user is an Agent change" in {
              testSubmitResponse(id = id, incomeSourceType, isAgent = true, isChange = true)
            }
          }

        }
        "UK Property - form is completed successfully" when {
          val incomeSourceType = UkProperty
          val id = None
          "called .submit" when {
            "user is an Individual" in {
              testSubmitResponse(id = id, incomeSourceType, isAgent = false, isChange = false)
            }
            "user is an Agent" in {
              testSubmitResponse(id = id, incomeSourceType, isAgent = true, isChange = false)
            }
            "user is an Individual change" in {
              testSubmitResponse(id = id, incomeSourceType, isAgent = false, isChange = true)
            }
            "user is an Agent change" in {
              testSubmitResponse(id = id, incomeSourceType, isAgent = true, isChange = true)
            }
          }
        }
        "Foreign Property - form is completed successfully" when {
          val incomeSourceType = ForeignProperty
          val id = None
          "called .submit" when {
            "user is an Individual" in {
              testSubmitResponse(id = id, incomeSourceType, isAgent = false, isChange = false)
            }
            "user is an Agent" in {
              testSubmitResponse(id = id, incomeSourceType, isAgent = true, isChange = false)
            }
            "user is an Individual change" in {
              testSubmitResponse(id = id, incomeSourceType, isAgent = false, isChange = true)
            }
            "user is an Agent change" in {
              testSubmitResponse(id = id, incomeSourceType, isAgent = true, isChange = true)
            }
          }
        }
      }
      "return 400 BAD_REQUEST" when {
        def testFormError(isAgent: Boolean, isChange: Boolean): Unit = {
          implicit class FormEncoding(request: FakeRequest[AnyContentAsEmpty.type]) {
            def withIncorrectDateInFormEncoding: FakeRequest[AnyContentAsFormUrlEncoded] = request.withMethod("POST")
              .withFormUrlEncodedBody("value.day" -> "", "value.month" -> "8",
                "value.year" -> "2022")
          }
          val id = Some(mkIncomeSourceId(testSelfEmploymentId).toHash.hash)
          val incomeSourceType = SelfEmployment
          if (isAgent) setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          else setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
          enable(IncomeSources)
          mockBusinessIncomeSource()

          val result: Future[Result] = (isAgent, isChange) match {
            case (true, true) =>
              TestIncomeSourceEndDateController.submit(id, incomeSourceType, isAgent, isChange)(fakeRequestConfirmedClient().withIncorrectDateInFormEncoding)
            case (true, false) =>
              TestIncomeSourceEndDateController.submit(id, incomeSourceType, isAgent, isChange)(fakeRequestConfirmedClient().withIncorrectDateInFormEncoding)
            case (false, true) =>
              TestIncomeSourceEndDateController.submit(id, incomeSourceType, isAgent, isChange)(fakeRequestWithActiveSession.withIncorrectDateInFormEncoding)
            case (false, false) =>
              TestIncomeSourceEndDateController.submit(id, incomeSourceType, isAgent, isChange)(fakeRequestWithActiveSession.withIncorrectDateInFormEncoding)
          }

          status(result) shouldBe BAD_REQUEST

          val document: Document = Jsoup.parse(contentAsString(result))
          document.title shouldBe getValidationErrorTabTitle(incomeSourceType)
          document.getElementsByClass("govuk-error-message").text() shouldBe "Error:: The date must include a day"
        }

        "the form is not completed successfully" when {
          "called .submit" when {
            "user is an Individual" in {
              testFormError(isAgent = false, isChange = false)
            }
            "user is an Agent" in {
              testFormError(isAgent = true, isChange = false)
            }
            "user is an Individual change" in {
              testFormError(isAgent = false, isChange = true)
            }
            "user is an Agent change" in {
              testFormError(isAgent = true, isChange = true)
            }
          }
        }
      }
      "return 500 INTERNAL SERVER ERROR to internal server page" when {
        def testInternalServerErrors(isAgent: Boolean, isChange: Boolean, id: Option[String] = None, incomeSourceType: IncomeSourceType = SelfEmployment): Unit = {
          implicit class FormEncoding(request: FakeRequest[AnyContentAsEmpty.type]) {
            def withDateInFormEncoding: FakeRequest[AnyContentAsFormUrlEncoded] = request.withMethod("POST")
              .withFormUrlEncodedBody("value.day" -> "27", "value.month" -> "8",
                "value.year" -> "2022")
          }

          setupMockAuthorisationSuccess(isAgent)
          disableAllSwitches()
          enable(IncomeSources)
          mockBothPropertyBothBusiness()


          val result: Future[Result] = (isAgent, isChange) match {
            case (true, true) =>
              TestIncomeSourceEndDateController.submit(id, incomeSourceType, isAgent, isChange)(fakeRequestConfirmedClient().withDateInFormEncoding)
            case (true, false) =>
              TestIncomeSourceEndDateController.submit(id, incomeSourceType, isAgent, isChange)(fakeRequestConfirmedClient().withDateInFormEncoding)
            case (false, true) =>
              TestIncomeSourceEndDateController.submit(id, incomeSourceType, isAgent, isChange)(fakeRequestWithActiveSession.withDateInFormEncoding)
            case (false, false) =>
              TestIncomeSourceEndDateController.submit(id, incomeSourceType, isAgent, isChange)(fakeRequestWithActiveSession.withDateInFormEncoding)
          }

          status(result) shouldBe INTERNAL_SERVER_ERROR
        }

        "income source ID is missing and income source type is Self Employment " when {
          "called .submit" when {
            "user is an Individual" in {
              testInternalServerErrors(isAgent = false, isChange = false)
            }
            "user is an Agent" in {
              testInternalServerErrors(isAgent = true, isChange = false)
            }
            "user is an Individual change" in {
              testInternalServerErrors(isAgent = false, isChange = true)
            }
            "user is an Agent change" in {
              testInternalServerErrors(isAgent = true, isChange = true)
            }
          }
        }
        s"SelfEmployment - unable to set session data ${CeaseIncomeSourceData.dateCeasedField}" when {
          "called .submit" in {
            setupMockCreateSession(true)
            setupMockSetSessionKeyMongo(CeaseIncomeSourceData.dateCeasedField)(Left(new Exception()))
            testInternalServerErrors(isAgent = false, isChange = false, id = Some(testSelfEmploymentId))
          }
        }
        s"SelfEmployment - unable to set session data ${CeaseIncomeSourceData.incomeSourceIdField}" when {
          "called .submit" in {
            setupMockCreateSession(true)
            setupMockSetSessionKeyMongo(CeaseIncomeSourceData.dateCeasedField)(Right(true))
            setupMockSetSessionKeyMongo(CeaseIncomeSourceData.incomeSourceIdField)(Left(new Exception()))
            testInternalServerErrors(isAgent = false, isChange = false, id = Some(testSelfEmploymentId))
          }
        }
        s"Property - unable to set session data ${CeaseIncomeSourceData.dateCeasedField}" when {
          "called .submit" in {
            setupMockCreateSession(true)
            setupMockSetSessionKeyMongo(CeaseIncomeSourceData.dateCeasedField)(Left(new Exception()))
            testInternalServerErrors(isAgent = false, isChange = false, id = None, incomeSourceType = ForeignProperty)
          }
        }
      }
    }
  }

