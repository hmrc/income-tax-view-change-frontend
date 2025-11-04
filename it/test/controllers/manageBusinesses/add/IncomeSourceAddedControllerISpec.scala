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
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.UIJourneySessionData
import models.admin.NavBarFs
import models.incomeSourceDetails.AddIncomeSourceData
import models.obligations.{GroupedObligationsModel, ObligationsModel, SingleObligationModel, StatusFulfilled}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants._
import testConstants.BusinessDetailsIntegrationTestConstants.business1
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, foreignPropertyOnlyResponse, ukPropertyOnlyResponse}
import testConstants.PropertyDetailsIntegrationTestConstants.ukProperty

import java.time.LocalDate

class IncomeSourceAddedControllerISpec extends ControllerISpecHelper {

  val prefix: String = "business.added"
  val viewBusinessesLinkText: String = "View your businesses"
  val day: LocalDate = LocalDate.of(2023, 1, 1)
  val testObligationsModel: ObligationsModel = ObligationsModel(Seq(GroupedObligationsModel("123", List(SingleObligationModel(day, day.plusDays(1), day.plusDays(2), "EOPS", None, "EOPS", StatusFulfilled)))))


  val pageTitle: String = messagesAPI("htmlTitle.agent", {
    s"${messagesAPI("business.added.uk-property.title")}"
  })
  val confirmationPanelContent: String = {
    s"${messagesAPI("business.added.uk-property.panel.title")} " +
      s"${messagesAPI("business.added.uk-property.panel.body")}"
  }

  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Add))
  }

  def getPath(mtdRole: MTDUserRole, incomeSourceType: IncomeSourceType): String = {
    val pathStart = if (mtdRole == MTDIndividual) "/manage-your-businesses" else "/agents/manage-your-businesses"
    incomeSourceType match {
      case SelfEmployment => s"$pathStart/add-sole-trader/business-added"
      case UkProperty => s"$pathStart/add-uk-property/uk-property-added"
      case ForeignProperty => s"$pathStart/add-foreign-property/foreign-property-added"
    }
  }

  def getExpectedPageTitle(incomeSourceType: IncomeSourceType): String = {
    incomeSourceType match {
      case SelfEmployment => messagesAPI("business.added.sole-trader.title", business1.tradingName.get)
      case UkProperty =>
        s"${messagesAPI("business.added.uk-property.title")}"
      case _ => messagesAPI("business.added.foreign-property.title")
    }
  }

  def getIncomeSourceDetailsResponse(incomeSourceType: IncomeSourceType) = {
    incomeSourceType match {
      case SelfEmployment => businessOnlyResponse
      case UkProperty => ukPropertyOnlyResponse
      case ForeignProperty => foreignPropertyOnlyResponse
    }
  }

  mtdAllRoles.foreach { mtdUserRole =>
    List(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSourceType =>
      val path = getPath(mtdUserRole, incomeSourceType)
      s"GET $path" when {
        val additionalCookies = getAdditionalCookies(mtdUserRole)
        s"GET $path" when {
          s"a user is a $mtdUserRole" that {
            "is authenticated, with a valid enrolment" should {
              "render the Business Added page" when {
                "using the manage businesses journey" in {
                  disable(NavBarFs)
                  stubAuthorised(mtdUserRole)

                  await(sessionService.createSession(IncomeSourceJourneyType(Add, incomeSourceType)))

                  val (incomeSourceId, journeyType) = incomeSourceType match {
                    case SelfEmployment => (testSelfEmploymentId, "ADD-SE")
                    case UkProperty => (testPropertyIncomeId, "ADD-UK")
                    case _ => (testPropertyIncomeId, "ADD-FP")
                  }

                  await(sessionService.setMongoData(UIJourneySessionData(testSessionId, journeyType,
                    addIncomeSourceData = Some(AddIncomeSourceData(incomeSourceId = Some(incomeSourceId), dateStarted = Some(LocalDate.of(2024, 1, 1)))))))
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, getIncomeSourceDetailsResponse(incomeSourceType))
                  IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, testObligationsModel)

                  val result = buildGETMTDClient(path, additionalCookies).futureValue
                  IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

                  val expectedText: String = getExpectedPageTitle(incomeSourceType)

                  sessionService.getMongoKey(AddIncomeSourceData.incomeSourceCreatedJourneyCompleteField, IncomeSourceJourneyType(Add, incomeSourceType)).futureValue shouldBe Right(Some(true))

                  result should have(
                    httpStatus(OK),
                    pageTitle(mtdUserRole, expectedText),
                    elementTextByID("view-businesses-link")(viewBusinessesLinkText)
                  )
                }
              }

              if (incomeSourceType == UkProperty) {
                "render error page" when {
                  "UK property income source is missing trading start date" in {
                    disable(NavBarFs)
                    stubAuthorised(mtdUserRole)
                    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse.copy(properties = List(ukProperty.copy(tradingStartDate = None))))

                    await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-UK",
                      addIncomeSourceData = Some(AddIncomeSourceData(incomeSourceId = Some(testPropertyIncomeId))))))

                    val result = buildGETMTDClient(path, additionalCookies).futureValue

                    result should have(
                      httpStatus(INTERNAL_SERVER_ERROR),
                      pageTitle(mtdUserRole, "standardError.heading", isErrorPage = true)
                    )
                  }
                }
              }
              testAuthFailures(path, mtdUserRole)
            }
          }
        }
      }
    }
  }
}
