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
import auth.MtdItUser
import models.admin.IncomeSources
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import enums.JourneyType.Cease
import helpers.ComponentSpecBase
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub}
import models.core.IncomeSourceId.mkIncomeSourceId
import models.incomeSourceDetails.{CeaseIncomeSourceData, UIJourneySessionData}
import models.updateIncomeSource.{Cessation, UpdateIncomeSourceRequestModel, UpdateIncomeSourceResponseModel}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.UIJourneySessionDataRepository
import services.SessionService
import testConstants.BaseIntegrationTestConstants._
import testConstants.BusinessDetailsIntegrationTestConstants.business1
import testConstants.IncomeSourceIntegrationTestConstants._
import testConstants.PropertyDetailsIntegrationTestConstants.{foreignProperty, ukProperty}
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

import java.time.LocalDate

class CheckCeaseIncomeSourceDetailsControllerISpec extends ComponentSpecBase {

  val sessionService: SessionService = app.injector.instanceOf[SessionService]
  val repository = app.injector.instanceOf[UIJourneySessionDataRepository]

  val cessationDate = "2022-10-10"
  val businessEndShortLongDate = "23 April 2022"
  val testLongEndDate2022: String = "10 October 2022"
  val changeLink = "Change"
  val testBusinessName = "business"
  val timestamp = "2023-01-31T09:26:17Z"
  val businessAddressAsString = "8 Test New Court New Town New City NE12 6CI United Kingdom"


  val showCheckCeaseBusinessDetailsControllerUrl = controllers.manageBusinesses.cease.routes.CeaseCheckIncomeSourceDetailsController.show(SelfEmployment).url
  val formActionSE = controllers.manageBusinesses.cease.routes.CeaseCheckIncomeSourceDetailsController.submit(SelfEmployment).url

  val businessStopDateLabel = messagesAPI("cease-check-answers.cease-date")
  val businessNameLabel = messagesAPI("cease-check-answers.business-name")
  val businessAddressLabel = messagesAPI("cease-check-answers.address")
  val pageTitleMsgKey = messagesAPI("cease-check-answers.title")
  val unknown: String = messagesAPI("cease-check-answers.unknown")

  val redirectUriSE = controllers.manageBusinesses.cease.routes.IncomeSourceCeasedObligationsController.show(SelfEmployment).url
  val requestSE: UpdateIncomeSourceRequestModel = UpdateIncomeSourceRequestModel(
    nino = testNino,
    incomeSourceID = business1.incomeSourceId,
    cessation = Some(Cessation(true, Some(LocalDate.parse(cessationDate))))
  )

  val showCheckCeaseUKPropertyDetailsControllerUrl = controllers.manageBusinesses.cease.routes.CeaseCheckIncomeSourceDetailsController.show(UkProperty).url
  val formActionUK = controllers.manageBusinesses.cease.routes.CeaseCheckIncomeSourceDetailsController.submit(UkProperty).url
  val businessStopDateLabelUK = messagesAPI("cease-check-answers.cease-date")
  val pageTitleMsgKeyUK = messagesAPI("cease-check-answers.title")
  val redirectUriUK = controllers.manageBusinesses.cease.routes.IncomeSourceCeasedObligationsController.show(UkProperty).url
  val requestUK: UpdateIncomeSourceRequestModel = UpdateIncomeSourceRequestModel(
    nino = testNino,
    incomeSourceID = ukProperty.incomeSourceId,
    cessation = Some(Cessation(true, Some(LocalDate.parse(cessationDate))))
  )

  val showCheckCeaseForeignPropertyDetailsControllerUrl = controllers.manageBusinesses.cease.routes.CeaseCheckIncomeSourceDetailsController.show(ForeignProperty).url
  val formActionFP = controllers.manageBusinesses.cease.routes.CeaseCheckIncomeSourceDetailsController.submit(ForeignProperty).url
  val pageTitleMsgKeyFP = messagesAPI("cease-check-answers.title")
  val redirectUriFP = controllers.manageBusinesses.cease.routes.IncomeSourceCeasedObligationsController.show(ForeignProperty).url
  val requestFP: UpdateIncomeSourceRequestModel = UpdateIncomeSourceRequestModel(
    nino = testNino,
    incomeSourceID = foreignProperty.incomeSourceId,
    cessation = Some(Cessation(true, Some(LocalDate.parse(cessationDate))))
  )

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, None, multipleBusinessesAndPropertyResponse,
    None, Some("1234567890"), Some("12345-credId"), Some(Individual), None
  )(FakeRequest())

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Cease))
  }

  s"calling GET ${showCheckCeaseBusinessDetailsControllerUrl}" should {
    "render the Cease Business Details Page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with a SE buisness")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-SE", ceaseIncomeSourceData =
          Some(CeaseIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = None, journeyIsComplete = Some(false))))))

        When(s"I call GET ${showCheckCeaseBusinessDetailsControllerUrl}")
        val res = IncomeTaxViewChangeFrontendManageBusinesses.getCheckCeaseBusinessAnswers
        verifyIncomeSourceDetailsCall(testMtditid)

        res should have(
          httpStatus(OK),
          pageTitleIndividual(pageTitleMsgKey),
          elementTextBySelectorList(".govuk-summary-list__key", "dt:nth-of-type(1)")(messagesAPI("cease-check-answers.cease-date")),
          elementTextBySelectorList(".govuk-summary-list__value", "dd:nth-of-type(1)")(testLongEndDate2022),
          elementTextByID("change")(changeLink),

          elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(2) dt:nth-of-type(1)")(messagesAPI("cease-check-answers.business-name")),
          elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(2) dd:nth-of-type(1)")(testBusinessName),

          elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(3) dt:nth-of-type(1)")(messagesAPI("cease-check-answers.trade")),
          elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(3) dd:nth-of-type(1)")(testIncomeSource),

          elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(4) dt:nth-of-type(1)")(businessAddressLabel),
          elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(4) dd:nth-of-type(1)")(businessAddressAsString),


          elementAttributeBySelector("form", "action")(formActionSE)
        )
      }
    }

    "render the Cease Business Page with unknown address and title and trade" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with a SE business with no address")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponseWithUnknownAddressName)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-SE", ceaseIncomeSourceData =
          Some(CeaseIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = None, journeyIsComplete = Some(false))))))

        When(s"I call GET ${showCheckCeaseBusinessDetailsControllerUrl}")
        val res = IncomeTaxViewChangeFrontendManageBusinesses.getCheckCeaseBusinessAnswers
        verifyIncomeSourceDetailsCall(testMtditid)

        res should have(
          httpStatus(OK),
          pageTitleIndividual(pageTitleMsgKey),
          elementTextBySelectorList(".govuk-summary-list__key", "dt:nth-of-type(1)")(messagesAPI("cease-check-answers.cease-date")),
          elementTextBySelectorList(".govuk-summary-list__value", "dd:nth-of-type(1)")(testLongEndDate2022),
          elementTextByID("change")(changeLink),

          elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(2) dt:nth-of-type(1)")(messagesAPI("cease-check-answers.business-name")),
          elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(2) dd:nth-of-type(1)")(unknown),

          elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(3) dt:nth-of-type(1)")(messagesAPI("cease-check-answers.trade")),
          elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(3) dd:nth-of-type(1)")(unknown),

          elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(4) dt:nth-of-type(1)")(businessAddressLabel),
          elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(4) dd:nth-of-type(1)")(unknown),
          elementAttributeBySelector("form", "action")(formActionSE)
        )
      }
    }
  }

  s"calling POST ${showCheckCeaseBusinessDetailsControllerUrl}" should {
    s"redirect to $redirectUriSE" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        enable(IncomeSources)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-SE", ceaseIncomeSourceData =
          Some(CeaseIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = None, journeyIsComplete = Some(false))))))

        val res = IncomeTaxViewChangeFrontendManageBusinesses.postCheckCeaseBusinessAnswers
        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxViewChangeStub.verifyUpdateIncomeSource(Some(Json.toJson(requestSE).toString()))

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(redirectUriSE),
        )

        AuditStub.verifyAuditContainsDetail(CeaseIncomeSourceAuditModel(SelfEmployment, testEndDate2022, mkIncomeSourceId(testSelfEmploymentId), None)(testUser, hc).detail)
      }
    }
  }

  s"calling GET ${showCheckCeaseUKPropertyDetailsControllerUrl}" should {
    "render the Cease UK Property Details Page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with a UK property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-UK", ceaseIncomeSourceData =
          Some(CeaseIncomeSourceData(incomeSourceId = None, endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))

        When(s"I call GET ${showCheckCeaseUKPropertyDetailsControllerUrl}")
        val res = IncomeTaxViewChangeFrontendManageBusinesses.getCheckCeaseUKPropertyAnswers
        verifyIncomeSourceDetailsCall(testMtditid)


        res should have(
          httpStatus(OK),
          pageTitleIndividual(pageTitleMsgKeyUK),
          elementTextBySelectorList(".govuk-summary-list__key", "dt:nth-of-type(1)")(messagesAPI("cease-check-answers.cease-date")),
          elementTextBySelectorList(".govuk-summary-list__value", "dd:nth-of-type(1)")(testLongEndDate2022),
          elementTextByID("change")(changeLink),
          elementAttributeBySelector("form", "action")(formActionUK)
        )
      }
    }
  }

  s"calling POST ${showCheckCeaseUKPropertyDetailsControllerUrl}" should {
    s"redirect to $redirectUriUK" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with a UK property")
        enable(IncomeSources)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-UK", ceaseIncomeSourceData =
          Some(CeaseIncomeSourceData(incomeSourceId = None, endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))

        val res = IncomeTaxViewChangeFrontendManageBusinesses.postCheckCeaseUKPropertyAnswers
        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxViewChangeStub.verifyUpdateIncomeSource(Some(Json.toJson(requestUK).toString()))

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(redirectUriUK),
        )

        AuditStub.verifyAuditContainsDetail(CeaseIncomeSourceAuditModel(UkProperty, testEndDate2022, mkIncomeSourceId(testPropertyIncomeId), None)(testUser, hc).detail)
      }
    }
  }

  s"calling GET ${showCheckCeaseForeignPropertyDetailsControllerUrl}" should {
    "render the Cease Foreign Property Details Page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with a Foreign Property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-FP", ceaseIncomeSourceData =
          Some(CeaseIncomeSourceData(incomeSourceId = None, endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))

        When(s"I call GET ${showCheckCeaseForeignPropertyDetailsControllerUrl}")
        val res = IncomeTaxViewChangeFrontendManageBusinesses.getCheckCeaseForeignPropertyAnswers
        verifyIncomeSourceDetailsCall(testMtditid)

        res should have(
          httpStatus(OK),
          pageTitleIndividual(pageTitleMsgKeyUK),
          elementTextBySelectorList(".govuk-summary-list__key", "dt:nth-of-type(1)")(messagesAPI("cease-check-answers.cease-date")),
          elementTextBySelectorList(".govuk-summary-list__value", "dd:nth-of-type(1)")(testLongEndDate2022),
          elementTextByID("change")(changeLink),
          elementAttributeBySelector("form", "action")(formActionFP)
        )
      }
    }
  }

  s"calling POST ${showCheckCeaseForeignPropertyDetailsControllerUrl}" should {
    s"redirect to $redirectUriFP" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with a Foriegn Property")
        enable(IncomeSources)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-FP", ceaseIncomeSourceData =
          Some(CeaseIncomeSourceData(incomeSourceId = None, endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))

        val res = IncomeTaxViewChangeFrontendManageBusinesses.postCheckCeaseForeignPropertyAnswers
        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxViewChangeStub.verifyUpdateIncomeSource(Some(Json.toJson(requestFP).toString()))

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(redirectUriFP),
        )
        AuditStub.verifyAuditContainsDetail(CeaseIncomeSourceAuditModel(ForeignProperty, testEndDate2022, mkIncomeSourceId(testPropertyIncomeId), None)(testUser, hc).detail)
      }
    }
  }
}
