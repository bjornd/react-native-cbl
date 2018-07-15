
package com.reactlibrary;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.ImageView;

import com.couchbase.lite.Blob;
import com.couchbase.lite.DataSource;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseConfiguration;
import com.couchbase.lite.Dictionary;
import com.couchbase.lite.Document;
import com.couchbase.lite.DocumentChange;
import com.couchbase.lite.DocumentChangeListener;
import com.couchbase.lite.Endpoint;
import com.couchbase.lite.Expression;
import com.couchbase.lite.Meta;
import com.couchbase.lite.MetaExpression;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.OrderBy;
import com.couchbase.lite.Ordering;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryBuilder;
import com.couchbase.lite.QueryChange;
import com.couchbase.lite.QueryChangeListener;
import com.couchbase.lite.Replicator;
import com.couchbase.lite.ReplicatorConfiguration;
import com.couchbase.lite.Result;
import com.couchbase.lite.ResultSet;
import com.couchbase.lite.Select;
import com.couchbase.lite.SelectResult;
import com.couchbase.lite.URLEndpoint;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;

import com.couchbase.lite.CouchbaseLiteException;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.uimanager.NativeViewHierarchyManager;
import com.facebook.react.uimanager.UIBlock;
import com.facebook.react.uimanager.UIManagerModule;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.util.UUID;

import javax.annotation.Nullable;



public class RNReactNativeCblModule extends ReactContextBaseJavaModule {

  private final ReactContext mReactContext;
  private Database db = null;
  private final HashMap<String, Document> liveDocuments = new HashMap<>();
  private final HashMap<String, Query> liveQueries = new HashMap<>();

  @Override
  public String getName() {
    return "RNReactNativeCbl";
  }

  public RNReactNativeCblModule(ReactApplicationContext reactContext) {
    super(reactContext);
    mReactContext = reactContext;
  }

  private void sendEvent(String eventName, @Nullable WritableMap params) {
    mReactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(eventName, params);
  }

  @ReactMethod
  public void openDb(String name, Boolean installPrebuildDb, Promise promise) {
    if (this.db == null) {
      try {
        DatabaseConfiguration config = new DatabaseConfiguration(mReactContext.getApplicationContext());
        this.db = new Database(name, config);
        /*if (installPrebuildDb) {
          Database db = manager.getExistingDatabase(name);
          if (db == null) {
            ZipUtils.unzip(this.mReactContext.getAssets().open(name + ".zip"), manager.getContext().getFilesDir());
          }
        }*/
        //this.db.addChangeListener(this);
        promise.resolve(null);
      } catch (CouchbaseLiteException e) {
        promise.reject("open_database", "Can not open database", e);
      }
    } else {
      promise.resolve(null);
    }
  }

  @ReactMethod
  public void getDocument(String docId, Promise promise) {
    Document doc = this.db.getDocument(docId);
    if (doc == null) {
      promise.reject("get_document", "Can not find document");
    } else {
      promise.resolve( ConversionUtil.toWritableMap( this.serializeDocument(doc) ) );
    }
  }

  @ReactMethod
  public void createDocument(ReadableMap properties, Promise promise) {
    MutableDocument doc = new MutableDocument();
    doc.setData( properties.toHashMap() );
    try {
      this.db.save(doc);
      promise.resolve(doc.getId());
    } catch (CouchbaseLiteException e) {
      promise.reject("create_document", "Can not create document", e);
    }
  }

  @ReactMethod
  public void updateDocument(String docId, ReadableMap properties, Promise promise) {
    Document doc = this.db.getDocument(docId);
    if (doc == null) {
      promise.reject("update_document", "Can not find document");
      return;
    }
    MutableDocument mutableDoc = doc.toMutable();
    for (Map.Entry<String, Object> entry : properties.toHashMap().entrySet()) {
      mutableDoc.setValue(entry.getKey(), entry.getValue());
    }
    try {
      this.db.save( mutableDoc );
      promise.resolve(null);
    } catch (CouchbaseLiteException e) {
      promise.reject("update_document", "Can not update document", e);
    }
  }

  @ReactMethod
  public void deleteDocument(String docId, Promise promise) {
    Document doc = this.db.getDocument(docId);
    try {
      this.db.delete(doc);
      promise.resolve(null);
    } catch (CouchbaseLiteException e) {
      promise.reject("delete_document", "Can not delete document", e);
    }
  }

  @ReactMethod
  public void createLiveDocument(String docId, Promise promise) {
    Document doc = this.db.getDocument(docId);
    final String uuid = UUID.randomUUID().toString();
    final RNReactNativeCblModule self = this;
    this.db.addDocumentChangeListener(docId, new DocumentChangeListener() {
      public void changed(DocumentChange change) {
        WritableMap params = Arguments.createMap();
        params.putString("uuid", uuid);
        Document changedDoc = change.getDatabase().getDocument( change.getDocumentID() );
        Map<String, Object> props;
        if (changedDoc == null) {
          props = new HashMap<String, Object>();
        } else {
          props = self.serializeDocument( changedDoc );
        }
        params.putMap("data", ConversionUtil.toWritableMap(props));
        self.sendEvent("liveDocumentChange", params);
      }
    });
    this.liveDocuments.put(uuid, doc);
    promise.resolve(uuid);
    WritableMap params = Arguments.createMap();
    params.putString("uuid", uuid);
    params.putMap("data", ConversionUtil.toWritableMap(this.serializeDocument(doc)));
    this.sendEvent("liveDocumentChange", params);
  }

  @ReactMethod
  public void destroyLiveDocument(String uuid, Promise promise) {
    this.liveDocuments.remove(uuid);
    promise.resolve(null);
  }

  @ReactMethod
  public void query(String view, ReadableArray params, Promise promise) {
    Query query = QueryBuilder.select( SelectResult.all() );
    try {
      ResultSet result = query.execute();
      promise.resolve( ConversionUtil.toWritableArray( this.getQueryResults(result).toArray() ) );
    } catch (CouchbaseLiteException e) {
      promise.reject("query", "Error running query", e);
    }
  }

  @ReactMethod
  public void createLiveQuery(ReadableArray params, Promise promise) {
    final String uuid = UUID.randomUUID().toString();
    final RNReactNativeCblModule self = this;
    Query query = this.queryFromJson(params);
    query.addChangeListener(new QueryChangeListener() {
      public void changed(QueryChange change) {
        WritableMap eventParams = Arguments.createMap();
        eventParams.putString("uuid", uuid);
        eventParams.putArray("data", ConversionUtil.toWritableArray( self.getQueryResults(change.getResults()).toArray() ));
        self.sendEvent("liveQueryChange", eventParams);
      }
    });
    this.liveQueries.put(uuid, query);
    promise.resolve(uuid);

    try {
      ResultSet result = query.execute();
      WritableMap eventParams = Arguments.createMap();
      eventParams.putString("uuid", uuid);
      eventParams.putArray("data", ConversionUtil.toWritableArray( this.getQueryResults(result).toArray() ) );
      self.sendEvent("liveQueryChange", eventParams);
    } catch (CouchbaseLiteException e) {
      promise.reject("live_query", "Error running live query for the first time", e);
    }
  }

  @ReactMethod
  public void destroyLiveQuery(String uuid, Promise promise) {
    this.liveQueries.remove(uuid);
    promise.resolve(null);
  }

  private Query queryFromJson(ReadableArray params) {
    ReadableMap select = params.getMap(1);

    ArrayList what = select.getArray("WHAT").toArrayList();
    ArrayList<SelectResult> results = new ArrayList<>();
    for (Object whatArg : what) {
      results.add( whatArg instanceof String ? SelectResult.property((String)whatArg) : SelectResult.expression( this.toExp(whatArg) ) );
    }

    List where = select.getArray("WHERE").toArrayList();

    List orderByParams = select.getArray("ORDER_BY").toArrayList();
    ArrayList<Ordering> orderBy = new ArrayList<>();
    for (Object orderByArg : orderByParams) {
      if (orderByArg instanceof String) {
        orderBy.add( Ordering.property((String)orderByArg) );
      } else {
        List order = (List)orderByArg;
        if (order.get(0).equals("DESC")) {
          if (order.get(1) instanceof String) {
            orderBy.add( Ordering.property((String)(order.get(1))).descending() );
          } else {
            orderBy.add( Ordering.expression(this.toExp(order.get(1))).descending() );
          }
        } else {
          orderBy.add( Ordering.expression(this.toExp(order)) );
        }
      }
    }

    return QueryBuilder.select( results.toArray(new SelectResult[results.size()]) )
            .from( DataSource.database(this.db) )
            .where( this.toExp(where) )
            .orderBy( orderBy.toArray(new Ordering[orderBy.size()]) );
  }

  private Expression toExp(Object json){
    if (json instanceof List) {
      List exp = (List)json;
      String operator = (String)exp.get(0);
      switch (operator) {
        case "+": return this.toExp(exp.get(1)).add( this.toExp(exp.get(2)) );
        case "-": return this.toExp(exp.size() == 2 ? 0 : exp.get(1)).subtract( this.toExp(exp.get(2)) );
        case "*": return this.toExp(exp.get(1)).multiply( this.toExp(exp.get(2)) );
        case "/": return this.toExp(exp.get(1)).divide( this.toExp(exp.get(2)) );
        case "%": return this.toExp(exp.get(1)).modulo( this.toExp(exp.get(2)) );
        case "=": return this.toExp(exp.get(1)).equalTo( this.toExp(exp.get(2)) );
        case "!=": return this.toExp(exp.get(1)).notEqualTo( this.toExp(exp.get(2)) );
        case "<": return this.toExp(exp.get(1)).lessThan( this.toExp(exp.get(2)) );
        case "<=": return this.toExp(exp.get(1)).lessThanOrEqualTo( this.toExp(exp.get(2)) );
        case ">": return this.toExp(exp.get(1)).greaterThan( this.toExp(exp.get(2)) );
        case ">=": return this.toExp(exp.get(1)).greaterThanOrEqualTo( this.toExp(exp.get(2)) );
        case "BETWEEN": return this.toExp(exp.get(1)).between( this.toExp(exp.get(2)), this.toExp(exp.get(3)) );
        case "IS": return this.toExp(exp.get(1)).is( this.toExp(exp.get(2)) );
        case "IS NOT": return this.toExp(exp.get(1)).isNot( this.toExp(exp.get(2)) );
        case "LIKE": return this.toExp(exp.get(1)).like( this.toExp(exp.get(2)) );
        case "MATCH": return this.toExp(exp.get(1)).regex( this.toExp(exp.get(2)) );
        case "IN": return this.toExp(exp.get(1)).in( this.toExp(exp.get(2)) );
        case "IS NULL OR MISSING": return this.toExp(exp.get(1)).isNullOrMissing();
        case "IS NOT NULL OR MISSING": return this.toExp(exp.get(1)).notNullOrMissing();
        case "NOT": return Expression.negated( this.toExp(exp.get(1)) );
        case "AND": return this.toExp(exp.get(1)).and( this.toExp(exp.get(2)) );
        case "OR": return this.toExp(exp.get(1)).or( this.toExp(exp.get(2)) );
        case "?": return Expression.parameter( (String)exp.get(1) );
        case ".": return Expression.property( exp.size() > 1 ? TextUtils.join(",", exp.subList(1, exp.size())) : "" );
      }
    }
    if (json instanceof Number || json instanceof String || json instanceof Boolean) {
      return Expression.value( json );
    }
    return null;
  }

  private ArrayList getQueryResults(ResultSet result) {
    ArrayList<Map<String, Object>> list = new ArrayList<>();
    for (Result row : result.allResults()) {
      Map<String, Object> properties = new HashMap<String, Object>();
      properties.put("id", row.getString("_id"));
      Dictionary values = row.getDictionary(this.db.getName());
      if (values != null) {
        properties.putAll( values.toMap() );
      }
      list.add( properties );
    }
    return list;
  }

  private Map<String, Object> serializeDocument(Document document) {
    Map<String, Object> properties = new HashMap<>(document.toMap());
    properties.put("id", document.getId());
    for(Map.Entry<String, Object> entry: properties.entrySet()) {
      if (entry.getValue() instanceof Blob) {
        Blob blob = (Blob)entry.getValue();
        Map<String, Object> blobProps = new HashMap<>(blob.getProperties());
        String path = this.db.getPath().concat("Attachments/").concat( blob.digest().substring(5) ).concat(".blob");
        try {
          blobProps.put("url", new File(path).toURI().toURL().toString());
        } catch (MalformedURLException e) {
          blobProps.put("url", null);
        }
        properties.put(entry.getKey(), blobProps);
      }
    }
    return properties;
  }

  @ReactMethod
  public void startReplication(String remoteUrl, String facebookToken, Promise promise) {
    try {
      Endpoint targetEndpoint = new URLEndpoint(new URI(remoteUrl));
      ReplicatorConfiguration config = new ReplicatorConfiguration(this.db, targetEndpoint);
      config.setReplicatorType(ReplicatorConfiguration.ReplicatorType.PUSH_AND_PULL);
      //replConfig.setAuthenticator(new BasicAuthenticator("john", "pass"));
      Replicator replicator = new Replicator(config);
      replicator.start();
      promise.resolve(null);
    } catch (URISyntaxException e) {
      promise.reject("start_replication", "Malformed remote URL", e);
    }
  }

  @ReactMethod
  public void addAttachment(String contentUri, String attachmentName, String documentId, Promise promise) {
    try {
      Uri uri = Uri.parse(contentUri);
      InputStream stream = this.mReactContext.getContentResolver().openInputStream(uri);
      String contentType = this.mReactContext.getContentResolver().getType(uri);
      MutableDocument doc = this.db.getDocument(documentId).toMutable();
      doc.setBlob( attachmentName, new Blob(contentType, stream));
      this.db.save(doc);
      promise.resolve(null);
    } catch (CouchbaseLiteException e) {
      promise.reject("add_attachment", "Can not add attachment", e);
    } catch (FileNotFoundException e) {
      promise.reject("add_attachment", "File not found", e);
    }
  }

  @ReactMethod
  public void removeAttachment(String attachmentName, String documentId, Promise promise) {
    try {
      MutableDocument doc = this.db.getDocument(documentId).toMutable();
      doc.remove( attachmentName );
      this.db.save( doc );
      promise.resolve(null);
    } catch (CouchbaseLiteException e) {
      promise.reject("remove_attachment", "Can not remove attachment", e);
    }
  }

  /*@ReactMethod
  public void connectAttachmentToImage(final int reactTag, String documentId, String attachmentName) {
    try {
      Document doc = this.db.getDocument(documentId);
      Revision rev = doc.getCurrentRevision();
      Attachment att = rev.getAttachment(attachmentName);
      if (att != null) {
        InputStream is = att.getContent();
        final Drawable d = Drawable.createFromStream(is, attachmentName);
        UIManagerModule uiManager = this.mReactContext.getNativeModule(UIManagerModule.class);
        uiManager.addUIBlock(new UIBlock() {
          @Override
          public void execute(NativeViewHierarchyManager nativeViewHierarchyManager) {
            ImageView view = (ImageView)nativeViewHierarchyManager.resolveView(reactTag);
            view.setImageDrawable(d);
          }
        });
      }
    } catch (CouchbaseLiteException e) {

    }
  }*/
}
