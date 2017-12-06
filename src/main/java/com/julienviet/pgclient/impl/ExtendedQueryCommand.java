/*
 * Copyright (C) 2017 Julien Viet
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
 *
 */

package com.julienviet.pgclient.impl;

import com.julienviet.pgclient.Tuple;
import com.julienviet.pgclient.codec.DataFormat;
import com.julienviet.pgclient.codec.decoder.DecodeContext;
import com.julienviet.pgclient.codec.decoder.ResultDecoder;
import com.julienviet.pgclient.codec.encoder.message.Bind;
import com.julienviet.pgclient.codec.encoder.message.Execute;
import com.julienviet.pgclient.codec.encoder.message.Parse;
import com.julienviet.pgclient.codec.encoder.message.Sync;

import java.util.List;

public class ExtendedQueryCommand<T> extends ExtendedQueryCommandBase<T> {

  private final Tuple params;

  ExtendedQueryCommand(PreparedStatement ps,
                       Tuple params,
                       ResultDecoder<T> decoder,
                       QueryResultHandler<T> handler) {
    this(ps, params, 0, null, false, decoder, handler);
  }

  ExtendedQueryCommand(PreparedStatement ps,
                       Tuple params,
                       int fetch,
                       String portal,
                       boolean suspended,
                       ResultDecoder<T> decoder,
                       QueryResultHandler<T> handler) {
    super(ps, fetch, portal, suspended, decoder, handler);
    this.params = params;
  }

  @Override
  void exec(SocketConnection conn) {
    conn.decodeQueue.add(new DecodeContext(false, ps.rowDesc, DataFormat.BINARY, decoder));
    if (suspended) {
      conn.writeMessage(new Execute().setPortal(portal).setRowCount(fetch));
      conn.writeMessage(Sync.INSTANCE);
    } else {
      if (ps.statement == 0) {
        conn.writeMessage(new Parse(ps.sql));
      }
      conn.writeMessage(new Bind().setParamValues((List<Object>) params).setDataTypes(ps.paramDesc.getParamDataTypes()).setPortal(portal).setStatement(ps.statement));
      conn.writeMessage(new Execute().setPortal(portal).setRowCount(fetch));
      conn.writeMessage(Sync.INSTANCE);
    }
  }
}
