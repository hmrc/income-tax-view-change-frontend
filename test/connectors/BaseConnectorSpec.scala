/*
 * Copyright 2024 HM Revenue & Customs
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

package connectors

import auth.MtdItUser
import com.codahale.metrics.Timer
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import models.admin.FeatureSwitchName.allFeatureSwitches
import org.mockito.Mockito.{mock, reset}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.HeaderNames
import play.api.i18n.MessagesApi
import play.api.mvc.AnyContentAsEmpty
import play.api.test.{FakeRequest, Injecting}
import play.api.{Configuration, Environment}
import testConstants.BaseTestConstants._
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessAndPropertyAligned
import testUtils.UnitSpec
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{DurationInt, FiniteDuration}

trait BaseConnectorSpec extends UnitSpec with BeforeAndAfterEach with GuiceOneAppPerSuite with Injecting with ScalaFutures with FeatureSwitching {

  val mockHttpClientV2: HttpClientV2 = mock(classOf[HttpClientV2])
  val mockRequestBuilder: RequestBuilder = mock(classOf[RequestBuilder])
  val mockTimerContext: Timer.Context = mock(classOf[Timer.Context])

  def messagesApi: MessagesApi = inject[MessagesApi]

  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session-123456")), deviceID = Some("some device Id")).withExtraHeaders(HeaderNames.REFERER -> testReferrerUrl)

  implicit val conf: Configuration = app.configuration
  implicit val environment: Environment = app.injector.instanceOf[Environment]
  implicit val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  def getIndividualUser(request: FakeRequest[AnyContentAsEmpty.type]): MtdItUser[_] =
    MtdItUser(
      mtditid = testMtditid,
      nino = testNino,
      userName = Some(testRetrievedUserName),
      incomeSources = businessAndPropertyAligned,
      btaNavPartial = None,
      saUtr = Some(testSaUtr),
      credId = Some(testCredId),
      userType = Some(testUserTypeIndividual),
      arn = None
    )(request)

  implicit val individualUser: MtdItUser[_] = getIndividualUser(FakeRequest())


  def disableAllSwitches(): Unit = {
    allFeatureSwitches.foreach(switch => disable(switch))
  }

  implicit val timeoutDuration: FiniteDuration = 20.seconds

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockHttpClientV2)
    reset(mockTimerContext)
    reset(mockRequestBuilder)
  }
}
