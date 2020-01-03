/*
 * Copyright 2020 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}

import config.FrontendAppConfig
import controllers.predicates._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}

@Singleton
class BTAPartialController @Inject()(implicit val config: FrontendAppConfig,
                                     implicit val messagesApi: MessagesApi,
                                     val checkSessionTimeout: SessionTimeoutPredicate,
                                     val authenticate: AuthenticationPredicate
                                    ) extends BaseController {

  val setupPartial: Action[AnyContent] = (checkSessionTimeout andThen authenticate) {
    implicit request => Ok(views.html.btaPartial())
  }

}
