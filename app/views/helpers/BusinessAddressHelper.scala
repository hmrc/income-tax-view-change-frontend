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

package views.helpers

import models.BusinessModel
import play.api.i18n.Messages
import play.twirl.api.{Html, HtmlFormat}

object BusinessAddressHelper {

  def address(model: BusinessModel)(implicit messages: Messages):Html = {
    val bizList: Seq[String] = List(
      model.businessAddressLineOne,
      model.businessAddressLineTwo,
      model.businessAddressLineThree,
      model.businessAddressLineFour,
      model.businessPostcode).flatten

    HtmlFormat.fill(
      bizList.zipWithIndex.map { case (line: String, index: Int) => {
        Html(
        s"""
           |<tr ${if(line != bizList.reverse.head){Html("""class="no-border-bottom"""")} else {Html("")}}>
           |  ${if(line == bizList.head){
                  Html(s"""<th id="business-address" class="table-heading" rowspan="${bizList.length}">${messages("business.businessAddress")}</th>""")
                } else {
                  Html("")
                }
              }
           |  <td id="address-line-${index + 1}">$line</td>
           |</tr>
           |""".stripMargin
        )
      }}.to[collection.immutable.Seq]
    )
  }

}
