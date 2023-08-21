package controllers.incomeSources.cease

import config.featureswitch.IncomeSources
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.updateIncomeSource.{Cessation, UpdateIncomeSourceRequestModel, UpdateIncomeSourceResponseModel}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.Json
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino}
import testConstants.IncomeSourceIntegrationTestConstants.foreignPropertyOnlyResponse
import testConstants.PropertyDetailsIntegrationTestConstants.foreignProperty

import java.time.LocalDate

class CheckCeaseForeignPropertyDetailsControllerISpec extends ComponentSpecBase {
  val cessationDate = "2022-04-23"
  val sessionCeaseForeignPropertyEndDate = Map(forms.utils.SessionKeys.ceaseForeignPropertyEndDate -> cessationDate)
  val showCheckCeaseForeignPropertyDetailsControllerUrl = controllers.incomeSources.cease.routes.CheckCeaseForeignPropertyDetailsController.show().url
  val formAction = controllers.incomeSources.cease.routes.CheckCeaseForeignPropertyDetailsController.submit(cessationDate).url
  val pageTitleMsgKey = messagesAPI("check-cease-foreign-property-details.heading")
  val timestamp = "2023-01-31T09:26:17Z"
  val redirectUri = controllers.incomeSources.cease.routes.ForeignPropertyCeasedObligationsController.show().url
  val request: UpdateIncomeSourceRequestModel = UpdateIncomeSourceRequestModel(
    nino = testNino,
    incomeSourceId = foreignProperty.incomeSourceId,
    cessation = Some(Cessation(true, Some(LocalDate.parse(cessationDate))))
  )

  s"calling GET ${showCheckCeaseForeignPropertyDetailsControllerUrl}" should {
    "render the Cease Foreign Property Page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)
        When(s"I call GET ${showCheckCeaseForeignPropertyDetailsControllerUrl}")
        val res = IncomeTaxViewChangeFrontend.getCheckCeaseForeignPropertyDetails(sessionCeaseForeignPropertyEndDate)
        verifyIncomeSourceDetailsCall(testMtditid)

        res should have(
          httpStatus(OK),
          pageTitleIndividual(pageTitleMsgKey),
          elementAttributeBySelector("form", "action")(formAction)
        )
      }
    }
  }

  s"calling POST ${showCheckCeaseForeignPropertyDetailsControllerUrl}" should {
    s"redirect to $redirectUri" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        enable(IncomeSources)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

        val res = IncomeTaxViewChangeFrontend.postCheckCeaseForeignPropertyDetails(cessationDate, Map.empty)
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
