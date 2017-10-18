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

package audit

import javax.inject.{Inject, Singleton}

import audit.models.AuditModel
import config.{FrontendAppConfig, FrontendAuditConnector}
import play.api.Logger
import play.api.libs.json.Json
import uk.gov.hmrc.play.audit.AuditExtensions
import uk.gov.hmrc.play.audit.http.connector.AuditResult.{Failure, Success}
import uk.gov.hmrc.play.audit.model.DataEvent

import scala.concurrent.ExecutionContext
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class AuditingService @Inject()(appConfig: FrontendAppConfig, auditConnector: FrontendAuditConnector) {

  def audit(auditModel: AuditModel, path: String = "N/A")(implicit hc: HeaderCarrier, ec: ExecutionContext): Unit = {
    val dataEvent = toDataEvent(appConfig.appName, auditModel, path)
    Logger.debug(s"Splunk Audit Event:\n\n${Json.toJson(dataEvent)}")
    auditConnector.sendEvent(dataEvent).map {
      //$COVERAGE-OFF$ Disabling scoverage as returns Unit, only used for Debug messages
      case Success =>
        Logger.debug("Splunk Audit Successful")
      case Failure(err,_) =>
        Logger.debug(s"Splunk Audit Error, message: $err")
      case _ =>
        Logger.debug(s"Unknown Splunk Error")
      //$COVERAGE-ON$
    }
  }

  def toDataEvent(appName: String, auditModel: AuditModel, path: String)(implicit hc: HeaderCarrier): DataEvent = {
    val auditType: String = auditModel.auditType
    val transactionName: String = auditModel.transactionName
    val detail: Map[String, String] = auditModel.detail

    DataEvent(
      auditSource = appName,
      auditType = auditType,
      tags = AuditExtensions.auditHeaderCarrier(hc).toAuditTags(transactionName, path),
      detail = AuditExtensions.auditHeaderCarrier(hc).toAuditDetails(detail.toSeq: _*)
    )
  }
}
