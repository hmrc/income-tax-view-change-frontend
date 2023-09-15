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

package services.helpers

import auth.MtdItUser
import models.incomeSourceDetails.PropertyDetailsModel

trait ActivePropertyBusinessesHelper {
  def getActiveForeignPropertyFromUserIncomeSources(implicit user: MtdItUser[_]): Either[Throwable, PropertyDetailsModel] = {
    val activeForeignProperty = user.incomeSources.properties.filterNot(_.isCeased).filter(_.isForeignProperty)
    activeForeignProperty match {
      case list: List[PropertyDetailsModel] if list.length == 1 => Right(list.head)
      case list: List[PropertyDetailsModel] if list.length > 1 => Left(new Error("Too many active foreign properties found. There should only be one."))
      case _ => Left(new Error("No active foreign properties found."))
    }
  }

  def getActiveUkPropertyFromUserIncomeSources(implicit user: MtdItUser[_]): Either[Throwable, PropertyDetailsModel] = {
    val activeUkProperty = user.incomeSources.properties.filterNot(_.isCeased).filter(_.isUkProperty)
    activeUkProperty match {
      case list: List[PropertyDetailsModel] if list.length == 1 => Right(list.head)
      case list: List[PropertyDetailsModel] if list.length > 1 => Left(new Error("Too many active foreign properties found. There should only be one."))
      case _ => Left(new Error("No active foreign properties found."))
    }
  }
}