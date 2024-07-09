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

package services.admin

import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import models.admin.{FeatureSwitch, FeatureSwitchName}
import play.api.Logger
import play.api.mvc.MessagesControllerComponents
import repositories.admin.FeatureSwitchRepository
import uk.gov.hmrc.auth.core.AuthorisedFunctions

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FeatureSwitchService @Inject()(
                                      val featureSwitchRepository: FeatureSwitchRepository,
                                      val authorisedFunctions: AuthorisedFunctions,
                                      val appConfig: FrontendAppConfig)
                                    (implicit val ec: ExecutionContext,
                                     mcc: MessagesControllerComponents,
                                     implicit val itvcErrorHandler: ItvcErrorHandler,
                                     implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler) extends ClientConfirmedController with FeatureSwitching {

  def get(featureSwitchName: FeatureSwitchName): Future[FeatureSwitch] =
    featureSwitchRepository
      .getFeatureSwitch(featureSwitchName)
      .map(_.getOrElse(FeatureSwitch(featureSwitchName, false)))


  def getAll: Future[List[FeatureSwitch]] = {

    Logger("application").info(s"reading FSS - ${appConfig.readFeatureSwitchesFromMongo}")
    if (appConfig.readFeatureSwitchesFromMongo) {
      // TODO: do we need to apply fallback in case can not connect to mongoDb?
      featureSwitchRepository.getFeatureSwitches.map { mongoSwitches =>
        Logger("application").debug(s"reading FSS: ${mongoSwitches}")
        FeatureSwitchName.allFeatureSwitches
          .foldLeft(mongoSwitches) { (featureSwitches, missingSwitch) =>
            if (featureSwitches.map(_.name).contains(missingSwitch))
              featureSwitches
            else
              FeatureSwitch(missingSwitch, false) :: featureSwitches
          }
          .reverse
      }
    } else {
      Future.successful(getFSList)
    }
  }

  def set(featureSwitchName: FeatureSwitchName, enabled: Boolean): Future[Boolean] = {
    Logger("application").info(s"Setting feature switch ${featureSwitchName.name} to ${enabled.toString}")
    if (appConfig.readFeatureSwitchesFromMongo) {
      featureSwitchRepository.setFeatureSwitch(featureSwitchName, enabled)
    } else {
      setFS(featureSwitchName, enabled)
      Future.successful(true)
    }
  }

  def setAll(featureSwitches: Map[FeatureSwitchName, Boolean]): Future[Unit] = {
    Logger("application").info(s"Setting all feature switches. FS values: $featureSwitches")
    if (appConfig.readFeatureSwitchesFromMongo) {
      featureSwitchRepository.setFeatureSwitches(featureSwitches)
    } else {
      featureSwitches.foreach(featureSwitch =>
        setFS(featureSwitch._1, featureSwitch._2)
      )
      Future.successful((): Unit)
    }
  }

}