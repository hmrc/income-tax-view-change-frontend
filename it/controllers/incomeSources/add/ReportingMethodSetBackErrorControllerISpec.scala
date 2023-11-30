package controllers.incomeSources.add

import config.featureswitch.IncomeSources
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.{OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.businessOnlyResponse

class ReportingMethodSetBackErrorControllerISpec extends ComponentSpecBase{

  private lazy val backErrorController = controllers.incomeSources.add.routes.YouCannotGoBackErrorController

  val selfEmploymentBackErrorUrl: String = backErrorController.show(SelfEmployment).url
  val ukPropertyBackErrorUrl: String = backErrorController.show(UkProperty).url
  val foreignPropertyBackErrorUrl: String = backErrorController.show(ForeignProperty).url

  val title = messagesAPI("cannot-go-back.heading")
  val headingSE = messagesAPI("cannot-go-back.sole-trader")
  val headingUk = messagesAPI("cannot-go-back.uk-property")
  val headingFP = messagesAPI("cannot-go-back.foreign-property")

  s"calling GET $selfEmploymentBackErrorUrl" should {
    "render the self employment business not added error page" when {
      "Income Sources FS is enabled" in {
        enable(IncomeSources)

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        val result = IncomeTaxViewChangeFrontend
          .get(s"/income-sources/add/add-business-cannot-go-back")

        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleIndividual(s"$title - $headingSE")
        )
      }
    }
    "Income Sources FS is disabled" in {
      Given("Income Sources FS is enabled")
      disable(IncomeSources)

      And("API 1771  returns a success response")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

      val result = IncomeTaxViewChangeFrontend
        .get(s"/income-sources/add/add-business-cannot-go-back")

      verifyIncomeSourceDetailsCall(testMtditid)

      result should have(
        httpStatus(SEE_OTHER)
      )
    }
  }

  s"calling GET $ukPropertyBackErrorUrl" should {
    "render the self employment business not added error page" when {
      "Income Sources FS is enabled" in {
        enable(IncomeSources)

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        val result = IncomeTaxViewChangeFrontend
          .get(s"/income-sources/add/add-uk-property-cannot-go-back")

        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleIndividual(s"$title - $headingUk")
        )
      }
    }
    "Income Sources FS is disabled" in {
      Given("Income Sources FS is enabled")
      disable(IncomeSources)

      And("API 1771  returns a success response")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

      val result = IncomeTaxViewChangeFrontend
        .get(s"/income-sources/add/add-uk-property-cannot-go-back")

      verifyIncomeSourceDetailsCall(testMtditid)

      result should have(
        httpStatus(SEE_OTHER)
      )
    }
  }

  s"calling GET $foreignPropertyBackErrorUrl" should {
    "render the self employment business not added error page" when {
      "Income Sources FS is enabled" in {
        enable(IncomeSources)

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        val result = IncomeTaxViewChangeFrontend
          .get(s"/income-sources/add/add-foreign-property-cannot-go-back")

        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleIndividual(s"$title - $headingFP")
        )
      }
    }
    "Income Sources FS is disabled" in {
      Given("Income Sources FS is enabled")
      disable(IncomeSources)

      And("API 1771  returns a success response")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

      val result = IncomeTaxViewChangeFrontend
        .get(s"/income-sources/add/add-foreign-property-cannot-go-back")

      verifyIncomeSourceDetailsCall(testMtditid)

      result should have(
        httpStatus(SEE_OTHER)
      )
    }
  }
}
