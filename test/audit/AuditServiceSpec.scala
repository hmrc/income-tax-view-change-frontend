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

import audit.models.ExtendedAuditModel
import config.FrontendAppConfig
import org.mockito.Mockito.mock
import org.scalatest.PrivateMethodTester
import play.api.libs.json.{JsValue, Json}
import testUtils.TestSupport
import uk.gov.hmrc.play.audit.http.connector.AuditConnector


class AuditServiceSpec extends TestSupport with PrivateMethodTester {

  "The AuditService" should {

    val appConfig: FrontendAppConfig = mock(classOf[FrontendAppConfig])
    val auditConnector: AuditConnector = mock(classOf[AuditConnector])

    val obj = new AuditingService(appConfig, auditConnector)
    val auditModel = new ExtendedAuditModel {
      override val transactionName: String = "transactionName"
      override val detail: JsValue = Json.obj("detail" -> "detail")
      override val auditType: String = "auditType"
    }
    s"return ExtendedDataEvent" in {
      val result = obj.toExtendedDataEvent("appName", auditModel, "path")

      result.auditSource shouldBe "appName"
      result.auditType shouldBe "auditType"
    }
  }

}
