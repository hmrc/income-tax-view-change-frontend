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
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, JourneyType}
import helpers.servicemocks.{IncomeTaxViewChangeStub, MTDIndividualAuthStub}
import models.admin.{IncomeSources, NavBarFs}
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants._
import testConstants.BusinessDetailsIntegrationTestConstants.business1
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, foreignPropertyOnlyResponse, ukPropertyOnlyResponse}
import testConstants.IncomeSourcesObligationsIntegrationTestConstants.testObligationsModel
import testConstants.PropertyDetailsIntegrationTestConstants.ukProperty

import java.time.LocalDate

class IncomeSourceAddedControllerISpec extends ControllerISpecHelper {

  val prefix: String = "business-added"
  val continueButtonText: String = messagesAPI(s"$prefix.income-sources-button")
  val day: LocalDate = LocalDate.of(2023, 1, 1)

  val HomeControllerShowUrl: String = controllers.routes.HomeController.show().url
  val pageTitle: String = messagesAPI("htmlTitle", {
    s"${messagesAPI("business-added.uk-property.h1")} " +
      s"${messagesAPI("business-added.uk-property.base")}".trim()
  })
  val confirmationPanelContent: String = {
    s"${messagesAPI("business-added.uk-property.h1")} " +
      s"${messagesAPI("business-added.uk-property.base")}"
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Add))
  }

  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  val pathSEAdded = "/income-sources/add/business-added"
  val pathUKPropertyAdded = "/income-sources/add/uk-property-added"
  val pathForeignPropertyAdded = "/income-sources/add/foreign-property-added"

    s"GET $pathSEAdded" when {
      "the user is authenticated, with a valid MTD enrolment" should {
        "render the Business Added page" when {
          "income sources is enabled" in {
            enable(IncomeSources)
            disable(NavBarFs)
            MTDIndividualAuthStub.stubAuthorised()
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)
            IncomeTaxViewChangeStub.stubGetNextUpdates(testMtditid, testObligationsModel)

            await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-SE",
              addIncomeSourceData = Some(AddIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId))))))

            val result = buildGETMTDClient(pathSEAdded).futureValue
            verifyIncomeSourceDetailsCall(testMtditid)

            val expectedText: String = if (messagesAPI("business-added.sole-trader.head").nonEmpty) {
              messagesAPI("business-added.sole-trader.head") + " " + business1.tradingName.getOrElse("") + " " + messagesAPI("business-added.sole-trader.base")
            }
            else {
              business1.tradingName.getOrElse("") + " " + messagesAPI("business-added.sole-trader.base")
            }
            sessionService.getMongoKey(AddIncomeSourceData.journeyIsCompleteField, JourneyType(Add, SelfEmployment)).futureValue shouldBe Right(Some(true))

            result should have(
              httpStatus(OK),
              pageTitleIndividual(expectedText),
              elementTextByID("continue-button")(continueButtonText)
            )
          }
        }
        s"redirect to $HomeControllerShowUrl" when {
          "Income Sources Feature Switch is disabled" in {
            disable(IncomeSources)
            disable(NavBarFs)
            MTDIndividualAuthStub.stubAuthorised()
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

            val result = buildGETMTDClient(pathSEAdded).futureValue

            result should have(
              httpStatus(SEE_OTHER),
              redirectURI(HomeControllerShowUrl)
            )
          }
        }
      }
      testAuthFailuresForMTDIndividual(pathSEAdded)
    }

  s"POST $pathSEAdded" when {
    "the user is authenticated, with a valid MTD enrolment" should {
      "redirect to add income source controller" in {
        enable(IncomeSources)
        disable(NavBarFs)
        MTDIndividualAuthStub.stubAuthorised()
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        val result = buildGETMTDClient(pathSEAdded).futureValue

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI("/report-quarterly/income-and-expenses/view/income-sources/add/new-income-sources")
        )
      }
    }
  }

  s"GET $pathUKPropertyAdded" when {
    "the user is authenticated, with a valid MTD enrolment" should {
        "render the Business Added page" when {
          "income sources is enabled" in {
            enable(IncomeSources)
            disable(NavBarFs)
            MTDIndividualAuthStub.stubAuthorised()

            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)
            IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, testObligationsModel)
            await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-UK",
              addIncomeSourceData = Some(AddIncomeSourceData(incomeSourceId = Some(testPropertyIncomeId))))))

            val result = buildGETMTDClient(pathUKPropertyAdded).futureValue
            sessionService.getMongoKey(AddIncomeSourceData.journeyIsCompleteField, JourneyType(Add, UkProperty)).futureValue shouldBe Right(Some(true))

            result should have(
              httpStatus(OK),
              pageTitleCustom(pageTitle),
              elementTextBySelectorList(".govuk-panel.govuk-panel--confirmation")(confirmationPanelContent)
            )
          }
        }
        s"redirect to $HomeControllerShowUrl" when {
          "Income Sources Feature Switch is disabled" in {
            disable(IncomeSources)
            disable(NavBarFs)
            MTDIndividualAuthStub.stubAuthorised()
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

            val result = buildGETMTDClient(pathUKPropertyAdded).futureValue

            result should have(
              httpStatus(SEE_OTHER),
              redirectURI(HomeControllerShowUrl)
            )
          }
        }
        "render error page" when {
          "UK property income source is missing trading start date" in {
            enable(IncomeSources)
            MTDIndividualAuthStub.stubAuthorised()
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse.copy(properties = List(ukProperty.copy(tradingStartDate = None))))

            await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-UK",
              addIncomeSourceData = Some(AddIncomeSourceData(incomeSourceId = Some(testPropertyIncomeId))))))

            val result = buildGETMTDClient(pathUKPropertyAdded).futureValue

            result should have(
              httpStatus(INTERNAL_SERVER_ERROR),
              pageTitleAgent("standardError.heading", isErrorPage = true)
            )
          }
        }
      }
      testAuthFailuresForMTDIndividual(pathUKPropertyAdded)
  }

  s"POST $pathUKPropertyAdded" when {
    "the user is authenticated, with a valid MTD enrolment" should {
      "redirect to add income source controller" in {
        enable(IncomeSources)
        disable(NavBarFs)
        MTDIndividualAuthStub.stubAuthorised()
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = buildGETMTDClient(pathUKPropertyAdded).futureValue

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI("/report-quarterly/income-and-expenses/view/income-sources/add/new-income-sources")
        )
      }
    }
  }

  s"GET $pathForeignPropertyAdded" when {
    "the user is authenticated, with a valid MTD enrolment" should {
      "render the Business Added page" when {
        "income sources is enabled" in {
          enable(IncomeSources)
          disable(NavBarFs)
          MTDIndividualAuthStub.stubAuthorised()
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)
          IncomeTaxViewChangeStub.stubGetNextUpdates(testMtditid, testObligationsModel)

          await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-FP",
            addIncomeSourceData = Some(AddIncomeSourceData(incomeSourceId = Some(testPropertyIncomeId))))))

          val result = buildGETMTDClient(pathForeignPropertyAdded).futureValue
          verifyIncomeSourceDetailsCall(testMtditid)
          sessionService.getMongoKey(AddIncomeSourceData.journeyIsCompleteField, JourneyType(Add, ForeignProperty)).futureValue shouldBe Right(Some(true))

          val expectedText: String = messagesAPI("business-added.foreign-property.h1") + " " + messagesAPI("business-added.foreign-property.base")

          result should have(
            httpStatus(OK),
            pageTitleIndividual(expectedText),
            elementTextByID("continue-button")(continueButtonText)
          )
        }
      }
      s"redirect to $HomeControllerShowUrl" when {
        "Income Sources Feature Switch is disabled" in {
          disable(IncomeSources)
          disable(NavBarFs)
          MTDIndividualAuthStub.stubAuthorised()
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

          val result = buildGETMTDClient(pathForeignPropertyAdded).futureValue

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(HomeControllerShowUrl)
          )
        }
      }
    }
    testAuthFailuresForMTDIndividual(pathForeignPropertyAdded)
  }

  s"POST $pathForeignPropertyAdded" when {
    "the user is authenticated, with a valid MTD enrolment" should {
      "redirect to add income source controller" in {
        enable(IncomeSources)
        disable(NavBarFs)
        MTDIndividualAuthStub.stubAuthorised()
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        val result = buildGETMTDClient(pathForeignPropertyAdded).futureValue

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI("/report-quarterly/income-and-expenses/view/income-sources/add/new-income-sources")
        )
      }
    }
  }
}
