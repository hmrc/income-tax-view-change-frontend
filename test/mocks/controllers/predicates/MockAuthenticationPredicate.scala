/*
 * Copyright 2023 HM Revenue & Customs
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
import controllers.predicates.AuthenticationPredicate
import mocks.auth._
import play.api.i18n.MessagesApi
import play.api.mvc.MessagesControllerComponents
import play.api.{Configuration, Environment}
import testUtils.TestSupport

trait MockAuthenticationPredicate extends TestSupport with MockFrontendAuthorisedFunctions with MockAuditingService {

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
