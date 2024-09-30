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

import config.FrontendAppConfig
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.scalatest._
import org.scalatest.concurrent.PatienceConfiguration
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Environment
import play.api.mvc.{AnyContentAsEmpty, MessagesControllerComponents}
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag

trait BaseSpec
  extends AnyWordSpecLike
    with Matchers
    with OptionValues
    with GuiceOneAppPerSuite
    with PatienceConfiguration
    with BeforeAndAfterEach
    with Injecting {
  this: Suite =>

  implicit val hc: HeaderCarrier = HeaderCarrier()

  def injected[T](c: Class[T]): T = app.injector.instanceOf(c)

  def injected[T](implicit evidence: ClassTag[T]): T = app.injector.instanceOf[T]

  lazy val messagesControllerComponents: MessagesControllerComponents = injected[MessagesControllerComponents]
  lazy val environment: Environment = injected[Environment]

  implicit val appConfig: FrontendAppConfig = injected[FrontendAppConfig]
  implicit val ec: ExecutionContext = injected[ExecutionContext]

  implicit val actorSystem: ActorSystem = ActorSystem("test")
  implicit val materializer: Materializer = Materializer(actorSystem)

  lazy val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/foo")

  lazy val invalidTaxYear: String = "1999"

//  lazy val fakeRequestWithNino: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
//    .withSession(ninoSessionKey -> nino)
//  lazy val fakeRequestWithTaxYear: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
//    .withSession(taxYearSessionKey -> invalidTaxYear)
//  lazy val fakeRequestWithTaxYearAndNino: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
//    .withSession(ninoSessionKey -> nino, taxYearSessionKey -> invalidTaxYear)
//  lazy val noSelectedTaxYear: SelectTaxYear = SelectTaxYear(None)

}
