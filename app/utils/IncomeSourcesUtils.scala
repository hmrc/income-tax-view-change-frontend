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

import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import enums.IncomeSourceJourney.*
import enums.IncomeSourceJourney.IncomeSourceType.{ SelfEmployment, UkProperty}
import models.incomeSourceDetails.PropertyDetailsModel
import play.api.Logger

trait IncomeSourcesUtils extends FeatureSwitching {

  def selectActiveProperty(incomeSourceType: IncomeSourceType, filter: PropertyDetailsModel => Boolean)(implicit user: MtdItUser[_]): Option[PropertyDetailsModel] = {

    val activeProperty: Seq[PropertyDetailsModel] = user.incomeSources.properties.filter(p => !p.isCeased && filter(p))

    activeProperty match {
      case property :: Nil =>
        Some(property)
      case _ =>
        Logger("application").error(s"Invalid amount of $incomeSourceType: expected 1, found ${activeProperty.length}")
        None
    }
  }

  def getActiveProperty(incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Option[PropertyDetailsModel] = {
    incomeSourceType match {
      case SelfEmployment => None
      case UkProperty => selectActiveProperty(UkProperty, _.isUkProperty)
      case foreignProperty => selectActiveProperty(foreignProperty, _.isForeignProperty)
    }
  }

}
