/*
 * Copyright 2024 HM Revenue & Customs
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

package auth.authV2

import audit.AuditingService
import auth.authV2.AuthActionsTestData._
import auth.authV2.actions._
import auth.{FrontendAuthorisedFunctions, MtdItUserOptionNino}
import config.FrontendAppConfig
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.Assertion
import play.api
import play.api.http.HeaderNames
import play.api.{Application, Play}
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.mvc.{Request, Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future

class AuthoriseAndRetrieveIndividualSpec extends AuthActionsSpecHelper {

  override def afterEach(): Unit = {
    Play.stop(fakeApplication())
    super.afterEach()
  }

  override def fakeApplication(): Application = {
    val frontendAuthFunctions = new FrontendAuthorisedFunctions(mockAuthConnector)

    new GuiceApplicationBuilder()
      .overrides(
        api.inject.bind[FrontendAuthorisedFunctions].toInstance(frontendAuthFunctions),
        api.inject.bind[AuditingService].toInstance(mockAuditingService)
      )
      .build()
  }

  def defaultAsyncBody(
                        requestTestCase: MtdItUserOptionNino[_] => Assertion
                      ): MtdItUserOptionNino[_] => Future[Result] = testRequest => {
    requestTestCase(testRequest)
    Future.successful(Results.Ok("Successful"))
  }

  lazy val authAction = fakeApplication().injector.instanceOf[AuthoriseAndRetrieveIndividual]

  "refine" should {
    "return the expected MtdItUserOptionNino response" when {
      "the user is an Individual enrolled into HMRC-MTD-IT with the required confidence level" that {
        "also has a name, nino and sa enrolment" in {

            when(mockAuthConnector.authorise[AuthRetrievals](any(), any())(any(), any())).thenReturn(
              Future.successful[AuthRetrievals](
                getAllEnrolmentsIndividual(true, true) ~ Some(userName) ~ Some(credentials) ~ Some(Individual) ~ confidenceLevel
              )
            )

            val result = authAction.invokeBlock(
              fakeRequestWithActiveSession,
              defaultAsyncBody(_ shouldBe getMtdItUserOptionNinoForAuthorise(true)(fakeRequestWithActiveSession)))

            result.map{res => println(res)}
            status(result) shouldBe OK
            contentAsString(result) shouldBe "Successful"
          }
      }
    }
  }
}
