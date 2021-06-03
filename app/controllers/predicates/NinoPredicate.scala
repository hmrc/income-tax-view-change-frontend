/*
 * Copyright 2021 HM Revenue & Customs
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


import audit.AuditingService
import audit.models.NinoLookupAuditing.{NinoLookupAuditModel, NinoLookupErrorAuditModel}
import auth.{MtdItUserOptionNino, MtdItUserWithNino}
import config.ItvcErrorHandler
import models.core.{Nino, NinoResponseError}
import play.api.Logger
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}
import services.NinoLookupService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class NinoPredicate @Inject()(val ninoLookupService: NinoLookupService,
                              val itvcErrorHandler: ItvcErrorHandler,
                              val auditingService: AuditingService)(
                               implicit val executionContext: ExecutionContext) extends ActionRefiner[MtdItUserOptionNino, MtdItUserWithNino] {

  override def refine[A](request: MtdItUserOptionNino[A]): Future[Either[Result, MtdItUserWithNino[A]]] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
    implicit val req: MtdItUserOptionNino[A] = request

    def buildMtdUserWithNino(nino: String) = MtdItUserWithNino(request.mtditid, nino, request.userName,
      request.saUtr, request.credId, request.userType, None)

    (request.nino, request.session.get("nino")) match {
      case (Some(nino), _) =>
        Logger.debug(s"[NinoPredicate][buildMtdUserWithNino] NINO retrieved from request")
        Future.successful(Right(buildMtdUserWithNino(nino)))
      case (_, Some(nino)) =>
        Logger.debug(s"[NinoPredicate][buildMtdUserWithNino] NINO retrieved from stored session")
        Future.successful(Right(buildMtdUserWithNino(nino)))
      case (_, _) =>
        Logger.debug(s"[NinoPredicate][buildMtdUserWithNino] NINO not found for user.  Requesting from NinoLookupService")
        ninoLookupService.getNino(request.mtditid).map {
          case nino: Nino =>
            Logger.debug(s"[NinoPredicate][buildMtdUserWithNino] NINO retrieved from NinoLookupService")
            auditNinoLookup(nino, request.mtditid)
            Left(Redirect(request.uri).addingToSession("nino" -> nino.nino))
          case error: NinoResponseError =>
            Logger.error(s"[NinoPredicate][buildMtdUserWithNino] NINO could not be retrieved from NinoLookupService")
            auditNinoLookupError(error, request.mtditid)
            Left(itvcErrorHandler.showInternalServerError)
        }
    }
  }

  private def auditNinoLookup(nino: Nino, mtdRef: String)(implicit hc: HeaderCarrier): Unit =
    auditingService.audit(NinoLookupAuditModel(nino, mtdRef))

  private def auditNinoLookupError(ninoError: NinoResponseError, mtdRef: String)(implicit hc: HeaderCarrier): Unit =
    auditingService.audit(NinoLookupErrorAuditModel(ninoError, mtdRef))
}
