import { NativeModules } from 'react-native'

/** @class */
const CouchbaseLite = NativeModules.RNReactNativeCbl

const proxyMethods = {
  /**
   * Open database
   * @memberof CouchbaseLite
   * @param {string} name - Database name, if database with the given name does not exist it will be created.
   * @param {boolean} installPrebuildDb - If true, database will be installed from the assets.
   * @return {Promise}
   */
  openDb: function(nativeMethod, ...args){
    this.defaultDbName = args[0]
    return nativeOpenDb.apply( this, args )
  },

  /**
   * Get document properties
   * @kind function
   * @memberof CouchbaseLite
   * @param {string} docId - Document id
   * @fulfil {Object} - Document properties
   * @return {Promise}
   */
  getDocument: null,

  /**
   * Create new document
   * @kind function
   * @memberof CouchbaseLite
   * @param {Object} properties - Document properties
   * @fulfil {String} - id of created document
   * @return {Promise}
   */
  createDocument: null,

  /**
   * Update document properties
   * @kind function
   * @memberof CouchbaseLite
   * @param {String} docId - Document id
   * @param {Object} properties - Document properties
   * @return {Promise}
   */
  updateDocument: null,

  /**
   * Update document properties
   * @kind function
   * @memberof CouchbaseLite
   * @param {String} docId - Document id
   * @return {Promise}
   */
  deleteDocument: null,

  /**
   * Start listening for document changes, returns current document properties immidiatly
   * @kind function
   * @memberof CouchbaseLite
   * @param {String} docId - Document id
   * @fulfil {String} - unique id of live document, should be used to distinguish between updates of different live documents in the listener
   * @return {Promise}
   */
  createLiveDocument: null,

  /**
   * Stop listening for document changes
   * @kind function
   * @memberof CouchbaseLite
   * @param {String} uuid - Live document UUID
   * @return {Promise}
   */
  destroyLiveDocument: null,

  /**
   * Query existing Couchbase Lite view
   * @kind function
   * @memberof CouchbaseLite
   * @param {String} view - View name to query
   * @param {Object} params - Query params
   * @fulfil {Array} - array of document properties
   * @return {Promise}
   */
  query: null,

  /**
   * Start listening for query updates, returns current query data immidiatly
   * @kind function
   * @memberof CouchbaseLite
   * @param {String} view - View name to query
   * @param {Object} params - Query params
   * @fulfil {String} - unique id of live query, should be used to distinguish between updates of different live queries in the listener
   * @return {Promise}
   */
  createLiveQuery: null,

  /**
   * Stop listening for query changes
   * @kind function
   * @memberof CouchbaseLite
   * @param {String} uuid - Live query UUID
   * @return {Promise}
   */
  destroyLiveQuery: null,

  /**
   * Add attachment to document
   * @kind function
   * @memberof CouchbaseLite
   * @param {String} assetUri - Asset URI to use as attachment
   * @param {String} attachmentName - Attachment name
   * @param {String} docId - Document id to add attachment to
   * @return {Promise}
   */
  addAttachment: null,

  /**
   * Add attachment to document
   * @kind function
   * @memberof CouchbaseLite
   * @param {String} attachmentName - Attachment name
   * @param {String} docId - Document id to remove attachment from
   * @return {Promise}
   */
  removeAttachment: null,
}

const applyProxy = (target, methods) => {
  Object.entries(methods).forEach( ([name, method]) => {
    if (method !== null) {
      target[name] = method.bind(target, target[name])
    }
  })
}

export default applyProxy(CouchbaseLite, proxyMethods)
