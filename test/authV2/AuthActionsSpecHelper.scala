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

import audit.AuditingService
import config.{AgentItvcErrorHandler, FrontendAppConfig, FrontendAuthConnector, ItvcErrorHandler}
import controllers.bta.BtaNavBarController
import org.mockito.Mockito
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar.mock
import services.admin.FeatureSwitchService
import services.{IncomeSourceDetailsService, SessionDataService}
import testUtils.TestSupport
import views.html.navBar.PtaPartial

trait AuthActionsSpecHelper extends TestSupport with ScalaFutures {

  lazy val mockAuthConnector = mock[FrontendAuthConnector]
  lazy val mockIncomeSourceDetailsService = mock[IncomeSourceDetailsService]

  lazy val mockAppConfig = mock[FrontendAppConfig]
  lazy val mockAuditingService = mock[AuditingService]
  lazy val mockSessionDataService = mock[SessionDataService]
  lazy val mockItvcErrorHandler = mock[ItvcErrorHandler]

  lazy val mockBtaNavBarController = mock[BtaNavBarController]
  lazy val mockPtaPartial = mock[PtaPartial]
  lazy val mockFeatureSwitchService = mock[FeatureSwitchService]
  lazy val mockAgentErrorHandler = mock[AgentItvcErrorHandler]

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(mockAuthConnector)
    Mockito.reset(mockIncomeSourceDetailsService)
    Mockito.reset(mockAuditingService)
  }

}
