package io.gridgo.rpc.dynamic;

import io.gridgo.bean.BElement;
import io.gridgo.connector.Producer;
import io.gridgo.framework.support.Message;

public abstract class EndpointDirectRpcResponder extends AbstractDynamicRpcResponder<String> {

    @Override
    protected String genKey(String replyTo) {
        return replyTo;
    }

    @Override
    public void sendResponse(String replyTo, Message response) {
        Producer responder = lookupResponder(replyTo);
        if (responder == null) {
            responder = this.buildResponder(replyTo);
        }
        responder.send(response);
    }

    @Override
    protected BElement translateMessage(Message request) {
        return request.getPayload().getBody();
    }
}
