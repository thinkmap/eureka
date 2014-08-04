/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.eureka.transport.utils;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.netflix.eureka.transport.MessageBroker;
import rx.Observable;
import rx.functions.Action1;

/**
 * Helper methods/classes.
 *
 * @author Tomasz Bak
 */
public class BrokerUtils {

    /**
     * Orchestrate client - server broker connection setup.
     */
    public static class BrokerPair {

        private final MessageBroker clientBroker;
        private final MessageBroker serverBroker;

        public BrokerPair(Observable<MessageBroker> serverObservable, Observable<MessageBroker> clientObservable) throws InterruptedException, TimeoutException {
            final BlockingQueue<MessageBroker> queue = new LinkedBlockingQueue<MessageBroker>(1);
            serverObservable.subscribe(new Action1<MessageBroker>() {
                @Override
                public void call(MessageBroker messageBroker) {
                    queue.add(messageBroker);
                }
            });
            clientBroker = clientObservable.toBlocking().first();
            serverBroker = queue.poll(1000, TimeUnit.MILLISECONDS);
            if (serverBroker == null) {
                throw new TimeoutException("no connection on server side");
            }
        }

        public MessageBroker getClientBroker() {
            return clientBroker;
        }

        public MessageBroker getServerBroker() {
            return serverBroker;
        }
    }
}
