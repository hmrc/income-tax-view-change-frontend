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

package utils

import assets.BaseTestConstants._
import assets.IncomeSourceDetailsTestConstants._
import auth.MtdItUser
import com.typesafe.config.Config
import config.{FrontendAppConfig, ItvcHeaderCarrierForPartialsConverter}
import models.UserDetailsModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Suite}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.MessagesApi
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.{Configuration, Environment}
import play.twirl.api.Html
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.partials.HeaderCarrierForPartials
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.{ExecutionContext, Future}

trait TestSupport extends UnitSpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterAll with MaterializerSupport {
  this: Suite =>

  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  implicit val mockItvcHeaderCarrierForPartialsConverter: ItvcHeaderCarrierForPartialsConverter = mock[ItvcHeaderCarrierForPartialsConverter]

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
  implicit val hcwc: HeaderCarrierForPartials = HeaderCarrierForPartials(headerCarrier, "")

  implicit val conf: Configuration = app.configuration
  implicit val environment: Environment = app.injector.instanceOf[Environment]
  implicit val frontendAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  implicit val config: Config = app.configuration.underlying

  implicit val user: MtdItUser[_] = MtdItUser(
    mtditid = "12341234",
    nino = "AA123456A",
    userDetails =
      Some(
        UserDetailsModel(
          name = "Test User",
          email = None,
          affinityGroup = "",
          credentialRole = ""
        )
      ),
    incomeSources = bothIncomeSourcesSuccessBusinessAligned
  )(FakeRequest())

  implicit val serviceInfo: Html = Html("")

  implicit class JsoupParse(x: Future[Result]) {
    def toHtmlDocument: Document = Jsoup.parse(bodyOf(x))
  }

  lazy val fakeRequestWithActiveSession = FakeRequest().withSession(
    SessionKeys.lastRequestTimestamp -> "1498236506662",
    SessionKeys.authToken -> "Bearer Token"
  )
  lazy val fakeRequestWithTimeoutSession = FakeRequest().withSession(
    SessionKeys.lastRequestTimestamp -> "1498236506662"
  )
  lazy val fakeRequestWithNino = fakeRequestWithActiveSession.withSession("nino" -> testNino)
  lazy val fakeRequestNoSession = FakeRequest()

}