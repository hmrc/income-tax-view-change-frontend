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

import com.google.inject.Inject
import config.AppConfig
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

import scala.concurrent.Future


class HelloWorld @Inject()( implicit val config: AppConfig,
                            val authorisedAction: AuthorisedAction
                          ) extends BaseController {

  val helloWorld = authorisedAction.async{ implicit request =>
    Future.successful(Ok(views.html.helloworld.hello_world(config)))
  }
}
