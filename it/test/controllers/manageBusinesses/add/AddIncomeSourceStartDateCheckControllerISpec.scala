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

import controllers.ControllerISpecHelper
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import enums.{MTDIndividual, MTDUserRole}
import forms.manageBusinesses.add.AddIncomeSourceStartDateCheckForm.{responseNo, responseYes}
import forms.manageBusinesses.add.AddIncomeSourceStartDateCheckForm
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.UIJourneySessionData
import models.admin.{NavBarFs, AccountingMethodJourney}
import models.core.{CheckMode, Mode, NormalMode}
import models.incomeSourceDetails.AddIncomeSourceData.{accountingPeriodEndDateField, accountingPeriodStartDateField, dateStartedField}
import models.incomeSourceDetails.AddIncomeSourceData
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants.{testMtditid, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, noPropertyOrBusinessResponse, ukPropertyOnlyResponse}

import java.time.LocalDate
import scala.concurrent.ExecutionContext

class AddIncomeSourceStartDateCheckControllerISpec extends ControllerISpecHelper {
  val continueButtonText: String = messagesAPI("base.continue")
  val incomeSourcePrefix: String = "start-date-check"

  val sessionService: SessionService = app.injector.instanceOf[SessionService]
  val journeyTypeSE: IncomeSourceJourneyType = IncomeSourceJourneyType(Add, SelfEmployment)
  val journeyTypeUK: IncomeSourceJourneyType = IncomeSourceJourneyType(Add, UkProperty)
  val journeyTypeFP: IncomeSourceJourneyType = IncomeSourceJourneyType(Add, ForeignProperty)
  override implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  val testBusinessStartDate: LocalDate = LocalDate.of(2022, 10, 10)
  val testAccountingPeriodStartDate: LocalDate = LocalDate.of(2022, 10, 10)
  val testAccountingPeriodEndDate: LocalDate = LocalDate.of(2023, 4, 5)
  val testBusinessName: String = "Test Business"
  val testBusinessTrade: String = "Plumbing"


  val testAddIncomeSourceDataWithStartDate: IncomeSourceType => AddIncomeSourceData = (incomeSourceType: IncomeSourceType) =>
    if (incomeSourceType.equals(SelfEmployment)) {
      AddIncomeSourceData(
        businessName = Some(testBusinessName),
        businessTrade = Some(testBusinessTrade),
        dateStarted = Some(testBusinessStartDate)
      )
    } else {
      AddIncomeSourceData(
        businessName = None,
        businessTrade = None,
        dateStarted = Some(testBusinessStartDate)
      )
    }

  def testUIJourneySessionData(incomeSourceType: IncomeSourceType): UIJourneySessionData = UIJourneySessionData(
    sessionId = testSessionId,
    journeyType = IncomeSourceJourneyType(Add, incomeSourceType).toString,
    addIncomeSourceData = Some(testAddIncomeSourceDataWithStartDate(incomeSourceType)))

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Add))
  }

  def getPath(mtdRole: MTDUserRole, incomeSourceType: IncomeSourceType, mode: Mode): String = {
    val pathStart = if(mtdRole == MTDIndividual) "/manage-your-businesses" else "/agents/manage-your-businesses"
    val pathEnd = s"/${if(mode == CheckMode) "change-" else ""}business-start-date-check"
    incomeSourceType match {
      case SelfEmployment => s"$pathStart/add-sole-trader$pathEnd"
      case UkProperty => s"$pathStart/add-uk-property$pathEnd"
      case ForeignProperty => s"$pathStart/add-foreign-property$pathEnd"
    }
  }

  def getJourneyType(incomeSourceType: IncomeSourceType): IncomeSourceJourneyType = {
    incomeSourceType match {
      case SelfEmployment => journeyTypeSE
      case UkProperty => journeyTypeUK
      case ForeignProperty => journeyTypeFP
    }
  }

  def getIncomeSourceDetailsResponse(incomeSourceType: IncomeSourceType) = {
    incomeSourceType match {
      case SelfEmployment => businessOnlyResponse
      case UkProperty => ukPropertyOnlyResponse
      case ForeignProperty => noPropertyOrBusinessResponse
    }
  }

  def verifySessionUpdate(incomeSourceType: IncomeSourceType, journeyType: IncomeSourceJourneyType) = {
    sessionService.getMongoKeyTyped[LocalDate](dateStartedField, journeyType).futureValue shouldBe Right(Some(testBusinessStartDate))
    sessionService.getMongoKeyTyped[LocalDate](accountingPeriodStartDateField, journeyType).futureValue shouldBe
      Right{ if(incomeSourceType == SelfEmployment) Some(testAccountingPeriodStartDate) else None }
    sessionService.getMongoKeyTyped[LocalDate](accountingPeriodEndDateField, journeyType).futureValue shouldBe
      Right{if(incomeSourceType == SelfEmployment) Some(testAccountingPeriodEndDate) else None }
  }

  List(CheckMode, NormalMode).foreach { mode =>
    List(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSourceType =>
      mtdAllRoles.foreach { mtdUserRole =>
        val path = getPath(mtdUserRole, incomeSourceType, mode)
        val additionalCookies = getAdditionalCookies(mtdUserRole)
        s"GET $path" when {
          s"a user is a $mtdUserRole" that {
            "is authenticated, with a valid enrolment" should {
              "render the Business Start Date Check Page" when {
                "using the manage businesses journey" in {
                  disable(NavBarFs)
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, getIncomeSourceDetailsResponse(incomeSourceType))

                  await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))

                  val result = buildGETMTDClient(path, additionalCookies).futureValue
                  IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

                  result should have(
                    httpStatus(OK),
                    pageTitle(mtdUserRole, messagesAPI("radioForm.checkDate.heading.withDate", "10 October 2022")),
                    elementTextByID("continue-button")(continueButtonText)
                  )
                }
              }
            }
            testAuthFailures(path, mtdUserRole)
          }
        }

        s"POST $path" when {
          val journeyType = getJourneyType(incomeSourceType)
          val isAgent = mtdUserRole != MTDIndividual
          s"a user is a $mtdUserRole" that {
            "is authenticated, with a valid enrolment" should {
              if(mode == CheckMode) {
                val checkDetailsUrl = if(isAgent) {
                  controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController.showAgent(incomeSourceType).url
                } else {
                  controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController.show(incomeSourceType).url
                }
                s"redirect to $checkDetailsUrl" when {
                  "form response is Yes" in {
                    disable(NavBarFs)
                    stubAuthorised(mtdUserRole)
                    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, getIncomeSourceDetailsResponse(incomeSourceType))

                    await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))

                    val result = buildPOSTMTDPostClient(path, additionalCookies,
                      body = Map(AddIncomeSourceStartDateCheckForm.response -> Seq(responseYes))).futureValue

                    verifySessionUpdate(incomeSourceType, journeyType)
                    result should have(
                      httpStatus(SEE_OTHER),
                      redirectURI(checkDetailsUrl)
                    )
                  }
                }
              } else if(incomeSourceType == SelfEmployment) {
                val addTradeUrl = if(isAgent) {
                  controllers.manageBusinesses.add.routes.AddBusinessTradeController.showAgent(mode = NormalMode).url
                  } else {
                  controllers.manageBusinesses.add.routes.AddBusinessTradeController.show(mode = NormalMode).url
                  }
                s"redirect to $addTradeUrl" when {
                  "form response is Yes" in {
                    disable(NavBarFs)
                    stubAuthorised(mtdUserRole)
                    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, getIncomeSourceDetailsResponse(incomeSourceType))

                    await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))

                    val result = buildPOSTMTDPostClient(path, additionalCookies,
                      body = Map(AddIncomeSourceStartDateCheckForm.response -> Seq(responseYes))).futureValue

                    verifySessionUpdate(incomeSourceType, journeyType)
                    result should have(
                      httpStatus(SEE_OTHER),
                      redirectURI(addTradeUrl)
                    )
                  }
                }
              } else {
                val accountingMethodUrl = controllers.manageBusinesses.add.routes.IncomeSourcesAccountingMethodController.show(incomeSourceType, isAgent).url
                val checkDetailsUrl =
                  if(isAgent) controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController.showAgent(incomeSourceType).url
                  else controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController.show(incomeSourceType).url
                s"redirect to $accountingMethodUrl" when {
                  "form response is Yes" in {
                    enable(AccountingMethodJourney)
                    disable(NavBarFs)
                    stubAuthorised(mtdUserRole)
                    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, getIncomeSourceDetailsResponse(incomeSourceType))

                    await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))

                    val result = buildPOSTMTDPostClient(path, additionalCookies,
                      body = Map(AddIncomeSourceStartDateCheckForm.response -> Seq(responseYes))).futureValue

                    verifySessionUpdate(incomeSourceType, journeyType)
                    result should have(
                      httpStatus(SEE_OTHER),
                      redirectURI(accountingMethodUrl)
                    )
                  }
                  "form response is Yes (accounting method FS disabled)" in {
                    disable(NavBarFs)
                    stubAuthorised(mtdUserRole)
                    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, getIncomeSourceDetailsResponse(incomeSourceType))

                    await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))

                    val result = buildPOSTMTDPostClient(path, additionalCookies,
                      body = Map(AddIncomeSourceStartDateCheckForm.response -> Seq(responseYes))).futureValue

                    verifySessionUpdate(incomeSourceType, journeyType)
                    result should have(
                      httpStatus(SEE_OTHER),
                      redirectURI(checkDetailsUrl)
                    )
                  }
                }
              }
              val expectedUrlForNo = controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateController
                .show(isAgent, mode, incomeSourceType).url
              s"redirect to $expectedUrlForNo" when {
                "form response is No" in {
                  disable(NavBarFs)
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, getIncomeSourceDetailsResponse(incomeSourceType))

                  await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))

                  val result = buildPOSTMTDPostClient(path, additionalCookies,
                    body = Map(AddIncomeSourceStartDateCheckForm.response -> Seq(responseNo))).futureValue

                  result should have(
                    httpStatus(SEE_OTHER),
                    redirectURI(expectedUrlForNo)
                  )

                  sessionService.getMongoKeyTyped[LocalDate](dateStartedField, journeyType).futureValue shouldBe Right(None)
                  sessionService.getMongoKeyTyped[LocalDate](accountingPeriodStartDateField, journeyType).futureValue shouldBe Right(None)
                  sessionService.getMongoKeyTyped[LocalDate](accountingPeriodEndDateField, journeyType).futureValue shouldBe Right(None)
                }
              }
              "return a BAD_REQUEST" when {
                "form is empty" in {
                  disable(NavBarFs)
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, getIncomeSourceDetailsResponse(incomeSourceType))

                  await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))

                  val result = buildPOSTMTDPostClient(path, additionalCookies, body = Map.empty).futureValue

                  result should have(
                    httpStatus(BAD_REQUEST),
                    elementTextByID(s"$incomeSourcePrefix-error")(messagesAPI("base.error-prefix") + " " +
                      messagesAPI(s"${incomeSourceType.addStartDateCheckMessagesPrefix}.error"))
                  )
                }

                "invalid entry given" in {
                  disable(NavBarFs)
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, getIncomeSourceDetailsResponse(incomeSourceType))

                  await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))

                  val result = buildPOSTMTDPostClient(path, additionalCookies,
                    body = Map(incomeSourceType.addStartDateCheckMessagesPrefix -> Seq("@"))).futureValue

                  result should have(
                    httpStatus(BAD_REQUEST)
                  )
                }
              }
            }
            testAuthFailures(path, mtdUserRole, optBody = Some(Map.empty))
          }
        }
      }
    }
  }
}
