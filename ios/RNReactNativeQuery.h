//
//  RNReactNativeQuery.h
//  RNReactNativeCbl
//
//  Created by Kirill Lebedev on 17/07/2018.
//  Copyright © 2018 Facebook. All rights reserved.
//

#import <CouchbaseLite/CouchbaseLite.h>

@interface RNReactNativeQuery : CBLQuery

- (instancetype) initWithJson:(id)jsonSchema database:(CBLDatabase*)database;

@property (atomic, copy, nullable) id jsonSchema;
@property (atomic, copy, nullable) CBLDatabase* database;

@end
