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

import audit.models.CreateIncomeSourceAuditModel
import controllers.ControllerISpecHelper
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub}
import models.UIJourneySessionData
import models.admin.{AccountingMethodJourney, NavBarFs}
import models.createIncomeSource.{CreateIncomeSourceErrorResponse, CreateIncomeSourceResponse}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants.{testMtditid, testSelfEmploymentId, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.{emptyUIJourneySessionData, multipleBusinessesAndPropertyResponse, noPropertyOrBusinessResponse}

class IncomeSourceCheckDetailsControllerISpec extends ControllerISpecHelper {

  import helpers.IncomeSourceCheckDetailsConstants._

  def errorPageUrl(incomeSourceType: IncomeSourceType, mtdUserRole: MTDUserRole): String = {
    if(mtdUserRole == MTDIndividual) {
      controllers.manageBusinesses.add.routes.IncomeSourceNotAddedController.show(incomeSourceType).url
    } else {
      controllers.manageBusinesses.add.routes.IncomeSourceNotAddedController.showAgent(incomeSourceType).url
    }
  }

  val continueButtonText: String = "Confirm and continue"
  val descriptionText: String = "Once you confirm these details, you will not be able to amend them in the next step and will need to contact HMRC to do so."

  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Add))
  }

  def testUIJourneySessionData(incomeSourceType: IncomeSourceType): UIJourneySessionData = UIJourneySessionData(
    sessionId = testSessionId,
    journeyType = IncomeSourceJourneyType(Add, incomeSourceType).toString,
    addIncomeSourceData = Some(if (incomeSourceType == SelfEmployment) testAddBusinessData else testAddPropertyData))

  def getPath(mtdRole: MTDUserRole, incomeSourceType: IncomeSourceType): String = {
    val pathStart = if(mtdRole == MTDIndividual) "/manage-your-businesses" else "/agents/manage-your-businesses"
    incomeSourceType match {
      case SelfEmployment => s"$pathStart/add-sole-trader/business-check-answers"
      case UkProperty => s"$pathStart/add-uk-property/check-answers"
      case ForeignProperty => s"$pathStart/add-foreign-property/check-answers"
    }
  }

  mtdAllRoles.foreach { case mtdUserRole =>
    List(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSourceType =>
      val path = getPath(mtdUserRole, incomeSourceType)
      val additionalCookies = getAdditionalCookies(mtdUserRole)
      s"GET $path" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid enrolment" should {
            "render the Check Business details page with accounting method" when {
              "the user has no existing businesses" in {
                enable(AccountingMethodJourney)
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

                val response = List(CreateIncomeSourceResponse(testSelfEmploymentId))
                IncomeTaxViewChangeStub.stubCreateBusinessDetailsResponse(testMtditid)(OK, response)

                await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))

                val result = buildGETMTDClient(path, additionalCookies).futureValue

                incomeSourceType match {
                  case SelfEmployment =>
                    result should have(
                      httpStatus(OK),
                      pageTitle(mtdUserRole, "check-details.title"),
                      elementTextBySelectorList(".govuk-summary-list__value", "dd:nth-of-type(1)")(testBusinessName),
                      elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(2) dd:nth-of-type(1)")("1 January 2023"),
                      elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(3) dd:nth-of-type(1)")(testBusinessTrade),
                      elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(4) dd:nth-of-type(1)")(testBusinessAddressLine1 + " " + testBusinessPostCode + " " + testBusinessCountryCode),
                      elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(5) dd:nth-of-type(1)")(testBusinessAccountingMethodView),
                      elementTextByID("check-details-description")(descriptionText),
                      elementTextByID("confirm-button")(continueButtonText)
                    )

                  case UkProperty =>
                    result should have(
                      httpStatus(OK),
                      pageTitle(mtdUserRole,"check-details-uk.title"),
                      elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(1) dd:nth-of-type(1)")("1 January 2023"),
                      elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(2) dd:nth-of-type(1)")(testBusinessAccountingMethodView),
                      elementTextByID("check-details-description")(descriptionText),
                      elementTextByID("confirm-button")(continueButtonText)
                    )

                  case _ =>
                    result should have(
                      httpStatus(OK),
                      pageTitle(mtdUserRole, "check-details-fp.title"),
                      elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(1) dd:nth-of-type(1)")("1 January 2023"),
                      elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(2) dd:nth-of-type(1)")(testBusinessAccountingMethodView),
                      elementTextByID("check-details-description")(descriptionText),
                      elementTextByID("confirm-button")(continueButtonText)
                    )
                }
              }
            }
          }
          testAuthFailures(path, mtdUserRole)
        }
      }

      s"POST $path" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid enrolment" should {
            "redirect to IncomeSourceReportingFrequencyController" when {
              "user selects 'confirm and continue'" in {
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                val response = List(CreateIncomeSourceResponse(testSelfEmploymentId))
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)
                IncomeTaxViewChangeStub.stubCreateBusinessDetailsResponse(testMtditid)(OK, response)
                await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))
                val result = buildPOSTMTDPostClient(path, additionalCookies, Map.empty).futureValue

                incomeSourceType match {
                  case SelfEmployment => AuditStub.verifyAuditContainsDetail(
                    CreateIncomeSourceAuditModel(SelfEmployment, testSEViewModel, None, None, Some(CreateIncomeSourceResponse(testSelfEmploymentId)))
                    (getTestUser(mtdUserRole, multipleBusinessesAndPropertyResponse)).detail)

                  case UkProperty => AuditStub.verifyAuditContainsDetail(
                    CreateIncomeSourceAuditModel(UkProperty, testUKPropertyViewModel, None, None, Some(CreateIncomeSourceResponse(testSelfEmploymentId)))
                    (getTestUser(mtdUserRole, multipleBusinessesAndPropertyResponse)).detail)

                  case ForeignProperty => AuditStub.verifyAuditContainsDetail(
                    CreateIncomeSourceAuditModel(ForeignProperty, testForeignPropertyViewModel, None, None, Some(CreateIncomeSourceResponse(testSelfEmploymentId)))
                    (getTestUser(mtdUserRole, multipleBusinessesAndPropertyResponse)).detail)
                }

                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(controllers.manageBusinesses.add.routes.IncomeSourceReportingFrequencyController.show(isAgent = mtdUserRole != MTDIndividual, false, incomeSourceType).url)
                )
              }
            }

            "render the error page" when {
              "error in response from API" in {
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                val response = List(CreateIncomeSourceErrorResponse(500, "INTERNAL_SERVER_ERROR"))
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)
                IncomeTaxViewChangeStub.stubCreateBusinessDetailsErrorResponseNew(testMtditid)(response)
                await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))

                val result = buildPOSTMTDPostClient(path, additionalCookies, Map.empty).futureValue

                incomeSourceType match {
                  case SelfEmployment => AuditStub.verifyAuditContainsDetail(
                    CreateIncomeSourceAuditModel(SelfEmployment, testSEViewModel, Some("API_FAILURE"), Some(testErrorReason), None)
                    (getTestUser(mtdUserRole, multipleBusinessesAndPropertyResponse)).detail)

                  case UkProperty => AuditStub.verifyAuditContainsDetail(
                    CreateIncomeSourceAuditModel(UkProperty, testUKPropertyViewModel, Some("API_FAILURE"), Some(testErrorReason), None)
                    (getTestUser(mtdUserRole, multipleBusinessesAndPropertyResponse)).detail)

                  case ForeignProperty => AuditStub.verifyAuditContainsDetail(
                    CreateIncomeSourceAuditModel(ForeignProperty, testForeignPropertyViewModel, Some("API_FAILURE"), Some(testErrorReason), None)
                    (getTestUser(mtdUserRole, multipleBusinessesAndPropertyResponse)).detail)
                }

                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(errorPageUrl(incomeSourceType, mtdUserRole))
                )
              }

              "user session has no details" in {
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

                await(sessionService.setMongoData(emptyUIJourneySessionData(IncomeSourceJourneyType(Add, incomeSourceType))))

                val result = buildPOSTMTDPostClient(path, additionalCookies, Map.empty).futureValue

                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(errorPageUrl(incomeSourceType, mtdUserRole))
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