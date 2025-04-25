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
import models.admin.FeatureSwitchName.allFeatureSwitches
import models.admin.{FeatureSwitchName, InvalidFS}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.admin.FeatureSwitchService
import testOnly.views.html.FeatureSwitchView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FeatureSwitchController @Inject()(featureSwitchView: FeatureSwitchView,
                                        featureSwitchService: FeatureSwitchService)
                                       (implicit mcc: MessagesControllerComponents,
                                        val appConfig: FrontendAppConfig,
                                        ec: ExecutionContext)
  extends FrontendController(mcc) with FeatureSwitching with I18nSupport {

  val ENABLE_ALL_FEATURES: String = "feature-switch.enable-all-switches"
  val DISABLE_ALL_FEATURES: String = "feature-switch.disable-all-switches"
  val PROD_FEATURES: String = "feature-switch.prod-switches"

  def setSwitch(featureFlagName: FeatureSwitchName, isEnabled: Boolean): Action[AnyContent] = Action.async { _ =>
    featureSwitchService.set(featureFlagName, isEnabled).map {
      case true =>
        Logger("application").info(s"Set FSS - $FeatureSwitchName - $isEnabled: result success")
        Ok(s"Flag $featureFlagName set to $isEnabled")
      case false =>
        Logger("application").info(s"Set FSS - $FeatureSwitchName - $isEnabled: result failure")
        InternalServerError(s"Error while setting flag $featureFlagName to $isEnabled")
    }
  }

  def show(): Action[AnyContent] = Action.async { implicit user =>
    featureSwitchService.getAll.flatMap { featureSwitches =>
      val fss = featureSwitches.filter(_.name.name != InvalidFS.name).map(x => {
        (FeatureSwitchName.allFeatureSwitches.find(_.name == x.name.name).get -> x.isEnabled)
      }).toMap
      Future.successful(
        Ok(
          featureSwitchView(
            switchNames = fss,
            testOnly.controllers.routes.FeatureSwitchController.submit()
          )
        )
      )
    }
  }

  // TODO: refactor next method
  def submit(): Action[AnyContent] = Action.async { implicit request =>

    val submittedData: Set[String] = request.body.asFormUrlEncoded match {
      case None => Set.empty
      case Some(data) => data.keySet
    }

    //Team is in agreement that environments in app configs will be exactly as in prod, and we use test-only/feature-switch to change FS.
    val prodEnabledFsList: Set[String] = featureSwitchService.getFSListFromConfig.filter(_.isEnabled).map(_.name.name).toSet

    def getEnabledFeatureSwitches: Map[FeatureSwitchName, Boolean] = {
      val subData: Set[String] =
        submittedData match {
          case _ if submittedData.contains(DISABLE_ALL_FEATURES) => Set.empty
          case _ if submittedData.contains(ENABLE_ALL_FEATURES) => allFeatureSwitches.map(_.name)
          case _ if submittedData.contains(PROD_FEATURES) => prodEnabledFsList
          case _ => submittedData
        }
      subData.map(x => allFeatureSwitches.find(e => e.name == x)).collect {
        case Some(fs) => fs
      }.map(x => x -> true).toMap
    }

    def getDisabledFeatureSwitches: Map[FeatureSwitchName, Boolean] = {
      val subData: Set[String] =
        submittedData match {
          case _ if submittedData.contains(ENABLE_ALL_FEATURES) => Set.empty
          case _ if submittedData.contains(DISABLE_ALL_FEATURES) => allFeatureSwitches.map(_.name)
          case _ if submittedData.contains(PROD_FEATURES) => allFeatureSwitches.map(_.name) diff prodEnabledFsList
          case _ => allFeatureSwitches.map(_.name) diff submittedData
        }

      subData.map(x => allFeatureSwitches.find(e => e.name == x)).collect {
        case Some(fs) => fs
      }.map(x => x -> false).toMap
    }

    // TODO: might worth to use setAll method from relevant repo (transactional approach?)
    for {
      _ <- Future.sequence(
        for {
          (fs, enableState) <- (getEnabledFeatureSwitches ++ getDisabledFeatureSwitches)
        } yield featureSwitchService.set(fs, enableState)
      )
    } yield Redirect(testOnly.controllers.routes.FeatureSwitchController.show())

  }

  def enableAll(): Action[AnyContent] = Action.async {
    for {
      featureSwitches <- featureSwitchService.getAll
      _ <- Future.sequence(
        featureSwitches.map(featureSwitch =>
          featureSwitchService.set(featureSwitch.name, enabled = true)
        )
      )
    } yield {
      Logger("application").info(s"Enabled all FSS")
      Redirect(testOnly.controllers.routes.FeatureSwitchController.show())
    }
  }
}