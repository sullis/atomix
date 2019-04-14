/*
 * Copyright 2017-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.atomix.primitive.partition.impl;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.google.common.collect.Sets;
import io.atomix.primitive.operation.OperationMetadata;
import io.atomix.primitive.operation.OperationType;
import io.atomix.primitive.operation.PrimitiveOperation;
import io.atomix.primitive.partition.GroupMember;
import io.atomix.primitive.partition.ManagedPrimaryElection;
import io.atomix.primitive.partition.PartitionId;
import io.atomix.primitive.partition.PrimaryElection;
import io.atomix.primitive.partition.PrimaryElectionEvent;
import io.atomix.primitive.partition.PrimaryElectionService;
import io.atomix.primitive.partition.PrimaryTerm;
import io.atomix.primitive.session.SessionClient;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.atomix.utils.concurrent.Futures.uncheck;

/**
 * Leader elector based primary election.
 */
public class DefaultPrimaryElection implements ManagedPrimaryElection {
  private final PartitionId partitionId;
  private final SessionClient proxy;
  private final PrimaryElectionService service;
  private final Set<Consumer<PrimaryElectionEvent>> listeners = Sets.newCopyOnWriteArraySet();
  private final Consumer<PrimaryElectionEvent> eventListener;
  private final AtomicBoolean started = new AtomicBoolean();

  public DefaultPrimaryElection(PartitionId partitionId, SessionClient proxy, PrimaryElectionService service) {
    this.partitionId = checkNotNull(partitionId);
    this.proxy = proxy;
    this.service = service;
    this.eventListener = event -> {
      if (event.getPartitionId().equals(partitionId)) {
        listeners.forEach(l -> l.accept(event));
      }
    };
    service.addListener(eventListener);
  }

  @Override
  public CompletableFuture<PrimaryTerm> enter(GroupMember member) {
    return proxy.execute(PrimitiveOperation.newBuilder()
        .setId(OperationMetadata.newBuilder()
            .setType(OperationType.COMMAND)
            .setName("ENTER")
            .build())
        .setValue(EnterRequest.newBuilder()
            .setPartitionId(partitionId)
            .setMember(member)
            .build()
            .toByteString())
        .build())
        .thenApply(uncheck(EnterResponse::parseFrom))
        .thenApply(EnterResponse::getTerm);
  }

  @Override
  public CompletableFuture<PrimaryTerm> getTerm() {
    return proxy.execute(PrimitiveOperation.newBuilder()
        .setId(OperationMetadata.newBuilder()
            .setType(OperationType.QUERY)
            .setName("GET_TERM")
            .build())
        .setValue(GetTermRequest.newBuilder()
            .setPartitionId(partitionId)
            .build()
            .toByteString())
        .build())
        .thenApply(uncheck(GetTermResponse::parseFrom))
        .thenApply(GetTermResponse::getTerm);
  }

  @Override
  public synchronized void addListener(Consumer<PrimaryElectionEvent> listener) {
    listeners.add(checkNotNull(listener));
  }

  @Override
  public synchronized void removeListener(Consumer<PrimaryElectionEvent> listener) {
    listeners.remove(checkNotNull(listener));
  }

  @Override
  public CompletableFuture<PrimaryElection> start() {
    started.set(true);
    return CompletableFuture.completedFuture(this);
  }

  @Override
  public boolean isRunning() {
    return started.get();
  }

  @Override
  public CompletableFuture<Void> stop() {
    service.removeListener(eventListener);
    started.set(false);
    return CompletableFuture.completedFuture(null);
  }
}
