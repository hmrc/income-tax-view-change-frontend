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

package utils.logback

import ch.qos.logback.classic.pattern.ClassicConverter
import ch.qos.logback.classic.spi.ILoggingEvent

import scala.util.matching.Regex

class SimpleMethodLog extends ClassicConverter {

  private val pattern: Regex = """.*\.([^\.]+)$""".r

  private def toSimpleClassName(className: String): String = {
    val cleanClassName = if(className.contains("$")) className.substring(0, className.indexOf("$")) else className
    cleanClassName match {
      case pattern(lastPart) => lastPart
      case _ => "No match found"
    }
  }

  override def convert(event: ILoggingEvent): String = {
    val data: Array[StackTraceElement] = event.getCallerData

    val logData = data(0)
    val className: String = toSimpleClassName(logData.getClassName)

    val methodNameParts: List[String] = logData.getMethodName.split("\\$").toList
    val methodName = methodNameParts match {
      case methodName :: Nil => methodName
      case _ :: methodName :: Nil => methodName
      case _ :: _ :: methodName :: _ => methodName
      case _ => "default"
    }
    val lineNumber = logData.getLineNumber

    s"$className.$methodName:$lineNumber"
  }

}