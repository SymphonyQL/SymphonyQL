import sbt.Command

object Commands {

  val FmtSbtCommand = Command.command("fmt")(state => "scalafmtSbt" :: "scalafmtAll" :: "javafmtAll" :: state)

  val FmtSbtCheckCommand =
    Command.command("check")(state => "scalafmtSbtCheck" :: "scalafmtCheckAll" :: "javafmtCheckAll" :: state)

  val value = Seq(
    FmtSbtCommand,
    FmtSbtCheckCommand
  )

}
