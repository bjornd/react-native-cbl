import React from 'react'
import { NativeEventEmitter } from 'react-native'
import { entriesToObject } from './utils'
import hoistNonReactStatics from 'hoist-non-react-statics'
import CouchbaseLite from './react-native-cbl'
import PropTypes from 'prop-types'
import cblQueryParser from 'couchbase-lite-query-parser'

const cblEventEmitter = new NativeEventEmitter(CouchbaseLite)

function shallowDiffers(a, b) {
  for (let i in a) if (!(i in b)) return true
  for (let i in b) if (a[i] !== b[i]) return true
  return false
}

function optimizeQuery(q) {
  if (q instanceof Array) {
    const nq = q.map( v => optimizeQuery(v) )
    if (nq[0] === '.') {
      return ['.'+nq.slice(1).join('.')]
    } else {
      return nq
    }
  } else if (q instanceof Object) {
    return Object.entries(q).map(([k, v]) => [k, optimizeQuery(v)])
      .reduce( (acc, [k, v]) => ({...acc, [k]: v}), [] )
  } else {
    return q
  }
}

function convertQueryResult(results, query) {
  const fields = query[1]["WHAT"]
  return results.map( row => {
    const rowData = {}
    fields.forEach( (field, index) => {
      if (field instanceof Array && field.length === 1) {
        if (field[0] === '.') {
          Object.assign(rowData, row[index])
        } else {
          rowData[field[0].substr(1)] = row[index];
        }
      }
    })
    return rowData
  })
}

export function cblProvider(getParams) {
  return WrappedComponent => {
    class CblProvider extends React.Component {
      static contextTypes = {
        cblConnection: PropTypes.object,
      }

      constructor(props, context) {
        super(props, context)

        this.liveQueries = {}
        this.liveDocuments = {}
        this.postProcess = {}
        this.queries = {}
        this.parsedQueries = {}
        this.queryUuidByKey = {}

        this.state = {
          results: entriesToObject(
            Object.entries( getParams(props) ).map( ([key, value]) => [key, value.query ? [] : {} ] )
          )
        }

        this.liveQueryListener = cblEventEmitter.addListener(
          'liveQueryChange', this.onLiveQueryChange.bind(this)
        )
        this.liveDocumentListener = cblEventEmitter.addListener(
          'liveDocumentChange', this.onLiveDocumentChange.bind(this)
        )

        this.createQueries(props, context.cblConnection)
      }

      createQueries(props, connection) {
        connection.promise.then( () =>
          Object.entries( getParams(props) ).forEach( ([key, values]) => {
            if (values.query) {
              if (this.queries[key] !== values.query) {
                const parsedQuery = optimizeQuery(cblQueryParser.parse(values.query));
                if (values.live === false) {
                  CouchbaseLite.query(parsedQuery).then( data => {
                    const convertedData = convertQueryResult(data, parsedQuery)
                    this.queries[key] = values.query
                    this.setState( ({ results }) => {
                      return ({ results: { ...results, [key]: convertedData } })
                    })
                  })
                } else {
                  let whenReady
                  if (this.queries[key]) {
                    whenReady = CouchbaseLite.destroyLiveQuery(this.queryUuidByKey[key])
                  } else {
                    whenReady = Promise.resolve()
                  }
                  whenReady.then(() => CouchbaseLite.createLiveQuery(parsedQuery)).then( uuid => {
                    this.parsedQueries[uuid] = parsedQuery
                    this.liveQueries[uuid] = key
                    this.queryUuidByKey[key] = uuid
                    this.postProcess[uuid] = values.postProcess
                    this.queries[key] = values.query
                  })
                }
              }
            } else if (values.docId) {
              if (values.live === false) {
                CouchbaseLite.getDocument(values.docId).then( data => {
                  this.setState( ({ results }) =>
                    ({ results: { ...results, [key]: data } })
                  )
                })
              } else {
                CouchbaseLite.createLiveDocument(values.docId).then( uuid => {
                  this.liveDocuments[uuid] = key
                })
              }
            }
          })
        )
      }

      onLiveQueryChange({data, uuid}) {
        if (!this.liveQueries[uuid]) {
          return
        }
        data = convertQueryResult(data, this.parsedQueries[uuid])
        if (this.postProcess[uuid]) {
          data = this.postProcess[uuid](data)
        }
        this.setState( ({ results }) => {
          return ({ results: { ...results, [this.liveQueries[uuid]]: data } })
        })
      }

      onLiveDocumentChange({data, uuid}) {
        if (!this.liveDocuments[uuid]) {
          return
        }
        this.setState( ({ results }) =>
          ({ results: { ...results, [this.liveDocuments[uuid]]: data } })
        )
      }

      componentDidUpdate(prevProps) {
        if (shallowDiffers(prevProps, this.props)) {
          this.createQueries(this.props, this.context.cblConnection)
        }
      }

      componentWillUnmount() {
        Object.entries( this.liveQueries ).forEach( ([key, value]) => {
          CouchbaseLite.destroyLiveQuery( key )
          delete this.liveQueries[key]
          delete this.postProcess[key]
          delete this.parsedQueries[key]
        })
        Object.entries( this.liveDocuments ).forEach( ([key, value]) => {
          CouchbaseLite.destroyLiveDocument( key )
          delete this.liveDocuments[key]
        })
        this.liveQueryListener.remove()
        this.liveDocumentListener.remove()
      }

      render() {
        return <WrappedComponent {...this.props} {...this.state.results} />
      }
    }

    hoistNonReactStatics(CblProvider, WrappedComponent)

    return CblProvider
  }
}
