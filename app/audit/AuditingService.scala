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

package audit

import javax.inject.{Inject, Singleton}

import audit.models.{AuditModel, ExtendedAuditModel}
import config.{FrontendAppConfig, FrontendAuditConnector}
import play.api.Logger
import play.api.libs.json.{JsObject, JsValue, Json, Writes}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.http.connector.AuditResult.{Disabled, Failure, Success}
import uk.gov.hmrc.play.audit.model.{DataEvent, ExtendedDataEvent}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuditingService @Inject()(appConfig: FrontendAppConfig, auditConnector: FrontendAuditConnector) {

  implicit val dataEventWrites: Writes[DataEvent] = Json.writes[DataEvent]
  implicit val extendedDataEventWrites: Writes[ExtendedDataEvent] = Json.writes[ExtendedDataEvent]

  def audit(auditModel: AuditModel, path: String = "-")(implicit hc: HeaderCarrier, ec: ExecutionContext): Unit = {
    val dataEvent = toDataEvent(appConfig.appName, auditModel, path)
    Logger.debug(s"Splunk Audit Event:\n\n${Json.toJson(dataEvent)}")
    handleAuditResult(auditConnector.sendEvent(dataEvent))
  }

  def toDataEvent(appName: String, auditModel: AuditModel, path: String)(implicit hc: HeaderCarrier): DataEvent =
    DataEvent(
      auditSource = appName,
      auditType = auditModel.auditType,
      tags = AuditExtensions.auditHeaderCarrier(hc).toAuditTags(auditModel.transactionName, path),
      detail = AuditExtensions.auditHeaderCarrier(hc).toAuditDetails(auditModel.detail: _*)
    )


  def extendedAudit(auditModel: ExtendedAuditModel, path: String = "-")(implicit hc: HeaderCarrier, ec: ExecutionContext): Unit = {
    val extendedDataEvent = toExtendedDataEvent(appConfig.appName, auditModel, path)
    Logger.debug(s"Splunk Audit Event:\n\n${Json.toJson(extendedDataEvent)}")
    handleAuditResult(auditConnector.sendExtendedEvent(extendedDataEvent))
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

  private def handleAuditResult(auditResult: Future[AuditResult])(implicit ec: ExecutionContext): Unit = auditResult.map {
    //$COVERAGE-OFF$ Disabling scoverage as returns Unit, only used for Debug messages
    case Success =>
      Logger.debug("Splunk Audit Successful")
    case Failure(err, _) =>
      Logger.debug(s"Splunk Audit Error, message: $err")
    case Disabled =>
      Logger.debug(s"Auditing Disabled")
    //$COVERAGE-ON$
  }
}
