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

package controllers.manageBusinesses.manage

import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{IncomeSourceJourneyType, Manage}
import enums.{MTDIndividual, MTDUserRole}
import implicits.ImplicitDateFormatter
import mocks.auth.MockAuthActions
import mocks.services.MockSessionService
import models.admin.IncomeSourcesNewJourney
import play.api
import play.api.Application
import play.api.http.Status
import play.api.http.Status.SEE_OTHER
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.{DateService, SessionService}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{completedUIJourneySessionData, emptyUIJourneySessionData, notCompletedUIJourneySessionData}

import scala.concurrent.Future

class ConfirmReportingMethodSharedControllerSpec extends MockAuthActions
  with ImplicitDateFormatter
  with MockSessionService {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[SessionService].toInstance(mockSessionService),
      api.inject.bind[DateService].toInstance(dateService)
    ).build()

  lazy val testConfirmReportingMethodSharedController: ConfirmReportingMethodSharedController = app.injector.instanceOf[ConfirmReportingMethodSharedController]

  val individual: Boolean = true
  val agent: Boolean = false


  trait SetupGET {
    val taxYear: String = "2022-2023"
    val changeTo: String = "annual"
    val incomeSourceType: IncomeSourceType
    val mtdRole: MTDUserRole

    lazy val action: Action[AnyContent] = if (mtdRole == MTDIndividual) {
      testConfirmReportingMethodSharedController.show(taxYear, changeTo, incomeSourceType)
    } else {
      testConfirmReportingMethodSharedController.showAgent(taxYear, changeTo, incomeSourceType)
    }
  }

  Seq(SelfEmployment, UkProperty, ForeignProperty).foreach { testIncomeSourceType =>
    mtdAllRoles.foreach { testMtdRole =>
      s"show${if (testMtdRole != MTDIndividual) "Agent"}($testIncomeSourceType)" when {
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(testMtdRole).withMethod("GET")
        s"the user is authenticated as s$testMtdRole" should {
          s"return ${Status.OK}" when {
            "all query parameters are valid" in new SetupGET {
              override val incomeSourceType: IncomeSourceType = testIncomeSourceType
              override val mtdRole: MTDUserRole = testMtdRole

              setupMockSuccess(mtdRole)
              enable(IncomeSourcesNewJourney)
              mockBothPropertyBothBusinessWithLatency()

              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Manage, incomeSourceType)))))

              setupMockSetMongoData(true)

              val result: Future[Result] = action(fakeRequest)
              status(result) shouldBe Status.OK
            }
          }
          "redirect to the Cannot Go Back page" when {
            "Journey is complete" in new SetupGET {
              override val incomeSourceType: IncomeSourceType = testIncomeSourceType
              override val mtdRole: MTDUserRole = testMtdRole
              setupMockSuccess(mtdRole)
              enable(IncomeSourcesNewJourney)
              mockBothPropertyBothBusiness()
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(completedUIJourneySessionData(IncomeSourceJourneyType(Manage, incomeSourceType)))))
              val result: Future[Result] = action(fakeRequest)


              val expectedRedirectUrl: String = routes.CannotGoBackErrorController.show(isAgent = mtdRole != MTDIndividual, incomeSourceType).url

              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some(expectedRedirectUrl)
            }
          }
          s"return ${Status.SEE_OTHER} and redirect to the home page" when {
            "The IncomeSourcesNewJourney FS is disabled" in new SetupGET {
              override val incomeSourceType: IncomeSourceType = testIncomeSourceType
              override val mtdRole: MTDUserRole = testMtdRole
              disable(IncomeSourcesNewJourney)
              setupMockSuccess(mtdRole)
              mockBothPropertyBothBusiness()
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(completedUIJourneySessionData(IncomeSourceJourneyType(Manage, incomeSourceType)))))
              val result: Future[Result] = action(fakeRequest)
              val expectedEndpoint: String = if (mtdRole == MTDIndividual) {
                controllers.routes.HomeController.show().url
              } else {
                controllers.routes.HomeController.showAgent().url
              }

              status(result) shouldBe Status.SEE_OTHER

              redirectLocation(result) shouldBe Some(expectedEndpoint)
            }
          }
          s"return ${Status.INTERNAL_SERVER_ERROR}" when {
            "taxYear parameter has an invalid format " in new SetupGET {
              override val incomeSourceType: IncomeSourceType = testIncomeSourceType
              override val mtdRole: MTDUserRole = testMtdRole
              override val taxYear: String = invalidTaxYear

              setupMockSuccess(mtdRole)
              enable(IncomeSourcesNewJourney)
              mockBothPropertyBothBusinessWithLatency()

              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Manage, incomeSourceType)))))

              setupMockSetMongoData(true)

              val result: Future[Result] = action(fakeRequest)
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            }
            "taxYear parameter doesn't match latency details " in new SetupGET {
              override val incomeSourceType: IncomeSourceType = testIncomeSourceType
              override val mtdRole: MTDUserRole = testMtdRole
              override val taxYear: String = invalidTaxYearLatency

              setupMockSuccess(mtdRole)
              enable(IncomeSourcesNewJourney)
              mockBothPropertyBothBusinessWithLatency()

              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Manage, incomeSourceType)))))

              setupMockSetMongoData(true)

              val result: Future[Result] = action(fakeRequest)
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            }
            "changeTo parameter has an invalid format " in new SetupGET {
              override val incomeSourceType: IncomeSourceType = testIncomeSourceType
              override val mtdRole: MTDUserRole = testMtdRole
              override val changeTo: String = invalidChangeTo

              setupMockSuccess(mtdRole)
              enable(IncomeSourcesNewJourney)
              mockBothPropertyBothBusinessWithLatency()

              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Manage, incomeSourceType)))))

              setupMockSetMongoData(true)

              val result: Future[Result] = action(fakeRequest)
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            }
            if (testIncomeSourceType == SelfEmployment) {
              "the given incomeSourceId cannot be found in the user's Sole Trader business income sources" in new SetupGET {
                override val incomeSourceType: IncomeSourceType = testIncomeSourceType
                override val mtdRole: MTDUserRole = testMtdRole

                setupMockSuccess(mtdRole)
                enable(IncomeSourcesNewJourney)
                mockBothPropertyBothBusinessWithLatency()

                setupMockCreateSession(true)
                setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, incomeSourceType)))))

                val result: Future[Result] = action(fakeRequest)
                status(result) shouldBe Status.INTERNAL_SERVER_ERROR
              }
            }
          }
        }
      }
    }
  }


  trait SetupPOST {
    val taxYear: String = "2022-2023"
    val changeTo: String = "annual"
    val incomeSourceType: IncomeSourceType
    val mtdRole: MTDUserRole

    lazy val action: Action[AnyContent] = if (mtdRole == MTDIndividual) {
      testConfirmReportingMethodSharedController.submit(taxYear, changeTo, incomeSourceType)
    } else {
      testConfirmReportingMethodSharedController.submitAgent(taxYear, changeTo, incomeSourceType)
    }
  }

  Seq(SelfEmployment, UkProperty, ForeignProperty).foreach { testIncomeSourceType =>
    mtdAllRoles.foreach { testMtdRole =>
      s"submit${if (testMtdRole != MTDIndividual) "Agent"}($testIncomeSourceType)" when {
        val fakeRequest = fakePostRequestBasedOnMTDUserType(testMtdRole).withMethod("POST")
        s"the user is authenticated as s$testMtdRole" should {
          s"return ${Status.SEE_OTHER} and redirect to the Manage Obligations page for a property" when {
            "the user's property reporting method is updated to annual" in new SetupPOST {
              override val incomeSourceType: IncomeSourceType = testIncomeSourceType
              override val mtdRole: MTDUserRole = testMtdRole

              setupMockSuccess(mtdRole)
              enable(IncomeSourcesNewJourney)
              mockBothPropertyBothBusiness()
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Manage, incomeSourceType)))))
              setupMockSetMongoData(true)
              val result: Future[Result] = action(fakeRequest)
              val expectedEndpoint: String = if (mtdRole == MTDIndividual) {
                controllers.manageBusinesses.manage.routes
                  .CheckYourAnswersController.show(isAgent = false, incomeSourceType).url
              } else {
                controllers.manageBusinesses.manage.routes
                  .CheckYourAnswersController.show(isAgent = true, incomeSourceType).url
              }

              status(result) shouldBe Status.SEE_OTHER
              redirectLocation(result) shouldBe
                Some(expectedEndpoint)
            }
            "the user's property reporting method is updated to quarterly" in new SetupPOST {
              override val incomeSourceType: IncomeSourceType = testIncomeSourceType
              override val mtdRole: MTDUserRole = testMtdRole
              override val changeTo: String = "quarterly"
              setupMockSuccess(mtdRole)
              enable(IncomeSourcesNewJourney)
              mockBothPropertyBothBusiness()
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Manage, incomeSourceType)))))
              setupMockSetMongoData(true)
              val result: Future[Result] = action(fakeRequest)
              val expectedEndpoint: String = if (mtdRole == MTDIndividual) {
                controllers.manageBusinesses.manage.routes
                  .CheckYourAnswersController.show(isAgent = false, incomeSourceType).url
              } else {
                controllers.manageBusinesses.manage.routes
                  .CheckYourAnswersController.show(isAgent = true, incomeSourceType).url
              }

              status(result) shouldBe Status.SEE_OTHER
              redirectLocation(result) shouldBe
                Some(expectedEndpoint)
            }

          }
          s"return ${Status.SEE_OTHER} and redirect to the home page" when {
            "the IncomeSourcesNewJourney FS is disabled" in new SetupPOST {
              override val incomeSourceType: IncomeSourceType = testIncomeSourceType
              override val mtdRole: MTDUserRole = testMtdRole

              disable(IncomeSourcesNewJourney)

              setupMockSuccess(mtdRole)
              mockBothPropertyBothBusiness()
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(completedUIJourneySessionData(IncomeSourceJourneyType(Manage, incomeSourceType)))))
              setupMockSetMongoData(true)
              val result: Future[Result] = action(fakeRequest)
              val expectedEndpoint: String = if (mtdRole == MTDIndividual) {
                controllers.routes.HomeController.show().url
              } else {
                controllers.routes.HomeController.showAgent().url
              }

              status(result) shouldBe Status.SEE_OTHER

              redirectLocation(result) shouldBe Some(expectedEndpoint)
            }
          }
          s"return ${Status.INTERNAL_SERVER_ERROR} " when {
            "The taxYear parameter has an invalid Tax Year format" in new SetupPOST {
              override val incomeSourceType: IncomeSourceType = testIncomeSourceType
              override val mtdRole: MTDUserRole = testMtdRole
              override val taxYear: String = invalidTaxYear
              setupMockSuccess(mtdRole)
              enable(IncomeSourcesNewJourney)
              mockBothPropertyBothBusiness()
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Manage, incomeSourceType)))))
              setupMockSetMongoData(true)
              val result: Future[Result] = action(fakeRequest)

              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            }
          }
          s"return ${Status.INTERNAL_SERVER_ERROR} " when {
            "The taxYear parameter has an invalid ChangeTo format" in new SetupPOST {
              override val incomeSourceType: IncomeSourceType = testIncomeSourceType
              override val mtdRole: MTDUserRole = testMtdRole
              override val changeTo: String = invalidChangeTo
              setupMockSuccess(mtdRole)
              enable(IncomeSourcesNewJourney)
              mockBothPropertyBothBusiness()
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Manage, incomeSourceType)))))
              setupMockSetMongoData(true)
              val result: Future[Result] = action(fakeRequest)

              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            }
          }
        }
      }
    }
  }

  private lazy val invalidTaxYear = "$$$$-££££"
  private lazy val invalidTaxYearLatency = "2055-2056"
  private lazy val invalidChangeTo = "randomText"
}