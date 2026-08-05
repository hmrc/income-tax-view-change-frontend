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

package testOnly.services.admin

import common.config.FrontendAppConfig
import common.config.featureswitch.FeatureSwitching
import common.connectors.FeatureSwitchConnector
import common.models.admin.{FeatureSwitch, FeatureSwitchName}
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FeatureSwitchService @Inject()(val featureSwitchConnector: FeatureSwitchConnector,
                                     val appConfig: FrontendAppConfig)
                                    (implicit val ec: ExecutionContext) extends FeatureSwitching with Logging {

  def getAll()(implicit hc: HeaderCarrier): Future[List[FeatureSwitch]] = {
    featureSwitchConnector.getAllSwitches()
  }

  def set(featureSwitchName: FeatureSwitchName, enabled: Boolean)(implicit hc: HeaderCarrier): Future[Boolean] = {
    logger.info(s"Setting feature switch ${featureSwitchName.name} to ${enabled.toString}")
    if (appConfig.readFeatureSwitchesFromMongo) {
      featureSwitchConnector.setSwitch(featureSwitchName, enabled)
    } else {
      logger.error("[set] Cannot set feature switch when read-from-mongo is disabled")
      Future(false)
    }
  }

  def setAll(featureSwitches: Map[FeatureSwitchName, Boolean])(implicit hc: HeaderCarrier): Future[Unit] = {
    logger.info(s"Setting all feature switches. FS values: $featureSwitches")
    if (appConfig.readFeatureSwitchesFromMongo) {
      featureSwitchConnector.setSwitches(featureSwitches).map(_ => ())
    } else {
      logger.error("[setAll] Cannot set feature switches when read-from-mongo is disabled")
      Future.successful((): Unit)
    }
  }
  
  def resetToProd()(implicit hc: HeaderCarrier): Future[Boolean] = {
    logger.info("Resetting feature switches to production values")
    featureSwitchConnector.resetToProd()
  }

}