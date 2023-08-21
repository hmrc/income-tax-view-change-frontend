package controllers.incomeSources.cease

import config.featureswitch.IncomeSources
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.updateIncomeSource.{Cessation, UpdateIncomeSourceRequestModel, UpdateIncomeSourceResponseModel}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.Json
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino, testSelfEmploymentId}
import testConstants.BusinessDetailsIntegrationTestConstants.business1
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, businessOnlyResponseWithUnknownAddressName}

import java.time.LocalDate

class CheckCeaseBusinessDetailsControllerISpec extends ComponentSpecBase {
  val cessationDate = "2022-04-23"
  val sessionCeaseBusinessDetails = Map(forms.utils.SessionKeys.ceaseBusinessEndDate -> cessationDate, forms.utils.SessionKeys.ceaseBusinessIncomeSourceId -> testSelfEmploymentId)
  val showCheckCeaseBusinessDetailsControllerUrl = controllers.incomeSources.cease.routes.CheckCeaseBusinessDetailsController.show().url
  val formAction = controllers.incomeSources.cease.routes.CheckCeaseBusinessDetailsController.submit().url
  val businessEndShortLongDate = "23 April 2022"
  val businessStopDateLabel = messagesAPI("incomeSources.ceaseBusiness.checkDetails.dateStopped")
  val businessNameLabel = messagesAPI("incomeSources.ceaseBusiness.checkDetails.businessName")
  val businessAddressLabel = messagesAPI("incomeSources.ceaseBusiness.checkDetails.businessAddress")
  val pageTitleMsgKey = messagesAPI("incomeSources.ceaseBusiness.checkDetails.heading")
  val unknown: String = messagesAPI("incomeSources.ceaseBusiness.checkDetails.unknown")
  val timestamp = "2023-01-31T09:26:17Z"
  val redirectUri = controllers.incomeSources.cease.routes.BusinessCeasedObligationsController.show().url
  val request: UpdateIncomeSourceRequestModel = UpdateIncomeSourceRequestModel(
    nino = testNino,
    incomeSourceId = business1.incomeSourceId,
    cessation = Some(Cessation(true, Some(LocalDate.parse(cessationDate))))
  )

  s"calling GET ${showCheckCeaseBusinessDetailsControllerUrl}" should {
    "render the Cease Business Page" when {
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
          elementTextByID("businessStopDate")(businessEndShortLongDate),
          elementTextByID("businessStopDateLabel")(businessStopDateLabel),
          elementTextByID("businessNameLabel")(businessNameLabel),
          elementTextByID("businessAddressLabel")(businessAddressLabel),
          elementAttributeBySelector("form", "action")(formAction)
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
          elementTextByID("businessStopDate")(businessEndShortLongDate),
          elementTextByID("businessStopDateLabel")(businessStopDateLabel),
          elementTextByID("businessNameLabel")(businessNameLabel),
          elementTextByID("businessName")(unknown),
          elementTextByID("businessAddressLabel")(businessAddressLabel),
          elementTextByID("businessAddress")(unknown),
          elementAttributeBySelector("form", "action")(formAction)
        )
      }
    }
  }

  s"calling POST ${showCheckCeaseBusinessDetailsControllerUrl}" should {
    s"redirect to $redirectUri" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        enable(IncomeSources)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        val res = IncomeTaxViewChangeFrontend.postCheckCeaseBusinessDetails(sessionCeaseBusinessDetails)
        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxViewChangeStub.verifyUpdateIncomeSource(Some(Json.toJson(request).toString()))

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(redirectUri),
        )
      }
    }
  }
}
