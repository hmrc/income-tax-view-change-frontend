
package testConstants.messages

import helpers.servicemocks.AuthStub.{messagesAPI, lang}

object TaxYearsMessages {

  val viewSummary: String = messagesAPI("taxYears.viewSummary")

  val updateReturn: String = messagesAPI("taxYears.updateReturn")

  def taxYearMessage(from: Int, to: Int): String = messagesAPI("taxYears.taxYears", from.toString, to.toString)
}
