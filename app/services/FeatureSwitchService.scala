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

import models.admin.{FeatureSwitch, FeatureSwitchName}

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{Duration, FiniteDuration, SECONDS => Seconds}
import scala.concurrent.{ExecutionContext, Future}
import play.api.cache.AsyncCacheApi
import repositories.admin.FeatureSwitchRepository


@Singleton
class FeatureSwitchService @Inject()(featureSwitchRepository: FeatureSwitchRepository,
                                     cache: AsyncCacheApi)
                                    (implicit ec: ExecutionContext) {
  val cacheValidFor: FiniteDuration = Duration(2, Seconds)
  private val allFeatureSwitchesCacheKey = "*$*$allFeatureSwitches*$*$"

  def get(featureSwitchName: FeatureSwitchName): Future[FeatureSwitch] =
    cache.getOrElseUpdate(featureSwitchName.name, cacheValidFor) {
      featureSwitchRepository
        .getFeatureSwitch(featureSwitchName)
        .map(_.getOrElse(FeatureSwitch(featureSwitchName, false)))
    }

  def getAll: Future[List[FeatureSwitch]] =
    cache.getOrElseUpdate(allFeatureSwitchesCacheKey, cacheValidFor) {
      featureSwitchRepository.getFeatureSwitches.map { mongoSwitches =>
        FeatureSwitchName.allFeatureSwitches
          .foldLeft(mongoSwitches) { (featureSwitches, missingSwitch) =>
            if (featureSwitches.map(_.name).contains(missingSwitch))
              featureSwitches
            else
              FeatureSwitch(missingSwitch, false) :: featureSwitches
          }
          .reverse
      }
    }

  def set(featureSwitchName: FeatureSwitchName, enabled: Boolean): Future[Boolean] =
    for {
      _ <- cache.remove(featureSwitchName.name)
      _ <- cache.remove(allFeatureSwitchesCacheKey)
      result <- featureSwitchRepository.setFeatureSwitch(featureSwitchName, enabled)
      //blocking thread to allow other containers to update their cache
      _ <- Future.successful(Thread.sleep(5000))
    } yield result

  def setAll(featureSwitches: Map[FeatureSwitchName, Boolean]): Future[Unit] =
    Future
      .sequence(featureSwitches.keys.map(featureSwitch => cache.remove(featureSwitch.name)))
      .flatMap { _ =>
        cache.remove(allFeatureSwitchesCacheKey)
        featureSwitchRepository.setFeatureSwitches(featureSwitches)
      }
      .map { _ =>
        //blocking thread to allow other containers to update their cache
        Thread.sleep(5000)
        ()
      }

}