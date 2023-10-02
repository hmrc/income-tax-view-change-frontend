package controllers.incomeSources.cease

import config.featureswitch.IncomeSources
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.updateIncomeSource.{Cessation, UpdateIncomeSourceRequestModel, UpdateIncomeSourceResponseModel}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.Json
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino, testSelfEmploymentId}
import testConstants.BusinessDetailsIntegrationTestConstants.business1
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, businessOnlyResponseWithUnknownAddressName, foreignPropertyOnlyResponse, ukPropertyOnlyResponse}
import testConstants.PropertyDetailsIntegrationTestConstants.{foreignProperty, ukProperty}

import java.time.LocalDate

class CheckCeaseIncomeSourceDetailsControllerISpec extends ComponentSpecBase{
  val cessationDate = "2022-04-23"
  val businessEndShortLongDate = "23 April 2022"
  val changeLink = "Change"
  val testBusinessName = "business"
  val timestamp = "2023-01-31T09:26:17Z"


  val sessionCeaseBusinessDetails = Map(forms.utils.SessionKeys.ceaseBusinessEndDate -> cessationDate, forms.utils.SessionKeys.ceaseBusinessIncomeSourceId -> testSelfEmploymentId)
  val showCheckCeaseBusinessDetailsControllerUrl = controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.show(SelfEmployment).url
  val formActionSE = controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.submit(SelfEmployment).url

  val businessStopDateLabel = messagesAPI("incomeSources.ceaseBusiness.checkDetails.dateStopped")
  val businessNameLabel = messagesAPI("incomeSources.ceaseBusiness.checkDetails.businessName")
  val businessAddressLabel = messagesAPI("incomeSources.ceaseBusiness.checkDetails.businessAddress")
  val pageTitleMsgKey = messagesAPI("incomeSources.ceaseBusiness.checkDetails.heading")
  val unknown: String = messagesAPI("incomeSources.ceaseBusiness.checkDetails.unknown")

  val redirectUriSE = controllers.incomeSources.cease.routes.BusinessCeasedObligationsController.show().url
  val requestSE: UpdateIncomeSourceRequestModel = UpdateIncomeSourceRequestModel(
    nino = testNino,
    incomeSourceId = business1.incomeSourceId,
    cessation = Some(Cessation(true, Some(LocalDate.parse(cessationDate))))
  )

  val sessionCeaseUKPropertyEndDate = Map(forms.utils.SessionKeys.ceaseUKPropertyEndDate -> cessationDate)
  val showCheckCeaseUKPropertyDetailsControllerUrl = controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.show(UkProperty).url
  val formActionUK = controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.submit(UkProperty).url
  val businessStopDateLabelUK = messagesAPI("incomeSources.ceaseUKProperty.checkDetails.content")
  val pageTitleMsgKeyUK = messagesAPI("incomeSources.ceaseUKProperty.checkDetails.heading")
  val redirectUriUK = controllers.incomeSources.cease.routes.UKPropertyCeasedObligationsController.show().url
  val requestUK: UpdateIncomeSourceRequestModel = UpdateIncomeSourceRequestModel(
    nino = testNino,
    incomeSourceId = ukProperty.incomeSourceId,
    cessation = Some(Cessation(true, Some(LocalDate.parse(cessationDate))))
  )

  val sessionCeaseForeignPropertyEndDate = Map(forms.utils.SessionKeys.ceaseForeignPropertyEndDate -> cessationDate)
  val showCheckCeaseForeignPropertyDetailsControllerUrl = controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.show(ForeignProperty).url
  val formActionFP = controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.submit(ForeignProperty).url
  val pageTitleMsgKeyFP = messagesAPI("check-cease-foreign-property-details.heading")
  val redirectUriFP = controllers.incomeSources.cease.routes.ForeignPropertyCeasedObligationsController.show().url
  val requestFP: UpdateIncomeSourceRequestModel = UpdateIncomeSourceRequestModel(
    nino = testNino,
    incomeSourceId = foreignProperty.incomeSourceId,
    cessation = Some(Cessation(true, Some(LocalDate.parse(cessationDate))))
  )

  s"calling GET ${showCheckCeaseBusinessDetailsControllerUrl}" should {
    "render the Cease Business Details Page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)
        When(s"I call GET ${showCheckCeaseBusinessDetailsControllerUrl}")
        val res = IncomeTaxViewChangeFrontend.getCheckCeaseBusinessDetails(sessionCeaseBusinessDetails)
        verifyIncomeSourceDetailsCall(testMtditid)

        res should have(
          httpStatus(OK),
          pageTitleIndividual(pageTitleMsgKey),
          elementTextBySelectorList(".govuk-summary-list__key", "dt:nth-of-type(1)")(messagesAPI("incomeSources.ceaseBusiness.checkDetails.dateStopped")),
          elementTextBySelectorList(".govuk-summary-list__value", "dd:nth-of-type(1)")(businessEndShortLongDate),
          elementTextByID("change-link")(changeLink),

          elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(2) dt:nth-of-type(1)")(messagesAPI("incomeSources.ceaseBusiness.checkDetails.businessName")),
          elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(2) dd:nth-of-type(1)")(testBusinessName),
          elementAttributeBySelector("form", "action")(formActionSE)
        )
      }
    }

    "render the Cease Business Page with unknown address and title" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponseWithUnknownAddressName)
        When(s"I call GET ${showCheckCeaseBusinessDetailsControllerUrl}")
        val res = IncomeTaxViewChangeFrontend.getCheckCeaseBusinessDetails(sessionCeaseBusinessDetails)
        verifyIncomeSourceDetailsCall(testMtditid)

        res should have(
          httpStatus(OK),
          pageTitleIndividual(pageTitleMsgKey),
          elementTextBySelectorList(".govuk-summary-list__key", "dt:nth-of-type(1)")(messagesAPI("incomeSources.ceaseBusiness.checkDetails.dateStopped")),
          elementTextBySelectorList(".govuk-summary-list__value", "dd:nth-of-type(1)")(businessEndShortLongDate),
          elementTextByID("change-link")(changeLink),

          elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(2) dt:nth-of-type(1)")(messagesAPI("incomeSources.ceaseBusiness.checkDetails.businessName")),
          elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(2) dd:nth-of-type(1)")(unknown),

          elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(3) dt:nth-of-type(1)")(businessAddressLabel),
          elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(3) dd:nth-of-type(1)")(unknown),
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

        val res = IncomeTaxViewChangeFrontend.postCheckCeaseBusinessDetails(sessionCeaseBusinessDetails)
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
    "render the Cease UK Property Details Page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        When(s"I call GET ${showCheckCeaseUKPropertyDetailsControllerUrl}")
        val res = IncomeTaxViewChangeFrontend.getCheckCeaseUKPropertyDetails(sessionCeaseUKPropertyEndDate)
        verifyIncomeSourceDetailsCall(testMtditid)


        res should have(
          httpStatus(OK),
          pageTitleIndividual(pageTitleMsgKeyUK),
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
        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        enable(IncomeSources)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        val res = IncomeTaxViewChangeFrontend.postCheckCeaseUKPropertyDetails(sessionCeaseUKPropertyEndDate)
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
    "render the Cease Foreign Property Details Page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)
        When(s"I call GET ${showCheckCeaseForeignPropertyDetailsControllerUrl}")
        val res = IncomeTaxViewChangeFrontend.getCheckCeaseForeignPropertyDetails(sessionCeaseForeignPropertyEndDate)
        verifyIncomeSourceDetailsCall(testMtditid)

        res should have(
          httpStatus(OK),
          pageTitleIndividual(pageTitleMsgKeyUK),
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
        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        enable(IncomeSources)


        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        val res = IncomeTaxViewChangeFrontend.postCheckCeaseForeignPropertyDetails(sessionCeaseForeignPropertyEndDate)
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
