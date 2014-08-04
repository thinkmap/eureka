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

package com.netflix.eureka.client.transport.registration.protocol.http;

import com.netflix.eureka.client.transport.common.http.HttpErrorHandler;
import com.netflix.eureka.client.transport.registration.RegistrationClient;
import com.netflix.eureka.protocol.registration.Update;
import com.netflix.eureka.registry.InstanceInfo;
import com.netflix.eureka.utils.Json;
import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import rx.Observable;
import rx.Observable.Operator;
import rx.functions.Func1;
import rx.subjects.ReplaySubject;

/**
 * @author Tomasz Bak
 */
public class HttpRegistrationClient implements RegistrationClient {

    private static final Operator<HttpClientResponse<ByteBuf>, HttpClientResponse<ByteBuf>> errorHandler = new HttpErrorHandler<ByteBuf>();

    private final String baseURI;
    private final HttpClient<ByteBuf, ByteBuf> httpClient;

    private final ReplaySubject<Void> statusSubject = ReplaySubject.create();

    public HttpRegistrationClient(String baseURI, HttpClient<ByteBuf, ByteBuf> httpClient) {
        this.baseURI = baseURI;
        this.httpClient = httpClient;
    }

    @Override
    public Observable<Void> register(InstanceInfo instanceInfo) {
        return httpClient.submit(HttpClientRequest.createPost(baseURI + "/apps")
                .withContent(Json.toByteBufJson(instanceInfo))
                .withHeader("ContentType", "application/json"))
                .lift(errorHandler)
                .flatMap(new Func1<HttpClientResponse<ByteBuf>, Observable<Void>>() {
                    @Override
                    public Observable<Void> call(HttpClientResponse<ByteBuf> response) {
                        return Observable.empty();
                    }
                });
    }

    @Override
    public Observable<Void> update(InstanceInfo instanceInfo, Update update) {
        return httpClient.submit(HttpClientRequest.createPut(baseURI + "/apps/" + instanceInfo.getId())
                .withContent(Json.toByteBufJson(update))
                .withHeader("ContentType", "application/json"))
                .lift(errorHandler)
                .flatMap(new Func1<HttpClientResponse<ByteBuf>, Observable<Void>>() {
                    @Override
                    public Observable<Void> call(HttpClientResponse<ByteBuf> response) {
                        return Observable.empty();
                    }
                });
    }

    @Override
    public Observable<Void> unregister(InstanceInfo instanceInfo) {
        return httpClient.submit(HttpClientRequest.createDelete(baseURI + "/apps/" + instanceInfo.getId()))
                .lift(errorHandler)
                .flatMap(new Func1<HttpClientResponse<ByteBuf>, Observable<Void>>() {
                    @Override
                    public Observable<Void> call(HttpClientResponse<ByteBuf> response) {
                        return Observable.empty();
                    }
                });
    }

    @Override
    public void shutdown() {
        try {
            httpClient.shutdown();
        } finally {
            statusSubject.onCompleted();
        }
    }

    @Override
    public Observable<Void> lifecycleObservable() {
        return statusSubject.asObservable();
    }
}
