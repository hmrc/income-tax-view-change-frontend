/*
 * Copyright 2022 HM Revenue & Customs
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

import auth.{MtdItUser, MtdItUserWithNino}
import config.ItvcErrorHandler
import controllers.BaseController
import models.incomeSourceDetails.IncomeSourceDetailsModel
import play.api.mvc.{ActionRefiner, MessagesControllerComponents, Result}
import services.IncomeSourceDetailsService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.http.HeaderNames
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeSourceDetailsPredicate @Inject()(val incomeSourceDetailsService: IncomeSourceDetailsService,
                                             val itvcErrorHandler: ItvcErrorHandler)
                                            (implicit val executionContext: ExecutionContext,
                                             mcc: MessagesControllerComponents) extends BaseController with
  ActionRefiner[MtdItUserWithNino, MtdItUser] {

  override def refine[A](request: MtdItUserWithNino[A]): Future[Either[Result, MtdItUser[A]]] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    implicit val req: MtdItUserWithNino[A] = request

    val sessionId = request.headers.get(HeaderNames.xSessionId).getOrElse("")
    val cacheKey = s"${sessionId + request.nino}-incomeSources"
    incomeSourceDetailsService.getIncomeSourceDetails(Some(cacheKey)) map {
      case sources: IncomeSourceDetailsModel =>
        Right(MtdItUser(request.mtditid, request.nino, request.userName, sources, None, request.saUtr, request.credId, request.userType, request.arn))
      case _ => Left(itvcErrorHandler.showInternalServerError)
    }

  }
}