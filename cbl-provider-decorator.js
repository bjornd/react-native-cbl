import React from 'react'
import { NativeEventEmitter } from 'react-native'
import { entriesToObject } from './utils'
import hoistNonReactStatics from 'hoist-non-react-statics'
import CouchbaseLite from './react-native-cbl'
import PropTypes from 'prop-types'

const cblEventEmitter = new NativeEventEmitter(CouchbaseLite)

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

        this.state = {
          results: entriesToObject(
            Object.entries( getParams(props) ).map( ([key, value]) => [key, value.view ? [] : {} ] )
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
            if (values.view) {
              if (values.live === false) {
                CouchbaseLite.query(values.view, values.params).then( data => {
                  this.setState( ({ results }) => {
                    return ({ results: { ...results, [key]: data } })
                  })
                })
              } else {
                CouchbaseLite.createLiveQuery(values.view, values.params).then( uuid => {
                  this.liveQueries[uuid] = key
                  this.postProcess[uuid] = values.postProcess
                })
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

      componentWillUnmount() {
        Object.entries( this.liveQueries ).forEach( ([key, value]) => {
          CouchbaseLite.destroyLiveQuery( key )
          delete this.liveQueries[key]
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
