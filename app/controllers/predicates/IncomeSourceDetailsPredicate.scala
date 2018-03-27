/*
 * Copyright 2018 HM Revenue & Customs
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

import audit.AuditingService
import audit.models.{IncomeSourceDetailsRequestAuditModel, IncomeSourceDetailsResponseAuditModel}
import auth.{MtdItUser, MtdItUserWithNino}
import config.ItvcErrorHandler
import controllers.BaseController
import models.incomeSourcesWithDeadlines.{IncomeSourcesWithDeadlinesError, IncomeSourcesWithDeadlinesModel}
import play.api.i18n.MessagesApi
import play.api.mvc.{ActionRefiner, Result}
import services.IncomeSourceDetailsService
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.Future

@Singleton
class IncomeSourceDetailsPredicate @Inject()(implicit val messagesApi: MessagesApi,
                                             val incomeSourceDetailsService: IncomeSourceDetailsService,
                                             val itvcErrorHandler: ItvcErrorHandler
                                            ) extends BaseController with ActionRefiner[MtdItUserWithNino, MtdItUser] {

  override def refine[A](request: MtdItUserWithNino[A]): Future[Either[Result, MtdItUser[A]]] = {

    implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
    implicit val req = request

    incomeSourceDetailsService.getIncomeSourceDetails(request.mtditid, request.nino) map {
      case sources: IncomeSourcesWithDeadlinesModel => Right(MtdItUser(request.mtditid, request.nino, request.userDetails, sources))
      case IncomeSourcesWithDeadlinesError => Left(itvcErrorHandler.showInternalServerError)
    }
  }
}
