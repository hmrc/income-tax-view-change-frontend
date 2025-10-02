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

package controllers.manageBusinesses.cease

import audit.models.CeaseIncomeSourceAuditModel
import controllers.ControllerISpecHelper
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.Cease
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub}
import models.UIJourneySessionData
import models.admin.NavBarFs
import models.core.IncomeSourceId.mkIncomeSourceId
import models.incomeSourceDetails.CeaseIncomeSourceData
import models.updateIncomeSource.UpdateIncomeSourceResponseModel
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.test.Helpers._
import services.SessionService
import testConstants.BaseIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants._

import java.time.LocalDate

class CheckCeaseIncomeSourceDetailsControllerISpec extends ControllerISpecHelper {

  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  val testLongEndDate2022: String = "10 October 2022"
  val changeLink = "Change"
  val testBusinessName = "business"
  val timestamp = "2023-01-31T09:26:17Z"
  val businessAddressAsString = "8 Test New Court New Town New City NE12 6CI United Kingdom"


  def getExpectedFormAction(mtdUserRole: MTDUserRole, incomeSourceType: IncomeSourceType) = {
    if (mtdUserRole == MTDIndividual) {
      controllers.manageBusinesses.cease.routes.CeaseCheckIncomeSourceDetailsController.submit(incomeSourceType).url
    } else {
      controllers.manageBusinesses.cease.routes.CeaseCheckIncomeSourceDetailsController.submitAgent(incomeSourceType).url
    }
  }

  def getExpectedRedirectUri(mtdUserRole: MTDUserRole, incomeSourceType: IncomeSourceType) = {
    if (mtdUserRole == MTDIndividual) {
      controllers.manageBusinesses.cease.routes.IncomeSourceCeasedObligationsController.show(incomeSourceType).url
    } else {
      controllers.manageBusinesses.cease.routes.IncomeSourceCeasedObligationsController.showAgent(incomeSourceType).url
    }
  }

  val businessAddressLabel = messagesAPI("cease-check-answers.address")
  val pageTitleMsgKey = messagesAPI("cease-check-answers.title")
  val unknown: String = messagesAPI("cease-check-answers.unknown")


  val pageTitleMsgKeyUK = messagesAPI("cease-check-answers.title")

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Cease))
  }

  mtdAllRoles.foreach { mtdUserRole =>
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    val pathStart = if (mtdUserRole == MTDIndividual) "" else "/agents"
    val selfEmploymentPath = s"$pathStart/manage-your-businesses/cease/business-check-answers"
    val ukPropertyPath = s"$pathStart/manage-your-businesses/cease/uk-property-check-answers"
    val foreignPropertyPath = s"$pathStart/manage-your-businesses/cease/foreign-property-check-answers"
    s"calling GET ${selfEmploymentPath}" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "render the Cease Business Details Page" when {
            "using the manage businesses journey" in {
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

              await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-SE", ceaseIncomeSourceData =
                Some(CeaseIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = None, journeyIsComplete = Some(false))))))

              When(s"I call GET ${selfEmploymentPath}")
              val result = buildGETMTDClient(selfEmploymentPath, additionalCookies).futureValue
              IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, pageTitleMsgKey),
                elementTextBySelectorList(".govuk-summary-list__key", "dt:nth-of-type(1)")(messagesAPI("cease-check-answers.cease-date")),
                elementTextBySelectorList(".govuk-summary-list__value", "dd:nth-of-type(1)")(testLongEndDate2022),
                elementTextByID("change")(changeLink),

                elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(2) dt:nth-of-type(1)")(messagesAPI("cease-check-answers.business-name")),
                elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(2) dd:nth-of-type(1)")(testBusinessName),

                elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(3) dt:nth-of-type(1)")(messagesAPI("cease-check-answers.trade")),
                elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(3) dd:nth-of-type(1)")(testIncomeSource),

                elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(4) dt:nth-of-type(1)")(businessAddressLabel),
                elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(4) dd:nth-of-type(1)")(businessAddressAsString),


                elementAttributeBySelector("form", "action")(getExpectedFormAction(mtdUserRole, SelfEmployment))
              )
            }
          }
          "render the Cease Business Page with unknown address and title and trade" when {
            "using the manage businesses journey" in {
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponseWithUnknownAddressName)

              await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-SE", ceaseIncomeSourceData =
                Some(CeaseIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = None, journeyIsComplete = Some(false))))))

              When(s"I call GET ${selfEmploymentPath}")
              val result = buildGETMTDClient(selfEmploymentPath, additionalCookies).futureValue
              IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, pageTitleMsgKey),
                elementTextBySelectorList(".govuk-summary-list__key", "dt:nth-of-type(1)")(messagesAPI("cease-check-answers.cease-date")),
                elementTextBySelectorList(".govuk-summary-list__value", "dd:nth-of-type(1)")(testLongEndDate2022),
                elementTextByID("change")(changeLink),

                elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(2) dt:nth-of-type(1)")(messagesAPI("cease-check-answers.business-name")),
                elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(2) dd:nth-of-type(1)")(unknown),

                elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(3) dt:nth-of-type(1)")(messagesAPI("cease-check-answers.trade")),
                elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(3) dd:nth-of-type(1)")(unknown),

                elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(4) dt:nth-of-type(1)")(businessAddressLabel),
                elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(4) dd:nth-of-type(1)")(unknown),
                elementAttributeBySelector("form", "action")(getExpectedFormAction(mtdUserRole, SelfEmployment))
              )
            }
          }
          testAuthFailures(selfEmploymentPath, mtdUserRole)
        }
      }
    }

    s"calling GET ${ukPropertyPath}" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "render the Cease UK Property Details Page" when {
            "using the manage businesses journey" in {
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

              await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-UK", ceaseIncomeSourceData =
                Some(CeaseIncomeSourceData(incomeSourceId = None, endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))

              When(s"I call GET ${ukPropertyPath}")
              val result = buildGETMTDClient(ukPropertyPath, additionalCookies).futureValue
              IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, pageTitleMsgKeyUK),
                elementTextBySelectorList(".govuk-summary-list__key", "dt:nth-of-type(1)")(messagesAPI("cease-check-answers.cease-date")),
                elementTextBySelectorList(".govuk-summary-list__value", "dd:nth-of-type(1)")(testLongEndDate2022),
                elementTextByID("change")(changeLink),
                elementAttributeBySelector("form", "action")(getExpectedFormAction(mtdUserRole, UkProperty))
              )
            }
          }
          testAuthFailures(ukPropertyPath, mtdUserRole)
        }
      }
    }

    s"calling GET ${foreignPropertyPath}" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "render the Cease Foreign Property Details Page" when {
            "using the manage businesses journey" in {
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

              await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-FP", ceaseIncomeSourceData =
                Some(CeaseIncomeSourceData(incomeSourceId = None, endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))

              When(s"I call GET ${foreignPropertyPath}")
              val result = buildGETMTDClient(foreignPropertyPath, additionalCookies).futureValue
              IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, pageTitleMsgKeyUK),
                elementTextBySelectorList(".govuk-summary-list__key", "dt:nth-of-type(1)")(messagesAPI("cease-check-answers.cease-date")),
                elementTextBySelectorList(".govuk-summary-list__value", "dd:nth-of-type(1)")(testLongEndDate2022),
                elementTextByID("change")(changeLink),
                elementAttributeBySelector("form", "action")(getExpectedFormAction(mtdUserRole, ForeignProperty))
              )
            }
          }
          testAuthFailures(foreignPropertyPath, mtdUserRole)
        }
      }
    }

    s"calling POST ${selfEmploymentPath}" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          val redirectUrl = getExpectedRedirectUri(mtdUserRole, SelfEmployment)
          s"redirect to $redirectUrl" in {
            disable(NavBarFs)
            stubAuthorised(mtdUserRole)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)
            IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

            await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-SE", ceaseIncomeSourceData =
              Some(CeaseIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = None, journeyIsComplete = Some(false))))))

            val result = buildPOSTMTDPostClient(selfEmploymentPath, additionalCookies, Map.empty).futureValue

            result should have(
              httpStatus(SEE_OTHER),
              redirectURI(redirectUrl),
            )

            AuditStub.verifyAuditContainsDetail(
              CeaseIncomeSourceAuditModel(SelfEmployment, testEndDate2022, mkIncomeSourceId(testSelfEmploymentId), None)
              (getTestUser(mtdUserRole, businessOnlyResponse)).detail)
          }
        }
        testAuthFailures(selfEmploymentPath, mtdUserRole, optBody = Some(Map.empty))
      }
    }

    s"calling POST ${ukPropertyPath}" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          val redirectUrl = getExpectedRedirectUri(mtdUserRole, UkProperty)
          s"redirect to $redirectUrl" in {
            disable(NavBarFs)
            stubAuthorised(mtdUserRole)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)
            IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

            await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-UK", ceaseIncomeSourceData =
              Some(CeaseIncomeSourceData(incomeSourceId = None, endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))

            val result = buildPOSTMTDPostClient(ukPropertyPath, additionalCookies, Map.empty).futureValue

            result should have(
              httpStatus(SEE_OTHER),
              redirectURI(redirectUrl),
            )

            AuditStub.verifyAuditContainsDetail(CeaseIncomeSourceAuditModel(
              UkProperty, testEndDate2022, mkIncomeSourceId(testPropertyIncomeId), None)
            (getTestUser(mtdUserRole, ukPropertyOnlyResponse)).detail)
          }
        }
        testAuthFailures(ukPropertyPath, mtdUserRole, optBody = Some(Map.empty))
      }
    }

    s"calling POST ${foreignPropertyPath}" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          val redirectUrl = getExpectedRedirectUri(mtdUserRole, ForeignProperty)
          s"redirect to $redirectUrl" in {
            disable(NavBarFs)
            stubAuthorised(mtdUserRole)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)
            IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

            await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-FP", ceaseIncomeSourceData =
              Some(CeaseIncomeSourceData(incomeSourceId = None, endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))

            val result = buildPOSTMTDPostClient(foreignPropertyPath, additionalCookies, Map.empty).futureValue

            result should have(
              httpStatus(SEE_OTHER),
              redirectURI(redirectUrl),
            )
            AuditStub.verifyAuditContainsDetail(CeaseIncomeSourceAuditModel(
              ForeignProperty, testEndDate2022, mkIncomeSourceId(testPropertyIncomeId), None)
            (getTestUser(mtdUserRole, foreignPropertyOnlyResponse)).detail)
          }
        }
        testAuthFailures(foreignPropertyPath, mtdUserRole, optBody = Some(Map.empty))
      }
    }
  }
}
