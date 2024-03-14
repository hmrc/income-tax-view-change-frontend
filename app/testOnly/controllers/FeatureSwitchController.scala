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
import config.featureswitch.FeatureSwitching
import models.admin.{ FeatureSwitchName}
import models.admin.FeatureSwitchName.allFeatureSwitches

import javax.inject.{Inject, Singleton}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.admin.FeatureSwitchService
import testOnly.views.html.FeatureSwitchView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class FeatureSwitchController @Inject()(featureSwitchView: FeatureSwitchView,
                                        featureSwitchService: FeatureSwitchService)
                                       (implicit mcc: MessagesControllerComponents,
                                        val appConfig: FrontendAppConfig)
  extends FrontendController(mcc) with FeatureSwitching with I18nSupport {

  val ENABLE_ALL_FEATURES: String = "feature-switch.enable-all-switches"
  val DISABLE_ALL_FEATURES: String = "feature-switch.disable-all-switches"

//  private def view(switchNames: Map[FeatureSwitchName, Boolean])(implicit request: Request[_]): Html = {
//    featureSwitchView(
//      switchNames = switchNames,
//      testOnly.controllers.routes.FeatureSwitchController.submit
//    )
//  }

  def setSwitch(featureFlagName: FeatureSwitchName, isEnabled: Boolean): Action[AnyContent] = Action.async { request =>
    featureSwitchService.set(featureFlagName, isEnabled).map {
      case true => Ok(s"Flag $featureFlagName set to $isEnabled")
      case false => InternalServerError(s"Error while setting flag $featureFlagName to $isEnabled")
    }
  }

//  lazy val show: Action[AnyContent] = Action { implicit req =>
//    val featureSwitches = ListMap(allFeatureSwitches.toSeq sortBy (_.name) map (switch => switch -> isEnabled(switch)): _*)
//    Ok(view(featureSwitches))
//  }

  lazy val submit: Action[AnyContent] = Action { implicit req =>

    val submittedData: Set[String] = req.body.asFormUrlEncoded match {
      case None => Set.empty
      case Some(data) => data.keySet
    }

    val featureSwitches = submittedData flatMap FeatureSwitchName.get

    allFeatureSwitches.foreach(fs =>
      if (submittedData.contains(ENABLE_ALL_FEATURES))
        enable(fs)
      else if (submittedData.contains(DISABLE_ALL_FEATURES))
        disable(fs)
      else if (featureSwitches.contains(fs))
        enable(fs)
      else
        disable(fs)
    )

    Ok("OK...")
    //Redirect(testOnly.controllers.routes.FeatureSwitchController.show)
  }

}