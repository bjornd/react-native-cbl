
# react-native-react-native-cbl

## Getting started

`$ npm install react-native-cbl --save`

or

`$ yarn add react-native-cbl`

### Mostly automatic installation

`$ react-native link react-native-cbl`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-cbl` and add `RNReactNativeCbl.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNReactNativeCbl.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.reactlibrary.RNReactNativeCblPackage;` to the imports at the top of the file
  - Add `new RNReactNativeCblPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-cbl'
  	project(':react-native-cbl').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-cbl/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-cbl')
  	```

## Usage
```javascript
import CouchbaseLite from 'react-native-cbl';

// TODO: What to do with the module?
CouchbaseLite;
```
