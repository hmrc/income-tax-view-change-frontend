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

import controllers.ControllerISpecHelper
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import enums.{MTDIndividual, MTDUserRole}
import forms.incomeSources.add.AddIncomeSourceStartDateCheckForm
import forms.incomeSources.add.AddIncomeSourceStartDateCheckForm.{responseNo, responseYes}
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.{IncomeSourcesFs, NavBarFs}
import models.incomeSourceDetails.AddIncomeSourceData.{accountingPeriodEndDateField, accountingPeriodStartDateField, dateStartedField}
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
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

  val sessionService:                SessionService          = app.injector.instanceOf[SessionService]
  val journeyTypeSE:                 IncomeSourceJourneyType = IncomeSourceJourneyType(Add, SelfEmployment)
  val journeyTypeUK:                 IncomeSourceJourneyType = IncomeSourceJourneyType(Add, UkProperty)
  val journeyTypeFP:                 IncomeSourceJourneyType = IncomeSourceJourneyType(Add, ForeignProperty)
  override implicit val ec:          ExecutionContext        = app.injector.instanceOf[ExecutionContext]
  val testBusinessStartDate:         LocalDate               = LocalDate.of(2022, 10, 10)
  val testAccountingPeriodStartDate: LocalDate               = LocalDate.of(2022, 10, 10)
  val testAccountingPeriodEndDate:   LocalDate               = LocalDate.of(2023, 4, 5)
  val testBusinessName:              String                  = "Test Business"
  val testBusinessTrade:             String                  = "Plumbing"

  val testAddIncomeSourceDataWithStartDate: IncomeSourceType => AddIncomeSourceData =
    (incomeSourceType: IncomeSourceType) =>
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

  def testUIJourneySessionData(incomeSourceType: IncomeSourceType): UIJourneySessionData =
    UIJourneySessionData(
      sessionId = testSessionId,
      journeyType = IncomeSourceJourneyType(Add, incomeSourceType).toString,
      addIncomeSourceData = Some(testAddIncomeSourceDataWithStartDate(incomeSourceType))
    )

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Add))
  }

  def getPath(mtdRole: MTDUserRole, incomeSourceType: IncomeSourceType, isChange: Boolean): String = {
    val pathStart       = if (mtdRole == MTDIndividual) "/income-sources/add" else "/agents/income-sources/add"
    val changeFlagIfReq = if (isChange) "/change-" else "/"
    incomeSourceType match {
      case SelfEmployment  => pathStart + changeFlagIfReq + "business-start-date-check"
      case UkProperty      => pathStart + changeFlagIfReq + "uk-property-start-date-check"
      case ForeignProperty => pathStart + changeFlagIfReq + "foreign-property-start-date-check"
    }
  }

  def getJourneyType(incomeSourceType: IncomeSourceType): IncomeSourceJourneyType = {
    incomeSourceType match {
      case SelfEmployment  => journeyTypeSE
      case UkProperty      => journeyTypeUK
      case ForeignProperty => journeyTypeFP
    }
  }

  def getIncomeSourceDetailsResponse(incomeSourceType: IncomeSourceType) = {
    incomeSourceType match {
      case SelfEmployment  => businessOnlyResponse
      case UkProperty      => ukPropertyOnlyResponse
      case ForeignProperty => noPropertyOrBusinessResponse
    }
  }

  def verifySessionUpdate(incomeSourceType: IncomeSourceType, journeyType: IncomeSourceJourneyType) = {
    sessionService.getMongoKeyTyped[LocalDate](dateStartedField, journeyType).futureValue shouldBe Right(
      Some(testBusinessStartDate)
    )
    sessionService.getMongoKeyTyped[LocalDate](accountingPeriodStartDateField, journeyType).futureValue shouldBe
      Right { if (incomeSourceType == SelfEmployment) Some(testAccountingPeriodStartDate) else None }
    sessionService.getMongoKeyTyped[LocalDate](accountingPeriodEndDateField, journeyType).futureValue shouldBe
      Right { if (incomeSourceType == SelfEmployment) Some(testAccountingPeriodEndDate) else None }
  }

  List(true, false).foreach { isChange =>
    List(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSourceType =>
      mtdAllRoles.foreach { mtdUserRole =>
        val path              = getPath(mtdUserRole, incomeSourceType, isChange)
        val additionalCookies = getAdditionalCookies(mtdUserRole)
        s"GET $path" when {
          s"a user is a $mtdUserRole" that {
            "is authenticated, with a valid enrolment" should {
              "render the Business Start Date Check Page" when {
                "incomesources is enabled" in {
                  enable(IncomeSourcesFs)
                  disable(NavBarFs)
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                    OK,
                    getIncomeSourceDetailsResponse(incomeSourceType)
                  )

                  await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))

                  val result = buildGETMTDClient(path, additionalCookies).futureValue
                  IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

                  result should have(
                    httpStatus(OK),
                    pageTitle(mtdUserRole, "dateForm.check.heading"),
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
          val isAgent     = mtdUserRole != MTDIndividual
          s"a user is a $mtdUserRole" that {
            "is authenticated, with a valid enrolment" should {
              if (isChange) {
                val checkDetailsUrl = if (isAgent) {
                  controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController
                    .showAgent(incomeSourceType)
                    .url
                } else {
                  controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController.show(incomeSourceType).url
                }
                s"redirect to $checkDetailsUrl" when {
                  "form response is Yes" in {
                    enable(IncomeSourcesFs)
                    disable(NavBarFs)
                    stubAuthorised(mtdUserRole)
                    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                      OK,
                      getIncomeSourceDetailsResponse(incomeSourceType)
                    )

                    await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))

                    val result = buildPOSTMTDPostClient(
                      path,
                      additionalCookies,
                      body = Map(AddIncomeSourceStartDateCheckForm.response -> Seq(responseYes))
                    ).futureValue

                    verifySessionUpdate(incomeSourceType, journeyType)
                    result should have(
                      httpStatus(SEE_OTHER),
                      redirectURI(checkDetailsUrl)
                    )
                  }
                }
              } else if (incomeSourceType == SelfEmployment) {
                val addTradeUrl = if (isAgent) {
                  controllers.incomeSources.add.routes.AddBusinessTradeController.showAgent(isChange = false).url
                } else {
                  controllers.incomeSources.add.routes.AddBusinessTradeController.show(isChange = false).url
                }
                s"redirect to $addTradeUrl" when {
                  "form response is Yes" in {
                    enable(IncomeSourcesFs)
                    disable(NavBarFs)
                    stubAuthorised(mtdUserRole)
                    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                      OK,
                      getIncomeSourceDetailsResponse(incomeSourceType)
                    )

                    await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))

                    val result = buildPOSTMTDPostClient(
                      path,
                      additionalCookies,
                      body = Map(AddIncomeSourceStartDateCheckForm.response -> Seq(responseYes))
                    ).futureValue

                    verifySessionUpdate(incomeSourceType, journeyType)
                    result should have(
                      httpStatus(SEE_OTHER),
                      redirectURI(addTradeUrl)
                    )
                  }
                }
              } else {
                val accountingMethodUrl = controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController
                  .show(incomeSourceType, isAgent)
                  .url
                s"redirect to $accountingMethodUrl" when {
                  "form response is Yes" in {
                    enable(IncomeSourcesFs)
                    disable(NavBarFs)
                    stubAuthorised(mtdUserRole)
                    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                      OK,
                      getIncomeSourceDetailsResponse(incomeSourceType)
                    )

                    await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))

                    val result = buildPOSTMTDPostClient(
                      path,
                      additionalCookies,
                      body = Map(AddIncomeSourceStartDateCheckForm.response -> Seq(responseYes))
                    ).futureValue

                    verifySessionUpdate(incomeSourceType, journeyType)
                    result should have(
                      httpStatus(SEE_OTHER),
                      redirectURI(accountingMethodUrl)
                    )
                  }
                }
              }
              val expectedUrlForNo = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController
                .show(isAgent, isChange, incomeSourceType)
                .url
              s"redirect to $expectedUrlForNo" when {
                "form response is No" in {
                  enable(IncomeSourcesFs)
                  disable(NavBarFs)
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                    OK,
                    getIncomeSourceDetailsResponse(incomeSourceType)
                  )

                  await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))

                  val result = buildPOSTMTDPostClient(
                    path,
                    additionalCookies,
                    body = Map(AddIncomeSourceStartDateCheckForm.response -> Seq(responseNo))
                  ).futureValue

                  result should have(
                    httpStatus(SEE_OTHER),
                    redirectURI(expectedUrlForNo)
                  )

                  sessionService.getMongoKeyTyped[LocalDate](dateStartedField, journeyType).futureValue shouldBe Right(
                    None
                  )
                  sessionService
                    .getMongoKeyTyped[LocalDate](accountingPeriodStartDateField, journeyType)
                    .futureValue shouldBe Right(None)
                  sessionService
                    .getMongoKeyTyped[LocalDate](accountingPeriodEndDateField, journeyType)
                    .futureValue shouldBe Right(None)
                }
              }
              "return a BAD_REQUEST" when {
                "form is empty" in {
                  enable(IncomeSourcesFs)
                  disable(NavBarFs)
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                    OK,
                    getIncomeSourceDetailsResponse(incomeSourceType)
                  )

                  await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))

                  val result = buildPOSTMTDPostClient(path, additionalCookies, body = Map.empty).futureValue

                  result should have(
                    httpStatus(BAD_REQUEST),
                    elementTextByID(s"$incomeSourcePrefix-error")(
                      messagesAPI("base.error-prefix") + " " +
                        messagesAPI(s"${incomeSourceType.addStartDateCheckMessagesPrefix}.error")
                    )
                  )
                }

                "invalid entry given" in {
                  enable(IncomeSourcesFs)
                  disable(NavBarFs)
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                    OK,
                    getIncomeSourceDetailsResponse(incomeSourceType)
                  )

                  await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))

                  val result = buildPOSTMTDPostClient(
                    path,
                    additionalCookies,
                    body = Map(incomeSourceType.addStartDateCheckMessagesPrefix -> Seq("@"))
                  ).futureValue

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
