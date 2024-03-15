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

import config.FrontendAppConfig
import models.admin.{FeatureSwitch, FeatureSwitchName}
import play.api.Logger

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{Duration, FiniteDuration, SECONDS => Seconds}
import scala.concurrent.{ExecutionContext, Future}
import play.api.cache.AsyncCacheApi
import repositories.admin.FeatureSwitchRepository
import play.api.Logger


@Singleton
class FeatureSwitchService @Inject()(
                                      val featureSwitchRepository: FeatureSwitchRepository,
                                      val appConfig: FrontendAppConfig)
                                    (implicit ec: ExecutionContext) {

  def get(featureSwitchName: FeatureSwitchName): Future[FeatureSwitch] =
      featureSwitchRepository
        .getFeatureSwitch(featureSwitchName)
        .map(_.getOrElse(FeatureSwitch(featureSwitchName, false)))


  def getAll: Future[List[FeatureSwitch]] = {

    Logger("application").info(s"[FeatureSwitchService][getAll] - reading FSS - ${appConfig.readFeatureSwitchesFromMongo}")
    if (appConfig.readFeatureSwitchesFromMongo) {
      featureSwitchRepository.getFeatureSwitches.map { mongoSwitches =>
        Logger("application").info(s"[FeatureSwitchService][getAll] - reading FSS: ${mongoSwitches}")
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
      Future.successful( List[FeatureSwitch]())
    }
  }

  def set(featureSwitchName: FeatureSwitchName, enabled: Boolean): Future[Boolean] =
    for {
      //_ <- cache.remove(featureSwitchName.name)
//      _ <- cache.remove(allFeatureSwitchesCacheKey)
      result <- featureSwitchRepository.setFeatureSwitch(featureSwitchName, enabled)
      //blocking thread to allow other containers to update their cache
      //_ <- Future.successful(Thread.sleep(5000))
    } yield result


  def setAll(featureSwitches: Map[FeatureSwitchName, Boolean]): Future[Unit] =
    featureSwitchRepository.setFeatureSwitches(featureSwitches)

}