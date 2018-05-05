---
id: cbl-provider
title: Couchbase Lite Provider
---
```
import React from 'react'
import { View } from 'react-native'
import { cblProvider } from 'react-native-cbl'

@cblProvider( props => ({
  category: {
    docId: props.categoryId,
  },
  notes: {
    view: 'main/notes',
    params: {
      startKey: [props.categoryId, {}],
      endKey: [props.categoryId],
    }
  },
}))
export default class CategoryNotes extends React.Component {
  render() {
    return (
      <View>
        <Text>{ this.props.category.title }</Text>
        {
          this.props.notes.map( note =>
            <View>
              <Text>{note.title}</Text>
              <Text>{note.text}</Text>
            </View>
          )
        }
      </View>
    )
  }
}
```
