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

package controllers.agent.incomeSources.add

import audit.models.CreateIncomeSourceAuditModel
import auth.MtdItUser
import controllers.agent.ControllerISpecHelper
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, JourneyType}
import enums.{MTDPrimaryAgent, MTDSupportingAgent}
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub, MTDIndividualAuthStub}
import models.admin.{IncomeSources, NavBarFs}
import models.createIncomeSource.{CreateIncomeSourceErrorResponse, CreateIncomeSourceResponse}
import models.incomeSourceDetails.UIJourneySessionData
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants.{getAgentClientDetailsForCookie, testMtditid, testNino, testSelfEmploymentId, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.{emptyUIJourneySessionData, multipleBusinessesAndPropertyResponse, noPropertyOrBusinessResponse}
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

class IncomeSourceCheckDetailsControllerISpec extends ControllerISpecHelper {

  import helpers.ManageBusinessesIncomeSourceCheckDetailsConstants._

  def errorPageUrl(incomeSourceType: IncomeSourceType): String = controllers.incomeSources.add.routes.IncomeSourceNotAddedController.showAgent(incomeSourceType).url

  val continueButtonText: String = messagesAPI("base.confirm-and-continue")

  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, None, multipleBusinessesAndPropertyResponse,
    None, Some("1234567890"), Some("12345-credId"), Some(Individual), None
  )(FakeRequest())

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Add))
  }

  val pathSE = "/agents/income-sources/add/business-check-details"
  val pathUKProperty = "/agents/income-sources/add/uk-property-check-details"
  val pathForeignProperty = "/agents/income-sources/add/foreign-property-check-details"

  val incomeSourceTypeWithPath = Map(
    SelfEmployment -> pathSE,
    UkProperty -> pathUKProperty,
    ForeignProperty -> pathForeignProperty
  )

  def testUIJourneySessionData(incomeSourceType: IncomeSourceType): UIJourneySessionData = UIJourneySessionData(
    sessionId = testSessionId,
    journeyType = JourneyType(Add, incomeSourceType).toString,
    addIncomeSourceData = Some(if (incomeSourceType == SelfEmployment) testAddBusinessData else testAddPropertyData))

  List(MTDPrimaryAgent, MTDSupportingAgent).foreach { mtdUserRole =>
    val isSupportingAgent = mtdUserRole == MTDSupportingAgent
    val additionalCookies = getAgentClientDetailsForCookie(isSupportingAgent, true)
    incomeSourceTypeWithPath.foreach { case (incomeSourceType, path) =>
      s"GET $path" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid agent and client delegated enrolment" should {
            "render the Check Business details page with accounting method" when {
              "the user has no existing businesses" in {
                enable(IncomeSources)
                disable(NavBarFs)
                MTDIndividualAuthStub.stubAuthorised()
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

                val response = List(CreateIncomeSourceResponse(testSelfEmploymentId))
                IncomeTaxViewChangeStub.stubCreateBusinessDetailsResponse(testMtditid)(OK, response)

                await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))

                val result = buildGETMTDClient(path, additionalCookies)

                incomeSourceType match {
                  case SelfEmployment =>
                    result should have(
                      httpStatus(OK),
                      pageTitleIndividual("check-business-details.title"),
                      elementTextBySelectorList(".govuk-summary-list__value", "dd:nth-of-type(1)")(testBusinessName),
                      elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(2) dd:nth-of-type(1)")("1 January 2023"),
                      elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(3) dd:nth-of-type(1)")(testBusinessTrade),
                      elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(4) dd:nth-of-type(1)")(testBusinessAddressLine1 + " " + testBusinessPostCode + " " + testBusinessCountryCode),
                      elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(5) dd:nth-of-type(1)")(testBusinessAccountingMethodView),
                      elementTextByID("confirm-button")(continueButtonText)
                    )

                  case UkProperty =>
                    result should have(
                      httpStatus(OK),
                      pageTitleIndividual("incomeSources.add.checkUKPropertyDetails.title"),
                      elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(1) dd:nth-of-type(1)")("1 January 2023"),
                      elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(2) dd:nth-of-type(1)")(testBusinessAccountingMethodView),
                      elementTextByID("confirm-button")(continueButtonText)
                    )

                  case ForeignProperty =>
                    result should have(
                      httpStatus(OK),
                      pageTitleIndividual("incomeSources.add.foreign-property-check-details.title"),
                      elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(1) dd:nth-of-type(1)")("1 January 2023"),
                      elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(2) dd:nth-of-type(1)")(testBusinessAccountingMethodView),
                      elementTextByID("confirm-button")(continueButtonText)
                    )
                }
              }
            }
          }
          testAuthFailuresForMTDAgent(path, isSupportingAgent)
        }
      }

      s"POST $path" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid agent and client delegated enrolment" should {
            "redirect to IncomeSourceReportingMethodController" when {
              "user selects 'confirm and continue'" in {
                enable(IncomeSources)
                disable(NavBarFs)
                MTDIndividualAuthStub.stubAuthorised()

                val response = List(CreateIncomeSourceResponse(testSelfEmploymentId))
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)
                IncomeTaxViewChangeStub.stubCreateBusinessDetailsResponse(testMtditid)(OK, response)
                await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))

                val result = buildPOSTMTDPostClient(path, additionalCookies, Map.empty)

                incomeSourceType match {
                  case SelfEmployment => AuditStub.verifyAuditContainsDetail(
                    CreateIncomeSourceAuditModel(SelfEmployment, testSEViewModel, None, None, Some(CreateIncomeSourceResponse(testSelfEmploymentId)))(testUser).detail)

                  case UkProperty => AuditStub.verifyAuditContainsDetail(
                    CreateIncomeSourceAuditModel(UkProperty, testUKPropertyViewModel, None, None, Some(CreateIncomeSourceResponse(testSelfEmploymentId)))(testUser).detail)

                  case ForeignProperty => AuditStub.verifyAuditContainsDetail(
                    CreateIncomeSourceAuditModel(ForeignProperty, testForeignPropertyViewModel, None, None, Some(CreateIncomeSourceResponse(testSelfEmploymentId)))(testUser).detail)
                }


                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(controllers.incomeSources.add.routes.IncomeSourceReportingMethodController.show(isAgent = true, incomeSourceType).url)
                )
              }
            }
            s"render the error page" when {
              "error in response from API" in {
                enable(IncomeSources)
                disable(NavBarFs)
                MTDIndividualAuthStub.stubAuthorised()
                val response = List(CreateIncomeSourceErrorResponse(500, "INTERNAL_SERVER_ERROR"))
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)
                IncomeTaxViewChangeStub.stubCreateBusinessDetailsErrorResponseNew(testMtditid)(response)
                await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))

                val result = buildPOSTMTDPostClient(path, additionalCookies, Map.empty)

                incomeSourceType match {
                  case SelfEmployment => AuditStub.verifyAuditContainsDetail(
                    CreateIncomeSourceAuditModel(SelfEmployment, testSEViewModel, Some("API_FAILURE"), Some(testErrorReason), None)(testUser).detail)

                  case UkProperty => AuditStub.verifyAuditContainsDetail(
                    CreateIncomeSourceAuditModel(UkProperty, testUKPropertyViewModel, Some("API_FAILURE"), Some(testErrorReason), None)(testUser).detail)

                  case ForeignProperty => AuditStub.verifyAuditContainsDetail(
                    CreateIncomeSourceAuditModel(ForeignProperty, testForeignPropertyViewModel, Some("API_FAILURE"), Some(testErrorReason), None)(testUser).detail)
                }

                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(errorPageUrl(incomeSourceType))
                )
              }

              "user session has no details" in {
                enable(IncomeSources)
                disable(NavBarFs)
                MTDIndividualAuthStub.stubAuthorised()
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

                await(sessionService.setMongoData(emptyUIJourneySessionData(JourneyType(Add, incomeSourceType))))

                val result = buildPOSTMTDPostClient(path, additionalCookies, Map.empty)

                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(errorPageUrl(incomeSourceType))
                )
              }
            }
          }
          testAuthFailuresForMTDAgent(path, isSupportingAgent)
        }
      }
    }
  }
}
