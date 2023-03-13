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

package controllers.testOnly

import models.admin._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.admin.FeatureSwitchService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton()
class FeatureSwitchController @Inject() (
                                         mcc: MessagesControllerComponents,
                                         featureSwitchService: FeatureSwitchService
                                       )(implicit ec: ExecutionContext)
  extends FrontendController(mcc) {

  def setSwitch(featureSwitchName: FeatureSwitchName, isEnabled: Boolean): Action[AnyContent] = Action.async {
    featureSwitchService.set(featureSwitchName, isEnabled).map {
      case true  => Ok(s"Switch $featureSwitchName set to $isEnabled")
      case false => InternalServerError(s"Error while setting flag $featureSwitchName to $isEnabled")
    }
  }

  def setDefaults: Action[AnyContent] = Action {
    featureSwitchService.setAll(Map(MFACreditsAndDebits -> false, CutOverCredits -> false))
    Ok("Default switches set")
  }
}