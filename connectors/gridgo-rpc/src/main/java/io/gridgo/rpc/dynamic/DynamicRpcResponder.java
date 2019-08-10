package io.gridgo.rpc.dynamic;

import io.gridgo.bean.BElement;
import io.gridgo.connector.Producer;
import io.gridgo.framework.ComponentLifecycle;
import io.gridgo.framework.support.Message;
import io.gridgo.rpc.ConectorResolvable;
import io.gridgo.rpc.RpcMessageRegistry;
import io.gridgo.rpc.RpcResponder;

public interface DynamicRpcResponder<KeyType> extends //
        RpcMessageRegistry<Message, BElement>, //
        RpcResponder<KeyType>, //
        ConectorResolvable, //
        ComponentLifecycle {

    void setFixedResponder(Producer fixedResponder);
}
