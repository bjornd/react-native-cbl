---
title: cblProvider
---
react-native-cbl provides a very convenient way to access data from Couchbase Lite using HOC pattern. To activate this ability for components you first need to add `<CBLConnector>` component to your application, so it will be a ancestor for any underlying component requiring access to Couchbase Lite. This is somewhat similar to `<Provider>` component in redux:
```
import { CBLConnection, CBLConnector } from 'react-native-cbl'

const cblConnection = new CBLConnection({
  dbName: 'mydb',
  syncGatewayUrl: 'http://sg.myapp.com/mydb',
  views: { ... },
})

export default class App extends React.Component {
  render() {
    return (
      <CBLConnector connection={cblConnection}>
        ...
      </CBLConnector>
    )
  }
}
```
After that `cblProvider` can be used to decorate components requiring some data from Couchbase Lite:
```
@cblProvider( props => ({
  ...
}))
class ItemView extends React.Component {
  ...
}
```
Callback passed to cblProvider returns configuration object, keys of this object will be converted to corresponding component props holding data retrieved from Couchbase Lite.
```
@cblProvider( props => ({
  item: {
    docId: 'documentid',
  },
  subitems: {
    view: 'viewname',
  },
}))
class ItemView extends React.Component {
  render() {
    return (
      <View>
        <Text>{this.props.item.title}</Text>
        { this.props.subitems.map( subitem => <View>{subitem.text}</View> ) }
      </View>
    )
  }
}
```
Callback function passed to cblProvider accepts component props. This could be used to dynamically generate config:
```
@cblProvider( props => ({
  item: {
    docId: props.itemId,
  },
  subitems: {
    view: 'viewname',
    params: {
      descending: props.direction === 'desc',
    }
  },
}))
```
By default cblProvider not only retrieve data once, it also listens to any related changes in database and updates data passed to component accordingly. This behavior can be changed by using `live: false` parameter:
```
@cblProvider( props => ({
  item: {
    docId: props.itemId,
    live: false,
  },
  subitems: {
    view: 'viewname',
    live: false,
  },
}))
```
View queries support additional option `params`, which modifies the way view is queried by Couchbase Lite. The full list of available options is available at the Couchbase Lite [Query API reference](https://developer.couchbase.com/documentation/mobile/1.5/guides/couchbase-lite/native-api/query/index.html#creating-and-configuring-queries).
```
@cblProvider( props => ({
  items: {
    view: 'viewname',
    params: {
      descending: true,
      startKey: 'a',
      endKey: 'b',
    }
  },
}))
```
