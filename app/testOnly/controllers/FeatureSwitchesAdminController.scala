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

package controllers.admin

import models.admin.{FeatureSwitch, FeatureSwitchName}
import play.api.libs.json.{JsBoolean, Json}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import services.admin.FeatureSwitchServiceImpl

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FeatureSwitchesAdminController @Inject() (
                                                 featureSwitchService: FeatureSwitchServiceImpl,
                                                 cc: ControllerComponents
                                               )(implicit ec: ExecutionContext)
  extends AbstractController(cc) {

  def get: Action[AnyContent] = Action.async {
    featureSwitchService.getAll
      .map(switches => Ok(Json.toJson(switches)))
  }

  def put(featureSwitchName: FeatureSwitchName): Action[AnyContent] = Action.async { request =>
    request.body.asJson match {
      case Some(JsBoolean(enabled)) =>
        featureSwitchService
          .set(featureSwitchName, enabled)
          .map(_ => NoContent)
      case _                        =>
        Future.successful(BadRequest)
    }
  }

  def putAll: Action[AnyContent] = Action.async { request =>
    val switches = request.body.asJson
      .map(_.as[Seq[FeatureSwitch]])
      .getOrElse(Seq.empty)
      .map(switch => (switch.name -> switch.isEnabled))
      .toMap
    featureSwitchService.setAll(switches).map(_ => NoContent)
  }
}