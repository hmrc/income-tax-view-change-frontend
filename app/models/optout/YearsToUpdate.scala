package models.optout

import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.ITSAStatus

case class OptOutYearsToUpdate(taxYear:TaxYear, itsaStatus: ITSAStatus)
