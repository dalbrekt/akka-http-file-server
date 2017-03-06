package akkahttp.javadsl;

import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.BodyPartEntity;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpEntities;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.Multipart;
import akka.http.javadsl.model.Multiparts;
import akka.http.javadsl.model.Query;
import akka.http.javadsl.model.RequestEntity;
import akka.http.javadsl.model.Uri;
import akka.http.javadsl.unmarshalling.Unmarshaller;
import akka.japi.Pair;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.FileIO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import scala.concurrent.ExecutionContextExecutor;

import java.io.File;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static java.lang.String.format;

/**
 * Created by <a href="https://github.com/dalbrekt">Tony Dalbrekt</a>.
 */
public class FileClient {

    private final ActorSystem actorSystem;
    private final String host;
    private final int port;
    private final Uri server;
    private final ActorMaterializer materializer;
    private final ExecutionContextExecutor dispatcher;

    public FileClient(ActorSystem actorSystem, String host, int port) {
        this.actorSystem = actorSystem;
        this.host = host;
        this.port = port;
        this.server = Uri.create(format("http://%s:%s", this.host, this.port));
        this.materializer = ActorMaterializer.create(actorSystem);
        this.dispatcher = actorSystem.dispatcher();
    }

    public CompletableFuture<FileHandle> upload(final File file) {
        Uri target = server.addPathSegment("files");
        HttpRequest req = HttpRequest
                .PUT(target.toString())
                .withEntity(entity(file));

        CompletionStage<HttpResponse> responseFuture = Http.get(actorSystem)
                .singleRequest(req, materializer);

        return responseFuture
                .handle((resp, ex) -> new FileHandle(toFileInfo(resp)))
                .toCompletableFuture();
    }



    public CompletionStage<Void> download(final FileHandle remoteFile, final String saveAs) {
        String serverFile = remoteFile.getInfo().getTargetFile();
        Uri download = server.addPathSegment("files").query(Query.create(Pair.create("file", serverFile)));

        //download file to local
        CompletionStage<HttpResponse> response = Http.get(actorSystem)
                .singleRequest(HttpRequest
                        .GET(download.toString()), materializer);

        return response.thenCompose(resp -> resp.entity()
                .getDataBytes()
                .runWith(FileIO.toPath(Paths.get(saveAs)), materializer)).thenAccept(ioResult -> {
        });
    }

    // Utilities --------------------------------------------------------

    private RequestEntity entity(File file) {
        BodyPartEntity entity = HttpEntities
                .create(ContentTypes.APPLICATION_OCTET_STREAM, file.length(), FileIO.fromFile(file, 100000));
        Multipart.FormData.BodyPart body =
                Multiparts.createFormDataBodyPart("datafile", entity, ImmutableMap.of("filename", file.getName()));
        return Multiparts.createFormDataFromParts(body).toEntity();

    }

    private FileInfo toFileInfo(HttpResponse response) {
        try {
            String json = Unmarshaller.entityToString().unmarshal(response.entity(), dispatcher, materializer)
                    .toCompletableFuture().get();
            ObjectMapper mapper = new ObjectMapper();
            Map map = mapper.readValue(json, Map.class);
            return mapper.convertValue(map.get("datafile"), FileInfo.class);

        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid json parsable response", e);
        }
    }


}
