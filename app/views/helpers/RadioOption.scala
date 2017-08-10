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

package views.helpers

class RadioOption(val optionName: String, val message: String, val classes: Option[String]) extends Product with Serializable {

  override def toString: String = message

  def copy(optionName: String = this.optionName, message: String = this.message, classes: Option[String] = this.classes) = RadioOption(optionName, message, classes)

  override def equals(obj: scala.Any): Boolean =
    obj match {
      case that: RadioOption if that.optionName.equals(this.optionName) && that.message.equals(this.message) && that.classes.equals(this.classes) => true
      case _ => false
    }

  override def hashCode(): Int = {
    val prime = 37
    ((prime + optionName.hashCode) * prime + message.hashCode) * prime + classes.hashCode
  }

  override def canEqual(that: Any): Boolean = that.isInstanceOf[RadioOption]

  override def productArity: Int = 3

  override def productElement(n: Int): Any = n match {
    case 0 => optionName
    case 1 => message
    case 2 => classes
    case _ => throw new IndexOutOfBoundsException(s"The parameter for RadioName.productElement cannot exceed 2. {$n}")
  }
}

object RadioOption {
  def apply(optionName: String, message: String, classes: Option[String] = None): RadioOption = {
    if (optionName.contains(" ")) throw new IllegalArgumentException(s"RadioName: the optionName parameter must not contain any spaces {$optionName}")
    new RadioOption(optionName, message, classes)
  }

  def unapply(obj: RadioOption): Option[(String, String)] = Some((obj.optionName, obj.message))
}