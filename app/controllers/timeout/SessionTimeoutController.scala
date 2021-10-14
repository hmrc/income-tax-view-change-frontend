/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.timeout

import com.google.inject.{Inject, Singleton}
import config.FrontendAppConfig
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.timeout.Timeout

import scala.concurrent.Future

@Singleton
class SessionTimeoutController @Inject()(val timeoutView: Timeout)
                                        (implicit val config: FrontendAppConfig,
                                         mcc: MessagesControllerComponents
                                        ) extends FrontendController(mcc) {

  val timeout: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(timeoutView()).withNewSession)
  }

  val keepAlive: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok("OK").withSession(request.session))
  }

}
