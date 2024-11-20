/*
 * Copyright 2024 HM Revenue & Customs
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
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.UIJourneySessionDataRepository
import services.SessionService
import testConstants.BaseIntegrationTestConstants.{testMtditid, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.businessOnlyResponse

class IncomeSourceAddedBackErrorControllerISpec extends ControllerISpecHelper {

  private lazy val backErrorController = controllers.incomeSources.add.routes.IncomeSourceAddedBackErrorController

  val selfEmploymentBackErrorUrl: String = backErrorController.show(SelfEmployment).url
  val ukPropertyBackErrorUrl: String = backErrorController.show(UkProperty).url
  val foreignPropertyBackErrorUrl: String = backErrorController.show(ForeignProperty).url

  val title = messagesAPI("cannotGoBack.heading")

  val sessionService: SessionService = app.injector.instanceOf[SessionService]
  val UIJourneySessionDataRepository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]
  val journeyType: JourneyType = JourneyType(Add, SelfEmployment)

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Add))
  }

  val pathSE = "/income-sources/add/cannot-go-back-business-reporting-method"
  val pathUKProperty = "/income-sources/add/cannot-go-back-uk-property-reporting-method"
  val pathForeignProperty = "/income-sources/add/cannot-go-back-foreign-property-reporting-method"

    s"GET $pathSE" when {
      "the user is authenticated, with a valid MTD enrolment" should {
        "render the business not added error page" when {
          "Income Sources FS is enabled" in {
            enable(IncomeSources)
            disable(NavBarFs)
            MTDIndividualAuthStub.stubAuthorised()
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

            await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-SE",
              addIncomeSourceData = Some(AddIncomeSourceData(incomeSourceId = Some("1234"), incomeSourceAdded = Some(true), journeyIsComplete = None)))))

            val result = buildGETMTDClient(pathSE).futureValue

            verifyIncomeSourceDetailsCall(testMtditid)

            result should have(
              httpStatus(OK),
              pageTitleIndividual(s"$title")
            )
          }
        }
        "redirect to home page" when {
          "Income Sources FS is disabled" in {
            disable(IncomeSources)
            disable(NavBarFs)
            MTDIndividualAuthStub.stubAuthorised()

            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

            val result = buildGETMTDClient(pathSE).futureValue

            verifyIncomeSourceDetailsCall(testMtditid)

            result should have(
              httpStatus(SEE_OTHER)
            )
          }
        }
      }
    testAuthFailuresForMTDIndividual(pathSE)
  }

  s"GET $pathUKProperty" when {
    "the user is authenticated, with a valid MTD enrolment" should {
      "render the self employment business not added error page" when {
        "Income Sources FS is enabled" in {
          enable(IncomeSources)
          disable(NavBarFs)
          MTDIndividualAuthStub.stubAuthorised()

          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

          await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-UK",
            addIncomeSourceData = Some(AddIncomeSourceData(incomeSourceId = Some("1234"), incomeSourceAdded = Some(true), journeyIsComplete = None)))))

          val result = buildGETMTDClient(pathUKProperty).futureValue

          verifyIncomeSourceDetailsCall(testMtditid)

          result should have(
            httpStatus(OK),
            pageTitleIndividual(s"$title")
          )
        }
      }
      "redirect to home page" when {
        "Income Sources FS is disabled" in {
          disable(NavBarFs)
          MTDIndividualAuthStub.stubAuthorised()
          disable(IncomeSources)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

          val result = buildGETMTDClient(pathUKProperty).futureValue


          verifyIncomeSourceDetailsCall(testMtditid)

          result should have(
            httpStatus(SEE_OTHER)
          )
        }
      }
    }
    testAuthFailuresForMTDIndividual(pathUKProperty)
  }

  s"GET $pathForeignProperty" when {
    "the user is authenticated, with a valid MTD enrolment" should {
      "render the self employment business not added error page" when {
        "Income Sources FS is enabled" in {
          enable(IncomeSources)
          disable(NavBarFs)
          MTDIndividualAuthStub.stubAuthorised()

          And("API 1771  returns a success response")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

          await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-FP",
            addIncomeSourceData = Some(AddIncomeSourceData(incomeSourceId = Some("1234"), incomeSourceAdded = Some(true), journeyIsComplete = None)))))

          val result = buildGETMTDClient(pathForeignProperty).futureValue


          verifyIncomeSourceDetailsCall(testMtditid)

          result should have(
            httpStatus(OK),
            pageTitleIndividual(s"$title")
          )
        }
      }
      "redirect to home page" when {
        "Income Sources FS is disabled" in {
          disable(NavBarFs)
          MTDIndividualAuthStub.stubAuthorised()
          disable(IncomeSources)

          And("API 1771  returns a success response")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

          val result = buildGETMTDClient(pathForeignProperty).futureValue

          verifyIncomeSourceDetailsCall(testMtditid)

          result should have(
            httpStatus(SEE_OTHER)
          )
        }
      }
    }
    testAuthFailuresForMTDIndividual(pathForeignProperty)
  }

}
