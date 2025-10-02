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
import forms.manageBusinesses.add.AddIncomeSourceStartDateCheckForm
import implicits.ImplicitDateFormatter
import mocks.auth.MockAuthActions
import mocks.services.MockSessionService
import models.UIJourneySessionData
import models.core.{CheckMode, NormalMode}
import models.admin.AccountingMethodJourney
import models.incomeSourceDetails.AddIncomeSourceData.dateStartedField
import models.incomeSourceDetails.AddIncomeSourceData
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.verify
import play.api
import play.api.Application
import play.api.http.Status
import play.api.http.Status.OK
import play.api.test.Helpers._
import services.{DateService, SessionService}
import testConstants.BaseTestConstants.testSessionId

import java.time.LocalDate


class AddIncomeSourceStartDateCheckControllerSpec extends MockAuthActions
  with ImplicitDateFormatter
  with MockSessionService {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[SessionService].toInstance(mockSessionService),
      api.inject.bind[DateService].toInstance(dateService)
    ).build()

  lazy val testAddIncomeSourceStartDateCheckController = app.injector.instanceOf[AddIncomeSourceStartDateCheckController]


  val testStartDate: LocalDate = LocalDate.of(2022, 11, 11)
  val testBusinessAccountingPeriodStartDate: LocalDate = LocalDate.of(2022, 11, 11)
  val testBusinessAccountingPeriodEndDate: LocalDate = LocalDate.of(2024, 4, 5)

  val responseNo: String = AddIncomeSourceStartDateCheckForm.responseNo
  val responseYes: String = AddIncomeSourceStartDateCheckForm.responseYes

  val journeyTypeSE = IncomeSourceJourneyType(Add, SelfEmployment)
  val journeyTypeUK = IncomeSourceJourneyType(Add, UkProperty)
  val journeyTypeFP = IncomeSourceJourneyType(Add, ForeignProperty)

  def journeyType(sourceType: IncomeSourceType) = IncomeSourceJourneyType(Add, sourceType)

  val addIncomeSourceDataEmpty = AddIncomeSourceData()
  val addIncomeSourceDataProperty = AddIncomeSourceData(dateStarted = Some(testStartDate))
  val addIncomeSourceDataSE = AddIncomeSourceData(dateStarted = Some(testStartDate), accountingPeriodStartDate = Some(testBusinessAccountingPeriodStartDate),
    accountingPeriodEndDate = Some(testBusinessAccountingPeriodEndDate))

  val addIncomeSourceDataPropertyWithAccSD = AddIncomeSourceData(dateStarted = Some(testStartDate), accountingPeriodStartDate = Some(testStartDate))

  val uiJourneySessionDataSE: UIJourneySessionData = UIJourneySessionData("session-123456", "ADD-SE", Some(addIncomeSourceDataSE))
  val uiJourneySessionDataUK: UIJourneySessionData = UIJourneySessionData("session-123456", "ADD-UK", Some(addIncomeSourceDataProperty))
  val uiJourneySessionDataFP: UIJourneySessionData = UIJourneySessionData("session-123456", "ADD-FP", Some(addIncomeSourceDataProperty))

  def getValidationErrorTabTitle: String = {
    s"${messages("htmlTitle.invalidInput", messages("radioForm.checkDate.heading.withDate", "11 November 2022"))}"
  }

  def uiJourneySessionData(incomeSourceType: IncomeSourceType): UIJourneySessionData = incomeSourceType match {
    case SelfEmployment => uiJourneySessionDataSE
    case UkProperty => uiJourneySessionDataUK
    case ForeignProperty => uiJourneySessionDataFP
  }

  val UIJourneySessionDataUkWithAccSD: UIJourneySessionData = UIJourneySessionData("session-123456", "ADD-UK", Some(addIncomeSourceDataPropertyWithAccSD))
  val UIJourneySessionDataFpWithAccSD: UIJourneySessionData = UIJourneySessionData("session-123456", "ADD-FP", Some(addIncomeSourceDataPropertyWithAccSD))

  def dataWithAccSD(incomeSourceType: IncomeSourceType) = incomeSourceType match {
    case SelfEmployment => uiJourneySessionDataSE
    case UkProperty => UIJourneySessionDataUkWithAccSD
    case ForeignProperty => UIJourneySessionDataFpWithAccSD
  }

  val incomeSourceTypes: Seq[IncomeSourceType with Serializable] = List(SelfEmployment, UkProperty, ForeignProperty)

  val heading: String = messages("dateForm.check.heading")
  val titleAgent: String = s"${messages("htmlTitle.agent", heading)}"
  val title: String = s"${messages("htmlTitle", heading)}"


  def sessionDataCompletedJourney(journeyType: IncomeSourceJourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData(incomeSourceCreatedJourneyComplete = Some(true))))

  def sessionDataISAdded(journeyType: IncomeSourceJourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData(incomeSourceAdded = Some(true))))

  def sessionData(journeyType: IncomeSourceJourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData()))

  def sessionDataWithDate(journeyType: IncomeSourceJourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData(dateStarted = Some(LocalDate.parse("2022-11-11")))))

  def getInitialMongo(sourceType: IncomeSourceType): Option[UIJourneySessionData] = {
    Some(sessionData(IncomeSourceJourneyType(Add, sourceType)))
  }

  def verifyMongoDatesRemoved(): Unit = {
    val argument: ArgumentCaptor[UIJourneySessionData] = ArgumentCaptor.forClass(classOf[UIJourneySessionData])
    verify(mockSessionService).setMongoData(argument.capture())

    argument.getValue.addIncomeSourceData shouldBe Some(addIncomeSourceDataEmpty)
  }

  def verifySetMongoData(incomeSourceType: IncomeSourceType): Unit = {
    val argument: ArgumentCaptor[UIJourneySessionData] = ArgumentCaptor.forClass(classOf[UIJourneySessionData])
    verify(mockSessionService).setMongoData(argument.capture())

    if (incomeSourceType.equals(SelfEmployment)) {
      argument.getValue.addIncomeSourceData shouldBe Some(addIncomeSourceDataSE)
    } else {
      argument.getValue.addIncomeSourceData shouldBe Some(addIncomeSourceDataProperty)
    }
  }

  mtdAllRoles.foreach { mtdRole =>
    List(NormalMode, CheckMode).foreach { mode =>
      val isAgent = mtdRole != MTDIndividual
      incomeSourceTypes.foreach { incomeSourceType =>
        s"show(${if (isAgent) "Agent"}, $mode, $incomeSourceType)" when {
          val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
          val action = testAddIncomeSourceStartDateCheckController.show(isAgent, mode, incomeSourceType)
          s"the user is authenticated as a $mtdRole" should {
            s"render the start date check page" when {
              s"session contains key: ${AddIncomeSourceData.accountingPeriodStartDateField}" in {
                setupMockSuccess(mtdRole)
                mockNoIncomeSources()
                setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyType(incomeSourceType), Right(Some(testStartDate)))

                if(mode == CheckMode) {
                  setupMockGetMongo(Right(Some(sessionDataWithDate(journeyType(incomeSourceType)))))
                } else {
                  setupMockGetMongo(Right(Some(dataWithAccSD(incomeSourceType))))
                }

                val result = action(fakeRequest)

                val document: Document = Jsoup.parse(contentAsString(result))

                document.getElementById("back-fallback").attr("href") shouldBe routes.AddIncomeSourceStartDateController.show(incomeSourceType = incomeSourceType, isAgent = isAgent, mode = mode).url
                status(result) shouldBe OK
              }
            }

            s"return ${Status.SEE_OTHER}: redirect to the relevant You Cannot Go Back page" when {
              "user has already completed the journey" in {
                setupMockSuccess(mtdRole)

                mockNoIncomeSources()
                setupMockGetMongo(Right(Some(sessionDataCompletedJourney(journeyType(incomeSourceType)))))

                val result = action(fakeRequest)

                status(result) shouldBe SEE_OTHER
                val redirectUrl = if (isAgent) routes.ReportingMethodSetBackErrorController.showAgent(incomeSourceType).url
                else routes.ReportingMethodSetBackErrorController.show(incomeSourceType).url
                redirectLocation(result) shouldBe Some(redirectUrl)
              }
              "user has already added their income source" in {
                setupMockSuccess(mtdRole)

                mockNoIncomeSources()
                setupMockGetMongo(Right(Some(sessionDataISAdded(journeyType(incomeSourceType)))))

                val result = action(fakeRequest)

                status(result) shouldBe SEE_OTHER
                val redirectUrl = if (isAgent) routes.IncomeSourceAddedBackErrorController.showAgent(incomeSourceType).url
                else routes.IncomeSourceAddedBackErrorController.show(incomeSourceType).url
                redirectLocation(result) shouldBe Some(redirectUrl)
              }
            }

            s"render the error page" when {
              s"calling Business Start Date Check Page but session does not contain key: ${AddIncomeSourceData.accountingPeriodStartDateField}" in {
                setupMockSuccess(mtdRole)

                mockNoIncomeSources()
                setupMockGetMongo(Right(Some(sessionData(journeyType(incomeSourceType)))))

                val result = action(fakeRequest)

                status(result) shouldBe INTERNAL_SERVER_ERROR
              }
            }
          }
        }

        s"submit(${if (isAgent) "Agent"}, ${mode == CheckMode}, $incomeSourceType)" when {
          val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole).withMethod("POST")
          val action = testAddIncomeSourceStartDateCheckController.submit(isAgent, mode, incomeSourceType)
          s"the user is authenticated as a $mtdRole" should {

            s"return ${Status.SEE_OTHER}: redirect back to add $incomeSourceType start date page with ${AddIncomeSourceData.accountingPeriodStartDateField} removed from session, isAgent = $isAgent" when {
              "No is submitted with the form" in {
                setupMockSuccess(mtdRole)

                mockNoIncomeSources()
                setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyType(incomeSourceType), Right(Some(testStartDate)))
                setupMockGetMongo(Right(Some(uiJourneySessionData(incomeSourceType))))
                setupMockSetMongoData(result = true)

                val result = action(fakeRequest
                  .withFormUrlEncodedBody(
                    AddIncomeSourceStartDateCheckForm.response -> responseNo
                  ))

                status(result) shouldBe SEE_OTHER
                verifyMongoDatesRemoved()
                redirectLocation(result) shouldBe Some(routes.AddIncomeSourceStartDateController.show(incomeSourceType = incomeSourceType, isAgent = isAgent, mode = mode).url)

              }
            }
            if(mode == NormalMode) {
              s"return ${Status.SEE_OTHER}: redirect to $incomeSourceType accounting method page, isAgent = $isAgent" when {
                "Yes is submitted with the form with a valid session" in {
                  setupMockSuccess(mtdRole)
                  enable(AccountingMethodJourney)

                  mockNoIncomeSources()
                  setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyType(incomeSourceType), Right(Some(testStartDate)))
                  setupMockSetMongoData(result = true)
                  setupMockGetMongo(Right(Some(sessionDataWithDate(IncomeSourceJourneyType(Add, incomeSourceType)))))

                  val result = action(fakeRequest
                    .withFormUrlEncodedBody(
                      AddIncomeSourceStartDateCheckForm.response -> responseYes
                    ))

                  status(result) shouldBe SEE_OTHER
                  if (incomeSourceType == SelfEmployment) verifySetMongoData(SelfEmployment)
                  redirectLocation(result) shouldBe Some({
                    incomeSourceType match {
                      case SelfEmployment if !isAgent => routes.AddBusinessTradeController.show(mode = mode)
                      case SelfEmployment => routes.AddBusinessTradeController.showAgent(mode = mode)
                      case _ => routes.IncomeSourcesAccountingMethodController.show(incomeSourceType, isAgent)
                    }
                  }.url)
                }
                "Yes is submitted with the form with a valid session (accounting method FS disabled)" in {
                  setupMockSuccess(mtdRole)

                  mockNoIncomeSources()
                  setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyType(incomeSourceType), Right(Some(testStartDate)))
                  setupMockSetMongoData(result = true)
                  setupMockGetMongo(Right(Some(sessionDataWithDate(IncomeSourceJourneyType(Add, incomeSourceType)))))

                  val result = action(fakeRequest
                    .withFormUrlEncodedBody(
                      AddIncomeSourceStartDateCheckForm.response -> responseYes
                    ))

                  status(result) shouldBe SEE_OTHER
                  if (incomeSourceType == SelfEmployment) verifySetMongoData(SelfEmployment)
                  redirectLocation(result) shouldBe Some({
                    incomeSourceType match {
                      case SelfEmployment if !isAgent => routes.AddBusinessTradeController.show(mode = mode)
                      case SelfEmployment => routes.AddBusinessTradeController.showAgent(mode = mode)
                      case _ if(isAgent) => routes.IncomeSourceCheckDetailsController.showAgent(incomeSourceType)
                      case _ => routes.IncomeSourceCheckDetailsController.show(incomeSourceType)
                    }
                  }.url)
                }
              }
            } else {
              s"return ${Status.SEE_OTHER}: redirect to check $incomeSourceType details page, isAgent = $isAgent" when {
                "Yes is submitted with isUpdate flag set to true" in {
                  setupMockSuccess(mtdRole)
                  mockNoIncomeSources()

                  setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyType(incomeSourceType), Right(Some(testStartDate)))
                  setupMockGetMongo(Right(Some(sessionDataWithDate(IncomeSourceJourneyType(Add, incomeSourceType)))))
                  setupMockSetMongoData(result = true)

                  val result = action(fakeRequest
                    .withFormUrlEncodedBody(
                      AddIncomeSourceStartDateCheckForm.response -> responseYes
                    ))

                  status(result) shouldBe SEE_OTHER
                  if (incomeSourceType == SelfEmployment) verifySetMongoData(SelfEmployment)
                  redirectLocation(result) shouldBe Some({
                    if (isAgent) routes.IncomeSourceCheckDetailsController.showAgent(incomeSourceType) else routes.IncomeSourceCheckDetailsController.show(incomeSourceType)
                  }.url)
                }
              }
            }

            s"return ${Status.BAD_REQUEST} with an error summary(isAgent = $isAgent, $incomeSourceType)" when {
              "form is submitted with neither radio option selected" in {
                setupMockSuccess(mtdRole)

                mockNoIncomeSources()
                setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyType(incomeSourceType), Right(Some(testStartDate)))
                setupMockGetMongo(Right(Some(sessionDataWithDate(IncomeSourceJourneyType(Add, incomeSourceType)))))

                val result = action(fakeRequest
                  .withFormUrlEncodedBody())

                status(result) shouldBe BAD_REQUEST
              }
              "an invalid response is submitted" in {
                setupMockSuccess(mtdRole)

                mockNoIncomeSources()
                setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyType(incomeSourceType), Right(Some(testStartDate)))
                setupMockGetMongo(Right(Some(sessionDataWithDate(IncomeSourceJourneyType(Add, incomeSourceType)))))
                setupMockGetMongo(Right(Some(uiJourneySessionDataFP)))

                val invalidResponse: String = "£££"

                val result = action(fakeRequest
                  .withFormUrlEncodedBody(
                    AddIncomeSourceStartDateCheckForm.response -> invalidResponse
                  ))

                status(result) shouldBe BAD_REQUEST

                val document: Document = Jsoup.parse(contentAsString(result))
                document.title shouldBe getValidationErrorTabTitle
              }
            }
          }
        }
      }
    }
  }
}

