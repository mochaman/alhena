
package brad.grier.alhena;

import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.net.NetSocket;


public class StreamSession {

    private NetSocket socket;
    private boolean closed;
    private HttpClientResponse clientResponse;
    private HttpServerResponse resp;
    
    public StreamSession(NetSocket socket) {
        this.socket = socket;
    }

    public StreamSession(HttpClientResponse clientResponse) {
        this.clientResponse = clientResponse;
    }

    public HttpClientResponse getHttpClientResponse() {
        return clientResponse;
    }

    public NetSocket getNetSocket() {
        return socket;
    }

    public void close() {

        if (!closed) {
            if (socket != null) {
                socket.close();
            }
            closed = true;
        }
    }

    public void markClosed() {
        closed = true;
        if (resp != null) {
            resp.end();
        }
    }

    public void pause() {
        if (!closed) {
            if (socket != null) {
                socket.pause();
            }else if(clientResponse != null){
                clientResponse.pause();
            }
        }
    }

    public void resume() {
        if (!closed) {
            if (socket != null) {
                socket.resume();
            }else if(clientResponse != null){
                clientResponse.resume();
            }
        }
    }

    public void setHttpResponse(HttpServerResponse resp) {
        this.resp = resp;
    }

}
