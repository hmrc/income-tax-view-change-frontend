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

package auth.authV2.actions

import auth.MtdItUser
import config.FrontendAppConfig
import play.api.i18n.MessagesApi
import play.api.mvc._
import services.admin.FeatureSwitchService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FeatureSwitchRetrievalAction @Inject()(
                                              val featureSwitchService: FeatureSwitchService
                                            )
                                            (
                                              implicit val appConfig: FrontendAppConfig,
                                              val executionContext: ExecutionContext,
                                              val messagesApi: MessagesApi
                                            ) extends ActionRefiner[MtdItUser, MtdItUser] {

  override def refine[A](request: MtdItUser[A]): Future[Either[Result, MtdItUser[A]]] = {
    featureSwitchService.getAll.map(fs =>
      Right(request.addFeatureSwitches(fs))
    )
  }
}

