---
title: cblProvider
---
cblProvider is decorator function for your components allowing easier access to Couchbase Lite data. It is usually used the following way:
```
@cblProvider( props => ({
  ...
}))
class ItemView extends React.Component {
  ...
}
```
Callback passed to cblProvider returns configuration object, keys of this object will be converted to corresponding props of the component holding data retrieved from Couchbase Lite.
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
