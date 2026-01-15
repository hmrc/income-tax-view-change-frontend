package enums.TriggeredMigration

sealed trait Channel

object Channel {
  val CustomerLedValue = "1"
  val UnconfirmedValue = "2"
  val ConfirmedValue = "3"
}

case object CustomerLed extends Channel {
  override val toString: String = Channel.CustomerLedValue
}

case object Unconfirmed extends Channel {
  override val toString: String = Channel.UnconfirmedValue
}

case object Confirmed extends Channel {
  override val toString: String = Channel.ConfirmedValue
}
