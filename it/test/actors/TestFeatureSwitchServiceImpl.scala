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

package actors

import actors.FeatureSwitchActor.{GetFs, SetFs}
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import jakarta.inject.Singleton
import models.admin.FeatureSwitchName.allFeatureSwitches
import models.admin.{FeatureSwitch, FeatureSwitchName}
import org.apache.pekko.actor.{ActorRef, ActorSystem}
import org.apache.pekko.util.Timeout
import play.api.Logger
import play.api.mvc.MessagesControllerComponents
import services.admin.FeatureSwitchService
import uk.gov.hmrc.auth.core.AuthorisedFunctions

import javax.inject.Inject
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TestFeatureSwitchServiceImpl @Inject()(system: ActorSystem,
                                             val appConfig: FrontendAppConfig,
                                             val authorisedFunctions: AuthorisedFunctions)
                                            (implicit val ec: ExecutionContext,
                                             mcc: MessagesControllerComponents,
                                             implicit val itvcErrorHandler: ItvcErrorHandler,
                                             implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler) extends ClientConfirmedController
  with FeatureSwitching with FeatureSwitchService {
  import org.apache.pekko.pattern.ask
  implicit val timeout: Timeout = 1.seconds

  val featureSwitchActor: ActorRef = system.actorOf(FeatureSwitchActor.props, "featureSwitch-actor")

  override def get(featureSwitchName: FeatureSwitchName): Future[FeatureSwitch] = {
    Logger("application").info(s"GET FS22${featureSwitchName}")
//    val sysProps = sys.props.get(featureSwitchName.name).getOrElse("false")
//    if (sysProps.nonEmpty) {
//       Future{ FeatureSwitch(featureSwitchName, sysProps == "true") }
//    } else {
      (featureSwitchActor ? GetFs(featureSwitchName)).mapTo[FeatureSwitch]
//    }
  }

  override def getAll: Future[List[FeatureSwitch]] = {
    Future.sequence( allFeatureSwitches
      .toList.map(fsn => (featureSwitchActor ? GetFs(fsn))
        .mapTo[FeatureSwitch]) )
      .map(x => {
        Logger("application").info(s"GET FS33: ${x.mkString("-\n")}")
        x
      })
  }
  override def set(featureSwitchName: FeatureSwitchName, enabled: Boolean): Future[Boolean] = {
    Logger("application").info(s"GET FS - SET - ${featureSwitchName} - $enabled")
    val fs = FeatureSwitch(featureSwitchName, enabled)
    (featureSwitchActor ? SetFs(fs)).mapTo[Boolean]
  }

  override def setAll(featureSwitches: Map[FeatureSwitchName, Boolean]): Future[Unit] = {
    val r = Future.sequence( featureSwitches
      .toList.map(fsn => (featureSwitchActor ? Set(FeatureSwitch(fsn._1, fsn._2))).mapTo[FeatureSwitch]) )
      .map(_ => ())
    r
  }

}
