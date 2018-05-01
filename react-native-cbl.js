import { NativeModules } from 'react-native'

const { RNReactNativeCbl } = NativeModules

const currentOpenDb = RNReactNativeCbl.openDb

RNReactNativeCbl.openDb = function(...args){
  RNReactNativeCbl.defaultDbName = args[0]
  return currentOpenDb.apply( RNReactNativeCbl, args )
}

export default RNReactNativeCbl
