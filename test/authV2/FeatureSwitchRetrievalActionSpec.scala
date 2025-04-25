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
import auth.authV2.actions.FeatureSwitchRetrievalAction
import authV2.AuthActionsTestData._
import models.admin.{FeatureSwitch, NavBarFs}
import org.mockito.Mockito.when
import org.scalatest.Assertion
import play.api
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Result, Results}
import play.api.test.Helpers._
import services.admin.FeatureSwitchService
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

import scala.concurrent.Future

class FeatureSwitchRetrievalActionSpec extends AuthActionsSpecHelper {

  override lazy val app: Application = {
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

  lazy val action = app.injector.instanceOf[FeatureSwitchRetrievalAction]


  "refine" when {
    "The feature switches are retrieved" should {
      "Return list of feature switches" in {
        val featureSwitch = List(FeatureSwitch(NavBarFs, true))
        val mtdItUserRequest = getMtdItUser(Individual)(fakeRequestWithActiveSession)
        when(mockFeatureSwitchService.getAll)
          .thenReturn(Future(featureSwitch))

        val result = action.invokeBlock(
          mtdItUserRequest,
          defaultAsyncBody (_.featureSwitches shouldBe featureSwitch)
        )

        status(result) shouldBe OK
        contentAsString(result) shouldBe "Successful"
      }
      "Return empty list of feature switches" in {
        val mtdItUserRequest = getMtdItUser(Individual)(fakeRequestWithActiveSession)
        when(mockFeatureSwitchService.getAll)
          .thenReturn(Future(List.empty))
        val result = action.invokeBlock(
          mtdItUserRequest,
          defaultAsyncBody(_.featureSwitches shouldBe List.empty)
        )

        status(result) shouldBe OK
        contentAsString(result) shouldBe "Successful"
      }
    }
  }
}
