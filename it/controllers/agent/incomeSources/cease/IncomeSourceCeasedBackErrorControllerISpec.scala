package controllers.agent.incomeSources.cease

import config.featureswitch.IncomeSources
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Cease, JourneyType}
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import org.scalatest.Assertion
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, completedUIJourneySessionData, foreignPropertyOnlyResponse, ukPropertyOnlyResponse}

class IncomeSourceCeasedBackErrorControllerISpec extends ComponentSpecBase {

  val title = messagesAPI("cannotGoBack.heading")
  val headingSE = messagesAPI("cannotGoBack.sole-trader-ceased")
  val headingUk = messagesAPI("cannotGoBack.uk-property-ceased")
  val headingFP = messagesAPI("cannotGoBack.foreign-property-ceased")

  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Cease))
  }

  val url: IncomeSourceType => String = (incomeSourceType: IncomeSourceType) =>
    controllers.incomeSources.cease.routes.IncomeSourceCeasedBackErrorController.show(incomeSourceType).url

  def runOKTest(incomeSourceType: IncomeSourceType): Assertion = {
    stubAuthorisedAgentUser(authorised = true)
    enable(IncomeSources)
    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

    await(sessionService.setMongoData(completedUIJourneySessionData(JourneyType(Cease, incomeSourceType))))

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
      pageTitleAgent(s"$expectedTitle")
    )
  }

  def runRedirectTest(incomeSourceType: IncomeSourceType): Assertion = {
    stubAuthorisedAgentUser(authorised = true)
    disable(IncomeSources)
    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

    val result: WSResponse = incomeSourceType match {
      case SelfEmployment => IncomeTaxViewChangeFrontend.getCeaseSECannotGoBack()
      case UkProperty => IncomeTaxViewChangeFrontend.getCeaseUKCannotGoBack()
      case ForeignProperty => IncomeTaxViewChangeFrontend.getCeaseFPCannotGoBack()
    }

    val expectedRedirect: String = controllers.routes.HomeController.showAgent.url

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