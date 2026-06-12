/*
 * Copyright 2026 HM Revenue & Customs
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

package common.auth.actions

import common.auth.MtdItUser
import common.config.FrontendAppConfig
import common.config.featureswitch.FeatureSwitching
import common.controllers.routes as appRoutes
import common.models.admin.NoIncomeSourcesRedirect
import play.api.Logger
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RedirectIfNoIncomeSourcesAction @Inject()(frontendAppConfig: FrontendAppConfig)
                                     (implicit val executionContext: ExecutionContext)
  extends ActionRefiner[MtdItUser, MtdItUser] with FeatureSwitching {

  override val appConfig: FrontendAppConfig = frontendAppConfig

  private val logger = Logger("application")

  override protected def refine[A](request: MtdItUser[A]): Future[Either[Result, MtdItUser[A]]] = {
    implicit val req: MtdItUser[A] = request

    if (isEnabled(NoIncomeSourcesRedirect)) {
      if (req.incomeSources.hasAnyIncomeSources) {
        Future.successful(Right(req))
      } else {
        logger.info(
          s"[RedirectIfNoIncomeSourcesAction][refine] User has no income sources. Redirecting to no income sources page. isAgent=${req.isAgent}"
        )
        Future.successful(Left(Redirect(appRoutes.NoIncomeSourcesController.show(req.isAgent))))
      }
    } else {
      Future.successful(Right(req))
    }
  }
}