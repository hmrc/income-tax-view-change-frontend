/*
 * Copyright 2018 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}

import config.FrontendAppConfig
import controllers.BaseController
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Result}
import testOnly.forms.FeatureSwitchForm
import testOnly.models.FeatureSwitchModel

@Singleton
class FeatureSwitchController @Inject()(val messagesApi: MessagesApi, implicit val appConfig: FrontendAppConfig) extends BaseController {

  def featureSwitch: Action[AnyContent] = Action { implicit request =>
    Ok(testOnly.views.html.featureSwitch(FeatureSwitchForm.form.fill(
      FeatureSwitchModel(
        homePageEnabled = appConfig.features.homePageEnabled(),
        paymentEnabled = appConfig.features.paymentEnabled()
      )
    )))
  }

  def submitFeatureSwitch: Action[AnyContent] = Action { implicit request =>
    FeatureSwitchForm.form.bindFromRequest().fold(
      _ => Redirect(routes.FeatureSwitchController.featureSwitch()),
      success = handleSuccess
    )
  }

  def handleSuccess(model: FeatureSwitchModel): Result = {
    appConfig.features.homePageEnabled(model.homePageEnabled)
    appConfig.features.paymentEnabled(model.paymentEnabled)
    Redirect(routes.FeatureSwitchController.featureSwitch())
  }

}
