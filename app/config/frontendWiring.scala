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

package config

import javax.inject.{Inject, Singleton}

import play.api.Mode.Mode
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector => Auditing}
import uk.gov.hmrc.play.bootstrap.config.LoadAuditingConfig
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCrypto
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.play.partials.HeaderCarrierForPartialsConverter

@Singleton
class FrontendAuditConnector @Inject()(val environment: Environment,
                                       val conf: Configuration) extends Auditing with AppName {

  override protected def appNameConfiguration: Configuration = conf
  protected val mode: Mode = environment.mode
  override lazy val auditingConfig = LoadAuditingConfig(appNameConfiguration, mode, s"auditing")
}

@Singleton
class FrontendAuthConnector @Inject()(val environment: Environment,
                                      val conf: Configuration,
                                      val WSHttp: HttpClient) extends PlayAuthConnector with ServicesConfig {
  override protected def runModeConfiguration: Configuration = conf
  override protected def mode: Mode = environment.mode
  lazy val serviceUrl: String = baseUrl("auth")
  lazy val http = WSHttp
}

@Singleton
class ItvcHeaderCarrierForPartialsConverter @Inject()(val sessionCookieCrypto: SessionCookieCrypto) extends HeaderCarrierForPartialsConverter {
  val crypto: String => String = cookie => sessionCookieCrypto.crypto.encrypt(PlainText(cookie)).value
}
