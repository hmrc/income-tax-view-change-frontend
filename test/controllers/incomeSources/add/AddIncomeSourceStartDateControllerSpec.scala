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
import enums.MTDIndividual
import implicits.ImplicitDateFormatter
import mocks.auth.MockAuthActions
import mocks.services.MockSessionService
import models.admin.IncomeSourcesFs
import models.incomeSourceDetails.AddIncomeSourceData.dateStartedField
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api
import play.api.Application
import play.api.http.Status
import play.api.test.Helpers._
import services.SessionService
import testConstants.BaseTestConstants.testSessionId
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.notCompletedUIJourneySessionData

import java.time.LocalDate


class AddIncomeSourceStartDateControllerSpec extends MockAuthActions with MockSessionService
  with ImplicitDateFormatter {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[SessionService].toInstance(mockSessionService)
    ).build()

  lazy val testController = app.injector.instanceOf[AddIncomeSourceStartDateController]

  val dayField = "value.day"
  val monthField = "value.month"
  val yearField = "value.year"

  val testDay = "01"
  val testMonth = "01"
  val testYear = "2022"

  val testStartDate: LocalDate = LocalDate.of(2022, 1, 1)

  val currentDate = dateService.getCurrentDate

  val incomeSourceTypes: List[IncomeSourceType] = List(SelfEmployment, UkProperty, ForeignProperty)

  def sessionDataCompletedJourney(journeyType: JourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData(journeyIsComplete = Some(true))))

  def sessionDataISAdded(journeyType: JourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData(incomeSourceAdded = Some(true))))

  def sessionData(journeyType: JourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString)

  def sessionDataWithDate(journeyType: JourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData(dateStarted = Some(LocalDate.parse("2022-01-01")))))

  def getInitialMongo(sourceType: IncomeSourceType): Option[UIJourneySessionData] = sourceType match {
    case SelfEmployment => Some(sessionData(IncomeSourceJourneyType(Add, SelfEmployment)))
    case _ =>
      setupMockCreateSession(true)
      None
  }

  def getBackUrl(isAgent: Boolean, isChange: Boolean, incomeSourceType: IncomeSourceType): String = ((isAgent, isChange, incomeSourceType) match {
    case (false, false, SelfEmployment) => routes.AddBusinessNameController.show(false)
    case (_, false, SelfEmployment) => routes.AddBusinessNameController.showAgent(false)
    case (false, _, SelfEmployment) => routes.IncomeSourceCheckDetailsController.show(SelfEmployment)
    case (_, _, SelfEmployment) => routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment)
    case (false, false, _) => routes.AddIncomeSourceController.show()
    case (_, false, _) => routes.AddIncomeSourceController.showAgent()
    case (false, _, UkProperty) => routes.IncomeSourceCheckDetailsController.show(UkProperty)
    case (_, _, UkProperty) => routes.IncomeSourceCheckDetailsController.showAgent(UkProperty)
    case (false, _, _) => routes.IncomeSourceCheckDetailsController.show(ForeignProperty)
    case (_, _, _) => routes.IncomeSourceCheckDetailsController.showAgent(ForeignProperty)
  }).url

  def getRedirectUrl(incomeSourceType: IncomeSourceType, isAgent: Boolean, isChange: Boolean): String = (incomeSourceType, isAgent, isChange) match {
    case (_, _, _) => routes.AddIncomeSourceStartDateCheckController.show(isAgent, isChange, incomeSourceType).url
  }

  def getRequest(isAgent: Boolean) = {
    if (isAgent) fakeRequestConfirmedClient()
    else fakeRequestWithActiveSession
  }

  def postRequest(isAgent: Boolean) = {
    if (isAgent) fakePostRequestConfirmedClient()
    else fakePostRequestWithActiveSession
  }

  def getValidationErrorTabTitle(incomeSourceType: IncomeSourceType): String = {
    s"${messages("htmlTitle.invalidInput", messages(s"${incomeSourceType.startDateMessagesPrefix}.heading"))}"
  }

  def getHintText(incomeSourceType: IncomeSourceType) = {
    incomeSourceType match {
      case SelfEmployment => "business started trading"
      case UkProperty => "UK property business started"
      case ForeignProperty => "foreign property business started"
    }
  }

  mtdAllRoles.foreach { mtdRole =>
    val isAgent = mtdRole != MTDIndividual
    incomeSourceTypes.foreach { incomeSourceType =>
      List(true, false).foreach { isChange =>
        s"show($isAgent, $isChange, $incomeSourceType)" when {
          val action = testController.show(isAgent, isChange, incomeSourceType)
          val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
          s"the user is authenticated as a $mtdRole" should {
            s"return ${Status.OK}: render the Add ${incomeSourceType.key} start date page" when {
              "incomeSources FS is enabled" in {
                setupMockSuccess(mtdRole)
                enable(IncomeSourcesFs)
                mockNoIncomeSources()
                if(isChange) {
                  val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)
                  setupMockGetMongo(Right(Some(sessionDataWithDate(journeyType))))
                } else {
                  setupMockGetMongo(Right(getInitialMongo(incomeSourceType)))
                }
                val result = action(fakeRequest)

                val document: Document = Jsoup.parse(contentAsString(result))
                document.title should include(messages(s"${incomeSourceType.startDateMessagesPrefix}.heading"))
                val backUrl = getBackUrl(isAgent, isChange, incomeSourceType)
                document.getElementById("back-fallback").attr("href") shouldBe backUrl
                if(isChange) {
                  document.getElementById("value.day").attr("value") shouldBe "1"
                  document.getElementById("value.month").attr("value") shouldBe "1"
                  document.getElementById("value.year").attr("value") shouldBe "2022"
                }
                status(result) shouldBe OK
              }
            }
            if(isChange) {
              s"return ${Status.OK}: render the Add Business start date Change page with form filled " when {
                s"session contains key: $dateStartedField (${incomeSourceType.key})" in {
                  setupMockSuccess(mtdRole)
                  enable(IncomeSourcesFs)

                  mockNoIncomeSources()
                  setupMockCreateSession(true)
                  val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)
                  setupMockGetMongo(Right(Some(sessionDataWithDate(journeyType))))

                  val result = action(fakeRequest)

                  status(result) shouldBe OK
                  val document: Document = Jsoup.parse(contentAsString(result))
                  document.title should include(messages(s"${incomeSourceType.startDateMessagesPrefix}.heading"))
                  document.getElementById("back-fallback").attr("href") shouldBe {
                    if (isAgent) routes.IncomeSourceCheckDetailsController.showAgent(incomeSourceType).url
                    else routes.IncomeSourceCheckDetailsController.show(incomeSourceType).url
                  }
                  document.getElementById("value.day").attr("value") shouldBe "1"
                  document.getElementById("value.month").attr("value") shouldBe "1"
                  document.getElementById("value.year").attr("value") shouldBe "2022"
                }
              }
            }
            s"return ${Status.SEE_OTHER}: redirect to the relevant You Cannot Go Back page" when {
              s"user has already completed the journey (${incomeSourceType.key})" in {
                setupMockSuccess(mtdRole)
                enable(IncomeSourcesFs)

                mockNoIncomeSources()
                setupMockGetMongo(Right(Some(sessionDataCompletedJourney(IncomeSourceJourneyType(Add, incomeSourceType)))))

                val result = action(fakeRequest)
                status(result) shouldBe SEE_OTHER
                val redirectUrl = if (isAgent) controllers.incomeSources.add.routes.ReportingMethodSetBackErrorController.showAgent(incomeSourceType).url
                else controllers.incomeSources.add.routes.ReportingMethodSetBackErrorController.show(incomeSourceType).url
                redirectLocation(result) shouldBe Some(redirectUrl)
              }
              s"user has already added their income source (${incomeSourceType.key})" in {
                setupMockSuccess(mtdRole)
                enable(IncomeSourcesFs)
                mockNoIncomeSources()
                setupMockGetMongo(Right(Some(sessionDataISAdded(IncomeSourceJourneyType(Add, incomeSourceType)))))

                val result = action(fakeRequest)
                status(result) shouldBe SEE_OTHER
                val redirectUrl = if (isAgent) controllers.incomeSources.add.routes.IncomeSourceAddedBackErrorController.showAgent(incomeSourceType).url
                else controllers.incomeSources.add.routes.IncomeSourceAddedBackErrorController.show(incomeSourceType).url
                redirectLocation(result) shouldBe Some(redirectUrl)
              }
            }
            s"return ${Status.SEE_OTHER} and redirect to home page" when {
              s"incomeSources FS is disabled (${incomeSourceType.key})" in {
                disable(IncomeSourcesFs)
                setupMockSuccess(mtdRole)

                mockNoIncomeSources()
                setupMockGetMongo(Right(getInitialMongo(incomeSourceType)))

                val result = action(fakeRequest)

                status(result) shouldBe SEE_OTHER
                val redirectUrl = if (isAgent) controllers.routes.HomeController.showAgent().url else controllers.routes.HomeController.show().url
                redirectLocation(result) shouldBe Some(redirectUrl)
              }
            }
          }
          testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
        }

        s"submit($isAgent, $isChange, $incomeSourceType)" when {
          val action = testController.submit(isAgent, isChange, incomeSourceType)
          val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole).withMethod("POST")
          s"the user is authenticated as a $mtdRole" should {
            s"redirect to the Add Business Start Date Check page" when {
              "a valid form is submitted" in {
                setupMockSuccess(mtdRole)
                enable(IncomeSourcesFs)

                mockNoIncomeSources()
                setupMockCreateSession(true)
                setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Add, SelfEmployment)))))
                setupMockSetMongoData(true)

                val result = action(fakeRequest.withFormUrlEncodedBody(
                  dayField -> "12",
                  monthField -> "08",
                  yearField -> "2023"
                ))

                status(result) shouldBe SEE_OTHER
                redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.submit(incomeSourceType = incomeSourceType, isAgent = isAgent, isChange = isChange).url)
              }
            }
            s"return ${Status.BAD_REQUEST}" when {
              "an invalid form is submitted" in {
                setupMockSuccess(mtdRole)
                enable(IncomeSourcesFs)

                mockNoIncomeSources()

                val result = action(fakeRequest.withFormUrlEncodedBody("INVALID" -> "INVALID"))

                status(result) shouldBe BAD_REQUEST

                val document: Document = Jsoup.parse(contentAsString(result))
                document.title shouldBe getValidationErrorTabTitle(incomeSourceType)
              }
              "an empty form is submitted" in {
                setupMockSuccess(mtdRole)
                enable(IncomeSourcesFs)

                mockNoIncomeSources()

                val result = action(fakeRequest.withFormUrlEncodedBody("" -> ""))

                status(result) shouldBe BAD_REQUEST

                val document: Document = Jsoup.parse(contentAsString(result))
                document.title shouldBe getValidationErrorTabTitle(incomeSourceType)
                document.getElementsByClass("govuk-error-message").text() shouldBe s"Error:: Enter the date your ${getHintText(incomeSourceType)}"
              }
              "no form is submitted" in {
                setupMockSuccess(mtdRole)
                enable(IncomeSourcesFs)

                mockNoIncomeSources()

                val result = action(fakeRequest)

                status(result) shouldBe BAD_REQUEST
              }
            }

            s"return ${Status.SEE_OTHER} and redirect to home page" when {
              "incomeSources FS is disabled" in {
                setupMockSuccess(mtdRole)
                disable(IncomeSourcesFs)

                mockNoIncomeSources()

                val result = action(fakeRequest)

                status(result) shouldBe SEE_OTHER
                val redirectUrl = if (isAgent) controllers.routes.HomeController.showAgent().url else controllers.routes.HomeController.show().url
                redirectLocation(result) shouldBe Some(redirectUrl)
              }
            }
          }

          testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
        }
      }
    }
  }

}
