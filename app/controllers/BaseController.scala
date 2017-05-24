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

package controllers

import javax.inject.Inject

import config.{FrontendAppConfig, FrontendAuthConnector}
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

trait BaseController extends FrontendController

class AuthorisedAction @Inject()(config: FrontendAppConfig,
                       val authConnector: FrontendAuthConnector) extends BaseController with AuthorisedFunctions {

  private type PlayRequest  = Request[AnyContent] => Result
  private type AsyncRequest = Request[AnyContent] => Future[Result]

  def async(action: AsyncRequest): Action[AnyContent] = {
    Action.async { implicit request =>
      authorised() {
        action(request)
      }
    }
  }
}
