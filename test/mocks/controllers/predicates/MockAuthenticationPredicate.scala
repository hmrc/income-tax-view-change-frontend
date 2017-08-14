/*
 * Copyright 2017 HM Revenue & Customs
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

import mocks.auth._
import config.FrontendAppConfig
import controllers.predicates.AuthenticationPredicate
import play.api.i18n.MessagesApi
import play.api.{Configuration, Environment}
import utils.TestSupport
import mocks.connectors.MockUserDetailsConnector

trait MockAuthenticationPredicate extends TestSupport with MockUserDetailsConnector with MockFrontendAuthorisedFunctions {

  object MockAuthenticationPredicate extends AuthenticationPredicate(
    mockAuthService,
    app.injector.instanceOf[FrontendAppConfig],
    app.injector.instanceOf[Configuration],
    app.injector.instanceOf[Environment],
    app.injector.instanceOf[MessagesApi],
    mockUserDetailsConnector
  )

}