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

package controllers.predicates


import auth.MtdItUser
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitch
import play.api.i18n.MessagesApi
import play.api.mvc._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FeatureSwitchPredicate @Inject()
                               (implicit val appConfig: FrontendAppConfig,
                                val executionContext: ExecutionContext,
                                val messagesApi: MessagesApi
                               ) extends ActionRefiner[MtdItUser, MtdItUser] with SaveOriginAndRedirect {

  override def refine[A](request: MtdItUser[A]): Future[Either[Result, MtdItUser[A]]] = {
    // Read fss: either from config like below Or from the storage
    val fss = FeatureSwitch.switches.foldLeft( List[FeatureSwitch]() ){ (acc, curr) =>
      if (isEnabled(curr)){
        acc :+ curr
      } else {
        acc
      }
    }

    // construct new request with a list of FS
    val newRequest : MtdItUser[A] = MtdItUser[A](
      mtditid = request.mtditid,
      nino = request.nino,
      userName = request.userName,
      incomeSources = request.incomeSources,
      btaNavPartial = request.btaNavPartial,
      saUtr = request.saUtr,
      credId = request.credId,
      userType = request.userType,
      arn = request.arn,
      featureSwitches = Some(fss))(request)

    Future.successful{
      Right(newRequest)
    }
  }

}

