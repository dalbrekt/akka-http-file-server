package akkahttp

import java.io.File

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.Await
import scala.io.Source

object TestAppScala extends App {

  val testConf: Config = ConfigFactory.load()

  implicit val system = ActorSystem("ServerTest", testConf)
  implicit val dispatcher = system.dispatcher
  val host = "127.0.0.1"
  val port = 9112

  val server = new FileServer(system, host, 9112)

  //start file server
  val binding = server.start
  val client = new FileServer.Client(system, host, port)

  // upload the file
  val testFile = new File(getClass.getResource("/testfile.txt").toURI())
  val fileHandler = client.upload(testFile)

  // upload the file
  val testFile2 = new File(getClass.getResource("/testfile2.txt").toURI())
  val fileHandler2 = client.uploadToStream(testFile2)

  //download the file
  val target = new File("target/testfile_download.scaladsl.txt")
  val future = fileHandler.flatMap{handler =>
    client.download(handler, target)
  }

  import scala.concurrent.duration._
  Await.result(future, 10 seconds)

  // check the file content.
  Source.fromFile(testFile).foreach{
    print
  }

  println()
  // now you can try to browser http://127.0.0.1:9112/
  println(s"Browser http://${host}:${port} to test download and upload")
  system.awaitTermination()
}