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

package authV2

import auth.MtdItUser
import auth.authV2.actions.IncomeSourceRetrievalAction
import authV2.AuthActionsTestData._
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import enums.{MTDIndividual, MTDPrimaryAgent}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.Assertion
import play.api
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Results.InternalServerError
import play.api.mvc.{Result, Results}
import play.api.test.Helpers._
import services.IncomeSourceDetailsService

import scala.concurrent.Future

class IncomeSourceRetrievalActionSpec extends AuthActionsSpecHelper {

  override lazy val app: Application = {
    new GuiceApplicationBuilder()
      .overrides(
        api.inject.bind[IncomeSourceDetailsService].toInstance(mockIncomeSourceDetailsService),
        api.inject.bind[ItvcErrorHandler].toInstance(mockItvcErrorHandler),
        api.inject.bind[AgentItvcErrorHandler].toInstance(mockAgentErrorHandler)
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

  lazy val action = app.injector.instanceOf[IncomeSourceRetrievalAction]

  "refine" when {
    "Return the income source details" should {
      "return the expected IncomeSourceDetails" in {
        val authorisedAndEnrolledRequest = defaultAuthorisedAndEnrolledRequest(MTDPrimaryAgent, fakeRequestWithActiveSession)
        when(mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
          .thenReturn(Future.successful(defaultIncomeSourcesData))

        val result = action.invokeBlock(
          authorisedAndEnrolledRequest,
          defaultAsyncBody(_.incomeSources shouldBe defaultIncomeSourcesData)
        )

        status(result) shouldBe OK
        contentAsString(result) shouldBe "Successful"
      }
    }
    "Income source details are not returned" should {
      "Show internal server error for individual" in {
        val authorisedAndEnrolledRequest = defaultAuthorisedAndEnrolledRequest(MTDIndividual, fakeRequestWithActiveSession)
        when(mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
          .thenReturn(Future.successful(invalidIncomeSourceData))

        when(mockItvcErrorHandler.showInternalServerError()(any()))
          .thenReturn(InternalServerError("ERROR PAGE"))

        val result = action.invokeBlock(
          authorisedAndEnrolledRequest,
          defaultAsync
        )

        status(result) shouldBe INTERNAL_SERVER_ERROR
        contentAsString(result) shouldBe "ERROR PAGE"
      }
      "Show internal server error for agent" in {
        val authorisedAndEnrolledRequest = defaultAuthorisedAndEnrolledRequest(MTDPrimaryAgent, fakeRequestWithActiveSession)
        when(mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
          .thenReturn(Future.successful(invalidIncomeSourceData))

        when(mockAgentErrorHandler.showInternalServerError()(any()))
          .thenReturn(InternalServerError("ERROR PAGE"))

        val result = action.invokeBlock(
          authorisedAndEnrolledRequest,
          defaultAsync
        )
        status(result) shouldBe INTERNAL_SERVER_ERROR
        contentAsString(result) shouldBe "ERROR PAGE"

      }
    }
  }

}