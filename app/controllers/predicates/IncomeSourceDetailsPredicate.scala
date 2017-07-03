/*
 * Copyright 2017 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}

import auth.MtdItUser
import controllers.BaseController
import models.IncomeSources
import play.api.i18n.MessagesApi
import play.api.mvc.{AnyContent, Request, Result}
import services.IncomeSourceDetailsService

import scala.concurrent.Future

@Singleton
class IncomeSourceDetailsPredicate @Inject()(implicit val messagesApi: MessagesApi,
                                             val incomeSourceDetailsService: IncomeSourceDetailsService
                                            ) extends BaseController {

  def retrieveIncomeSources(f: IncomeSources => Future[Result])(implicit request: Request[AnyContent], user: MtdItUser): Future[Result] = {
    incomeSourceDetailsService.getIncomeSourceDetails(user.nino).flatMap { sources =>
      f(sources)
    }
  }

}
