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

package mocks.controllers.predicates

import audit.mocks.MockAuditingService
import auth.FrontEndHeaderExtractor
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{AuthenticationPredicate, NinoPredicate, SessionTimeoutPredicate}
import mocks.MockItvcErrorHandler
import mocks.auth._
import mocks.services.admin.MockFeatureSwitchService
import play.api.mvc.MessagesControllerComponents
import play.api.{Configuration, Environment}
import testUtils.TestSupport
import utils.AuthenticatorPredicate

trait MockAuthenticationPredicate extends TestSupport with MockFrontendAuthorisedFunctions
  with MockAuditingService with MockIncomeSourceDetailsPredicate with MockNavBarEnumFsPredicate with MockItvcErrorHandler
  with MockFeatureSwitchService with MockFeatureSwitchPredicate {

  val testAuthenticator = new AuthenticatorPredicate(checkSessionTimeout = app.injector.instanceOf[SessionTimeoutPredicate],
    authenticate = MockAuthenticationPredicate,
    featureSwitchService = featureSwitchService,
    authorisedFunctions = mockAuthService,
    retrieveBtaNavBar = MockNavBarPredicate,
    featureSwitchPredicate = FeatureSwitchPredicate,
    retrieveNinoWithIncomeSources = MockIncomeSourceDetailsPredicate,
    incomeSourceDetailsService = mockIncomeSourceDetailsService,
    retrieveNino = app.injector.instanceOf[NinoPredicate])(
    app.injector.instanceOf[MessagesControllerComponents], app.injector.instanceOf[FrontendAppConfig], mockItvcErrorHandler, ec)

  object MockAuthenticationPredicate extends AuthenticationPredicate()(
    ec,
    mockAuthService,
    app.injector.instanceOf[FrontendAppConfig],
    app.injector.instanceOf[Configuration],
    app.injector.instanceOf[Environment],
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[MessagesControllerComponents],
    mockAuditingService,
    app.injector.instanceOf[FrontEndHeaderExtractor]
  )


}
