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

import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import enums.{MTDIndividual, MTDUserRole}
import mocks.auth.MockAuthActions
import mocks.services.MockSessionService
import models.admin.IncomeSourcesFs
import models.createIncomeSource.CreateIncomeSourceResponse
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api
import play.api.Application
import play.api.http.Status
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.{CreateBusinessDetailsService, SessionService}
import testConstants.BaseTestConstants.testSelfEmploymentId
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{emptyUIJourneySessionData, notCompletedUIJourneySessionData}

import scala.concurrent.Future

class IncomeSourceCheckDetailsControllerSpec extends MockAuthActions with MockSessionService {
  import testConstants.IncomeSourceCheckDetailsConstants._

  lazy val mockBusinessDetailsService: CreateBusinessDetailsService = mock(classOf[CreateBusinessDetailsService])

  val accruals: String = messages("incomeSources.add.accountingMethod.accruals")

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[SessionService].toInstance(mockSessionService),
      api.inject.bind[CreateBusinessDetailsService].toInstance(mockBusinessDetailsService)
    ).build()

  lazy val testCheckDetailsController = app.injector.instanceOf[IncomeSourceCheckDetailsController]

  def getHeading(sourceType: IncomeSourceType): String = {
    sourceType match {
      case SelfEmployment => messages("check-business-details.title")
      case UkProperty => messages("incomeSources.add.checkUKPropertyDetails.title")
      case ForeignProperty => messages("incomeSources.add.foreign-property-check-details.title")
    }
  }

  def getTitle(sourceType: IncomeSourceType, mdtUserRole: MTDUserRole): String = {
    val prefix: String = if (mdtUserRole != MTDIndividual) "htmlTitle.agent" else "htmlTitle"
    sourceType match {
      case SelfEmployment => s"${messages(prefix, messages("check-business-details.title"))}"
      case UkProperty => messages(prefix, messages("incomeSources.add.checkUKPropertyDetails.title"))
      case ForeignProperty => messages(prefix, messages("incomeSources.add.foreign-property-check-details.title"))
    }
  }

  def getLink(sourceType: IncomeSourceType): String = {
    sourceType match {
      case SelfEmployment => s"${messages("check-business-details.change")}"
      case UkProperty => s"${messages("check-business-details.change")}"
      case ForeignProperty => s"${messages("incomeSources.add.foreign-property-check-details.change")}"
    }
  }

  List(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSourceType =>
    mtdAllRoles.foreach { mtdRole =>
      s"show${if (mtdRole != MTDIndividual) "Agent"}(incomeSourceType = $incomeSourceType)" when {
        val action = if (mtdRole == MTDIndividual) testCheckDetailsController.show(incomeSourceType) else testCheckDetailsController.showAgent(incomeSourceType)
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
        s"the user is authenticated as a $mtdRole" should {
          "render the check details page" when {
            "the session contains full business details and FS enabled" in {
              enable(IncomeSourcesFs)
              setupMockSuccess(mtdRole)

              mockNoIncomeSources()
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Add, incomeSourceType)))))

              val result: Future[Result] = action(fakeRequest)

              val document: Document = Jsoup.parse(contentAsString(result))
              val changeDetailsLinks = document.select(".govuk-summary-list__actions .govuk-link")

              status(result) shouldBe OK
              document.title shouldBe getTitle(incomeSourceType, mtdRole)
              document.select("h1:nth-child(1)").text shouldBe getHeading(incomeSourceType)
              changeDetailsLinks.first().text shouldBe getLink(incomeSourceType)
            }
          }

          "return 303 and redirect an individual back to the home page" when {
            "the IncomeSources FS is disabled" in {
              disable(IncomeSourcesFs)
              setupMockSuccess(mtdRole)
              mockSingleBusinessIncomeSource()
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Add, incomeSourceType)))))

              val result: Future[Result] = action(fakeRequest)


              val redirectUrl = if (mtdRole == MTDIndividual) controllers.routes.HomeController.show().url
              else controllers.routes.HomeController.showAgent.url

              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some(redirectUrl)
            }
          }

          s"return ${Status.SEE_OTHER}: redirect to ReportingMethodSetBackErrorController" when {
            s"user has already completed the journey" in {
              enable(IncomeSourcesFs)
              mockNoIncomeSources()
              setupMockSuccess(mtdRole)
              setupMockGetMongo(Right(Some(sessionDataCompletedJourney(IncomeSourceJourneyType(Add, incomeSourceType)))))

              val result: Future[Result] = action(fakeRequest)
              status(result) shouldBe SEE_OTHER
              val redirectUrl = if (mtdRole == MTDIndividual) controllers.incomeSources.add.routes.ReportingMethodSetBackErrorController.show(incomeSourceType).url
              else controllers.incomeSources.add.routes.ReportingMethodSetBackErrorController.showAgent(incomeSourceType).url
              redirectLocation(result) shouldBe Some(redirectUrl)
            }
          }

          s"return ${Status.SEE_OTHER}: redirect to IncomeSourceAddedBackErrorController" when {
            s"user has already added their income source" in {
              enable(IncomeSourcesFs)
              mockNoIncomeSources()
              setupMockSuccess(mtdRole)
              setupMockGetMongo(Right(Some(sessionDataISAdded(IncomeSourceJourneyType(Add, incomeSourceType)))))

              val result: Future[Result] = action(fakeRequest)
              status(result) shouldBe SEE_OTHER
              val redirectUrl = if (mtdRole == MTDIndividual) controllers.incomeSources.add.routes.IncomeSourceAddedBackErrorController.show(incomeSourceType).url
              else controllers.incomeSources.add.routes.IncomeSourceAddedBackErrorController.showAgent(incomeSourceType).url
              redirectLocation(result) shouldBe Some(redirectUrl)
            }
          }

          "return 500 INTERNAL_SERVER_ERROR" when {
            "there is session data missing" in {
              enable(IncomeSourcesFs)

              mockNoIncomeSources()
              setupMockSuccess(mtdRole)
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Add, incomeSourceType)))))

              val result: Future[Result] = action(fakeRequest)

              status(result) shouldBe INTERNAL_SERVER_ERROR
            }
          }
        }
      }

      s"submit${if (mtdRole != MTDIndividual) "Agent"}(incomeSourceType = $incomeSourceType)" when {
        val action = if (mtdRole == MTDIndividual) testCheckDetailsController.submit(incomeSourceType) else testCheckDetailsController.submitAgent(incomeSourceType)
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole).withMethod("POST")
        s"the user is authenticated as a $mtdRole" should {
          "redirect to IncomeSourceReportingFrequencyController" when {
            "data is correct and redirect next page" in {
              setupMockSuccess(mtdRole)
              enable(IncomeSourcesFs)

              mockNoIncomeSources()
              when(mockBusinessDetailsService.createRequest(any())(any(), any(), any()))
                .thenReturn(Future {
                  Right(CreateIncomeSourceResponse(testBusinessId))
                })
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Add, incomeSourceType)))))
              setupMockSetMongoData(true)
              when(mockSessionService.deleteMongoData(any())(any())).thenReturn(Future(true))

              val result: Future[Result] = action(fakeRequest)

              val redirectUrl: (Boolean, IncomeSourceType, String) => String = (isAgent: Boolean, incomeSourceType: IncomeSourceType, id: String) =>
                routes.IncomeSourceReportingMethodController.show(isAgent, incomeSourceType).url

              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some(redirectUrl(mtdRole != MTDIndividual, incomeSourceType, testSelfEmploymentId))
            }
          }

          "redirect to custom error page" when {
            "unable to create business" in {
              enable(IncomeSourcesFs)
              mockNoIncomeSources()
              setupMockSuccess(mtdRole)
              when(mockBusinessDetailsService.createRequest(any())(any(), any(), any()))
                .thenReturn(Future {
                  Left(new Error("Test Error"))
                })
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Add, incomeSourceType)))))
              when(mockSessionService.deleteMongoData(any())(any())).thenReturn(Future(true))

              val result: Future[Result] = action(fakeRequest)

              val redirectUrl = if (mtdRole != MTDIndividual) controllers.incomeSources.add.routes.IncomeSourceNotAddedController.showAgent(incomeSourceType).url
              else controllers.incomeSources.add.routes.IncomeSourceNotAddedController.show(incomeSourceType).url

              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some(redirectUrl)
            }
          }
        }
      }
    }
  }
}
