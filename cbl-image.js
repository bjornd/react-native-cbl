import React from 'react'
import { Image, NativeModules, findNodeHandle, requireNativeComponent, Image } from 'react-native'
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
    const ImageImplementation = RNReactNativeCblImage || Image
    return (
      <ImageImplementation ref={ ref => this.image = ref } {...restProps} />
    )
  }
}
