package io.fire.core.server.modules.request;

import io.fire.core.common.interfaces.RequestBody;
import io.fire.core.common.packets.CompleteRequestPacket;
import io.fire.core.server.modules.client.objects.FireIoConnection;
import io.fire.core.server.modules.client.superclasses.Client;
import io.fire.core.server.modules.request.interfaces.RequestExecutor;
import io.fire.core.server.modules.request.objects.RequestResponse;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.*;

@NoArgsConstructor
public class RequestModule {

    //a list of request channels and all their listeners/executors
    private Map<String, List<RequestExecutor>> requestExecutors = new HashMap<>();

    public void trigger(String channel, RequestBody body, Client client, UUID uuid) {
        //trigger request listener/executors as we received one from a client
        //check if we have any registered handlers for this request
        if (!requestExecutors.containsKey(channel)) return;
        //loop for all handlers
        for (RequestExecutor executor : requestExecutors.get(channel)) {
            //create new completable executor
            RequestResponse requestResponse = new RequestResponse();
            //set the request id
            requestResponse.setRequestId(uuid);
            //generate function to execute when its marked as completed
            requestResponse.setCompletableRequest(requestBody -> {
                //cast the client to its connection
                FireIoConnection fireIoClient = (FireIoConnection) client;
                //crate a new packet to let the client know that we finished it as well with a result of the request
                CompleteRequestPacket completeRequestPacket = new CompleteRequestPacket();
                //set the if so the client can trigger the correct callback
                completeRequestPacket.setRequestId(uuid);
                //set the reply body with the data
                completeRequestPacket.setResult(requestBody);
                try {
                    //get the internal handler and send the internal packet
                    fireIoClient.getHandler().emit(completeRequestPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            //call the listener/executor with the client, body and the completable response
            executor.onRequest(client, body, requestResponse);
        }
    }

    public void register(String channel, RequestExecutor executor) {
        //register executor
        //check if channel exists, if it does not, then create it first
        if (!requestExecutors.containsKey(channel)) requestExecutors.put(channel, new ArrayList<>());
        //add executor to excising channel
        requestExecutors.get(channel).add(executor);
    }

}
