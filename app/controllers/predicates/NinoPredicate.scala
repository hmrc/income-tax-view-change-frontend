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

import auth.{MtdItUserOptionNino, MtdItUserWithNino}
import config.ItvcErrorHandler
import models.Nino
import play.api.mvc.{ActionRefiner, Result}
import play.api.mvc.Results.Redirect
import services.NinoLookupService
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

@Singleton()
class NinoPredicate @Inject()(val ninoLookupService: NinoLookupService,
                              val itvcErrorHandler: ItvcErrorHandler) extends ActionRefiner[MtdItUserOptionNino, MtdItUserWithNino] {

  override protected def refine[A](request: MtdItUserOptionNino[A]): Future[Either[Result, MtdItUserWithNino[A]]] = {

    implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    def buildMtdUserWithNino(nino: String) = MtdItUserWithNino(request.mtditid, nino, request.userDetails)(request)

    (request.nino, request.session.get("nino")) match {
      case (Some(nino), _) => Future.successful(Right(buildMtdUserWithNino(nino)))
      case (_, Some(nino)) => Future.successful(Right(buildMtdUserWithNino(nino)))
      case (_,_) => ninoLookupService.getNino(request.mtditid).map {
        case nino: Nino => Left(Redirect(request.uri).addingToSession("nino" -> nino.nino)(request))
        case _ => Left(itvcErrorHandler.showInternalServerError(request))
      }
    }
  }
}
