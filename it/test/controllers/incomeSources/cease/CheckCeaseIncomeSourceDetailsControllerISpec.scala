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
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import enums.JourneyType.Cease
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub, MTDIndividualAuthStub}
import models.admin.{IncomeSources, NavBarFs}
import models.core.IncomeSourceId.mkIncomeSourceId
import models.incomeSourceDetails.{CeaseIncomeSourceData, UIJourneySessionData}
import models.updateIncomeSource.UpdateIncomeSourceResponseModel
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.SessionService
import testConstants.BaseIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants._
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

import java.time.LocalDate

class CheckCeaseIncomeSourceDetailsControllerISpec extends ControllerISpecHelper {

  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  val testLongEndDate2022: String = "10 October 2022"
  val changeLink = "Change"
  val testBusinessName = "business"
  val timestamp = "2023-01-31T09:26:17Z"

  val selfEmploymentPath = "/income-sources/cease/business-check-details"
  val ukPropertyPath = "/income-sources/cease/uk-property-check-details"
  val foreignPropertyPath = "/income-sources/cease/foreign-property-check-details"

  val formActionSE = controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.submit(SelfEmployment).url

  val businessAddressLabel = messagesAPI("incomeSources.ceaseBusiness.checkDetails.businessAddress")
  val pageTitleMsgKey = messagesAPI("incomeSources.ceaseBusiness.checkDetails.heading")
  val unknown: String = messagesAPI("incomeSources.ceaseBusiness.checkDetails.unknown")

  val redirectUriSE = controllers.incomeSources.cease.routes.IncomeSourceCeasedObligationsController.show(SelfEmployment).url

  val formActionUK = controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.submit(UkProperty).url
  val pageTitleMsgKeyUK = messagesAPI("incomeSources.ceaseUKProperty.checkDetails.heading")
  val redirectUriUK = controllers.incomeSources.cease.routes.IncomeSourceCeasedObligationsController.show(UkProperty).url

  val formActionFP = controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.submit(ForeignProperty).url
  val redirectUriFP = controllers.incomeSources.cease.routes.IncomeSourceCeasedObligationsController.show(ForeignProperty).url

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, None, multipleBusinessesAndPropertyResponse,
    None, Some("1234567890"), Some("12345-credId"), Some(Individual), None
  )(FakeRequest())

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Cease))
  }

  s"GET $selfEmploymentPath" when {
    "the user is authenticated, with a valid MTD enrolment" should {
      "render the Cease Business Page" when {
        "Income source is enabled" in {
          enable(IncomeSources)
          disable(NavBarFs)
          MTDIndividualAuthStub.stubAuthorised()
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
            status = OK,
            response = businessOnlyResponse
          )

          await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-SE", ceaseIncomeSourceData =
            Some(CeaseIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = None, journeyIsComplete = Some(false))))))

          When(s"I call GET $selfEmploymentPath")
          val result = buildGETMTDClient(selfEmploymentPath).futureValue
          verifyIncomeSourceDetailsCall(testMtditid)

          result should have(
            httpStatus(OK),
            pageTitleIndividual(pageTitleMsgKey),
            elementTextBySelectorList(".govuk-summary-list__key", "dt:nth-of-type(1)")(messagesAPI("incomeSources.ceaseBusiness.checkDetails.dateStopped")),
            elementTextBySelectorList(".govuk-summary-list__value", "dd:nth-of-type(1)")(testLongEndDate2022),
            elementTextByID("change")(changeLink),

            elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(2) dt:nth-of-type(1)")(messagesAPI("incomeSources.ceaseBusiness.checkDetails.businessName")),
            elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(2) dd:nth-of-type(1)")(testBusinessName),
            elementAttributeBySelector("form", "action")(formActionSE)
          )
        }
      }
      "render the Cease Business Page with unknown address and title" when {
        "Income source is enabled" in {
          enable(IncomeSources)
          disable(NavBarFs)
          MTDIndividualAuthStub.stubAuthorised()
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponseWithUnknownAddressName)

          await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-SE", ceaseIncomeSourceData =
            Some(CeaseIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = None, journeyIsComplete = Some(false))))))

          When(s"I call GET ${selfEmploymentPath}")
          val result = buildGETMTDClient(selfEmploymentPath).futureValue
          verifyIncomeSourceDetailsCall(testMtditid)

          result should have(
            httpStatus(OK),
            pageTitleIndividual(pageTitleMsgKey),
            elementTextBySelectorList(".govuk-summary-list__key", "dt:nth-of-type(1)")(messagesAPI("incomeSources.ceaseBusiness.checkDetails.dateStopped")),
            elementTextBySelectorList(".govuk-summary-list__value", "dd:nth-of-type(1)")(testLongEndDate2022),
            elementTextByID("change")(changeLink),

            elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(2) dt:nth-of-type(1)")(messagesAPI("incomeSources.ceaseBusiness.checkDetails.businessName")),
            elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(2) dd:nth-of-type(1)")(unknown),

            elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(3) dt:nth-of-type(1)")(businessAddressLabel),
            elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(3) dd:nth-of-type(1)")(unknown),
            elementAttributeBySelector("form", "action")(formActionSE)
          )
        }
      }
      "redirect to Home Page" when {
        "Income source is disabled" in {
          disable(IncomeSources)
          disable(NavBarFs)
          MTDIndividualAuthStub.stubAuthorised()
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
            status = OK,
            response = businessAndPropertyResponse
          )

          await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-SE", ceaseIncomeSourceData =
            Some(CeaseIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = None, journeyIsComplete = Some(false))))))

          When(s"I call GET ${selfEmploymentPath}")
          val result = buildGETMTDClient(selfEmploymentPath).futureValue
          verifyIncomeSourceDetailsCall(testMtditid)

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.routes.HomeController.show().url)
          )
        }
      }
    }
    testAuthFailuresForMTDIndividual(selfEmploymentPath)
  }

  s"GET $ukPropertyPath" when {
    "the user is authenticated, with a valid MTD enrolment" should {
      "render the Cease Business Page" when {
        "Income source is enabled" in {
          enable(IncomeSources)
          disable(NavBarFs)
          MTDIndividualAuthStub.stubAuthorised()
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

          await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-UK", ceaseIncomeSourceData =
            Some(CeaseIncomeSourceData(incomeSourceId = None, endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))

          When(s"I call GET $ukPropertyPath")
          val result = buildGETMTDClient(ukPropertyPath).futureValue
          verifyIncomeSourceDetailsCall(testMtditid)

          result should have(
            httpStatus(OK),
            pageTitleIndividual(pageTitleMsgKeyUK),
            elementTextBySelectorList(".govuk-summary-list__key", "dt:nth-of-type(1)")(messagesAPI("incomeSources.ceaseUKProperty.checkDetails.dateStopped")),
            elementTextBySelectorList(".govuk-summary-list__value", "dd:nth-of-type(1)")(testLongEndDate2022),
            elementTextByID("change")(changeLink),
            elementAttributeBySelector("form", "action")(formActionUK)
          )
        }
      }
      "redirect to Home Page" when {
        "Income source is disabled" in {
          disable(IncomeSources)
          disable(NavBarFs)
          MTDIndividualAuthStub.stubAuthorised()
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

          await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-UK", ceaseIncomeSourceData =
            Some(CeaseIncomeSourceData(incomeSourceId = None, endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))


          When(s"I call GET $ukPropertyPath")
          val result = buildGETMTDClient(ukPropertyPath).futureValue
          verifyIncomeSourceDetailsCall(testMtditid)

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.routes.HomeController.show().url)
          )
        }
      }
    }
    testAuthFailuresForMTDIndividual(ukPropertyPath)
  }

  s"GET $foreignPropertyPath" when {
    "the user is authenticated, with a valid MTD enrolment" should {
      "render the Cease Business Page" when {
        "Income source is enabled" in {
          enable(IncomeSources)
          disable(NavBarFs)
          MTDIndividualAuthStub.stubAuthorised()
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

          await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-FP", ceaseIncomeSourceData =
            Some(CeaseIncomeSourceData(incomeSourceId = None, endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))

          When(s"I call GET $foreignPropertyPath")
          val result = buildGETMTDClient(foreignPropertyPath).futureValue
          verifyIncomeSourceDetailsCall(testMtditid)

          result should have(
            httpStatus(OK),
            pageTitleIndividual(pageTitleMsgKeyUK),
            elementTextBySelectorList(".govuk-summary-list__key", "dt:nth-of-type(1)")(messagesAPI("incomeSources.ceaseForeignProperty.checkDetails.dateStopped")),
            elementTextBySelectorList(".govuk-summary-list__value", "dd:nth-of-type(1)")(testLongEndDate2022),
            elementTextByID("change")(changeLink),
            elementAttributeBySelector("form", "action")(formActionFP)
          )
        }
      }
      "redirect to Home Page" when {
        "Income source is disabled" in {
          disable(IncomeSources)
          disable(NavBarFs)
          MTDIndividualAuthStub.stubAuthorised()
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

          await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-FP", ceaseIncomeSourceData =
            Some(CeaseIncomeSourceData(incomeSourceId = None, endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))

          When(s"I call GET $foreignPropertyPath")
          val result = buildGETMTDClient(foreignPropertyPath).futureValue
          verifyIncomeSourceDetailsCall(testMtditid)

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.routes.HomeController.show().url)
          )
        }
      }
    }
    testAuthFailuresForMTDIndividual(foreignPropertyPath)
  }

  s"POST ${selfEmploymentPath}" should {
    s"redirect to $redirectUriSE" when {
      "User is authorised" in {
        enable(IncomeSources)
        disable(NavBarFs)
        MTDIndividualAuthStub.stubAuthorised()
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-SE", ceaseIncomeSourceData =
          Some(CeaseIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = None, journeyIsComplete = Some(false))))))

        val result = buildPOSTMTDPostClient(selfEmploymentPath, Map.empty, Map.empty).futureValue

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(redirectUriSE),
        )

        AuditStub.verifyAuditContainsDetail(CeaseIncomeSourceAuditModel(SelfEmployment, testEndDate2022, mkIncomeSourceId(testSelfEmploymentId), None)(testUser, hc).detail)
      }
    }
    testAuthFailuresForMTDIndividual(selfEmploymentPath)
  }

  s"calling POST ${ukPropertyPath}" should {
    s"redirect to $redirectUriUK" when {
      "User is authorised" in {
        enable(IncomeSources)
        disable(NavBarFs)
        MTDIndividualAuthStub.stubAuthorised()
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-UK", ceaseIncomeSourceData =
          Some(CeaseIncomeSourceData(incomeSourceId = None, endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))

        val result = buildPOSTMTDPostClient(ukPropertyPath, Map.empty, Map.empty).futureValue

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(redirectUriUK),
        )

        AuditStub.verifyAuditContainsDetail(CeaseIncomeSourceAuditModel(UkProperty, testEndDate2022, mkIncomeSourceId(testPropertyIncomeId), None)(testUser, hc).detail)
      }
    }
    testAuthFailuresForMTDIndividual(ukPropertyPath)
  }

  s"calling POST ${foreignPropertyPath}" should {
    s"redirect to $redirectUriFP" when {
      "User is authorised" in {
        enable(IncomeSources)
        disable(NavBarFs)
        MTDIndividualAuthStub.stubAuthorised()
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-FP", ceaseIncomeSourceData =
          Some(CeaseIncomeSourceData(incomeSourceId = None, endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))

        val result = buildPOSTMTDPostClient(foreignPropertyPath, Map.empty, Map.empty).futureValue

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(redirectUriFP),
        )
        AuditStub.verifyAuditContainsDetail(CeaseIncomeSourceAuditModel(ForeignProperty, testEndDate2022, mkIncomeSourceId(testPropertyIncomeId), None)(testUser, hc).detail)
      }
    }
    testAuthFailuresForMTDIndividual(foreignPropertyPath)
  }
}
