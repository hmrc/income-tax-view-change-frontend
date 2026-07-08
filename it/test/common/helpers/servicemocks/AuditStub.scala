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

package common.helpers.servicemocks

import common.models.audit.ExtendedAuditModel
import common.testConstants.MicroserviceSpecificConstants
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.http.Status.NO_CONTENT
import play.api.libs.json.{JsValue, Json}

object AuditStub extends WiremockMethods with Eventually {

  override implicit val patienceConfig: PatienceConfig =
    PatienceConfig(
      timeout  = Span(5, Seconds),
      interval = Span(50, Millis)
    )

  lazy val auditSource = MicroserviceSpecificConstants.auditSource
  
  def stubAuditing(): Unit = {
    when(method = POST, uri = "/write/audit/merged")
      .thenReturn(status = NO_CONTENT, body = """{"x":2}""")

    when(method = POST, uri = "/write/audit")
      .thenReturn(status = NO_CONTENT, body = """{"x":2}""")
  }

  def verifyAudit(): Unit = {
    //We cannot verify content of audit body without string matching/regex
    //It is tested in more detail at unit level
    eventually {
      verify(method = POST, uri = "/write/audit")
    }
  }

  def verifyAuditContainsDetail(body: JsValue): Unit = {
    eventually {
      verifyContainsJson(method = POST, uri = "/write/audit", Json.obj("detail" -> body))
    }
  }

  def verifyAuditEvent(auditEvent: ExtendedAuditModel): Unit = {
    eventually {
      val expectedAuditJson =
        Json.obj(
          "auditSource" -> auditSource,
          "auditType" -> auditEvent.auditType,
          "tags" -> Json.obj("transactionName" -> auditEvent.transactionName),
          "detail" -> auditEvent.detail
        )
      verifyContainsJson(method = POST, uri = "/write/audit", expectedAuditJson)
    }
  }
}