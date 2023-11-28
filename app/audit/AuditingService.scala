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

package audit

import audit.models.{AuditModel, ExtendedAuditModel}
import config.FrontendAppConfig
import play.api.Logger
import play.api.libs.json.{JsObject, JsValue, Json, Writes}
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions
import uk.gov.hmrc.play.audit.http.connector.AuditResult.{Disabled, Failure, Success}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.{DataEvent, ExtendedDataEvent}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class AuditingService @Inject()(appConfig: FrontendAppConfig, auditConnector: AuditConnector)  {

//  implicit val extendedDataEventWrites: Writes[ExtendedDataEvent] = Json.writes[ExtendedDataEvent]
//  implicit val dataEventWrites: Writes[DataEvent] = Json.writes[DataEvent]

  def audit(auditModel: AuditModel, path: Option[String] = None)(implicit hc: HeaderCarrier, request: Request[_], ec: ExecutionContext): Unit = {
    val dataEvent = toDataEvent(appConfig.appName, auditModel, path.fold(request.path)(x => x))
//    Logger("application").debug(s"Splunk Audit Event:\n\n${Json.toJson(dataEvent)}")
    auditConnector.sendEvent(dataEvent).map {
      case Success =>
        Logger("application").debug("[AuditingService][audit] - Splunk Audit Successful")
      case Failure(err, _) =>
        Logger("application").debug(s"[AuditingService][audit] - Splunk Audit Error, message: $err")
      case Disabled =>
        Logger("application").debug(s"[AuditingService][audit] - Auditing Disabled")
    }
  }

  def toDataEvent(appName: String, auditModel: AuditModel, path: String)(implicit hc: HeaderCarrier): DataEvent =
    DataEvent(
      auditSource = appName,
      auditType = auditModel.auditType,
      tags = AuditExtensions.auditHeaderCarrier(hc).toAuditTags(auditModel.transactionName, path),
      detail = AuditExtensions.auditHeaderCarrier(hc).toAuditDetails(auditModel.detail: _*)
    )


  def extendedAudit(auditModel: ExtendedAuditModel, path: Option[String] = None)(implicit hc: HeaderCarrier,
                                                                                 request: Request[_],
                                                                                 ec: ExecutionContext): Unit = {
    val extendedDataEvent = toExtendedDataEvent(appConfig.appName, auditModel, path.fold(request.path)(x => x))
//    Logger("application").debug(s"Splunk Audit Event:\n\n${Json.toJson(extendedDataEvent)}")
    auditConnector.sendExtendedEvent(extendedDataEvent).map {
      case Success =>
        Logger("application").debug("[AuditingService][extendedAudit] - Splunk Audit Successful")
      case Failure(err, _) =>
        Logger("application").debug(s"[AuditingService][extendedAudit] - Splunk Audit Error, message: $err")
      case Disabled =>
        Logger("application").debug(s"[AuditingService][extendedAudit] - Auditing Disabled")
    }
  }

  def toExtendedDataEvent(appName: String, auditModel: ExtendedAuditModel, path: String)(implicit hc: HeaderCarrier): ExtendedDataEvent = {

    val details: JsValue =
      Json.toJson(AuditExtensions.auditHeaderCarrier(hc).toAuditDetails()).as[JsObject].deepMerge(auditModel.detail.as[JsObject])

    ExtendedDataEvent(
      auditSource = appName,
      auditType = auditModel.auditType,
      tags = AuditExtensions.auditHeaderCarrier(hc).toAuditTags(auditModel.transactionName, path),
      detail = details
    )
  }
}
