---
id: installation
title: Installation
---
Install package with npm:
```
npm install react-native-cbl --save
```
or yarn:
```
yarn add react-native-cbl
```
## iOS
Download [Couchbase Lite version 1.4.1](https://www.couchbase.com/downloads/thankyou/community?product=couchbase-lite&version=1.4.1&platform=ios&addon=false&beta=false) for iOS from official website.

Unzip archive and drag and drop the following files to Frameworks folder of your iOS project in Xcode:
- CouchbaseLite.framework
- CouchbaseLiteListener.framework
- CBLRegisterJSViewCompiler.h
- libCBLJSViewCompiler.a
