import better.files.*
import org.ini4j.*

import java.io.FileReader
import java.util.UUID
import scala.io.AnsiColor.*
import scala.io.{Source, StdIn}
import scala.util.{Try, Using, Success, Failure}

def compare():Unit = {
  print("Введите UUID первой сессии: ")
  val first = StdIn.readLine()
  print("Введите UUID второй сессии: ")
  val second = StdIn.readLine()

  val content: Try[String] = Using(Source.fromFile(first+".log", "UTF-8")) { source =>
    source.mkString
  }
  content match {
    case Success(content) => {
      println(s"${RED}Сессия ${first}: ${RESET}")
      println(content)
    }
    case Failure(ex)      => println(s"Не удалось открыть файл: ${ex.getMessage}")
  }

  val content2: Try[String] = Using(Source.fromFile(second + ".log", "UTF-8")) { source =>
    source.mkString
  }
  content2 match {
    case Success(content) => {
      println(s"${RED}Сессия ${first}: ${RESET}")
      println(content)
    }
    case Failure(ex) => println(s"Не удалось открыть файл: ${ex.getMessage}")
  }
}

def findAppPrint(all: List[String], need: List[String]): List[String] = {
  need.filter { needed =>
    val isFound = all.exists(fileName => fileName == needed)
    if isFound then
      println(s"${GREEN}[+]${RESET} ${needed}")
    else {
      println(s"${RED}[-]${RESET} ${needed}")
    }
    isFound
  }
}

def full_diagnostic(hand: AbstractHandler, input: List[List[String]]):Unit = {
  println("полная диагностика")
  hand.handle(input)
}

def part_diagnostic(hand: AbstractHandler, input: List[List[String]]):Unit = {
  println("частичная диагностика")
  println("выберите желаемое: ")
  println("1) проверка сетевого интерфейса ")
  println("2) проверка доступность локального шлюза ")
  println("3) доступность выбранного узла в интернете ")
  println("4) проверка DNS")
  println("5) проверка доступности web-ресурса ")
  println("6) проверить маршрут до выбранного узла ")
  println("7) проверить задержку и потери пакетов")
  println(s"${RED}ПИСАТЬ СТРОГО ЧЕРЕЗ ПРОБЕЛ БЕЗ СТОРОННИХ СИМВОЛОВ!!!${RESET}")
  print("Проверки: ")
  val command = StdIn.readLine()
  try{
    val selectedIds: Set[String] = command.split("\\s+").map(_.trim).toSet
    val filteredInput: List[List[String]] = input.filter { innerList =>
      val id = innerList.headOption.getOrElse("")
      selectedIds.contains(id)
    }
    hand.handle(filteredInput)
  } catch {
    case e: NumberFormatException=>println("некорректный ввод. повторите попытку")
  }
  finally {
    return
  }
}

object GlobalConfig {
  var conf_file :String = ""
  var output_file :String = ""
}

@main def main(config_file: String): Unit = {
  println(s"***************************************")
  println(s"****program present by Gimik Dmitry****")
  println(s"***********linux edition***************")
  val sessionUuid: String = UUID.randomUUID().toString

  val ini_reader = new Ini(new FileReader(config_file))

  GlobalConfig.conf_file = config_file
  GlobalConfig.output_file = sessionUuid + ".log"

  val root = ini_reader.get("bin", "path")

  val interface = ini_reader.get("bin", "interface").split(",").map(_.trim.toString).toList
  val trace = ini_reader.get("bin", "trace").split(",").map(_.trim.toString).toList
  val node_ip = ini_reader.get("bin", "node_ip").split(",").map(_.trim.toString).toList
  val DNS = ini_reader.get("bin", "DNS").split(",").map(_.trim.toString).toList
  val web_resource = ini_reader.get("bin", "web_resource").split(",").map(_.trim.toString).toList
  val path_to_node = ini_reader.get("bin", "path_to_node").split(",").map(_.trim.toString).toList
  val ping = ini_reader.get("bin", "ping").split(",").map(_.trim.toString).toList

  val progs = File(root).list.map(_.name).toList

  val findInterface = "1"::findAppPrint(progs, interface);
  val findTrace = "2"::findAppPrint(progs, trace);
  val findNode_ip = "3"::findAppPrint(progs, node_ip);
  val findDNS = "4"::findAppPrint(progs, DNS);
  val findWeb_resource = "5"::findAppPrint(progs, web_resource);
  val findPath_to_node = "6"::findAppPrint(progs, path_to_node);
  val findPing = "7"::findAppPrint(progs, ping);

  val inp = List(findInterface,findTrace,findNode_ip,findDNS,findWeb_resource,
    findPath_to_node,findPing)

  var run : Boolean = true


  val pa = PingApp()
  val pn = PathToNodeHandler().setNext(pa)
  val we = WebResHandler().setNext(pn)
  val dn = DNSHandler().setNext(we)
  val nd = NodeHandler().setNext(dn)
  val tr = TraceHandle().setNext(nd)
  val in = InterfaceHandler().setNext(tr)
  val em = EmptyHandler().setNext(in)

  val chain = em
  while (run){
    println(s"действия: ")
    println(s"1) полная диагностика")
    println(s"2) частичная диагностика")
    println(s"3) выход")
    println(s"4) сравнение сессий")
    println(s"5) отчет")
    print("выберите желаемое: ")
    val command = StdIn.readInt()
    command match{
      case 1 => full_diagnostic(chain,inp)
      case 2 => part_diagnostic(chain,inp)
      case 3 => run=false
      case 4 => compare()
      case 5 => Proper.PrintReport()
      case _ => println("ошибка")
    }
  }
}

