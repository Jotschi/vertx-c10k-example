package io.vertx.core.net.impl.transport;

/*
 * Copyright (c) 2011-2019 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */


import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.InternetProtocolFamily;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.incubator.channel.uring.IOUring;
import io.netty.incubator.channel.uring.IOUringChannelOption;
import io.netty.incubator.channel.uring.IOUringDatagramChannel;
import io.netty.incubator.channel.uring.IOUringEventLoopGroup;
import io.netty.incubator.channel.uring.IOUringServerSocketChannel;
import io.netty.incubator.channel.uring.IOUringSocketChannel;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.impl.SocketAddressImpl;

import java.net.SocketAddress;
import java.util.concurrent.ThreadFactory;

import org.apache.commons.lang3.NotImplementedException;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
class IoUringTransport extends Transport {

  IoUringTransport() {
  }

  @Override
  public SocketAddress convert(io.vertx.core.net.SocketAddress address) {
    if (address.isDomainSocket()) {
      return new DomainSocketAddress(address.path());
    } else {
      return super.convert(address);
    }
  }

  @Override
  public io.vertx.core.net.SocketAddress convert(SocketAddress address) {
    if (address instanceof DomainSocketAddress) {
      return new SocketAddressImpl(((DomainSocketAddress) address).path());
    }
    return super.convert(address);
  }

  @Override
  public boolean isAvailable() {
    return IOUring.isAvailable();
  }

  @Override
  public Throwable unavailabilityCause() {
    return IOUring.unavailabilityCause();
  }

  @Override
  public EventLoopGroup eventLoopGroup(int type, int nThreads, ThreadFactory threadFactory, int ioRatio) {
    IOUringEventLoopGroup eventLoopGroup = new IOUringEventLoopGroup(nThreads, threadFactory);
    return eventLoopGroup;
  }

  @Override
  public DatagramChannel datagramChannel() {
    return new IOUringDatagramChannel();
  }

  @Override
  public DatagramChannel datagramChannel(InternetProtocolFamily family) {
    return new IOUringDatagramChannel();
  }

  @Override
  public ChannelFactory<? extends Channel> channelFactory(boolean domainSocket) {
    if (domainSocket) {
      throw new NotImplementedException();
    } else {
      return IOUringSocketChannel::new;
    }
  }

  @Override
  public ChannelFactory<? extends ServerChannel> serverChannelFactory(boolean domainSocket) {
    if (domainSocket) {
      throw new NotImplementedException();
    } else {
      return IOUringServerSocketChannel::new;
    }
  }

  @Override
  public void configure(NetServerOptions options, boolean domainSocket, ServerBootstrap bootstrap) {
    if (!domainSocket) {
      bootstrap.option(IOUringChannelOption.SO_REUSEPORT, options.isReusePort());
    }
    super.configure(options, domainSocket, bootstrap);
  }

  @Override
  public void configure(DatagramChannel channel, DatagramSocketOptions options) {
    channel.config().setOption(IOUringChannelOption.SO_REUSEPORT, options.isReusePort());
    super.configure(channel, options);
  }
}
