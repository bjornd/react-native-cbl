---
id: introduction
title: Introduction
---
react-native-cbl is a [React Native](https://facebook.github.io/react-native/) wrapper for [Couchbase Lite](https://developer.couchbase.com/documentation/mobile/1.4/training/index.html) database. Unlike existing wrappers it's implemented using native bridge providing maximum performance and functionality. Features implemented:

- CRUD operations on documents.
- Support for live queries and live documents. Get notified on any changes, UI is updated accordingly.
- Attachments management. No big amounts of data is transferred across the React Native bridge providing maximum speed for attachment operations.
- Replications. All the changes are synchronized with the server.
