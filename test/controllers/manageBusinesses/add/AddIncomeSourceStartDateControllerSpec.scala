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

import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import enums.MTDIndividual
import implicits.ImplicitDateFormatter
import mocks.auth.MockAuthActions
import mocks.services.MockSessionService
import models.UIJourneySessionData
import models.core.{CheckMode, Mode, NormalMode}
import models.incomeSourceDetails.AddIncomeSourceData.dateStartedField
import models.incomeSourceDetails.AddIncomeSourceData
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


class AddIncomeSourceStartDateControllerSpec extends MockAuthActions
  with MockSessionService
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

  def sessionDataCompletedJourney(journeyType: IncomeSourceJourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData(incomeSourceCreatedJourneyComplete = Some(true))))

  def sessionDataISAdded(journeyType: IncomeSourceJourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData(incomeSourceAdded = Some(true))))

  def sessionData(journeyType: IncomeSourceJourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString)

  def sessionDataWithDate(journeyType: IncomeSourceJourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData(dateStarted = Some(LocalDate.parse("2022-01-01")))))

  def getInitialMongo(sourceType: IncomeSourceType): Option[UIJourneySessionData] = sourceType match {
    case SelfEmployment => Some(sessionData(IncomeSourceJourneyType(Add, SelfEmployment)))
    case _ =>
      setupMockCreateSession(true)
      None
  }

  def getBackUrl(isAgent: Boolean, mode: Mode, incomeSourceType: IncomeSourceType): String = ((isAgent, mode, incomeSourceType) match {
    case (false, NormalMode, SelfEmployment) => routes.AddBusinessNameController.show(mode = NormalMode)
    case (_, NormalMode, SelfEmployment) => routes.AddBusinessNameController.showAgent(mode = NormalMode)
    case (false, _, SelfEmployment) => routes.IncomeSourceCheckDetailsController.show(SelfEmployment)
    case (_, _, SelfEmployment) => routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment)
    case (false, NormalMode, _) => controllers.manageBusinesses.add.routes.AddPropertyController.show(isAgent)
    case (_, NormalMode, _) => controllers.manageBusinesses.add.routes.AddPropertyController.show(isAgent)
    case (false, _, UkProperty) => routes.IncomeSourceCheckDetailsController.show(UkProperty)
    case (_, _, UkProperty) => routes.IncomeSourceCheckDetailsController.showAgent(UkProperty)
    case (false, _, _) => routes.IncomeSourceCheckDetailsController.show(ForeignProperty)
    case (_, _, _) => routes.IncomeSourceCheckDetailsController.showAgent(ForeignProperty)
  }).url

  def getRedirectUrl(incomeSourceType: IncomeSourceType, isAgent: Boolean, mode: Mode): String = (incomeSourceType, isAgent, mode) match {
    case (_, _, _) => routes.AddIncomeSourceStartDateCheckController.show(isAgent, mode, incomeSourceType).url
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
      List(CheckMode, NormalMode).foreach { mode =>
        s"show($isAgent, $mode, $incomeSourceType)" when {
          val action = testController.show(isAgent, mode, incomeSourceType)
          val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
          s"the user is authenticated as a $mtdRole" should {
            s"return ${Status.OK}: render the Add ${incomeSourceType.key} start date page" when {
              "using the manage businesses journey" in {
                setupMockSuccess(mtdRole)
                mockNoIncomeSources()
                if(mode == CheckMode) {
                  val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)
                  setupMockGetMongo(Right(Some(sessionDataWithDate(journeyType))))
                } else {
                  setupMockGetMongo(Right(getInitialMongo(incomeSourceType)))
                }
                val result = action(fakeRequest)

                val document: Document = Jsoup.parse(contentAsString(result))
                document.title should include(messages(s"${incomeSourceType.startDateMessagesPrefix}.heading"))
                val backUrl = getBackUrl(isAgent, mode, incomeSourceType)
                document.getElementById("back-fallback").attr("href") shouldBe backUrl
                status(result) shouldBe OK
                if(mode == CheckMode) {
                  document.getElementById("value.day").attr("value") shouldBe "1"
                  document.getElementById("value.month").attr("value") shouldBe "1"
                  document.getElementById("value.year").attr("value") shouldBe "2022"
                }
                status(result) shouldBe OK
              }
            }
            if(mode == CheckMode) {
              s"return ${Status.OK}: render the Add Business start date Check page with form filled " when {
                s"session contains key: $dateStartedField (${incomeSourceType.key})" in {
                  setupMockSuccess(mtdRole)

                  mockNoIncomeSources()
                  setupMockCreateSession(true)
                  val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)
                  setupMockGetMongo(Right(Some(sessionDataWithDate(journeyType))))

                  val result = action(fakeRequest)

                  val document: Document = Jsoup.parse(contentAsString(result))
                  document.title should include(messages(s"${incomeSourceType.startDateMessagesPrefix}.heading"))
                  document.getElementById("back-fallback").attr("href") shouldBe {
                    if (isAgent) routes.IncomeSourceCheckDetailsController.showAgent(incomeSourceType).url
                    else routes.IncomeSourceCheckDetailsController.show(incomeSourceType).url
                  }
                  document.getElementById("value.day").attr("value") shouldBe "1"
                  document.getElementById("value.month").attr("value") shouldBe "1"
                  document.getElementById("value.year").attr("value") shouldBe "2022"
                  status(result) shouldBe OK
                }
              }
            }
            s"return ${Status.SEE_OTHER}: redirect to the relevant You Cannot Go Back page" when {
              s"user has already completed the journey (${incomeSourceType.key})" in {
                setupMockSuccess(mtdRole)

                mockNoIncomeSources()
                setupMockGetMongo(Right(Some(sessionDataCompletedJourney(IncomeSourceJourneyType(Add, incomeSourceType)))))

                val result = action(fakeRequest)
                status(result) shouldBe SEE_OTHER
                val redirectUrl = if (isAgent) controllers.manageBusinesses.add.routes.ReportingMethodSetBackErrorController.showAgent(incomeSourceType).url
                else controllers.manageBusinesses.add.routes.ReportingMethodSetBackErrorController.show(incomeSourceType).url
                redirectLocation(result) shouldBe Some(redirectUrl)
              }
              s"user has already added their income source (${incomeSourceType.key})" in {
                setupMockSuccess(mtdRole)
                mockNoIncomeSources()
                setupMockGetMongo(Right(Some(sessionDataISAdded(IncomeSourceJourneyType(Add, incomeSourceType)))))

                val result = action(fakeRequest)
                status(result) shouldBe SEE_OTHER
                val redirectUrl = if (isAgent) controllers.manageBusinesses.add.routes.IncomeSourceAddedBackErrorController.showAgent(incomeSourceType).url
                else controllers.manageBusinesses.add.routes.IncomeSourceAddedBackErrorController.show(incomeSourceType).url
                redirectLocation(result) shouldBe Some(redirectUrl)
              }
            }
          }
          testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
        }

        s"submit($isAgent, $mode, $incomeSourceType)" when {
          val action = testController.submit(isAgent, mode, incomeSourceType)
          val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole).withMethod("POST")
          s"the user is authenticated as a $mtdRole" should {
            s"redirect to the Add Business Start Date Check page" when {
              "a valid form is submitted" in {
                setupMockSuccess(mtdRole)

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
                redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateCheckController.submit(incomeSourceType = incomeSourceType, isAgent = isAgent, mode = mode).url)
              }
            }
            s"return ${Status.BAD_REQUEST}" when {
              "an invalid form is submitted" in {
                setupMockSuccess(mtdRole)

                mockNoIncomeSources()

                val result = action(fakeRequest.withFormUrlEncodedBody("INVALID" -> "INVALID"))

                status(result) shouldBe BAD_REQUEST

                val document: Document = Jsoup.parse(contentAsString(result))
                document.title shouldBe getValidationErrorTabTitle(incomeSourceType)
              }
              "an empty form is submitted" in {
                setupMockSuccess(mtdRole)

                mockNoIncomeSources()

                val result = action(fakeRequest.withFormUrlEncodedBody("" -> ""))

                status(result) shouldBe BAD_REQUEST

                val document: Document = Jsoup.parse(contentAsString(result))
                document.title shouldBe getValidationErrorTabTitle(incomeSourceType)
                document.getElementsByClass("govuk-list govuk-error-summary__list").text() shouldBe s"Enter the date your ${getHintText(incomeSourceType)}"
              }
              "no form is submitted" in {
                setupMockSuccess(mtdRole)

                mockNoIncomeSources()

                val result = action(fakeRequest)

                status(result) shouldBe BAD_REQUEST
              }
            }
          }

          testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
        }
      }
    }
  }
}
