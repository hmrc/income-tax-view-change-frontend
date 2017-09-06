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
import config.{FrontendAppConfig, FrontendAuditConnector}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.TestSupport

import scala.concurrent.{ExecutionContext, Future}

class AuditingServiceSpec extends TestSupport with BeforeAndAfterEach {

  val mockAuditConnector = mock[FrontendAuditConnector]
  val mockConfiguration = mock[FrontendAppConfig]

  val testAuditingService = new AuditingService(mockConfiguration, mockAuditConnector)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuditConnector, mockConfiguration)
  }

  "audit" when {

    "given a ExitSurveyAuditModel with Satisfaction and Improvements data" should {
      "extract the data and pass it into the AuditConnector" in {

        val testModel = ExitSurveyAuditModel(ExitSurveyModel(Some("Very satisfied"), Some("Awesome")))
        val expectedData = testAuditingService.toDataEvent(mockConfiguration.appName, testModel, controllers.feedback.routes.FeedbackController.show().url)

        when(mockAuditConnector.sendEvent(
          ArgumentMatchers.refEq(expectedData, "eventId", "generatedAt")
        )(
          ArgumentMatchers.any[HeaderCarrier],
          ArgumentMatchers.any[ExecutionContext]
        )) thenReturn Future.successful(Success)

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

    "given a ExitSurveyAuditModel with Satisfaction data only" should {
      "extract the data and pass it into the AuditConnector" in {

        val testModel = ExitSurveyAuditModel(ExitSurveyModel(Some("Very satisfied"), None))
        val expectedData = testAuditingService.toDataEvent(mockConfiguration.appName, testModel, controllers.feedback.routes.FeedbackController.show().url)

        when(mockAuditConnector.sendEvent(
          ArgumentMatchers.refEq(expectedData, "eventId", "generatedAt")
        )(
          ArgumentMatchers.any[HeaderCarrier],
          ArgumentMatchers.any[ExecutionContext]
        )) thenReturn Future.successful(Success)

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

    "given a ExitSurveyAuditModel with Improvements data only" should {
      "extract the data and pass it into the AuditConnector" in {

        val testModel = ExitSurveyAuditModel(ExitSurveyModel(None, Some("Awesome")))
        val expectedData = testAuditingService.toDataEvent(mockConfiguration.appName, testModel, controllers.feedback.routes.FeedbackController.show().url)

        when(mockAuditConnector.sendEvent(
          ArgumentMatchers.refEq(expectedData, "eventId", "generatedAt")
        )(
          ArgumentMatchers.any[HeaderCarrier],
          ArgumentMatchers.any[ExecutionContext]
        )) thenReturn Future.successful(Success)

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

    "given a ExitSurveyAuditModel with no data" should {
      "extract the data and pass it into the AuditConnector" in {

        val testModel = ExitSurveyAuditModel(ExitSurveyModel(None, None))
        val expectedData = testAuditingService.toDataEvent(mockConfiguration.appName, testModel, controllers.feedback.routes.FeedbackController.show().url)

        when(mockAuditConnector.sendEvent(
          ArgumentMatchers.refEq(expectedData, "eventId", "generatedAt")
        )(
          ArgumentMatchers.any[HeaderCarrier],
          ArgumentMatchers.any[ExecutionContext]
        )) thenReturn Future.successful(Success)

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
