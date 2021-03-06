/*
 * Copyright 2010 The Apache Software Foundation
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.hbase.master;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.ipc.HBaseRPC;
import org.apache.hadoop.hbase.MasterMonitorProtocol;
import org.apache.hadoop.hbase.protobuf.ProtobufUtil;
import org.apache.hadoop.hbase.protobuf.generated.MasterProtos.IsMasterRunningRequest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.google.protobuf.ServiceException;

@Category(MediumTests.class)
public class TestHMasterRPCException {

  @Test
  public void testRPCException() throws Exception {
    HBaseTestingUtility TEST_UTIL = new HBaseTestingUtility();
    TEST_UTIL.startMiniZKCluster();
    Configuration conf = TEST_UTIL.getConfiguration();
    conf.set(HConstants.MASTER_PORT, "0");

    HMaster hm = new HMaster(conf);

    ServerName sm = hm.getServerName();
    InetSocketAddress isa = new InetSocketAddress(sm.getHostname(), sm.getPort());
    int i = 0;
    //retry the RPC a few times; we have seen SocketTimeoutExceptions if we
    //try to connect too soon. Retry on SocketTimeoutException.
    while (i < 20) { 
      try {
        MasterMonitorProtocol inf = (MasterMonitorProtocol) HBaseRPC.getProxy(
            MasterMonitorProtocol.class,  MasterMonitorProtocol.VERSION, isa, conf, 100 * 10);
        inf.isMasterRunning(null, IsMasterRunningRequest.getDefaultInstance());
        fail();
      } catch (ServiceException ex) {
        IOException ie = ProtobufUtil.getRemoteException(ex);
        if (!(ie instanceof SocketTimeoutException)) {
          if(ie.getMessage().startsWith(
              "org.apache.hadoop.hbase.ipc.ServerNotRunningYetException: Server is not running yet")) {
            return;
          }
        } else {
          System.err.println("Got SocketTimeoutException. Will retry. ");
        }
      } catch (Throwable t) {
        fail("Unexpected throwable: " + t);
      }
      Thread.sleep(100);
      i++;
    }
    fail();
  }

  @org.junit.Rule
  public org.apache.hadoop.hbase.ResourceCheckerJUnitRule cu =
    new org.apache.hadoop.hbase.ResourceCheckerJUnitRule();
}

