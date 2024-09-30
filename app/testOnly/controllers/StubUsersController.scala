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

package testOnly.controllers

import config.FrontendAppConfig
import controllers.BaseController
import play.api.i18n.I18nSupport
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.api.{Configuration, Environment, Logger}
import testOnly.models.UserRecord
import testOnly.utils.UserRepository
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StubUsersController @Inject()(implicit val appConfig: FrontendAppConfig,
                                    override val config: Configuration,
                                    override val env: Environment,
                                    implicit val mcc: MessagesControllerComponents,
                                    implicit val executionContext: ExecutionContext,
                                    userRepository: UserRepository
                                   ) extends BaseController with AuthRedirects with I18nSupport {

  def stubUsers: Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[UserRecord](
      userRecord => {
        Logger("application").info("userRecord:" + userRecord)
        userRepository.addUser(userRecord).map { result =>
          if (result.wasAcknowledged()) {
            Ok("User upload success")
          } else {
            InternalServerError("Unable to upload user to database")
          }
        }
      }
    )
  }

  val deleteUsers: Action[AnyContent] = Action.async { implicit request =>
    userRepository.removeAll().flatMap(_ =>
      Future.successful(Ok("\nDeleted all mongo data from FE user collection"))
    )
  }

}
