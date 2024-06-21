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

package utils.claimToAdjust

import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import mocks.services.{MockClaimToAdjustService, MockPaymentOnAccountSessionService}
import services.{ClaimToAdjustService, PaymentOnAccountSessionService}
import testUtils.TestSupport

import scala.concurrent.ExecutionContext

class WithSessionAndPoaSpec extends TestSupport with MockPaymentOnAccountSessionService with MockClaimToAdjustService {

  val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val TestWithSessionAndPoa: WithSessionAndPoa = new WithSessionAndPoa {
    override val appConfig: FrontendAppConfig = mockAppConfig
    override val poaSessionService: PaymentOnAccountSessionService = mockPaymentOnAccountSessionService
    override val itvcErrorHandler: ItvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler]
    override val itvcErrorHandlerAgent: AgentItvcErrorHandler = app.injector.instanceOf[AgentItvcErrorHandler]
    override implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
    override val claimToAdjustService: ClaimToAdjustService = mockClaimToAdjustService
  }
}
