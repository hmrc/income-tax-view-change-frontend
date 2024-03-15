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
import models.admin.FeatureSwitchName
import models.admin.FeatureSwitchName.allFeatureSwitches
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.admin.FeatureSwitchService
import testOnly.views.html.FeatureSwitchView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class FeatureSwitchController @Inject()(featureSwitchView: FeatureSwitchView,
                                        featureSwitchService: FeatureSwitchService)
                                       (implicit mcc: MessagesControllerComponents,
                                        val appConfig: FrontendAppConfig)
  extends FrontendController(mcc) with FeatureSwitching with I18nSupport {

  val ENABLE_ALL_FEATURES: String = "feature-switch.enable-all-switches"
  val DISABLE_ALL_FEATURES: String = "feature-switch.disable-all-switches"


  def setSwitch(featureFlagName: FeatureSwitchName, isEnabled: Boolean): Action[AnyContent] = Action.async { request =>
    featureSwitchService.set(featureFlagName, isEnabled).map {
      case true => Ok(s"Flag $featureFlagName set to $isEnabled")
      case false => InternalServerError(s"Error while setting flag $featureFlagName to $isEnabled")
    }
  }

  def show(): Action[AnyContent] = Action.async { implicit user =>
    featureSwitchService.getAll.flatMap { featureSwitches =>
      val fss = featureSwitches.map(x => {
        (FeatureSwitchName.allFeatureSwitches.find(_.name == x.name.name).get -> x.isEnabled)
      }).toMap
      Future.successful(Ok(view(fss)))
    }

  }

  // TODO: refactor next method
  def submit(): Action[AnyContent] = Action.async { implicit request =>

    val submittedData: Set[String] = request.body.asFormUrlEncoded match {
      case None => Set.empty
      case Some(data) => data.keySet
    }

    def disableAll(): Boolean = submittedData.contains(DISABLE_ALL_FEATURES)

    def enableAll(): Boolean = submittedData.contains(ENABLE_ALL_FEATURES)

    val disabledFeatureSwitchers: Map[FeatureSwitchName, Boolean] = getDisabledFeatureSwitches(submittedData, disableAll())
    val enabledFeatureSwitchers: Map[FeatureSwitchName, Boolean] = if (disableAll()) {
      Map.empty
    } else {
      getEnabledFeatureSwitches(submittedData, enableAll())
    }

    for {
      _ <- Future.sequence(
        for {
          (fs, enableState) <- (disabledFeatureSwitchers ++ enabledFeatureSwitchers)
        } yield featureSwitchService.set(fs, enableState)
      )
    } yield Redirect(testOnly.controllers.routes.FeatureSwitchController.show())

  }

  private def getEnabledFeatureSwitches(submittedData: Set[String], enableAll: Boolean): Map[FeatureSwitchName, Boolean] = {
    val enabledFeatureSwitchers: Map[FeatureSwitchName, Boolean] = {
      if (enableAll) allFeatureSwitches.map(_.name) else submittedData
    }
      .map(x => allFeatureSwitches.find(e => e.name == x)).collect {
        case Some(fs) => fs
      }.map(x => (x -> true)).toMap
    enabledFeatureSwitchers
  }

  private def getDisabledFeatureSwitches(submittedData: Set[String], disableAll: Boolean): Map[FeatureSwitchName, Boolean] = {
    val disabledFeatureSwitchers: Map[FeatureSwitchName, Boolean] = {
      if (disableAll) {
        allFeatureSwitches.map(_.name)
      } else {
        allFeatureSwitches.map(_.name) diff submittedData
      }
    }.map(x => allFeatureSwitches.find(e => e.name == x)).collect {
      case Some(fs) => fs
    }.map(x => (x -> false)).toMap
    disabledFeatureSwitchers
  }

  private def view(switchNames: Map[FeatureSwitchName, Boolean])(implicit request: Request[_]): Html = {
    featureSwitchView(
      switchNames = switchNames,
      testOnly.controllers.routes.FeatureSwitchController.submit()
    )
  }
}