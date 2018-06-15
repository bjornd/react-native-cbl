import React from 'react'
import CouchbaseLite from './react-native-cbl'
import PropTypes from 'prop-types'

export class CBLConnection {
  constructor(config) {
    this.config = config
  }

  connect() {
    if (!this.promise) {
      this.promise = CouchbaseLite.openDb(
        this.config.dbName, false
      ).then( () => CouchbaseLite.startReplication( this.config.syncGatewayUrl, null ) )
    }
    return this.promise
  }
}

export class CBLConnector extends React.Component {
  static childContextTypes = {
    cblConnection: PropTypes.object,
  }

  constructor(props, ...args) {
    super(props, ...args)

    props.connection.connect()
  }

  getChildContext() {
    return {
      cblConnection: this.props.connection,
    }
  }

  render() {
    return this.props.children
  }
}
