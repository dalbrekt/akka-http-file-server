package akkahttp

import java.io.File
import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.client.RequestBuilding.Get
import akka.http.scaladsl.client.RequestBuilding.Put
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshaller._
import akka.http.scaladsl.marshalling.{Marshal, ToResponseMarshallable}
import akka.http.scaladsl.model.Uri.{Path, Query}
import akka.http.scaladsl.model.{HttpEntity, Multipart, _}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.{DebuggingDirectives, LogEntry, LoggingMagnet}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.unmarshalling.Unmarshaller._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Source}
import akkahttp.FileDirective.{FileInfo, _}
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class FileServer(system: ActorSystem, host: String, port: Int) {

  import system.dispatcher

  implicit val actorSystem = system
  implicit val materializer = ActorMaterializer()

  import FileServer.fileInfoFormat

  def logRequest(req: HttpRequest) = s"> ${req.protocol.value}\n> ${req.method.value} ${req.uri}\n> ${req.headers}\n> ${req.entity}\n"
  def logResponse(res: HttpResponse) = s"< ${res.status} \n< ${res.headers}\n< ${res.entity}\n"

  def requestMethodAndResponseStatusAsInfo(req: HttpRequest): RouteResult => Option[LogEntry] = {
    case RouteResult.Complete(res) => Some(LogEntry(logRequest(req) + "\n" + logResponse(res), Logging.InfoLevel))
    case _                         => None // no log entries for rejections
  }

  def printRequestResponse(req: HttpRequest)(res: RouteResult): Unit =
    println(requestMethodAndResponseStatusAsInfo(req)(res).map(_.obj.toString).getOrElse(""))

  val logRequestResultPrintln = DebuggingDirectives.logRequestResult(LoggingMagnet(_ => printRequestResponse))

  //def logRequestResponse(req: HttpRequest)(res: RouteResult): Unit = println()

  val route: Route =

      path("files") {
        put {
          logRequestResultPrintln {
            fileUpload("datafile") {
              case (metadata, byteSource) =>
                val path = Paths.get("target") resolve metadata.fileName
                val sink = FileIO.toPath(Paths.get("target") resolve metadata.fileName)
                val writeResult = byteSource.runWith(sink)

                onSuccess(writeResult) { result =>
                  result.status match {
                    case Success(_) => complete(ToResponseMarshallable(
                      Map(metadata.fieldName -> FileInfo(metadata.fileName, path.toString, result.count))))
                    case Failure(e) => throw e
                  }
                }
            }
          }
        } ~
        get {
          logRequestResultPrintln {
            parameters("file") { file =>
              downloadFile(file)
            }
          }
        }
      } ~
      pathEndOrSingleSlash {
        val entity = HttpEntity(ContentTypes.`text/html(UTF-8)`,
          """
            |
            |<h2>Please specify a file to upload:</h2>
            |<form action="http://127.0.0.1:9112/files" enctype="multipart/form-data" method="put">
            |<input type="file" name="datafile" size="40">
            |</p>
            |<div>
            |<input type="submit" value="Submit">
            |</div>
            |</form>
          """.
            stripMargin)
        complete(entity)
      }

  private var
  connection: Future[ServerBinding] = null

  def start: Unit = {
    connection = Http().bindAndHandle(Route.handlerFlow(route), host, port = port)
  }

  def stop: Future[Unit] = {
    connection.flatMap(_.unbind())
  }
}


object FileServer {
  implicit def fileInfoFormat: JsonFormat[FileDirective.FileInfo] = jsonFormat3(FileDirective.FileInfo.apply)

  class Client(system: ActorSystem, host: String, port: Int) {
    private implicit val actorSystem = system
    private implicit val materializer = ActorMaterializer()
    private implicit val ec = system.dispatcher

    val server = Uri(s"http://$host:$port")

    case class FileHandle(private[Client] val info: FileDirective.FileInfo)


    def upload(file: File): Future[FileHandle] = {
      val target = server.withPath(Path("/files"))
      val response = Http(system).singleRequest(Put(target, entity(file)))
      response.flatMap(some => Unmarshal(some).to[Map[Name, FileDirective.FileInfo]]).map(map => FileHandle(map.head._2))
    }


    def download(remoteFile: FileHandle, saveAs: File): Future[Unit] = {
      val serverFile = remoteFile.info.targetFile
      val download = server.withPath(Path("/files")).withQuery(Query("file" -> serverFile))

      //download file to local
      val response = Http(system).singleRequest(Get(download))

      val downloaded = response.flatMap { resp =>
        resp.entity.dataBytes.runWith(FileIO.toPath(saveAs.toPath))
      }
      downloaded.map(written => Unit)
    }

    private def entity(file: File)(implicit ec: ExecutionContext): Future[RequestEntity] = {
      val entity = HttpEntity(ContentTypes.`application/octet-stream`, file.length(), FileIO.fromPath(file.toPath, chunkSize = 100000))
      val body = Source.single(
        Multipart.FormData.BodyPart(
          "datafile",
          entity,
          Map("filename" -> file.getName)))
      val form = Multipart.FormData(body)

      Marshal(form).to[RequestEntity]
    }
  }

}