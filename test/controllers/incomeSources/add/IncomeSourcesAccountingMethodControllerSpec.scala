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
import enums.JourneyType.{Add, IncomeSourceJourneyType, JourneyType}
import enums.{AccrualsAsAccountingMethod, CashAsAccountingMethod, MTDIndividual}
import mocks.auth.MockAuthActions
import mocks.services.MockSessionService
import models.admin.IncomeSourcesFs
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Mockito.reset
import play.api
import play.api.Application
import play.api.http.Status
import play.api.http.Status.SEE_OTHER
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.{DateService, SessionService}
import testConstants.BaseTestConstants.testSessionId
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.notCompletedUIJourneySessionData

import scala.concurrent.Future

class IncomeSourcesAccountingMethodControllerSpec extends MockAuthActions with MockSessionService {

  override def fakeApplication(): Application = applicationBuilderWithAuthBindings()
    .overrides(
      api.inject.bind[SessionService].toInstance(mockSessionService),
      api.inject.bind[DateService].toInstance(dateService)
    ).build()

  val testController = fakeApplication().injector.instanceOf[IncomeSourcesAccountingMethodController]

  def businessResponseRoute(incomeSourceType: IncomeSourceType): String = {
    "incomeSources.add." + incomeSourceType.key + ".AccountingMethod"
  }

  def sessionDataCompletedJourney(journeyType: JourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData(journeyIsComplete = Some(true))))

  def sessionDataISAdded(journeyType: JourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData(incomeSourceAdded = Some(true))))

  def sessionData(journeyType: JourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData()))

  def sessionDataWithAccMethodCash(journeyType: JourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData(incomeSourcesAccountingMethod = Some("cash"))))

  def sessionDataWithAccMethodAccruals(journeyType: JourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData(incomeSourcesAccountingMethod = Some("accruals"))))

  def getHeading(incomeSourceType: IncomeSourceType): String = {
    messages("incomeSources.add." + incomeSourceType.key + ".AccountingMethod.heading")
  }

  def getTitle(incomeSourceType: IncomeSourceType, isAgent: Boolean = false): String = {
    if (isAgent)
      s"${messages("htmlTitle.agent", getHeading(incomeSourceType))}"
    else
      s"${messages("htmlTitle", getHeading(incomeSourceType))}"
  }

  def getInvalidInputTitle(incomeSourceType: IncomeSourceType): String = {
    s"${messages("htmlTitle.invalidInput", getHeading(incomeSourceType))}"
  }

  def getRedirectUrl(incomeSourceType: IncomeSourceType, isAgent: Boolean = false): String = {
    if (isAgent)
      controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController.showAgent(incomeSourceType).url
    else
      controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController.show(incomeSourceType).url
  }

  val incomeSourceTypes: Seq[IncomeSourceType with Serializable] = List(SelfEmployment, UkProperty, ForeignProperty)

  mtdAllRoles.foreach { mtdRole =>
    val isAgent = mtdRole != MTDIndividual
    incomeSourceTypes.foreach { incomeSourceType =>
      s"show($incomeSourceType, $isAgent)" when {
        val action = testController.show(incomeSourceType, isAgent)
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
        s"the user is authenticated as a $mtdRole" should {
          "return 200 OK" when {
            "navigating to the page with FS Enabled and no active " + incomeSourceType + " businesses" in {
              enable(IncomeSourcesFs)
              setupMockSuccess(mtdRole)
              mockNoIncomeSources()
              val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)
              setupMockGetMongo(Right(Some(sessionData(journeyType))))

              val result: Future[Result] = action(fakeRequest)
              val document: Document = Jsoup.parse(contentAsString(result))

              status(result) shouldBe Status.OK
              document.title shouldBe getTitle(incomeSourceType, isAgent)
              document.select("legend").text shouldBe getHeading(incomeSourceType)
            }
          }
          if (incomeSourceType == SelfEmployment) {
            "return 303 SEE_OTHER" when {
              "navigating to the page with FS Enabled and one  " + incomeSourceType + "  businesses, with the cashOrAccruals field set to the string accruals" in {
                enable(IncomeSourcesFs)
                setupMockSuccess(mtdRole)
                mockBusinessIncomeSourceWithAccruals()
                reset(mockSessionService)
                setupMockSetMongoData(true)
                val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)
                setupMockGetMongo(Right(Some(sessionDataWithAccMethodAccruals(journeyType))))

                val result: Future[Result] = action(fakeRequest)

                status(result) shouldBe Status.SEE_OTHER
                redirectLocation(result) shouldBe Some(getRedirectUrl(incomeSourceType, isAgent))
              }
              "navigating to the page with FS Enabled and one  " + incomeSourceType + "  businesses, with the cashOrAccruals field set to the string cash" in {
                enable(IncomeSourcesFs)
                setupMockSuccess(mtdRole)
                mockBusinessIncomeSource()
                setupMockSetMongoData(true)
                val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)
                setupMockGetMongo(Right(Some(sessionDataWithAccMethodCash(journeyType))))

                val result: Future[Result] = action(fakeRequest)

                status(result) shouldBe Status.SEE_OTHER
                redirectLocation(result) shouldBe Some(getRedirectUrl(incomeSourceType, isAgent))
              }
              "navigating to the page with FS Enabled and two SE businesses, one cash, one accruals (should be impossible, but in this case, we use head of list) for " + incomeSourceType in {
                enable(IncomeSourcesFs)
                setupMockSuccess(mtdRole)
                mockBusinessIncomeSourceWithCashAndAccruals()
                setupMockSetMongoData(true)
                val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)
                setupMockGetMongo(Right(Some(sessionData(journeyType))))

                val result: Future[Result] = action(fakeRequest)

                status(result) shouldBe Status.SEE_OTHER
                redirectLocation(result) shouldBe Some(getRedirectUrl(incomeSourceType, isAgent))
              }
            }
          }
          "return 303 SEE_OTHER and redirect to custom not found error page for " + incomeSourceType when {
            "navigating to the page with FS Disabled" in {
              disable(IncomeSourcesFs)
              setupMockSuccess(mtdRole)
              mockBusinessIncomeSource()
              setupMockSetMongoData(true)
              val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)
              setupMockGetMongo(Right(Some(sessionData(journeyType))))

              val result: Future[Result] = action(fakeRequest)

              status(result) shouldBe Status.SEE_OTHER
            }
          }
          s"return ${Status.SEE_OTHER}: redirect to the relevant You Cannot Go Back page" + incomeSourceType when {
            s"user has already completed the journey" in {
              enable(IncomeSourcesFs)
              setupMockSuccess(mtdRole)

              mockBusinessIncomeSource()
              setupMockSetMongoData(true)
              setupMockGetMongo(Right(Some(sessionDataCompletedJourney(IncomeSourceJourneyType(Add, incomeSourceType)))))

              val result: Future[Result] = action(fakeRequest)
              status(result) shouldBe SEE_OTHER
              val expectedUrl = if (isAgent) controllers.incomeSources.add.routes.ReportingMethodSetBackErrorController.showAgent(incomeSourceType)
              else controllers.incomeSources.add.routes.ReportingMethodSetBackErrorController.show(incomeSourceType)
              redirectLocation(result) shouldBe Some(expectedUrl.url)
            }
            s"user has already added their income source" in {
              enable(IncomeSourcesFs)
              setupMockSuccess(mtdRole)

              mockBusinessIncomeSource()
              setupMockSetMongoData(true)
              setupMockGetMongo(Right(Some(sessionDataISAdded(IncomeSourceJourneyType(Add, incomeSourceType)))))

              val result: Future[Result] = action(fakeRequest)
              status(result) shouldBe SEE_OTHER
              val expectedUrl = if (isAgent) controllers.incomeSources.add.routes.IncomeSourceAddedBackErrorController.showAgent(incomeSourceType)
              else controllers.incomeSources.add.routes.IncomeSourceAddedBackErrorController.show(incomeSourceType)
              redirectLocation(result) shouldBe Some(expectedUrl.url)
            }
          }
        }
        testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
      }

      s"changeIncomeSourcesAccountingMethod($incomeSourceType, $isAgent)" when {
        val action = testController.changeIncomeSourcesAccountingMethod(incomeSourceType, isAgent)
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
        s"the user is authenticated as a $mtdRole" should {
          "return 200 OK for change accounting method for isAgent = " + isAgent + "" when {
            "navigating to the page by change link with FS Enabled and no active " + incomeSourceType + " businesses" in {
              val cashOrAccrualsFlag: Option[String] = {
                incomeSourceType match {
                  case UkProperty => Some(AccrualsAsAccountingMethod)
                  case _ => Some(CashAsAccountingMethod)
                }
              }
              enable(IncomeSourcesFs)
              setupMockSuccess(mtdRole)
              mockNoIncomeSources()

              val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)
              setupMockGetMongo(Right(Some(UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData(incomeSourcesAccountingMethod = cashOrAccrualsFlag))))))
              setupMockSetMongoData(true)

              val result: Future[Result] = action(fakeRequest)
              val document: Document = Jsoup.parse(contentAsString(result))

              status(result) shouldBe Status.OK
              document.title shouldBe getTitle(incomeSourceType, isAgent)
              document.select("input").select("[checked]").`val`() shouldBe (if (cashOrAccrualsFlag.getOrElse("") == "cash") "cash" else "traditional")
              document.select("legend").text shouldBe getHeading(incomeSourceType)
            }
          }
        }
        testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
      }

      s"submit($incomeSourceType, $isAgent)" when {
        val action = testController.submit(incomeSourceType, isAgent)
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole).withMethod("POST")
        s"the user is authenticated as a $mtdRole" should {
          s"return 303 SEE_OTHER and redirect to ${getRedirectUrl(incomeSourceType, isAgent)}" when {
            "form is completed successfully with cash radio button selected for " + incomeSourceType in {
              val accountingMethod: String = CashAsAccountingMethod
              enable(IncomeSourcesFs)
              setupMockSuccess(mtdRole)
              mockNoIncomeSources()

              setupMockSetMongoData(true)
              setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Add, incomeSourceType)))))

              lazy val result: Future[Result] = action(fakeRequest.withFormUrlEncodedBody(businessResponseRoute(incomeSourceType) -> accountingMethod))

              status(result) shouldBe Status.SEE_OTHER
              redirectLocation(result) shouldBe Some(getRedirectUrl(incomeSourceType, isAgent))
            }
            "form is completed successfully with traditional radio button selected for " + incomeSourceType in {
              enable(IncomeSourcesFs)
              setupMockSuccess(mtdRole)
              mockNoIncomeSources()

              setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Add, incomeSourceType)))))
              setupMockSetMongoData(true)

              lazy val result: Future[Result] = action(fakeRequest.withFormUrlEncodedBody(businessResponseRoute(incomeSourceType) -> "traditional"))

              status(result) shouldBe Status.SEE_OTHER
              redirectLocation(result) shouldBe Some(getRedirectUrl(incomeSourceType, isAgent))
            }
          }
          "return 400 BAD_REQUEST" when {
            "the form is not completed successfully for " + incomeSourceType in {
              enable(IncomeSourcesFs)
              setupMockSuccess(mtdRole)
              mockNoIncomeSources()

              setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Add, incomeSourceType)))))
              setupMockSetMongoData(true)

              lazy val result: Future[Result] = action(fakeRequest.withFormUrlEncodedBody(businessResponseRoute(incomeSourceType) -> ""))
              val document: Document = Jsoup.parse(contentAsString(result))

              status(result) shouldBe Status.BAD_REQUEST
              document.title shouldBe getInvalidInputTitle(incomeSourceType)
              result.futureValue.session.get(AddIncomeSourceData.incomeSourcesAccountingMethodField) shouldBe None
            }
          }
        }
        testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
      }
    }
  }
}
