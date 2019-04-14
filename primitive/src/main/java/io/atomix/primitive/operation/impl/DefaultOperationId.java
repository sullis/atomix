/*
 * Copyright 2019-present Open Networking Foundation
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
package io.atomix.primitive.operation.impl;

import io.atomix.primitive.operation.OperationId;
import io.atomix.primitive.operation.OperationType;

/**
 * Default operation ID.
 */
public class DefaultOperationId implements OperationId {
  private final String name;
  private final OperationType type;

  public DefaultOperationId(String name, OperationType type) {
    this.name = name;
    this.type = type;
  }

  @Override
  public String operation() {
    return name;
  }

  @Override
  public OperationType type() {
    return type;
  }
}
