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

package common.auth.actions

import common.auth.MtdItUser
import common.auth.actions.AuthActionsTestData.*
import common.config.{AgentItvcErrorHandler, ItvcErrorHandler}
import common.connectors.ITSAStatusConnector
import common.models.admin.FeatureSwitchName
import common.models.itsaStatus.ITSAStatusResponseModel
import common.services.DateServiceInterface
import common.testUtils.TestSupport
import org.mockito.ArgumentMatchers.{any, anyBoolean}
import org.mockito.Mockito.{never, reset, verify}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{MessagesControllerComponents, Result}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent

import scala.concurrent.Future

class ItsaStatusRetrievalActionSpec extends TestSupport with ScalaFutures {

  lazy val mockItsaStatusConnector: ITSAStatusConnector = mock[ITSAStatusConnector]
  lazy val mockDateServiceInterface: DateServiceInterface = mock[DateServiceInterface]

  override lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure("play.http.router" -> "testOnlyDoNotUseInAppConf.Routes")
      .build()

  lazy val mcc = app.injector.instanceOf[MessagesControllerComponents]

  lazy val itvcErrorHandler: ItvcErrorHandler =
    app.injector.instanceOf[ItvcErrorHandler]

  lazy val agentErrorHandler: AgentItvcErrorHandler =
    app.injector.instanceOf[AgentItvcErrorHandler]

  def actionWithSwitch(enabledSwitches: Set[FeatureSwitchName]): ItsaStatusRetrievalAction =
    new ItsaStatusRetrievalAction(
      appConfig,
      mockItsaStatusConnector,
      mockDateServiceInterface
    )(
      ec,
      itvcErrorHandler,
      agentErrorHandler,
      mcc
    ) {
      override def isEnabled(featureSwitch: FeatureSwitchName)(implicit user: MtdItUser[_]): Boolean =
        enabledSwitches.contains(featureSwitch)
    }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockItsaStatusConnector)
  }

  ".refine()" should {

      "return the request unmodified" in {

        val action = actionWithSwitch(Set.empty)

        val mtdAgentUser = getMtdItUser(Agent)

        val result: Either[Result, MtdItUser[Any]] = action.refine(mtdAgentUser).futureValue

        result.foreach { user =>
          user shouldBe mtdAgentUser
        }

        verify(mockItsaStatusConnector, never())
          .getITSAStatusDetail(any(), any(), anyBoolean(), anyBoolean())(any())
      }
  }
}
