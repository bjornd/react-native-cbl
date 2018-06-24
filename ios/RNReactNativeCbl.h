#if __has_include("RCTBridgeModule.h")
#import <React/RCTBridgeModule.h>
#else
#import "RCTBridgeModule.h"
#endif

#import <UIKit/UIKit.h>
#import <Couchbaselite/CouchbaseLite.h>
#import <React/RCTEventEmitter.h>

@interface RNReacNativeCbl : RCTEventEmitter <RCTBridgeModule>

@property (nonatomic, copy) CBLDatabase* db;
@property (nonatomic, copy) NSMutableDictionary* liveQueries;
@property (nonatomic, copy) NSMutableDictionary* liveDocuments;
@property (nonatomic, copy) CBLReplicator* push;
@property (nonatomic, copy) CBLReplicator* pull;

@end

