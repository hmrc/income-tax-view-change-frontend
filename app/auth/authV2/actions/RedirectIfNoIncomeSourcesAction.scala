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

package auth.authV2.actions

import auth.MtdItUser
import play.api.Logger
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RedirectIfNoIncomeSourcesAction @Inject()()
                                     (implicit val executionContext: ExecutionContext)
  extends ActionRefiner[MtdItUser, MtdItUser] {

  private val logger = Logger("application")

  override protected def refine[A](request: MtdItUser[A]): Future[Either[Result, MtdItUser[A]]] =  {
    if (request.incomeSources.hasAnyIncomeSources) {
      Future.successful(Right(request))
    } else {

      logger.info(
        s"[RedirectIfNoIncomeSourcesAction][refine] User has no income sources. Redirecting to no income sources page. isAgent=${request.isAgent}"
      )

      Future.successful(
        Left(
          Redirect(controllers.routes.NoIncomeSourcesController.show(request.isAgent))
        )
      )
    }
  }
}