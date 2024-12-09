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

package controllers.incomeSources.cease

import audit.models.CeaseIncomeSourceAuditModel
import auth.MtdItUser
import controllers.ControllerISpecHelper
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.Cease
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub}
import models.admin.{IncomeSourcesFs, NavBarFs}
import models.core.IncomeSourceId.mkIncomeSourceId
import models.incomeSourceDetails.{CeaseIncomeSourceData, IncomeSourceDetailsModel, UIJourneySessionData}
import models.updateIncomeSource.UpdateIncomeSourceResponseModel
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.SessionService
import testConstants.BaseIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants._
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}

import java.time.LocalDate

class CheckCeaseIncomeSourceDetailsControllerISpec extends ControllerISpecHelper {

  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  val testLongEndDate2022: String = "10 October 2022"
  val changeLink = "Change"
  val testBusinessName = "business"
  val timestamp = "2023-01-31T09:26:17Z"

  def getExpectedFormAction(mtdUserRole: MTDUserRole, incomeSourceType: IncomeSourceType) = {
    if(mtdUserRole == MTDIndividual) {
      controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.submit(incomeSourceType).url
    } else {
      controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.submitAgent(incomeSourceType).url
    }
  }

  def getExpectedRedirectUri(mtdUserRole: MTDUserRole, incomeSourceType: IncomeSourceType) = {
    if(mtdUserRole == MTDIndividual) {
      controllers.incomeSources.cease.routes.IncomeSourceCeasedObligationsController.show(incomeSourceType).url
    } else {
      controllers.incomeSources.cease.routes.IncomeSourceCeasedObligationsController.showAgent(incomeSourceType).url
    }
  }

  val businessAddressLabel = messagesAPI("incomeSources.ceaseBusiness.checkDetails.businessAddress")
  val pageTitleMsgKey = messagesAPI("incomeSources.ceaseBusiness.checkDetails.heading")
  val unknown: String = messagesAPI("incomeSources.ceaseBusiness.checkDetails.unknown")

  val pageTitleMsgKeyUK = messagesAPI("incomeSources.ceaseUKProperty.checkDetails.heading")

  def testUser(mtdUserRole: MTDUserRole, incomeSourceDetailsModel: IncomeSourceDetailsModel): MtdItUser[_] = {
    val (affinityGroup, arn) = if(mtdUserRole == MTDIndividual) {
      (Individual, None)
    } else {
      (Agent, Some("1"))
    }
    MtdItUser(
      testMtditid, testNino, None, incomeSourceDetailsModel,
      None, Some("1234567890"), Some("12345-credId"), Some(affinityGroup), arn
    )(FakeRequest())
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Cease))
  }

  def getIncomeSourceDetailsResponse(incomeSourceType: IncomeSourceType) = {
    incomeSourceType match {
      case SelfEmployment => businessOnlyResponse
      case UkProperty => ukPropertyOnlyResponse
      case ForeignProperty => foreignPropertyOnlyResponse
    }
  }

  def getPath(mtdRole: MTDUserRole, incomeSourceType: IncomeSourceType): String = {
    val pathStart = if(mtdRole == MTDIndividual) "/income-sources/cease" else "/agents/income-sources/cease"
    incomeSourceType match {
      case SelfEmployment => s"$pathStart/business-check-details"
      case UkProperty => s"$pathStart/uk-property-check-details"
      case ForeignProperty => s"$pathStart/foreign-property-check-details"
    }
  }

  mtdAllRoles.foreach { mtdUserRole =>
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    val pathStart = if (mtdUserRole == MTDIndividual) "" else "/agents"
    val selfEmploymentPath = s"$pathStart/income-sources/cease/business-check-details"
    val ukPropertyPath = s"$pathStart/income-sources/cease/uk-property-check-details"
    val foreignPropertyPath = s"$pathStart/income-sources/cease/foreign-property-check-details"
    s"calling GET ${selfEmploymentPath}" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "render the Cease Business Details Page" when {
            "income source is enabled" in {
              enable(IncomeSourcesFs)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

              await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-SE", ceaseIncomeSourceData =
                Some(CeaseIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = None, journeyIsComplete = Some(false))))))

              When(s"I call GET ${selfEmploymentPath}")
              val result = buildGETMTDClient(selfEmploymentPath, additionalCookies).futureValue
              verifyIncomeSourceDetailsCall(testMtditid)

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, pageTitleMsgKey),
                elementTextBySelectorList(".govuk-summary-list__key", "dt:nth-of-type(1)")(messagesAPI("incomeSources.ceaseBusiness.checkDetails.dateStopped")),
                elementTextBySelectorList(".govuk-summary-list__value", "dd:nth-of-type(1)")(testLongEndDate2022),
                elementTextByID("change")(changeLink),

                elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(2) dt:nth-of-type(1)")(messagesAPI("incomeSources.ceaseBusiness.checkDetails.businessName")),
                elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(2) dd:nth-of-type(1)")(testBusinessName),
                elementAttributeBySelector("form", "action")(getExpectedFormAction(mtdUserRole, SelfEmployment))
              )
            }
          }
          "render the Cease Business Page with unknown address and title and trade" when {
            "income source is enabled" in {
              enable(IncomeSourcesFs)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponseWithUnknownAddressName)

              await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-SE", ceaseIncomeSourceData =
                Some(CeaseIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = None, journeyIsComplete = Some(false))))))

              When(s"I call GET ${selfEmploymentPath}")
              val result = buildGETMTDClient(selfEmploymentPath, additionalCookies).futureValue
              verifyIncomeSourceDetailsCall(testMtditid)

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, pageTitleMsgKey),
                elementTextBySelectorList(".govuk-summary-list__key", "dt:nth-of-type(1)")(messagesAPI("incomeSources.ceaseBusiness.checkDetails.dateStopped")),
                elementTextBySelectorList(".govuk-summary-list__value", "dd:nth-of-type(1)")(testLongEndDate2022),
                elementTextByID("change")(changeLink),

                elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(2) dt:nth-of-type(1)")(messagesAPI("incomeSources.ceaseBusiness.checkDetails.businessName")),
                elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(2) dd:nth-of-type(1)")(unknown),

                elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(3) dt:nth-of-type(1)")(businessAddressLabel),
                elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(3) dd:nth-of-type(1)")(unknown),
                elementAttributeBySelector("form", "action")(getExpectedFormAction(mtdUserRole, SelfEmployment))
              )
            }
          }
          "redirect to Home Page" when {
            "Income source is disabled" in {
              disable(IncomeSourcesFs)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                status = OK,
                response = businessAndPropertyResponse
              )

              await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-SE", ceaseIncomeSourceData =
                Some(CeaseIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = None, journeyIsComplete = Some(false))))))

              When(s"I call GET ${selfEmploymentPath}")
              val result = buildGETMTDClient(selfEmploymentPath, additionalCookies).futureValue
              verifyIncomeSourceDetailsCall(testMtditid)

              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(homeUrl(mtdUserRole))
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
            "income source is enabled" in {
              enable(IncomeSourcesFs)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

              await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-UK", ceaseIncomeSourceData =
                Some(CeaseIncomeSourceData(incomeSourceId = None, endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))

              When(s"I call GET ${ukPropertyPath}")
              val result = buildGETMTDClient(ukPropertyPath, additionalCookies).futureValue
              verifyIncomeSourceDetailsCall(testMtditid)

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, pageTitleMsgKeyUK),
                elementTextBySelectorList(".govuk-summary-list__key", "dt:nth-of-type(1)")(messagesAPI("incomeSources.ceaseUKProperty.checkDetails.dateStopped")),
                elementTextBySelectorList(".govuk-summary-list__value", "dd:nth-of-type(1)")(testLongEndDate2022),
                elementTextByID("change")(changeLink),
                elementAttributeBySelector("form", "action")(getExpectedFormAction(mtdUserRole, UkProperty))
              )
            }
          }
          "redirect to Home Page" when {
            "Income source is disabled" in {
              disable(IncomeSourcesFs)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

              await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-UK", ceaseIncomeSourceData =
                Some(CeaseIncomeSourceData(incomeSourceId = None, endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))


              When(s"I call GET $ukPropertyPath")
              val result = buildGETMTDClient(ukPropertyPath, additionalCookies).futureValue
              verifyIncomeSourceDetailsCall(testMtditid)

              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(homeUrl(mtdUserRole))
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
            "income sources is enabled" in {
              enable(IncomeSourcesFs)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

              await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-FP", ceaseIncomeSourceData =
                Some(CeaseIncomeSourceData(incomeSourceId = None, endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))

              When(s"I call GET ${foreignPropertyPath}")
              val result = buildGETMTDClient(foreignPropertyPath, additionalCookies).futureValue
              verifyIncomeSourceDetailsCall(testMtditid)

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, pageTitleMsgKeyUK),
                elementTextBySelectorList(".govuk-summary-list__key", "dt:nth-of-type(1)")(messagesAPI("incomeSources.ceaseForeignProperty.checkDetails.dateStopped")),
                elementTextBySelectorList(".govuk-summary-list__value", "dd:nth-of-type(1)")(testLongEndDate2022),
                elementTextByID("change")(changeLink),
                elementAttributeBySelector("form", "action")(getExpectedFormAction(mtdUserRole, ForeignProperty))
              )
            }
          }
          "redirect to Home Page" when {
            "Income source is disabled" in {
              disable(IncomeSourcesFs)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

              await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-FP", ceaseIncomeSourceData =
                Some(CeaseIncomeSourceData(incomeSourceId = None, endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))

              When(s"I call GET $foreignPropertyPath")
              val result = buildGETMTDClient(foreignPropertyPath, additionalCookies).futureValue
              verifyIncomeSourceDetailsCall(testMtditid)

              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(homeUrl(mtdUserRole))
              )
            }
          }
          testAuthFailures(foreignPropertyPath, mtdUserRole)
        }
      }
    }

    s"calling POST ${selfEmploymentPath}" when {
      val redirectUrl = getExpectedRedirectUri(mtdUserRole, SelfEmployment)
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          s"redirect to $redirectUrl" when {
            "User is authorised" in {
              enable(IncomeSourcesFs)
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

              AuditStub.verifyAuditContainsDetail(CeaseIncomeSourceAuditModel(SelfEmployment, testEndDate2022, mkIncomeSourceId(testSelfEmploymentId), None)(testUser(mtdUserRole, businessOnlyResponse), hc).detail)
            }
          }
          testAuthFailures(selfEmploymentPath, mtdUserRole, optBody = Some(Map.empty))
        }
      }
    }

    s"calling POST ${ukPropertyPath}" when {
      val redirectUrl = getExpectedRedirectUri(mtdUserRole, UkProperty)
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          s"redirect to $redirectUrl" when {
            "User is authorised" in {
              enable(IncomeSourcesFs)
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

              AuditStub.verifyAuditContainsDetail(CeaseIncomeSourceAuditModel(UkProperty, testEndDate2022, mkIncomeSourceId(testPropertyIncomeId), None)(testUser(mtdUserRole, ukPropertyOnlyResponse), hc).detail)
            }
          }
          testAuthFailures(ukPropertyPath, mtdUserRole, optBody = Some(Map.empty))
        }
      }
    }

    s"calling POST ${foreignPropertyPath}" when {
      val redirectUrl = getExpectedRedirectUri(mtdUserRole, ForeignProperty)
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          s"redirect to $redirectUrl" in {
              enable(IncomeSourcesFs)
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
              AuditStub.verifyAuditContainsDetail(CeaseIncomeSourceAuditModel(ForeignProperty, testEndDate2022, mkIncomeSourceId(testPropertyIncomeId), None)(testUser(mtdUserRole, foreignPropertyOnlyResponse), hc).detail)
            }
          }
          testAuthFailures(foreignPropertyPath, mtdUserRole, optBody = Some(Map.empty))
        }
      }
    }
}
