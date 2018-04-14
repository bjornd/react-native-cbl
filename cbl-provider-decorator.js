import React from 'react'
import { NativeEventEmitter } from 'react-native'
import { entriesToObject } from './utils'
import { NativeModules } from 'react-native'

const { RNReactNativeCbl } = NativeModules

export function cblProvider(getParams) {
  return WrappedComponent => {
    const liveQueries = {}
    const liveDocuments = {}
    const postProcess = {}
    const createQueries = props => {
      RNReactNativeCbl.openDb('odygos', true).then( () =>
        Object.entries( getParams(props) ).forEach( ([key, values]) => {
          if (values.view) {
            RNReactNativeCbl.createLiveQuery(values.view, values.params).then( uuid => {
              liveQueries[uuid] = key
              postProcess[uuid] = values.postProcess
            })
          } else if (values.docId) {
            RNReactNativeCbl.createLiveDocument(values.docId).then( uuid => {
              liveDocuments[uuid] = key
            })
          }
        })
      )
    }

    class CblProvider extends React.Component {
      constructor(props) {
        super(props)

        this.state = {
          results: entriesToObject(
            Object.entries( getParams(props) ).map( ([key, value]) => [key, value.view ? [] : {} ] )
          )
        }

        const cblEventEmitter = new NativeEventEmitter(RNReactNativeCbl)
        this.liveQueryListener = cblEventEmitter.addListener(
          'liveQueryChange',
          ({data, uuid}) => {
            if (!liveQueries[uuid]) {
              return
            }
            if (postProcess[uuid]) {
              data = postProcess[uuid](data)
            }
            this.setState( ({ results }) => {
              return ({ results: { ...results, [liveQueries[uuid]]: data } })
            })
          }
        )
        this.liveDocumentListener = cblEventEmitter.addListener(
          'liveDocumentChange',
          ({data, uuid}) => {
            if (!liveDocuments[uuid]) {
              return
            }
            this.setState( ({ results }) =>
              ({ results: { ...results, [liveDocuments[uuid]]: data } })
            )
          }
        )

        createQueries(props)
      }

      componentWillUnmount() {
        Object.entries( liveQueries ).forEach( ([key, value]) => {
          RNReactNativeCbl.destroyLiveQuery( key )
        })
        Object.entries( liveDocuments ).forEach( ([key, value]) => {
          RNReactNativeCbl.destroyLiveDocument( key )
        })
        this.liveQueryListener.remove()
        this.liveDocumentListener.remove()
      }

      render() {
        return <WrappedComponent {...this.props} {...this.state.results} />
      }
    }

    return CblProvider
  }
}
