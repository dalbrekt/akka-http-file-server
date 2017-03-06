package akkahttp

import java.io.File
import java.util.concurrent.{CompletableFuture, TimeUnit}

import akka.actor.ActorSystem
import akkahttp.javadsl.{FileClient, FileHandle}
import com.typesafe.config.{Config, ConfigFactory}

import scala.io.Source


/**
  * Created by <a href="https://github.com/dalbrekt">Tony Dalbrekt</a>.
  */

object TestAppJava extends App {


  val testConf: Config = ConfigFactory.load()

  implicit val system = ActorSystem("ServerTest", testConf)
  implicit val dispatcher = system.dispatcher
  val host = "127.0.0.1"
  val port = 9112

  val server = new FileServer(system, host, 9112)

  //start file server
  val binding = server.start

  val client = new FileClient(system, host, port);

  // upload the file
  val testFile = new File(getClass.getResource("/testfile.txt").toURI());

  val fileHandler: CompletableFuture[FileHandle] = client.upload(testFile);

  //download the file
  val target = new File("target/testfile_download.javadsl.txt")
  val future = fileHandler.thenAccept(handler => client.download(handler, target.toString))


  future.get(10, TimeUnit.SECONDS)

  Thread.sleep(500);
  // check the file content.
  Source.fromFile(testFile).foreach {
    print
  }

  println()
  // now you can try to browser http://127.0.0.1:9112/
  println(s"Browser http://${host}:${port} to test download and upload")
  system.awaitTermination()

}
