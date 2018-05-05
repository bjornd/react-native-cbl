---
id: documents-and-views
title: Views and Documents
---
The two basic entities defining how you store and retrieve data in Couchbase are **Document** and **View**. Basically **Document** is just a JSON structure containing user data and some special keys like `_id` and `_delete`. **Views** provide the way to get access to the collections of **Documents** in different ways. Using Views you can implement the following basic operations you can usually perform with any other data storage solution:

- Sorting
- Filtering
- Grouping and Aggregation

You can read more about Documents in Views in the official [Couchbase Lite tutorial](https://developer.couchbase.com/documentation/mobile/1.5/guides/couchbase-lite/native-api/data-modeling/index.html)
