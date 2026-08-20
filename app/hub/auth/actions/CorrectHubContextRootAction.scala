/*
 * Copyright 2023 HM Revenue & Customs
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

package hub.auth.actions

import common.auth.RequestWithFeatureSwitches
import common.config.FrontendAppConfig
import play.api.Logging
import play.api.mvc.*
import play.api.mvc.Results.Redirect

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CorrectHubContextRootAction @Inject()(appConfig: FrontendAppConfig)
                                           (implicit val executionContext: ExecutionContext)
  extends ActionRefiner[RequestWithFeatureSwitches, RequestWithFeatureSwitches] with Logging {

  override def refine[A](request: RequestWithFeatureSwitches[A]): Future[Either[Result, RequestWithFeatureSwitches[A]]] = {

    if(request.newHubContextRootEnabled) {
      Future.successful(Right(request))
    } else {
      logger.warn("Incorrect Hub Context Root.")
      val newPath = request.path.replace(appConfig.basePath, appConfig.hubBasePath)
      Future.successful(Left(Redirect(newPath)))
    }
  }
}