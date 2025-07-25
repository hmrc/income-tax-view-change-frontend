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

package testOnly

import config.FrontendAppConfig
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}

@Singleton
class TestOnlyAppConfig @Inject()(servicesConfig: ServicesConfig, config: Configuration) extends FrontendAppConfig(servicesConfig, config) {

  lazy val dynamicStubUrl: String = servicesConfig.baseUrl("itvc-dynamic-stub")

  lazy val penaltiesStubUrl: String = servicesConfig.baseUrl("penalties-stub")

  lazy val matchingStubUrl: String = servicesConfig.baseUrl("matching-stub")

  lazy val optOutUserPrefixes: Seq[String] = config.get[Seq[String]]("optOutNinoPrefix")

}
