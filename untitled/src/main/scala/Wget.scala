import better.files.File
import sys.process.*
import scala.jdk.CollectionConverters._

case class Wget() extends SystemUtility {
  val name: String = "Wget"
  val executable: String = "wget"
  val configSection: String = "wget"
  val defaultArgs: List[String] = List()

  def parseForConsole(fullOutput: String): String = {
    // Этот метод не используется напрямую, так как мы переопределяем run
    // Но должен быть реализован по контракту трейта
    "Wget проверка"
  }

  override def run(): List[UtilityResult] = {
    // Получаем аргументы из INI файла
    val iniArgs: List[String] = Option(ini_reader.getAll(configSection, "arg"))
      .map(_.asScala.toList.map(_.toString))
      .getOrElse(List(""))

    logger.info(s"Утилита '$name'. Секция INI: [$configSection]. Найдено вариантов: ${iniArgs.size}")

    // Обрабатываем каждый набор аргументов
    iniArgs.map { argString =>
      val argsList = argString.trim.split("\\s+").filter(_.nonEmpty).toList
      val command: Seq[String] = executable +: argsList

      logger.info(s"Выполняю: ${command.mkString(" ")}")

      try {
        // Используем ! вместо !!, так как wget --spider не выводит ничего
        val exitCode = command.!
        val success = (exitCode == 0)

        val consoleMsg = if success then "Доступен" else "Недоступен"
        val fullMsg = if success then s"OK (exit code: $exitCode)" else s"Failed (exit code: $exitCode)"

        logger.info(s"Результат: $consoleMsg")

        UtilityResult(
          success = success,
          exitCode = exitCode,
          consoleOutput = consoleMsg,
          fullOutput = fullMsg
        )
      } catch {
        case e: Exception =>
          logger.error(s"Ошибка '${command.mkString(" ")}': ${e.getMessage}")
          UtilityResult(
            success = false,
            exitCode = -1,
            consoleOutput = s"Ошибка запуска: ${e.getMessage}",
            fullOutput = e.getMessage,
            errors = List(e.getMessage)
          )
      }
    }
  }

  def insertToLogFile(fullOutput: String, pathToFile: String): Unit = {
    File(pathToFile).appendLine(s"\n=== Wget ===\n$fullOutput")
  }
}