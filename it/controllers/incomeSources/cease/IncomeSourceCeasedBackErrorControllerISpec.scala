package controllers.incomeSources.cease

import config.featureswitch.IncomeSources
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import org.scalatest.Assertion
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.businessOnlyResponse

class IncomeSourceCeasedBackErrorControllerISpec extends ComponentSpecBase {

  val title = messagesAPI("cannot-go-back.heading")
  val headingSE = messagesAPI("cannot-go-back.sole-trader-ceased")
  val headingUk = messagesAPI("cannot-go-back.uk-property-ceased")
  val headingFP = messagesAPI("cannot-go-back.foreign-property-ceased")

  val url: IncomeSourceType => String = (incomeSourceType: IncomeSourceType) =>
    controllers.incomeSources.cease.routes.IncomeSourceCeasedBackErrorController.show(incomeSourceType).url

  def runOKTest(incomeSourceType: IncomeSourceType): Assertion = {
    enable(IncomeSources)
    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

    val specificHeading = incomeSourceType match {
      case SelfEmployment => headingSE
      case UkProperty => headingUk
      case ForeignProperty => headingFP
    }

    val expectedTitle = s"$title - $specificHeading"

    lazy val result: WSResponse = incomeSourceType match {
      case SelfEmployment => IncomeTaxViewChangeFrontend.getCeaseSECannotGoBack()
      case UkProperty => IncomeTaxViewChangeFrontend.getCeaseUKCannotGoBack()
      case ForeignProperty => IncomeTaxViewChangeFrontend.getCeaseFPCannotGoBack()
    }

    result should have(
      httpStatus(OK),
      pageTitleIndividual(s"$expectedTitle")
    )
  }

  def runRedirectTest(incomeSourceType: IncomeSourceType): Assertion = {
    disable(IncomeSources)
    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

    val result: WSResponse = incomeSourceType match {
      case SelfEmployment => IncomeTaxViewChangeFrontend.getCeaseSECannotGoBack()
      case UkProperty => IncomeTaxViewChangeFrontend.getCeaseUKCannotGoBack()
      case ForeignProperty => IncomeTaxViewChangeFrontend.getCeaseFPCannotGoBack()
    }

    val expectedRedirect: String = controllers.routes.HomeController.show().url

    result should have(
      httpStatus(SEE_OTHER),
      redirectURI(expectedRedirect)
    )
  }


  s"calling GET ${url(UkProperty)}" should {
    "return 200 OK" when {
      "FS enabled - UK Property" in {
        runOKTest(UkProperty)
      }
    }
    "return 303 SEE_OTHER" when {
      "FS disabled - UK Property" in {
        runRedirectTest(UkProperty)
      }
    }
  }
  s"calling GET ${url(ForeignProperty)}" should {
    "return 200 OK" when {
      "FS enabled - Foreign Property" in {
        runOKTest(ForeignProperty)
      }
    }
    "return 303 SEE_OTHER" when {
      "FS disabled - Foreign Property" in {
        runRedirectTest(ForeignProperty)
      }
    }
  }
  s"calling GET ${url(SelfEmployment)}" should {
    "return 200 OK" when {
      "FS enabled - Self Employment" in {
        runOKTest(SelfEmployment)
      }
    }
    "return 303 SEE_OTHER" when {
      "FS disabled - Self Employment" in {
        runRedirectTest(SelfEmployment)
      }
    }
  }

}