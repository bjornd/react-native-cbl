import React from 'react'
import { Image, NativeModules, findNodeHandle, requireNativeComponent } from 'react-native'
import { cblConnectAttachmentToImage } from 'react-native-cbl'

const { RNReactNativeCbl } = NativeModules
const RNReactNativeCblImage = requireNativeComponent('RNReactNativeCblImage', null)

export class CBLImage extends React.Component {
  componentDidMount() {
    RNReactNativeCbl.connectAttachmentToImage(
      findNodeHandle(this.image),
      this.props.documentId,
      this.props.attachmentName
    )
  }

  render() {
    const { documentId, attachmentName, ...restProps } = this.props
    return (
      <RNReactNativeCblImage ref={ ref => this.image = ref } {...restProps} />
    )
  }
}
