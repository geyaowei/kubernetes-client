/**
 * Copyright (C) 2015 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.kubernetes.client.internal.readiness;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.KubernetesClientTimeoutException;
import io.fabric8.kubernetes.client.Watcher;

public class ReadinessWatcher<T extends HasMetadata> implements Watcher<T> {


  private final CountDownLatch latch = new CountDownLatch(1);
  private final AtomicReference<T> reference = new AtomicReference<>();

  private final T resource;

  public ReadinessWatcher(T resource) {
    this.resource = resource;
  }

  public void eventReceived(Action action, T resource) {
    switch (action) {
      case MODIFIED:
        if (Readiness.isReady(resource)) {
          reference.set(resource);
          latch.countDown();
        }
        break;
      default:
    }
  }

  @Override
  public void onClose(KubernetesClientException e) {

  }

  public T await(long amount, TimeUnit timeUnit) {
    try {
      if (latch.await(amount, timeUnit)) {
        return reference.get();
      }
      throw new KubernetesClientTimeoutException(resource, amount, timeUnit);
    } catch (InterruptedException e) {
      throw new KubernetesClientTimeoutException(resource, amount, timeUnit);
    }
  }
}
