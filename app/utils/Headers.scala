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

package utils

import uk.gov.hmrc.http.HeaderCarrier

object Headers {

  //Checks and adding the value to the test header
  def checkAndAddTestHeader(requestPath: String,
                            headerCarrier: HeaderCarrier,
                            configPages: Option[Seq[String]],
                            suffix: String): HeaderCarrier = {
    val page: Map[String, String] = configPages.map(kv =>
      kv.map(k => (k, suffix)).toMap
    ).getOrElse(Map[String, String]())
    val urlPathArray = requestPath.split('/')
    val actionPath = if (urlPathArray.isEmpty) "" else urlPathArray.last
    val updatedHeader = page.get(actionPath) match {
      case Some(data) => "Gov-Test-Scenario" -> data
      case _ => "Gov-Test-Scenario" -> ""
    }
    headerCarrier.withExtraHeaders(updatedHeader)
  }

}
