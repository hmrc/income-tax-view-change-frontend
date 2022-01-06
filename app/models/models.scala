/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.libs.json._

package object models {
  val defaultZero: JsPath => Reads[BigDecimal] = _.read[BigDecimal].orElse(Reads.pure[BigDecimal](0.00))

  def readNullable[T](path: JsPath)(implicit reads: Reads[T]): Reads[Option[T]] = path.readNullable[T] orElse Reads.pure[Option[T]](None)

  def readNullableList[T](path: JsPath)(implicit reads: Reads[List[T]]): Reads[List[T]] = path.read[List[T]] orElse Reads.pure[List[T]](Nil)

}
