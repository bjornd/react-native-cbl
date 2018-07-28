//
//  RNReactNativeQuery.m
//  RNReactNativeCbl
//
//  Created by Kirill Lebedev on 17/07/2018.
//  Copyright © 2018 Facebook. All rights reserved.
//

#import "RNReactNativeQuery.h"

@implementation RNReactNativeQuery

@synthesize database = _database;
@synthesize jsonSchema = _jsonSchema;

- (instancetype) initWithJson:(id)jsonSchema database:(CBLDatabase*)database
{
    self = [super init];
    if (self) {
        _database = database;
        _jsonSchema = jsonSchema;
    }
    return self;
}

- (NSDictionary*) generateColumnNames: (NSError**)outError {
    NSMutableDictionary* map = [NSMutableDictionary dictionary];
    NSDictionary *select = ((NSArray *)_jsonSchema)[1];
    NSUInteger count = [select[@"WHAT"] count];
    
    for (int i = 0; i < count; i++) {
        [map setObject:@(i) forKey:[NSString stringWithFormat:@"f%i", i]];
    }
    
    return map;
}

- (id) asJSON {
    return _jsonSchema;
}

@end
