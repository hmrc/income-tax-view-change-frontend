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

package uk.gov.hmrc.helptosavefrontend

import com.typesafe.config.Config
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, Suite}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Play}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext

trait TestSupport extends UnitSpec with MockFactory with BeforeAndAfterAll {
  this: Suite =>

  lazy val fakeApplication: Application =
    new GuiceApplicationBuilder()
      .configure("metrics.enabled" -> "false")
      .build()

  implicit val ec: ExecutionContext = fakeApplication.injector.instanceOf[ExecutionContext]

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  val config: Config = fakeApplication.configuration.underlying

  override def beforeAll() {
    super.beforeAll()
    Play.start(fakeApplication)
  }

  override def afterAll() {
    super.afterAll()
    Play.stop(fakeApplication)
  }

}