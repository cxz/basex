package org.basex.http.ws;

import static org.basex.http.web.WebText.*;
import static org.basex.query.QueryError.*;
import static org.basex.util.Token.*;

import java.io.*;
import java.util.*;

import org.basex.http.web.*;
import org.basex.http.ws.adapter.*;
import org.basex.query.*;
import org.basex.query.ann.*;
import org.basex.query.expr.*;
import org.basex.query.func.*;
import org.basex.query.util.list.*;
import org.basex.query.value.*;
import org.basex.query.value.item.*;
import org.basex.util.*;

/**
 * This class represents a single WebSocket function.
 *
 * @author Johannes Finckh
 * @author BaseX Team 2005-18, BSD License
 */
public class WsFunction extends WebFunction implements Comparable<WsFunction> {
  /** Path of the function. */
  public WsPath path;
  /** Message parameter. */
  public WebParam message;

  /**
   * Constructor.
   * @param function associated user function
   * @param qc query context
   * @param module web module
   */
  public WsFunction(final StaticFunc function, final QueryContext qc, final WebModule module) {
    super(function, qc, module);
  }

  /**
   * Checks if a WebSocket request matches this annotation.
   * @param annotation annotation the annotation parameter
   * @return boolean result of the check
   */
  public boolean matches(final Annotation annotation) {
    for(final Ann ann : function.anns) {
      if(ann.sig == annotation) return true;
    }
    return false;
  }

  /**
   * Checks if an WebSocket request matches this Annotation and Path.
   * @param annotation annotation
   * @param pth path to compare to
   * @return boolean result of check
   */
  public boolean matches(final Annotation annotation, final WsPath pth) {
    for(final Ann ann : function.anns) {
      if(path != null && ann.sig == annotation && path.compareTo(pth) == 0) return true;
    }
    return false;
  }

  /**
   * Checks a function for WebSocket annotations.
   * @return {@code true} if function contains relevant annotations
   * @throws QueryException exception
   */
  public boolean parse() throws QueryException {
    final boolean[] declared = new boolean[function.params.length];
    // counter for annotations that should occur only once
    boolean found = false;
    int count = 0;

    for(final Ann ann : function.anns) {
      final Annotation sig = ann.sig;
      if(sig == null || !eq(sig.uri, QueryText.WS_URI)) continue;

      found = true;
      final Item[] args = ann.args();
      switch(sig) {
        case _WS_HEADER_PARAM:
          final String name = toString(args[0]);
          final QNm var = checkVariable(toString(args[1]), declared);
          final int al = args.length;
          final ItemList items = new ItemList(al - 2);
          for(int a = 2; a < al; a++) items.add(args[a]);
          headerParams.add(new WebParam(var, name, items.value()));
          break;
        case _WS_CLOSE:
        case _WS_CONNECT:
          path = new WsPath(toString(args[0]));
          count++;
          break;
        case _WS_ERROR:
        case _WS_MESSAGE:
          final QNm msg = checkVariable(toString(args[1]), declared);
          message = new WebParam(msg, "message", null);
          path = new WsPath(toString(args[0]));
          count++;
          break;
        default:
          break;
      }
    }

    if(found) {
      if(count == 0) throw error(function.info, ANN_MISSING);
      if(count > 1) throw error(function.info, ANN_CONFLICT);
      final int dl = declared.length;
      for(int d = 0; d < dl; d++) {
        if(!declared[d]) throw error(function.info, VAR_UNDEFINED_X,
            function.params[d].name.string());
      }
    }
    return found;
  }

  /**
   * Processes the request. Parses new modules and discards obsolete ones.
   * @param ws WebSocket
   * @param msg message (can be {@code null}; otherwise string or byte array)
   * @throws IOException I/O exception
   * @throws QueryException query exception
   */
  public void process(final WsAdapter ws, final Object msg) throws IOException, QueryException {
    try {
      System.out.println("WsFunction process msg: " + msg);
      module.process(ws, this, msg);
    } catch(final QueryException ex) {
      if(ex.file() == null) ex.info(function.info);
      throw ex;
    }
  }

  @Override
  public int compareTo(final WsFunction wsxf) {
    return path.compareTo(wsxf.path);
  }

  /**
   * Creates an exception with the specified message.
   * @param msg message
   * @param ext error extension
   * @return exception
   */
  @Override
  public QueryException error(final String msg, final Object... ext) {
    return error(function.info, Util.info(msg, ext));
  }

  /**
   * Creates an exception with the specific message.
   * @param info the StaticFunc info
   * @param msg the message
   * @param ext error extension
   * @return exception
   */
  private QueryException error(final InputInfo info, final String msg, final Object... ext) {
    return BASEX_WS_X.get(info, Util.info(msg, ext));
  }

  /**
   * Binds the function parameters.
   * @param args arguments
   * @param qc query context
   * @param msg message (can be {@code null}; otherwise string or byte array)
   * @param header headers
   * @throws QueryException query exception
   */
  public void bind(final Expr[] args, final QueryContext qc, final Object msg,
      final Map<String, String> header) throws QueryException {

    // cache headers and message
    final Map<String, Value> values = new HashMap<>();
    header.forEach((k, v) -> values.put(k, new Atm(v)));
    if(msg != null) values.put("message", msg instanceof byte[] ?
      B64.get((byte[]) msg) : Str.get((String) msg));

    for(final WebParam rxp : headerParams) {
      bind(rxp.var, args, values.get(rxp.name), qc);
    }
    if(message != null) {
      bind(message.var, args, values.get(message.name), qc);
    }
  }
}
