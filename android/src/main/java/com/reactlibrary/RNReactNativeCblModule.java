
package com.reactlibrary;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.ImageView;

import com.couchbase.lite.Attachment;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.DocumentChange;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.Reducer;
import com.couchbase.lite.Revision;
import com.couchbase.lite.UnsavedRevision;
import com.couchbase.lite.View;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.auth.Authenticator;
import com.couchbase.lite.auth.AuthenticatorFactory;
import com.couchbase.lite.replicator.Replication;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableMap;

import com.couchbase.lite.Manager;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.util.ZipUtils;
import com.couchbase.lite.javascript.JavaScriptViewCompiler;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.uimanager.NativeViewHierarchyManager;
import com.facebook.react.uimanager.UIBlock;
import com.facebook.react.uimanager.UIManagerModule;
import com.facebook.react.views.image.ReactImageView;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.util.UUID;

import javax.annotation.Nullable;

public class RNReactNativeCblModule extends ReactContextBaseJavaModule implements Database.ChangeListener {

  private final ReactContext mReactContext;
  private Database db = null;
  private final HashMap<String, Document> liveDocuments = new HashMap<>();
  private final HashMap<String, LiveQuery> liveQueries = new HashMap<>();

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
        View.setCompiler(new JavaScriptViewCompiler());
        Manager manager = new Manager(new AndroidContext(mReactContext.getApplicationContext()), Manager.DEFAULT_OPTIONS);
        if (installPrebuildDb) {
          Database db = manager.getExistingDatabase(name);
          if (db == null) {
            ZipUtils.unzip(this.mReactContext.getAssets().open(name + ".zip"), manager.getContext().getFilesDir());
          }
        }
        this.db = manager.getDatabase(name);
        this.db.addChangeListener(this);
        promise.resolve(null);
      } catch (IOException | CouchbaseLiteException e) {
        promise.reject("open_database", "Can not open database", e);
      }
    } else {
      promise.resolve(null);
    }
  }

  @ReactMethod
  public void getDocument(String docId, Promise promise) {
    Document doc = this.db.getExistingDocument(docId);
    if (doc == null) {
      promise.reject("update_document", "Can not find document");
    } else {
      promise.resolve( ConversionUtil.toWritableMap( new HashMap<String, Object>(doc.getProperties())) );
    }
  }

  @ReactMethod
  public void createDocument(ReadableMap properties, Promise promise) {
    Document doc = this.db.createDocument();
    Map<String, Object> props = new HashMap<>();
    props.putAll(properties.toHashMap());
    try {
      doc.putProperties(props);
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
    Map<String, Object> props = new HashMap<>();
    if (doc.getCurrentRevision() != null) {
      props.putAll(doc.getProperties());
    }
    props.putAll(properties.toHashMap());
    try {
      doc.putProperties(props);
      promise.resolve(null);
    } catch (CouchbaseLiteException e) {
      promise.reject("update_document", "Can not update document", e);
    }
  }

  @ReactMethod
  public void deleteDocument(String docId, Promise promise) {
    Document doc = this.db.getDocument(docId);
    try {
      doc.delete();
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
    doc.addChangeListener(new Document.ChangeListener() {
      @Override
      public void changed(Document.ChangeEvent event) {
        DocumentChange docChange = event.getChange();
        WritableMap params = Arguments.createMap();
        params.putString("uuid", uuid);
        Map<String, Object> props = new HashMap<>(docChange.getAddedRevision().getProperties());
        params.putMap("data", ConversionUtil.toWritableMap(props));
        self.sendEvent("liveDocumentChange", params);
      }
    });
    this.liveDocuments.put(uuid, doc);
    promise.resolve(uuid);
    WritableMap params = Arguments.createMap();
    params.putString("uuid", uuid);
    Map<String, Object> props = new HashMap<>(doc.getProperties());
    params.putMap("data", ConversionUtil.toWritableMap(props));
    this.sendEvent("liveDocumentChange", params);
  }

  @ReactMethod
  public void destroyLiveDocument(String uuid, Promise promise) {
    this.liveDocuments.remove(uuid);
    promise.resolve(null);
  }

  private View getView(String view) {
    View dbview = this.db.getView(view);
    if (dbview.getMap() == null) {
      String[] path = view.split("/");
      Document ddoc = this.db.getDocument("_design/" + path[0]);
      String viewName = TextUtils.join("/", Arrays.copyOfRange(path, 1, path.length) );
      Map<String, Object> container = (Map<String, Object>) ddoc.getProperty("views");
      Map<String, String> viewDesc = (Map<String, String>)container.get(viewName);
      String mapString = viewDesc.get("map");
      String reduceString = viewDesc.get("reduce");
      Mapper mapBlock = View.getCompiler().compileMap(mapString, "javascript");
      Reducer reduceBlock = null;
      if (reduceString != null) {
        reduceBlock = View.getCompiler().compileReduce(reduceString, "javascript");
      }
      dbview.setMapReduce(mapBlock, reduceBlock, "1");
    }
    return dbview;
  }

  private void setQueryParams(Query query, ReadableMap params) {
    if (params == null) {
      return;
    }
    if (params.hasKey("groupLevel")) {
      query.setGroupLevel( params.getInt("groupLevel") );
    }
    if (params.hasKey("keys")) {
      query.setKeys( params.getArray("keys").toArrayList() );
    }
    if (params.hasKey("startKey")) {
      if (params.getType("startKey") == ReadableType.Array) {
        query.setStartKey( params.getArray("startKey").toArrayList() );
      } else {
        query.setStartKey( params.getString("startKey") );
      }
    }
    if (params.hasKey("endKey")) {
      if (params.getType("endKey") == ReadableType.Array) {
        query.setEndKey( params.getArray("endKey").toArrayList() );
      } else {
        query.setEndKey( params.getString("endKey") );
      }
    }
    if (params.hasKey("descending")) {
      query.setDescending( params.getBoolean("descending") );
    }
    if (params.hasKey("limit")) {
      query.setLimit( params.getInt("limit") );
    }
  }

  @ReactMethod
  public void query(String view, ReadableMap params, Promise promise) {
    View dbview = this.getView(view);
    Query query = dbview.createQuery();
    try {
      QueryEnumerator result = query.run();
      promise.resolve( ConversionUtil.toWritableArray( this.getQueryResults(result).toArray() ) );
    } catch (CouchbaseLiteException e) {
      promise.reject("query", "Error running query", e);
    }
  }

  @ReactMethod
  public void createLiveQuery(String view, ReadableMap params, Promise promise) {
    View dbview = this.getView(view);
    Query query = dbview.createQuery();
    this.setQueryParams(query, params);
    final LiveQuery liveQuery = query.toLiveQuery();
    final String uuid = UUID.randomUUID().toString();
    final RNReactNativeCblModule self = this;
    liveQuery.addChangeListener(new LiveQuery.ChangeListener() {
      @Override
      public void changed(LiveQuery.ChangeEvent event) {
        WritableMap params = Arguments.createMap();
        params.putString("uuid", uuid);
        params.putArray("data", ConversionUtil.toWritableArray( self.getQueryResults(event.getRows()).toArray() ));
        self.sendEvent("liveQueryChange", params);
      }
    });
    this.liveQueries.put(uuid, liveQuery);
    promise.resolve(uuid);
    liveQuery.start();
  }

  @ReactMethod
  public void destroyLiveQuery(String uuid, Promise promise) {
    this.liveQueries.remove(uuid);
    promise.resolve(null);
  }

  public void changed(Database.ChangeEvent event) {
    for (DocumentChange change : event.getChanges()) {
      for (String queryUuid : this.liveQueries.keySet()) {
        QueryEnumerator rows = this.liveQueries.get(queryUuid).getRows();
        while (rows.hasNext()) {
          QueryRow row = rows.next();
          if (row.getDocumentId().equals(change.getDocumentId())) {
            WritableMap params = Arguments.createMap();
            params.putString("uuid", queryUuid);
            params.putArray("data", ConversionUtil.toWritableArray(this.getQueryResults(rows).toArray()));
            this.sendEvent("liveQueryChange", params);
            break;
          }
        }
      }
    }
  }

  private ArrayList getQueryResults(QueryEnumerator result) {
    ArrayList<HashMap<String, Object>> list = new ArrayList<>();
    for (Iterator<QueryRow> it = result; it.hasNext(); ) {
      QueryRow row = it.next();
      Document doc = row.getDocument();
      if (doc != null) {
        HashMap<String, Object> props = new HashMap(doc.getProperties());
        if (props != null) {
          list.add( props );
        }
      } else {
        HashMap<String, Object> item = new HashMap();
        item.put("key", row.getKey());
        item.put("value", row.getValue());
        list.add(item);
      }
    }
    return list;
  }

  @ReactMethod
  public void startReplication(String remoteUrl, String facebookToken, Promise promise) {
    try {
      URL url = new URL(remoteUrl);
      Replication push = this.db.createPushReplication(url);
      Replication pull = this.db.createPullReplication(url);
      pull.setContinuous(true);
      push.setContinuous(true);
      Authenticator auth = AuthenticatorFactory.createFacebookAuthenticator(facebookToken);
      push.setAuthenticator(auth);
      pull.setAuthenticator(auth);
      promise.resolve(null);
    } catch (MalformedURLException e) {
      promise.reject("start_replication", "Malformed remote URL", e);
    }
  }

  @ReactMethod
  public void addAttachment(String contentUri, String attachmentName, String documentId, Promise promise) {
    try {
      Uri uri = Uri.parse(contentUri);
      InputStream stream = this.mReactContext.getContentResolver().openInputStream(uri);
      String contentType = this.mReactContext.getContentResolver().getType(uri);
      Document doc = this.db.getDocument(documentId);
      UnsavedRevision newRev = doc.getCurrentRevision().createRevision();
      newRev.setAttachment(attachmentName, contentType, stream);
      newRev.save();
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
      Document doc = this.db.getDocument(documentId);
      UnsavedRevision newRev = doc.getCurrentRevision().createRevision();
      newRev.removeAttachment(attachmentName);
      newRev.save();
      promise.resolve(null);
    } catch (CouchbaseLiteException e) {
      promise.reject("remove_attachment", "Can not remove attachment", e);
    }
  }

  @ReactMethod
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
  }
}
