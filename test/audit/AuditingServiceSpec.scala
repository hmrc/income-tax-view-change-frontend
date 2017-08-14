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

import _root_.models.ExitSurveyModel
import audit.models.ExitSurveyAuditing.ExitSurveyAuditModel
import config.FrontendAuditConnector
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.Configuration
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.TestSupport

import scala.concurrent.ExecutionContext

class AuditingServiceSpec extends TestSupport with BeforeAndAfterEach {

  val mockAuditConnector = mock[FrontendAuditConnector]
  val mockConfiguration = mock[Configuration]
  val testAppName = "app"

  val testAuditingService = new AuditingService(mockConfiguration, mockAuditConnector)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuditConnector, mockConfiguration)
    when(mockConfiguration.getString("appName")) thenReturn Some(testAppName)
  }

  "audit" when {
    "given a ExitSurveyAuditModel" should {
      "extract the data and pass it into the AuditConnector" in {

        val testModel = ExitSurveyAuditModel(ExitSurveyModel(Some("Very satisfied"), Some("Awesome")))
        val expectedData = testAuditingService.toDataEvent(testAppName, testModel, controllers.feedback.routes.FeedbackController.show().url)

        testAuditingService.audit(testModel, controllers.feedback.routes.FeedbackController.show().url)

        verify(mockAuditConnector)
          .sendEvent(
            ArgumentMatchers.refEq(expectedData, "eventId", "generatedAt")
          )(
            ArgumentMatchers.any[HeaderCarrier],
            ArgumentMatchers.any[ExecutionContext]
          )
      }
    }
  }
}
