package controllers.agent.incomeSources.cease

import config.featureswitch.IncomeSources
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.updateIncomeSource.{Cessation, UpdateIncomeSourceRequestModel, UpdateIncomeSourceResponseModel}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.Json
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid, testNino, testSelfEmploymentId}
import testConstants.BusinessDetailsIntegrationTestConstants.business1
import testConstants.IncomeSourceIntegrationTestConstants.{businessAndPropertyResponse, businessOnlyResponse, foreignPropertyOnlyResponse, propertyOnlyResponse, ukPropertyOnlyResponse}
import testConstants.PropertyDetailsIntegrationTestConstants.{foreignProperty, ukProperty}

import java.time.LocalDate

class CheckCeaseIncomeSourceDetailsControllerISpec extends ComponentSpecBase{

  val cessationDate = "2022-04-23"
  val businessEndShortLongDate = "23 April 2022"
  val changeLink = "Change"
  val testBusinessName = "business"
  val timestamp = "2023-01-31T09:26:17Z"


  val sessionCeaseBusinessDetails = Map(forms.utils.SessionKeys.ceaseBusinessEndDate -> cessationDate, forms.utils.SessionKeys.ceaseBusinessIncomeSourceId -> testSelfEmploymentId)
  val showCheckCeaseBusinessDetailsControllerUrl = controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.showAgent(SelfEmployment).url
  val submitCheckCeaseBusinessDetailsControllerUrl = controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.submitAgent(SelfEmployment).url
  val formActionSE = controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.submitAgent(SelfEmployment).url
  val businessStopDateLabel = messagesAPI("incomeSources.ceaseBusiness.checkDetails.dateStopped")
  val businessNameLabel = messagesAPI("incomeSources.ceaseBusiness.checkDetails.businessName")
  val businessAddressLabel = messagesAPI("incomeSources.ceaseBusiness.checkDetails.businessAddress")
  val pageTitleMsgKey = messagesAPI("incomeSources.ceaseBusiness.checkDetails.heading")
  val unknown: String = messagesAPI("incomeSources.ceaseBusiness.checkDetails.unknown")
  val redirectUriSE = controllers.incomeSources.cease.routes.BusinessCeasedObligationsController.showAgent().url
  val requestSE: UpdateIncomeSourceRequestModel = UpdateIncomeSourceRequestModel(
    nino = testNino,
    incomeSourceId = business1.incomeSourceId,
    cessation = Some(Cessation(true, Some(LocalDate.parse(cessationDate))))
  )

  val sessionCeaseUKPropertyEndDate = Map(forms.utils.SessionKeys.ceaseUKPropertyEndDate -> cessationDate)
  val showCheckCeaseUKPropertyDetailsControllerUrl = controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.showAgent(UkProperty).url
  val submitCheckCeaseUKPropertyDetailsControllerUrl = controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.submitAgent(UkProperty).url
  val formActionUK = controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.submitAgent(UkProperty).url
  val businessStopDateLabelUK = messagesAPI("incomeSources.ceaseUKProperty.checkDetails.content")
  val pageTitleMsgKeyUK = messagesAPI("incomeSources.ceaseUKProperty.checkDetails.heading")
  val redirectUriUK = controllers.incomeSources.cease.routes.UKPropertyCeasedObligationsController.showAgent().url
  val requestUK: UpdateIncomeSourceRequestModel = UpdateIncomeSourceRequestModel(
    nino = testNino,
    incomeSourceId = ukProperty.incomeSourceId,
    cessation = Some(Cessation(true, Some(LocalDate.parse(cessationDate))))
  )

  val sessionCeaseForeignPropertyEndDate = Map(forms.utils.SessionKeys.ceaseForeignPropertyEndDate -> cessationDate)
  val showCheckCeaseForeignPropertyDetailsControllerUrl = controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.showAgent(ForeignProperty).url
  val submitCheckCeaseForeignPropertyDetailsControllerUrl = controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.showAgent(ForeignProperty).url

  val formActionFP = controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.submitAgent(ForeignProperty).url
  val pageTitleMsgKeyFP = messagesAPI("incomeSources.ceaseForeignProperty.checkDetails.heading")
  val redirectUriFP = controllers.incomeSources.cease.routes.ForeignPropertyCeasedObligationsController.showAgent().url
  val requestFP: UpdateIncomeSourceRequestModel = UpdateIncomeSourceRequestModel(
    nino = testNino,
    incomeSourceId = foreignProperty.incomeSourceId,
    cessation = Some(Cessation(true, Some(LocalDate.parse(cessationDate))))
  )

  s"calling GET ${showCheckCeaseBusinessDetailsControllerUrl}" should {
    "render the Cease Business Page" when {
      "User is authorised" in {
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = businessAndPropertyResponse
        )

        val res = IncomeTaxViewChangeFrontend.getCheckCeaseBusinessDetails(sessionCeaseBusinessDetails ++ clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        res should have(
          httpStatus(OK),
          pageTitleAgent(pageTitleMsgKey),
          elementTextBySelectorList(".govuk-summary-list__key", "dt:nth-of-type(1)")(messagesAPI("incomeSources.ceaseBusiness.checkDetails.dateStopped")),
          elementTextBySelectorList(".govuk-summary-list__value", "dd:nth-of-type(1)")(businessEndShortLongDate),
          elementTextByID("change-link")(changeLink),

          elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(2) dt:nth-of-type(1)")(messagesAPI("incomeSources.ceaseBusiness.checkDetails.businessName")),
          elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(2) dd:nth-of-type(1)")(testBusinessName),
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
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))
        When(s"I call POST ${submitCheckCeaseBusinessDetailsControllerUrl}")

        val res = IncomeTaxViewChangeFrontend.postCheckCeaseBusinessDetails(sessionCeaseBusinessDetails ++ clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxViewChangeStub.verifyUpdateIncomeSource(Some(Json.toJson(requestSE).toString()))

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(redirectUriSE),
        )
      }
    }
  }

  s"calling GET ${showCheckCeaseUKPropertyDetailsControllerUrl}" should {
    "render the Cease UK Property Page" when {
      "User is authorised" in {
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = ukPropertyOnlyResponse
        )

        val res = IncomeTaxViewChangeFrontend.getCheckCeaseUKPropertyDetails(sessionCeaseUKPropertyEndDate ++ clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        println(res.body)
        res should have(
          httpStatus(OK),
          pageTitleAgent(pageTitleMsgKeyUK),
          elementTextBySelectorList(".govuk-summary-list__key", "dt:nth-of-type(1)")(messagesAPI("incomeSources.ceaseUKProperty.checkDetails.dateStopped")),
          elementTextBySelectorList(".govuk-summary-list__value", "dd:nth-of-type(1)")(businessEndShortLongDate),
          elementTextByID("change-link")(changeLink),
          elementAttributeBySelector("form", "action")(formActionUK)
        )
      }
    }
  }

  s"calling POST ${showCheckCeaseUKPropertyDetailsControllerUrl}" should {
    s"redirect to $redirectUriUK" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with UK property")
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))
        When(s"I call POST ${submitCheckCeaseUKPropertyDetailsControllerUrl}")

        val res = IncomeTaxViewChangeFrontend.postCheckCeaseUKPropertyDetails(sessionCeaseUKPropertyEndDate ++ clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxViewChangeStub.verifyUpdateIncomeSource(Some(Json.toJson(requestUK).toString()))

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(redirectUriUK),
        )
      }
    }
  }

  s"calling GET ${showCheckCeaseForeignPropertyDetailsControllerUrl}" should {
    "render the Cease Foreign Property Page" when {
      "User is authorised" in {
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = foreignPropertyOnlyResponse
        )

        val res = IncomeTaxViewChangeFrontend.getCheckCeaseForeignPropertyDetails(sessionCeaseForeignPropertyEndDate ++ clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        res should have(
          httpStatus(OK),
          pageTitleAgent(pageTitleMsgKeyFP),
          elementTextBySelectorList(".govuk-summary-list__key", "dt:nth-of-type(1)")(messagesAPI("incomeSources.ceaseForeignProperty.checkDetails.dateStopped")),
          elementTextBySelectorList(".govuk-summary-list__value", "dd:nth-of-type(1)")(businessEndShortLongDate),
          elementTextByID("change-link")(changeLink),
          elementAttributeBySelector("form", "action")(formActionFP)
        )
      }
    }
  }

  s"calling POST ${showCheckCeaseForeignPropertyDetailsControllerUrl}" should {
    s"redirect to $redirectUriFP" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with UK property")
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))
        When(s"I call POST ${submitCheckCeaseForeignPropertyDetailsControllerUrl}")

        val res = IncomeTaxViewChangeFrontend.postCheckCeaseForeignPropertyDetails(sessionCeaseForeignPropertyEndDate ++ clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxViewChangeStub.verifyUpdateIncomeSource(Some(Json.toJson(requestFP).toString()))

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(redirectUriFP),
        )
      }
    }
  }


}
