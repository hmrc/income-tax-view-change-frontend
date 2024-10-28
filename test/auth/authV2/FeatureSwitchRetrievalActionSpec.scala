package auth.authV2

import auth.MtdItUser
import auth.authV2.AuthActionsTestData.sessionGetSuccessResponse
import auth.authV2.actions.{ClientDataRequest, FeatureSwitchRetrievalAction}
import config.ItvcErrorHandler
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.Assertion
import play.api
import play.api.{Application, Play}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Result, Results}
import play.api.mvc.Results.InternalServerError
import play.api.test.Helpers.{contentAsString, redirectLocation, status}
import services.SessionDataService
import services.admin.FeatureSwitchService
import testOnly.models.SessionDataGetResponse.{SessionDataNotFound, SessionDataUnexpectedResponse}

import scala.concurrent.Future

class FeatureSwitchRetrievalActionSpec extends AuthActionsSpecHelper {

  override def afterEach(): Unit = {
    Play.stop(fakeApplication())
    super.afterEach()
  }

  override def fakeApplication(): Application = {
    new GuiceApplicationBuilder()
      .overrides(
        api.inject.bind[FeatureSwitchService].toInstance(mockFeatureSwitchService)
      )
      .build()
  }

  def defaultAsyncBody(
                        requestTestCase: MtdItUser[_] => Assertion
                      ): MtdItUser[_] => Future[Result] = testRequest => {
    requestTestCase(testRequest)
    Future.successful(Results.Ok("Successful"))
  }

  def defaultAsync: MtdItUser[_] => Future[Result] = (_) => Future.successful(Results.Ok("Successful"))

  lazy val action = fakeApplication().injector.instanceOf[FeatureSwitchRetrievalAction]

  "refine" when {


  }
}
